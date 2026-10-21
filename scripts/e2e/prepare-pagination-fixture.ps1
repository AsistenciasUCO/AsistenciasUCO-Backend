<#!
.SYNOPSIS
    Inspecciona o prepara los horarios DEV de FIX-PAG-300/1000/5000.

.DESCRIPTION
    Opera solo contra el contenedor SQL Server Developer autorizado y la base
    gestionasistenciadb. Antes de preparar hace un backup COPY_ONLY con checksum,
    valida el destino y compara un snapshot de estudiantes/matriculas/sesiones/asistencias.
    No recarga el fixture ni toca grupos, usuarios, estudiantes, matriculas o sesiones.

    Los horarios creados por este script usan IDs reservados DE00000B-... y la limpieza
    solo elimina esos IDs. Un horario preexistente de otro fixture se conserva.
#>

[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [ValidateSet('Inspect', 'Prepare', 'Cleanup')]
    [string]$Action = 'Inspect',
    [string]$ContainerName = 'sql_server_asistencias',
    [string]$BackupDirectory = 'C:\Users\josev\AsistenciasUCO\db-backups'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envPath = Join-Path $repoRoot '.env'
if (Test-Path -LiteralPath $envPath) {
    & (Join-Path $repoRoot 'scripts\load-env.ps1') -Path $envPath
}
$sqlPassword = [Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_PASSWORD')
if ([string]::IsNullOrWhiteSpace($sqlPassword)) {
    throw 'Falta SPRING_DATASOURCE_PASSWORD en el .env local; no se usara password por linea de comandos.'
}

$sqlCmd = '/opt/mssql-tools18/bin/sqlcmd'
$baseArgs = @('-S', 'localhost', '-d', 'gestionasistenciadb', '-U', 'sa', '-P', $sqlPassword, '-C', '-b', '-h', '-1', '-W', '-s', '|')

function Invoke-DevSql {
    param([Parameter(Mandatory = $true)][string]$Query)
    $result = @(& docker exec $ContainerName $sqlCmd @baseArgs -Q $Query 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "SQL Server rechazo la operacion de fixture DEV. Salida=$($result -join ' ')"
    }
    return $result
}

function Get-ProtectedSnapshot {
    $query = @'
SET NOCOUNT ON;
IF DB_NAME() <> N'gestionasistenciadb' THROW 58100, 'Unexpected database.', 1;
IF CAST(SERVERPROPERTY('Edition') AS NVARCHAR(128)) NOT LIKE 'Developer%' THROW 58101, 'Developer Edition required.', 1;
IF EXISTS (SELECT 1 FROM dbo.CatalogoParametro WHERE grupo=N'OPERACION' AND clave=N'AMBIENTE' AND UPPER(valor) IN (N'PRODUCCION',N'PROD',N'PRODUCTION')) THROW 58102, 'Production database blocked.', 1;
IF (SELECT COUNT(*) FROM dbo.Grupo WHERE nombre IN (N'FIX-PAG-300 Paginacion 300 estudiantes', N'FIX-PAG-1000 Paginacion 1000 estudiantes', N'FIX-PAG-5000 Paginacion 5000 estudiantes')) <> 3 THROW 58103, 'Expected pagination groups are missing.', 1;
IF EXISTS (SELECT 1 FROM dbo.Grupo WHERE nombre=N'FIX-PAG-300 Paginacion 300 estudiantes' AND cantidadEstudiantes <> 300) THROW 58104, 'FIX-PAG-300 counter mismatch.', 1;
IF EXISTS (SELECT 1 FROM dbo.Grupo WHERE nombre=N'FIX-PAG-1000 Paginacion 1000 estudiantes' AND cantidadEstudiantes <> 1000) THROW 58105, 'FIX-PAG-1000 counter mismatch.', 1;
IF EXISTS (SELECT 1 FROM dbo.Grupo WHERE nombre=N'FIX-PAG-5000 Paginacion 5000 estudiantes' AND cantidadEstudiantes <> 5000) THROW 58106, 'FIX-PAG-5000 counter mismatch.', 1;
DECLARE @studentHash VARCHAR(64) = CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', COALESCE((SELECT STRING_AGG(CONVERT(VARCHAR(MAX), CONVERT(CHAR(36), id)), ',') WITHIN GROUP (ORDER BY id) FROM dbo.Estudiante WHERE LEFT(CONVERT(CHAR(36), id), 8) = 'DE000002'), '')), 2);
DECLARE @enrollmentHash VARCHAR(64) = CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', COALESCE((SELECT STRING_AGG(CONVERT(VARCHAR(MAX), CONCAT(CONVERT(CHAR(36), estudiante), ':', CONVERT(CHAR(36), grupo), ':', CONVERT(CHAR(36), id))), ',') WITHIN GROUP (ORDER BY estudiante, grupo, id) FROM dbo.EstudianteGrupo WHERE grupo IN (SELECT id FROM dbo.Grupo WHERE nombre LIKE N'FIX-PAG-%')), '')), 2);
SELECT CONCAT('SNAPSHOT|students=', (SELECT COUNT(*) FROM dbo.Estudiante WHERE LEFT(CONVERT(CHAR(36), id), 8)='DE000002'), '|enrollments=', (SELECT COUNT(*) FROM dbo.EstudianteGrupo WHERE grupo IN (SELECT id FROM dbo.Grupo WHERE nombre LIKE N'FIX-PAG-%')), '|sessions=', (SELECT COUNT(*) FROM dbo.Sesion WHERE grupo IN (SELECT id FROM dbo.Grupo WHERE nombre LIKE N'FIX-PAG-%')), '|attendance=', (SELECT COUNT(*) FROM dbo.Asistencia a JOIN dbo.Sesion s ON s.id=a.sesion WHERE s.grupo IN (SELECT id FROM dbo.Grupo WHERE nombre LIKE N'FIX-PAG-%')), '|studentHash=', @studentHash, '|enrollmentHash=', @enrollmentHash);
SELECT g.nombre, g.id AS grupoId, d.id AS docenteId, u.id AS idUsuario, (SELECT COUNT(*) FROM dbo.EstudianteGrupo eg WHERE eg.grupo=g.id) AS matriculas, (SELECT COUNT(*) FROM dbo.Horario h WHERE h.grupo=g.id) AS horarios
FROM dbo.Grupo g JOIN dbo.Docente d ON d.id=g.docente JOIN dbo.Usuario u ON u.id=d.usuario
WHERE g.nombre LIKE N'FIX-PAG-%' ORDER BY g.nombre;
SELECT h.id, h.grupo, g.nombre, h.dia, h.horaInicio, h.horaFin
FROM dbo.Horario h JOIN dbo.Grupo g ON g.id=h.grupo WHERE g.nombre LIKE N'FIX-PAG-%' ORDER BY g.nombre, h.id;
'@
    return @(Invoke-DevSql -Query $query)
}

function Invoke-Backup {
    $timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
    $fileName = "maint003l_pagination_e2e_pre_$timestamp.bak"
    $hostPath = Join-Path $BackupDirectory $fileName
    if (-not (Test-Path -LiteralPath $BackupDirectory)) {
        New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null
    }
    & docker exec $ContainerName mkdir -p /var/opt/mssql/backup | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible preparar el directorio de backup dentro del contenedor.' }
    $backupPath = "/var/opt/mssql/backup/$fileName"
    $backupQuery = "BACKUP DATABASE [gestionasistenciadb] TO DISK=N'$backupPath' WITH COPY_ONLY, CHECKSUM, INIT;"
    Invoke-DevSql -Query $backupQuery | Out-Null
    & docker cp "${ContainerName}:$backupPath" $hostPath | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible copiar el backup al host.' }
    Invoke-DevSql -Query "RESTORE VERIFYONLY FROM DISK=N'$backupPath' WITH CHECKSUM;" | Out-Null
    Write-Host "[OK] Backup verificado: $hostPath"
}

function Invoke-ScheduleAction {
    param([ValidateSet('Prepare', 'Cleanup')][string]$Mode)
    $query = @"
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'gestionasistenciadb' THROW 58200, 'Unexpected database.', 1;
IF CAST(SERVERPROPERTY('Edition') AS NVARCHAR(128)) NOT LIKE 'Developer%' THROW 58201, 'Developer Edition required.', 1;
IF EXISTS (SELECT 1 FROM dbo.CatalogoParametro WHERE grupo=N'OPERACION' AND clave=N'AMBIENTE' AND UPPER(valor) IN (N'PRODUCCION',N'PROD',N'PRODUCTION')) THROW 58202, 'Production database blocked.', 1;
DECLARE @day UNIQUEIDENTIFIER = (SELECT TOP (1) id FROM dbo.Dia WHERE codigo=N'LU' ORDER BY id);
IF @day IS NULL THROW 58203, 'Catalog day LU is missing.', 1;
DECLARE @targets TABLE (n INT NOT NULL PRIMARY KEY, groupName NVARCHAR(100) NOT NULL, groupId UNIQUEIDENTIFIER NULL, scheduleId UNIQUEIDENTIFIER NOT NULL);
DECLARE @affected INT = 0;
INSERT INTO @targets(n, groupName, scheduleId) VALUES
 (1, N'FIX-PAG-300 Paginacion 300 estudiantes', 'DE00000B-0000-4000-8000-000000000001'),
 (2, N'FIX-PAG-1000 Paginacion 1000 estudiantes', 'DE00000B-0000-4000-8000-000000000002'),
 (3, N'FIX-PAG-5000 Paginacion 5000 estudiantes', 'DE00000B-0000-4000-8000-000000000003');
UPDATE t SET groupId=g.id FROM @targets t JOIN dbo.Grupo g ON g.nombre=t.groupName;
IF EXISTS (SELECT 1 FROM @targets WHERE groupId IS NULL) THROW 58204, 'A pagination group is missing.', 1;
IF EXISTS (SELECT 1 FROM dbo.Horario h JOIN @targets t ON t.scheduleId=h.id WHERE h.grupo <> t.groupId) THROW 58205, 'Reserved schedule ID belongs to another group.', 1;
BEGIN TRANSACTION;
IF N'$Mode' = N'Prepare'
BEGIN
    INSERT INTO dbo.Horario(id, grupo, dia, horaInicio, horaFin)
    SELECT t.scheduleId, t.groupId, @day, CAST('08:00' AS TIME), CAST('10:00' AS TIME)
    FROM @targets t
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Horario h WHERE h.grupo=t.groupId);
    SET @affected = @@ROWCOUNT;
END
ELSE
BEGIN
    DELETE h FROM dbo.Horario h JOIN @targets t ON t.scheduleId=h.id;
    SET @affected = @@ROWCOUNT;
END;
COMMIT TRANSACTION;
SELECT CONCAT('SCHEDULE_', UPPER(N'$Mode'), '|createdOrDeleted=', @affected);
"@
    Invoke-DevSql -Query $query | ForEach-Object { Write-Host $_ }
}

Write-Host "== Pagination fixture E2E :: $Action ==" -ForegroundColor Magenta
$before = @(Get-ProtectedSnapshot)
$before | ForEach-Object { Write-Host $_ }

if ($Action -eq 'Prepare') {
    if ($PSCmdlet.ShouldProcess('sql_server_asistencias/gestionasistenciadb', 'Crear horarios DEV faltantes')) {
        Invoke-Backup
        Invoke-ScheduleAction -Mode Prepare
    }
} elseif ($Action -eq 'Cleanup') {
    if ($PSCmdlet.ShouldProcess('horarios con IDs reservados DE00000B-*', 'Eliminar horarios propios E2E')) {
        Invoke-ScheduleAction -Mode Cleanup
    }
}

$after = @(Get-ProtectedSnapshot)
$after | ForEach-Object { Write-Host $_ }
$beforeSnapshot = $before | Where-Object { $_ -like 'SNAPSHOT|*' }
$afterSnapshot = $after | Where-Object { $_ -like 'SNAPSHOT|*' }
if (($beforeSnapshot -join '') -ne ($afterSnapshot -join '')) {
    throw 'PROTECTED_DATA_CHANGED: estudiantes/matriculas/sesiones/asistencias no coinciden antes y despues.'
}
Write-Host '[OK] Snapshot protegido sin cambios.' -ForegroundColor Green
