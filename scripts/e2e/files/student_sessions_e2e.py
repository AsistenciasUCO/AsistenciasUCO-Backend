"""LB-004D — E2E HTTP real de las sesiones del estudiante con su asistencia real.

Verifica contra backend + Keycloak + SQL Server reales (sin mocks) que `GET /api/v1/estudiante/materias/{id}/sesiones`
devuelve el estado de asistencia del ESTUDIANTE AUTENTICADO, que `reclamable` solo es verdadero para una ausencia real
sin solicitud previa, y compara `GET /api/v1/estudiante/horarios` con `dbo.uv_horario_estudiante`.

Precondicion de datos (solo lectura aqui): `student_absence_fixture.sql apply` deja SES-02 como ausencia (SJC) de
files.e2e.estudiante1; SES-01 queda sin detalle de asistencia (NO_DISPONIBLE).

Nunca imprime tokens ni passwords. Las aserciones SQL usan `docker exec` + sqlcmd con la clave en SQLCMDPASSWORD
(si falta, quedan NOT_RUN y el exit code es 2).

Uso (python -I recomendado):
  student_sessions_e2e.py --keycloak-env infra/keycloak/.env --secrets <files.env> [--base-url http://127.0.0.1:8080]
Exit code: 0 todos PASS, 1 algun FAIL, 2 ambiente incompleto (BLOCKED / NOT_RUN).
"""

import argparse
import json
import os
import re
import sys
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import files_storage_e2e as base  # noqa: E402
import support_binding_e2e as sb  # noqa: E402

ASIGNATURA_NOMBRE = "Arquitectura de Software"
SESION_1 = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"
SESION_2 = "c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f"
ESTADOS = {"PRESENTE", "AUSENTE", "JUSTIFICADA", "NO_DISPONIBLE"}
DTO_KEYS = {"id", "nombre", "numero", "codigo", "numeroSemana", "idGrupo", "codigoGrupo", "nombreGrupo",
            "fechaHoraInicio", "fechaHoraFin", "estadoAsistencia", "estadoSolicitudRevision", "reclamable"}


def get(root, path, token=None):
    status, _, raw = sb.api("GET", root + path, token)
    return status, base.json_body(raw)


def sesiones_de(root, token, asignatura):
    status, body = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, token)
    return status, {item["id"].lower(): item for item in (body.get("datos") or [])}, body


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--keycloak-env", required=True)
    parser.add_argument("--secrets", required=True)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    args = parser.parse_args()
    try:
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
            return run_sin_estudiante1(root, est2, titular)

        status, body = get(root, "/api/v1/estudiante/materias", est1)
        materias = body.get("datos") or []
        arq = next((m for m in materias if m.get("nombreAsignatura") == ASIGNATURA_NOMBRE), None)
        base.check("SES-01 estudiante1 ve %s en sus materias" % ASIGNATURA_NOMBRE, status == 200 and arq is not None,
                   "HTTP %d, materias=%d" % (status, len(materias)))
        if not arq:
            return summarize()
        asignatura = arq["idAsignatura"]

        status, sesiones, body = sesiones_de(root, est1, asignatura)
        base.check("SES-02 estudiante1 consulta sus sesiones (200, 2 sesiones)", status == 200 and len(sesiones) == 2,
                   "HTTP %d, sesiones=%d" % (status, len(sesiones)))
        s1, s2 = sesiones.get(SESION_1), sesiones.get(SESION_2)
        base.check("SES-03 contrato: claves exactas y estados validos",
                   all(set(s.keys()) == DTO_KEYS and s["estadoAsistencia"] in ESTADOS for s in sesiones.values()),
                   "claves=%s" % sorted({k for s in sesiones.values() for k in s.keys()} ^ DTO_KEYS))
        base.check("SES-04 SES-01 sin detalle de asistencia => NO_DISPONIBLE, no reclamable, nunca AUSENTE",
                   bool(s1) and s1["estadoAsistencia"] == "NO_DISPONIBLE" and s1["reclamable"] is False
                   and s1["estadoSolicitudRevision"] is None)
        base.check("SES-05 SES-02 ausencia SJC real => AUSENTE y reclamable",
                   bool(s2) and s2["estadoAsistencia"] == "AUSENTE" and s2["reclamable"] is True
                   and s2["estadoSolicitudRevision"] is None)
        utc = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z?$")
        base.check("SES-06 fechas de sesion presentes y bien formadas (UTC v2 sin cambios)",
                   all(utc.match(s["fechaHoraInicio"] or "") and utc.match(s["fechaHoraFin"] or "") for s in sesiones.values()),
                   "inicio SES-01=%s" % (s1 or {}).get("fechaHoraInicio"))

        sql_check_sessions(sesiones)

        # --- estudiante ajeno del mismo grupo: no ve la asistencia de estudiante1
        status, sesiones2, _ = sesiones_de(root, est2, asignatura)
        base.check("SES-07 estudiante2 (mismo grupo) ve las sesiones pero NINGUNA ausencia ajena",
                   status == 200 and len(sesiones2) == 2
                   and all(s["estadoAsistencia"] == "NO_DISPONIBLE" and s["reclamable"] is False for s in sesiones2.values()),
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
        base.check("HOR-01 GET /estudiante/horarios responde 200", status == 200 and isinstance(horarios, list), "HTTP %d, filas=%d" % (status, len(horarios)))
        rows = sb.sql_rows("SELECT COUNT(*) FROM dbo.uv_horario_estudiante h INNER JOIN dbo.uv_estudiante_identidad i ON i.id = h.idEstudiante "
                           "WHERE i.idUsuario = '%s'" % os.environ.get("E2E_ESTUDIANTE1_ID_USUARIO", "E1F2A3B4-0000-0000-0000-000000000004"))
        if rows is None:
            sb.SQL_NOT_RUN.append("HOR-02")
            print("HOR-02: NOT_RUN (sin SQLCMDPASSWORD)")
        else:
            base.check("HOR-02 filas HTTP == filas de uv_horario_estudiante", int(rows[0][0]) == len(horarios),
                       "sql=%s http=%d" % (rows[0][0], len(horarios)))
        return summarize()
    except base.Blocked as blocked:
        print("BLOCKED: " + str(blocked))
        return 2


def run_sin_estudiante1(root, est2, titular):
    """Camino reducido cuando la identidad de estudiante1 no se puede autenticar: valida el cableado real con estudiante2."""
    status, body = get(root, "/api/v1/estudiante/materias", est2)
    arq = next((m for m in (body.get("datos") or []) if m.get("nombreAsignatura") == ASIGNATURA_NOMBRE), None)
    base.check("RED-01 estudiante2 ve %s" % ASIGNATURA_NOMBRE, status == 200 and arq is not None, "HTTP %d" % status)
    if not arq:
        return summarize()
    asignatura = arq["idAsignatura"]
    status, sesiones, _ = sesiones_de(root, est2, asignatura)
    base.check("RED-02 estudiante2: 2 sesiones, claves exactas del contrato, estados validos",
               status == 200 and len(sesiones) == 2
               and all(set(s.keys()) == DTO_KEYS and s["estadoAsistencia"] in ESTADOS for s in sesiones.values()),
               "HTTP %d, sesiones=%d" % (status, len(sesiones)))
    base.check("RED-03 estudiante2 NO ve la ausencia de estudiante1 (aislamiento por estudiante autenticado)",
               all(s["estadoAsistencia"] == "NO_DISPONIBLE" and s["reclamable"] is False for s in sesiones.values()))
    status, _ = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, None)
    base.check("RED-04 sin Bearer => 401", status == 401, "HTTP %d" % status)
    status, _ = get(root, "/api/v1/estudiante/materias/%s/sesiones" % asignatura, titular)
    base.check("RED-05 docente => 403", status == 403, "HTTP %d" % status)
    status, qs = get(root, "/api/v1/estudiante/materias/%s/sesiones?idEstudiante=%s" % (asignatura, "F1A2B3C4-0000-0000-0000-000000000004"), est2)
    base.check("RED-06 idEstudiante ajeno enviado por el cliente se ignora (sigue sin ver la ausencia)",
               status == 200 and all(d["estadoAsistencia"] == "NO_DISPONIBLE" for d in (qs.get("datos") or [])), "HTTP %d" % status)
    return summarize()


def sql_check_sessions(sesiones):
    """Contrasta con SQL: SES-02 tiene detalle asistio=0/SJC y SES-01 no tiene detalle (solo lectura)."""
    rows = sb.sql_rows(
        "SELECT a.idSesion, ISNULL(CONVERT(varchar(1), d.asistio), 'N'), ISNULL(d.codigoRazonCausa, 'N') "
        "FROM dbo.uv_asistencia a LEFT JOIN dbo.uv_detalle_asistencia d ON d.idAsistencia = a.id "
        "WHERE a.idEstudianteGrupo = 'B1A2C3D4-0000-0000-0000-000000000001'")
    if rows is None:
        sb.SQL_NOT_RUN.append("SES-SQL")
        print("SES-SQL: NOT_RUN (sin SQLCMDPASSWORD)")
        return
    por_sesion = {r[0].lower(): (r[1], r[2]) for r in rows}
    base.check("SES-SQL la DB respalda el estado: SES-02=(asistio 0, SJC); SES-01 sin detalle",
               por_sesion.get(SESION_2) == ("0", "SJC") and por_sesion.get(SESION_1, ("N", "N")) == ("N", "N"),
               "filas=%s" % por_sesion)


def summarize():
    failed = [r for r in base.RESULTS if r["result"] != "PASS"]
    print("SUMMARY: %d checks, %d PASS, %d FAIL, %d SQL NOT_RUN" % (len(base.RESULTS), len(base.RESULTS) - len(failed), len(failed), len(sb.SQL_NOT_RUN)))
    if failed:
        return 1
    return 2 if sb.SQL_NOT_RUN else 0


if __name__ == "__main__":
    sys.exit(main())
