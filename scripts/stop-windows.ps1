# Stop the stack. Pass -Wipe to also drop the database volume.
param([switch]$Wipe)

$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

if ($Wipe) {
    docker compose down -v
    Write-Host 'Stack stopped and database volume removed.'
} else {
    docker compose down
    Write-Host 'Stack stopped. Data is kept; run with -Wipe to remove it.'
}
