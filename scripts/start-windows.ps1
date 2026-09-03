# Start the whole stack: Postgres, Keycloak, backend and frontend.
$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

if (-not (Test-Path '.env')) {
    Write-Host 'Creating .env from .env.example'
    Copy-Item '.env.example' '.env'
}

docker compose up --build -d
if ($LASTEXITCODE -ne 0) { throw 'docker compose failed to start the stack.' }

Write-Host ''
Write-Host 'Frontend   http://localhost:4200'
Write-Host 'Backend    http://localhost:8080'
Write-Host 'API docs   http://localhost:8080/swagger-ui.html'
Write-Host 'Keycloak   http://localhost:8081  (admin / admin)'
Write-Host ''
Write-Host 'Sign in as owner / owner, or assistant / assistant.'
Write-Host 'Keycloak takes about a minute on the first run while it imports the realm.'
