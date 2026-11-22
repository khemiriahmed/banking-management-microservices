# User Service — Sprint 3

Port : `8083` (direct) / `8080/users/**` et `8080/admin/**` via l'API Gateway. Base dédiée : `user_db`.

## Responsabilités

- Séparer le profil client des identifiants d'authentification : aucun mot de passe, email ou rôle n'est dupliqué dans `user_db`.
- Valider localement le JWT HS256 émis par `auth-service` ; le Gateway ne fait que transmettre l'en-tête `Authorization`.
- Dériver l'identité du claim `sub` (UUID) : aucun `userId` envoyé par le navigateur n'est accepté comme source de vérité.
- Créer le profil au premier appel `GET/PUT /users/me` avec un `firstName` issu de la partie locale de l'email et un `lastName` vide, modifiables ensuite.
- Migration Flyway `V1__create_customer_profiles.sql` : `customer_profiles(id, auth_user_id, first_name, last_name, phone, created_at)` avec `auth_user_id` unique. Aucune clé étrangère SQL entre `user_db` et `auth_db`.

## Endpoints

| Méthode | Chemin | Accès | Réponse |
| --- | --- | --- | --- |
| `GET` | `/users/me` | JWT `CLIENT` ou `ADMIN` | `200` + profil du `sub` du JWT |
| `PUT` | `/users/me` | JWT `CLIENT` ou `ADMIN` | `200` + profil mis à jour |
| `GET` | `/admin/users?page=&size=` | JWT `ADMIN` | `200` + page de profils |
| `GET` | `/admin/users/{authUserId}` | JWT `ADMIN` | `200` + profil trouvé par `authUserId` |
| `GET` | `/actuator/health` | public | `UP` |

`authUserId` n'est jamais modifiable : il n'est pas présent dans le corps de `PUT /users/me`. Les rôles sont lus depuis le claim `roles` et comparés avec `hasAuthority("ADMIN")`, car `auth-service` émet des valeurs brutes `CLIENT` et `ADMIN`.

Codes d'erreur JSON : `400` validation/UUID invalide, `401` JWT absent ou invalide, `403` rôle insuffisant, `404` profil introuvable, `500` erreur inattendue.

## Configuration

| Variable | Usage | Exemple |
| --- | --- | --- |
| `USER_DB_URL` | JDBC PostgreSQL | `jdbc:postgresql://postgres-user:5432/user_db` |
| `USER_DB_USER` | Utilisateur DB | `user_user` |
| `USER_DB_PASSWORD` | Mot de passe DB | défini dans `infrastructure/.env` |
| `USER_DB_NAME` | Nom de la base logique | `user_db` |
| `JWT_SECRET` | Même clé HMAC encodée en Base64 que `auth-service` | défini dans `infrastructure/.env` |
| `USER_SERVICE_PORT` | Port du service | `8083` |
| `CONFIG_SERVER_URL` | Config Server | `http://config-server:8888` |
| `EUREKA_URI` | Eureka | `http://discovery-server:8761/eureka/` |

## Tests

```powershell
$env:JAVA_HOME='C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\jdk-17.0.21+9'; $env:PATH="$env:JAVA_HOME\bin;C:\Users\Ahmed\AppData\Local\Temp\opencode\toolchain\apache-maven-3.9.16\bin;$env:PATH"; mvn -B -f backend/user-service/pom.xml test
```

Couverture : `JwtTokenVerifierTest` (claims, expiration, subject invalide), `CustomerProfileServiceTest` (provisioning, `authUserId` immuable, pagination), `UserServiceIntegrationTest` (isolation des profils, en-tête forgé ignoré, rôles CLIENT/ADMIN, JWT invalide, validation, H2 + Flyway). 13 tests verts.

## Validation Docker effectuée

```powershell
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

Résultats : 8 conteneurs `Up`, tous les healths `UP`, tables `customer_profiles` et `flyway_schema_history` dans `user_db`, routes Gateway `auth-service,demo-service,user-service`, Eureka `USER-SERVICE`, profil créé depuis le JWT `200`, `X-User-Id` forgé ignoré, `PUT /users/me` `200`, accès ADMIN refusé pour `CLIENT` `403`, liste/admin détail `200`, sans JWT et JWT invalide `401`.
