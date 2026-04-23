$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

Write-Host "Test 1: /api/sync/pull (GET)"
try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/pull?entity=usuarios`&since=0`&limit=10" -Method GET -Headers $headers
    Write-Host "SUCCESS: Status $($response.StatusCode)"
    Write-Host "Response: $($response.Content | ConvertFrom-Json | ConvertTo-Json)"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "FAILED: HTTP Status $statusCode"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        if ($body) { Write-Host "Response: $body" }
    }
    catch {}
}

Write-Host ""
Write-Host "Test 2: /api/sync/push (POST) with empty items"
$body = '{"items":[]}'
try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" -Method POST -Headers $headers -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: Status $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "FAILED: HTTP Status $statusCode"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        if ($body) { Write-Host "Response: $body" } else { Write-Host "Response: (empty)" }
    }
    catch {}
}
