# Account Service — Sprint 4

Port : `8084` (direct) / `8080/accounts/**` et `8080/admin/accounts/**` via l'API Gateway. Base dédiée : `account_db`, exposée localement sur `5434`.

## Responsabilités

- Créer et gérer des comptes bancaires fictifs avec `ownerUserId` issu du JWT, jamais d'un en-tête ou d'un corps de requête forgé par le client.
- Valider localement le JWT HS256 émis par `auth-service` ; le Gateway ne fait que transmettre `Authorization`.
- Isoler les lectures client : un compte appartenant à un autre utilisateur renvoie `404` pour éviter l'énumération.
- Réserver les opérations d'administration (création de compte, changement de statut) au rôle `ADMIN`.
- Préparer les primitives internes utilisées plus tard par `transfer-service` : réservation, débit, crédit et libération.
- Migration Flyway `V1__create_bank_accounts.sql` : `bank_accounts(id, owner_user_id, account_number_masked, account_type, currency, status, balance, reserved_balance, version, created_at)` avec index sur `owner_user_id` et `status`.
- Aucune clé étrangère SQL inter-bases : `ownerUserId` est un UUID issu du domaine d'authentification.

## Règles métier

- Types de compte : `CHECKING`, `SAVINGS`. Statuts : `ACTIVE`, `SUSPENDED`, `CLOSED`.
- Devise ISO 3 lettres majuscules ; solde initial `>= 0`, opérations `> 0`, au maximum deux décimales.
- Tous les montants utilisent `BigDecimal` mappé en `NUMERIC(19,2)` ; aucun `float` ni `double`.
- Solde disponible : `balance - reservedBalance`. `reservedBalance` n'est jamais exposé au client.
- `reserve` vérifie le solde disponible ; `debit` consomme la réservation ; `credit` augmente le solde ; `release` restitue une réservation.
- Concurrence : verrou pessimiste (`PESSIMISTIC_WRITE`) et `@Version` pour détecter les mises à jour simultanées.
- Un compte non `ACTIVE`, une devise différente ou un solde insuffisant renvoie `409`.

## Endpoints

| Méthode | Chemin | Accès | Réponse |
| --- | --- | --- | --- |
| `GET` | `/accounts` | JWT `CLIENT` ou `ADMIN` | `200` + comptes du `sub` du JWT |
| `GET` | `/accounts/{id}` | JWT `CLIENT` ou `ADMIN` | `200` + compte du `sub`, `404` sinon |
| `POST` | `/admin/accounts` | JWT `ADMIN` | `201` + compte créé |
| `PATCH` | `/admin/accounts/{id}/status` | JWT `ADMIN` | `200` + compte mis à jour |
| `POST` | `/internal/accounts/{id}/reserve` | `X-Internal-Service-Token` | `200` + soldes internes |
| `POST` | `/internal/accounts/{id}/debit` | `X-Internal-Service-Token` | `200` + soldes internes |
| `POST` | `/internal/accounts/{id}/credit` | `X-Internal-Service-Token` | `200` + soldes internes |
| `POST` | `/internal/accounts/{id}/release` | `X-Internal-Service-Token` | `200` + soldes internes |
| `GET` | `/actuator/health` | public | `UP` |

Les routes `/internal/**` ne sont pas exposées par le Gateway : elles sont accessibles uniquement depuis le réseau interne via le port `8084`. Le corps des opérations internes contient `amount` et `currency`.

Codes d'erreur JSON : `400` validation/montant invalide, `401` JWT ou token interne absent/invalide, `403` rôle insuffisant, `404` compte introuvable ou autre client, `409` conflit métier/concurrence, `500` erreur inattendue.

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
$env:JAVA_HOME='C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\jdk-17.0.21+9'; $env:PATH="$env:JAVA_HOME\bin;C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\apache-maven-3.9.16\bin;$env:PATH"; mvn -B -f services/account-service/pom.xml test
```

Couverture : `JwtTokenVerifierTest` (claims, expiration, subject invalide), `AccountServiceTest` (règles de solde, réservation, débit, crédit, libération, précision), `AccountServiceIntegrationTest` (isolation des clients, rôles, statut suspendu, token interne, BigDecimal et concurrence, H2 + Flyway). 16 tests verts.

## Validation Docker effectuée

```powershell
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

Résultats le 22/11/2026 : 10 conteneurs `Up`, healths `UP` pour les 9 conteneurs instrumentés, tables `bank_accounts` et `flyway_schema_history` dans `account_db`, routes Gateway `auth-service,demo-service,account-service,user-service`, Eureka `ACCOUNT-SERVICE`. Parcours validé : client sans compte `200`, accès ADMIN refusé pour un `CLIENT` `403`, solde initial négatif `400`, création ADMIN `201`, numéro masqué, `reservedBalance` absent de la réponse client, lecture du propre compte `200`, compte d'un autre client `404`, précision à 3 décimales `400`, token interne absent `401`, reserve/debit/credit `200`, libération après débit `409`, route interne via Gateway `404`, suspension ADMIN `200`, réservation sur compte suspendu `409`, solde final `949.75`.
