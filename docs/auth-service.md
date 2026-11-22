# Auth Service — Sprint 2

Port : `8082` (direct) / `8080/auth/**` via l'API Gateway. Base dédiée : `auth_db`.

## Responsabilités

- Inscription d'un utilisateur avec email unique, mot de passe haché BCrypt et rôle `CLIENT` par défaut.
- Login avec vérification BCrypt et émission d'un JWT HS256 signé (claims `sub`, `email`, `roles`, `iat`, `exp`).
- Protection des endpoints via un filtre JWT stateless ; le Gateway achemine, il ne décide pas de l'autorisation finale.
- Migration Flyway `V1__create_auth_tables.sql` : `roles`, `user_credentials`, `user_roles`, seeding `CLIENT` et `ADMIN`.

## Endpoints

| Méthode | Chemin | Accès | Réponse |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | public | `201` + `{id,email,roles}` |
| `POST` | `/auth/login` | public | `200` + `{accessToken,tokenType,expiresIn,roles}` |
| `GET` | `/auth/me` | JWT requis | `200` + `{id,email,roles}` |
| `GET` | `/actuator/health` | public | `UP` |

Codes d'erreur JSON : `400` validation, `401` identifiants/JWT invalides, `409` email déjà utilisé, `403` accès refusé, `500` erreur inattendue. Le hash du mot de passe n'est jamais exposé.

## Configuration

| Variable | Usage | Exemple |
| --- | --- | --- |
| `AUTH_DB_URL` | JDBC PostgreSQL | `jdbc:postgresql://postgres-auth:5432/auth_db` |
| `AUTH_DB_USER` | Utilisateur DB | `auth_user` |
| `AUTH_DB_PASSWORD` | Mot de passe DB | défini dans `infrastructure/.env` |
| `JWT_SECRET` | Clé HMAC encodée en Base64, 32 octets minimum | défini dans `infrastructure/.env` |
| `JWT_EXPIRATION_SECONDS` | Durée de vie du token | `3600` |
| `CONFIG_SERVER_URL` | Config Server | `http://config-server:8888` |
| `EUREKA_URI` | Eureka | `http://discovery-server:8761/eureka/` |

`infrastructure/.env.example` contient la matrice des variables. `infrastructure/.env` est ignoré par Git.

## Tests

```powershell
.\backend\auth-service\mvnw.cmd -f backend/auth-service/pom.xml test
```

Couverture : `AuthServiceTest` (règles métier), `JwtTokenProviderTest` (claims + expiration), `AuthControllerTest` (statuts HTTP/validation via MockMvc standalone), `AuthIntegrationTest` (parcours inscription → login → `/auth/me`, H2 + Flyway + profil `test`).

## Validation Docker effectuée

```powershell
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

Résultats : `auth-service` healthy, tables Flyway présentes dans `auth_db`, routes Gateway `auth-service,demo-service`, Eureka `API-GATEWAY,DEMO-SERVICE,AUTH-SERVICE`, inscription `201`, login `200`, `/auth/me` avec JWT `200`, sans JWT `401`, email dupliqué `409`, mot de passe incorrect `401`.
