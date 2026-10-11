<#
.SYNOPSIS
    Crea/verifica usuarios E2E opcionales (DOCENTE, ADMIN, COORDINADOR, ESTUDIANTE) para
    pruebas manuales de AsistenciasUCO.

.DESCRIPTION
    NO forma parte del realm base (realm-import/asistencias-uco-realm.json). Lee los datos
    exclusivamente desde infra/keycloak/.env. Si falta E2E_<ROL>_USERNAME o
    E2E_<ROL>_ID_USUARIO para un usuario, ese usuario se omite (nunca se inventa un UUID
    institucional).

    Los UUID E2E deben corresponder a dbo.Usuario.id REALES en SQL Server cuando se quiera
    probar autorizacion contextual. El script reconcilia de forma idempotente email, nombre,
    estado e idUsuario para los usuarios E2E existentes, sin tocar otros atributos.

    No resetea la password de un usuario E2E ya existente salvo que
    KC_RESET_EXISTING_E2E_PASSWORDS=true.

.EXAMPLE
    cd infra/keycloak
    .\scripts\seed-e2e-users.ps1
#>

[CmdletBinding()]
param(
    [string]$EnvFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path (Join-Path $PSScriptRoot '..') '.env'
}

Import-Module (Join-Path (Join-Path $PSScriptRoot 'lib') 'KeycloakAdmin.psm1') -Force

$envMap = Import-KcDotEnv -Path $EnvFile

$serverUrl   = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realmName   = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$apiClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'
$frontendClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$passwordMinLength = [int](Get-KcEnvValue -EnvMap $envMap -Key 'KC_PASSWORD_MIN_LENGTH' -Default '12')

$bootstrapAdminUser = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME'
$bootstrapAdminPass = Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD'

$resetExistingPasswords = (Get-KcEnvValue -EnvMap $envMap -Key 'KC_RESET_EXISTING_E2E_PASSWORDS' -Default 'false') -eq 'true'
$userIdAttribute = 'idUsuario'
$failures = New-Object System.Collections.Generic.List[string]

$userSpecs = @(
    @{ Key = 'DOCENTE'; Role = 'DOCENTE' },
    @{ Key = 'ADMIN'; Role = 'ADMINISTRADOR' },
    @{ Key = 'COORDINADOR'; Role = 'COORDINADOR' },
    @{ Key = 'ESTUDIANTE'; Role = 'ESTUDIANTE' }
)

function Test-E2ePasswordPolicy {
    param(
        [Parameter(Mandatory = $true)][string]$Password,
        [Parameter(Mandatory = $true)][string]$Username,
        [Parameter(Mandatory = $true)][string]$Email
    )

    $reasons = New-Object System.Collections.Generic.List[string]
    if ($Password.Length -lt $passwordMinLength) { $reasons.Add("minimo $passwordMinLength caracteres") | Out-Null }
    if ($Password -cnotmatch '[a-z]') { $reasons.Add('al menos 1 minuscula') | Out-Null }
    if ($Password -cnotmatch '[A-Z]') { $reasons.Add('al menos 1 mayuscula') | Out-Null }
    if ($Password -notmatch '[0-9]') { $reasons.Add('al menos 1 digito') | Out-Null }
    if ($Password -notmatch '[^A-Za-z0-9]') { $reasons.Add('al menos 1 caracter especial') | Out-Null }
    if ($Password -eq $Username) { $reasons.Add('no puede ser igual al username') | Out-Null }
    if ($Password -eq $Email) { $reasons.Add('no puede ser igual al email') | Out-Null }
    return @($reasons)
}

function Set-E2eUserProfile {
    param(
        [Parameter(Mandatory = $true)][string]$UserId,
        [Parameter(Mandatory = $true)][string]$Username,
        [Parameter(Mandatory = $true)][string]$Email,
        [Parameter(Mandatory = $true)][string]$FirstName,
        [Parameter(Mandatory = $true)][string]$LastName,
        [Parameter(Mandatory = $true)][string]$IdUsuario
    )

    $user = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$UserId" -Token $adminToken).Body
    $changed = $false

    foreach ($field in @{
        email         = $Email
        firstName     = $FirstName
        lastName      = $LastName
        enabled       = $true
        emailVerified = $true
    }.GetEnumerator()) {
        $property = $user.PSObject.Properties[$field.Key]
        if ($null -eq $property -or $property.Value -ne $field.Value) {
            $user | Add-Member -NotePropertyName $field.Key -NotePropertyValue $field.Value -Force
            $changed = $true
        }
    }

    $attributes = @{}
    $attributesProperty = $user.PSObject.Properties['attributes']
    if ($null -ne $attributesProperty -and $null -ne $attributesProperty.Value) {
        foreach ($property in $attributesProperty.Value.PSObject.Properties) {
            $attributes[$property.Name] = @($property.Value)
        }
    }

    $currentIdUsuario = @()
    if ($attributes.ContainsKey($userIdAttribute)) {
        $currentIdUsuario = @($attributes[$userIdAttribute])
    }
    if ($currentIdUsuario.Count -ne 1 -or [string]$currentIdUsuario[0] -ne $IdUsuario) {
        $attributes[$userIdAttribute] = @($IdUsuario)
        $changed = $true
    }

    if ($changed) {
        $user | Add-Member -NotePropertyName attributes -NotePropertyValue $attributes -Force
        Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$UserId" -Token $adminToken -Body $user | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Usuario ${Username}: perfil e idUsuario reconciliados"
    } else {
        Write-KcResult -Status 'OK' -Message "Usuario ${Username}: perfil e idUsuario"
    }
}

Write-Host ''
Write-Host "== Seed usuarios E2E :: realm $realmName ==" -ForegroundColor Magenta
Write-Host ''

$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username $bootstrapAdminUser -Password $bootstrapAdminPass
$apiClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $apiClientId
if ($null -eq $apiClient) {
    Write-KcResult -Status 'ERROR' -Message "Client $apiClientId no existe. Ejecuta bootstrap-keycloak.ps1 primero."
    exit 1
}
$frontendClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientId $frontendClientId
if ($null -eq $frontendClient) {
    Write-KcResult -Status 'ERROR' -Message "Client $frontendClientId no existe. Ejecuta bootstrap-keycloak.ps1 primero."
    exit 1
}

foreach ($spec in $userSpecs) {
    $prefix = "E2E_$($spec.Key)_"
    $username = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}USERNAME"
    $password = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}PASSWORD"
    $idUsuario = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}ID_USUARIO"
    $roleName = $spec.Role

    if ([string]::IsNullOrWhiteSpace($username) -or [string]::IsNullOrWhiteSpace($idUsuario)) {
        Write-KcResult -Status 'SKIPPED' -Message "Usuario $($spec.Key): falta ${prefix}USERNAME y/o ${prefix}ID_USUARIO en .env"
        continue
    }
    if ([string]::IsNullOrWhiteSpace($password)) {
        Write-KcResult -Status 'SKIPPED' -Message "Usuario $($spec.Key) ($username): falta ${prefix}PASSWORD en .env"
        continue
    }

    try {
        [void][System.Guid]::Parse($idUsuario)
    } catch {
        Write-KcResult -Status 'ERROR' -Message "${prefix}ID_USUARIO ('$idUsuario') no es un UUID valido."
        $failures.Add("${username}: idUsuario no es UUID") | Out-Null
        continue
    }

    $email = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}EMAIL" -Default "$username@example-e2e.uco.edu.co"
    $firstName = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}FIRST_NAME" -Default $spec.Key
    $lastName = Get-KcEnvValue -EnvMap $envMap -Key "${prefix}LAST_NAME" -Default 'Prueba'

    $existingUser = Find-KcUserByUsername -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -Username $username
    $isNew = $false

    if ($null -eq $existingUser) {
        $created = Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users" -Token $adminToken -Body @{
            username      = $username
            email         = $email
            firstName     = $firstName
            lastName      = $lastName
            enabled       = $true
            emailVerified = $true
            attributes    = @{ idUsuario = @($idUsuario) }
        }
        $userId = ($created.Location -split '/')[-1]
        $isNew = $true
        Write-KcResult -Status 'CREATED' -Message "Usuario $username"
    } else {
        $userId = $existingUser.id
        Write-KcResult -Status 'OK' -Message "Usuario $username"
    }

    Set-E2eUserProfile -UserId $userId -Username $username -Email $email -FirstName $firstName -LastName $lastName -IdUsuario $idUsuario

    $persistedUser = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId" -Token $adminToken).Body
    $persistedValues = @()
    $persistedAttributesProperty = $persistedUser.PSObject.Properties['attributes']
    if ($null -ne $persistedAttributesProperty -and $null -ne $persistedAttributesProperty.Value) {
        $persistedIdProperty = $persistedAttributesProperty.Value.PSObject.Properties[$userIdAttribute]
        if ($null -ne $persistedIdProperty) { $persistedValues = @($persistedIdProperty.Value) }
    }
    if ($persistedValues.Count -ne 1 -or [string]$persistedValues[0] -ne $idUsuario) {
        $message = "Usuario ${username}: Admin REST no persistio exactamente idUsuario=$idUsuario"
        Write-KcResult -Status 'ERROR' -Message $message
        $failures.Add($message) | Out-Null
        continue
    }
    Write-KcResult -Status 'OK' -Message "Usuario ${username}: idUsuario verificado por Admin REST"

    if ($isNew -or $resetExistingPasswords) {
        $passwordProblems = @(Test-E2ePasswordPolicy -Password $password -Username $username -Email $email)
        if ($passwordProblems.Count -gt 0) {
            $message = "Usuario ${username}: password E2E no cumple politica local: $($passwordProblems -join ', ')"
            Write-KcResult -Status 'ERROR' -Message $message
            $failures.Add($message) | Out-Null
        } else {
            try {
                Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId/reset-password" -Token $adminToken -Body @{
                    type      = 'password'
                    value     = $password
                    temporary = $false
                } | Out-Null
                Write-KcResult -Status 'UPDATED' -Message "Usuario ${username}: password establecida"
            } catch {
                $message = "Usuario ${username}: Keycloak rechazo la password. $($_.Exception.Message)"
                Write-KcResult -Status 'ERROR' -Message $message
                $failures.Add($message) | Out-Null
            }
        }
    } else {
        Write-KcResult -Status 'SKIPPED' -Message "Usuario ${username}: password no modificada (KC_RESET_EXISTING_E2E_PASSWORDS=false)"
    }

    $currentRoles = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId/role-mappings/clients/$($apiClient.id)" -Token $adminToken).Body
    $hasRole = @($currentRoles) | Where-Object { $_.name -eq $roleName }
    if (-not $hasRole) {
        $role = Find-KcClientRole -ServerUrl $serverUrl -Realm $realmName -Token $adminToken -ClientUuid $apiClient.id -RoleName $roleName
        if ($null -eq $role) {
            $message = "Role $roleName no existe en $apiClientId. Ejecuta bootstrap-keycloak.ps1 primero."
            Write-KcResult -Status 'ERROR' -Message $message
            $failures.Add($message) | Out-Null
            continue
        }
        Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realmName/users/$userId/role-mappings/clients/$($apiClient.id)" -Token $adminToken -Body @($role) | Out-Null
        Write-KcResult -Status 'UPDATED' -Message "Usuario ${username}: role $roleName asignado"
    } else {
        Write-KcResult -Status 'OK' -Message "Usuario ${username}: role $roleName"
    }

    $exampleToken = (Invoke-KcAdminApi -Method Get -ServerUrl $serverUrl -Path "/admin/realms/$realmName/clients/$($frontendClient.id)/evaluate-scopes/generate-example-access-token?userId=$userId" -Token $adminToken).Body
    $exampleIdUsuario = $null
    $exampleIdProperty = $exampleToken.PSObject.Properties[$userIdAttribute]
    if ($null -ne $exampleIdProperty) { $exampleIdUsuario = [string]$exampleIdProperty.Value }

    $exampleRoles = @()
    $resourceAccessProperty = $exampleToken.PSObject.Properties['resource_access']
    if ($null -ne $resourceAccessProperty -and $null -ne $resourceAccessProperty.Value) {
        $apiAccessProperty = $resourceAccessProperty.Value.PSObject.Properties[$apiClientId]
        if ($null -ne $apiAccessProperty -and $null -ne $apiAccessProperty.Value) {
            $rolesProperty = $apiAccessProperty.Value.PSObject.Properties['roles']
            if ($null -ne $rolesProperty) { $exampleRoles = @($rolesProperty.Value) }
        }
    }

    $exampleAudience = @()
    $audienceProperty = $exampleToken.PSObject.Properties['aud']
    if ($null -ne $audienceProperty) { $exampleAudience = @($audienceProperty.Value) }

    if ($exampleIdUsuario -ne $idUsuario -or $exampleRoles -notcontains $roleName -or $exampleAudience -notcontains $apiClientId) {
        $message = "Usuario ${username}: token de ejemplo no cumple idUsuario/audience/role esperados"
        Write-KcResult -Status 'ERROR' -Message $message
        $failures.Add($message) | Out-Null
    } else {
        Write-KcResult -Status 'OK' -Message "Usuario ${username}: token verificado (idUsuario, aud=$apiClientId, role=$roleName)"
    }
}

Write-Host ''
if ($failures.Count -gt 0) {
    Write-Host "Seed E2E termino con $($failures.Count) fallo(s)." -ForegroundColor Red
    exit 1
}
Write-Host 'Seed E2E completado y verificado.' -ForegroundColor Green
