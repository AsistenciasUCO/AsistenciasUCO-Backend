"""LB-004D — E2E HTTP real de las sesiones del estudiante con su asistencia real.

Verifica contra backend + Keycloak + SQL Server reales (sin mocks) que `GET /api/v1/estudiante/materias/{id}/sesiones`
devuelve el estado de asistencia del ESTUDIANTE AUTENTICADO, que `reclamable` solo es verdadero para una ausencia real
sin solicitud previa, y compara `GET /api/v1/estudiante/horarios` con `dbo.uv_horario_estudiante`.

Precondicion de datos (AISLADA, determinista y reversible): el harness crea sus PROPIAS sesiones E2E-FIX-A y E2E-FIX-B
en el grupo semilla con el procedimiento oficial usp_crear_sesion_v2 (docente titular), registra una ausencia (SJC) de
files.e2e.estudiante1 SOLO en E2E-FIX-B con usp_registrar_asistencias_sesion y, al terminar (incluso si una prueba falla),
elimina unicamente esas dos sesiones y sus dependientes. Nunca modifica asistencias, sesiones ni reclamos reales: las
asistencias historicas o tomadas manualmente en SES-01/SES-02 ya no condicionan el resultado.

Nunca imprime tokens ni passwords. Las aserciones SQL usan `docker exec` + sqlcmd con la clave en SQLCMDPASSWORD
(si falta, el fixture no puede prepararse y el exit code es 2).

Uso (python -I recomendado):
  student_sessions_e2e.py --keycloak-env infra/keycloak/.env --secrets <files.env> [--base-url http://127.0.0.1:8080]
Exit code: 0 todos PASS, 1 algun FAIL, 2 ambiente incompleto (BLOCKED / NOT_RUN).
"""

import argparse
import os
import re
import sys
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import files_storage_e2e as base  # noqa: E402
import support_binding_e2e as sb  # noqa: E402

ASIGNATURA_NOMBRE = "Arquitectura de Software"
SESION_SEMILLA = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"  # solo para resolver el grupo semilla
DOCENTE_TITULAR_USUARIO = "E1F2A3B4-0000-0000-0000-000000000003"
ESTUDIANTE1 = "F1A2B3C4-0000-0000-0000-000000000004"
ESTUDIANTE_GRUPO1 = "B1A2C3D4-0000-0000-0000-000000000001"
FIX_A, FIX_B = "E2E-FIX-A", "E2E-FIX-B"
FIX_A_INICIO, FIX_B_INICIO = "2026-09-07T13:00:00", "2026-09-14T13:00:00"
FIX_A_FIN, FIX_B_FIN = "2026-09-07T15:00:00", "2026-09-14T15:00:00"
ESTADOS = {"PRESENTE", "AUSENTE", "JUSTIFICADA", "NO_DISPONIBLE"}
DTO_KEYS = {"id", "nombre", "numero", "codigo", "numeroSemana", "idGrupo", "codigoGrupo", "nombreGrupo",
            "fechaHoraInicio", "fechaHoraFin", "estadoAsistencia", "estadoSolicitudRevision", "reclamable"}
UTC_WIRE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z?$")


def get(root, path, token=None):
    status, _, raw = sb.api("GET", root + path, token)
    return status, base.json_body(raw)


def sesiones_de(root, token, asignatura):
    status, body = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, token)
    return status, {item["id"].lower(): item for item in (body.get("datos") or [])}, body


# ---------------------------------------------------------------- fixture aislado

def fixture_apply():
    """Crea E2E-FIX-A (sin asistencia) y E2E-FIX-B (ausencia SJC de estudiante1). Devuelve {nombre: id} o None sin SQL."""
    rows = sb.sql_rows(
        "SET QUOTED_IDENTIFIER ON; DECLARE @g UNIQUEIDENTIFIER = (SELECT grupo FROM dbo.Sesion WHERE id = '%(semilla)s');"
        "DECLARE @doc UNIQUEIDENTIFIER = '%(doc)s'; "
        "DECLARE @r TABLE (a UNIQUEIDENTIFIER, b NVARCHAR(4000), c NVARCHAR(4000), d BIT);"
        "DECLARE @c1 UNIQUEIDENTIFIER = NEWID(), @c2 UNIQUEIDENTIFIER = NEWID(), @c3 UNIQUEIDENTIFIER = NEWID();"
        "IF NOT EXISTS (SELECT 1 FROM dbo.Sesion WHERE grupo = @g AND nombre = N'%(a)s') "
        "INSERT @r EXEC dbo.usp_crear_sesion_v2 @idGrupo = @g, @nombre = N'%(a)s', @fechaHoraInicio = '%(ai)s', "
        "@fechaHoraFin = '%(af)s', @idCorrelacion = @c1, @idUsuarioEjecutor = @doc;"
        "IF NOT EXISTS (SELECT 1 FROM dbo.Sesion WHERE grupo = @g AND nombre = N'%(b)s') "
        "INSERT @r EXEC dbo.usp_crear_sesion_v2 @idGrupo = @g, @nombre = N'%(b)s', @fechaHoraInicio = '%(bi)s', "
        "@fechaHoraFin = '%(bf)s', @idCorrelacion = @c2, @idUsuarioEjecutor = @doc;"
        "DECLARE @sb UNIQUEIDENTIFIER = (SELECT id FROM dbo.Sesion WHERE grupo = @g AND nombre = N'%(b)s');"
        "IF @sb IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Asistencia a JOIN dbo.DetalleAsistencia d ON d.asistencia = a.id "
        "WHERE a.sesion = @sb AND a.estudianteGrupo = '%(eg)s') "
        "INSERT @r EXEC dbo.usp_registrar_asistencias_sesion @idSesion = @sb, "
        "@asistenciaJSON = N'[{\"idEstudiante\":\"%(est)s\",\"estado\":\"SJC\"}]', @idCorrelacion = @c3, "
        "@idUsuarioEjecutor = @doc;"
        "SELECT 'FIX', CONVERT(varchar(36), id), nombre FROM dbo.Sesion WHERE grupo = @g AND nombre IN (N'%(a)s', N'%(b)s');"
        "SELECT 'RES', d FROM @r;" % {"semilla": SESION_SEMILLA, "doc": DOCENTE_TITULAR_USUARIO, "a": FIX_A, "b": FIX_B,
                                       "ai": FIX_A_INICIO, "bi": FIX_B_INICIO, "af": FIX_A_FIN, "bf": FIX_B_FIN, "eg": ESTUDIANTE_GRUPO1, "est": ESTUDIANTE1})
    if rows is None:
        return None
    ids = {r[2]: r[1].lower() for r in rows if r[0] == "FIX"}
    failed = [r for r in rows if r[0] == "RES" and r[1] != "1"]
    if len(ids) != 2 or failed:
        raise base.Blocked("no fue posible preparar el fixture aislado (sesiones=%d, SP fallidos=%d)" % (len(ids), len(failed)))
    return ids


def fixture_cleanup():
    """Elimina SOLO E2E-FIX-A/B del grupo semilla y sus dependientes (transaccion unica). Devuelve las sesiones restantes."""
    rows = sb.sql_rows(
        "SET XACT_ABORT ON; DECLARE @g UNIQUEIDENTIFIER = (SELECT grupo FROM dbo.Sesion WHERE id = '%(semilla)s');"
        "DECLARE @s TABLE (id UNIQUEIDENTIFIER); "
        "INSERT @s SELECT id FROM dbo.Sesion WHERE grupo = @g AND nombre IN (N'%(a)s', N'%(b)s');"
        "BEGIN TRAN;"
        "DELETE x FROM dbo.ArchivoSoporte x JOIN dbo.SolicitudRevisionAsistencia r ON r.id = x.solicitudRevisionAsistencia "
        "JOIN dbo.Asistencia s ON s.id = r.asistencia WHERE s.sesion IN (SELECT id FROM @s);"
        "DELETE r FROM dbo.SolicitudRevisionAsistencia r JOIN dbo.Asistencia s ON s.id = r.asistencia "
        "WHERE s.sesion IN (SELECT id FROM @s);"
        "DELETE d FROM dbo.DetalleAsistencia d JOIN dbo.Asistencia s ON s.id = d.asistencia WHERE s.sesion IN (SELECT id FROM @s);"
        "DELETE FROM dbo.Asistencia WHERE sesion IN (SELECT id FROM @s);"
        "DELETE FROM dbo.Sesion WHERE id IN (SELECT id FROM @s);"
        "COMMIT;"
        "SELECT COUNT(*) FROM dbo.Sesion WHERE grupo = @g AND nombre IN (N'%(a)s', N'%(b)s');"
        % {"semilla": SESION_SEMILLA, "a": FIX_A, "b": FIX_B})
    return None if rows is None else int(rows[-1][0])


# ---------------------------------------------------------------- checks

def run(args, ids):
    sesion_a, sesion_b = ids[FIX_A], ids[FIX_B]
    kc, users = sb.context(args)
    tokens = {}
    for role, (username, password) in users.items():
        try:
            tokens[role] = base.user_token(kc, username, password)
        except base.Blocked:
            print("BLOCKED_IDENTITY: %s no pudo autenticarse (credenciales desalineadas); sus checks quedan NOT_RUN" % role)
            sb.SQL_NOT_RUN.append("IDENTITY-" + role)
    root = args.base_url.rstrip("/")
    est1, est2, titular = tokens.get("ESTUDIANTE"), tokens.get("ESTUDIANTE2"), tokens.get("DOCENTE")
    if not (est2 and titular):
        raise base.Blocked("faltan identidades indispensables (ESTUDIANTE2 / DOCENTE)")
    if not est1:
        return run_sin_estudiante1(root, est2, titular, sesion_a, sesion_b)

    status, body = get(root, "/api/v1/estudiante/materias", est1)
    materias = body.get("datos") or []
    arq = next((m for m in materias if m.get("nombreAsignatura") == ASIGNATURA_NOMBRE), None)
    base.check("SES-01 estudiante1 ve %s en sus materias" % ASIGNATURA_NOMBRE, status == 200 and arq is not None,
               "HTTP %d, materias=%d" % (status, len(materias)))
    if not arq:
        return summarize()
    asignatura = arq["idAsignatura"]

    status, sesiones, body = sesiones_de(root, est1, asignatura)
    s_a, s_b = sesiones.get(sesion_a), sesiones.get(sesion_b)
    base.check("SES-02 estudiante1 consulta sus sesiones (200) y ve las dos sesiones del fixture",
               status == 200 and s_a is not None and s_b is not None, "HTTP %d, sesiones=%d" % (status, len(sesiones)))
    base.check("SES-03 contrato: claves exactas y estados validos en todas las sesiones devueltas",
               all(set(s.keys()) == DTO_KEYS and s["estadoAsistencia"] in ESTADOS for s in sesiones.values()),
               "claves=%s" % sorted({k for s in sesiones.values() for k in s.keys()} ^ DTO_KEYS))
    base.check("SES-04 sesion sin detalle de asistencia => NO_DISPONIBLE, no reclamable, nunca AUSENTE",
               bool(s_a) and s_a["estadoAsistencia"] == "NO_DISPONIBLE" and s_a["reclamable"] is False
               and s_a["estadoSolicitudRevision"] is None)
    base.check("SES-05 ausencia SJC real => AUSENTE y reclamable",
               bool(s_b) and s_b["estadoAsistencia"] == "AUSENTE" and s_b["reclamable"] is True
               and s_b["estadoSolicitudRevision"] is None)
    base.check("SES-06 fechas de sesion presentes, bien formadas y con el instante UTC sembrado (UTC v2 sin cambios)",
               bool(s_a) and bool(s_b)
               and all(UTC_WIRE.match(s["fechaHoraInicio"] or "") and UTC_WIRE.match(s["fechaHoraFin"] or "") for s in (s_a, s_b))
               and s_a["fechaHoraInicio"].startswith(FIX_A_INICIO) and s_b["fechaHoraInicio"].startswith(FIX_B_INICIO),
               "inicio A=%s B=%s" % ((s_a or {}).get("fechaHoraInicio"), (s_b or {}).get("fechaHoraInicio")))

    sql_check_sessions(sesion_a, sesion_b)

    # --- estudiante ajeno del mismo grupo: no ve la asistencia de estudiante1
    status, sesiones2, _ = sesiones_de(root, est2, asignatura)
    fix2 = [sesiones2.get(sesion_a), sesiones2.get(sesion_b)]
    base.check("SES-07 estudiante2 (mismo grupo) ve las sesiones del fixture pero NINGUNA ausencia ajena",
               status == 200 and all(f is not None for f in fix2)
               and all(f["estadoAsistencia"] == "NO_DISPONIBLE" and f["reclamable"] is False for f in fix2),
               "HTTP %d" % status)
    # --- asignatura inexistente / ajena: sin filas, sin fuga
    status, ajena, _ = sesiones_de(root, est1, str(uuid.uuid4()))
    base.check("SES-08 asignatura ajena/inexistente devuelve lista vacia (sin fuga)", status == 200 and not ajena, "HTTP %d" % status)
    status, _ = get(root, "/api/v1/estudiante/materias/no-es-uuid/sesiones", est1)
    base.check("SES-09 identificador malformado => 4xx", 400 <= status < 500, "HTTP %d" % status)
    # --- autorizacion
    status, _ = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, None)
    base.check("SES-10 sin Bearer => 401", status == 401, "HTTP %d" % status)
    status, _ = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, titular)
    base.check("SES-11 docente (sin rol estudiante) => 403", status == 403, "HTTP %d" % status)
    status, qs = get(root, "/api/v1/estudiante/materias/%s/sesiones?idEstudiante=%s" % (asignatura, uuid.uuid4()), est1)
    base.check("SES-12 un idEstudiante enviado por el cliente se ignora (misma respuesta)",
               status == 200 and {d["id"].lower() for d in (qs.get("datos") or [])} == set(sesiones.keys()), "HTTP %d" % status)

    # --- horarios: HTTP real vs uv_horario_estudiante
    status, hbody = get(root, "/api/v1/estudiante/horarios", est1)
    horarios = hbody.get("datos") or []
    base.check("HOR-01 GET /estudiante/horarios responde 200", status == 200 and isinstance(horarios, list),
               "HTTP %d, filas=%d" % (status, len(horarios)))
    rows = sb.sql_rows("SELECT COUNT(*) FROM dbo.uv_horario_estudiante h INNER JOIN dbo.uv_estudiante_identidad i ON i.id = h.idEstudiante "
                       "WHERE i.idUsuario = '%s'" % os.environ.get("E2E_ESTUDIANTE1_ID_USUARIO", "E1F2A3B4-0000-0000-0000-000000000004"))
    if rows is None:
        sb.SQL_NOT_RUN.append("HOR-02")
        print("HOR-02: NOT_RUN (sin SQLCMDPASSWORD)")
    else:
        base.check("HOR-02 filas HTTP == filas de uv_horario_estudiante", int(rows[0][0]) == len(horarios),
                   "sql=%s http=%d" % (rows[0][0], len(horarios)))
    return summarize()


def run_sin_estudiante1(root, est2, titular, sesion_a, sesion_b):
    """Camino reducido cuando la identidad de estudiante1 no se puede autenticar: valida el cableado real con estudiante2."""
    status, body = get(root, "/api/v1/estudiante/materias", est2)
    arq = next((m for m in (body.get("datos") or []) if m.get("nombreAsignatura") == ASIGNATURA_NOMBRE), None)
    base.check("RED-01 estudiante2 ve %s" % ASIGNATURA_NOMBRE, status == 200 and arq is not None, "HTTP %d" % status)
    if not arq:
        return summarize()
    asignatura = arq["idAsignatura"]
    status, sesiones, _ = sesiones_de(root, est2, asignatura)
    fix = [sesiones.get(sesion_a), sesiones.get(sesion_b)]
    base.check("RED-02 estudiante2: ve las 2 sesiones del fixture, claves exactas del contrato, estados validos",
               status == 200 and all(f is not None for f in fix)
               and all(set(s.keys()) == DTO_KEYS and s["estadoAsistencia"] in ESTADOS for s in sesiones.values()),
               "HTTP %d, sesiones=%d" % (status, len(sesiones)))
    base.check("RED-03 estudiante2 NO ve la ausencia de estudiante1 (aislamiento por estudiante autenticado)",
               all(f is not None and f["estadoAsistencia"] == "NO_DISPONIBLE" and f["reclamable"] is False for f in fix))
    status, _ = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, None)
    base.check("RED-04 sin Bearer => 401", status == 401, "HTTP %d" % status)
    status, _ = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, titular)
    base.check("RED-05 docente => 403", status == 403, "HTTP %d" % status)
    status, qs = get(root, "/api/v1/estudiante/materias/%s/sesiones?idEstudiante=%s" % (asignatura, ESTUDIANTE1), est2)
    qs_fix = [d for d in (qs.get("datos") or []) if d["id"].lower() in (sesion_a, sesion_b)]
    base.check("RED-06 idEstudiante ajeno enviado por el cliente se ignora (sigue sin ver la ausencia)",
               status == 200 and len(qs_fix) == 2 and all(d["estadoAsistencia"] == "NO_DISPONIBLE" for d in qs_fix), "HTTP %d" % status)
    return summarize()


def sql_check_sessions(sesion_a, sesion_b):
    """Contrasta con SQL: E2E-FIX-B tiene detalle asistio=0/SJC y E2E-FIX-A no tiene detalle (solo lectura)."""
    rows = sb.sql_rows(
        "SELECT a.idSesion, ISNULL(CONVERT(varchar(1), d.asistio), 'N'), ISNULL(d.codigoRazonCausa, 'N') "
        "FROM dbo.uv_asistencia a LEFT JOIN dbo.uv_detalle_asistencia d ON d.idAsistencia = a.id "
        "WHERE a.idEstudianteGrupo = '%s' AND a.idSesion IN ('%s', '%s')" % (ESTUDIANTE_GRUPO1, sesion_a, sesion_b))
    if rows is None:
        sb.SQL_NOT_RUN.append("SES-SQL")
        print("SES-SQL: NOT_RUN (sin SQLCMDPASSWORD)")
        return
    por_sesion = {r[0].lower(): (r[1], r[2]) for r in rows}
    base.check("SES-SQL la DB respalda el estado: FIX-B=(asistio 0, SJC); FIX-A sin detalle",
               por_sesion.get(sesion_b) == ("0", "SJC") and por_sesion.get(sesion_a, ("N", "N")) == ("N", "N"),
               "filas=%s" % por_sesion)


def summarize():
    failed = [r for r in base.RESULTS if r["result"] != "PASS"]
    print("SUMMARY: %d checks, %d PASS, %d FAIL, %d SQL NOT_RUN" % (len(base.RESULTS), len(base.RESULTS) - len(failed), len(failed), len(sb.SQL_NOT_RUN)))
    if failed:
        return 1
    return 2 if sb.SQL_NOT_RUN else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--keycloak-env", required=True)
    parser.add_argument("--secrets", required=True)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--keep-fixture", action="store_true", help="no eliminar E2E-FIX-A/B al terminar")
    args = parser.parse_args()
    try:
        ids = fixture_apply()
        if ids is None:
            raise base.Blocked("el fixture aislado requiere SQLCMDPASSWORD; los checks quedan NOT_RUN")
        code, remaining = 2, 0
        try:
            code = run(args, ids)
        finally:
            if not args.keep_fixture:
                remaining = fixture_cleanup()
                base.check("FIX-CLEAN las sesiones E2E-FIX-A/B y sus dependientes fueron eliminadas", remaining == 0,
                           "restantes=%s" % remaining)
        return 1 if remaining not in (0, None) else code
    except base.Blocked as blocked:
        print("BLOCKED: " + str(blocked))
        return 2


if __name__ == "__main__":
    sys.exit(main())
