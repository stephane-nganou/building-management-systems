# Run the end to end suite against a throwaway copy of the whole stack.
#
# It uses its own compose project, so the database it creates is not the one you
# develop against. It does use the same ports, because the realm's redirect URIs
# name them, so stop the development stack first.
$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

$project = 'bms-e2e'
$keepStack = $env:E2E_KEEP_STACK -eq '1'

# Waits for a URL to answer at all. The stack is only useful once every part is
# up: starting the tests a second too early is a flake, not a failure.
function Wait-ForUrl {
    param([string]$Name, [string]$Url, [int]$Attempts = 90)

    Write-Host "Waiting for $Name" -NoNewline
    for ($i = 0; $i -lt $Attempts; $i++) {
        try {
            Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 | Out-Null
            Write-Host ' ready'
            return
        } catch {
            Write-Host '.' -NoNewline
            Start-Sleep -Seconds 2
        }
    }
    Write-Host ''
    docker compose -p $project logs --tail 50
    throw "$Name never became ready at $Url"
}

try {
    # The compose file pins container names and the realm pins the ports, so the
    # development stack and this one cannot both be up. Say so plainly, rather
    # than letting docker report a name conflict.
    $running = docker ps --format '{{.Names}}|{{.Label "com.docker.compose.project"}}'
    $clash = @($running | Where-Object { $_ -like 'bms-*' -and ($_ -split '\|')[1] -ne $project })
    if ($clash.Count -gt 0) {
        $names = ($clash | ForEach-Object { ($_ -split '\|')[0] }) -join ' '
        Write-Host "Another stack is already running: $names"
        Write-Host 'It holds the same container names and ports. Stop it first:'
        Write-Host '  scripts/stop-windows.ps1'
        throw 'The development stack is in the way of the end to end stack.'
    }

    Write-Host "Starting the $project stack."
    docker compose -p $project up --build -d
    if ($LASTEXITCODE -ne 0) { throw 'docker compose failed to start the stack.' }

    Wait-ForUrl 'Keycloak' 'http://localhost:8081/realms/bms/.well-known/openid-configuration'
    Wait-ForUrl 'backend' 'http://localhost:8080/actuator/health'
    Wait-ForUrl 'frontend' 'http://localhost:4200/'

    Set-Location frontend
    if (-not (Test-Path 'node_modules')) { npm ci }
    npx playwright install chromium | Out-Null
    npx playwright test @args
    if ($LASTEXITCODE -ne 0) { throw 'The end to end suite failed.' }
} finally {
    Set-Location (Join-Path $PSScriptRoot '..')
    if ($keepStack) {
        Write-Host "Leaving the $project stack up, because E2E_KEEP_STACK=1."
    } else {
        docker compose -p $project down -v | Out-Null
    }
}
