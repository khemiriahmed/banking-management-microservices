# Ports et services — Sprint 1

## Versions retenues

| Composant | Version |
| --- | --- |
| Java | 17 (Temurin 17.0.21) |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 (Northfields) |
| Maven | 3.9.16 (Maven Wrapper fourni par service) |

## Services locaux

| Service | Artifact | Port | URL de base | Rôle |
| --- | --- | --- | --- | --- |
| Config Server | `config-server` | 8888 | `http://localhost:8888` | Configuration non sensible, profil `native` |
| Discovery Server | `discovery-server` | 8761 | `http://localhost:8761` | Tableau de bord Eureka |
| API Gateway | `api-gateway` | 8080 | `http://localhost:8080` | Point d'entrée unique, routes `lb://` |
| Demo Service | `demo-service` | 8081 | `http://localhost:8081` | Service temporaire de validation du routage |

`demo-service` existe uniquement pour prouver la découverte et le routage ; il sera retiré ou remplacé lorsque les vrais services métier seront disponibles.

## Configuration externe

Dossier : `infrastructure/config-repo/`, lu par le Config Server en mode `native`.

- `application.yml` : valeurs communes non sensibles, intervalle de rafraîchissement Eureka en dev.
- `api-gateway.yml` : route `demo-service` vers `lb://DEMO-SERVICE` sur `Path=/demo/**` (préfixe `spring.cloud.gateway.server.webflux`).
- `demo-service.yml` : `app.message=pong-from-config-server`, preuve que la configuration est bien distribuée.

Aucun secret n'est stocké dans ce dossier. La surcharge possible est `CONFIG_REPO_LOCATION`.

## Ordre de démarrage

```powershell
.\services\config-server\mvnw.cmd -f services/config-server/pom.xml spring-boot:run
.\services\discovery-server\mvnw.cmd -f services/discovery-server/pom.xml spring-boot:run
.\services\demo-service\mvnw.cmd -f services/demo-service/pom.xml spring-boot:run
.\services\api-gateway\mvnw.cmd -f services/api-gateway/pom.xml spring-boot:run
```

Attendre que chaque `/actuator/health` réponde `UP`, puis attendre l'inscription de `DEMO-SERVICE` dans Eureka avant de tester le Gateway.

## Vérifications Sprint 1

| Vérification | Commande / URL | Résultat attendu |
| --- | --- | --- |
| Config Server | `GET http://localhost:8888/demo-service/default` | `app.message=pong-from-config-server` |
| Eureka | `GET http://localhost:8761/eureka/apps` | `DEMO-SERVICE` et `API-GATEWAY` enregistrés |
| Demo direct | `GET http://localhost:8081/demo/ping` | `{"status":"ok","message":"pong-from-config-server"}` |
| Routes Gateway | `GET http://localhost:8080/actuator/gateway/routes` | route `demo-service`, uri `lb://DEMO-SERVICE` |
| Via Gateway | `GET http://localhost:8080/demo/ping` | même réponse que le demo direct |

Toutes ces vérifications ont été exécutées localement avec succès le 20/11/2026. Les `target/*.jar` et logs de démarrage restent hors de Git (`.gitignore`, dossier temporaire).
