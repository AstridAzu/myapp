# Enhanced test script with different payload scenarios

$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

Write-Host "=== Testing /api/sync/push endpoint ==="
Write-Host "Base URL: $syncBaseUrl"
Write-Host "Token suffix: ...$(($syncToken).Substring($syncToken.Length - 8))"
Write-Host "x-user-id: $userId"
Write-Host ""

# Test 1: Empty items
Write-Host "[Test 1] Empty items array"
$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
    "Content-Type" = "application/json"
}
$body = '{"items":[]}'
Write-Host "Payload: $body"

try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -ErrorAction Stop
    Write-Host "✓ Status: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "✗ Status: $statusCode"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error body: $errorBody"
    } catch {}
}
Write-Host ""

# Test 2: Item with minimal fields
Write-Host "[Test 2] Single ejercicio item"
$ejercicioPayload = @{
    nombre = "Test Ejercicio"
    grupoMuscular = "pecho"
    descripcion = "Prueba"
} | ConvertTo-Json

$item = @{
    entityType = "ejercicios"
    id = [guid]::NewGuid().ToString()
    updatedAt = ([System.DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    syncStatus = "PENDING"
    payload = $ejercicioPayload | ConvertFrom-Json
}

$body = @{
    items = @($item)
} | ConvertTo-Json -Depth 10

Write-Host "Payload: $body"

try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -ErrorAction Stop
    Write-Host "✓ Status: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "✗ Status: $statusCode"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error body: $errorBody"
    } catch {}
}
Write-Host ""

# Test 3: Without x-user-id header
Write-Host "[Test 3] Without x-user-id header"
$headerNoUserId = @{
    "Authorization" = "Bearer $syncToken"
    "Content-Type" = "application/json"
}
$body = '{"items":[]}'

try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
        -Method POST `
        -Headers $headerNoUserId `
        -Body $body `
        -ErrorAction Stop
    Write-Host "✓ Status: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "✗ Status: $statusCode"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error body: $errorBody"
    } catch {}
}
