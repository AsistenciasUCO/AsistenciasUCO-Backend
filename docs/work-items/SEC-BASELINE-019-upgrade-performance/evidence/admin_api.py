"""Sonda ADMINISTRADOR por API real (uco-administrador). GET reales; escrituras con cuerpo vacio sobre IDs inexistentes
(no mutan); contraste de roles. Sin tokens en la salida."""
import json
import sys

sys.path.insert(0, r"C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra\security-baseline\scripts")
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

base = "http://127.0.0.1:8080"
env = L.read_env()
UID = "11111111-2222-4333-8444-555555555555"


def call(token, method, path, body=None):
    status, _, raw = L.http(L.plain_opener(), method, base + path, body, headers={"Authorization": "Bearer " + token})
    try:
        data = json.loads(raw or b"{}")
    except ValueError:
        data = {}
    return status, (data if isinstance(data, dict) else {"datos": data})


adm = spa_token.get_token(env, "uco-administrador")["access_token"]
print("auth/me:", (call(adm, "GET", "/api/v1/auth/me")[1].get("datos") or {}).get("roles"))
bad = []
gets = ["/api/v1/admin/indicadores", "/api/v1/admin/tendencia-ausentismo", "/api/v1/admin/comparativa-facultades",
        "/api/v1/admin/facultades", "/api/v1/admin/decanos", "/api/v1/admin/instituciones", "/api/v1/admin/sedes",
        "/api/v1/admin/areas", "/api/v1/admin/espacios-fisicos", "/api/v1/admin/tipos-programa", "/api/v1/admin/parametros",
        "/api/v1/admin/auditoria", "/api/v1/coordinador/periodos-academicos", "/api/v1/docentes", "/api/v1/grupos",
        "/api/v1/estudiantes", "/api/v1/tipos-identificacion", "/api/v1/usuarios/perfil", "/api/v1/realtime/status"]
for path in gets:
    status, data = call(adm, "GET", path)
    d = data.get("datos")
    n = len(d) if isinstance(d, list) else ("obj" if d else "-")
    print(f"GET   {path:<48} {status} datos={n} {data.get('code') or ''}")
    if status != 200:
        bad.append((path, status, data.get("code")))
_, par = call(adm, "GET", "/api/v1/admin/parametros")
rows = par.get("datos") or []
if rows:
    print("  parametro ejemplo:", json.dumps({k: v for k, v in rows[0].items() if not isinstance(v, (dict, list))}, ensure_ascii=False)[:380])

writes = [("POST", "/api/v1/admin/areas"), ("PUT", f"/api/v1/admin/areas/{UID}"), ("PATCH", f"/api/v1/admin/areas/{UID}/estado"),
          ("POST", "/api/v1/admin/facultades"), ("PUT", f"/api/v1/admin/facultades/{UID}"), ("PATCH", f"/api/v1/admin/facultades/{UID}/estado"),
          ("POST", "/api/v1/admin/decanos"), ("PATCH", f"/api/v1/admin/decanos/{UID}/toggle"),
          ("POST", "/api/v1/admin/instituciones"), ("PUT", f"/api/v1/admin/instituciones/{UID}"),
          ("PATCH", f"/api/v1/admin/instituciones/{UID}/toggle-estado"),
          ("POST", "/api/v1/admin/sedes"), ("PUT", f"/api/v1/admin/sedes/{UID}"), ("PATCH", f"/api/v1/admin/sedes/{UID}/estado"),
          ("POST", "/api/v1/admin/espacios-fisicos"), ("PUT", f"/api/v1/admin/espacios-fisicos/{UID}"),
          ("POST", "/api/v1/admin/tipos-programa"), ("PUT", f"/api/v1/admin/tipos-programa/{UID}"),
          ("PUT", f"/api/v1/admin/parametros/{UID}"), ("POST", "/api/v1/admin/periodos-academicos"),
          ("PUT", f"/api/v1/admin/periodos-academicos/{UID}"), ("DELETE", f"/api/v1/admin/periodos-academicos/{UID}"),
          ("POST", "/api/v1/admin/cierre-masivo")]
for method, path in writes:
    status, data = call(adm, method, path, {} if method != "DELETE" else None)
    ok = status in (400, 404, 409, 422)
    print(f"{method:<6} {path[:60]:<60} {status} {data.get('code') or ''} {'OK(sin mutar)' if ok else 'REVISAR'}")
    if not ok:
        bad.append((method + " " + path, status, data.get("code")))

for user in ("uco-decano", "uco-coordinador", "uco-docente", "uco-estudiante"):
    t2 = spa_token.get_token(env, user)["access_token"]
    res = [call(t2, "GET", "/api/v1/admin/indicadores")[0], call(t2, "GET", "/api/v1/admin/decanos")[0],
           call(t2, "POST", "/api/v1/admin/facultades", {})[0], call(t2, "POST", "/api/v1/admin/cierre-masivo", {})[0],
           call(t2, "PUT", f"/api/v1/admin/parametros/{UID}", {})[0]]
    print(f"{user:<16} admin/* -> {res} {'OK(denegado)' if set(res) == {403} else 'REVISAR'}")
    if set(res) != {403}:
        bad.append((user, res, None))
print("Bearer invalido:", call("invalido", "GET", "/api/v1/admin/indicadores")[0])
print("RESUMEN fallos:", bad)
