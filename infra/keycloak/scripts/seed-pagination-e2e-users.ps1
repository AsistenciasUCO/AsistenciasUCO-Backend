<#!
.SYNOPSIS
    Crea/verifica exactamente tres usuarios DOCENTE E2E para FIX-PAG-300/1000/5000.

.DESCRIPTION
    Reutiliza KeycloakAdmin.psm1, no ejecuta bootstrap del realm y no toca docente.prueba.
    Las passwords deben existir en el .env local como E2E_PAGINACION_<size>_PASSWORD o se
    pueden introducir interactivamente con -PromptForMissingPasswords. Nunca se imprimen.
    Para un usuario existente, un idUsuario conflictivo detiene la corrida: no se sobreescribe.
#>

[CmdletBinding()]
param(
    [string]$EnvFile,
    [switch]$ResetExistingPasswords,
    [switch]$PromptForMissingPasswords
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($EnvFile)) { $EnvFile = Join-Path (Join-Path $PSScriptRoot '..') '.env' }
Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force
$envMap = Import-KcDotEnv -Path $EnvFile
$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$apiClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$expectedIssuer = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_ISSUER_URI' -Default "$serverUrl/realms/$realmName"
$expectedAudience = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_EXPECTED_AUDIENCE' -Default $apiClientId
$passwordMinLength = [int](Get-KcEnvValue -EnvMap $envMap -Key 'KC_PASSWORD_MIN_LENGTH' -Default '12')
$adminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$adminPassword = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'
$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $adminUser -Password $adminPassword
$apiClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $apiClientId
$frontendClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $frontendClientId
if ($null -eq $apiClient -or $null -eq $frontendClient) { throw 'Los clients asistencias-api/asistencias-uco-frontend son obligatorios; no se ejecuta bootstrap automatico.' }
$docenteRole = Find-KcClientRole -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientUuid $apiClient.id -RoleName 'DOCENTE'
if ($null -eq $docenteRole) { throw 'El client role DOCENTE no existe; ejecutar el bootstrap autorizado antes del seed.' }

$specs = @(
    @{ Size = 300; Key = 'PAGINACION_300'; Username = 'docente.paginacion300'; IdUsuario = 'DE000003-0000-4000-8000-000000000001' },
    @{ Size = 1000; Key = 'PAGINACION_1000'; Username = 'docente.paginacion1000'; IdUsuario = 'DE000003-0000-4000-8000-000000000002' },
    @{ Size = 5000; Key = 'PAGINACION_5000'; Username = 'docente.paginacion5000'; IdUsuario = 'DE000003-0000-4000-8000-000000000003' }
)

function ConvertFrom-Base64Url {
    param([Parameter(Mandatory = $true)][string]$Value)
    $padded = $Value.Replace('-', '+').Replace('_', '/')
    switch ($padded.Length % 4) { 2 { $padded += '==' } 3 { $padded += '=' } }
    return [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($padded)) | ConvertFrom-Json
}

function Get-PasswordForSpec {
    param([Parameter(Mandatory = $true)]$Spec)
    $key = "E2E_$($Spec.Key)_PASSWORD"
    $password = Get-KcEnvValue -EnvMap $envMap -Key $key
    if ([string]::IsNullOrWhiteSpace($password) -and $PromptForMissingPasswords) {
        $secure = Read-Host -Prompt "Password local para $($Spec.Username)" -AsSecureString
        $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
    }
    if ([string]::IsNullOrWhiteSpace($password)) { throw "Falta $key en el .env local; no se crea la cuenta $($Spec.Username)." }
    if ($password.Length -lt $passwordMinLength -or $password -cnotmatch '[a-z]' -or $password -cnotmatch '[A-Z]' -or $password -notmatch '[0-9]' -or $password -notmatch '[^A-Za-z0-9]' -or $password -eq $Spec.Username) {
        throw "La password local de $($Spec.Username) no cumple la politica configurada; no se imprime ni se persiste."
    }
    return $password
}

function Get-TokenClaims {
    param([Parameter(Mandatory = $true)][string]$Username, [Parameter(Mandatory = $true)][string]$Password)
    $body = @(
        'grant_type=password',
        "client_id=$(ConvertTo-KcUrlEncoded $frontendClientId)",
        "username=$(ConvertTo-KcUrlEncoded $Username)",
        "password=$(ConvertTo-KcUrlEncoded $Password)"
    ) -join '&'
    try { $response = Invoke-RestMethod -Method Post -Uri "$serverUrl/realms/$realmName/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body $body }
    catch { throw "No fue posible obtener el token de $Username; verifica la password local y Direct Access Grants." }
    if ([string]::IsNullOrWhiteSpace($response.access_token)) { throw "Keycloak no devolvio access_token para $Username." }
    $segments = ([string]$response.access_token).Split('.')
    if ($segments.Count -lt 2) { throw "El token de $Username no tiene formato JWT." }
    return ConvertFrom-Base64Url -Value $segments[1]
}

function Get-ApiRoles {
    param([Parameter(Mandatory = $true)]$Claims)
    $ra = $Claims.PSObject.Properties['resource_access']
    if ($null -eq $ra -or $null -eq $ra.Value) { return @() }
    $api = $ra.Value.PSObject.Properties[$apiClientId]
    if ($null -eq $api -or $null -eq $api.Value) { return @() }
    $roles = $api.Value.PSObject.Properties['roles']
    if ($null -eq $roles) { return @() }
    return @($roles.Value)
}

function Set-OnlyDocenteClientRole {
    param([Parameter(Mandatory = $true)][string]$UserId, [Parameter(Mandatory = $true)][string]$Username)
    $path = "/admin/realms/$realmName/users/$UserId/role-mappings/clients/$($apiClient.id)"
    $current = @((Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path $path -Token $adminToken).Body)
    foreach ($role in @($current | Where-Object { $_.name -ne 'DOCENTE' })) {
        Invoke-KcAdminApi -Method Delete -ServerUrl $serverUrl -Path $path -Token $adminToken -Body @($role) | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Usuario ${Username}: role API removido ($($role.name))"
    }
    $currentRoleNames = @($current | ForEach-Object { $_.name })
    if (-not ($currentRoleNames -contains 'DOCENTE')) {
        Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path $path -Token $adminToken -Body @($docenteRole) | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Usuario ${Username}: role API DOCENTE asignado"
    }
}

Write-Host "== Seed E2E paginacion :: realm $realmName ==" -ForegroundColor Magenta
foreach ($spec in $specs) {
    [void][Guid]::Parse($spec.IdUsuario)
    $password = Get-PasswordForSpec -Spec $spec
    $email = "e2e.paginacion.$($spec.Size)@example-e2e.uco.edu.co"
    $user = Find-KcUserByUsername -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -Username $spec.Username
    $isNew = $false
    if ($null -eq $user) {
        $created = Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users" -Token $adminToken -Body @{ username=$spec.Username; email=$email; firstName='Docente'; lastName="Paginacion$($spec.Size)"; enabled=$true; emailVerified=$true; attributes=@{ idUsuario=@($spec.IdUsuario) } }
        $userId = ($created.Location -split '/')[-1]
        $isNew = $true
        Write-KcResult -Status 'CREATED' -Message "Usuario $($spec.Username)"
    } else {
        $userId = [string]$user.id
        $existing = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId" -Token $adminToken).Body
        $attrs = $existing.PSObject.Properties['attributes']
        $currentId = $null
        if ($null -ne $attrs -and $null -ne $attrs.Value -and $attrs.Value.PSObject.Properties['idUsuario']) { $currentId = [string](@($attrs.Value.idUsuario)[0]) }
        if (-not [string]::IsNullOrWhiteSpace($currentId) -and $currentId -ne $spec.IdUsuario) { throw "Usuario $($spec.Username) ya tiene un idUsuario distinto; no se sobrescribe." }
        if ($currentId -ne $spec.IdUsuario) { $existing.attributes = @{ idUsuario=@($spec.IdUsuario) }; Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId" -Token $adminToken -Body $existing | Out-Null; Write-KcResult -Status 'UPDATED' -Message "Usuario $($spec.Username): idUsuario reconciliado" }
        Write-KcResult -Status 'OK' -Message "Usuario $($spec.Username) existente"
    }
    if ($isNew -or $ResetExistingPasswords) {
        Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId/reset-password" -Token $adminToken -Body @{ type='password'; value=$password; temporary=$false } | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Usuario $($spec.Username): password local establecida"
    } else { Write-KcResult -Status 'SKIPPED' -Message "Usuario $($spec.Username): password existente preservada" }
    Set-OnlyDocenteClientRole -UserId $userId -Username $spec.Username
    $claims = Get-TokenClaims -Username $spec.Username -Password $password
    $aud = @($claims.aud)
    $roles = @(Get-ApiRoles -Claims $claims)
    if ($claims.iss -ne $expectedIssuer -or $aud -notcontains $expectedAudience -or [string]$claims.idUsuario -ne $spec.IdUsuario -or $roles.Count -ne 1 -or $roles[0] -ne 'DOCENTE') { throw "Token contractual invalido para $($spec.Username): issuer/audiencia/idUsuario/rol no coinciden." }
    if ([string]$claims.sub -eq $spec.IdUsuario) { throw "El sub de $($spec.Username) coincide con idUsuario; se esperaba el UUID propio de Keycloak." }
    Write-KcResult -Status 'OK' -Message "Usuario $($spec.Username): token verificado (idUsuario, sub propio, aud=$expectedAudience, role=DOCENTE)"
}
Write-Host 'Seed E2E de paginacion completado y verificado. No se imprimieron passwords ni JWTs.' -ForegroundColor Green
