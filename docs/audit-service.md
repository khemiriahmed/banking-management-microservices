# Audit Service

Service Spring Boot 3.5.16 / Java 17 dédié à la piste d'audit, ajouté au Sprint 8.

## Responsabilités

- Consommer les événements RabbitMQ du exchange `banking.events.exchange` via la queue `audit.events.queue` avec routing `banking.#`.
- Persister chaque `eventId` une seule fois dans `audit_db`.
- Enregistrer `actorId`, `action`, `targetId`, date et métadonnées non sensibles.

## Endpoints

| Méthode | Endpoint | Rôle | Description |
| --- | --- | --- | --- |
| `GET` | `/admin/audit` | `ADMIN` | Liste paginée des événements d'audit. |
| `GET` | `/actuator/health` | public | Healthcheck Docker. |

Les endpoints OpenAPI `/swagger-ui.html` et `/v3/api-docs/**` sont également publics.

## Configuration

- Port : `8088`.
- Base : PostgreSQL `audit_db` (variables `AUDIT_DB_*`).
- RabbitMQ : `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`.
- Conteneurs : `postgres-audit`, `audit-service`.
- Route Gateway : `/admin/audit/**` vers `lb://AUDIT-SERVICE`.
- Files DLX/DLQ : `banking.events.dlx` / `audit.events.dlq` ; `default-requeue-rejected=false`.

## Validation

- Tests `mvnw test` : 3/3 verts.
- Validation statique Docker et healthcheck documentée dans [ports-and-services.md](ports-and-services.md).
