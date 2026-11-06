"""Aprovisiona SOLO a ana.gomez@uco.edu.co en el laboratorio Security Baseline reutilizando la logica oficial
(provision_asistencias.provision_users) con TEST_USERS restringido a esa identidad. Nunca imprime secretos.
Uso: python -X utf8 -I ana_identity.py check | apply
"""
import json
import os
import sys

SCRIPTS = r"C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra\security-baseline\scripts"
sys.path.insert(0, SCRIPTS)
import provision_asistencias as P  # noqa: E402
import lab_pdp as L  # noqa: E402

USERNAME = "ana.gomez@uco.edu.co"
EMAIL = "ana.gomez@uco.edu.co"
ID_USUARIO = "e1f2a3b4-0000-0000-0000-000000000005"


def check(env):
    kc = P.Keycloak(env)
    out = {}
    for q in (f"username={USERNAME}&exact=true", f"email={EMAIL}&exact=true", f"q=idUsuario:{ID_USUARIO}"):
        _, _, users = kc.call("GET", f"/users?{q}")
        out[q.split("=")[0]] = [{"username": u.get("username"), "email": u.get("email"),
                                 "idUsuario": (u.get("attributes") or {}).get("idUsuario")} for u in users]
    return out


def main():
    env = L.read_env()
    mode = sys.argv[1] if len(sys.argv) > 1 else "check"
    before = check(env)
    print(json.dumps({"realm_admin_base": P.Keycloak(env).base if hasattr(P.Keycloak(env), "base") else "ver Keycloak()",
                      "antes": before}, ensure_ascii=False, indent=1))
    if mode != "apply":
        return
    if any(before.values()):
        raise SystemExit("Ya existe una identidad con ese username, correo o idUsuario: no se crea otra.")
    P.TEST_USERS = {"ESTUDIANTE": (USERNAME, EMAIL, ID_USUARIO)}
    evidence = P.provision_users(env)
    print(json.dumps({"provision": evidence, "despues": check(env)}, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
