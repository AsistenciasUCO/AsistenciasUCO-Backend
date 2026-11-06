"""E2E de asignación de perfiles con una identidad SINTÉTICA (no toca a los usuarios existentes).

Uso: python -X utf8 -I infra/security-baseline/scripts/e2e_profile_assignment.py --sql-container NAME --sql-password-file FILE
         [--database DB] [--backend URL]
Crea un usuario nuevo (POST /api/v1/usuarios como ADMINISTRADOR) y comprueba, capa por capa, que:
  1. sin perfil (aunque la cuenta traiga el rol por defecto de Keycloak): DENY del PEP;
  2. con el perfil `Docencia` en el PDP: el PEP permite SOLO los recursos del rol DOCENTE (el backend sigue denegando mientras
     el token no traiga el rol de Keycloak: defensa en profundidad);
  3. con perfil + rol de Keycloak: ALLOW del PEP solo a sus recursos; `GET /api/v1/auth/me` y `GET /api/v1/me/applications`
     lo reflejan;
  4. al revocar el perfil: DENY otra vez, sin concesiones vigentes;
  5. dos perfiles a la vez (Docencia + Coordinación de programa) suman sus roles y nada más.
ALLOW/DENY es la decisión de seguridad (cabecera X-Decision-Id del PEP): la cuenta sintética no tiene fila de Docente ni de
Coordinador en SQL, así que la capa de negocio puede responder 403 propio, que no se confunde con una denegación del PEP.
No crea concesiones universales ni perfiles nuevos y no cambia roles, perfiles ni asignaciones de las cuentas existentes. Al
terminar (también si aborta) revoca todo lo de la identidad sintética y la elimina de Keycloak y de SQL Server; compara las
asignaciones VIGENTES del PDP y el estado SQL con la instantánea inicial. El PDP no borra asignaciones ni usuarios: cierra la
vigencia (`validUntil`) y conserva el historial, y la fila de usuario del PDP queda sin concesiones (se informa como INFO).
Solo corre contra una base desechable (ver `e2e_safety.py`). La contraseña de la cuenta se genera al vuelo y no se imprime.
"""
import datetime
import json
import os
import secrets
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import e2e_baseline as E  # noqa: E402
import e2e_safety as S  # noqa: E402
import lab_pdp as L  # noqa: E402
import provision_asistencias as P  # noqa: E402
import spa_token  # noqa: E402

API_CLIENT = "asistencias-api"
APP = "asistencias-uco"
DOCENTE_PROFILE = "Docencia"
COORDINATOR_PROFILE = "Coordinación de programa"
# Un recurso representativo por rol (GET) y el rol que lo permite.
ROUTES = {"ADMINISTRADOR": "/api/v1/admin/areas", "DECANO": "/api/v1/decano/programas",
          "COORDINADOR": "/api/v1/coordinador/periodos-academicos", "DOCENTE": "/api/v1/docente/horarios",
          "ESTUDIANTE": "/api/v1/estudiante/horarios"}
RESULTS = []


def check(name, condition, detail=""):
    RESULTS.append((name, bool(condition)))
    print(("PASS " if condition else "FAIL ") + name + (f"  [{detail}]" if detail else ""))


def utc_now():
    return datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")


def effective(assignment, now):
    """El PDP no borra una asignación al revocarla: cierra su vigencia (`validUntil`) y conserva la fila como historial."""
    until = assignment.get("validUntil")
    return not until or until[:19] > now


def assignment_state(profiles, roles, fetch):
    """{('profile'|'role', nombre): {(id, userId)}} de las asignaciones VIGENTES (las revocadas son historial, no concesión)."""
    state, now = {}, utc_now()
    for name, profile_id in profiles.items():
        state[("profile", name)] = {(a["id"], a["userId"]) for a in fetch(f"/api/v1/profiles/{profile_id}/assignments")
                                    if effective(a, now)}
    for name, role_id in roles.items():
        state[("role", name)] = {(a["id"], a["userId"]) for a in fetch(f"/api/v1/roles/{role_id}/assignments")
                                 if effective(a, now)}
    return state


def assignment_diff(before, after):
    return [f"{kind} {name}: +{sorted(after.get((kind, name), set()) - before.get((kind, name), set()))} "
            f"-{sorted(before.get((kind, name), set()) - after.get((kind, name), set()))}"
            for kind, name in sorted(set(before) | set(after)) if before.get((kind, name)) != after.get((kind, name))]


def catalog_roles():
    """Roles que el catálogo autoriza en cada recurso representativo (GET): la expectativa sale de la fuente única."""
    catalog = P.load_catalog()
    return {e["path"]: set(e["roles"]) for e in catalog["endpoints"] if e["method"] == "GET" and e["path"] in ROUTES.values()}


def mismatches(held, outcome):
    """Rutas cuyo resultado difiere de lo que el catálogo prevé para quien tiene SOLO los roles `held`.

    `outcome(path)` devuelve "ALLOW", "DENY" u "OTHER" (401/503/etc.). ALLOW <=> el catálogo da ese recurso a algún rol de `held`.
    """
    allowed_by = catalog_roles()
    return [f"{path}: esperado {expected}, obtenido {got}" for path in ROUTES.values()
            for expected, got in [("ALLOW" if allowed_by[path] & held else "DENY", outcome(path))] if expected != got]


def pep_allowed(status, headers):
    """El PEP/PDP permitió la petición (X-Decision-Id). El estado final lo fija la capa de negocio."""
    return status not in (401, 503) and "X-Decision-Id" in headers


def pep_denied(status, headers):
    return status == 403 and "X-Decision-Id" not in headers


def main(argv=None):
    args = S.parse_args(argv if argv is not None else sys.argv[1:], "E2E de asignación de perfiles (identidad sintética).")
    target = S.require_disposable(S.Target(args.sql_container, S.read_password(args.sql_password_file), args.database),
                                  args.backend)
    base_url = args.backend.rstrip("/")
    env = L.read_env()
    realm = f"{L.KEYCLOAK}/admin/realms/{L.REALM}"
    tag = secrets.token_hex(4)
    email = f"e2e.perfil.{tag}@uco.edu.co"
    identification = 988_000_000 + secrets.randbelow(900_000)
    username = str(identification)  # el backend crea la cuenta del realm con la identificación como nombre de usuario
    password = "Aa1!" + L.random_secret(24)  # solo en memoria
    password_key = "LAB_USER_E2E_PERFIL_" + tag.upper() + "_PASSWORD"
    env[password_key] = password

    def kc_call(method, path, body=None, expect=(200, 201, 204, 404)):
        headers = {"Authorization": "Bearer " + L.kc_admin_token(env)}
        status, _, raw = L.http(L.plain_opener(), method, realm + path, body, headers=headers)
        if status not in expect:
            raise SystemExit(f"Keycloak {method} {path}: HTTP {status}")
        return status, (json.loads(raw) if raw else None)

    def keycloak_users():
        return kc_call("GET", f"/users?email={email}&exact=true")[1] or []

    pdp = P.Pdp(env)
    app_id = env["ASISTENCIAS_PDP_APPLICATION_ID"]
    pdp_state = P.read_pdp_state(pdp, app_id)
    profiles, roles = pdp_state["profiles"], pdp_state["roles"]
    for needed in (DOCENTE_PROFILE, COORDINATOR_PROFILE):
        if needed not in profiles:
            raise SystemExit(f"el PDP no tiene el perfil '{needed}': ejecute provision_asistencias.py pdp")
    client = kc_call("GET", f"/clients?clientId={API_CLIENT}")[1][0]
    role_reps = {r["name"]: r for r in kc_call("GET", f"/clients/{client['id']}/roles")[1]}

    if keycloak_users() or target.rows(f"SELECT id FROM dbo.uv_usuario WHERE correo = {S.literal(email)}"):
        raise SystemExit("colisión improbable de la cuenta sintética; reintente")
    before = S.capture_state(target)
    before_assignments = assignment_state(profiles, roles, pdp.paged)
    admin_token = spa_token.get_token(env, "uco-administrador")["access_token"]
    tipo_id = target.scalar("SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion = 'CC'")

    held = {}  # nombre de perfil -> id de asignación creada por este E2E
    problems, aborted = [], None
    pdp_user_id = kc_user_id = None

    def fresh_token():
        return spa_token.get_token(env, username, password=password)["access_token"]

    def call(path, token):
        return E.request(base_url, "GET", path, token=token)

    def denied(path, token):
        status, headers, _ = call(path, token)
        return pep_denied(status, headers)

    def backend_matches(token, held):
        """El backend sigue el catálogo: ALLOW del PEP (X-Decision-Id) o DENY del PEP (403 sin X-Decision-Id), nada intermedio."""
        def outcome(path):
            status, headers, _ = call(path, token)
            return "ALLOW" if pep_allowed(status, headers) else "DENY" if pep_denied(status, headers) else "OTHER"
        return not mismatches(held, outcome)

    def pep_matches(token, held):
        def outcome(path):
            status, headers = P.pep_decision(env, token, "GET", path)
            if status == 204 and headers.get("X-Decision-Id"):
                return "ALLOW"
            return "DENY" if status == 403 else "OTHER"
        return not mismatches(held, outcome)

    def assign_profile(name):
        status, body = pdp.call("POST", f"/api/v1/profiles/{profiles[name]}/assignments",
                                {"profileId": profiles[name], "userId": pdp_user_id, "applicationId": app_id})
        if status != 201:
            raise SystemExit(f"asignar perfil {name}: HTTP {status} {body.get('code')}")
        now = utc_now()
        mine = [a for a in pdp.paged(f"/api/v1/profiles/{profiles[name]}/assignments")
                if a["userId"] == pdp_user_id and effective(a, now)]
        if len(mine) != 1:
            raise SystemExit(f"se esperaba exactamente una asignación vigente de {name} para la identidad sintética")
        held[name] = mine[0]["id"]

    def revoke_profile(name):
        status, _ = pdp.call("DELETE", f"/api/v1/profiles/{profiles[name]}/assignments/{held[name]}")
        if status not in (200, 204):
            raise SystemExit(f"revocar perfil {name}: HTTP {status}")
        held.pop(name)

    def grant_keycloak_role(name):
        kc_call("POST", f"/users/{kc_user_id}/role-mappings/clients/{client['id']}", [role_reps[name]], expect=(204,))

    def revoke_keycloak_role(name):
        kc_call("DELETE", f"/users/{kc_user_id}/role-mappings/clients/{client['id']}", [role_reps[name]], expect=(204,))

    def my_applications():
        opener, _, status, _, _ = L.bff_session(env, username, password_key)
        _, _, raw = L.http(opener, "GET", L.PDP + "/api/v1/me/applications")
        data = (json.loads(raw or b"{}").get("data") or {}).get("applications") or []
        mine = [a for a in data if a.get("name") == APP]
        return status, json.dumps(mine, ensure_ascii=False)

    def me(token):
        status, _, raw = call("/api/v1/auth/me", token)
        return status, ((json.loads(raw or b"{}").get("datos") or {}) if status == 200 else {})

    try:
        try:
            body = {"tipoIdIdentificacion": tipo_id, "numeroIdentificacion": identification, "primerApellido": "Perfil",
                    "segundoApellido": "Prueba", "primerNombre": "Cuenta", "segundoNombre": "Sintetica", "correo": email,
                    "password": password}
            status, _, raw = E.request(base_url, "POST", "/api/v1/usuarios", token=admin_token, body=body)
            if status != 201:
                raise SystemExit(f"POST /api/v1/usuarios: HTTP {status} {raw[:200].decode('utf-8', 'replace')}")
            kc_user = keycloak_users()[0]
            kc_user_id = kc_user["id"]
            # Como las cuentas de laboratorio: correo verificado y sin acciones pendientes.
            kc_call("PUT", f"/users/{kc_user_id}", dict(kc_user, emailVerified=True, requiredActions=[]), expect=(204,))
            # El alta por el backend deja en Keycloak el rol por defecto del realm; se parte de él, no se asume vacío.
            initial_roles = {r["name"] for r in kc_call("GET", f"/users/{kc_user_id}/role-mappings/clients/{client['id']}")[1]}
            sql_id = S.guid(target.scalar(f"SELECT id FROM dbo.uv_usuario WHERE correo = {S.literal(email)}"))
            session_status = L.bff_session(env, username, password_key)[2]  # el PDP lo aprovisiona al primer login
            pdp_user = [u for u in pdp.paged("/api/v1/users") if u["email"].lower() == email]
            if session_status != 200 or len(pdp_user) != 1:
                raise SystemExit(f"el PDP no conoce a la identidad sintética tras la sesión BFF (HTTP {session_status})")
            pdp_user_id = pdp_user[0]["id"]

            # 1. Sin perfil: DENY en todos los recursos, aunque el token ya traiga el rol por defecto de Keycloak.
            token = fresh_token()
            check("sin perfil: ningún recurso permitido por el PEP", pep_matches(token, set()))
            check(f"sin perfil (rol de Keycloak {sorted(initial_roles)}): el backend deniega todo recurso de rol (403 del PEP)",
                  backend_matches(token, set()))
            check("sin perfil: GET /api/v1/auth/me -> 403 del PEP", denied("/api/v1/auth/me", token))

            # 2. Perfil Docencia en el PDP, sin el rol DOCENTE en el token.
            assign_profile(DOCENTE_PROFILE)
            check("perfil Docencia: el PEP permite exactamente lo que el catálogo da a DOCENTE",
                  pep_matches(token, {"DOCENTE"}))
            check("perfil Docencia sin rol DOCENTE en el token: el backend sigue denegando (403 de Spring)",
                  call(ROUTES["DOCENTE"], token)[0] == 403)

            # 3. Perfil + rol de Keycloak: ALLOW solo para sus recursos y /auth/me + /me/applications lo reflejan.
            grant_keycloak_role("DOCENTE")
            token = fresh_token()
            status, headers, _ = call(ROUTES["DOCENTE"], token)
            check("DOCENTE (perfil + rol): recurso propio -> ALLOW del PEP", pep_allowed(status, headers),
                  f"HTTP {status} (estado final de negocio)")
            check("DOCENTE: el backend sigue el catálogo (ALLOW solo a sus recursos, DENY del PEP al resto)",
                  backend_matches(token, {"DOCENTE"}))
            status, data = me(token)
            check("GET /api/v1/auth/me: UUID institucional correcto y roles == iniciales + DOCENTE",
                  status == 200 and (data.get("idUsuario") or "").lower() == sql_id
                  and set(data.get("roles") or []) == initial_roles | {"DOCENTE"}, f"HTTP {status}")
            session_status, applications = my_applications()
            check("GET /api/v1/me/applications lista el perfil Docencia en asistencias-uco",
                  session_status == 200 and DOCENTE_PROFILE in applications, applications[:160])

            # 4. Revocación del perfil: DENY aunque el token conserve el rol de Keycloak.
            revoke_profile(DOCENTE_PROFILE)
            check("perfil revocado: el PEP deniega todo", pep_matches(token, set()))
            check("perfil revocado: el backend deniega con el mismo token (403 del PEP)", backend_matches(token, set()))
            check("perfil revocado: GET /api/v1/auth/me -> 403 del PEP", denied("/api/v1/auth/me", token))
            check("perfil revocado: /api/v1/me/applications ya no lo lista", DOCENTE_PROFILE not in my_applications()[1])
            revoke_keycloak_role("DOCENTE")

            # 5. Varios roles mediante varios perfiles.
            assign_profile(DOCENTE_PROFILE)
            assign_profile(COORDINATOR_PROFILE)
            grant_keycloak_role("DOCENTE")
            grant_keycloak_role("COORDINADOR")
            token = fresh_token()
            docente, coordinador = call(ROUTES["DOCENTE"], token), call(ROUTES["COORDINADOR"], token)
            check("dos perfiles: DOCENTE y COORDINADOR -> ALLOW del PEP",
                  pep_allowed(docente[0], docente[1]) and pep_allowed(coordinador[0], coordinador[1]),
                  f"HTTP {docente[0]}/{coordinador[0]} (negocio)")
            check("dos perfiles: el backend sigue el catálogo para {DOCENTE, COORDINADOR} (nada más se permite)",
                  backend_matches(token, {"DOCENTE", "COORDINADOR"}))
            status, data = me(token)
            check("GET /api/v1/auth/me con dos perfiles: roles == iniciales + {DOCENTE, COORDINADOR}",
                  status == 200 and set(data.get("roles") or []) == initial_roles | {"DOCENTE", "COORDINADOR"}, f"HTTP {status}")
            applications = my_applications()[1]
            check("GET /api/v1/me/applications lista los dos perfiles",
                  DOCENTE_PROFILE in applications and COORDINATOR_PROFILE in applications)
            revoke_profile(DOCENTE_PROFILE)
            revoke_profile(COORDINATOR_PROFILE)
            check("ambos perfiles revocados: el PEP deniega todo", pep_matches(token, set()))
        except (SystemExit, RuntimeError, KeyError, IndexError) as error:
            aborted = error  # se informa DESPUÉS de limpiar
            print(f"ABORTADO {type(error).__name__}: {error}")
    finally:
        # Revoca lo que siga vigente para la identidad sintética (por usuario, no por lo que "se recuerda"), luego la elimina.
        try:
            if pdp_user_id:
                now = utc_now()
                for profile_id in profiles.values():
                    for a in pdp.paged(f"/api/v1/profiles/{profile_id}/assignments"):
                        if a["userId"] == pdp_user_id and effective(a, now):
                            pdp.call("DELETE", f"/api/v1/profiles/{profile_id}/assignments/{a['id']}")
                for role_id in roles.values():
                    for a in pdp.paged(f"/api/v1/roles/{role_id}/assignments"):
                        if a["userId"] == pdp_user_id and effective(a, now):
                            pdp.call("DELETE", f"/api/v1/roles/{role_id}/assignments/{a['id']}")
        except (SystemExit, RuntimeError, KeyError) as error:
            problems.append(f"limpieza del PDP interrumpida: {error}")
        try:
            for user in keycloak_users():
                kc_call("DELETE", f"/users/{user['id']}")
            if keycloak_users():
                problems.append("la identidad sintética sigue en Keycloak")
        except (SystemExit, RuntimeError) as error:
            problems.append(f"limpieza de Keycloak interrumpida: {error}")
        try:
            S.delete_created_user(target, email, existed_before=False)
            problems += S.restore_and_verify(target, before, tolerated_growth=S.APPEND_ONLY_TABLES)
            for table, rows in S.audit_growth(before, S.capture_state(target)).items():
                print(f"INFO {table}: +{rows} fila(s) de auditoría append-only del backend (no es residuo)")
        except (RuntimeError, ValueError) as error:
            problems.append(f"limpieza SQL interrumpida: {error}")

    try:
        problems += ["asignaciones vigentes del PDP: " + line
                     for line in assignment_diff(before_assignments, assignment_state(profiles, roles, pdp.paged))]
        if pdp_user_id:
            history = sum(1 for profile_id in profiles.values() for a in pdp.paged(f"/api/v1/profiles/{profile_id}/assignments")
                          if a["userId"] == pdp_user_id)
            print(f"INFO el PDP no borra usuarios ni historial: la fila {pdp_user_id} ({email}) queda sin concesiones vigentes "
                  f"y con {history} asignación(es) de perfil revocadas (validUntil)")
    except SystemExit as error:
        problems.append(f"verificación final del PDP interrumpida: {error}")
    check("limpieza: sin concesiones vigentes, sin cuenta sintética en Keycloak ni filas residuales en SQL",
          not problems, "; ".join(problems))
    failed = [name for name, ok in RESULTS if not ok]
    print(f"\n{len(RESULTS) - len(failed)}/{len(RESULTS)} PASS")
    sys.exit(1 if failed or aborted else 0)


if __name__ == "__main__":
    main()
