@echo off
REM Test sync endpoint with curl

set SYNC_BASE_URL=https://ratita-gym--worker.azucenapolo6.workers.dev
set SYNC_TOKEN=ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0
set USER_ID=00000001-0000-5000-8000-000000000001

echo Testing /api/sync/push endpoint...
echo.

REM Test 1: Empty payload
echo [Test 1] Empty items array
curl -X POST "%SYNC_BASE_URL%/api/sync/push" ^
  -H "Authorization: Bearer %SYNC_TOKEN%" ^
  -H "x-user-id: %USER_ID%" ^
  -H "Content-Type: application/json" ^
  -d "{\"items\":[]}" ^
  -w "\nHTTP Status: %%{http_code}\n" ^
  -v

echo.
echo [Test 2] Without x-user-id header
curl -X POST "%SYNC_BASE_URL%/api/sync/push" ^
  -H "Authorization: Bearer %SYNC_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"items\":[]}" ^
  -w "\nHTTP Status: %%{http_code}\n" ^
  -v

echo.
echo Done.
