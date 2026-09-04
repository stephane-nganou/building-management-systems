# Point git at the hooks kept in the repository, so everyone runs the same ones.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
git config core.hooksPath .githooks
Write-Host 'Hooks installed. The end to end suite now runs before every push.'
