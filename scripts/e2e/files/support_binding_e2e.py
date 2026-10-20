"""LB-004D — E2E HTTP real del soporte de revision: Keycloak + backend + SQL Server + MinIO + ClamAV.

Flujo: el estudiante sube un soporte (ClamAV -> MinIO -> borrador DRAFT en SQL), radica con soporteArchivoId
(DRAFT -> ATTACHED en la misma transaccion que crea la solicitud) y el docente TITULAR lista el reclamo y descarga el
soporte con autorizacion resuelta en SQL; SHA-256 identico. Casos negativos: docente ajeno, otro estudiante del mismo
grupo, administrador, coordinador, archivo ajeno/inexistente/ya usado, EICAR, > 5 MiB, nombre > 250, sin Bearer.

Reutiliza los helpers de files_storage_e2e.py (identidades dedicadas, Keycloak, multipart). No usa mocks. Nunca imprime
tokens, passwords, claves ni object keys. Las aserciones contra SQL usan `docker exec` + sqlcmd con la clave en la
variable de entorno SQLCMDPASSWORD del proceso (si falta, esas aserciones quedan en NOT_RUN y el exit code es 2).

Fases:  main | idempotency | after-restart | clamav-down | minio-down | sql-down | cleanup
`main` e `idempotency` limpian por defecto sus filas y objetos (marker E2E-LB004D-<run>); use --keep solo para
el flujo main -> reinicio -> after-restart -> cleanup.
Exit code: 0 todos PASS, 1 algun FAIL, 2 ambiente incompleto (BLOCKED / NOT_RUN).
"""

import argparse
import json
import os
import re
import subprocess
import sys
import threading
import time
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import files_storage_e2e as base  # noqa: E402

SQL_CONTAINER = os.environ.get("E2E_SQL_CONTAINER", "sql_server_asistencias")
# Sesion 1 del "Grupo 001 - Arq Software" (semilla): UTC_V2, 2026-08-17T13:00:00Z. Titular: E2E_DOCENTE.
DEFAULT_SESSION = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"
EXPECTED_SESSION_UTC = "2026-08-17T13:00:00Z"
UTC_INSTANT = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,7})?Z$")
MARKER_PREFIX = "E2E-LB004D-"
SQL_NOT_RUN = []


# ---------------------------------------------------------------- SQL (aserciones de estado, solo lectura/limpieza acotada)

def sql_rows(query):
    """Devuelve filas (lista de listas de str) o None si no hay clave SQL disponible."""
    if not os.environ.get("SQLCMDPASSWORD"):
        return None
    result = subprocess.run(
        ["docker", "exec", "-e", "SQLCMDPASSWORD", SQL_CONTAINER, "/opt/mssql-tools18/bin/sqlcmd", "-S", "localhost",
         "-U", "sa", "-C", "-I", "-d", "gestionasistenciadb", "-b", "-h", "-1", "-W", "-s", "|", "-Q",
         "SET NOCOUNT ON; " + query],
        capture_output=True, text=True, timeout=60)
    if result.returncode != 0:
        raise base.Blocked("sqlcmd fallo: " + result.stdout.strip()[:200])
    return [line.split("|") for line in result.stdout.splitlines() if line.strip()]


def sql_check(name, query, predicate, detail=""):
    rows = sql_rows(query)
    if rows is None:
        SQL_NOT_RUN.append(name)
        print("%s: NOT_RUN (sin SQLCMDPASSWORD)" % name)
        return None
    return base.check(name, predicate(rows), detail or "filas=%d" % len(rows))


def count_archivos():
    rows = sql_rows("SELECT COUNT(*) FROM dbo.ArchivoSoporte")
    return int(rows[0][0]) if rows else None


MINIO_CONTAINER = os.environ.get("E2E_MINIO_CONTAINER", "asistencias-files-minio-1")
MINIO_BUCKET = os.environ.get("E2E_MINIO_BUCKET", "asistencias-soportes")
UUID_RE = re.compile(r"^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")


def _minio(script):
    """Ejecuta mc DENTRO del contenedor MinIO (usa sus credenciales de entorno; nada se imprime ni sale del contenedor)."""
    result = subprocess.run(
        ["docker", "exec", MINIO_CONTAINER, "sh", "-c",
         'mc alias set e2e http://127.0.0.1:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1 && ' + script],
        capture_output=True, text=True, timeout=60)
    return result.returncode, result.stdout


def count_objects():
    """Cantidad de objetos soportes/* en MinIO; None si el contenedor no esta disponible."""
    try:
        code, out = _minio("mc ls --recursive e2e/%s/soportes/ | wc -l" % MINIO_BUCKET)
    except (OSError, subprocess.SubprocessError):
        return None
    return int(out.strip()) if code == 0 and out.strip().isdigit() else None


def remove_objects(file_ids):
    """Elimina SOLO los objetos soportes/<fileId> creados por este E2E (ids validados como UUID)."""
    removed = 0
    for file_id in file_ids:
        if not UUID_RE.match(file_id):
            continue
        code, _ = _minio("mc rm --force e2e/%s/soportes/%s >/dev/null 2>&1" % (MINIO_BUCKET, file_id))
        removed += 1 if code == 0 else 0
    return removed


# ---------------------------------------------------------------- HTTP

def api(method, url, token=None, payload=None, extra_headers=None):
    headers = dict(extra_headers or {})
    body = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if payload is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(payload).encode()
    return base.http(method, url, headers, body)


def radicar(root, token, session_id, justificacion, soporte=None, correlation=None):
    """Radica un reclamo. `correlation` fija X-Correlation-Id: mismo valor = mismo intento logico (reintento tecnico)."""
    payload = {"sesionId": session_id, "categoria": "Médico / Salud", "justificacion": justificacion}
    if soporte is not None:
        payload["soporteArchivoId"] = soporte
    headers = {"X-Correlation-Id": correlation} if correlation else None
    return api("POST", root + "/api/v1/asistencias/revisiones", token, payload, headers)


def reclamos(root, token, estado="P", tamano=100):
    status, _, raw = api("GET", "%s/api/v1/docente/reclamos?estado=%s&tamano=%d" % (root, estado, tamano), token)
    body = base.json_body(raw)
    return status, body.get("datos") or [], body


def find_reclamo(items, marker):
    for item in items:
        if item.get("justificacionSolicitud") == marker:
            return item
    return None


def context(args):
    kc, users = base.context(args)
    secrets_env = base.load_env(args.secrets)
    if not secrets_env.get("FILES_E2E_DOCENTE2_USERNAME"):
        raise base.Blocked("falta la identidad FILES_E2E_DOCENTE2_*; ejecute files_storage_e2e.py provision-users")
    users["DOCENTE2"] = (secrets_env["FILES_E2E_DOCENTE2_USERNAME"], secrets_env["FILES_E2E_DOCENTE2_PASSWORD"])
    return kc, users


def compressible_pdf(tag):
    return base.pdf_with_embedded((("Soporte LB-004D %s " % tag) * 1500).encode())


# ---------------------------------------------------------------- fases

def phase_main(args):
    kc, users = context(args)
    tokens = base.tokens_for(kc, users)
    root = args.base_url.rstrip("/")
    a, b = tokens["ESTUDIANTE"], tokens["ESTUDIANTE2"]
    titular, ajeno = tokens["DOCENTE"], tokens["DOCENTE2"]
    session_id = args.session_id
    run = uuid.uuid4().hex[:10]
    marker = MARKER_PREFIX + run
    state = {"run": run, "marker": marker, "session": session_id, "files": []}
    before = count_archivos()

    # --- autenticacion y autorizacion de rol
    base.expect_error("E2E-AUTH-01 subir sin Bearer 401", base.upload(root, None, "s.pdf", "application/pdf", b"%PDF-1"), 401)
    base.expect_error("E2E-AUTH-02 listar reclamos sin Bearer 401", api("GET", root + "/api/v1/docente/reclamos"), 401)
    base.expect_error("E2E-AUTH-03 docente no puede subir soportes 403", base.upload(root, titular, "s.pdf", "application/pdf", compressible_pdf("x")), 403)
    base.expect_error("E2E-AUTH-04 docente no puede radicar 403", radicar(root, titular, session_id, marker + "-doc"), 403)
    status, _, _ = api("GET", root + "/api/v1/docente/reclamos", a)
    base.check("E2E-AUTH-05 estudiante no consulta reclamos del docente (403)", status == 403, "HTTP %d" % status)

    # --- E2E-01..03: subida (validacion + ClamAV + MinIO + SQL DRAFT)
    pdf = compressible_pdf(run)
    expected_sha = base.sha256(pdf)
    t0 = time.time()
    file_id = base.expect_upload_ok("E2E-01..03 estudiante sube PDF valido 201", base.upload(root, a, "evidencia medica.pdf", "application/pdf", pdf), len(pdf))
    if not file_id:
        return summarize()
    state["files"].append({"fileId": file_id, "sha256": expected_sha, "size": len(pdf), "contentType": "application/pdf"})
    sql_check("E2E-03 SQL: borrador DRAFT del propietario con checksum y UTC de 7 decimales",
              "SELECT estado, CONVERT(varchar(64), usuarioPropietario), checksumSha256, tamanioBytes, tipoContenido, "
              "CONVERT(varchar(27), fechaRegistroUtc, 126), CASE WHEN solicitudRevisionAsistencia IS NULL THEN 1 ELSE 0 END "
              "FROM dbo.ArchivoSoporte WHERE id = '%s'" % file_id,
              lambda r: len(r) == 1 and r[0][0] == "DRAFT" and r[0][2] == expected_sha and int(r[0][3]) == len(pdf)
              and r[0][4] == "application/pdf" and re.match(r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{7}$", r[0][5]) and r[0][6] == "1")

    # --- propietario descarga su DRAFT; el docente titular aun no (no esta ATTACHED)
    base.expect_download_identical("E2E-OWNER-DRAFT propietario descarga el DRAFT 200 mismo SHA-256", base.download(root, a, file_id), pdf, "application/pdf")
    base.expect_error("E2E-DRAFT-TEACHER docente titular NO descarga un DRAFT 404", base.download(root, titular, file_id), 404)

    # --- E2E-04: radicacion con soporte (DRAFT -> ATTACHED atomico)
    marker_con = marker + "-con-soporte"
    status, _, raw = radicar(root, a, session_id, marker_con, file_id)
    base.check("E2E-04 radicar con soporteArchivoId 202", status == 202, "HTTP %d" % status)
    sql_check("E2E-04 SQL: ATTACHED vinculado a la solicitud con fecha de adjuncion UTC",
              "SELECT a.estado, CONVERT(varchar(27), a.fechaAdjuncionUtc, 126), CASE WHEN a.idCorrelacionAdjuncion IS NULL THEN 0 ELSE 1 END, "
              "s.justificacionSolicitud FROM dbo.ArchivoSoporte a JOIN dbo.SolicitudRevisionAsistencia s ON s.id = a.solicitudRevisionAsistencia "
              "WHERE a.id = '%s'" % file_id,
              lambda r: len(r) == 1 and r[0][0] == "ATTACHED" and r[0][2] == "1" and r[0][3] == marker_con
              and re.match(r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{7}$", r[0][1]))

    # --- E2E-05: el docente titular consulta el reclamo
    status, items, body = reclamos(root, titular)
    item = find_reclamo(items, marker_con)
    soporte = (item or {}).get("soporteAdjunto") or {}
    base.check("E2E-05 docente titular ve el reclamo con su soporte",
               status == 200 and item is not None and soporte.get("fileId") == file_id and soporte.get("tamanioBytes") == len(pdf)
               and soporte.get("tipo") == "application/pdf" and soporte.get("url") == "/api/v1/archivos/" + file_id
               and item.get("estadoSolicitud") == "PENDIENTE", "HTTP %d total=%s" % (status, body.get("total")))
    base.check("E2E-UTC-01 instantes UTC con sufijo Z y hasta 7 decimales (sesion y soporte)",
               bool(item) and UTC_INSTANT.match(str(item.get("fechaSesionUtc") or "")) is not None
               and UTC_INSTANT.match(str(soporte.get("fechaSubidaUtc") or "")) is not None,
               "sesion=%s" % (item or {}).get("fechaSesionUtc"))
    base.check("E2E-UTC-02 la sesion UTC_V2 del seed llega como instante exacto", bool(item) and item.get("fechaSesionUtc") == EXPECTED_SESSION_UTC
               and item.get("procedenciaTemporalSesion") == "UTC_V2", "valor=%s" % (item or {}).get("fechaSesionUtc"))
    base.check("E2E-PRIV-01 el listado del docente no expone correo ni datos del storage",
               bool(item) and "correo" not in json.dumps(item).lower() and base.no_internal_leak(json.dumps(body).encode()))
    state["expected"] = {"fechaSesionUtc": (item or {}).get("fechaSesionUtc"), "fechaSubidaUtc": soporte.get("fechaSubidaUtc"),
                         "marker": marker_con}

    # --- E2E-06/07: descarga autorizada por el docente titular + SHA-256
    response = base.download(root, titular, file_id)
    base.expect_download_identical("E2E-06/07 docente titular descarga 200 con SHA-256 identico", response, pdf, "application/pdf")
    base.expect_download_identical("E2E-OWNER propietario descarga el ATTACHED 200", base.download(root, a, file_id), pdf, "application/pdf")

    # --- E2E-08/09: actores sin relacion => 404 indistinguible de inexistente
    missing = base.download(root, titular, str(uuid.uuid4()))
    missing_body = base.json_body(missing[2])
    for label, token in (("docente ajeno", ajeno), ("estudiante del mismo grupo", b),
                         ("coordinador", tokens["COORDINADOR"]), ("administrador", tokens["ADMIN"])):
        foreign = base.download(root, token, file_id)
        body = base.json_body(foreign[2])
        base.check("E2E-08/09 %s -> 404 indistinguible" % label,
                   foreign[0] == 404 and missing[0] == 404 and body.get("code") == missing_body.get("code")
                   and body.get("message") == missing_body.get("message") and base.no_internal_leak(foreign[2]),
                   "HTTP %d" % foreign[0])
    _, items_ajeno, _ = reclamos(root, ajeno)
    base.check("E2E-08 el docente ajeno no ve el reclamo", find_reclamo(items_ajeno, marker_con) is None)

    # --- E2E-13: doble radicacion con el mismo fileId
    status, _, raw = radicar(root, a, session_id, marker + "-doble", file_id)
    base.check("E2E-13 segunda radicacion con el mismo fileId 404", status == 404, "HTTP %d" % status)
    sql_check("E2E-13 SQL: una sola solicitud con soporte y ninguna para la segunda",
              "SELECT (SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud = '%s'),"
              "(SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud = '%s'),"
              "(SELECT COUNT(*) FROM dbo.ArchivoSoporte WHERE solicitudRevisionAsistencia IS NOT NULL AND id = '%s')"
              % (marker_con, marker + "-doble", file_id),
              lambda r: r[0][0] == "1" and r[0][1] == "0" and r[0][2] == "1")

    # --- soporte ajeno / inexistente: mismo 404
    file_b = base.expect_upload_ok("E2E-FOREIGN estudiante B sube su propio soporte 201", base.upload(root, b, "de-b.pdf", "application/pdf", compressible_pdf("b" + run)), len(compressible_pdf("b" + run)))
    if file_b:
        state["files"].append({"fileId": file_b, "sha256": base.sha256(compressible_pdf("b" + run)), "size": len(compressible_pdf("b" + run)), "contentType": "application/pdf"})
        s_ajeno, _, r_ajeno = radicar(root, a, session_id, marker + "-ajeno", file_b)
        s_falso, _, r_falso = radicar(root, a, session_id, marker + "-falso", str(uuid.uuid4()))
        base.check("E2E-FOREIGN radicar con el soporte de OTRO estudiante 404 identico a inexistente",
                   s_ajeno == 404 and s_falso == 404 and base.json_body(r_ajeno).get("code") == base.json_body(r_falso).get("code"),
                   "HTTP %d/%d" % (s_ajeno, s_falso))
    status, _, _ = radicar(root, a, session_id, marker + "-malformado", "no-es-uuid")
    base.check("E2E-INPUT soporteArchivoId malformado 400", status == 400, "HTTP %d" % status)

    # --- E2E-15: radicar sin soporte (opcional)
    marker_sin = marker + "-sin-soporte"
    status, _, _ = radicar(root, a, session_id, marker_sin)
    base.check("E2E-15 radicar SIN soporte 202", status == 202, "HTTP %d" % status)
    _, items2, _ = reclamos(root, titular)
    sin = find_reclamo(items2, marker_sin)
    base.check("E2E-15 el docente ve el reclamo sin soporte", sin is not None and sin.get("soporteAdjunto") is None)

    # --- E2E-11: seguridad de contenido (nada se almacena ni se registra)
    before_bad = count_archivos()
    base.expect_error("E2E-11 EICAR dentro de un PDF 400", base.upload(root, a, "infectado.pdf", "application/pdf", base.pdf_with_embedded(base.EICAR)), 400, "VALIDATION_ERROR")
    oversize = base.random_with_magic(b"%PDF-", base.MAX_FILE_SIZE + 1, 7)
    base.expect_error("E2E-SIZE archivo de 5 MiB + 1 byte 400", base.upload(root, a, "grande.pdf", "application/pdf", oversize), 400)
    base.expect_error("E2E-NAME nombre de 251 caracteres 400", base.upload(root, a, "a" * 247 + ".pdf", "application/pdf", pdf), 400)
    base.expect_error("E2E-TYPE ejecutable renombrado como PDF 400", base.upload(root, a, "programa.pdf", "application/pdf", b"MZ\x90\x00" + os.urandom(256)), 400)
    after_bad = count_archivos()
    if before_bad is not None:
        base.check("E2E-11 SQL: ningun archivo rechazado dejo fila", before_bad == after_bad, "antes=%s despues=%s" % (before_bad, after_bad))

    with open(args.state, "w", encoding="utf-8") as handle:
        json.dump(state, handle)
    if not args.keep:
        cleanup_state(state)
    return summarize()


def phase_idempotency(args):
    """Idempotencia de la radicacion: reintento con la misma correlacion, concurrencia, rollback y reclamos legitimos."""
    kc, users = context(args)
    tokens = base.tokens_for(kc, users)
    root = args.base_url.rstrip("/")
    a, b, titular = tokens["ESTUDIANTE"], tokens["ESTUDIANTE2"], tokens["DOCENTE"]
    session_id = args.session_id
    run = uuid.uuid4().hex[:10]
    marker = MARKER_PREFIX + run
    state = {"run": run, "marker": marker, "session": session_id, "files": []}
    objects_before, files_before = count_objects(), count_archivos()
    try:
        _idempotency_checks(root, a, b, titular, session_id, marker, state)
        with open(args.state, "w", encoding="utf-8") as handle:
            json.dump(state, handle)
    finally:
        if not args.keep:
            cleanup_state(state)
            objects_after, files_after = count_objects(), count_archivos()
            if objects_before is not None and objects_after is not None:
                base.check("IDEM-CLEAN sin objetos huerfanos en MinIO tras la corrida",
                           objects_after == objects_before, "antes=%s despues=%s" % (objects_before, objects_after))
            if files_before is not None and files_after is not None:
                base.check("IDEM-CLEAN sin filas ArchivoSoporte huerfanas tras la corrida",
                           files_after == files_before, "antes=%s despues=%s" % (files_before, files_after))
    return summarize()


def _upload_pdf(root, token, tag, state):
    pdf = compressible_pdf(tag)
    file_id = base.expect_upload_ok("IDEM-UP subida %s 201" % tag, base.upload(root, token, "idem-%s.pdf" % tag, "application/pdf", pdf), len(pdf))
    if file_id:
        state["files"].append({"fileId": file_id, "sha256": base.sha256(pdf), "size": len(pdf), "contentType": "application/pdf"})
    return file_id


def _count_claims(marker_text):
    rows = sql_rows("SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud = '%s'" % marker_text)
    return None if rows is None else int(rows[0][0])


def _idempotency_checks(root, a, b, titular, session_id, marker, state):
    objects_start = count_objects()

    # IDEM-01: respuesta HTTP perdida despues de persistir; el cliente reintenta con la MISMA correlacion.
    file_1 = _upload_pdf(root, a, "r1-" + state["run"], state)
    if not file_1:
        return
    text_1, corr_1 = marker + "-reintento", str(uuid.uuid4())
    first = radicar(root, a, session_id, text_1, file_1, corr_1)[0]
    retry = radicar(root, a, session_id, text_1, file_1, corr_1)[0]
    base.check("IDEM-01 reintento con la misma correlacion tras exito: 202 y 202 idempotente", (first, retry) == (202, 202), "HTTP %s/%s" % (first, retry))
    sql_check("IDEM-01 SQL: una sola solicitud y un solo soporte ATTACHED",
              "SELECT (SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud = '%s'),"
              "(SELECT COUNT(*) FROM dbo.ArchivoSoporte WHERE id = '%s' AND estado = 'ATTACHED')" % (text_1, file_1),
              lambda r: r[0][0] == "1" and r[0][1] == "1")

    # IDEM-02: cinco peticiones concurrentes con la misma correlacion => una sola solicitud.
    file_2 = _upload_pdf(root, a, "r2-" + state["run"], state)
    text_2, corr_2 = marker + "-concurrente", str(uuid.uuid4())
    statuses, lock = [], threading.Lock()

    def fire():
        status = radicar(root, a, session_id, text_2, file_2, corr_2)[0]
        with lock:
            statuses.append(status)

    threads = [threading.Thread(target=fire) for _ in range(5)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    base.check("IDEM-02 5 peticiones concurrentes con la misma correlacion: todas 202", statuses == [202] * 5, "HTTP %s" % sorted(statuses))
    claims_2 = _count_claims(text_2)
    if claims_2 is None:
        SQL_NOT_RUN.append("IDEM-02-SQL")
    else:
        base.check("IDEM-02 SQL: exactamente una solicitud persistida", claims_2 == 1, "filas=%d" % claims_2)

    # IDEM-03: mismo contenido con OTRA correlacion tras el exito (lo que hacia el cliente sin reutilizar la clave).
    other = radicar(root, a, session_id, text_2, file_2, str(uuid.uuid4()))[0]
    base.check("IDEM-03 misma carga con otra correlacion: el soporte ya vinculado se rechaza (404), sin segunda solicitud",
               other == 404 and _count_claims(text_2) in (1, None), "HTTP %s" % other)

    # IDEM-04: sin soporte el SP v2 no tiene clave de idempotencia (brecha contractual D1.1): se mide, no se aprueba.
    text_4, corr_4 = marker + "-sin-soporte-misma-correlacion", str(uuid.uuid4())
    http_4 = [radicar(root, a, session_id, text_4, None, corr_4)[0] for _ in range(2)]
    claims_4 = _count_claims(text_4)
    print("INFO IDEM-04 GAP-D1.1 (sin soporte, misma correlacion x2): HTTP %s, filas=%s (contrato actual: %s)" % (
        http_4, claims_4, "NO idempotente" if claims_4 == 2 else "idempotente"))
    state["gap_sin_soporte_filas"] = claims_4

    # IDEM-05: dos reclamos legitimos distintos sobre la misma sesion estan permitidos por el contrato.
    legit = [radicar(root, a, session_id, marker + "-legitimo-%d" % i, None, str(uuid.uuid4()))[0] for i in (1, 2)]
    rows = sql_rows("SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud LIKE '%s-legitimo-%%'" % marker)
    base.check("IDEM-05 dos reclamos legitimos distintos (sin restriccion de un reclamo por sesion)",
               legit == [202, 202] and (rows is None or rows[0][0] == "2"), "HTTP %s filas=%s" % (legit, rows[0][0] if rows else "NOT_RUN"))

    # IDEM-06: rollback; soporte de otro estudiante => 404 sin solicitud ni objetos adicionales.
    file_b = _upload_pdf(root, b, "rb-" + state["run"], state)
    text_6 = marker + "-rollback"
    before_claims = _count_claims(text_6)
    status_6 = radicar(root, a, session_id, text_6, file_b, str(uuid.uuid4()))[0]
    base.check("IDEM-06 soporte ajeno: 404 y ninguna solicitud creada (rollback)", status_6 == 404 and _count_claims(text_6) in (0, None), "HTTP %s" % status_6)
    sql_check("IDEM-06 SQL: el soporte ajeno sigue DRAFT sin vinculo",
              "SELECT estado, CASE WHEN solicitudRevisionAsistencia IS NULL THEN 1 ELSE 0 END FROM dbo.ArchivoSoporte WHERE id = '%s'" % file_b,
              lambda r: r[0][0] == "DRAFT" and r[0][1] == "1")

    # Conteo final: tres subidas => tres objetos nuevos (ninguno duplicado por los reintentos).
    objects_end = count_objects()
    if objects_start is not None and objects_end is not None:
        base.check("IDEM-07 MinIO: un objeto por subida, ninguno duplicado por reintentos/concurrencia",
                   objects_end - objects_start == 3, "delta=%d" % (objects_end - objects_start))
    docente_items = reclamos(root, titular, estado="P", tamano=200)[1]
    visible = [i for i in docente_items if str(i.get("justificacionSolicitud", "")).startswith(marker)]
    print("INFO IDEM-08 el docente titular ve %d reclamos de la corrida" % len(visible))


def phase_after_restart(args):
    kc, users = context(args)
    tokens = base.tokens_for(kc, users)
    root = args.base_url.rstrip("/")
    with open(args.state, encoding="utf-8") as handle:
        state = json.load(handle)
    item_state = state["files"][0]
    pdf_sha = item_state["sha256"]
    status, headers, raw = base.download(root, tokens["DOCENTE"], item_state["fileId"])
    base.check("E2E-10 tras reiniciar, el docente titular sigue descargando 200 con el mismo SHA-256",
               status == 200 and base.sha256(raw) == pdf_sha, "HTTP %d zona=%s" % (status, args.label))
    status, items, _ = reclamos(root, tokens["DOCENTE"])
    item = find_reclamo(items, state["expected"]["marker"])
    base.check("E2E-10 tras reiniciar, el reclamo sigue visible con su soporte",
               item is not None and (item.get("soporteAdjunto") or {}).get("fileId") == item_state["fileId"])
    base.check("E2E-UTC-03 mismos instantes UTC con otra zona horaria del host (%s)" % args.label,
               item is not None and item.get("fechaSesionUtc") == state["expected"]["fechaSesionUtc"]
               and (item.get("soporteAdjunto") or {}).get("fechaSubidaUtc") == state["expected"]["fechaSubidaUtc"],
               "sesion=%s" % (item or {}).get("fechaSesionUtc"))
    foreign = base.download(root, tokens["ESTUDIANTE2"], item_state["fileId"])
    base.check("E2E-10 tras reiniciar, otro estudiante sigue recibiendo 404", foreign[0] == 404, "HTTP %d" % foreign[0])
    return summarize()


def phase_clamav_down(args):
    kc, users = context(args)
    token = base.user_token(kc, *users["ESTUDIANTE"])
    root = args.base_url.rstrip("/")
    before = count_archivos()
    base.expect_error("E2E-12 ClamAV caido: la subida falla cerrada (500)", base.upload(root, token, "s.pdf", "application/pdf", compressible_pdf("clamav-down")), 500, "INTERNAL_APPLICATION_ERROR")
    if before is not None:
        base.check("E2E-12 SQL: sin fila cuando ClamAV esta caido", count_archivos() == before)
    return summarize()


def phase_minio_down(args):
    kc, users = context(args)
    tokens = base.tokens_for(kc, users)
    root = args.base_url.rstrip("/")
    with open(args.state, encoding="utf-8") as handle:
        state = json.load(handle)
    before = count_archivos()
    base.expect_error("E2E-12 MinIO caido: la subida falla (500) sin filtrar internals", base.upload(root, tokens["ESTUDIANTE"], "s.pdf", "application/pdf", compressible_pdf("minio-down")), 500)
    if before is not None:
        base.check("E2E-12 SQL: sin fila cuando MinIO esta caido (el put falla antes de registrar)", count_archivos() == before)
    response = base.download(root, tokens["DOCENTE"], state["files"][0]["fileId"])
    base.expect_error("E2E-12 MinIO caido: la descarga autorizada falla 500 sin filtrar internals", response, 500)
    return summarize()


def phase_sql_down(args):
    kc, users = context(args)
    tokens = base.tokens_for(kc, users)
    root = args.base_url.rstrip("/")
    started = time.time()
    status, _, raw = base.upload(root, tokens["ESTUDIANTE"], "s.pdf", "application/pdf", compressible_pdf("sql-down"))
    base.check("E2E-12 SQL caido: la subida falla (5xx) sin filtrar internals", 500 <= status < 600 and base.no_internal_leak(raw), "HTTP %d en %.1fs" % (status, time.time() - started))
    status, _, raw = api("GET", root + "/api/v1/docente/reclamos", tokens["DOCENTE"])
    base.check("E2E-12 SQL caido: el listado del docente falla (5xx)", 500 <= status < 600 and base.no_internal_leak(raw), "HTTP %d" % status)
    return summarize()


def cleanup_state(state, label="CLEANUP"):
    """Elimina filas (marker E2E-LB004D-<run>) y objetos creados por la corrida. No toca datos ajenos al marker/fileIds."""
    marker = state["marker"]
    ids = [item["fileId"] for item in state["files"] if UUID_RE.match(item["fileId"])]
    id_list = ",".join("'%s'" % item for item in ids) or "'00000000-0000-0000-0000-000000000000'"
    rows = sql_rows(
        "DELETE FROM dbo.ArchivoSoporte WHERE id IN (%s) OR solicitudRevisionAsistencia IN "
        "(SELECT id FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud LIKE '%s%%');"
        "DELETE FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud LIKE '%s%%';"
        "SELECT (SELECT COUNT(*) FROM dbo.ArchivoSoporte WHERE id IN (%s)),"
        "(SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud LIKE '%s%%')"
        % (id_list, marker, marker, id_list, marker))
    if rows is None:
        raise base.Blocked("cleanup requiere SQLCMDPASSWORD")
    base.check("%s filas de fixture E2E eliminadas" % label, rows[-1][0] == "0" and rows[-1][1] == "0")
    removed = remove_objects(ids)
    base.check("%s objetos MinIO de la corrida eliminados (%d de %d)" % (label, removed, len(ids)), removed == len(ids))


def phase_cleanup(args):
    with open(args.state, encoding="utf-8") as handle:
        state = json.load(handle)
    cleanup_state(state)
    return summarize()


def summarize():
    failed = [r for r in base.RESULTS if r["result"] != "PASS"]
    print("SUMMARY: %d checks, %d PASS, %d FAIL, %d SQL NOT_RUN" % (len(base.RESULTS), len(base.RESULTS) - len(failed), len(failed), len(SQL_NOT_RUN)))
    if failed:
        return 1
    return 2 if SQL_NOT_RUN else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("phase", choices=["main", "idempotency", "after-restart", "clamav-down", "minio-down", "sql-down", "cleanup"])
    parser.add_argument("--keycloak-env", required=True)
    parser.add_argument("--secrets", required=True)
    parser.add_argument("--state", default="support-binding-e2e-state.json")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--session-id", default=DEFAULT_SESSION)
    parser.add_argument("--label", default="host")
    parser.add_argument("--keep", action="store_true",
                        help="no limpiar los datos de la corrida (necesario para main -> after-restart)")
    args = parser.parse_args()
    phases = {"main": phase_main, "idempotency": phase_idempotency, "after-restart": phase_after_restart, "clamav-down": phase_clamav_down,
              "minio-down": phase_minio_down, "sql-down": phase_sql_down, "cleanup": phase_cleanup}
    try:
        return phases[args.phase](args)
    except base.Blocked as blocked:
        print("BLOCKED: " + str(blocked))
        return 2


if __name__ == "__main__":
    sys.exit(main())
