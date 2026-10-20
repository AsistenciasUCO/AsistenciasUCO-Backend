<#
.SYNOPSIS
    Helper LOCAL para obtener un access token de prueba y ver sus claims decodificados.

.DESCRIPTION
    Pide la password de forma interactiva/segura si no hay una variable local disponible para
    ese username (E2E_DOCENTE_PASSWORD / E2E_ADMIN_PASSWORD). Usa el client frontend
    (Direct Access Grants, ON temporalmente por compatibilidad — ver README) para obtener el
    token via password grant.

    Por default solo imprime claims relevantes decodificados: issuer, audience, idUsuario,
    resource_access.asistencias-api.roles, exp. El access token crudo solo se imprime si se
    pasa -ShowToken explicitamente. El refresh token NUNCA se imprime.

    Este helper existe unicamente para pruebas manuales; no se versiona ningun token.

.EXAMPLE
    .\scripts\get-test-token.ps1 -Username docente.prueba

.EXAMPLE
    .\scripts\get-test-token.ps1 -Username docente.prueba -ShowToken
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Username,
    [switch]$ShowToken,
    [string]$EnvFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path (Join-Path $PSScriptRoot '..') '.env'
}

Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force

$envMap = Import-KcDotEnv -Path $EnvFile

$serverUrl        = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName        = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$apiClientId      = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'

$knownPassword = $null
foreach ($prefix in @('E2E_DOCENTE_', 'E2E_ADMIN_', 'E2E_PAGINACION_300_', 'E2E_PAGINACION_1000_', 'E2E_PAGINACION_5000_')) {
    if ((Get-KcEnvValue -EnvMap $envMap -Key "${prefix}USERNAME") -eq $Username) {
        $knownPassword = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}PASSWORD"
    }
}

if ([string]::IsNullOrWhiteSpace($knownPassword)) {
    $securePassword = Read-Host -Prompt "Password para $Username" -AsSecureString
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
} else {
    $plainPassword = $knownPassword
}

$tokenUrl = "$serverUrl/realms/$realmName/protocol/openid-connect/token"
$bodyPairs = @(
    'grant_type=password',
    "client_id=$(ConvertTo-KcUrlEncoded $frontendClientId)",
    "username=$(ConvertTo-KcUrlEncoded $Username)",
    "password=$(ConvertTo-KcUrlEncoded $plainPassword)"
)
$body = [string]::Join('&', $bodyPairs)
$plainPassword = $null

try {
    $response = Invoke-RestMethod -Method Post -Uri $tokenUrl -ContentType 'application/x-www-form-urlencoded' -Body $body
} catch {
    Write-Host "[ERROR] No fue posible obtener token para $Username. Verifica usuario/password y que $frontendClientId tenga Direct Access Grants habilitado." -ForegroundColor Red
    exit 1
}

$accessToken = $response.access_token
$response = $null
$body = $null

function ConvertFrom-Base64Url {
    param([Parameter(Mandatory = $true)][string]$Value)
    $padded = $Value.Replace('-', '+').Replace('_', '/')
    switch ($padded.Length % 4) {
        2 { $padded += '==' }
        3 { $padded += '=' }
    }
    return [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($padded))
}

$segments = $accessToken.Split('.')
if ($segments.Length -lt 2) {
    Write-Host '[ERROR] El access token recibido no tiene formato JWT valido.' -ForegroundColor Red
    exit 1
}
$claims = ConvertFrom-Base64Url $segments[1] | ConvertFrom-Json

$roles = @()
$resourceAccessProperty = $claims.PSObject.Properties['resource_access']
if ($null -ne $resourceAccessProperty -and $null -ne $resourceAccessProperty.Value) {
    $apiAccessProperty = $resourceAccessProperty.Value.PSObject.Properties[$apiClientId]
    if ($null -ne $apiAccessProperty -and $null -ne $apiAccessProperty.Value) {
        $rolesProperty = $apiAccessProperty.Value.PSObject.Properties['roles']
        if ($null -ne $rolesProperty) {
            $roles = @($rolesProperty.Value)
        }
    }
}

$idUsuario = $null
$idUsuarioProperty = $claims.PSObject.Properties['idUsuario']
if ($null -ne $idUsuarioProperty) {
    $idUsuario = $idUsuarioProperty.Value
}

$audience = @()
$audienceProperty = $claims.PSObject.Properties['aud']
if ($null -ne $audienceProperty) {
    $audience = @($audienceProperty.Value)
}

$expDate = $null
$expProperty = $claims.PSObject.Properties['exp']
if ($null -ne $expProperty -and $null -ne $expProperty.Value) {
    $expDate = [DateTimeOffset]::FromUnixTimeSeconds([long]$expProperty.Value).LocalDateTime
}

Write-Host ''
Write-Host "== Claims :: $Username ==" -ForegroundColor Magenta
Write-Host "issuer                : $($claims.iss)"
Write-Host "audience              : $($audience -join ', ')"
Write-Host "idUsuario             : $idUsuario"
Write-Host "resource_access.$apiClientId.roles : $($roles -join ', ')"
Write-Host "exp                   : $expDate"
Write-Host ''

if ($ShowToken) {
    Write-Host '== Access token (solicitado explicitamente con -ShowToken) ==' -ForegroundColor Yellow
    Write-Host $accessToken
} else {
    Write-Host '(usa -ShowToken para imprimir el access token crudo)' -ForegroundColor DarkGray
}
