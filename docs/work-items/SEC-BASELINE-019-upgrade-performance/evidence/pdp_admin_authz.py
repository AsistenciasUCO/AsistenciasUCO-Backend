"""Autorizacion de la API administrativa del PDP 0.1.8 con Bearer de cada rol (solo GET; sin tokens en salida)."""
import json
import sys

sys.path.insert(0, r"C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra\security-baseline\scripts")
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

env = L.read_env()
APP = env["ASISTENCIAS_PDP_APPLICATION_ID"]
paths = ["/api/v1/applications", "/api/v1/users", f"/api/v1/applications/{APP}/security/summary",
         f"/api/v1/applications/{APP}/security/roles?page=0&size=5", f"/api/v1/applications/{APP}/security/administrators?page=0&size=5",
         f"/api/v1/applications/{APP}/security/resources?page=0&size=5"]
for user in ("uco-administrador", "uco-decano", "uco-coordinador", "uco-docente", "uco-estudiante"):
    tok = spa_token.get_token(env, user)["access_token"]
    res = []
    for p in paths:
        status, _, raw = L.http(L.plain_opener(), "GET", L.PDP + p, headers={"Authorization": "Bearer " + tok})
        try:
            code = json.loads(raw or b"{}").get("code")
        except ValueError:
            code = None
        res.append((status, code))
    print(f"{user:<18}", [s for s, _ in res], sorted({c for _, c in res if c}))
