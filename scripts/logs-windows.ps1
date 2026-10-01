# Read the stack's logs, on Windows.
#
#   scripts/logs-windows.ps1                   follow every service, live
#   scripts/logs-windows.ps1 backend           follow one service
#   scripts/logs-windows.ps1 -Save             write every service's logs to logs/<date-time>.log
#   scripts/logs-windows.ps1 backend -Save     the same, for one service
#   scripts/logs-windows.ps1 -Viewer           start the web viewer
#   scripts/logs-windows.ps1 -ViewerOff        stop it
param(
    [string]$Service,
    [switch]$Save,
    [switch]$Viewer,
    [switch]$ViewerOff
)

$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

$services = @($Service | Where-Object { $_ })

# Docker writes UTF-8; read it as such, or an umlaut in a log line is garbled.
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

if ($Save) {
    New-Item -ItemType Directory -Force 'logs' | Out-Null
    $suffix = if ($Service) { "-$Service" } else { '' }
    $file = Join-Path (Get-Location) "logs/$(Get-Date -Format 'yyyyMMdd-HHmmss')$suffix.log"
    $lines = docker compose logs --no-color --timestamps @services
    # UTF-8 without the byte order mark Out-File would add on Windows PowerShell.
    [IO.File]::WriteAllLines($file, [string[]]$lines)
    Write-Host "Saved to $file"
} elseif ($Viewer) {
    docker compose --profile logs up -d logs
    # The port Docker published, which LOGS_PORT in .env may have moved.
    Write-Host "Logs viewer at http://$(docker compose --profile logs port logs 8080)"
} elseif ($ViewerOff) {
    docker compose --profile logs rm --stop --force logs
} else {
    docker compose logs --follow --tail 100 @services
}
