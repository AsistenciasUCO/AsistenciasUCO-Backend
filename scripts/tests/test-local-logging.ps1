[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$testRoot = Join-Path ([IO.Path]::GetTempPath()) ("asistenciasuco-log-tests-" + [guid]::NewGuid().ToString('N'))
$loggingScript = Join-Path $PSScriptRoot '..\lib\local-logging.ps1'
. $loggingScript

function Assert-True {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

try {
    New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
    $paths = Initialize-LocalLogDirectory -RootPath $testRoot -RetentionDays 30 -MaxHistoricalFiles 3 -MaxHistoricalSizeMb 1
    Assert-True (Test-Path -LiteralPath $paths.Directory -PathType Container) 'La carpeta de logs no fue creada.'

    Set-Content -LiteralPath $paths.Out -Value 'first output' -NoNewline
    Set-Content -LiteralPath $paths.Err -Value 'first error' -NoNewline
    Set-Content -LiteralPath (Join-Path $paths.Directory 'keep.txt') -Value 'unrelated' -NoNewline

    $second = Initialize-LocalLogDirectory -RootPath $testRoot -RetentionDays 30 -MaxHistoricalFiles 3 -MaxHistoricalSizeMb 1
    $historical = @(Get-ChildItem -LiteralPath $second.Directory -File -Filter 'backend-*.log')
    Assert-True ($historical.Count -eq 2) 'La segunda ejecución no rotó ambos logs activos.'
    Assert-True (Test-Path -LiteralPath (Join-Path $second.Directory 'keep.txt') -PathType Leaf) 'La retención tocó un archivo no administrado.'

    foreach ($index in 1..5) {
        $file = Join-Path $second.Directory ("backend-20260101-00000{0}-out.log" -f $index)
        Set-Content -LiteralPath $file -Value ('x' * 2048) -NoNewline
        (Get-Item -LiteralPath $file).LastWriteTime = (Get-Date).AddDays(-$index)
    }
    Invoke-LocalLogRetention -Directory $second.Directory -Prefix 'backend' -RetentionDays 30 -MaxHistoricalFiles 2 -MaxHistoricalSizeMb 1
    $afterRetention = @(Get-ChildItem -LiteralPath $second.Directory -File -Filter 'backend-*.log')
    Assert-True ($afterRetention.Count -le 2) 'La retención no limitó la cantidad de históricos.'
    Assert-True (-not (Get-ChildItem -LiteralPath $second.Directory -File -Filter 'backend-*.log' | Where-Object { $_.Length -gt 1MB })) 'La retención dejó un histórico sobredimensionado.'

    $runnerRoot = Join-Path $testRoot 'runner'
    $runnerScripts = Join-Path $runnerRoot 'scripts'
    New-Item -ItemType Directory -Path (Join-Path $runnerScripts 'lib') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot '..\run-local.ps1') -Destination (Join-Path $runnerScripts 'run-local.ps1')
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot '..\load-env.ps1') -Destination (Join-Path $runnerScripts 'load-env.ps1')
    Copy-Item -LiteralPath $loggingScript -Destination (Join-Path $runnerScripts 'lib\local-logging.ps1')
    Set-Content -LiteralPath (Join-Path $runnerRoot '.env') -Value 'TEST_LOG_RUNNER=1' -NoNewline
    Set-Content -LiteralPath (Join-Path $runnerRoot 'mvnw.cmd') -Value "@echo off`necho fake output`n1>&2 echo fake error`nexit /b 7" -NoNewline
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $runnerOutput = & powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass `
            -File (Join-Path $runnerScripts 'run-local.ps1') 2>&1 | Out-String
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $runnerExitCode = $LASTEXITCODE
    Assert-True ($runnerExitCode -eq 7) "El runner no propagó el exit code Maven: $runnerExitCode ($runnerOutput)"
    $runnerLogs = Get-LocalLogPaths -RootPath $runnerRoot
    Assert-True ((Get-Content -LiteralPath $runnerLogs.Out -Raw).Contains('fake output')) 'El stdout no quedó centralizado.'
    Assert-True ((Get-Content -LiteralPath $runnerLogs.Err -Raw).Contains('fake error')) 'El stderr no quedó centralizado.'

    Write-Output 'local-logging tests: passed=5 failed=0'
    exit 0
} finally {
    if (Test-Path -LiteralPath $testRoot -PathType Container) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
}

