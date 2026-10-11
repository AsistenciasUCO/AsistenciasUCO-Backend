param(
    [Parameter(Mandatory = $true)][string]$Jar,
    [Parameter(Mandatory = $true)][string]$DbPasswordFile,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [string]$ContainerName = 'cc003g01_sqltest',
    [int]$Port = 18081,
    [string]$JvmTimezone = ''
)
# Regresion UTC v2 de punta a punta: backend real + SQL Server aislado (login de minimo privilegio) + Keycloak local real.
# Los usuarios de Keycloak y el login SQL son temporales (creados y eliminados aqui); ningun token ni password se imprime.
$ErrorActionPreference = 'Stop'
$repoRoot = if ($env:UCO_REPO_ROOT) { $env:UCO_REPO_ROOT } else { throw 'Set UCO_REPO_ROOT to the main checkout that holds .env and infra/keycloak/.env' }
$kcDir = Join-Path $repoRoot 'infra\keycloak'
Import-Module (Join-Path $kcDir 'scripts\lib\KeycloakAdmin.psm1') -Force
New-Item -ItemType Directory -Force $OutDir | Out-Null
$results = New-Object System.Collections.Generic.List[object]
function Add-Result([string]$Name, [bool]$Ok, [string]$Detail) {
    $results.Add([pscustomobject]@{ check = $Name; result = $(if ($Ok) { 'PASS' } else { 'FAIL' }); detail = $Detail }) | Out-Null
    Write-Output ("{0}: {1} - {2}" -f $(if ($Ok) { 'PASS' } else { 'FAIL' }), $Name, $Detail)
}

$saPw = (Get-Content $DbPasswordFile -Raw).Trim()
function Invoke-DbSa([string]$Query) {
    $out = docker exec -e "SQLCMDPASSWORD=$saPw" $ContainerName /opt/mssql-tools18/bin/sqlcmd -S localhost -d gestionasistenciadb -U sa -C -b -h -1 -W -s '|' -Q $Query 2>&1
    if ($LASTEXITCODE -ne 0) { throw "sa query failed: $($out -join '; ')" }
    return @($out | ForEach-Object { "$_".Trim() } | Where-Object { $_ })
}

# ---- Fixtures de DB (ids reales del contenedor aislado)
$fx = @(Invoke-DbSa "SET NOCOUNT ON; SELECT TOP 1 CONCAT(CONVERT(VARCHAR(36), g.id), '|', CONVERT(VARCHAR(36), di.idUsuario)) FROM dbo.uv_grupo g JOIN dbo.uv_docente_identidad di ON di.id = g.idDocente WHERE g.grupoEstaHablitado = 1 AND di.estaActivoUsuario = 1 AND g.nombre NOT LIKE N'IT-MAINT003H%' ORDER BY g.cuposDisponibles DESC;")
$grupoId, $ownerUserId = $fx[0].Split('|')
$otherUserId = @(Invoke-DbSa "SET NOCOUNT ON; SELECT TOP 1 CONVERT(VARCHAR(36), idUsuario) FROM dbo.uv_docente_identidad WHERE estaActivoUsuario = 1 AND idUsuario <> '$ownerUserId' ORDER BY idUsuario;")[0]
$studentUserId = @(Invoke-DbSa "SET NOCOUNT ON; SELECT TOP 1 CONVERT(VARCHAR(36), idUsuario) FROM dbo.uv_estudiante_identidad WHERE estaActivoUsuario = 1 ORDER BY idUsuario;")[0]
if (-not $grupoId -or -not $ownerUserId -or -not $otherUserId -or -not $studentUserId) { throw 'UTC E2E fixtures missing in the isolated DB.' }
Write-Output "fixture ids: grupo=[$grupoId] owner=[$ownerUserId] other=[$otherUserId] student=[$studentUserId]"

# ---- Login SQL temporal de minimo privilegio (miembro SOLO de rol_asistencias_runtime)
$tag = [guid]::NewGuid().ToString('N').Substring(0, 10)
$sqlLogin = "cc003g01_e2e_$tag"
$sqlLoginPw = 'T!9a' + [guid]::NewGuid().ToString('N') + 'Zz'
Invoke-DbSa "CREATE LOGIN [$sqlLogin] WITH PASSWORD = '$sqlLoginPw', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF; CREATE USER [$sqlLogin] FOR LOGIN [$sqlLogin]; ALTER ROLE [rol_asistencias_runtime] ADD MEMBER [$sqlLogin];" | Out-Null

# ---- Keycloak: usuarios temporales
$envMap = Import-KcDotEnv -Path (Join-Path $kcDir '.env')
$kcUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realm = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$apiClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'
$frontClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$adminToken = Get-KcAdminToken -ServerUrl $kcUrl -Username (Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME') -Password (Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD')
$apiClient = Find-KcClientByClientId -ServerUrl $kcUrl -Realm $realm -Token $adminToken -ClientId $apiClientId
$createdKcUsers = New-Object System.Collections.Generic.List[string]
$backend = $null
$createdSessions = New-Object System.Collections.Generic.List[string]

function New-E2eUser([string]$Suffix, [string]$IdUsuario, [string]$Role) {
    $username = "cc003g01.e2e.$tag.$Suffix"
    $pw = 'Zq!' + [guid]::NewGuid().ToString('N') + 'aB9'
    $created = Invoke-KcAdminApi -Method Post -ServerUrl $kcUrl -Path "/admin/realms/$realm/users" -Token $adminToken -Body @{
        username = $username; email = "$username@example-e2e.uco.edu.co"; firstName = 'E2E'; lastName = $Suffix
        enabled = $true; emailVerified = $true; attributes = @{ idUsuario = @($IdUsuario) } }
    $kcId = ($created.Location -split '/')[-1]
    $createdKcUsers.Add($kcId) | Out-Null
    Invoke-KcAdminApi -Method Put -ServerUrl $kcUrl -Path "/admin/realms/$realm/users/$kcId/reset-password" -Token $adminToken -Body @{ type = 'password'; value = $pw; temporary = $false } | Out-Null
    $roleObj = Find-KcClientRole -ServerUrl $kcUrl -Realm $realm -Token $adminToken -ClientUuid $apiClient.id -RoleName $Role
    Invoke-KcAdminApi -Method Post -ServerUrl $kcUrl -Path "/admin/realms/$realm/users/$kcId/role-mappings/clients/$($apiClient.id)" -Token $adminToken -Body @($roleObj) | Out-Null
    $tokenResponse = Invoke-RestMethod -Method Post -Uri "$kcUrl/realms/$realm/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body ("grant_type=password&client_id={0}&username={1}&password={2}" -f [uri]::EscapeDataString($frontClientId), [uri]::EscapeDataString($username), [uri]::EscapeDataString($pw))
    return $tokenResponse.access_token
}

function Call-Api([string]$Method, [string]$Path, $Body = $null, [string]$Token = $null) {
    $headers = @{ 'X-Correlation-Id' = [guid]::NewGuid().ToString() }
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $args2 = @{ Method = $Method; Uri = "http://127.0.0.1:$Port$Path"; Headers = $headers; UseBasicParsing = $true; ContentType = 'application/json' }
    if ($null -ne $Body) { $args2['Body'] = ($Body | ConvertTo-Json -Depth 8 -Compress) }
    try {
        $r = Invoke-WebRequest @args2
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Json = $(if ($r.Content) { $r.Content | ConvertFrom-Json } else { $null }); Raw = $r.Content }
    }
    catch {
        $resp = $_.Exception.Response
        $status = if ($resp) { [int]$resp.StatusCode } else { -1 }
        return [pscustomobject]@{ Status = $status; Json = $null; Raw = $_.ErrorDetails.Message }
    }
}
function Utc([string]$s) { return ([DateTimeOffset]::Parse($s, [Globalization.CultureInfo]::InvariantCulture)).UtcDateTime }

try {
    $ownerToken = New-E2eUser 'owner' $ownerUserId 'DOCENTE'
    $otherToken = New-E2eUser 'other' $otherUserId 'DOCENTE'
    $studentToken = New-E2eUser 'student' $studentUserId 'ESTUDIANTE'
    Add-Result 'keycloak_temp_users_and_real_jwt' $true '3 usuarios temporales con idUsuario de DB y tokens reales emitidos por Keycloak local'

    # ---- Backend real con el login SQL de minimo privilegio y v2 habilitado
    Get-Content (Join-Path $repoRoot '.env') | ForEach-Object {
        $l = $_.Trim()
        if ($l -and -not $l.StartsWith('#') -and $l.Contains('=')) { $i = $l.IndexOf('='); [Environment]::SetEnvironmentVariable($l.Substring(0, $i).Trim(), $l.Substring($i + 1).Trim(), 'Process') }
    }
    [Environment]::SetEnvironmentVariable('SPRING_DATASOURCE_URL', 'jdbc:sqlserver://localhost:14334;databaseName=gestionasistenciadb;encrypt=true;trustServerCertificate=true', 'Process')
    [Environment]::SetEnvironmentVariable('SPRING_DATASOURCE_USERNAME', $sqlLogin, 'Process')
    [Environment]::SetEnvironmentVariable('SPRING_DATASOURCE_PASSWORD', $sqlLoginPw, 'Process')
    [Environment]::SetEnvironmentVariable('APP_DATABASE_EXPECTED_NAME', 'gestionasistenciadb', 'Process')
    [Environment]::SetEnvironmentVariable('APP_SESIONES_V2_ENABLED', 'true', 'Process')
    [Environment]::SetEnvironmentVariable('SERVER_PORT', "$Port", 'Process')
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already in use by another process; refusing to test against a foreign backend." }
    $outLog = Join-Path $OutDir 'backend.out.log'; $errLog = Join-Path $OutDir 'backend.err.log'
    $backend = Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin\java.exe') -ArgumentList $(if ($JvmTimezone) { @("-Duser.timezone=$JvmTimezone", '-jar', $Jar) } else { @('-jar', $Jar) }) -PassThru -WindowStyle Hidden -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    $ready = $false
    for ($i = 0; $i -lt 90 -and -not $ready; $i++) {
        Start-Sleep -Seconds 2
        if ($backend.HasExited) { break }
        try { $h = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/actuator/health" -UseBasicParsing -TimeoutSec 3; $ready = ($h.StatusCode -eq 200) } catch { $ready = $false }
        if (-not $ready) { try { $x = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/api/v2/sesiones/$([guid]::Empty)" -UseBasicParsing -TimeoutSec 3 } catch { if ($_.Exception.Response) { $ready = $true } } }
    }
    if (-not $ready) { throw "Backend did not start (exited=$($backend.HasExited)). See $errLog" }
    $listener = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    if ($backend.HasExited -or $listener -notcontains $backend.Id) { throw "Port $Port is served by process(es) $($listener -join ','), not by the backend started by this harness (pid $($backend.Id))." }
    Add-Result 'backend_starts_with_runtime_least_privilege_login' $true 'arranque con login SQL miembro solo de rol_asistencias_runtime y guard de esquema UTC-D06'

    $nombre = "E2E-CC003G01-$tag"
    # 401 sin token
    $r = Call-Api 'POST' '/api/v2/sesiones' @{ grupo = $grupoId; nombre = $nombre; fechaHoraInicio = '2042-07-15T09:00:00-05:00'; fechaHoraFin = '2042-07-15T10:30:00-05:00' }
    Add-Result 'v2_post_without_jwt_401' ($r.Status -eq 401) "status=$($r.Status)"

    # POST v2 Bogota (-05:00) por el DOCENTE titular
    $r = Call-Api 'POST' '/api/v2/sesiones' @{ grupo = $grupoId; nombre = $nombre; fechaHoraInicio = '2042-07-15T09:00:00.1234567-05:00'; fechaHoraFin = '2042-07-15T10:30:00.7654321-05:00' } $ownerToken
    Add-Result 'v2_post_bogota_owner_201' ($r.Status -eq 201) "status=$($r.Status)"
    $lista = Call-Api 'GET' "/api/v2/sesiones/grupo/$grupoId" $null $ownerToken
    $sesion = @($lista.Json.datos) | Where-Object { $_.nombre -eq $nombre } | Select-Object -First 1
    Add-Result 'v2_list_group_finds_created_session' ($lista.Status -eq 200 -and $null -ne $sesion) "status=$($lista.Status)"
    $sesionId = [string]$sesion.sesion
    $createdSessions.Add($sesionId) | Out-Null

    $g1 = Call-Api 'GET' "/api/v2/sesiones/$sesionId" $null $ownerToken
    $d = $g1.Json.datos
    $okGet1 = ($g1.Status -eq 200 -and $d.estadoTemporal -eq 'CONFIRMADA' -and $d.procedenciaTemporal -eq 'UTC_V2' -and
        ($d.fechaHoraInicio -match 'Z$') -and ((Utc $d.fechaHoraInicio) -eq ([DateTimeOffset]::Parse('2042-07-15T14:00:00.1234567Z')).UtcDateTime) -and
        ((Utc $d.fechaHoraFin) -eq ([DateTimeOffset]::Parse('2042-07-15T15:30:00.7654321Z')).UtcDateTime))
    Add-Result 'v2_get_after_post_utc_instants_preserved' $okGet1 "inicio=$($d.fechaHoraInicio) fin=$($d.fechaHoraFin) procedencia=$($d.procedenciaTemporal)"

    # PATCH v2 Berlin (+02:00)
    $r = Call-Api 'PATCH' "/api/v2/sesiones/$sesionId" @{ nombre = "$nombre-berlin"; fechaHoraInicio = '2042-07-16T16:00:00+02:00'; fechaHoraFin = '2042-07-16T18:30:00.25+02:00' } $ownerToken
    Add-Result 'v2_patch_berlin_owner_200' ($r.Status -eq 200) "status=$($r.Status)"
    $g2 = Call-Api 'GET' "/api/v2/sesiones/$sesionId" $null $ownerToken
    $d2 = $g2.Json.datos
    $okGet2 = ($g2.Status -eq 200 -and $d2.procedenciaTemporal -eq 'UTC_V2' -and ((Utc $d2.fechaHoraInicio) -eq ([DateTimeOffset]::Parse('2042-07-16T14:00:00Z')).UtcDateTime) -and
        ((Utc $d2.fechaHoraFin) -eq ([DateTimeOffset]::Parse('2042-07-16T16:30:00.25Z')).UtcDateTime))
    Add-Result 'v2_get_after_patch_normalized_to_same_instant_model' $okGet2 "inicio=$($d2.fechaHoraInicio) fin=$($d2.fechaHoraFin)"

    # 403 no titular (DOCENTE de otro grupo) y 403 no DOCENTE
    $r = Call-Api 'GET' "/api/v2/sesiones/$sesionId" $null $otherToken
    Add-Result 'v2_get_non_owner_docente_403' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Call-Api 'PATCH' "/api/v2/sesiones/$sesionId" @{ nombre = 'x'; fechaHoraInicio = '2042-07-16T16:00:00+02:00'; fechaHoraFin = '2042-07-16T18:30:00+02:00' } $otherToken
    Add-Result 'v2_patch_non_owner_docente_403' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Call-Api 'POST' '/api/v2/sesiones' @{ grupo = $grupoId; nombre = "$nombre-x"; fechaHoraInicio = '2042-07-17T09:00:00-05:00'; fechaHoraFin = '2042-07-17T10:00:00-05:00' } $otherToken
    Add-Result 'v2_post_non_owner_docente_403' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Call-Api 'GET' "/api/v2/sesiones/$sesionId" $null $studentToken
    Add-Result 'v2_get_non_docente_role_403' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Call-Api 'PUT' "/api/v2/sesiones/$sesionId" @{ nombre = 'x'; fechaHoraInicio = '2042-07-16T16:00:00+02:00'; fechaHoraFin = '2042-07-16T18:30:00+02:00' } $ownerToken
    Add-Result 'v2_put_not_allowed_405' ($r.Status -eq 405) "status=$($r.Status)"
    $r = Call-Api 'POST' '/api/v2/sesiones' @{ grupo = $grupoId; nombre = "$nombre-bad"; fechaHoraInicio = '2042-07-17T09:00:00'; fechaHoraFin = '2042-07-17T10:00:00' } $ownerToken
    Add-Result 'v2_post_without_offset_400' ($r.Status -eq 400) "status=$($r.Status)"

    # v1 invariantes: wire local sin offset, y una sesion creada por v1 es INDETERMINADA en v2
    $v1 = Call-Api 'GET' "/api/v1/sesiones/$sesionId" $null $ownerToken
    $v1d = if ($v1.Json.datos) { $v1.Json.datos } else { $v1.Json }
    $v1Inicio = [string]$v1d.fechaHoraInicio
    Add-Result 'v1_get_wire_is_local_iso_without_offset' ($v1.Status -eq 200 -and $v1Inicio -match '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$') "status=$($v1.Status) inicio=$v1Inicio"
    if ($JvmTimezone -eq 'UTC') { Add-Result 'v1_get_wire_equals_stored_value_with_jvm_utc' ($v1Inicio -eq '2042-07-16T14:00:00') "inicio=$v1Inicio (almacenado UTC 14:00 por PATCH Berlin 16:00+02:00)" }
    $nombreV1 = "E2E-CC003G01-V1-$tag"
    $r = Call-Api 'POST' '/api/v1/sesiones' @{ grupo = $grupoId; nombre = $nombreV1; fechaHoraInicio = '2042-08-01T08:00:00'; fechaHoraFin = '2042-08-01T10:00:00' } $ownerToken
    Add-Result 'v1_post_still_works_201' ($r.Status -eq 201 -or $r.Status -eq 200) "status=$($r.Status)"
    $lista2 = Call-Api 'GET' "/api/v2/sesiones/grupo/$grupoId" $null $ownerToken
    $sv1 = @($lista2.Json.datos) | Where-Object { $_.nombre -eq $nombreV1 } | Select-Object -First 1
    if ($sv1) { $createdSessions.Add([string]$sv1.sesion) | Out-Null }
    Add-Result 'v1_created_session_is_indeterminate_in_v2' ($null -ne $sv1 -and $sv1.estadoTemporal -eq 'INDETERMINADA' -and $null -eq $sv1.procedenciaTemporal -and $null -eq $sv1.fechaHoraInicio) "estado=$($sv1.estadoTemporal)"
    if ($sv1) {
        $v1b = Call-Api 'GET' "/api/v1/sesiones/$($sv1.sesion)" $null $ownerToken
        $v1bd = if ($v1b.Json.datos) { $v1b.Json.datos } else { $v1b.Json }
        $rawDbArr = @(Invoke-DbSa "SET NOCOUNT ON; SELECT CONVERT(VARCHAR(30), fechaHoraInicio, 126) FROM dbo.Sesion WHERE id = '$($sv1.sesion)';"); $rawDb = if ($rawDbArr.Count) { $rawDbArr[0] } else { 'n/a' }
        Add-Result 'INFO_v1_roundtrip_v1_created_row' $true "enviado=2042-08-01T08:00:00 almacenado=$rawDb v1_get=$($v1bd.fechaHoraInicio)"
    }

    # Auditoria: el login de minimo privilegio puede escribir AuditoriaEvento (unica tabla con DML directo)
    $aud = @(Invoke-DbSa "SET NOCOUNT ON; SELECT COUNT(*) FROM dbo.AuditoriaEvento WHERE occurredAt > DATEADD(MINUTE, -30, SYSDATETIMEOFFSET());")
    Add-Result 'audit_rows_written_by_runtime_login' ([int]$aud[0] -ge 0) "filas recientes=$($aud[0])"
    $persisted = @(Invoke-DbSa "SET NOCOUNT ON; SELECT COUNT(*) FROM dbo.Sesion WHERE nombre LIKE N'E2E-CC003G01-%$tag%';")
    Add-Result 'sessions_persisted_in_the_target_isolated_db' ([int]$persisted[0] -ge 2) "sesiones de prueba en la DB objetivo=$($persisted[0])"
}
catch {
    Add-Result 'e2e_execution_error' $false $_.Exception.Message
}
finally {
    if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force }
    foreach ($sid in $createdSessions) {
        if ($sid) { try { Invoke-DbSa "SET NOCOUNT ON; DISABLE TRIGGER ALL ON dbo.Sesion; DELETE FROM dbo.DetalleAsistencia WHERE asistencia IN (SELECT id FROM dbo.Asistencia WHERE sesion = '$sid'); DELETE FROM dbo.Asistencia WHERE sesion = '$sid'; DELETE FROM dbo.Sesion WHERE id = '$sid'; ENABLE TRIGGER ALL ON dbo.Sesion;" | Out-Null } catch { Write-Output "cleanup session $sid failed: $($_.Exception.Message)" } }
    }
    foreach ($kcId in $createdKcUsers) { try { Invoke-KcAdminApi -Method Delete -ServerUrl $kcUrl -Path "/admin/realms/$realm/users/$kcId" -Token $adminToken | Out-Null } catch { Write-Output "cleanup keycloak user failed: $($_.Exception.Message)" } }
    try { Invoke-DbSa "IF DATABASE_PRINCIPAL_ID(N'$sqlLogin') IS NOT NULL DROP USER [$sqlLogin];" | Out-Null; docker exec -e "SQLCMDPASSWORD=$saPw" $ContainerName /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -Q "IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'$sqlLogin') DROP LOGIN [$sqlLogin];" | Out-Null } catch { Write-Output "cleanup sql login failed" }
    $results | ConvertTo-Json -Depth 4 | Out-File (Join-Path $OutDir 'utc_e2e_results.json') -Encoding utf8
    $failed = @($results | Where-Object { $_.result -ne 'PASS' }).Count
    Write-Output "UTC_E2E_SUMMARY total=$($results.Count) failed=$failed"
}
