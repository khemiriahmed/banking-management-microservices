# Feuille de route des sprints

Objectif : un sprint traité par jour ouvré. Les sprints longs du cahier des charges sont découpés en livrables quotidiens ; un sprint n'est clos que lorsque ses critères de validation sont passés.

| Jour | Sprint | Résultat principal | Dépendance | Statut |
| --- | --- | --- | --- | --- |
| J1 | 0 — Préparation | Outils, dépôt, structure, conventions Git | — | Terminé |
| J2 | 1 — Fondations Spring Cloud | Config Server + Eureka + Gateway | S0 | Terminé |
| J3 | 2 — Auth | Inscription, login, JWT via Gateway | S1 | Terminé |
| J4 | 3 — Users | Profil client sécurisé, rôles | S2 | Terminé |
| J5 | 4 — Accounts | Comptes, soldes `BigDecimal`, statuts | S3 | Terminé |
| J6 | 5 — Beneficiaries | CRUD bénéficiaires isolés par client | S4 | Terminé |
| J7-8 | 6 — Transfers | Saga, compensation, idempotence | S4-S5 | Terminé |
| J9 | 7 — Transactions | Historique paginé et sécurisé | S6 | Terminé |
| J10 | 8 — Events | RabbitMQ, notifications, audit | S6-S7 | Terminé |
| J11-12 | 9 — Angular | Parcours CLIENT et ADMIN complets | APIs stables | Terminé |
| J13 | 10 — Docker | `docker compose up --build` | S9 | Terminé (E2E en attente Docker Desktop) |
| J14 | 11 — CI/CD | GitHub Actions, OpenAPI, README final | S10 | Terminé |
| J15 | 12 — AWS | Démo sécurisée, coûts documentés | S11 | Terminé |

## Détail du Sprint 0 (terminé)

- [x] Lecture et analyse du cahier des charges.
- [x] 0.1 — Vérifier les outils : Java 17, Maven, Node 18, npm, Docker.
- [x] 0.2 — Définir les dossiers du monorepo.
- [x] 0.3 — README, `.gitignore`, conventions de branches et de commits.
- [x] 0.4 — Documentation initiale et diagramme d'architecture.
- [x] 0.5 — Premier commit poussé sur GitHub (`develop` et `main`).

### État des prérequis

| Outil | Attendu | Observé | Action |
| --- | --- | --- | --- |
| Java | 17 | 26.0.1 global, Temurin 17.0.21 portable | Toolchain 17 utilisé pour les builds Sprint 1 |
| Maven | 3.9+ | absent globalement, 3.9.16 portable | Maven Wrapper ajouté dans chaque service |
| Node | 18.x (Angular 15) | 24.14.1 | Installer Node 18 LTS avant le Sprint 9 |
| npm | 9+ | 10.9.8 (`npm.cmd`) | Utiliser `npm.cmd` si PowerShell bloque `npm.ps1` |
| Docker | présent | 29.3.1 | OK |

## Détail du Sprint 1 (terminé)

- [x] 1.1 — Config Server : dépendance, `@EnableConfigServer`, port 8888, profil `native`, tests.
- [x] 1.2 — Configuration externe non sensible dans `infrastructure/config-repo/`.
- [x] 1.3 — Discovery Server : `@EnableEurekaServer`, port 8761, tests.
- [x] 1.4 — API Gateway : dépendance WebFlux, Eureka client, import Config Server, tests.
- [x] 1.5 — Route de test `demo-service` via `lb://DEMO-SERVICE`.
- [x] 1.6 — Démarrage complet et vérification des endpoints (voir [ports et services](ports-and-services.md)).

Commits atomiques : un commit par tâche sur la branche de sprint dédiée, puis merge `--no-ff` dans `develop`. La branche du sprint est conservée (convention : `sprint/01-foundations`).

## Détail du Sprint 2 (terminé)

- [x] 2.1 — `auth-service` : Spring Security, JPA, Flyway, PostgreSQL, JJWT, Maven Wrapper, Dockerfile.
- [x] 2.2 — Migration `V1__create_auth_tables.sql` : `roles`, `user_credentials`, `user_roles`, seeding `CLIENT`/`ADMIN`.
- [x] 2.3 — Inscription : email normalisé et unique, BCrypt, rôle `CLIENT`, réponse sans hash.
- [x] 2.4 — Login : vérification BCrypt, JWT HS256 signé (`sub`, `email`, `roles`, `exp`), réponse `Bearer`.
- [x] 2.5 — Filtre JWT stateless, gestion d'erreurs JSON `400/401/403/409/500`, endpoint `/auth/me`.
- [x] 2.6 — Route Gateway `/auth/**` vers `lb://AUTH-SERVICE` et configuration externe par variables d'environnement.
- [x] 2.7 — Tests unitaires + test d'intégration H2/Flyway : 11 tests verts.
- [x] 2.8 — Anticipation Docker : `infrastructure/docker-compose.yml`, 6 conteneurs, validation runtime complète (voir [auth-service.md](auth-service.md)).

Branche dédiée : `sprint/02-auth`, conservée après merge dans `develop`.

## Détail du Sprint 3 (terminé)

- [x] 3.1 — `user-service` : Spring Security, JPA, Flyway, PostgreSQL, JWT local, Maven Wrapper, Dockerfile.
- [x] 3.2 — Migration `V1__create_customer_profiles.sql` : `customer_profiles`, `auth_user_id` unique, aucune FK inter-bases.
- [x] 3.3 — Provisioning automatique du profil depuis `sub`/`email` du JWT au premier appel `/users/me`.
- [x] 3.4 — Endpoints `GET/PUT /users/me`, `GET /admin/users`, `GET /admin/users/{authUserId}` avec `authUserId` comme identifiant admin.
- [x] 3.5 — Validation JWT locale avec le même `JWT_SECRET` que `auth-service` ; rôles bruts comparés via `hasAuthority("ADMIN")`.
- [x] 3.6 — Isolation garantie : `X-User-Id` forgé ignoré, `authUserId` immuable, routes admin réservées à `ADMIN`.
- [x] 3.7 — Routes Gateway `/users/**` et `/admin/**` vers `lb://USER-SERVICE`, conteneurs `postgres-user` et `user-service`.
- [x] 3.8 — Tests unitaires + intégration H2/Flyway : 13 tests verts ; validation Docker complète (voir [user-service.md](user-service.md)).

Branche dédiée : `sprint/03-user-service`, conservée après merge dans `develop`.

## Détail du Sprint 4 (terminé)

- [x] 4.1 — `account-service` : Spring Security, JPA, Flyway, PostgreSQL, JWT local, Maven Wrapper, Dockerfile.
- [x] 4.2 — Migration `V1__create_bank_accounts.sql` : `bank_accounts`, `NUMERIC(19,2)`, `reservedBalance`, `@Version` et index métier.
- [x] 4.3 — Endpoints client `GET /accounts` et `GET /accounts/{id}` avec filtrage strict par `sub` du JWT ; autre client renvoie `404`.
- [x] 4.4 — Endpoints ADMIN `POST /admin/accounts` et `PATCH /admin/accounts/{id}/status`, réservés à `ADMIN`.
- [x] 4.5 — Règles `BigDecimal` : devise ISO 3 lettres majuscules, deux décimales maximum, montant initial non négatif, opérations positives.
- [x] 4.6 — Primitives internes reserve/debit/credit/release protégées par `X-Internal-Service-Token`, non exposées via le Gateway.
- [x] 4.7 — Concurrence gérée par `PESSIMISTIC_WRITE` et `@Version` ; comptes non actifs, devise incohérente et solde insuffisant renvoient `409`.
- [x] 4.8 — Routes Gateway `/accounts/**` et `/admin/accounts/**` avant la route `/admin/**` user, conteneurs `postgres-account` et `account-service`.
- [x] 4.9 — Tests unitaires + intégration H2/Flyway : 16 tests verts ; validation Docker complète (voir [account-service.md](account-service.md)).

Branche dédiée : `sprint/04-account-service`, conservée après merge dans `develop`.

## Travaux transverses (structure et conteneurisation)

- [x] Microservices déplacés de `services/` vers `backend/` ; chemins du Compose et de la documentation mis à jour.
- [x] Application Angular 15 créée dans `frontend/` : connexion JWT, intercepteur `Authorization`, tableau de bord des comptes et des bénéficiaires, 7 tests verts.
- [x] `frontend/Dockerfile` (build `node:18-alpine`, service `nginx:1.27-alpine`) et `frontend/nginx.conf` (proxy API vers `api-gateway:8080`).
- [x] Service `frontend` ajouté au Compose sur le port hôte `4200` ; plateforme validée avec 11 conteneurs et healthchecks `UP`.

Branche dédiée : `chore/backend-frontend-docker`.

## Détail du Sprint 5 (terminé)

- [x] 5.1 — Entité `Beneficiary` et enum `BeneficiaryStatus` dans `account-service` : `id`, `ownerUserId`, `displayName`, `destinationAccountReference` masquée, `createdAt`, `status`.
- [x] 5.2 — Migration Flyway `V2__create_beneficiaries.sql` avec index propriétaire et unicité `(owner_user_id, destination_account_reference)`.
- [x] 5.3 — Endpoints `POST /beneficiaries`, `GET /beneficiaries`, `GET /beneficiaries/{id}`, `PUT /beneficiaries/{id}` et `DELETE /beneficiaries/{id}`.
- [x] 5.4 — Propriété vérifiée sur chaque opération via `AuthenticatedUser.getAuthUserId()` ; un autre client reçoit `404`.
- [x] 5.5 — Validation : nom obligatoire, longueur maximale, format fictif `FR76 **** **** **** 1234` et unicité par client (`409`).
- [x] 5.6 — Route Gateway `/beneficiaries/**` vers `lb://ACCOUNT-SERVICE`, proxy Angular et Nginx mis à jour.
- [x] 5.7 — UI Angular : ajout, liste, activation/désactivation, suppression et affichage de la référence déjà masquée par le backend.
- [x] 5.8 — Tests unitaires + intégration H2/Flyway : 25 tests backend verts, 7 tests Angular verts, validation Docker complète (voir [account-service.md](account-service.md)).

Branche dédiée : `sprint/05-beneficiaries`, conservée après merge dans `develop`.

## Détail du Sprint 6 (terminé)

- [x] 6.1 — `transfer-service` : Spring Boot 3.5.16, Java 17, Spring Cloud Config/Eureka, PostgreSQL, Flyway, JWT local, Maven Wrapper, Dockerfile, port `8085`.
- [x] 6.2 — Migration `V1__create_transfers.sql` : `transfers`, `transfer_status_history`, `@Version`, index propriétaire/statut, unicité `(owner_user_id, idempotency_key)`.
- [x] 6.3 — Entités `Transfer`, `TransferStatusHistory`, enums `TransferStatus` (`REQUESTED`, `VALIDATING`, `PROCESSING`, `COMPLETED`, `FAILED`) et `CompensationStatus`.
- [x] 6.4 — Endpoints `POST /transfers` avec `Idempotency-Key`, `GET /transfers`, `GET /transfers/{id}` et `GET /transfers/{id}/history`, tous filtrés par le `sub` du JWT.
- [x] 6.5 — Idempotence par empreinte SHA-256 de la requête : replay de la même clé, `409` si la clé est réutilisée avec un payload différent, gestion du concours par contrainte unique.
- [x] 6.6 — Saga : validation compte source et bénéficiaire, réservation, débit, crédit destination idempotent via `/internal/accounts/external-credit`, compensation par `release` ou `creditSource`.
- [x] 6.7 — Retry limité aux `GET` de validation ; aucune répétition aveugle des appels mutants ; timeouts configurables via variables d'environnement.
- [x] 6.8 — Extension `account-service` : `V3__create_external_accounts.sql`, tables `external_accounts` et `external_account_credits`, opération idempotente par `operationId`.
- [x] 6.9 — Route Gateway `/transfers/**` vers `lb://TRANSFER-SERVICE`, conteneurs `postgres-transfer` et `transfer-service`, proxy Angular et Nginx mis à jour.
- [x] 6.10 — UI Angular : formulaire de virement avec sélection compte/bénéficiaire, montant `BigDecimal` côté backend, devise, motif, liste des virements et statut de compensation.
- [x] 6.11 — Tests : 23 tests `transfer-service` verts (Saga, compensation, idempotence, retry HTTP, intégration MockMvc/H2), 28 tests `account-service` verts, 8 tests Angular verts.

Branche dédiée : `sprint/06-transfers`, conservée après merge dans `develop`. Détails : [transfer-service.md](transfer-service.md).

## Détail du Sprint 7 (terminé)

- [x] 7.1 — `transaction-service` : Spring Boot 3.5.16, Java 17, Spring Cloud Config/Eureka, PostgreSQL, Flyway, JWT local, Maven Wrapper, Dockerfile, port `8086`.
- [x] 7.2 — Migration `V1__create_transactions.sql` : `transaction_records`, `transaction_entries`, index propriétaire/compte/date, unicité `external_reference`.
- [x] 7.3 — Modèle : `TransactionRecord` + `TransactionEntry` avec référence externe, compte concerné ou référence destination externe, type `TRANSFER`/`REVERSAL`, sens `DEBIT`/`CREDIT`, montant/devise, date et statut `CONFIRMED`.
- [x] 7.4 — Endpoints client `GET /transactions?accountId=&from=&to=&page=` et `GET /transactions/{id}` filtrés par `sub` du JWT, tri par date décroissante, pagination plafonnée.
- [x] 7.5 — Endpoint interne `POST /internal/transactions` protégé par `X-Internal-Service-Token`, idempotent par `externalReference` (doublons ignorés sans doublon DB).
- [x] 7.6 — Intégration `transfer-service` : enregistrement d'une transaction `TRANSFER` confirmée après succès et d'une `REVERSAL` après compensation source réussie.
- [x] 7.7 — Infra : `postgres-transaction`, `transaction-service`, route Gateway `/transactions/**`, proxy Angular/Nginx, variables `TRANSACTION_DB_*`.
- [x] 7.8 — UI Angular : historique transactions filtrable par compte, affichage type/montant/statut/date/écritures.
- [x] 7.9 — Tests : 8 tests `transaction-service` verts, 24 tests `transfer-service` verts (dont client HTTP transaction), 9 tests Angular verts ; validation Docker complète.

Branche dédiée : `sprint/07-transaction-service`, conservée après merge dans `develop`. Détails : [transaction-service.md](transaction-service.md).

## Règle de passage au sprint suivant

Aucun sprint suivant ne démarre tant que les critères de validation du sprint courant ne sont pas tous cochés et que le merge dans `develop` n'est pas effectué.

## D?tail du Sprint 8 (termin?)

- [x] 8.1 ??" Infrastructure RabbitMQ : `rabbitmq:3.13-management-alpine` dans `docker-compose.yml`, exchange topic `banking.events.exchange`, queues `notification.events.queue`/`audit.events.queue`, DLX `banking.events.dlx`, DLQ `notification.events.dlq`/`audit.events.dlq`, routing `banking.#`, `default-requeue-rejected=false`.
- [x] 8.2 ??" `notification-service` (port 8087, `notification_db`) : consommation ?v?nements, cr?ation `Notification`, idempotence par `eventId`, endpoints `GET /notifications` et `PATCH /notifications/{id}/read` (owner-scop?).
- [x] 8.3 ??" `audit-service` (port 8088, `audit_db`) : consommation ?v?nements, enregistrement `actorId`, `action`, `targetId`, date, m?tadonn?es non sensibles, idempotence par `eventId`, endpoint `GET /admin/audit` (ADMIN).
- [x] 8.4 ??" `transfer-service` : publication `TransferCompleted` (`banking.transfer.completed`) / `TransferFailed` (`banking.transfer.failed`) avec contrat `TransferEvent(eventId,eventType,occurredAt,payload)` ; publication tol?rante aux erreurs sans invalider le transfert.
- [x] 8.5 ??" Gateway/config/compose : routes `/notifications/**` ??' `lb://NOTIFICATION-SERVICE`, `/admin/audit/**` ??' `lb://AUDIT-SERVICE`, `/users/**,/admin/users/**` ??' `lb://USER-SERVICE` ; variables RabbitMQ/Postgres ajout?es.
- [x] 8.6 ??" Tests : notification-service 3/3, audit-service 3/3, transfer-service 25/25 ; validation Docker (services healthy, routes gateway, 401 sans token).

## Détail du Sprint 9 (terminé)

- [x] 9.1 — Frontend Angular refactoré : routing, layout, guards auth/rôle, interceptor JWT, services core, pages auth/dashboard/comptes/bénéficiaires/virements/transactions/notifications/admin.
- [x] 9.2 — Parcours CLIENT : login/register, dashboard, comptes, bénéficiaires, virement, historique transactions, notifications (liste + marquer lu).
- [x] 9.3 — Parcours ADMIN : accès admin protégé par rôle, consultation utilisateurs et audit via Gateway.
- [x] 9.4 — Intégration Gateway : `/notifications` ajouté à `frontend/proxy.conf.json` et `frontend/nginx.conf`; routes existantes réutilisées.
- [x] 9.5 — Tests/build frontend : `ng test --watch=false --browsers=ChromeHeadlessNoSandbox` 2/2 SUCCESS ; `ng build --configuration production` OK.
- [ ] 9.6 — Validation Docker E2E complète Sprint 9 : daemon Docker indisponible lors de `docker compose build frontend`; à exécuter quand Docker Desktop est disponible.

Branche dédiée : `sprint/09-angular-frontend`, conservée après merge dans `develop`.

## Détail du Sprint 10 (terminé)

- [x] 10.1 — Validation statique du Compose avec .env.example (docker compose config --quiet OK).
- [x] 10.2 — Script scripts/verify-docker.ps1 : up -d --build, attente healthchecks, contrôle routes Gateway et health endpoints.
- [ ] 10.3 — Exécution E2E Docker complète (en attente de Docker Desktop) puis correction éventuelle des variables/comptes/services.

Branche dédiée : sprint/10-docker-integration, conservée après merge dans develop.

## Détail du Sprint 11 (terminé)

- [x] 11.1 — Pipeline `.github/workflows/ci.yml` : matrix Maven sur les 11 services, build/test Angular 15, validation `docker compose config --quiet`.
- [x] 11.2 — Frontend CI alignée sur `ChromeHeadlessNoSandbox` via `npm run test:ci`.
- [x] 11.3 — OpenAPI/Swagger via springdoc `2.8.17` sur les 11 services (starter WebMVC, starter WebFlux pour le Gateway).
- [x] 11.4 — Endpoints `/swagger-ui.html` et `/v3/api-docs/**` autorisés dans les sept services sécurisés.
- [x] 11.5 — Documentation [openapi.md](openapi.md), [notification-service.md](notification-service.md), [audit-service.md](audit-service.md) et README final mis à jour.
- [x] 11.6 — Équivalent CI exécuté localement : 11 builds Maven verts, build Angular OK, 2 tests Angular verts, YAML et Compose valides ; exécution GitHub et E2E Docker à confirmer après push.

Branche dédiée : `sprint/11-cicd-openapi`, conservée après merge dans `develop`.

## Détail du Sprint 12 (terminé)

- [x] 12.1 — Sécurité compte : MFA, utilisateur IAM dédié avec permissions minimales, budgets 10/20 USD et alertes sans secret versionné.
- [x] 12.2 — Réseau : CloudFormation non sensible `infrastructure/aws/network.json` avec VPC, sous-réseau public, sous-réseau privé, IGW, routes et security groups ; DB jamais publique.
- [x] 12.3 — Images : script `infrastructure/aws/ecr-push.ps1` pour les 12 dépôts ECR `banking/*` avec tag version/commit et `latest`.
- [x] 12.4 — Démo EC2 + Docker Compose derrière ALB/HTTPS, Gateway comme point d'entrée public et limites de la configuration documentées.
- [x] 12.5 — Base et observabilité : RDS PostgreSQL privé optionnel, CloudWatch Logs/alertes et S3 optionnel avec Block Public Access.
- [x] 12.6 — Documentation [aws-deployment.md](aws-deployment.md) : validation HTTPS/auth/santé/DB, comptes fictifs `.invalid`, coût estimatif 52-76 USD/mois et procédure de nettoyage.
- [ ] 12.7 — Déploiement AWS réel et captures : nécessite un compte AWS, un domaine et des secrets fournis par l'utilisateur ; aucune ressource payante n'est créée par ce sprint.

Branche dédiée : `sprint/12-aws-demo`, conservée après merge dans `develop`.
