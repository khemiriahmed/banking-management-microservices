# Notification Service

Service Spring Boot 3.5.16 / Java 17 dédié aux notifications client, ajouté au Sprint 8.

## Responsabilités

- Consommer les événements RabbitMQ du exchange `banking.events.exchange` via la queue `notification.events.queue` avec routing `banking.#`.
- Persister une `Notification` par `eventId` (idempotence) dans `notification_db`.
- Exposer les notifications au client connecté, filtrées par le `sub` du JWT.

## Endpoints

| Méthode | Endpoint | Rôle | Description |
| --- | --- | --- | --- |
| `GET` | `/notifications` | `CLIENT`, `ADMIN` | Liste des notifications du propriétaire, triées par date décroissante. |
| `PATCH` | `/notifications/{id}/read` | `CLIENT`, `ADMIN` | Marque une notification comme lue ; `404` si elle appartient à un autre utilisateur. |
| `GET` | `/actuator/health` | public | Healthcheck Docker. |

Les endpoints OpenAPI `/swagger-ui.html` et `/v3/api-docs/**` sont également publics.

## Configuration

- Port : `8087`.
- Base : PostgreSQL `notification_db` (variables `NOTIFICATION_DB_*`).
- RabbitMQ : `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`.
- Conteneurs : `postgres-notification`, `notification-service`.
- Route Gateway : `/notifications/**` vers `lb://NOTIFICATION-SERVICE`.
- Files DLX/DLQ : `banking.events.dlx` / `notification.events.dlq` ; `default-requeue-rejected=false`.

## Validation

- Tests `mvnw test` : 3/3 verts.
- Validation statique Docker et healthcheck documentée dans [ports-and-services.md](ports-and-services.md).
