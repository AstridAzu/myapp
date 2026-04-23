$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

Write-Host "Testing /api/sync/push endpoint"
Write-Host "Base URL: $syncBaseUrl"
Write-Host "Token (last 8): ...$(($syncToken).Substring($syncToken.Length - 8))"
Write-Host "User ID: $userId"
Write-Host ""

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

$body = '{"items":[]}'

Write-Host "Sending POST to /api/sync/push with empty items..."
Write-Host "Headers:"
$headers.GetEnumerator() | ForEach-Object { Write-Host "  $($_.Name): $($_.Value.Substring(0, [Math]::Min(20, $_.Value.Length)))..." }
Write-Host "Body: $body"
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -ContentType "application/json" `
        -ErrorAction Stop
    
    Write-Host "✓ SUCCESS"
    Write-Host "Status Code: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
}
catch {
    Write-Host "✗ FAILED"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.Value__)"
    Write-Host "Status Description: $($_.Exception.Response.StatusDescription)"
    
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Response Body: $errorBody"
    } catch {
        Write-Host "Could not read response body"
    }
}
