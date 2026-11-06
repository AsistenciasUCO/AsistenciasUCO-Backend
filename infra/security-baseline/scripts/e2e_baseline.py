"""E2E real del modo baseline: PKCE → Bearer → backend (Spring) → PEP/PDP → SQL Server.

Uso: python -X utf8 -I infra/security-baseline/scripts/e2e_baseline.py [--backend http://127.0.0.1:8080] [--pep-down]
Requisitos: instalación infra/security-baseline arriba, `provision_asistencias.py keycloak|pdp|users` ejecutado y el
backend en modo baseline. Sale con código 0 solo si TODAS las comprobaciones pasan. No imprime tokens ni secretos.
--pep-down detiene el contenedor del PEP durante la comprobación de fail-closed y lo vuelve a arrancar.
"""
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lab_pdp as L  # noqa: E402
import spa_token  # noqa: E402

UID = "11111111-2222-4333-8444-555555555555"
RESULTS = []


def check(name, condition, detail=""):
    RESULTS.append((name, bool(condition), detail))
    print(("PASS " if condition else "FAIL ") + name + (f"  [{detail}]" if detail else ""))


def request(base, method, path, token=None, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.status, dict(response.headers), response.read()
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers), error.read()


def main():
    base = "http://127.0.0.1:8080"
    if "--backend" in sys.argv:
        base = sys.argv[sys.argv.index("--backend") + 1]
    env = L.read_env()
    tokens = {role: spa_token.get_token(env, user)["access_token"] for role, user in (
        ("ADMINISTRADOR", "uco-administrador"), ("DECANO", "uco-decano"), ("COORDINADOR", "uco-coordinador"),
        ("DOCENTE", "uco-docente"), ("ESTUDIANTE", "uco-estudiante"))}

    # 401: sin credencial y con credenciales inválidas (nunca 200)
    status, _, _ = request(base, "GET", "/api/v1/docente/horarios")
    check("sin Bearer -> 401", status == 401, status)
    status, _, _ = request(base, "GET", "/api/v1/docente/horarios", token="no-es-un-jwt")
    check("Bearer inválido -> 401", status == 401, status)
    status, _, _ = request(base, "GET", "/api/v1/docente/horarios", headers={"Cookie": "SECURITY_BASELINE_SESSION=forjada"})
    check("cookie de sesión BFF sin Bearer -> 401", status == 401, status)
    status, _, _ = request(base, "GET", "/api/v1/docente/horarios", headers={"X-User-Id": "E1F2A3B4-0000-0000-0000-000000000003"})
    check("X-User-Id enviado por el cliente no autentica -> 401", status == 401, status)

    # ALLOW por rol (GET) y X-Decision-Id del PEP
    for role, path in (("ADMINISTRADOR", "/api/v1/admin/areas"), ("DECANO", "/api/v1/decano/programas"),
                       ("COORDINADOR", "/api/v1/coordinador/periodos-academicos"), ("DOCENTE", "/api/v1/docente/horarios"),
                       ("ESTUDIANTE", "/api/v1/estudiante/horarios")):
        status, headers, _ = request(base, "GET", path, token=tokens[role])
        # ALLOW = Spring autenticó (JWT+vínculo+roles) y el PEP/PDP devolvió 204 con X-Decision-Id. El estado final lo
        # fija la capa de negocio (algunas rutas responden 501 FEATURE_UNAVAILABLE o 500 por permisos SQL del login
        # de pruebas): eso no es una decisión de seguridad.
        check(f"{role} GET {path} -> ALLOW (PEP)", status not in (401, 403, 503) and "X-Decision-Id" in headers,
              f"HTTP {status}")

    # DENY: Spring (rol) y PEP/PDP (política) coinciden
    status, _, _ = request(base, "GET", "/api/v1/estudiante/horarios", token=tokens["DOCENTE"])
    check("DOCENTE GET /estudiante/horarios -> 403", status == 403, status)
    status, _, _ = request(base, "GET", "/api/v1/admin/areas", token=tokens["ESTUDIANTE"])
    check("ESTUDIANTE GET /admin/areas -> 403", status == 403, status)

    # Métodos con efectos (POST/PATCH/PUT/DELETE) y rutas con variables: pasan PEP (no 401/403/503 del PEP)
    for role, method, path, body in (
            ("DOCENTE", "POST", "/api/v1/asistencias/lote", {}),
            ("DOCENTE", "PATCH", f"/api/v1/docente/reclamos/{UID}", {}),
            ("DOCENTE", "PUT", f"/api/v1/sesiones/{UID}", {}),
            ("ESTUDIANTE", "DELETE", f"/api/v1/estudiante/reclamos/{UID}", None),
            ("COORDINADOR", "DELETE", f"/api/v1/coordinador/grupos/{UID}/estudiantes/{UID}", None)):
        status, headers, _ = request(base, method, path, token=tokens[role], body=body)
        check(f"{role} {method} {path.replace(UID, '<uuid>')} atraviesa Spring+PEP", status not in (401, 403, 503)
              and "X-Decision-Id" in headers, status)
    status, _, _ = request(base, "DELETE", f"/api/v1/estudiante/reclamos/{UID}", token=tokens["DOCENTE"])
    check("DOCENTE DELETE /estudiante/reclamos/<uuid> -> 403", status == 403, status)

    # Identidad: vínculo issuer+subject -> Usuario.id estable (segunda petición no depende del correo)
    status, _, _ = request(base, "GET", "/api/v1/docente/horarios", token=tokens["DOCENTE"])
    check("petición repetida con vínculo existente -> 200", status == 200, status)

    if "--pep-down" in sys.argv:
        subprocess.run(["docker", "stop", "asistencias-security-lab-pep-1"], check=True, capture_output=True)
        try:
            time.sleep(2)
            status, _, _ = request(base, "GET", "/api/v1/docente/horarios", token=tokens["DOCENTE"])
            check("PEP caído -> 503 sin bypass", status == 503, status)
        finally:
            subprocess.run(["docker", "start", "asistencias-security-lab-pep-1"], check=True, capture_output=True)
            for _ in range(30):
                time.sleep(2)
                status, _, _ = request(base, "GET", "/api/v1/docente/horarios", token=tokens["DOCENTE"])
                if status == 200:
                    break
            check("PEP recuperado -> 200", status == 200, status)

    failed = [name for name, ok, _ in RESULTS if not ok]
    print(f"\n{len(RESULTS) - len(failed)}/{len(RESULTS)} PASS")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
