# OpenAPI / Swagger

Chaque service Spring Boot expose sa propre documentation OpenAPI via springdoc `2.8.17`. Les URLs sont accessibles directement sur le port du service ; le Gateway n'a pas de route dédiée à `/swagger-ui` ou `/v3/api-docs`.

| Service | Port | Swagger UI | JSON OpenAPI |
| --- | ---: | --- | --- |
| Config Server | 8888 | `http://localhost:8888/swagger-ui.html` | `http://localhost:8888/v3/api-docs` |
| Discovery Server | 8761 | `http://localhost:8761/swagger-ui.html` | `http://localhost:8761/v3/api-docs` |
| API Gateway | 8080 | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs` |
| Demo Service | 8081 | `http://localhost:8081/swagger-ui.html` | `http://localhost:8081/v3/api-docs` |
| Auth Service | 8082 | `http://localhost:8082/swagger-ui.html` | `http://localhost:8082/v3/api-docs` |
| User Service | 8083 | `http://localhost:8083/swagger-ui.html` | `http://localhost:8083/v3/api-docs` |
| Account Service | 8084 | `http://localhost:8084/swagger-ui.html` | `http://localhost:8084/v3/api-docs` |
| Transfer Service | 8085 | `http://localhost:8085/swagger-ui.html` | `http://localhost:8085/v3/api-docs` |
| Transaction Service | 8086 | `http://localhost:8086/swagger-ui.html` | `http://localhost:8086/v3/api-docs` |
| Notification Service | 8087 | `http://localhost:8087/swagger-ui.html` | `http://localhost:8087/v3/api-docs` |
| Audit Service | 8088 | `http://localhost:8088/swagger-ui.html` | `http://localhost:8088/v3/api-docs` |

Les endpoints de documentation sont autorisés sans JWT dans les services sécurisés ; les endpoints métier restent protégés par JWT et par rôle selon le service.

Pour régénérer un contrat statique :

```powershell
$json = Invoke-RestMethod -Uri "http://localhost:8085/v3/api-docs"
$json | ConvertTo-Json -Depth 100 | Set-Content openapi-transfer.json
```
