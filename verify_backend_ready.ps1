$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

Write-Host "🔍 VERIFICATION: Backend Push Endpoint Status"
Write-Host "============================================="
Write-Host ""

# Test 1: Empty items (should work now)
Write-Host "Test 1: POST /api/sync/push with empty items"
Write-Host "Payload: {\"items\":[]}"
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
        -Method POST `
        -Headers $headers `
        -Body '{"items":[]}' `
        -ContentType "application/json" `
        -UseBasicParsing `
        -ErrorAction Stop
    
    Write-Host "✅ SUCCESS"
    Write-Host "Status: $($response.StatusCode)"
    $json = $response.Content | ConvertFrom-Json
    Write-Host "Response:"
    $json | ConvertTo-Json -Depth 3
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "❌ FAILED"
    Write-Host "Status: $statusCode"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        if ($body) { Write-Host "Response: $body" }
    }
    catch {}
}

Write-Host ""
Write-Host ""

# Test 2: Pull endpoint (should still work)
Write-Host "Test 2: GET /api/sync/pull (regression test)"
try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/pull?entity=usuarios`&since=0`&limit=5" `
        -Method GET `
        -Headers $headers `
        -UseBasicParsing `
        -ErrorAction Stop
    
    Write-Host "✅ SUCCESS"
    Write-Host "Status: $($response.StatusCode)"
    $json = $response.Content | ConvertFrom-Json
    Write-Host "Items: $($json.items.Count)"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "❌ FAILED"
    Write-Host "Status: $statusCode"
}
