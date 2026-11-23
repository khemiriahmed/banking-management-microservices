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

## Règle de passage au sprint suivant

Aucun sprint suivant ne démarre tant que les critères de validation du sprint courant ne sont pas tous cochés et que le merge dans `develop` n'est pas effectué.
