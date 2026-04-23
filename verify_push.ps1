$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

Write-Host "Verificando Backend - POST /api/sync/push"
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
        -Method POST `
        -Headers $headers `
        -Body '{"items":[]}' `
        -ContentType "application/json" `
        -UseBasicParsing -ErrorAction Stop
    
    Write-Host "SUCCESS - Status $($response.StatusCode)"
    Write-Host $response.Content
}
catch {
    $code = $_.Exception.Response.StatusCode.Value__
    Write-Host "FAILED - Status $code"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        if ($body) { Write-Host $body }
    }
    catch {}
}
