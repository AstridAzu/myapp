$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"
$userId = "00000001-0000-5000-8000-000000000001"

$headers = @{
    "Authorization" = "Bearer $syncToken"
    "x-user-id" = $userId
}

Write-Host "Checking what entities/data exist for user..."
Write-Host "User ID: $userId"
Write-Host ""

$key_entities = @("usuarios", "rutinas", "rutina_accesos", "rutina_ejercicios")

foreach ($entity in $key_entities) {
    try {
        $uri = "$syncBaseUrl/api/sync/pull?entity=$entity`&since=0`&limit=10"
        $response = Invoke-WebRequest -Uri $uri -Method GET -Headers $headers -ErrorAction Stop
        
        $json = $response.Content | ConvertFrom-Json
        $itemCount = $json.items.Count
        Write-Host "✓ $entity : $itemCount items"
        
        if ($itemCount -gt 0) {
            Write-Host "  Sample:"
            $json.items[0] | Add-Member -NotePropertyName "__hint" -NotePropertyValue "Item 1 of $itemCount" -PassThru | ConvertTo-Json | Select-Object -First 20
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        Write-Host "✗ $entity : HTTP $statusCode"
    }
    Write-Host ""
}
