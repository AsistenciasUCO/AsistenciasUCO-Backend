[CmdletBinding()]
param(
    [switch]$Check,
    [switch]$Stop,
    [switch]$NoDependencies,
    [switch]$NoBackend,
    [switch]$NoFrontend,
    [ValidateSet('baseline', 'legacy')][string]$Provider = 'baseline',
    [ValidateRange(10, 600)][int]$WaitSeconds = 60,
    [string]$FilesEnvFile,
    [string]$FrontendRoot,
    [string]$DbRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Rutas oficiales de los repositorios hermanos. Prioridad: parametro, variable de entorno, ruta oficial.
# Se valida que la carpeta sea un repositorio Git con su archivo marcador y que su nombre no sea un clon,
# laboratorio, cuarentena o copia; no se buscan carpetas alternativas.
function Resolve-WorkspaceRoot {
    param(
        [string]$Explicit,
        [Parameter(Mandatory = $true)][string]$EnvironmentVariable,
        [Parameter(Mandatory = $true)][string]$OfficialPath,
        [Parameter(Mandatory = $true)][string]$Marker
    )
    $candidate = $Explicit
    if ([string]::IsNullOrWhiteSpace($candidate)) { $candidate = [Environment]::GetEnvironmentVariable($EnvironmentVariable) }
    if ([string]::IsNullOrWhiteSpace($candidate)) { $candidate = $OfficialPath }
    $canonicalPath = [System.IO.Path]::GetFullPath($candidate).TrimEnd('\', '/')
    $officialCanonicalPath = [System.IO.Path]::GetFullPath($OfficialPath).TrimEnd('\', '/')
    if (-not [string]::Equals($canonicalPath, $officialCanonicalPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Ruta fuera de los tres repositorios oficiales para ${EnvironmentVariable}: $candidate. Ruta permitida: $OfficialPath"
    }
    if ($candidate -match '(?i)(^|[\\/_.-])(lab|sandbox|quarantine|cuarentena|snapshot|recovery|backup|temporary|temp|copy|clone)([\\/_.-]|$)') {
        throw "Ruta no permitida para ${EnvironmentVariable}: parece un clon o laboratorio ($candidate). Use la ruta oficial: $OfficialPath"
    }
    if ((Test-Path -LiteralPath $candidate -PathType Container) -and
        -not ((Test-Path -LiteralPath (Join-Path $candidate '.git')) -and (Test-Path -LiteralPath (Join-Path $candidate $Marker) -PathType Leaf))) {
        throw "La ruta $candidate no es el repositorio oficial esperado (falta .git o $Marker)."
    }
    return $candidate
}

$backendRoot = Split-Path -Parent $PSScriptRoot
$documents = Join-Path $env:USERPROFILE 'OneDrive\Documentos'
$frontendRoot = Resolve-WorkspaceRoot -Explicit $FrontendRoot -EnvironmentVariable 'ASISTENCIAS_FRONTEND_ROOT' `
    -OfficialPath (Join-Path $documents 'Front_Asistecias\AsistenciasUCO-Frontend') -Marker 'package.json'
$dbRoot = Resolve-WorkspaceRoot -Explicit $DbRoot -EnvironmentVariable 'ASISTENCIAS_DB_ROOT' `
    -OfficialPath (Join-Path $documents 'AsisteciaUco_db\git\gestion-asistencia-db') -Marker 'docker-compose.dev.yml'
$keycloakRoot = Join-Path $backendRoot 'infra\keycloak'
$filesRoot = Join-Path $backendRoot 'infra\files'
$securityLabRoot = Join-Path $backendRoot 'infra\security-baseline'
$labEnvFile = Join-Path $env:USERPROFILE '.asistencias-security-lab\lab.env'
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
        # Los puertos publicados por Docker Desktop no siempre aparecen como Listen: se confirma con una conexion TCP.
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $pending = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
            return ($pending.AsyncWaitHandle.WaitOne(500) -and $client.Connected)
        } catch {
            return $false
        } finally {
            $client.Close()
        }
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

    foreach ($trackedPid in @($tracked | Sort-Object -Descending)) {
        Stop-Process -Id $trackedPid -Force -ErrorAction SilentlyContinue
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
        @{ Name = 'db-env'; Path = (Join-Path $dbRoot '.env') },
        $(if ($Provider -eq 'baseline') { @{ Name = 'security-lab-env'; Path = $labEnvFile } } else { @{ Name = 'keycloak-env'; Path = (Join-Path $keycloakRoot '.env') } })
    )) {
    Write-Status -Kind 'PATH' -Name $required.Name -Value ($(if (Test-Path -LiteralPath $required.Path) { 'PRESENT' } else { 'MISSING' }))
}
Write-Status -Kind 'PATH' -Name 'files-env' -Value ($(if (Test-RequiredFile -Path $FilesEnvFile) { 'PRESENT' } else { 'MISSING' }))

Write-Status -Kind 'SECURITY' -Name 'provider' -Value $Provider
$securityPorts = if ($Provider -eq 'baseline') { @(19090, 18080, 18081) } else { @(8081) }
foreach ($port in (@(1433) + $securityPorts + @(9000, 3310, 3025, 8080, 4200))) {
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
        & docker start sql_server_asistencias | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "No se pudo iniciar SQL Server original." }
        if (-not (Wait-ListeningPort -Port 1433)) { throw 'SQL Server no abrio el puerto 1433.' }
    }
    if ($Provider -eq 'baseline') {
        if (-not (Test-RequiredFile -Path $labEnvFile)) {
            throw "Falta $labEnvFile (Security Baseline); no se generan ni se imprimen secretos."
        }
        if (-not (Test-ListeningPort -Port 19090) -or -not (Test-ListeningPort -Port 18080) -or -not (Test-ListeningPort -Port 18081)) {
            Invoke-ComposeUp -Root $securityLabRoot -ComposeFile (Join-Path $securityLabRoot 'compose.yaml') -EnvFile $labEnvFile
            foreach ($securityPort in $securityPorts) {
                if (-not (Wait-ListeningPort -Port $securityPort)) { throw "Security Baseline no abrio el puerto $securityPort." }
            }
        }
    } elseif (-not (Test-ListeningPort -Port 8081)) {
        # Alternativa explicita (-Provider legacy): Keycloak legacy, nunca un fallback automatico.
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
    if (-not (Test-ListeningPort -Port 3025)) {
        # SMTP de pruebas (GreenMail): el outbox SMTP del backend necesita un servidor que responda; no entrega a nadie.
        Push-Location (Join-Path $backendRoot 'infra\mail')
        try {
            & docker compose --file compose.yaml up -d
            if ($LASTEXITCODE -ne 0) { throw 'Compose no pudo iniciar GreenMail.' }
        } finally {
            Pop-Location
        }
        if (-not (Wait-ListeningPort -Port 3025)) { throw 'GreenMail no abrio el puerto SMTP 3025.' }
    }
}

$started = @()
if (-not $NoBackend -and -not (Test-ListeningPort -Port 8080)) {
    $backendLog = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-backend.out.log'
    $backendErr = Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-backend.err.log'
    # Un solo selector (APP_ADAPTERS_SECURITY_PROVIDER) gobierna seguridad e identidad; lo fija el launcher.
    $backendRunner = Join-Path $securityLabRoot 'scripts\start-backend-baseline.ps1'
    $process = Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $backendRunner, '-Provider', $Provider) -WorkingDirectory $backendRoot -WindowStyle Hidden -RedirectStandardOutput $backendLog -RedirectStandardError $backendErr -PassThru
    $started += [pscustomobject]@{ Name = 'backend'; Pid = $process.Id; Log = $backendLog }
    Save-State -Processes $started
    if (-not (Wait-ListeningPort -Port 8080)) { throw 'Backend no abrio el puerto 8080; consultar el log temporal reportado.' }
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
Write-Status -Kind 'LOG' -Name 'backend' -Value (Join-Path $backendRoot 'infra\observability\logs')
Write-Status -Kind 'LOG' -Name 'frontend' -Value (Join-Path ([System.IO.Path]::GetTempPath()) 'asistencias-uco-local-stack-frontend.out.log')
Write-Status -Kind 'STACK' -Name 'result' -Value 'READY'
