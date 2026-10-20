<#!
.SYNOPSIS
    Valida HTTP real de los tres docentes E2E de paginacion.

.DESCRIPTION
    Usa passwords del .env local, obtiene tokens por OIDC sin imprimirlos y valida horarios,
    estudiantes, sesiones v2, ownership y el directorio con la cuenta COORDINADOR existente.
    No crea usuarios ni modifica la DB.
#>

[CmdletBinding()]
param(
    [string]$EnvFile,
    [string]$BackendBaseUrl = 'http://127.0.0.1:8082'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($EnvFile)) { $EnvFile = Join-Path (Join-Path $PSScriptRoot '..\..\infra\keycloak') '.env' }
Import-Module (Join-Path (Split-Path $EnvFile) 'scripts\lib\KeycloakAdmin.psm1') -Force
$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'

$specs = @(
    @{ Size = 300; Username = 'docente.paginacion300'; PasswordKey = 'E2E_PAGINACION_300_PASSWORD'; GroupId = 'DE000005-0000-4000-8000-000000000001'; Expected = 300 },
    @{ Size = 1000; Username = 'docente.paginacion1000'; PasswordKey = 'E2E_PAGINACION_1000_PASSWORD'; GroupId = 'DE000005-0000-4000-8000-000000000002'; Expected = 1000 },
    @{ Size = 5000; Username = 'docente.paginacion5000'; PasswordKey = 'E2E_PAGINACION_5000_PASSWORD'; GroupId = 'DE000005-0000-4000-8000-000000000003'; Expected = 5000 }
)

function Get-Token {
    param([Parameter(Mandatory = $true)][string]$Username, [Parameter(Mandatory = $true)][string]$Password)
    $body = @('grant_type=password', "client_id=$(ConvertTo-KcUrlEncoded $frontendClientId)", "username=$(ConvertTo-KcUrlEncoded $Username)", "password=$(ConvertTo-KcUrlEncoded $Password)") -join '&'
    $response = Invoke-RestMethod -Method Post -Uri "$serverUrl/realms/$realmName/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body $body
    if ([string]::IsNullOrWhiteSpace($response.access_token)) { throw "Keycloak no devolvio token para $Username." }
    return [string]$response.access_token
}

function Invoke-Api {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$Token)
    try {
        $response = Invoke-WebRequest -Method Get -Uri "$BackendBaseUrl$Path" -Headers @{ Authorization = "Bearer $Token" } -UseBasicParsing
        $body = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        return [pscustomobject]@{ Status = [int]$response.StatusCode; Body = $body }
    } catch {
        $status = 0
        if ($_.Exception.Response) { try { $status = [int]$_.Exception.Response.StatusCode } catch { } }
        return [pscustomobject]@{ Status = $status; Body = $null }
    }
}

function Get-CollectionItems {
    param($Body)
    if ($null -eq $Body) { return @() }
    if ($null -ne $Body.PSObject.Properties['datos']) { return @($Body.datos) }
    if ($null -ne $Body.PSObject.Properties['items']) { return @($Body.items) }
    return @()
}

function Assert-Result {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "E2E_VALIDATION_FAILED: $Message" }
}

$tokens = @{}
foreach ($spec in $specs) {
    $password = Get-KcRequiredEnvValue -EnvMap $envMap -Key $spec.PasswordKey
    $tokens[$spec.Username] = Get-Token -Username $spec.Username -Password $password
    $schedule = Invoke-Api -Path '/api/v1/docente/horarios' -Token $tokens[$spec.Username]
    $scheduleItems = @(Get-CollectionItems -Body $schedule.Body)
    $groups = @($scheduleItems | ForEach-Object { [string]$_.idGrupo.ToLowerInvariant() } | Sort-Object -Unique)
    Assert-Result ($schedule.Status -eq 200 -and $scheduleItems.Count -eq 1 -and $groups[0] -eq $spec.GroupId.ToLowerInvariant()) "$($spec.Username) horarios status/ownership"
    $students = Invoke-Api -Path "/api/v1/grupos/$($spec.GroupId)/estudiantes" -Token $tokens[$spec.Username]
    $studentItems = @(Get-CollectionItems -Body $students.Body)
    Assert-Result ($students.Status -eq 200 -and $studentItems.Count -eq $spec.Expected) "$($spec.Username) estudiantes expected=$($spec.Expected)"
    $sessions = Invoke-Api -Path "/api/v2/sesiones/grupo/$($spec.GroupId)" -Token $tokens[$spec.Username]
    $sessionItems = @(Get-CollectionItems -Body $sessions.Body)
    $utcSessions = @($sessionItems | Where-Object { [string]$_.procedenciaTemporal -eq 'UTC_V2' })
    $indeterminateSessions = @($sessionItems | Where-Object { [string]::IsNullOrWhiteSpace([string]$_.procedenciaTemporal) })
    $unexpectedProvenance = @($sessionItems | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_.procedenciaTemporal) -and [string]$_.procedenciaTemporal -ne 'UTC_V2' })
    Assert-Result ($sessions.Status -eq 200 -and $utcSessions.Count -ge 4 -and $unexpectedProvenance.Count -eq 0) "$($spec.Username) sesiones v2"
    Write-Output "DOCENTE|$($spec.Size)|horarios=200/1|estudiantes=200/$($studentItems.Count)|sesionesV2=200/$($sessionItems.Count)|utcV2=$($utcSessions.Count)|indeterminadas=$($indeterminateSessions.Count)"
}

$cross = Invoke-Api -Path '/api/v2/sesiones/grupo/DE000005-0000-4000-8000-000000000002' -Token $tokens['docente.paginacion300']
Assert-Result ($cross.Status -eq 403) 'ownership cruzado de sesiones v2'
Write-Output 'OWNERSHIP|docente.paginacion300->FIX-PAG-1000|403'

$coordinatorUsername = Get-KcEnvValue -EnvMap $envMap -Key 'E2E_COORDINADOR_USERNAME'
$coordinatorPassword = Get-KcEnvValue -EnvMap $envMap -Key 'E2E_COORDINADOR_PASSWORD'
if ([string]::IsNullOrWhiteSpace($coordinatorUsername) -or [string]::IsNullOrWhiteSpace($coordinatorPassword)) {
    throw 'E2E_VALIDATION_BLOCKED: falta la cuenta COORDINADOR local para el directorio.'
}
$coordinatorToken = Get-Token -Username $coordinatorUsername -Password $coordinatorPassword
$directory = Invoke-Api -Path '/api/v1/estudiantes?page=0&size=20' -Token $coordinatorToken
$directoryItems = @(Get-CollectionItems -Body $directory.Body)
$totalItems = if ($directory.Body -and $directory.Body.PSObject.Properties['totalItems']) { [int]$directory.Body.totalItems } else { -1 }
Assert-Result ($directory.Status -eq 200 -and $directoryItems.Count -eq 20 -and $totalItems -eq 5003) 'directorio coordinador page=0 size=20'
Write-Output "DIRECTORIO_COORDINADOR|200/items=$($directoryItems.Count)|totalItems=$totalItems"

$docenteDirectory = Invoke-Api -Path '/api/v1/estudiantes?page=0&size=20' -Token $tokens['docente.paginacion5000']
Assert-Result ($docenteDirectory.Status -eq 403) 'DOCENTE no debe acceder al directorio general'
Write-Output 'DIRECTORIO_DOCENTE|403'
Write-Output 'E2E_VALIDATION_OK|No se imprimieron tokens, passwords ni datos personales.'
