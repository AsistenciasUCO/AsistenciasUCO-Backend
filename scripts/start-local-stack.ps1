[CmdletBinding()]
param(
    [switch]$Check,
    [switch]$Stop,
    [switch]$NoDependencies,
    [switch]$NoBackend,
    [switch]$NoFrontend,
    [ValidateRange(10, 600)][int]$WaitSeconds = 60,
    [string]$FilesEnvFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = 'C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend'
$dbRoot = 'C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db'
$keycloakRoot = Join-Path $backendRoot 'infra\keycloak'
$filesRoot = Join-Path $backendRoot 'infra\files'
$statePath = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack.json'

if ([string]::IsNullOrWhiteSpace($FilesEnvFile)) {
    $FilesEnvFile = Join-Path $filesRoot '.env'
}

function Test-ListeningPort {
    param([Parameter(Mandatory = $true)][int]$Port)
    try {
        $null = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

function Write-Status {
    param(
        [Parameter(Mandatory = $true)][string]$Kind,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )
    Write-Output ('{0}|{1}|{2}' -f $Kind, $Name, $Value)
}

function Test-RequiredFile {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Test-Path -LiteralPath $Path -PathType Leaf)
}

function Test-FilesEnv {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-RequiredFile -Path $Path)) { return $false }
    $content = Get-Content -LiteralPath $Path -Raw
    foreach ($key in @('MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY')) {
        $pattern = '(?m)^\s*' + [regex]::Escape($key) + '\s*=\s*(.+?)\s*$'
        $match = [regex]::Match($content, $pattern)
        if (-not $match.Success -or [string]::IsNullOrWhiteSpace($match.Groups[1].Value) -or $match.Groups[1].Value -like 'CHANGE_ME*') {
            return $false
        }
    }
    return $true
}

function Wait-ListeningPort {
    param([Parameter(Mandatory = $true)][int]$Port)
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-ListeningPort -Port $Port) { return $true }
        Start-Sleep -Seconds 2
    }
    return (Test-ListeningPort -Port $Port)
}

function Invoke-ComposeUp {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$ComposeFile,
        [Parameter(Mandatory = $true)][string]$EnvFile
    )
    Push-Location $Root
    try {
        & docker compose --env-file $EnvFile --file $ComposeFile up -d
        if ($LASTEXITCODE -ne 0) {
            throw "Compose no pudo iniciar el conjunto en $Root."
        }
    } finally {
        Pop-Location
    }
}

function Save-State {
    param([Parameter(Mandatory = $true)][object[]]$Processes)
    $payload = [pscustomobject]@{
        CreatedAt = (Get-Date).ToUniversalTime().ToString('o')
        Processes = @($Processes)
    }
    $payload | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $statePath -Encoding UTF8
}

function Stop-TrackedProcesses {
    if (-not (Test-RequiredFile -Path $statePath)) {
        Write-Status -Kind 'STOP' -Name 'processes' -Value 'NONE_REGISTERED'
        return
    }

    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $roots = @($state.Processes | ForEach-Object { [int]$_.Pid })
    $processes = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $tracked = New-Object 'System.Collections.Generic.HashSet[int]'
    foreach ($rootPid in $roots) { $null = $tracked.Add($rootPid) }

    $changed = $true
    while ($changed) {
        $changed = $false
        foreach ($process in $processes) {
            if ($tracked.Contains([int]$process.ParentProcessId) -and -not $tracked.Contains([int]$process.ProcessId)) {
                $null = $tracked.Add([int]$process.ProcessId)
                $changed = $true
            }
        }
    }

    foreach ($pid in @($tracked | Sort-Object -Descending)) {
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $statePath -Force -ErrorAction SilentlyContinue
    Write-Status -Kind 'STOP' -Name 'processes' -Value ('STOPPED=' + $tracked.Count)
}

if ($Check -and $Stop) {
    throw 'Use -Check o -Stop, no ambos.'
}

if ($Stop) {
    Stop-TrackedProcesses
    exit 0
}

foreach ($required in @(
        @{ Name = 'backend-root'; Path = $backendRoot },
        @{ Name = 'frontend-root'; Path = $frontendRoot },
        @{ Name = 'db-root'; Path = $dbRoot },
        @{ Name = 'backend-env'; Path = (Join-Path $backendRoot '.env') },
        @{ Name = 'frontend-env'; Path = (Join-Path $frontendRoot '.env') },
        @{ Name = 'keycloak-env'; Path = (Join-Path $keycloakRoot '.env') },
        @{ Name = 'db-env'; Path = (Join-Path $dbRoot '.env') }
    )) {
    Write-Status -Kind 'PATH' -Name $required.Name -Value ($(if (Test-Path -LiteralPath $required.Path) { 'PRESENT' } else { 'MISSING' }))
}
Write-Status -Kind 'PATH' -Name 'files-env' -Value ($(if (Test-RequiredFile -Path $FilesEnvFile) { 'PRESENT' } else { 'MISSING' }))

foreach ($port in @(1433, 8081, 9000, 3310, 8080, 4200)) {
    Write-Status -Kind 'PORT' -Name ([string]$port) -Value ($(if (Test-ListeningPort -Port $port) { 'LISTENING' } else { 'FREE' }))
}

if ($Check) {
    exit 0
}

if (-not $NoDependencies) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI no esta disponible.'
    }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Engine no esta disponible.'
    }

    if (-not (Test-ListeningPort -Port 1433)) {
        Invoke-ComposeUp -Root $dbRoot -ComposeFile (Join-Path $dbRoot 'docker-compose.dev.yml') -EnvFile (Join-Path $dbRoot '.env')
        if (-not (Wait-ListeningPort -Port 1433)) { throw 'SQL Server no abrio el puerto 1433.' }
    }
    if (-not (Test-ListeningPort -Port 8081)) {
        Invoke-ComposeUp -Root $keycloakRoot -ComposeFile (Join-Path $keycloakRoot 'compose.yaml') -EnvFile (Join-Path $keycloakRoot '.env')
        if (-not (Wait-ListeningPort -Port 8081)) { throw 'Keycloak no abrio el puerto 8081.' }
    }
    if (-not (Test-ListeningPort -Port 9000) -or -not (Test-ListeningPort -Port 3310)) {
        if (-not (Test-FilesEnv -Path $FilesEnvFile)) {
            throw 'Falta infra/files/.env local con credenciales MinIO validas; no se generan ni se imprimen secretos.'
        }
        Invoke-ComposeUp -Root $filesRoot -ComposeFile (Join-Path $filesRoot 'compose.yaml') -EnvFile $FilesEnvFile
        if (-not (Wait-ListeningPort -Port 9000) -or -not (Wait-ListeningPort -Port 3310)) { throw 'MinIO/ClamAV no abrieron sus puertos oficiales.' }
    }
}

$started = @()
if (-not $NoBackend -and -not (Test-ListeningPort -Port 8080)) {
    $backendLog = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-backend.out.log'
    $backendErr = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-backend.err.log'
    $backendRunner = Join-Path $backendRoot 'scripts\run-local.ps1'
    $process = Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $backendRunner) -WorkingDirectory $backendRoot -WindowStyle Hidden -RedirectStandardOutput $backendLog -RedirectStandardError $backendErr -PassThru
    $started += [pscustomobject]@{ Name = 'backend'; Pid = $process.Id; Log = $backendLog }
    Save-State -Processes $started
    if (-not (Wait-ListeningPort -Port 8080)) { throw 'Backend no abrio el puerto 8080; consultar logs/backend.out.log.' }
}

if (-not $NoFrontend -and -not (Test-ListeningPort -Port 4200)) {
    $frontendLog = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-frontend.out.log'
    $frontendErr = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-frontend.err.log'
    $process = Start-Process -FilePath 'npm.cmd' -ArgumentList @('start', '--', '--port', '4200') -WorkingDirectory $frontendRoot -WindowStyle Hidden -RedirectStandardOutput $frontendLog -RedirectStandardError $frontendErr -PassThru
    $started += [pscustomobject]@{ Name = 'frontend'; Pid = $process.Id; Log = $frontendLog }
    Save-State -Processes $started
    if (-not (Wait-ListeningPort -Port 4200)) { throw 'Frontend no abrio el puerto 4200; consultar el log temporal reportado.' }
}

if ($started.Count -eq 0) {
    Write-Status -Kind 'STACK' -Name 'processes' -Value 'REUSED_EXISTING'
} else {
    foreach ($item in $started) { Write-Status -Kind 'PROCESS' -Name $item.Name -Value ('STARTED_PID=' + $item.Pid) }
}
Write-Status -Kind 'LOG' -Name 'backend' -Value (Join-Path $backendRoot 'logs\backend.out.log')
Write-Status -Kind 'LOG' -Name 'frontend' -Value (Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-frontend.out.log')
Write-Status -Kind 'STACK' -Name 'result' -Value 'READY'
