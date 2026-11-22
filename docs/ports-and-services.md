# Ports et services — Sprint 5 + frontend

## Versions retenues

| Composant | Version |
| --- | --- |
| Java | 17 (Temurin 17.0.21) |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 (Northfields) |
| Maven | 3.9.16 (Maven Wrapper fourni par service) |
| JJWT | 0.13.0 |
| PostgreSQL | 16-alpine (containers `banking-postgres-auth`, `banking-postgres-user`, `banking-postgres-account`) |
| Angular | 15.2.10 |
| Node (build frontend) | 18-alpine |
| Nginx | 1.27-alpine |

## Services locaux

| Service | Artifact | Port hôte | URL de base | Rôle |
| --- | --- | --- | --- | --- |
| Config Server | `config-server` | 8888 | `http://localhost:8888` | Configuration non sensible, profil `native`, dépôt monté en `/config-repo` |
| Discovery Server | `discovery-server` | 8761 | `http://localhost:8761` | Tableau de bord Eureka |
| API Gateway | `api-gateway` | 8080 | `http://localhost:8080` | Point d'entrée unique, routes `lb://` |
| Demo Service | `demo-service` | 8081 | `http://localhost:8081` | Service temporaire de validation du routage |
| Auth Service | `auth-service` | 8082 | `http://localhost:8082` | Inscription, login BCrypt, JWT HS256, `/auth/me` |
| User Service | `user-service` | 8083 | `http://localhost:8083` | Profils clients, endpoints `/users/me` et `/admin/users` |
| Account Service | `account-service` | 8084 | `http://localhost:8084` | Comptes, bénéficiaires, soldes, statuts, opérations internes `/internal/**` |
| Frontend | `frontend` | 4200 | `http://localhost:4200` | Application Angular 15 servie par Nginx, API proxifiée vers `api-gateway:8080` |
| PostgreSQL Auth | `postgres-auth` | 5432 | `jdbc:postgresql://localhost:5432/auth_db` | Base logique de l'authentification |
| PostgreSQL User | `postgres-user` | 5433 | `jdbc:postgresql://localhost:5433/user_db` | Base logique des profils clients |
| PostgreSQL Account | `postgres-account` | 5434 | `jdbc:postgresql://localhost:5434/account_db` | Base logique des comptes |

`demo-service` reste uniquement un service de validation du routage.

## Configuration externe

Dossier : `infrastructure/config-repo/`, lu par le Config Server en mode `native`.

- `application.yml` : valeurs communes non sensibles, intervalle de rafraîchissement Eureka.
- `api-gateway.yml` : routes `auth-service` (`/auth/**` → `lb://AUTH-SERVICE`), `demo-service` (`/demo/**` → `lb://DEMO-SERVICE`), `account-service` (`/accounts/**,/admin/accounts/**,/beneficiaries/**` → `lb://ACCOUNT-SERVICE`) et `user-service` (`/users/**,/admin/**` → `lb://USER-SERVICE`). Les routes `/internal/**` ne sont pas exposées. Le proxy Angular (`proxy.conf.json` en dev, `nginx.conf` en conteneur) relaie aussi `/beneficiaries/**` vers le Gateway.
- `demo-service.yml` : `app.message=pong-from-config-server`, preuve que la configuration est bien distribuée.

Aucun secret n'est stocké dans ce dossier. Les surcharges possibles sont `CONFIG_REPO_LOCATION`, `CONFIG_SERVER_URL` et `EUREKA_URI`.

## Démarrage Docker (mode retenu)

```powershell
copy infrastructure\.env.example infrastructure\.env
# renseigner AUTH_DB_PASSWORD, USER_DB_PASSWORD, ACCOUNT_DB_PASSWORD, JWT_SECRET et INTERNAL_SERVICE_TOKEN dans infrastructure/.env
docker compose -f infrastructure/docker-compose.yml --env-file infrastructure/.env up -d --build
```

`docker compose down` arrête tout ; ajouter `-v` supprime également les volumes `postgres-auth-data`, `postgres-user-data` et `postgres-account-data`.

## Vérifications Sprint 2, Sprint 3, Sprint 4 et Sprint 5

| Vérification | Commande / URL | Résultat attendu |
| --- | --- | --- |
| Santé | `GET /actuator/health` sur 8888/8761/8080/8082/8083/8084 | `UP` |
| Flyway Auth | `docker exec banking-postgres-auth psql -U auth_user -d auth_db -c '\dt'` | `roles`, `user_credentials`, `user_roles`, `flyway_schema_history` |
| Flyway User | `docker exec banking-postgres-user psql -U user_user -d user_db -c '\dt'` | `customer_profiles`, `flyway_schema_history` |
| Flyway Account | `docker exec banking-postgres-account psql -U account_user -d account_db -c '\dt'` | `bank_accounts`, `beneficiaries`, `flyway_schema_history` |
| Routes Gateway | `GET http://localhost:8080/actuator/gateway/routes` | `auth-service,demo-service,account-service,user-service`, route account avec `/beneficiaries/**` |
| Frontend | `GET http://localhost:4200/` | `200`, page Angular, health `UP` |
| Eureka | `GET http://localhost:8761/eureka/apps` | `API-GATEWAY,DEMO-SERVICE,AUTH-SERVICE,USER-SERVICE,ACCOUNT-SERVICE` |
| Inscription | `POST http://localhost:8080/auth/register` | `201`, rôle `CLIENT` |
| Login | `POST http://localhost:8080/auth/login` | `200`, JWT `Bearer` |
| Profil protégé | `GET http://localhost:8080/auth/me` avec `Authorization: Bearer <jwt>` | `200` |
| Sans token | `GET http://localhost:8080/auth/me` | `401` |
| Email dupliqué | `POST /auth/register` avec le même email | `409` |
| Mot de passe invalide | `POST /auth/login` avec un mauvais mot de passe | `401` |
| Profil depuis JWT | `GET http://localhost:8080/users/me` avec `Bearer <jwt>` | `200`, `authUserId` = claim `sub` |
| En-tête forgé ignoré | `GET /users/me` avec `X-User-Id` arbitraire | `authUserId` inchangé |
| Mise à jour profil | `PUT http://localhost:8080/users/me` | `200`, champs validés, `authUserId` immuable |
| Rôle CLIENT | `GET http://localhost:8080/admin/users` avec un JWT `CLIENT` | `403` |
| Rôle ADMIN | `GET /admin/users` et `GET /admin/users/{authUserId}` avec un JWT `ADMIN` | `200` |
| Comptes du client | `GET http://localhost:8080/accounts` avec `Bearer <jwt>` | `200`, uniquement les comptes du `sub` |
| Autre client | `GET /accounts/{id}` avec un JWT d'un autre client | `404` |
| Création ADMIN | `POST http://localhost:8080/admin/accounts` avec un JWT `ADMIN` | `201`, `reservedBalance` absent |
| Solde négatif | `POST /admin/accounts` avec `initialBalance: -1` | `400` |
| Précision décimale | `POST /internal/accounts/{id}/reserve` avec `10.123` | `400` |
| Token interne | `POST http://localhost:8084/internal/accounts/{id}/reserve` sans `X-Internal-Service-Token` | `401` |
| Opérations internes | reserve/debit/credit sur `http://localhost:8084/internal/accounts/{id}/...` | `200`, solde final `949.75` |
| Route interne non exposée | `POST http://localhost:8080/internal/accounts/{id}/reserve` | `404` |
| Compte suspendu | `PATCH /admin/accounts/{id}/status` puis reserve interne | `200` puis `409` |
| Bénéficiaires du client | `GET http://localhost:8080/beneficiaries` avec `Bearer <jwt>` | `200`, uniquement les bénéficiaires du `sub` |
| Création bénéficiaire | `POST http://localhost:8080/beneficiaries` avec référence `FR76 **** **** **** 1234` | `201`, statut `ACTIVE`, référence masquée |
| Doublon bénéficiaire | `POST /beneficiaries` avec la même référence pour le même client | `409` |
| Référence non masquée | `POST /beneficiaries` avec un IBAN complet | `400` |
| Autre client bénéficiaire | `GET`, `PUT` ou `DELETE /beneficiaries/{id}` avec un JWT d'un autre client | `404` |
| Mise à jour bénéficiaire | `PUT http://localhost:8080/beneficiaries/{id}` | `200`, champs modifiés |
| Suppression bénéficiaire | `DELETE http://localhost:8080/beneficiaries/{id}` puis `GET` | `204` puis `404` |

Toutes ces vérifications ont été exécutées avec succès dans Docker les 21/11/2026 et 22/11/2026. Détails : [auth-service.md](auth-service.md), [user-service.md](user-service.md) et [account-service.md](account-service.md).
