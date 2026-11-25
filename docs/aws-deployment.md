# Déploiement AWS — démonstration sécurisée

Ce document décrit le déploiement pédagogique du Sprint 12. La plateforme reste une simulation bancaire : aucun vrai fonds, aucune donnée personnelle réelle et aucun secret ne doit être commité.

## Prérequis

- Sprints 0 à 11 terminés, CI GitHub verte et `docker compose up --build` validé localement.
- AWS CLI configuré avec un profil dédié, par exemple `banking-demo`.
- Docker et Docker Compose disponibles sur l'instance EC2.
- Un domaine ou sous-domaine de démonstration avec un certificat ACM en région choisie.

## 1. Sécuriser le compte

- Activer MFA sur l'utilisateur racine et sur l'utilisateur de déploiement.
- Créer un utilisateur IAM `banking-demo-deployer` avec permissions minimales : `AmazonEC2ContainerRegistryFullAccess` porté sur les dépôts `banking/*`, `AmazonEC2FullAccess`, `AmazonRDSFullAccess`, `AmazonS3FullAccess` sur un bucket dédié, `CloudWatchLogsFullAccess` et `AWSBudgetsActionsWithAWSResourceControlAccess`.
- Attacher une policy de déni explicite sur `iam:CreateUser`, `iam:AttachUserPolicy` et `iam:CreateAccessKey` pour l'utilisateur de déploiement.
- Créer deux budgets AWS Budgets : `banking-demo-monthly-10` à 10 USD et `banking-demo-monthly-20` à 20 USD, avec alerte email vers une adresse de démonstration.
- Stocker `JWT_SECRET`, `INTERNAL_SERVICE_TOKEN`, les mots de passe DB et `RABBITMQ_PASSWORD` dans AWS Systems Manager Parameter Store en type `SecureString`, puis les injecter dans `infrastructure/.env` sur EC2 au moment du démarrage.

```powershell
aws configure --profile banking-demo
aws budgets create-budget --account-id "<account-id>" --budget file://infrastructure/aws/budget.json --notifications-with-subscribers file://infrastructure/aws/budget-notifications.json --profile banking-demo
aws ssm put-parameter --name "/banking/demo/jwt-secret" --type SecureString --value "<valeur-generee>" --profile banking-demo
```

Les fichiers `budget.json` et `budget-notifications.json` sont des exemples sans donnée secrète ; remplacer les espaces réservés avant exécution.

## 2. Réseau

La définition CloudFormation non sensible se trouve dans [`infrastructure/aws/network.json`](../infrastructure/aws/network.json).

```powershell
aws cloudformation deploy --template-file infrastructure/aws/network.json --stack-name banking-demo-network --capabilities CAPABILITY_NAMED_IAM --parameter-overrides Environment=demo AllowedSshCidr="<votre-ip>/32" --profile banking-demo
```

Topologie retenue :

- VPC `10.20.0.0/16`, un sous-réseau public `10.20.1.0/24` pour l'ALB et l'EC2, un sous-réseau privé `10.20.2.0/24` pour RDS.
- Internet Gateway associé uniquement au sous-réseau public.
- `sg-alb` accepte 443 depuis Internet ; `sg-app` accepte 4200/8080 uniquement depuis `sg-alb` et SSH uniquement depuis `AllowedSshCidr`.
- `sg-db` accepte 5432 uniquement depuis `sg-app`.
- RDS est lancé avec `PubliclyAccessible=false` ; aucun port DB n'est exposé sur Internet.

## 3. Publier les images dans ECR

Les 12 dépôts attendus sont : `banking/config-server`, `banking/discovery-server`, `banking/api-gateway`, `banking/demo-service`, `banking/auth-service`, `banking/user-service`, `banking/account-service`, `banking/transfer-service`, `banking/transaction-service`, `banking/notification-service`, `banking/audit-service` et `banking/frontend`.

```powershell
powershell -File infrastructure/aws/ecr-push.ps1 -AccountId "<account-id>" -Region "eu-west-3" -Version "git:<sha>"
```

Le script étiquette chaque image avec le SHA Git court et `latest`, pousse vers ECR et n'affiche aucun secret. La version doit correspondre au commit de la CI GitHub.

## 4. Déployer la démonstration

Option retenue pour le budget : une instance EC2 `t3.small` Amazon Linux 2023 avec Docker Compose, derrière un ALB public terminant TLS via ACM.

```powershell
ssh -i "<cle-privee-locale>" ec2-user@<ip-publique>
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo curl -L "https://github.com/docker/compose/releases/download/v2.39.4/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
git clone https://github.com/khemiriahmed/banking-management-microservices.git
cd banking-management-microservices
aws ssm get-parameter --name "/banking/demo/jwt-secret" --with-decryption --query Parameter.Value --output text --region <region> > /tmp/jwt.txt
```

Renseigner `infrastructure/.env` sur l'instance à partir des paramètres SSM, puis lancer :

```powershell
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

Points d'entrée publics :

- `https://demo.<domaine>` → ALB → `frontend:4200` (Nginx).
- Le frontend relaie `/auth`, `/users`, `/accounts`, `/beneficiaries`, `/transfers`, `/transactions`, `/notifications` et `/admin/audit` vers `api-gateway:8080` à l'intérieur du VPC.
- `/actuator/health` et `/actuator/gateway/routes` ne sont accessibles qu'à l'intérieur du réseau ou via une règle ALB dédiée protégée.
- Les NoSQL et bases PostgreSQL restent privées ; aucun `5432`, `5672` ou `15672` n'est exposé publiquement.

Limites de cette configuration : une seule EC2, pas de redondance multi-AZ applicative, pas d'auto-scaling, pas de rolling update. Elle sert uniquement à la démonstration ; une mise en production demanderait ECS/Fargate ou EKS, plusieurs AZ et des migrations gérées.

## 5. Base de données et logs

- Option A recommandée : RDS PostgreSQL `db.t3.micro`, Multi-AZ désactivé, sauvegardes 7 jours, accès depuis `sg-db` uniquement. Flyway applique les migrations au démarrage des services.
- Option B budget : PostgreSQL local dans Compose sur l'EC2 ; documenter explicitement qu'il n'y a ni sauvegarde managée ni haute disponibilité.
- CloudWatch Logs : installer l'agent `amazon-cloudwatch-agent` sur EC2 et collecter `/var/lib/docker/containers/*/*.log`, `/var/log/messages` et les healthchecks.
- Créer une alarme CloudWatch sur `CPUUtilization > 80 %` pendant 5 minutes et sur l'absence de réponse `HTTPS 200` du frontend.
- S3 reste optionnel : bucket `banking-demo-evidence-<account-id>` avec Block Public Access activé, versioning activé et chiffrement SSE-S3, utilisé uniquement pour les captures de démonstration.

## 6. Validation

- [ ] Le budget AWS et les deux alertes sont actifs.
- [ ] MFA actif sur root et utilisateur IAM de déploiement.
- [ ] VPC, sous-réseaux, IGW, route tables et security groups correspondent au template.
- [ ] Les 12 images sont présentes dans ECR avec le tag du commit CI.
- [ ] `https://demo.<domaine>` répond `200`, le certificat ACM est valide et le proxy frontend fonctionne.
- [ ] Inscription/login via Gateway, consultation comptes, création bénéficiaire et virement fictif réussissent.
- [ ] Sans JWT, les routes protégées répondent `401` ; un `CLIENT` ne peut pas accéder à `/admin/audit`.
- [ ] RDS n'est pas publiquement accessible ; seul `sg-app` peut joindre `sg-db`.
- [ ] Les logs Docker et system sont visibles dans CloudWatch.
- [ ] Aucun secret, fichier `.env`, clé privée ou token n'est présent dans Git ni dans les logs.
- [ ] Les ressources inutilisées sont arrêtées ou supprimées après la démonstration.

## Comptes de démonstration fictifs

Créer les comptes via `POST /auth/register` avec des adresses `.invalid`, par exemple `client.demo@banking.invalid` et `admin.demo@banking.invalid`. Le rôle `ADMIN` doit être attribué uniquement via la procédure documentée du service Auth. Ne jamais committer les mots de passe utilisés pour la démo.

## Coût estimatif

Estimation mensuelle indicative pour la région `eu-west-3`, hors free tier et hors trafic Internet important :

| Ressource | Configuration | Estimation USD/mois |
| --- | --- | ---: |
| EC2 | `t3.small`, 20 GiB gp3 | 15-20 |
| ALB | 1 ALB + 1 target group | 18-22 |
| RDS | `db.t3.micro`, 20 GiB gp3, 7 jours de backups | 15-20 |
| ECR | 12 images, ~200 GiB de couches partagées | 1-3 |
| CloudWatch | Logs + 2 alarmes + métriques | 2-5 |
| S3 | Quelques captures, SSE-S3 | < 1 |
| Data transfer | Faible trafic de démonstration | 1-5 |
| **Total** | | **52-76** |

Pour réduire le coût : utiliser l'option PostgreSQL locale, supprimer l'ALB et terminer TLS directement sur EC2 avec Caddy, arrêter l'EC2 hors sessions de démo, et supprimer RDS après validation.

## Nettoyage

```powershell
aws cloudformation delete-stack --stack-name banking-demo-network --profile banking-demo
aws rds delete-db-instance --db-instance-identifier banking-demo-db --skip-final-snapshot --profile banking-demo
aws ec2 disassociate-address --association-id "<eip-association-id>" --profile banking-demo
aws ec2 release-address --allocation-id "<eip-allocation-id>" --profile banking-demo
aws ecr list-images --repository-name banking/frontend --profile banking-demo
aws s3 rb s3://banking-demo-evidence-<account-id> --force --profile banking-demo
```

Supprimer ensuite les dépôts ECR, l'instance EC2, les volumes, les alarmes CloudWatch, les budgets et les paramètres SSM. Vérifier le dashboard AWS Cost Explorer après suppression pour confirmer l'arrêt des coûts.
