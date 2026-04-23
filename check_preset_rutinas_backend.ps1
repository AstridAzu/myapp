$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

Write-Host "Verificando disponibilidad de RUTINAS PREDEFINIDAS (system)"
Write-Host ""

# Test: Pull rutinas desde sync endpoint
Write-Host "Test 1: GET /api/sync/pull?entity=rutinas"
try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/pull?entity=rutinas&since=0&limit=20" `
        -Method GET `
        -Headers $headers `
        -UseBasicParsing -ErrorAction Stop
    
    Write-Host "Status: $($response.StatusCode) ✓"
    $json = $response.Content | ConvertFrom-Json
    
    Write-Host "Items totales: $($json.items.Count)"
    if ($json.items.Count -gt 0) {
        Write-Host ""
        Write-Host "Primeras 3 rutinas:"
        $json.items | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - id=$($_.id) nombre=$($_.nombre) idCreador=$($_.idCreador)"
        }
        
        # Filtrar por 'system'
        $systemRutinas = $json.items | Where-Object { $_.idCreador -eq 'system' }
        Write-Host ""
        Write-Host "Rutinas con idCreador='system': $($systemRutinas.Count)"
    }
}
catch {
    $code = $_.Exception.Response.StatusCode.Value__
    Write-Host "❌ FAILED - Status $code"
}

Write-Host ""
Write-Host ""

# Test: Pull ejercicios base (ejercicios del catalogo)
Write-Host "Test 2: GET /api/sync/pull?entity=ejercicios"
try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/pull?entity=ejercicios&since=0&limit=20" `
        -Method GET `
        -Headers $headers `
        -UseBasicParsing -ErrorAction Stop
    
    Write-Host "Status: $($response.StatusCode) ✓"
    $json = $response.Content | ConvertFrom-Json
    
    Write-Host "Items totales: $($json.items.Count)"
    if ($json.items.Count -gt 0) {
        Write-Host ""
        Write-Host "Primeros 3 ejercicios:"
        $json.items | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - id=$($_.id) nombre=$($_.nombre)"
        }
    }
}
catch {
    $code = $_.Exception.Response.StatusCode.Value__
    Write-Host "❌ FAILED - Status $code"
}

Write-Host ""
Write-Host ""

# Test: Ver si hay endpoint de rutinas base públicas
Write-Host "Test 3: GET /api/routines/base/links (alternativa pública)"
try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/routines/base/links?limit=10" `
        -Method GET `
        -UseBasicParsing -ErrorAction Stop
    
    Write-Host "Status: $($response.StatusCode) ✓"
    $json = $response.Content | ConvertFrom-Json
    Write-Host "Items: $($json.Count)"
    if ($json.Count -gt 0) {
        $json | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - routineId=$($_.routineId)"
        }
    }
}
catch {
    $code = $_.Exception.Response.StatusCode.Value__
    Write-Host "⚠️  Status $code (puede ser normal si endpoint no existe)"
}
