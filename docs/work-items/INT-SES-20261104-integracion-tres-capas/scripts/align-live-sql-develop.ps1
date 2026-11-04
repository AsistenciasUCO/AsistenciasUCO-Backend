<#
.SYNOPSIS
  Alinea sql_server_asistencias/gestionasistenciadb con develop DB aplicando SOLO el delta funcional
  detectado el 2026-11-04, leído de una revisión git exacta (no del working tree).
.PARAMETER Package
  A = corte d82ad2f (PRs #76/#77/#78/#79/#80 fusionados por el owner DB): F3, F2, HU112-HU157.
  B = SES-01/SES-04/SES-02 (PRs #81/#82/#83 sin revisión del owner DB): requiere -ApprovedBy del owner DB.
.PARAMETER Mode
  Preflight (solo lectura) | Apply (BACKUP COPY_ONLY + una transacción XACT_ABORT + ROLLBACK ante error) | Verify (solo lectura)
#>
param(
  [ValidateSet('A','B')][string]$Package = 'A',
  [ValidateSet('Preflight','Apply','Verify')][string]$Mode = 'Preflight',
  [string]$AuthorizedBy = '',
  [string]$ApprovedBy = '',
  [string]$BackupDir = ''
)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
$OutputEncoding = [Console]::OutputEncoding
function GitText($rev, $f) {
  $t = (git -C $repo show "${rev}:$f") -join "`n"
  if ($t -match '�|Ã[-¿]|Â[-¿]'.Replace('�',[string][char]0xFFFD).Replace('Ã',[string][char]0xC3).Replace('Â',[string][char]0xC2).Replace('',[string][char]0x80).Replace('¿',[string][char]0xBF)) { throw "Codificacion invalida al leer ${rev}:$f (mojibake): abortado." }
  return $t
}
$repo = 'C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db'
$sp = 'schema/stored-procedures'
if ($Package -eq 'A') {
  $rev = 'd82ad2f'
  $files = @(
    "$sp/usp_validar_gestion_grupo_coordinador_o_titular_interno.sql",
    "$sp/usp_validar_consulta_paginada_interno.sql",
    "$sp/usp_precargar_sesiones_grupo_interno.sql",
    "$sp/usp_precargar_sesiones_grupo.sql",
    "$sp/usp_consultar_oferta_creacion_grupo_docente.sql",
    "$sp/usp_buscar_asignaturas_paginado.sql",
    "$sp/usp_buscar_estudiantes_institucion_paginado.sql",
    "$sp/usp_consultar_docentes_facultad_paginado.sql",
    "$sp/usp_consultar_historial_estudiante.sql",
    "$sp/usp_consultar_programas_creditos_facultad_paginado.sql",
    "$sp/usp_crear_grupo.sql",
    "$sp/usp_guardar_horarios_grupo.sql",
    'schema/security/rol_asistencias_runtime_f2_precarga_sesiones.sql',
    'schema/security/rol_asistencias_runtime_f3_docente_crea_grupo.sql',
    'schema/security/rol_asistencias_runtime_hu112_hu157.sql',
    'schema/seed/04_mensaje.sql')
  $newProcs = 'usp_validar_gestion_grupo_coordinador_o_titular_interno','usp_validar_consulta_paginada_interno','usp_precargar_sesiones_grupo_interno','usp_precargar_sesiones_grupo','usp_consultar_oferta_creacion_grupo_docente','usp_buscar_asignaturas_paginado','usp_buscar_estudiantes_institucion_paginado','usp_consultar_docentes_facultad_paginado','usp_consultar_historial_estudiante','usp_consultar_programas_creditos_facultad_paginado'
  $codes = @('HOR_005')
} else {
  $rev = '11d7d50'
  $files = @(
    "$sp/usp_validar_sesion_ventana_registro_interno.sql",
    "$sp/usp_precargar_sesiones_grupo_interno.sql",
    "$sp/usp_registrar_asistencias_sesion.sql",
    "$sp/usp_registrar_asistencia_estudiante_autonomo.sql",
    "$sp/usp_resolver_solicitud_revision_asistencia.sql",
    'schema/seed/04_mensaje.sql')
  $newProcs = @('usp_validar_sesion_ventana_registro_interno')
  $codes = 'SES_007','SES_008','SES_009','REV_001','REV_002','REV_003','REV_004','REV_005'
}

$pw = (Get-Content "$repo\.env" | Where-Object { $_ -match '^MSSQL_SA_PASSWORD=' } | Select-Object -First 1) -replace '^MSSQL_SA_PASSWORD=', ''
$cs = "Server=127.0.0.1,1433;Database=gestionasistenciadb;User ID=sa;Password=$pw;TrustServerCertificate=True;Encrypt=False;Application Name=align-live-sql-develop"
function Q($cn, $sql, $tx = $null) { $c = $cn.CreateCommand(); $c.CommandText = $sql; $c.CommandTimeout = 600; if ($tx) { $c.Transaction = $tx }; $c.ExecuteScalar() }
function Status($cn) {
  $missing = @($newProcs | Where-Object { (Q $cn "SELECT COUNT(*) FROM sys.procedures WHERE name = '$_'") -eq 0 })
  $inList = "('" + ($codes -join "','") + "')"
  $u = Q $cn "SELECT COUNT(*) FROM dbo.CatalogoMensajeUsuario WHERE codigo IN $inList"
  $t = Q $cn "SELECT COUNT(*) FROM dbo.CatalogoMensajeTecnico WHERE codigo IN $inList"
  "SP nuevos ausentes: $($missing.Count)/$($newProcs.Count) $($missing -join ', ')"
  "mensajes presentes: usuario $u/$($codes.Count), tecnico $t/$($codes.Count)"
  return ,@($missing.Count, $u, $t)
}

$cn = New-Object System.Data.SqlClient.SqlConnection $cs; $cn.Open()
try {
  $db = Q $cn 'SELECT DB_NAME()'; $srv = Q $cn 'SELECT @@SERVERNAME'
  $container = ((docker inspect -f '{{.Config.Hostname}}' sql_server_asistencias) 2>$null)
  "paquete $Package @ $rev | instancia $srv / $db (hostname contenedor sql_server_asistencias=$container)"
  if ($db -ne 'gestionasistenciadb' -or $srv -ne $container) { throw 'Instancia inesperada: abortado.' }
  git -C $repo merge-base --is-ancestor $rev origin/develop; if ($LASTEXITCODE -ne 0) { throw "$rev no está en origin/develop" }
  foreach ($f in $files) { git -C $repo cat-file -e "${rev}:$f"; if ($LASTEXITCODE -ne 0) { throw "falta ${rev}:$f" } }
  $st = Status $cn; $st[0..1]
  "sesiones de usuario abiertas en la base: " + (Q $cn "SELECT COUNT(*) FROM sys.dm_exec_sessions WHERE database_id=DB_ID('gestionasistenciadb') AND session_id<>@@SPID AND is_user_process=1")
  foreach ($f in $files) { $null = GitText $rev $f }
  $seed = GitText $rev 'schema/seed/04_mensaje.sql'
  if ($seed -notmatch ('[Ss]esi' + [char]0x00F3 + 'n')) { throw 'Seed de mensajes sin acentos esperados: posible problema de codificacion.' }
  'codificación UTF-8 de los archivos: OK'
  if ($Mode -eq 'Preflight') { "PREFLIGHT OK: $($files.Count) archivos de $rev."; return }
  if ($Mode -eq 'Verify') { $r = $st[2]; if ($r[0] -eq 0 -and $r[1] -eq $codes.Count -and $r[2] -eq $codes.Count) { 'VERIFY: PASS' } else { 'VERIFY: FAIL' }; return }

  if ($Package -eq 'B' -and [string]::IsNullOrWhiteSpace($ApprovedBy)) { throw 'Paquete B requiere -ApprovedBy del owner DB (revisión formal ausente en PRs #81/#82/#83).' }
  if ([string]::IsNullOrWhiteSpace($AuthorizedBy) -or [string]::IsNullOrWhiteSpace($BackupDir)) { throw 'Apply requiere -AuthorizedBy y -BackupDir.' }
  New-Item -ItemType Directory -Force $BackupDir | Out-Null
  $stamp = Get-Date -Format 'yyyyMMdd_HHmmss'; $bakName = "gestionasistenciadb_pre_align${Package}_$stamp.bak"; $bak = "/var/opt/mssql/data/$bakName"
  Q $cn "BACKUP DATABASE gestionasistenciadb TO DISK = N'$bak' WITH COPY_ONLY, INIT, CHECKSUM" | Out-Null
  Q $cn "RESTORE VERIFYONLY FROM DISK = N'$bak' WITH CHECKSUM" | Out-Null
  $env:MSYS_NO_PATHCONV = '1'; docker cp "sql_server_asistencias:$bak" (Join-Path $BackupDir $bakName) | Out-Null
  "backup COPY_ONLY verificado: $bak -> $(Join-Path $BackupDir $bakName)"
  $tx = $cn.BeginTransaction()
  try {
    Q $cn 'SET XACT_ABORT ON' $tx | Out-Null
    foreach ($f in $files) {
      $text = GitText $rev $f
      $batches = [regex]::Split($text, '^\s*GO\s*;?\s*$', [System.Text.RegularExpressions.RegexOptions]::Multiline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
      $n = 0
      foreach ($b in $batches) { $t = $b.Trim(); if ($t -and $t -notmatch '^USE\s+\[?gestionasistenciadb\]?\s*;?$') { Q $cn $b $tx | Out-Null; $n++ } }
      "aplicado: $f ($n lotes)"
    }
    $tx.Commit(); "COMMIT. Autorizado por: $AuthorizedBy" + $(if ($ApprovedBy) { " | aprobado owner DB: $ApprovedBy" } else { '' })
  } catch { try { $tx.Rollback() } catch {}; "ROLLBACK: $($_.Exception.Message)"; throw }
  $st = Status $cn; $st[0..1]
} finally { $cn.Close() }
