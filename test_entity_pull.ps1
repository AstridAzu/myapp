$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

$entities = @(
    "usuarios",
    "especialidades",
    "certificaciones",
    "objetivos",
    "ejercicios",
    "rutinas",
    "planes_semana",
    "plan_dias",
    "plan_dias_fecha",
    "sesiones_programadas",
    "notificaciones",
    "asignaciones",
    "plan_asignaciones",
    "rutina_ejercicios",
    "rutina_accesos",
    "sesiones_rutina",
    "registros_series"
)

Write-Host "Testing /api/sync/pull for each entity type..."
Write-Host "User: $userId"
Write-Host ""

foreach ($entity in $entities) {
    try {
        $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/pull?entity=$entity`&since=0`&limit=5" `
            -Method GET `
            -Headers $headers `
            -ContentType "application/json" `
            -UseBasicParsing `
            -ErrorAction Stop
        
        $json = $response.Content | ConvertFrom-Json
        $itemCount = $json.items.Count
        Write-Host "✓ $entity : $itemCount items, nextSince=$($json.nextSince)"
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        Write-Host "✗ $entity : HTTP $statusCode"
    }
}

Write-Host ""
Write-Host "Sample responses for key entities..."

# Get detailed response for rutinas
Write-Host ""
Write-Host "=== rutinas detail ==="
try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/pull?entity=rutinas`&since=0`&limit=10" `
        -Method GET `
        -Headers $headers `
        -ContentType "application/json" `
        -UseBasicParsing
    
    $json = $response.Content | ConvertFrom-Json
    Write-Host "Items count: $($json.items.Count)"
    if ($json.items.Count -gt 0) {
        Write-Host "First item:"
        $json.items[0] | ConvertTo-Json -Depth 3
    }
}
catch {
    Write-Host "Error: $($_.Exception.Message)"
}
