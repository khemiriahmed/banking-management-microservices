# Account Service — Sprint 5

Port : `8084` (direct) / `8080/accounts/**`, `8080/admin/accounts/**` et `8080/beneficiaries/**` via l'API Gateway. Base dédiée : `account_db`, exposée localement sur `5434`.

## Responsabilités

- Créer et gérer des comptes bancaires fictifs avec `ownerUserId` issu du JWT, jamais d'un en-tête ou d'un corps de requête forgé par le client.
- Enregistrer, consulter, modifier et supprimer les bénéficiaires du client authentifié, sans jamais faire confiance à un `ownerUserId` envoyé par le frontend.
- Valider localement le JWT HS256 émis par `auth-service` ; le Gateway ne fait que transmettre `Authorization`.
- Isoler les lectures client : un compte ou un bénéficiaire appartenant à un autre utilisateur renvoie `404` pour éviter l'énumération.
- Réserver les opérations d'administration (création de compte, changement de statut) au rôle `ADMIN`.
- Préparer les primitives internes utilisées plus tard par `transfer-service` : réservation, débit, crédit et libération.
- Migration Flyway `V1__create_bank_accounts.sql` : `bank_accounts(id, owner_user_id, account_number_masked, account_type, currency, status, balance, reserved_balance, version, created_at)` avec index sur `owner_user_id` et `status`.
- Migration Flyway `V2__create_beneficiaries.sql` : `beneficiaries(id, owner_user_id, display_name, destination_account_reference, status, created_at)`, index sur `owner_user_id` et contrainte d'unicité `(owner_user_id, destination_account_reference)`.
- Aucune clé étrangère SQL inter-bases : `ownerUserId` est un UUID issu du domaine d'authentification.

## Règles métier

- Types de compte : `CHECKING`, `SAVINGS`. Statuts : `ACTIVE`, `SUSPENDED`, `CLOSED`.
- Devise ISO 3 lettres majuscules ; solde initial `>= 0`, opérations `> 0`, au maximum deux décimales.
- Tous les montants utilisent `BigDecimal` mappé en `NUMERIC(19,2)` ; aucun `float` ni `double`.
- Solde disponible : `balance - reservedBalance`. `reservedBalance` n'est jamais exposé au client.
- `reserve` vérifie le solde disponible ; `debit` consomme la réservation ; `credit` augmente le solde ; `release` restitue une réservation.
- Concurrence : verrou pessimiste (`PESSIMISTIC_WRITE`) et `@Version` pour détecter les mises à jour simultanées.
- Un compte non `ACTIVE`, une devise différente ou un solde insuffisant renvoie `409`.
- Bénéficiaires : `displayName` obligatoire (100 caractères maximum), référence destination déjà masquée au format fictif `FR76 **** **** **** 1234`, statuts `ACTIVE`/`INACTIVE`, unicité de la référence par client.
- Chaque lecture, mise à jour et suppression de bénéficiaire est filtrée par `AuthenticatedUser.getAuthUserId()` ; un autre client reçoit `404`.

## Endpoints

| Méthode | Chemin | Accès | Réponse |
| --- | --- | --- | --- |
| `GET` | `/accounts` | JWT `CLIENT` ou `ADMIN` | `200` + comptes du `sub` du JWT |
| `GET` | `/accounts/{id}` | JWT `CLIENT` ou `ADMIN` | `200` + compte du `sub`, `404` sinon |
| `POST` | `/admin/accounts` | JWT `ADMIN` | `201` + compte créé |
| `PATCH` | `/admin/accounts/{id}/status` | JWT `ADMIN` | `200` + compte mis à jour |
| `GET` | `/beneficiaries` | JWT requis | `200` + bénéficiaires du `sub` |
| `GET` | `/beneficiaries/{id}` | JWT requis | `200` + bénéficiaire du `sub`, `404` sinon |
| `POST` | `/beneficiaries` | JWT requis | `201` + bénéficiaire créé, référence masquée |
| `PUT` | `/beneficiaries/{id}` | JWT requis | `200` + bénéficiaire mis à jour, `404` sinon |
| `DELETE` | `/beneficiaries/{id}` | JWT requis | `204`, `404` si autre client |
| `POST` | `/internal/accounts/{id}/reserve` | `X-Internal-Service-Token` | `200` + soldes internes |
| `POST` | `/internal/accounts/{id}/debit` | `X-Internal-Service-Token` | `200` + soldes internes |
| `POST` | `/internal/accounts/{id}/credit` | `X-Internal-Service-Token` | `200` + soldes internes |
| `POST` | `/internal/accounts/{id}/release` | `X-Internal-Service-Token` | `200` + soldes internes |
| `GET` | `/actuator/health` | public | `UP` |

Les routes `/internal/**` ne sont pas exposées par le Gateway : elles sont accessibles uniquement depuis le réseau interne via le port `8084`. Le corps des opérations internes contient `amount` et `currency`.

Codes d'erreur JSON : `400` validation/montant/référence invalide, `401` JWT ou token interne absent/invalide, `403` rôle insuffisant, `404` compte ou bénéficiaire introuvable ou autre client, `409` conflit métier/concurrence/doublon, `500` erreur inattendue.

## Configuration

| Variable | Usage | Exemple |
| --- | --- | --- |
| `ACCOUNT_DB_URL` | JDBC PostgreSQL | `jdbc:postgresql://postgres-account:5432/account_db` |
| `ACCOUNT_DB_NAME` | Nom de la base logique | `account_db` |
| `ACCOUNT_DB_USER` | Utilisateur DB | `account_user` |
| `ACCOUNT_DB_PASSWORD` | Mot de passe DB | défini dans `infrastructure/.env` |
| `JWT_SECRET` | Même clé HMAC encodée en Base64 que `auth-service` | défini dans `infrastructure/.env` |
| `INTERNAL_SERVICE_TOKEN` | Secret partagé pour les appels `/internal/**` | défini dans `infrastructure/.env` |
| `ACCOUNT_SERVICE_PORT` | Port du service | `8084` |
| `CONFIG_SERVER_URL` | Config Server | `http://config-server:8888` |
| `EUREKA_URI` | Eureka | `http://discovery-server:8761/eureka/` |

`infrastructure/.env.example` contient la matrice des variables. `infrastructure/.env` est ignoré par Git.

## Tests

```powershell
$env:JAVA_HOME='C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\jdk-17.0.21+9'; $env:PATH="$env:JAVA_HOME\bin;C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\apache-maven-3.9.16\bin;$env:PATH"; mvn -B -f backend/account-service/pom.xml test
```

Couverture : `JwtTokenVerifierTest` (claims, expiration, subject invalide), `AccountServiceTest` et `AccountServiceIntegrationTest` (Sprint 4), `BeneficiaryServiceTest` (validation, unicité, propriété, mapping) et `BeneficiaryIntegrationTest` (CRUD via MockMvc, isolation entre clients, référence masquée, doublon `409`, entrées invalides `400`, H2 + Flyway). 25 tests verts.

Les tests Angular correspondants (`BeneficiaryService`, `AppComponent`) sont exécutés dans `node:18-alpine` avec Chromium headless : 7 tests verts.

## Validation Docker effectuée

```powershell
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env config --quiet
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

Résultats le 22/11/2026 : 11 conteneurs `Up`, healths `UP` pour les conteneurs instrumentés, tables `bank_accounts`, `beneficiaries` et `flyway_schema_history` dans `account_db`, route Gateway `account-service` avec prédicat `/accounts/**,/admin/accounts/**,/beneficiaries/**`, Eureka `ACCOUNT-SERVICE`. Parcours bénéficiaires validé via `http://localhost:8080` : inscription de deux clients `201`, création `201` avec référence masquée et statut `ACTIVE`, liste `200`, doublon `409`, référence non masquée `400`, lecture par un autre client `404`, mise à jour `200` avec statut `INACTIVE`, suppression `204`, lecture après suppression `404`. Frontend `http://localhost:4200/` `200` et proxy `/auth/me` sans token `401`.
