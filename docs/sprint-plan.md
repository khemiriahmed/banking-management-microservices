# Feuille de route des sprints

Objectif : un sprint traité par jour ouvré. Les sprints longs du cahier des charges sont découpés en livrables quotidiens ; un sprint n'est clos que lorsque ses critères de validation sont passés.

| Jour | Sprint | Résultat principal | Dépendance | Statut |
| --- | --- | --- | --- | --- |
| J1 | 0 — Préparation | Outils, dépôt, structure, conventions Git | — | Terminé |
| J2 | 1 — Fondations Spring Cloud | Config Server + Eureka + Gateway | S0 | Terminé |
| J3 | 2 — Auth | Inscription, login, JWT via Gateway | S1 | Terminé |
| J4 | 3 — Users | Profil client sécurisé, rôles | S2 | Terminé |
| J5 | 4 — Accounts | Comptes, soldes `BigDecimal`, statuts | S3 | Terminé |
| J6 | 5 — Beneficiaries | CRUD bénéficiaires isolés par client | S4 | À faire |
| J7-8 | 6 — Transfers | Saga, compensation, idempotence | S4-S5 | À faire |
| J9 | 7 — Transactions | Historique paginé et sécurisé | S6 | À faire |
| J10 | 8 — Events | RabbitMQ, notifications, audit | S6-S7 | À faire |
| J11-12 | 9 — Angular | Parcours CLIENT et ADMIN complets | APIs stables | À faire |
| J13 | 10 — Docker | `docker compose up --build` | S9 | Anticipé en S2/S3 |
| J14 | 11 — CI/CD | GitHub Actions, OpenAPI, README final | S10 | À faire |
| J15 | 12 — AWS | Démo sécurisée, coûts documentés | S11 | À faire |

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
- [x] Application Angular 15 créée dans `frontend/` : connexion JWT, intercepteur `Authorization`, tableau de bord des comptes, 3 tests verts.
- [x] `frontend/Dockerfile` (build `node:18-alpine`, service `nginx:1.27-alpine`) et `frontend/nginx.conf` (proxy API vers `api-gateway:8080`).
- [x] Service `frontend` ajouté au Compose sur le port hôte `4200` ; plateforme validée avec 11 conteneurs et healthchecks `UP`.

Branche dédiée : `chore/backend-frontend-docker`.

## Règle de passage au sprint suivant

Aucun sprint suivant ne démarre tant que les critères de validation du sprint courant ne sont pas tous cochés et que le merge dans `develop` n'est pas effectué.
