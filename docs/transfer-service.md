# Transfer Service — Sprint 6

Port : `8085` (direct) / `8080/transfers/**` via l'API Gateway. Base dédiée : `transfer_db`, exposée localement sur `5435`. Le service orchestre un virement fictif et n'effectue aucun paiement réel.

## Responsabilités

- Créer un virement à partir d'un compte source et d'un bénéficiaire appartenant au client authentifié.
- Valider localement le JWT HS256 ; l'identité du propriétaire est toujours le claim `sub`, jamais le corps de requête.
- Exécuter une Saga synchrone avec compensation : validation, réservation, débit, crédit destination, puis compensation si nécessaire.
- Garantir l'idempotence par `(ownerUserId, Idempotency-Key)` : même clé et même corps renvoient la même réponse ; même clé avec un corps différent renvoie `409`.
- Conserver chaque transition de statut dans `transfer_status_history`.
- Appeler `account-service` uniquement sur le réseau interne via `X-Internal-Service-Token` ; ces routes ne sont pas exposées par le Gateway.

## Migration et modèle

`V1__create_transfers.sql` crée :

- `transfers` : identifiant, propriétaire, compte source, bénéficiaire, référence destination, montant `NUMERIC(19,2)`, devise, motif, statut, clé d'idempotence, empreinte SHA-256, raison d'échec, statut de compensation, horodatages, `version` et contrainte unique `(owner_user_id, idempotency_key)`.
- `transfer_status_history` : statut source, statut cible, motif et horodatage de chaque transition.
- Index sur `(owner_user_id, created_at DESC)` et sur `status`.

`account-service` reçoit la migration `V3__create_external_accounts.sql` avec `external_accounts` et `external_account_credits`. Le crédit destination est idempotent par `operationId` : un nouvel essai avec le même identifiant ne crédite pas deux fois.

## Saga

| Étape | Appel account-service | En cas d'échec |
| --- | --- | --- |
| Validation | `GET /accounts/{id}`, `GET /beneficiaries/{id}` avec `Authorization` | statut `FAILED`, compensation `NOT_REQUIRED` |
| Réservation | `POST /internal/accounts/{id}/reserve` | statut `FAILED`, compensation `NOT_REQUIRED` |
| Débit | `POST /internal/accounts/{id}/debit` | `POST /internal/accounts/{id}/release`, puis `FAILED` |
| Crédit destination | `POST /internal/accounts/external-credit` | `POST /internal/accounts/{id}/credit`, puis `FAILED` |
| Succès | aucun appel supplémentaire | statut `COMPLETED`, compensation `NOT_REQUIRED` |

Statuts : `REQUESTED` → `VALIDATING` → `PROCESSING` → `COMPLETED` ou `FAILED`. Les statuts terminaux sont immuables.

Les retries sont limités aux `GET` de validation. Les opérations mutantes ne sont jamais rejouées aveuglément : les identifiants d'opération et l'idempotence de `account-service` protègent les effets de bord. Timeouts et tentatives sont configurables.

## Endpoints

| Méthode | Chemin | Accès | Réponse |
| --- | --- | --- | --- |
| `POST` | `/transfers` | JWT + `Idempotency-Key` | `201` + virement, `409` si clé réutilisée avec un autre corps |
| `GET` | `/transfers?page=0&size=20` | JWT | `200` + page paginée, `size` plafonné à 50 |
| `GET` | `/transfers/{id}` | JWT | `200` + virement du `sub`, `404` sinon |
| `GET` | `/transfers/{id}/history` | JWT | `200` + transitions du virement |
| `GET` | `/actuator/health` | public | `UP` |

Corps de création :

```json
{
  "sourceAccountId": "e4d4f0c8-0b1e-4f6a-9e7a-1b6f0f8d6c11",
  "beneficiaryId": "d0e1f2a3-4b5c-4d6e-8f90-1a2b3c4d5e6f",
  "amount": 25.00,
  "currency": "EUR",
  "reason": "facture-42"
}
```

Codes d'erreur JSON : `400` validation/montant/en-tête invalide, `401` JWT absent ou invalide, `403` accès refusé, `404` virement introuvable ou autre client, `409` conflit d'idempotence ou concurrence, `500` erreur inattendue. Un échec métier de la Saga renvoie `201` avec un virement au statut `FAILED` et un `failureReason`, car l'opération a bien été enregistrée et tracée.

## Configuration

| Variable | Usage | Exemple |
| --- | --- | --- |
| `TRANSFER_DB_URL` | JDBC PostgreSQL | `jdbc:postgresql://postgres-transfer:5432/transfer_db` |
| `TRANSFER_DB_USER` | Utilisateur DB | `transfer_user` |
| `TRANSFER_DB_PASSWORD` | Mot de passe DB | défini dans `infrastructure/.env` |
| `TRANSFER_SERVICE_PORT` | Port du service | `8085` |
| `JWT_SECRET` | Même clé HMAC encodée en Base64 que `auth-service` | défini dans `infrastructure/.env` |
| `INTERNAL_SERVICE_TOKEN` | Secret partagé pour les appels `/internal/**` | défini dans `infrastructure/.env` |
| `ACCOUNT_SERVICE_URL` | Base URL interne de `account-service` | `http://account-service:8084` |
| `ACCOUNT_CONNECT_TIMEOUT_MS` | Timeout de connexion | `2000` |
| `ACCOUNT_READ_TIMEOUT_MS` | Timeout de lecture | `3000` |
| `ACCOUNT_RETRY_ATTEMPTS` | Tentatives sur les `GET` | `2` |
| `ACCOUNT_RETRY_DELAY_MS` | Délai entre tentatives | `100` |
| `CONFIG_SERVER_URL` | Config Server | `http://config-server:8888` |
| `EUREKA_URI` | Eureka | `http://discovery-server:8761/eureka/` |

## Tests

```powershell
$env:JAVA_HOME='C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\jdk-17.0.21+9'; $env:PATH="$env:JAVA_HOME\bin;C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\apache-maven-3.9.16\bin;$env:PATH"; mvn -B -f backend/transfer-service/pom.xml test
```

23 tests verts : orchestration Saga et compensations, idempotence et conflits, retry HTTP sans rejeu des mutations, intégration MockMvc/H2/Flyway, sécurité JWT, validation des entrées. Les 28 tests de `account-service` couvrent le crédit externe idempotent. Les 8 tests Angular couvrent le formulaire de virement et l'en-tête `Idempotency-Key`.

## Validation Docker

```powershell
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env config --quiet
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

La plateforme passe à 13 conteneurs : les services précédents, `postgres-transfer` et `transfer-service`. Résultats du 23/11/2026 : 13 conteneurs `Up`, tables `transfers`, `transfer_status_history`, `external_accounts` et `external_account_credits` présentes, route Gateway `/transfers/**` active, `TRANSFER-SERVICE` enregistré dans Eureka. Parcours validés via le Gateway : création `201`/`COMPLETED`, replay idempotent avec le même identifiant, conflit de clé `409`, solde insuffisant `FAILED`, bénéficiaire inactif `FAILED`, échec de crédit avec compensation `COMPLETED`, `401` sans JWT, `400` pour clé manquante ou montant négatif, liste/détail/historique `200` avec 4 transitions. Frontend `200` et proxy `/transfers` sans token `401`. Vérifications détaillées : [ports-and-services.md](ports-and-services.md).
