# Banking Management System — AhmedKH

Application web bancaire **fictive** à but pédagogique : inscription, authentification, comptes, bénéficiaires, virements simulés, historique, notifications et audit. Aucun paiement réel, aucune donnée personnelle réelle.

## Stack imposée

| Domaine | Choix |
| --- | --- |
| Backend | Java 17, Spring Boot 3, Spring Security, Spring Data JPA, Hibernate, Bean Validation |
| Microservices | Spring Cloud Gateway, Eureka Discovery Server, Spring Cloud Config |
| Frontend | Angular 15, TypeScript, RxJS |
| Données | PostgreSQL + Flyway, une base logique par service |
| Messagerie | RabbitMQ à partir du sprint 8 |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers, tests Angular |
| DevOps | Docker, Docker Compose, GitHub Actions |
| Cloud | AWS IAM/MFA/Budgets, VPC, EC2, ECR, RDS, S3, CloudWatch |

## Architecture cible

```mermaid
flowchart TD
    UI[Angular 15] -->|HTTPS / REST / JWT| GW[API Gateway]
    GW --> DS[Eureka Discovery]
    GW --> CS[Config Server]
    GW --> AUTH[Auth Service]
    GW --> USER[User Service]
    GW --> ACC[Account Service]
    GW --> TRF[Transfer Service]
    GW --> TX[Transaction Service]
    GW --> NOTIF[Notification Service]
    GW --> AUDIT[Audit Service]
    TRF -->|événements| MQ[(RabbitMQ)]
    MQ --> NOTIF
    MQ --> AUDIT
    AUTH --> DB1[(auth_db)]
    USER --> DB2[(user_db)]
    ACC --> DB3[(account_db)]
    TRF --> DB4[(transfer_db)]
    TX --> DB5[(transaction_db)]
```

Règles fortes : chaque service a sa propre base, le Gateway n'est pas une couche de sécurité suffisante, les montants utilisent `BigDecimal`, le frontend n'est jamais source de vérité.

## Structure du dépôt

```text
.
├── frontend/                 # Angular 15
├── services/
│   ├── config-server/
│   ├── discovery-server/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── user-service/
│   ├── account-service/
│   ├── transfer-service/
│   ├── transaction-service/
│   ├── notification-service/
│   └── audit-service/
├── infrastructure/           # Docker, AWS, scripts
├── docs/                     # cahier des charges, diagrammes, API, guides
├── .github/workflows/        # CI GitHub Actions
├── docker-compose.yml        # Sprint 10
└── README.md
```

Package Java de base : `com.ahmedkh.banking` (ex. `com.ahmedkh.banking.account`).

## Git — travail quotidien

Branches : `main` (releases), `develop` (intégration), `feature/sprint-XX-slug`, `fix/...`, `docs/...`, `chore/...`.

```powershell
git switch develop
git pull origin develop
git switch -c feature/sprint-01-foundations
# une tâche = un commit
git commit -m "feat(gateway): add test route"
git switch develop
git merge --no-ff feature/sprint-01-foundations -m "Merge feature/sprint-01-foundations into develop"
git branch -d feature/sprint-01-foundations
```

Messages : `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `build`, `ci`. Détail complet : [docs/git-workflow.md](docs/git-workflow.md).

## Prérequis (à valider avec la tâche 0.1)

- **JDK 17** : `java -version` doit afficher 17 (attention : une JDK 26 est installée par défaut sur la machine).
- **Maven 3.9+** : `mvn -version`.
- **Node 18.x** : Angular 15 ne prend pas en charge Node 24 installé actuellement.
- **Docker** : `docker --version`.
- Sur PowerShell Windows, si `npm` est bloqué par la politique d'exécution, utiliser `npm.cmd` ou `cmd /c npm`.

## Sprints

Feuille de route détaillée : [docs/sprint-plan.md](docs/sprint-plan.md). Statut actuel : **Sprint 0 en cours**.

## Sécurité et qualité

- Frontend jamais source de vérité pour rôles, propriété d'un compte ou montant.
- Chaque service valide ses propres autorisations.
- Aucune base partagée entre services, aucun secret commité, aucun token dans les logs.
- `BigDecimal` uniquement pour l'argent, Saga + idempotence pour les virements.

Voir aussi : [architecture](docs/architecture-overview.md) · [cahier des charges PDF](docs/Cahier_des_charges_Banking_Microservices_AhmedKH_v2_Sprints_Detailles.pdf).
