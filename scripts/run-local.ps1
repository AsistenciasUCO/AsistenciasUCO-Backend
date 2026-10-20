[CmdletBinding()]
param(
    [switch]$Help,
    [ValidateRange(0, 3650)][int]$RetentionDays = 7,
    [ValidateRange(0, 10000)][int]$MaxHistoricalFiles = 20,
    [ValidateRange(1, 4096)][int]$MaxHistoricalSizeMb = 25,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArguments
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Help) {
    @'
Uso: .\scripts\run-local.ps1 [-RetentionDays N] [-MaxHistoricalFiles N] [-MaxHistoricalSizeMb N] [argumentos Maven]

Ejecuta Spring Boot desde la raiz del backend. Los logs activos quedan en
logs\backend.out.log y logs\backend.err.log; al iniciar una nueva ejecucion
se rotan y se aplica retencion por antiguedad, cantidad y tamano.
'@ | Write-Output
    exit 0
}

$backendRoot = Split-Path -Parent $PSScriptRoot
$loggingScript = Join-Path $PSScriptRoot 'lib\local-logging.ps1'
. $loggingScript

$logPaths = Initialize-LocalLogDirectory `
    -RootPath $backendRoot `
    -Prefix 'backend' `
    -RetentionDays $RetentionDays `
    -MaxHistoricalFiles $MaxHistoricalFiles `
    -MaxHistoricalSizeMb $MaxHistoricalSizeMb

. (Join-Path $PSScriptRoot 'load-env.ps1')

$mavenWrapper = Join-Path $backendRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $mavenWrapper -PathType Leaf)) {
    throw "No existe el wrapper Maven esperado: $mavenWrapper"
}

$arguments = @('spring-boot:run')
if ($null -ne $MavenArguments) {
    $arguments += $MavenArguments
}

Push-Location $backendRoot
try {
    $process = Start-Process `
        -FilePath $mavenWrapper `
        -ArgumentList $arguments `
        -WorkingDirectory $backendRoot `
        -RedirectStandardOutput $logPaths.Out `
        -RedirectStandardError $logPaths.Err `
        -NoNewWindow `
        -Wait `
        -PassThru
    $exitCode = $process.ExitCode
    if (Test-Path -LiteralPath $logPaths.Out -PathType Leaf) {
        Get-Content -LiteralPath $logPaths.Out
    }
} finally {
    Pop-Location
}

Invoke-LocalLogRetention `
    -Directory $logPaths.Directory `
    -Prefix $logPaths.Prefix `
    -RetentionDays $RetentionDays `
    -MaxHistoricalFiles $MaxHistoricalFiles `
    -MaxHistoricalSizeMb $MaxHistoricalSizeMb

exit $exitCode
