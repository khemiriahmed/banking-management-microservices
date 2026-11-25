param(
  [string]$ComposeFile = "infrastructure/docker-compose.yml",
  [string]$EnvFile = "infrastructure/.env"
)
$ErrorActionPreference = "Stop"
function Invoke-Compose([string[]]$ComposeArgs) { docker compose -f $ComposeFile --env-file $EnvFile @ComposeArgs }
Write-Host "== Docker compose up =="
Invoke-Compose @("up","-d","--build")
Write-Host "== Waiting for health =="
$services = @("config-server","discovery-server","api-gateway","auth-service","user-service","account-service","transaction-service","transfer-service","notification-service","audit-service","frontend")
foreach ($s in $services) {
  $id = docker compose -f $ComposeFile --env-file $EnvFile ps -q $s
  if (-not $id) { throw "Service not found: $s" }
  for ($i=0; $i -lt 60; $i++) {
    $status = docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $id
    if ($status -eq "healthy" -or $status -eq "running") { Write-Host "$s=$status"; break }
    if ($status -eq "unhealthy") { docker logs --tail 100 $id; throw "Unhealthy: $s" }
    Start-Sleep -Seconds 5
    if ($i -eq 59) { throw "Timeout waiting for $s ($status)" }
  }
}
Write-Host "== Gateway routes =="
Invoke-RestMethod -Uri "http://localhost:8080/actuator/gateway/routes" | Select-Object -ExpandProperty route_id
Write-Host "== Basic checks =="
@(
  "http://localhost:4200/",
  "http://localhost:8080/actuator/health",
  "http://localhost:8082/actuator/health",
  "http://localhost:8083/actuator/health",
  "http://localhost:8084/actuator/health",
  "http://localhost:8085/actuator/health",
  "http://localhost:8086/actuator/health",
  "http://localhost:8087/actuator/health",
  "http://localhost:8088/actuator/health"
) | ForEach-Object { $r = Invoke-WebRequest -UseBasicParsing -Uri $_; Write-Host "$_ $($r.StatusCode)" }
Write-Host "DONE"
