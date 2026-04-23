# Test script to diagnose sync authentication issue

$syncBaseUrl = "https://ratita-gym--worker.azucenapolo6.workers.dev"
$syncToken = "ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0"

# Try multiple user IDs to diagnose
$testUserIds = @(
    "00000001-0000-5000-8000-000000000001",  # Standard UUID format
    "00000002-0000-5000-8000-000000000002",  # Another UUID
    "1",                                       # Legacy numeric
    "2"                                        # Another legacy
)

Write-Host "Testing /api/sync/push endpoint..."
Write-Host "Base URL: $syncBaseUrl"
Write-Host "Token suffix: ...$(($syncToken).Substring($syncToken.Length - 8))"
Write-Host ""

foreach ($userId in $testUserIds) {
    Write-Host "Testing with x-user-id: $userId"
    
    $headers = @{
        "Authorization" = "Bearer $syncToken"
        "x-user-id" = $userId
        "Content-Type" = "application/json"
    }
    
    $body = @{
        items = @()
    } | ConvertTo-Json
    
    try {
        $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/push" `
            -Method POST `
            -Headers $headers `
            -Body $body `
            -ErrorAction Stop
        
        Write-Host "  ✓ Status: $($response.StatusCode)"
        Write-Host "  Response: $($response.Content)"
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        $statusMessage = $_.Exception.Response.StatusDescription
        Write-Host "  ✗ Status: $statusCode - $statusMessage"
        
        # Try to read error body
        if ($_.Exception.Response.StatusCode -eq 403 -or $_.Exception.Response.StatusCode -eq 401) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $errorBody = $reader.ReadToEnd()
                Write-Host "  Error body: $errorBody"
            }
            catch {
                Write-Host "  (Could not read error body)"
            }
        }
    }
    
    Write-Host ""
}

Write-Host "Also testing /api/sync/pull with same credentials..."
foreach ($userId in @($testUserIds[0])) {  # Just test first one for pull
    Write-Host "Testing with x-user-id: $userId"
    
    $headers = @{
        "Authorization" = "Bearer $syncToken"
        "x-user-id" = $userId
    }
    
    try {
        $response = Invoke-WebRequest -Uri "$syncBaseUrl/api/sync/pull?entity=usuarios`&since=0`&limit=10" `
            -Method GET `
            -Headers $headers `
            -ErrorAction Stop
        
        Write-Host "  ✓ Status: $($response.StatusCode)"
        Write-Host "  Response: $($response.Content)"
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        $statusMessage = $_.Exception.Response.StatusDescription
        Write-Host "  ✗ Status: $statusCode - $statusMessage"
    }
}
