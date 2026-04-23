$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

Write-Host "Testing /api/sync/push endpoint"
Write-Host ""

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

$body = '{"items":[]}'

try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -ContentType "application/json" `
        -ErrorAction Stop
    
    Write-Host "SUCCESS: Status $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
}
catch [System.Net.Http.HttpRequestException] {
    Write-Host "ERROR: $($_.Exception.Message)"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "FAILED: HTTP Status $statusCode"
    
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        Write-Host "Response: $body"
    }
    catch {
        Write-Host "Could not read response"
    }
}
