<#
.SYNOPSIS
    E2E LOCAL (solo desarrollo) con JWT reales de Keycloak contra un backend ya levantado y la DB de desarrollo.

.DESCRIPTION
    MAINT-003K. Comprueba, por HTTP real:
      * seguridad: 401 sin bearer, 403 rol incorrecto (ESTUDIANTE), 403 DOCENTE sin titularidad, 2xx DOCENTE titular;
      * v1: lectura de sesiones = reloj DATETIME2 literal de SQL (sin desplazamiento por la zona de la JVM del backend);
      * v2: POST -> GET -> PATCH -> GET con instantes UTC exactos (100 ns), offsets Bogota/Berlin equivalentes,
            sesion historica NULL => INDETERMINADA, PUT => 405;
      * regresion basica de lecturas v1 (horarios, estudiantes, asistencias).

    No imprime tokens ni passwords. Crea UN usuario temporal en Keycloak (rol DOCENTE, idUsuario = docente fixture dev)
    y lo elimina en finally; borra las sesiones 'E2E-%' que cree en el grupo fixture. Las credenciales E2E se leen de
    infra/keycloak/.env (local, ignorado por Git). Requiere: Keycloak local, SQL Server dev (contenedor), fixtures dev
    (test/fixtures/dev del repo DB) y el backend con app.sesiones.v2.enabled=true.

.EXAMPLE
    .\scripts\e2e\utc-real-jwt-e2e.ps1 -BaseUrl http://127.0.0.1:18181 -TimeZoneLabel America/Bogota
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:18181',
    [string]$SqlContainer = 'sql_server_asistencias',
    [string]$TimeZoneLabel = 'default',
    [string]$ReportPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$kcDir = Join-Path $repoRoot 'infra/keycloak'
Import-Module (Join-Path $kcDir 'scripts/lib/KeycloakAdmin.psm1') -Force
$envMap = Import-KcDotEnv -Path (Join-Path $kcDir '.env')

$serverUrl = Get-KcEnvValue -EnvMap $envMap -Key 'KEYCLOAK_SERVER_URL' -Default 'http://127.0.0.1:8081'
$realm = Get-KcEnvValue -EnvMap $envMap -Key 'KC_REALM' -Default 'asistencias-uco'
$frontendClient = Get-KcEnvValue -EnvMap $envMap -Key 'KC_FRONTEND_CLIENT_ID' -Default 'asistencias-uco-frontend'
$apiClientId = Get-KcEnvValue -EnvMap $envMap -Key 'KC_API_CLIENT_ID' -Default 'asistencias-api'

# --- Datos de referencia (seed 12/14 y fixtures dev del repo DB) ---
$seedGroup = 'A1B2C3D4-E5F6-7A8B-9C0D-1E2F3A4B5C6D'            # titular: docente E2E (E1F2A3B4-...-0003)
$fixGroup = 'DE000005-0000-4000-8000-000000000001'               # FIX-PAG-300, titular: DE000003-...-0001
$fixTeacherUser = 'DE000003-0000-4000-8000-000000000001'
$tmpUsername = 'tmp.e2e.docente.fix300'

$results = New-Object System.Collections.Generic.List[object]
function Add-Result([string]$Id, [bool]$Pass, [string]$Detail) {
    $results.Add([pscustomobject]@{ id = $Id; pass = $Pass; detail = $Detail }) | Out-Null
    $tag = if ($Pass) { 'PASS' } else { 'FAIL' }
    Write-Host ("[{0}] {1} {2}" -f $tag, $Id, $Detail)
}

function Invoke-Http([string]$Method, [string]$Path, [string]$Token, $Body = $null) {
    $args = @{ Method = $Method; Uri = "$BaseUrl$Path"; UseBasicParsing = $true; ContentType = 'application/json; charset=utf-8'; Headers = @{ 'X-Correlation-Id' = [guid]::NewGuid().ToString() } }
    if ($Token) { $args.Headers['Authorization'] = "Bearer $Token" }
    if ($null -ne $Body) { $args['Body'] = ($Body | ConvertTo-Json -Depth 10 -Compress) }
    try {
        $r = Invoke-WebRequest @args
        $status = [int]$r.StatusCode; $text = $r.Content
    } catch {
        $responseProperty = $_.Exception.PSObject.Properties['Response']
        $resp = if ($null -ne $responseProperty) { $responseProperty.Value } else { $null }
        if ($null -eq $resp) {
            throw "HTTP $Method $Path failed without an HTTP response: $($_.Exception.Message)"
        }
        $status = [int]$resp.StatusCode
        $text = $null
        if ($null -ne $_.ErrorDetails) { $text = $_.ErrorDetails.Message }
    }
    $json = $null
    # El contrato HTTP conserva las fechas como strings ISO; evitar que PowerShell las
    # convierta a DateTime local y las vuelva a imprimir con la cultura del host.
    if ($text) { try { $json = $text | ConvertFrom-Json -DateKind String } catch { $json = $null } }
    return [pscustomobject]@{ Status = $status; Json = $json; Text = $text }
}

function Get-UserToken([string]$Username, [string]$Password) {
    $body = 'grant_type=password&client_id=' + (ConvertTo-KcUrlEncoded $frontendClient) + '&username=' + (ConvertTo-KcUrlEncoded $Username) + '&password=' + (ConvertTo-KcUrlEncoded $Password)
    $r = Invoke-RestMethod -Method Post -Uri "$serverUrl/realms/$realm/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body $body
    return $r.access_token
}

function Invoke-Sql([string]$Query) {
    # La password de sa nunca sale del contenedor: sqlcmd la lee de MSSQL_SA_PASSWORD; la consulta viaja en un archivo temporal ASCII.
    # Separador de columnas: coma (los valores consultados no la contienen).
    $tmp = [IO.Path]::GetTempFileName()
    try {
        [IO.File]::WriteAllText($tmp, "SET NOCOUNT ON; $Query", (New-Object System.Text.UTF8Encoding($false)))
        docker cp $tmp "${SqlContainer}:/tmp/uco_e2e.sql" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'docker cp failed' }
        $out = docker exec $SqlContainer bash -c '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $MSSQL_SA_PASSWORD -C -d gestionasistenciadb -h -1 -W -s , -i /tmp/uco_e2e.sql'
        if ($LASTEXITCODE -ne 0) { throw "SQL failed: $($out -join ' ')" }
        return @($out | Where-Object { $_ -and $_.Trim() })
    }
    finally { Remove-Item $tmp -ErrorAction SilentlyContinue }
}

function Get-Ticks([string]$IsoLocal) {
    return [datetime]::ParseExact($IsoLocal.TrimEnd('Z'), [string[]]@("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.FFFFFFF"), [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::None).Ticks
}

$e2eDocenteUser = Get-KcEnvValue -EnvMap $envMap -Key 'E2E_DOCENTE_USERNAME'
$e2eDocentePass = Get-KcEnvValue -EnvMap $envMap -Key 'E2E_DOCENTE_PASSWORD'
$e2eEstUser = Get-KcEnvValue -EnvMap $envMap -Key 'E2E_ESTUDIANTE_USERNAME'
$e2eEstPass = Get-KcEnvValue -EnvMap $envMap -Key 'E2E_ESTUDIANTE_PASSWORD'
$tmpPassword = 'Tmp-' + [guid]::NewGuid().ToString('N') + '-Xz9!'

$adminToken = Get-KcAdminToken -ServerUrl $serverUrl -Username (Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_USERNAME') -Password (Get-KcRequiredEnvValue -EnvMap $envMap -Key 'KC_BOOTSTRAP_ADMIN_PASSWORD')
$apiClient = Find-KcClientByClientId -ServerUrl $serverUrl -Realm $realm -Token $adminToken -ClientId $apiClientId
$tmpUserId = $null
try {
    # Usuario temporal: DOCENTE titular del grupo fixture (alcance minimo; se elimina en finally).
    $existing = Find-KcUserByUsername -ServerUrl $serverUrl -Realm $realm -Token $adminToken -Username $tmpUsername
    if ($existing) { Invoke-KcAdminApi -Method Delete -ServerUrl $serverUrl -Path "/admin/realms/$realm/users/$($existing.id)" -Token $adminToken | Out-Null }
    $created = Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realm/users" -Token $adminToken -Body @{
        username = $tmpUsername; email = "$tmpUsername@example-e2e.uco.edu.co"; firstName = 'Tmp'; lastName = 'E2E'
        enabled = $true; emailVerified = $true; attributes = @{ idUsuario = @($fixTeacherUser.ToLower()) } }
    $tmpUserId = ($created.Location -split '/')[-1]
    Invoke-KcAdminApi -Method Put -ServerUrl $serverUrl -Path "/admin/realms/$realm/users/$tmpUserId/reset-password" -Token $adminToken -Body @{ type = 'password'; value = $tmpPassword; temporary = $false } | Out-Null
    $role = Find-KcClientRole -ServerUrl $serverUrl -Realm $realm -Token $adminToken -ClientUuid $apiClient.id -RoleName 'DOCENTE'
    Invoke-KcAdminApi -Method Post -ServerUrl $serverUrl -Path "/admin/realms/$realm/users/$tmpUserId/role-mappings/clients/$($apiClient.id)" -Token $adminToken -Body @(@{ id = $role.id; name = $role.name }) | Out-Null

    $owner = Get-UserToken $tmpUsername $tmpPassword          # DOCENTE titular del grupo fixture
    $other = Get-UserToken $e2eDocenteUser $e2eDocentePass    # DOCENTE titular del grupo seed (ajeno al fixture)
    $student = Get-UserToken $e2eEstUser $e2eEstPass          # ESTUDIANTE (rol incorrecto)

    # ---------- Seguridad ----------
    $bodyV2 = @{ grupo = $fixGroup; nombre = 'E2E-UTC-sin-jwt'; fechaHoraInicio = '2042-07-15T20:00:00-05:00'; fechaHoraFin = '2042-07-15T22:00:00-05:00' }
    $r = Invoke-Http 'POST' '/api/v2/sesiones' $null $bodyV2
    Add-Result 'AUTH-01 POST v2 sin bearer' ($r.Status -eq 401) "status=$($r.Status)"
    $r = Invoke-Http 'POST' '/api/v2/sesiones' $student $bodyV2
    Add-Result 'AUTH-02 POST v2 rol ESTUDIANTE' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Invoke-Http 'POST' '/api/v2/sesiones' $other $bodyV2
    Add-Result 'AUTH-03 POST v2 DOCENTE ajeno al grupo' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Invoke-Http 'GET' "/api/v2/sesiones/grupo/$fixGroup" $other
    Add-Result 'AUTH-03b GET v2 grupo ajeno' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Invoke-Http 'GET' "/api/v1/sesiones/grupo/$fixGroup" $other
    Add-Result 'AUTH-03c GET v1 grupo ajeno' ($r.Status -eq 403) "status=$($r.Status)"
    $r = Invoke-Http 'GET' "/api/v1/sesiones/grupo/$fixGroup" $null
    Add-Result 'AUTH-01b GET v1 sin bearer' ($r.Status -eq 401) "status=$($r.Status)"
    $bodyActor = @{ grupo = $fixGroup; nombre = 'E2E-UTC-actor'; fechaHoraInicio = '2042-07-15T20:00:00-05:00'; fechaHoraFin = '2042-07-15T22:00:00-05:00'; usuarioEjecutor = $fixTeacherUser }
    $r = Invoke-Http 'POST' '/api/v2/sesiones' $owner $bodyActor
    Add-Result 'AUTH-05 actor del body rechazado (FIELD_UNKNOWN)' ($r.Status -eq 400) "status=$($r.Status)"

    # ---------- v1: lectura = reloj SQL literal ----------
    foreach ($g in @(@{ n = 'seed'; id = $seedGroup; tok = $other }, @{ n = 'fixture'; id = $fixGroup; tok = $owner })) {
        $r = Invoke-Http 'GET' "/api/v1/sesiones/grupo/$($g.id)" $g.tok
        $ok = ($r.Status -eq 200)
        $mismatch = 0; $count = 0
        if ($ok) {
            $sql = @(Invoke-Sql ("SELECT CAST(id AS VARCHAR(36)), CONVERT(VARCHAR(27), fechaHoraInicio, 126), CONVERT(VARCHAR(27), fechaHoraFin, 126) FROM dbo.Sesion WHERE grupo = '$($g.id)' AND nombre NOT LIKE 'E2E-%'"))
            foreach ($line in $sql) {
                $p = $line -split ','
                $row = @($r.Json.datos | Where-Object { $_.sesion -eq $p[0].ToLower() })
                $count++
                if ($row.Count -ne 1 -or (Get-Ticks $row[0].fechaHoraInicio) -ne (Get-Ticks $p[1]) -or (Get-Ticks $row[0].fechaHoraFin) -ne (Get-Ticks $p[2])) { $mismatch++ }
            }
        }
        Add-Result "TIM-v1 lectura v1 grupo $($g.n) = SQL literal ($TimeZoneLabel)" ($ok -and $count -gt 0 -and $mismatch -eq 0) "status=$($r.Status) filas=$count desajustes=$mismatch"
    }

    # ---------- v2: lectura de fixture (100 ns, UTC_V2) ----------
    $r = Invoke-Http 'GET' "/api/v2/sesiones/grupo/$fixGroup" $owner
    $s1 = $null
    if ($r.Status -eq 200) { $s1 = @($r.Json.datos | Where-Object { $_.numero -eq 1 -and $_.nombre -notlike 'E2E-*' })[0] }
    Add-Result "TIM-05 GET v2 fixture sesion 1 Z exacta ($TimeZoneLabel)" ($null -ne $s1 -and $s1.fechaHoraInicio -eq '2026-09-14T13:00:00.1234567Z' -and $s1.fechaHoraFin -eq '2026-09-14T15:00:00.7654321Z' -and $s1.estadoTemporal -eq 'CONFIRMADA' -and $s1.procedenciaTemporal -eq 'UTC_V2') ("status=$($r.Status) ini=" + $(if ($s1) { $s1.fechaHoraInicio } else { 'n/a' }))

    # ---------- v2: POST -> GET -> PATCH -> GET ----------
    $nombre = 'E2E-UTC-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
    $r = Invoke-Http 'POST' '/api/v2/sesiones' $owner @{ grupo = $fixGroup; nombre = $nombre; fechaHoraInicio = '2042-07-15T20:00:00.1234567-05:00'; fechaHoraFin = '2042-07-15T22:30:00.7654321-05:00' }
    Add-Result "AUTH-04/TIM-07 POST v2 titular ($TimeZoneLabel)" ($r.Status -eq 201) "status=$($r.Status)"
    $r = Invoke-Http 'GET' "/api/v2/sesiones/grupo/$fixGroup" $owner
    $created = $null
    if ($r.Status -eq 200) { $created = @($r.Json.datos | Where-Object { $_.nombre -eq $nombre })[0] }
    Add-Result 'TIM-07 GET v2 tras POST: id estable y UTC exacta' ($null -ne $created -and $created.fechaHoraInicio -eq '2042-07-16T01:00:00.1234567Z' -and $created.fechaHoraFin -eq '2042-07-16T03:30:00.7654321Z' -and $created.procedenciaTemporal -eq 'UTC_V2') ("ini=" + $(if ($created) { $created.fechaHoraInicio } else { 'n/a' }))
    if ($created) {
        $sid = $created.sesion
        $sql = @(Invoke-Sql "SELECT CONVERT(VARCHAR(27), fechaHoraInicio, 126), CONVERT(VARCHAR(27), fechaHoraFin, 126), procedenciaTemporal FROM dbo.Sesion WHERE id = '$sid'")
        Add-Result 'TIM-07b SQL datetime2(7) UTC + marca' ($sql[0] -eq '2042-07-16T01:00:00.1234567,2042-07-16T03:30:00.7654321,UTC_V2') "sql=$($sql[0])"
        $r = Invoke-Http 'GET' "/api/v2/sesiones/$sid" $owner
        Add-Result 'GET v2 por id' ($r.Status -eq 200 -and $r.Json.datos.fechaHoraInicio -eq '2042-07-16T01:00:00.1234567Z') "status=$($r.Status)"
        $r = Invoke-Http 'PATCH' "/api/v2/sesiones/$sid" $owner @{ nombre = "$nombre-b"; fechaHoraInicio = '2042-07-16T03:00:00.1234567+02:00'; fechaHoraFin = '2042-07-16T05:30:00.7654321+02:00' }
        Add-Result "TIM-08 PATCH v2 offset Berlin mismo instante ($TimeZoneLabel)" ($r.Status -eq 200) "status=$($r.Status)"
        $r = Invoke-Http 'GET' "/api/v2/sesiones/$sid" $owner
        Add-Result 'TIM-08 GET v2 tras PATCH identico (sin doble conversion)' ($r.Status -eq 200 -and $r.Json.datos.fechaHoraInicio -eq '2042-07-16T01:00:00.1234567Z' -and $r.Json.datos.fechaHoraFin -eq '2042-07-16T03:30:00.7654321Z' -and $r.Json.datos.nombre -eq "$nombre-b") "ini=$($r.Json.datos.fechaHoraInicio)"
        $r = Invoke-Http 'PATCH' "/api/v2/sesiones/$sid" $other @{ nombre = 'x'; fechaHoraInicio = '2042-07-16T03:00:00+02:00'; fechaHoraFin = '2042-07-16T05:30:00+02:00' }
        Add-Result 'AUTH-03d PATCH v2 DOCENTE ajeno' ($r.Status -eq 403) "status=$($r.Status)"
        $r = Invoke-Http 'GET' "/api/v2/sesiones/$sid" $other
        Add-Result 'AUTH-03e GET v2 por id DOCENTE ajeno' ($r.Status -eq 403) "status=$($r.Status)"
        $r = Invoke-Http 'PUT' "/api/v2/sesiones/$sid" $owner @{ nombre = 'x' }
        Add-Result 'COMP-02 PUT v2 => 405' ($r.Status -eq 405) "status=$($r.Status)"
    }

    # ---------- TIM-09 validacion estricta ----------
    foreach ($case in @(
            @{ id = 'TIM-09a naive sin offset'; ini = '2042-07-15T20:00:00'; fin = '2042-07-15T22:00:00' },
            @{ id = 'TIM-09b 8 decimales'; ini = '2042-07-15T20:00:00.12345678-05:00'; fin = '2042-07-15T22:00:00-05:00' },
            @{ id = 'TIM-09c offset sin minutos'; ini = '2042-07-15T20:00:00-05'; fin = '2042-07-15T22:00:00-05:00' })) {
        $r = Invoke-Http 'POST' '/api/v2/sesiones' $owner @{ grupo = $fixGroup; nombre = 'E2E-UTC-invalida'; fechaHoraInicio = $case.ini; fechaHoraFin = $case.fin }
        Add-Result $case.id ($r.Status -eq 400) "status=$($r.Status)"
    }

    # ---------- TIM-06 historica NULL (escrita por v1) => INDETERMINADA ----------
    $v1name = 'E2E-V1-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
    $r = Invoke-Http 'POST' '/api/v1/sesiones' $owner @{ grupo = $fixGroup; nombre = $v1name; fechaHoraInicio = '2042-08-01T08:00:00'; fechaHoraFin = '2042-08-01T10:00:00' }
    Add-Result 'v1 POST sesion (reloj local, sin procedencia)' ($r.Status -eq 201 -or $r.Status -eq 200) "status=$($r.Status)"
    $r = Invoke-Http 'GET' "/api/v2/sesiones/grupo/$fixGroup" $owner
    $ind = $null
    if ($r.Status -eq 200) { $ind = @($r.Json.datos | Where-Object { $_.nombre -eq $v1name })[0] }
    Add-Result 'TIM-06 GET v2 de fila v1: INDETERMINADA con horas null' ($null -ne $ind -and $ind.estadoTemporal -eq 'INDETERMINADA' -and $null -eq $ind.fechaHoraInicio -and $null -eq $ind.procedenciaTemporal) ("estado=" + $(if ($ind) { $ind.estadoTemporal } else { 'n/a' }))
    $r = Invoke-Http 'GET' "/api/v1/sesiones/grupo/$fixGroup" $owner
    $v1row = $null
    if ($r.Status -eq 200) { $v1row = @($r.Json.datos | Where-Object { $_.nombre -eq $v1name })[0] }
    Add-Result "TIM-v1b v1 devuelve literal 08:00-10:00 sin desplazar ($TimeZoneLabel)" ($null -ne $v1row -and (Get-Ticks $v1row.fechaHoraInicio) -eq (Get-Ticks '2042-08-01T08:00:00') -and (Get-Ticks $v1row.fechaHoraFin) -eq (Get-Ticks '2042-08-01T10:00:00')) ("ini=" + $(if ($v1row) { $v1row.fechaHoraInicio } else { 'n/a' }))

    # ---------- Regresion de lecturas v1 ----------
    foreach ($c in @(@{ id = 'REG docente/horarios'; p = '/api/v1/docente/horarios'; t = $other }, @{ id = 'REG grupo/estudiantes'; p = "/api/v1/grupos/$fixGroup/estudiantes"; t = $owner }, @{ id = 'REG grupo/asistencias'; p = "/api/v1/grupos/$seedGroup/asistencias"; t = $other })) {
        $r = Invoke-Http 'GET' $c.p $c.t
        Add-Result $c.id ($r.Status -eq 200) "status=$($r.Status)"
    }
}
finally {
    try { Invoke-Sql "DELETE FROM dbo.Sesion WHERE grupo = '$fixGroup' AND nombre LIKE 'E2E-%'" | Out-Null } catch { Write-Host "[WARN] limpieza SQL: $($_.Exception.Message)" }
    if ($tmpUserId) { try { Invoke-KcAdminApi -Method Delete -ServerUrl $serverUrl -Path "/admin/realms/$realm/users/$tmpUserId" -Token $adminToken | Out-Null } catch { Write-Host "[WARN] no se pudo eliminar el usuario temporal Keycloak" } }
    $tmpPassword = $null
}

$failed = @($results | Where-Object { -not $_.pass })
Write-Host ("E2E_SUMMARY tz={0} total={1} pass={2} fail={3}" -f $TimeZoneLabel, $results.Count, ($results.Count - $failed.Count), $failed.Count)
if ($ReportPath) { $results | ConvertTo-Json -Depth 4 | Set-Content -Path $ReportPath -Encoding UTF8 }
if ($failed.Count -gt 0) { exit 1 }
