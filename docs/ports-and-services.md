# Ports et services — Sprint 2

## Versions retenues

| Composant | Version |
| --- | --- |
| Java | 17 (Temurin 17.0.21) |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 (Northfields) |
| Maven | 3.9.16 (Maven Wrapper fourni par service) |
| JJWT | 0.13.0 |
| PostgreSQL | 16-alpine (container Docker `banking-postgres-auth`) |

## Services locaux

| Service | Artifact | Port hôte | URL de base | Rôle |
| --- | --- | --- | --- | --- |
| Config Server | `config-server` | 8888 | `http://localhost:8888` | Configuration non sensible, profil `native`, dépôt monté en `/config-repo` |
| Discovery Server | `discovery-server` | 8761 | `http://localhost:8761` | Tableau de bord Eureka |
| API Gateway | `api-gateway` | 8080 | `http://localhost:8080` | Point d'entrée unique, routes `lb://` |
| Demo Service | `demo-service` | 8081 | `http://localhost:8081` | Service temporaire de validation du routage |
| Auth Service | `auth-service` | 8082 | `http://localhost:8082` | Inscription, login BCrypt, JWT HS256, `/auth/me` |
| PostgreSQL | `postgres-auth` | 5432 | `jdbc:postgresql://localhost:5432/auth_db` | Base logique de l'authentification |

`demo-service` reste uniquement un service de validation du routage.

## Configuration externe

Dossier : `infrastructure/config-repo/`, lu par le Config Server en mode `native`.

- `application.yml` : valeurs communes non sensibles, intervalle de rafraîchissement Eureka.
- `api-gateway.yml` : routes `auth-service` (`/auth/**` → `lb://AUTH-SERVICE`) et `demo-service` (`/demo/**` → `lb://DEMO-SERVICE`).
- `demo-service.yml` : `app.message=pong-from-config-server`, preuve que la configuration est bien distribuée.

Aucun secret n'est stocké dans ce dossier. Les surcharges possibles sont `CONFIG_REPO_LOCATION`, `CONFIG_SERVER_URL` et `EUREKA_URI`.

## Démarrage Docker (mode retenu)

```powershell
copy infrastructure\.env.example infrastructure\.env
# renseigner AUTH_DB_PASSWORD et JWT_SECRET dans infrastructure/.env
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

`docker compose down` arrête tout ; ajouter `-v` supprime également le volume `postgres-auth-data`.

## Vérifications Sprint 2

| Vérification | Commande / URL | Résultat attendu |
| --- | --- | --- |
| Santé | `GET /actuator/health` sur 8888/8761/8080/8082 | `UP` |
| Flyway | `docker exec banking-postgres-auth psql -U auth_user -d auth_db -c '\dt'` | `roles`, `user_credentials`, `user_roles`, `flyway_schema_history` |
| Routes Gateway | `GET http://localhost:8080/actuator/gateway/routes` | `auth-service,demo-service` |
| Eureka | `GET http://localhost:8761/eureka/apps` | `API-GATEWAY,DEMO-SERVICE,AUTH-SERVICE` |
| Inscription | `POST http://localhost:8080/auth/register` | `201`, rôle `CLIENT` |
| Login | `POST http://localhost:8080/auth/login` | `200`, JWT `Bearer` |
| Profil protégé | `GET http://localhost:8080/auth/me` avec `Authorization: Bearer <jwt>` | `200` |
| Sans token | `GET http://localhost:8080/auth/me` | `401` |
| Email dupliqué | `POST /auth/register` avec le même email | `409` |
| Mot de passe invalide | `POST /auth/login` avec un mauvais mot de passe | `401` |

Toutes ces vérifications ont été exécutées avec succès dans Docker le 21/11/2026. Détails : [auth-service.md](auth-service.md).
