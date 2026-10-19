# Wrapper hacia scripts/backend/run-local.ps1
$ErrorActionPreference = "Stop"
& "$PSScriptRoot\..\..\scripts\backend\run-local.ps1" @args
