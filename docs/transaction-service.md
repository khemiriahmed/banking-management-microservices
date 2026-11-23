# Transaction Service — Sprint 7

Port : `8086` (direct) / `8080/transactions/**` via l'API Gateway. Base dédiée : `transaction_db`, exposée localement sur `5436`. Le service conserve l'historique métier des virements confirmés et des compensations.

## Responsabilités

- Enregistrer des écritures de transaction à partir d'appels internes idempotents.
- Exposer l'historique paginé et filtré aux clients authentifiés.
- Garantir que seul le propriétaire (`sub` du JWT) peut consulter ses transactions.
- Ne jamais exposer `/internal/**` via l'API Gateway.

## Modèle

`V1__create_transactions.sql` crée :

- `transaction_records` : `id`, `external_reference` unique, `transfer_id`, `owner_user_id`, `type` (`TRANSFER`/`REVERSAL`), `amount` `NUMERIC(19,2)`, `currency`, `occurred_at`, `status` (`CONFIRMED`), `created_at`.
- `transaction_entries` : `id`, `record_id`, `account_id` nullable, `destination_reference` nullable, `side` (`DEBIT`/`CREDIT`), `amount`, `currency`.
- Contraintes : une écriture porte exactement un `account_id` ou une `destination_reference` ; index sur propriétaire/date, compte/date et référence destination.

## Endpoints

| Méthode | Chemin | Accès | Réponse |
| --- | --- | --- | --- |
| `GET` | `/transactions?accountId=&from=&to=&page=` | JWT | `200` + page triée par `occurredAt desc`, `size` plafonné à 50 |
| `GET` | `/transactions/{id}` | JWT | `200` + transaction du `sub`, `404` sinon |
| `POST` | `/internal/transactions` | `X-Internal-Service-Token` | `200` + transaction créée ou rejouée, `409` si même `externalReference` avec corps différent |
| `GET` | `/actuator/health` | public | `UP` |

Corps interne :

```json
{
  "externalReference": "transfer:...",
  "transferId": "...",
  "ownerUserId": "...",
  "type": "TRANSFER",
  "amount": 25.00,
  "currency": "EUR",
  "occurredAt": "2026-11-23T10:00:00Z",
  "entries": [
    { "accountId": "...", "side": "DEBIT", "amount": 25.00, "currency": "EUR" },
    { "destinationReference": "FR76 **** **** **** 1234", "side": "CREDIT", "amount": 25.00, "currency": "EUR" }
  ]
}
```

## Idempotence et sécurité

- `externalReference` est unique : un doublon avec le même corps renvoie l'enregistrement existant ; un corps différent renvoie `409`.
- `transfer-service` utilise `transfer:<id>` pour un virement confirmé et `transfer:<id>:reversal` pour une compensation.
- L'identité consultée provient du claim `sub` du JWT. Un autre client reçoit `404`.
- Le Gateway expose uniquement `/transactions/**` ; `/internal/transactions` reste interne.

## Configuration

| Variable | Usage | Exemple |
| --- | --- | --- |
| `TRANSACTION_DB_URL` | JDBC PostgreSQL | `jdbc:postgresql://postgres-transaction:5432/transaction_db` |
| `TRANSACTION_DB_USER` | Utilisateur DB | `transaction_user` |
| `TRANSACTION_DB_PASSWORD` | Mot de passe DB | défini dans `infrastructure/.env` |
| `TRANSACTION_SERVICE_PORT` | Port du service | `8086` |
| `JWT_SECRET` | Même clé HMAC encodée en Base64 que `auth-service` | défini dans `infrastructure/.env` |
| `INTERNAL_SERVICE_TOKEN` | Secret partagé pour `/internal/**` | défini dans `infrastructure/.env` |
| `CONFIG_SERVER_URL` | Config Server | `http://config-server:8888` |
| `EUREKA_URI` | Eureka | `http://discovery-server:8761/eureka/` |

## Tests

```powershell
$env:JAVA_HOME='C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\jdk-17.0.21+9'; $env:PATH="$env:JAVA_HOME\bin;C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\apache-maven-3.9.16\bin;$env:PATH"; mvn -B -f backend/transaction-service/pom.xml test
```

8 tests verts : idempotence/conflit/payload interne, pagination/tri/filtres/date, isolation client `404`, intégration MockMvc/H2/Flyway, sécurité JWT/token interne.

## Validation Docker

La plateforme passe à 15 conteneurs : services précédents, `postgres-transaction` et `transaction-service`. Parcours validés via Gateway : virement confirmé visible dans `/transactions`, doublon événement interne sans duplication, consultation autre client `404`, filtre par compte, pagination/tri par date. Détails : [ports-and-services.md](ports-and-services.md).
