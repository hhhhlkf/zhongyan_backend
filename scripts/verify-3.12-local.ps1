param(
    [string]$BaseUrl = "http://localhost:8088/v2",
    [string]$GeoServerUrl = "http://localhost:8080/geoserver",
    [string]$GrafanaUrl = "http://localhost:3000",
    [string]$PrometheusUrl = "http://localhost:9090",
    [string]$Username = "admin",
    [string]$Password = "admin"
)

$ErrorActionPreference = "Stop"

function Assert-True($Condition, $Message) {
    if (-not $Condition) {
        throw $Message
    }
}

function Get-Token {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/token" `
        -ContentType "application/json" -Body $body -TimeoutSec 10
    Assert-True ($response.code -eq 200) "token request failed"
    return $response.data.accessToken
}

$compose = docker compose -f deploy/docker-compose.local.yml ps
Assert-True ($LASTEXITCODE -eq 0) "docker compose ps failed"
Assert-True ($compose -match "zhongyan-uav-postgres") "postgres container is missing"
Assert-True ($compose -match "zhongyan-uav-kafka") "kafka container is missing"
Assert-True ($compose -match "zhongyan-uav-minio") "minio container is missing"
Assert-True ($compose -match "zhongyan-uav-geoserver") "geoserver container is missing"
Assert-True ($compose -match "zhongyan-uav-prometheus") "prometheus container is missing"
Assert-True ($compose -match "zhongyan-uav-grafana") "grafana container is missing"

$token = Get-Token
$headers = @{ Authorization = "Bearer $token" }

$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
Assert-True ($health.status -eq "UP") "application health is not UP"

$metrics = curl.exe -s -H "Authorization: Bearer $token" "$BaseUrl/actuator/prometheus"
Assert-True ($metrics -match "jvm_info") "prometheus endpoint did not expose JVM metrics"
Assert-True ($metrics -match "http_server_requests") "prometheus endpoint did not expose HTTP metrics"

$rateCodes = @()
1..140 | ForEach-Object {
    $content = curl.exe -s -H "Authorization: Bearer $token" "$BaseUrl/missions"
    $rateCodes += (($content | ConvertFrom-Json).code)
}
Assert-True (($rateCodes | Where-Object { $_ -eq 429 }).Count -gt 0) "rate limit did not trigger"
Start-Sleep -Seconds 60
$token = Get-Token
$headers = @{ Authorization = "Bearer $token" }

$postgres = docker exec zhongyan-uav-postgres pg_isready -U zhongyan -d zhongyan_uav
Assert-True ($postgres -match "accepting connections") "postgres is not accepting connections"
$extensions = docker exec zhongyan-uav-postgres psql -U zhongyan -d zhongyan_uav -t -c "select string_agg(extname, ',') from pg_extension where extname in ('postgis','vector');"
Assert-True ($extensions -match "postgis") "postgis extension is missing"
Assert-True ($extensions -match "vector") "pgvector extension is missing"

$redis = docker exec zhongyan-uav-redis sh -c "REDISCLI_AUTH=zhongyan_redis_local redis-cli ping"
Assert-True ($redis -match "PONG") "redis ping failed"

$topics = docker exec zhongyan-uav-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
Assert-True ($topics -match "task-events") "task-events topic is missing"
Assert-True ($topics -match "uav-telemetry") "uav-telemetry topic is missing"

$minioHealth = curl.exe -s -o NUL -w "%{http_code}" http://localhost:9000/minio/health/live
Assert-True ($minioHealth -eq "200") "minio health check failed"
$minioRoundTrip = docker run --rm --network zhongyan-uav-local_default --entrypoint sh minio/mc:latest -c "mc alias set local http://minio:9000 zhongyan zhongyan_minio_local >/dev/null && printf validation-ok | mc pipe local/zhongyan-uav-assets/validation/3.12.txt >/dev/null && mc cat local/zhongyan-uav-assets/validation/3.12.txt"
Assert-True ($minioRoundTrip -match "validation-ok") "minio object round trip failed"

$geoVersion = curl.exe -s -u admin:geoserver "$GeoServerUrl/rest/about/version.xml"
Assert-True ($geoVersion -match "GeoServer") "geoserver version endpoint failed"

$missionBody = @{
    name = "3.12 geo verify"
    scenarioType = "verification"
    priority = 1
    createdBy = "verification"
    description = "GeoServer publish verification"
} | ConvertTo-Json
$mission = Invoke-RestMethod -Method Post -Uri "$BaseUrl/missions" -Headers $headers -ContentType "application/json" -Body $missionBody -TimeoutSec 20
$missionId = $mission.data.missionId

$taskBody = @{
    taskType = "PUBLISH_LAYER"
    priority = 1
    deviceId = "verify-device"
    modelId = "verify-model"
    configSnapshot = @{}
    inputAssetIds = @()
    createdBy = "verification"
} | ConvertTo-Json
$task = Invoke-RestMethod -Method Post -Uri "$BaseUrl/missions/$missionId/tasks" -Headers $headers -ContentType "application/json" -Body $taskBody -TimeoutSec 20
$taskId = $task.data.taskId

$assetBody = @{
    missionId = $missionId
    taskId = $taskId
    assetType = "IMAGE"
    role = "OUTPUT"
    name = "geo verify asset"
    objectKey = "verify/geo.jpg"
    contentType = "image/jpeg"
    sizeBytes = 12
    checksum = "sha256-verify"
    metadata = @{ latitude = 30.71; longitude = 104.11 }
    createdBy = "verification"
} | ConvertTo-Json -Depth 8
$asset = Invoke-RestMethod -Method Post -Uri "$BaseUrl/assets" -Headers $headers -ContentType "application/json" -Body $assetBody -TimeoutSec 20
$assetId = $asset.data.assetId

$geoBody = @{ requestedBy = "verification"; parameters = @{ latitude = 30.71; longitude = 104.11; halfSizeDegrees = 0.0005 } } | ConvertTo-Json -Depth 8
Invoke-RestMethod -Method Post -Uri "$BaseUrl/assets/$assetId/geo-boundary" -Headers $headers -ContentType "application/json" -Body $geoBody -TimeoutSec 30 | Out-Null

$publishBody = @{ requestedBy = "verification"; parameters = @{ approved = $true; layerId = "asset_geometry" } } | ConvertTo-Json -Depth 8
$publish = Invoke-RestMethod -Method Post -Uri "$BaseUrl/assets/$assetId/publish-layer" -Headers $headers -ContentType "application/json" -Body $publishBody -TimeoutSec 60
Assert-True ($publish.code -eq 200) "layer publish failed"

$layerStatus = curl.exe -s -o NUL -w "%{http_code}" -u admin:geoserver "$GeoServerUrl/rest/layers/zhongyan:asset_geometry.xml"
Assert-True ($layerStatus -eq "200") "geoserver layer is missing"

$promTargets = curl.exe -s "$PrometheusUrl/api/v1/targets" | ConvertFrom-Json
$backendTarget = $promTargets.data.activeTargets | Where-Object { $_.labels.job -eq "zhongyan-uav-backend" }
Assert-True ($backendTarget.health -eq "up") "prometheus backend target is not up"

$grafanaHealth = curl.exe -s -u admin:admin "$GrafanaUrl/api/health" | ConvertFrom-Json
Assert-True ($grafanaHealth.database -eq "ok") "grafana health failed"
$grafanaQuery = curl.exe -s -u admin:admin "$GrafanaUrl/api/datasources/proxy/uid/PBFA97CFB590B2093/api/v1/query?query=up"
Assert-True ($grafanaQuery -match '"zhongyan-uav-backend"') "grafana datasource query failed"

[pscustomobject]@{
    status = "ok"
    baseUrl = $BaseUrl
    missionId = $missionId
    taskId = $taskId
    assetId = $assetId
    prometheusTarget = $backendTarget.health
    grafana = $grafanaHealth.version
} | ConvertTo-Json
