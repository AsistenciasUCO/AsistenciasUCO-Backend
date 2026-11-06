"""Aprovisionamiento idempotente de AsistenciasUCO en Security Baseline 0.1.7.

Uso (desde la raíz del repo, con la instalación de infra/security-baseline arriba):
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py catalog           # offline, sin lab.env
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py keycloak
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py pdp [--prune]
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py users [--migrate-direct]
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py verify            # PDP vs catálogo (lectura)
    python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py verify-decisions  # decisiones reales del PEP

Lee y escribe %USERPROFILE%\\.asistencias-security-lab\\lab.env (fuera de Git). Nunca imprime contraseñas, secretos de
cliente, credenciales de aplicación ni tokens.

catalog   Offline: compara los endpoints de los controladores (extract_endpoints.py) con access-catalog.json. Un endpoint
          sin regla o una regla obsoleta devuelven código 1 e imprimen el esqueleto a completar.
keycloak  Realm central: cliente de recurso `asistencias-api` (cinco roles), cliente público PKCE `asistencias-uco-spa`
          (audiencias, roles, idUsuario, email_verified) y cliente de servicio `asistencias-provisioner` (Admin REST
          mínimo para el IdentityProviderPort del modo baseline).
pdp       PDP: aplicación `asistencias-uco`, recursos, roles, concesiones y perfiles de access-catalog.json. Incremental:
          agrega lo que falta; la deriva (concesiones que el catálogo no autoriza) se informa y solo se revoca con --prune.
users     Usuarios de prueba por rol (correo verificado = correo de las semillas SQL, atributo idUsuario), sesión BFF
          para aprovisionar su identidad en el PDP y asignación de su perfil (--migrate-direct retira la asignación
          directa del rol que el perfil ya cubre).
verify    Solo lectura: PDP vs catálogo; código 1 si falta o sobra algo.
verify-decisions  Solo lectura: decisión real PEP→PDP (ALLOW/DENY) por endpoint del catálogo y rol.
"""
import json
import os
import re
import sys
import urllib.parse

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import extract_endpoints  # noqa: E402
import lab_pdp as L  # noqa: E402

APP_NAME = "asistencias-uco"
API_CLIENT = "asistencias-api"
SPA_CLIENT = "asistencias-uco-spa"
PROVISIONER = "asistencias-provisioner"
USER_ID_ATTRIBUTE = "idUsuario"  # atributo del usuario que vincula la cuenta de Keycloak con Usuario.id de la base de datos
SPA_ORIGINS = ["http://localhost:4200", "http://127.0.0.1:4200"]
ROLES = ["ADMINISTRADOR", "DECANO", "COORDINADOR", "DOCENTE", "ESTUDIANTE"]
ALL = tuple(ROLES)

# Usuarios de prueba: correo y Usuario.id de las semillas de gestion-asistencia-db (schema/seed/12_usuarios_roles.sql).
TEST_USERS = {
    "ADMINISTRADOR": ("uco-administrador", "admin.institucional@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000000"),
    "DECANO": ("uco-decano", "decano.ingenieria@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000001"),
    "COORDINADOR": ("uco-coordinador", "coordinador.sistemas@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000002"),
    "DOCENTE": ("uco-docente", "maria.rostagno@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000003"),
    "ESTUDIANTE": ("uco-estudiante", "carlos.zapata@uco.edu.co", "E1F2A3B4-0000-0000-0000-000000000004"),
}


# ── Catálogo de acceso compartido ───────────────────────────────────────────────────────────────────────────────
# `infra/security-baseline/access-catalog.json` es la ÚNICA definición de qué roles llaman a cada endpoint. La verifica
# AccessCatalogConsistencyTest contra Spring MVC (endpoints existentes) y contra el SecurityFilterChain real (roles);
# este script la replica en el PDP. Un endpoint nuevo sin fila bloquea (`catalog`, `pdp`, `verify`), nunca recibe acceso.
CATALOG_PATH = os.path.join(HERE, "..", "access-catalog.json")
VERBS = {"GET", "POST", "PUT", "PATCH", "DELETE"}


def load_catalog(path=CATALOG_PATH):
    with open(path, encoding="utf-8") as handle:
        catalog = json.load(handle)
    problems = validate_catalog(catalog)
    if problems:
        raise SystemExit("access-catalog.json inválido:\n  " + "\n  ".join(problems))
    return catalog


def validate_catalog(catalog):
    problems = []
    if catalog.get("roles") != ROLES:
        problems.append("`roles` debe ser exactamente los cinco roles institucionales, en orden")
    seen = set()
    for entry in catalog.get("endpoints", []):
        key = (entry["method"], entry["path"])
        label = f"{key[0]} {key[1]}"
        if key in seen:
            problems.append("duplicado: " + label)
        seen.add(key)
        roles = entry.get("roles") or []
        if entry["method"] not in VERBS or not entry["path"].startswith("/api/") or "*" in entry["path"]:
            problems.append("método/ruta inválidos (sin comodines): " + label)
        if not roles or not set(roles) <= set(ROLES) or len(roles) != len(set(roles)):
            problems.append("roles vacíos, repetidos o ajenos: " + label)
        elif (len(roles) == len(ROLES)) != (entry.get("access") == "AUTHENTICATED"):
            problems.append("access=AUTHENTICATED si y solo si concede los cinco roles: " + label)
    for exempt in catalog.get("exempt", []):
        if (exempt["method"], exempt["path"]) in seen or not exempt.get("reason"):
            problems.append("exención inválida: " + exempt["method"] + " " + exempt["path"])
    profile_names = set()
    for profile in catalog.get("profiles", []):
        if profile["name"] in profile_names or not (3 <= len(profile["name"]) <= 60):
            problems.append("perfil duplicado o con nombre fuera de 3-60 caracteres: " + profile["name"])
        profile_names.add(profile["name"])
        if not profile.get("roles") or not set(profile["roles"]) <= set(ROLES):
            problems.append("perfil vacío o con roles ajenos: " + profile["name"])
    return problems


def catalog_pairs(catalog):
    return {(e["method"], e["path"]) for e in catalog["endpoints"]}


def exempt_pairs(catalog):
    return {(e["method"], e["path"]) for e in catalog.get("exempt", [])}


def inventory_gaps(catalog):
    """(sin_regla, obsoletas): endpoints del código ausentes del catálogo y filas del catálogo sin endpoint."""
    discovered = set(extract_endpoints.endpoints())
    known = catalog_pairs(catalog) | exempt_pairs(catalog)
    return sorted(discovered - known, key=lambda x: (x[1], x[0])), sorted(known - discovered, key=lambda x: (x[1], x[0]))


def desired_grants(catalog):
    grants = {role: set() for role in ROLES}
    for entry in catalog["endpoints"]:
        for role in entry["roles"]:
            grants[role].add((entry["method"], entry["path"]))
    return grants


def command_catalog():
    """Offline: el inventario del código debe coincidir con el catálogo; imprime esqueletos para lo que falte."""
    catalog = load_catalog()
    missing, stale = inventory_gaps(catalog)
    for method, path in missing:
        print(f"SIN REGLA  {method} {path}")
        print('   agregar a access-catalog.json: {"method": "%s", "path": "%s", "roles": [<DECIDIR>], '
              '"access": "ROLES", "ownership": null}' % (method, path))
    for method, path in stale:
        print(f"OBSOLETA   {method} {path}  (sin endpoint en los controladores)")
    print(f"{len(catalog['endpoints'])} endpoints en el catálogo, {len(missing)} sin regla, {len(stale)} obsoletos")
    return 1 if missing or stale else 0


# ── Keycloak ────────────────────────────────────────────────────────────────────────────────────────────────────
class Keycloak:
    def __init__(self, env):
        self.env = env
        self.token = L.kc_admin_token(env)
        self.base = f"{L.KEYCLOAK}/admin/realms/{L.REALM}"

    def call(self, method, path, body=None, expect=(200, 201, 204)):
        status, headers, raw = L.http(L.plain_opener(), method, self.base + path, body,
                                      headers={"Authorization": "Bearer " + self.token})
        if status not in expect:
            raise SystemExit(f"Keycloak {method} {path}: HTTP {status}")
        return status, headers, (json.loads(raw) if raw else None)

    def client(self, client_id):
        _, _, found = self.call("GET", "/clients?clientId=" + urllib.parse.quote(client_id))
        return found[0] if found else None

    def ensure_client(self, representation):
        existing = self.client(representation["clientId"])
        if existing:
            merged = dict(existing)
            merged.update({k: v for k, v in representation.items() if k != "protocolMappers"})
            self.call("PUT", f"/clients/{existing['id']}", merged)
            known = {m["name"]: m for m in existing.get("protocolMappers", [])}
            for mapper in representation.get("protocolMappers", []):
                if mapper["name"] in known:
                    self.call("PUT", f"/clients/{existing['id']}/protocol-mappers/models/{known[mapper['name']]['id']}",
                              dict(mapper, id=known[mapper["name"]]["id"]))
                else:
                    self.call("POST", f"/clients/{existing['id']}/protocol-mappers/models", mapper)
            return existing["id"], False
        self.call("POST", "/clients", representation)
        return self.client(representation["clientId"])["id"], True


def provision_keycloak(env, enable_pdp_profile_sync=False):
    kc = Keycloak(env)
    evidence = {}

    # 1. Cliente de recurso con los cinco roles institucionales.
    api = kc.client(API_CLIENT)
    if not api:
        kc.call("POST", "/clients", {"clientId": API_CLIENT, "enabled": True, "bearerOnly": True,
                                     "publicClient": False, "protocol": "openid-connect"})
        api = kc.client(API_CLIENT)
    _, _, existing_roles = kc.call("GET", f"/clients/{api['id']}/roles")
    have = {r["name"] for r in existing_roles}
    for role in ROLES:
        if role not in have:
            kc.call("POST", f"/clients/{api['id']}/roles", {"name": role})
    evidence["api_roles"] = ROLES

    # 2. Cliente público Authorization Code + PKCE (la SPA nunca recibe secretos ni usa password grant).
    spa = {
        "clientId": SPA_CLIENT, "name": "AsistenciasUCO SPA", "protocol": "openid-connect", "enabled": True,
        "publicClient": True, "standardFlowEnabled": True, "implicitFlowEnabled": False,
        "directAccessGrantsEnabled": False, "serviceAccountsEnabled": False, "fullScopeAllowed": False,
        "redirectUris": [o + "/*" for o in SPA_ORIGINS], "webOrigins": SPA_ORIGINS,
        "attributes": {
            "pkce.code.challenge.method": "S256", "post.logout.redirect.uris": "##".join(o + "/*" for o in SPA_ORIGINS),
            "access.token.lifespan": "300",
        },
        "defaultClientScopes": ["security-baseline-claims", "email", "profile"],
        "protocolMappers": [
            _mapper("audience-asistencias-api", "oidc-audience-mapper", {"included.client.audience": API_CLIENT}),
            _mapper("idUsuario", "oidc-usermodel-attribute-mapper", {
                "user.attribute": "idUsuario", "claim.name": "idUsuario", "jsonType.label": "String", "multivalued": "false"}),
            _mapper("email_verified", "oidc-usermodel-property-mapper", {
                "user.attribute": "emailVerified", "claim.name": "email_verified", "jsonType.label": "boolean"}),
            _mapper("asistencias-api-client-roles", "oidc-usermodel-client-role-mapper", {
                "usermodel.clientRoleMapping.clientId": API_CLIENT, "claim.name": "resource_access.${client_id}.roles",
                "jsonType.label": "String", "multivalued": "true"}),
        ],
    }
    spa_id, created = kc.ensure_client(spa)
    _ensure_default_scopes(kc, spa_id, spa["defaultClientScopes"])
    # Con fullScopeAllowed=false el mapper solo emite los roles que están en el scope del cliente.
    _, _, api_role_reps = kc.call("GET", f"/clients/{api['id']}/roles")
    kc.call("POST", f"/clients/{spa_id}/scope-mappings/clients/{api['id']}", api_role_reps)
    evidence["spa_client"] = {"created": created, "pkce": "S256", "origins": SPA_ORIGINS}

    # El logout BFF del PDP (/api/v1/session/logout) vuelve al origen de la SPA: debe estar entre los post-logout
    # del cliente BFF del PDP. Se AÑADEN los de la SPA conservando los existentes.
    bff = kc.client("security-baseline-bff")
    current = bff.get("attributes", {}).get("post.logout.redirect.uris", "")
    entries = [e for e in current.split("##") if e]
    for origin in SPA_ORIGINS:
        for entry in (origin, origin + "/*"):
            if entry not in entries:
                entries.append(entry)
    bff["attributes"]["post.logout.redirect.uris"] = "##".join(entries)
    kc.call("PUT", f"/clients/{bff['id']}", bff)
    evidence["bff_post_logout"] = "SPA origins added"

    # 3. Cliente de servicio para el aprovisionamiento del backend (Admin REST mínimo).
    prov = {"clientId": PROVISIONER, "name": "AsistenciasUCO provisioner", "protocol": "openid-connect", "enabled": True,
            "publicClient": False, "serviceAccountsEnabled": True, "standardFlowEnabled": False,
            "directAccessGrantsEnabled": False, "fullScopeAllowed": False, "clientAuthenticatorType": "client-secret"}
    prov_id, created = kc.ensure_client(prov)
    _, _, svc_user = kc.call("GET", f"/clients/{prov_id}/service-account-user")
    rm = kc.client("realm-management")
    _, _, rm_roles = kc.call("GET", f"/clients/{rm['id']}/roles")
    wanted = [r for r in rm_roles if r["name"] in ("manage-users", "view-users", "query-users", "view-clients", "query-clients")]
    kc.call("POST", f"/users/{svc_user['id']}/role-mappings/clients/{rm['id']}", wanted)
    # Con fullScopeAllowed=false el token solo incluye los roles que están en el scope del cliente.
    kc.call("POST", f"/clients/{prov_id}/scope-mappings/clients/{rm['id']}", wanted)
    _, _, secret = kc.call("GET", f"/clients/{prov_id}/client-secret")
    L.write_env_value("SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET", secret["value"])
    evidence["provisioner"] = {"created": created, "realm_management_roles": [r["name"] for r in wanted],
                               "secret": "stored in lab.env"}
    if enable_pdp_profile_sync:
        # OPT-IN explícito del responsable: con la audiencia del PDP, el token de servicio del backend puede invocar la API
        # de administración del PDP (asignar/revocar perfiles de las cuentas que el propio backend registra). La API 0.1.7
        # del PDP no distingue administradores entre los Bearer con esa audiencia, por eso no se concede por defecto.
        names = [m["name"] for m in kc.client(PROVISIONER).get("protocolMappers", [])]
        if "audience-pdp" not in names:
            kc.call("POST", f"/clients/{prov_id}/protocol-mappers/models",
                    _mapper("audience-pdp", "oidc-audience-mapper", {"included.client.audience": "security-baseline-pdp"}))
        evidence["provisioner"]["pdp_audience"] = "security-baseline-pdp (sincronización de perfiles del backend)"

    # 4. Perfil de usuario del realm: Keycloak 26 descarta en silencio los atributos no declarados. Sin `idUsuario` las cuentas
    #    que crea el backend (registro, matrícula por QR) quedan sin vínculo institucional y la verificación de correo no las
    #    encuentra (500 "No existe la cuenta de identidad"). Solo el administrador del realm puede editar el perfil.
    _, _, profile = kc.call("GET", "/users/profile")
    if not any(a.get("name") == USER_ID_ATTRIBUTE for a in profile.get("attributes", [])):
        profile["attributes"].append({
            "name": USER_ID_ATTRIBUTE, "displayName": "Id de usuario institucional", "validations": {},
            "annotations": {}, "permissions": {"view": ["admin"], "edit": ["admin"]}, "multivalued": False})
        kc.call("PUT", "/users/profile", profile)
    evidence["user_profile"] = {"attribute": USER_ID_ATTRIBUTE, "declared": True}
    return evidence


def _mapper(name, kind, config):
    base = {"id.token.claim": "false", "access.token.claim": "true", "introspection.token.claim": "true",
            "userinfo.token.claim": "false"}
    base.update(config)
    return {"name": name, "protocol": "openid-connect", "protocolMapper": kind, "consentRequired": False, "config": base}


def _ensure_default_scopes(kc, client_uuid, names):
    _, _, scopes = kc.call("GET", "/client-scopes")
    by_name = {s["name"]: s["id"] for s in scopes}
    _, _, current = kc.call("GET", f"/clients/{client_uuid}/default-client-scopes")
    have = {s["name"] for s in current}
    for name in names:
        if name not in have and name in by_name:
            kc.call("PUT", f"/clients/{client_uuid}/default-client-scopes/{by_name[name]}")


# ── PDP ─────────────────────────────────────────────────────────────────────────────────────────────────────────
class Pdp:
    def __init__(self, env):
        self.env = env
        self.opener, self.jar, status, _, _ = L.bff_session(env, "lab-pdp-admin", "LAB_USER_LAB_PDP_ADMIN_PASSWORD")
        if status != 200:
            raise SystemExit(f"Sesión BFF del administrador PDP: HTTP {status}")
        self.headers = L.xsrf_headers(self.jar)

    def call(self, method, path, body=None):
        status, _, raw = L.http(self.opener, method, L.PDP + path, body, headers=self.headers)
        try:
            return status, (json.loads(raw) if raw else {})
        except ValueError:
            return status, {}

    def paged(self, path):
        """Todas las filas de un listado paginado del PDP (tamaño de página máximo 100)."""
        page, rows = 0, []
        while True:
            status, body = self.call("GET", f"{path}{'&' if '?' in path else '?'}page={page}&size=100")
            if status != 200:
                raise SystemExit(f"GET {path}: HTTP {status} {body.get('code')}")
            data = body.get("data") or {}
            if isinstance(data, list):  # algunos listados devuelven todas las filas sin paginar
                return data
            content = data.get("content", [])
            rows += content
            if not content or len(rows) >= data.get("total", 0):
                return rows
            page += 1


def ensure_application(pdp, env, evidence):
    apps = pdp.paged("/api/v1/applications")
    found = [a for a in apps if a["name"] == APP_NAME]
    if found:
        app_id = found[0]["id"]
        evidence["application"] = {"id": app_id, "created": False}
        if not env.get("SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS"):
            status, body = pdp.call("POST", f"/api/v1/applications/{app_id}/credential-rotations")
            if status not in (200, 201):
                raise SystemExit(f"rotación de credencial: HTTP {status} {body.get('code')}")
            L.write_env_value("SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS", body["data"]["credential"])
    else:
        status, body = pdp.call("POST", "/api/v1/applications", {
            "name": APP_NAME, "description": "AsistenciasUCO backend (Security Baseline 0.1.7)",
            "baseUrl": "http://127.0.0.1:8080"})
        if status != 201:
            raise SystemExit(f"registrar aplicación: HTTP {status} {body.get('code')}")
        app_id = body["data"]["id"]
        L.write_env_value("SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS", body["data"]["credential"])
        evidence["application"] = {"id": app_id, "created": True}
    L.write_env_value("ASISTENCIAS_PDP_APPLICATION_ID", app_id)
    return app_id


def read_pdp_state(pdp, app_id):
    """Estado actual del PDP para la aplicación: recursos, roles institucionales, concesiones y perfiles."""
    _, res = pdp.call("GET", f"/api/v1/applications/{app_id}/resources")
    resources = {(r["method"], r["path"]): r["id"] for r in (res.get("data") or [])}
    roles = {r["name"]: r["id"] for r in pdp.paged(f"/api/v1/applications/{app_id}/security/roles")}
    grants = {}
    for name in ROLES:
        if name in roles:
            rows = pdp.paged(f"/api/v1/applications/{app_id}/security/roles/{roles[name]}/resources")
            grants[name] = {(x["method"], x["path"]) for x in rows}
    profiles = {p["name"]: p["id"] for p in pdp.paged(f"/api/v1/applications/{app_id}/security/profiles")}
    profile_roles = {}
    for name, profile_id in profiles.items():
        rows = pdp.paged(f"/api/v1/applications/{app_id}/security/profiles/{profile_id}/roles")
        profile_roles[name] = {r["name"] for r in rows}
    return {"resources": resources, "roles": roles, "grants": grants, "profiles": profiles,
            "profile_roles": profile_roles}


def diff_state(catalog, state):
    """Diferencias catálogo ↔ PDP. `extra_grants` son concesiones que el catálogo no autoriza (deriva)."""
    want, pairs = desired_grants(catalog), catalog_pairs(catalog)
    registered = set(state["resources"])
    return {
        "missing_resources": sorted(pairs - registered),
        "orphan_resources": sorted(registered - pairs),
        "missing_grants": {r: sorted(want[r] - state["grants"].get(r, set())) for r in ROLES},
        "extra_grants": {r: sorted(state["grants"].get(r, set()) - want[r]) for r in ROLES},
        "missing_profiles": [p["name"] for p in catalog.get("profiles", []) if p["name"] not in state["profiles"]],
        "profile_role_gaps": {p["name"]: sorted(set(p["roles"]) ^ state["profile_roles"].get(p["name"], set()))
                              for p in catalog.get("profiles", [])
                              if p["name"] in state["profiles"]
                              and set(p["roles"]) != state["profile_roles"].get(p["name"], set())},
    }


def summarize_diff(diff):
    return {
        "missing_resources": len(diff["missing_resources"]),
        "orphan_resources": len(diff["orphan_resources"]),
        "missing_grants": {r: len(v) for r, v in diff["missing_grants"].items() if v},
        "extra_grants": {r: len(v) for r, v in diff["extra_grants"].items() if v},
        "missing_profiles": diff["missing_profiles"],
        "profile_role_gaps": diff["profile_role_gaps"],
    }


def is_clean(diff):
    """Orphan resources sin concesiones no abren acceso (fail-closed): se informan pero no fallan la verificación."""
    return not (diff["missing_resources"] or diff["missing_profiles"] or diff["profile_role_gaps"]
                or any(diff["missing_grants"].values()) or any(diff["extra_grants"].values()))


def require_complete_inventory(catalog):
    missing, stale = inventory_gaps(catalog)
    if missing or stale:
        raise SystemExit("El catálogo no coincide con los controladores (ejecute `catalog`): "
                         f"{len(missing)} endpoints sin regla, {len(stale)} obsoletos. Nada se registró en el PDP.")


def provision_pdp(env, prune=False):
    """Reconciliación incremental: registra solo lo que falta; la deriva (concesiones sobrantes) se informa y solo se
    revoca con `--prune`. Nunca borra recursos ni roles."""
    catalog = load_catalog()
    require_complete_inventory(catalog)
    pdp = Pdp(env)
    evidence = {}
    app_id = ensure_application(pdp, env, evidence)

    state = read_pdp_state(pdp, app_id)
    before = diff_state(catalog, state)
    evidence["before"] = summarize_diff(before)

    for name in ROLES:  # roles institucionales de la aplicación
        if name not in state["roles"]:
            status, body = pdp.call("POST", "/api/v1/roles", {"name": name, "scope": "APPLICATION", "applicationId": app_id})
            if status != 201:
                raise SystemExit(f"rol {name}: HTTP {status} {body.get('code')}")
            state["roles"][name] = body["data"]["id"]
    for method, path in before["missing_resources"]:
        status, body = pdp.call("POST", f"/api/v1/applications/{app_id}/resources", {"path": path, "method": method})
        if status != 201:
            raise SystemExit(f"recurso {method} {path}: HTTP {status} {body.get('code')}")
        state["resources"][(method, path)] = body["data"]["id"]
    applied = {"resources": len(before["missing_resources"]), "grants_added": 0, "grants_revoked": 0}
    for role in ROLES:
        for method, path in before["missing_grants"][role]:
            status, body = pdp.call("POST", f"/api/v1/roles/{state['roles'][role]}/resources",
                                    {"roleId": state["roles"][role], "resourceId": state["resources"][(method, path)]})
            if status != 200:
                raise SystemExit(f"concesión {role} {method} {path}: HTTP {status} {body.get('code')}")
            applied["grants_added"] += 1
        if prune:
            for method, path in before["extra_grants"][role]:
                status, body = pdp.call("DELETE", f"/api/v1/roles/{state['roles'][role]}/resources/"
                                                  f"{state['resources'][(method, path)]}")
                if status != 200:
                    raise SystemExit(f"revocar {role} {method} {path}: HTTP {status} {body.get('code')}")
                applied["grants_revoked"] += 1
                print(f"REVOCADA concesión sobrante: {role} {method} {path}", file=sys.stderr)

    for profile in catalog.get("profiles", []):  # perfiles = agrupaciones de roles reales, nunca vacíos
        if profile["name"] not in state["profiles"]:
            status, body = pdp.call("POST", "/api/v1/profiles",
                                    {"name": profile["name"], "scope": "APPLICATION", "applicationId": app_id})
            if status != 201:
                raise SystemExit(f"perfil {profile['name']}: HTTP {status} {body.get('code')}")
            state["profiles"][profile["name"]] = body["data"]["id"]
            state["profile_roles"][profile["name"]] = set()
        for role in profile["roles"]:
            if role not in state["profile_roles"][profile["name"]]:
                status, body = pdp.call("POST", f"/api/v1/profiles/{state['profiles'][profile['name']]}/roles",
                                        {"roleId": state["roles"][role]})
                if status != 200:
                    raise SystemExit(f"rol {role} en perfil {profile['name']}: HTTP {status} {body.get('code')}")
    evidence["applied"] = applied
    after = diff_state(catalog, read_pdp_state(pdp, app_id))
    evidence["after"] = summarize_diff(after)
    evidence["catalog"] = {"endpoints": len(catalog["endpoints"]), "exempt": len(catalog.get("exempt", [])),
                           "profiles": len(catalog.get("profiles", []))}
    for name, role_id in state["roles"].items():
        if name in ROLES:
            L.write_env_value("ASISTENCIAS_PDP_ROLE_" + name + "_ID", role_id)
    for name, profile_id in state["profiles"].items():
        L.write_env_value("ASISTENCIAS_PDP_PROFILE_" + re.sub(r"[^A-Z0-9]", "_", name.upper()) + "_ID", profile_id)
    if not is_clean(after):
        evidence["status"] = "DRIFT: use --prune para revocar las concesiones sobrantes listadas por `verify`"
    return evidence


def verify_pdp(env):
    """Solo lectura: compara el PDP con el catálogo; código de salida 1 si hay recursos/concesiones/perfiles distintos."""
    catalog = load_catalog()
    require_complete_inventory(catalog)
    pdp = Pdp(env)
    app_id = env["ASISTENCIAS_PDP_APPLICATION_ID"]
    diff = diff_state(catalog, read_pdp_state(pdp, app_id))
    report = summarize_diff(diff)
    for role, rows in diff["extra_grants"].items():
        for method, path in rows:
            print(f"SOBRA   {role} {method} {path}")
    for role, rows in diff["missing_grants"].items():
        for method, path in rows:
            print(f"FALTA   {role} {method} {path}")
    for method, path in diff["missing_resources"]:
        print(f"FALTA   recurso {method} {path}")
    for method, path in diff["orphan_resources"]:
        print(f"HUERFANO recurso {method} {path} (sin endpoint; no abre acceso si no tiene concesiones)")
    report["clean"] = is_clean(diff)
    return report


# ── Decisiones reales del PEP (aplicación → PEP) ────────────────────────────────────────────────────────────────
SAMPLE_ID = "11111111-2222-4333-8444-555555555555"
NEGATIVE_PROBES = [  # rutas NO registradas: el PEP no debe aceptar coincidencias excesivamente amplias
    ("GET", "/api/v1/grupos/" + SAMPLE_ID + "/no-registrada"),
    ("GET", "/api/v1/docente/horarios/extra"),
    ("GET", "/api/v1/"),
    ("DELETE", "/api/v1/auth/me"),
    ("GET", "/api/v2/sesiones/" + SAMPLE_ID + "/inexistente"),
]


def pep_decision(env, token, method, path):
    headers = {"X-Application-Credential": env["SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS"],
               "X-Request-Id": "catalog-verify"}
    if token:
        headers["Authorization"] = "Bearer " + token
    status, response_headers, _ = L.http(
        L.plain_opener(), "POST", L.PEP + "/internal/v1/embedded-access-decisions",
        {"applicationName": APP_NAME, "environment": "dev", "path": path, "method": method}, headers=headers)
    return status, response_headers


def verify_decisions(env):
    """Solo lectura: para cada endpoint del catálogo y cada rol pide la decisión REAL al PEP (que consulta al PDP):
    204 + X-Decision-Id si el catálogo lo permite, 403 si no. Incluye sonda sin Bearer y rutas no registradas."""
    import spa_token  # noqa: E402
    catalog = load_catalog()
    tokens = {role: spa_token.get_token(env, user)["access_token"] for role, (user, _, _) in TEST_USERS.items()}
    failures, total = [], 0
    for entry in catalog["endpoints"]:
        concrete = re.sub(r"\{[^}]+\}", SAMPLE_ID, entry["path"])
        for role in ROLES:
            total += 1
            status, headers = pep_decision(env, tokens[role], entry["method"], concrete)
            expected_allow = role in entry["roles"]
            allowed = status == 204 and bool(headers.get("X-Decision-Id"))
            if allowed != expected_allow or (not expected_allow and status != 403):
                failures.append(f"{role} {entry['method']} {entry['path']}: esperado "
                                f"{'ALLOW(204)' if expected_allow else 'DENY(403)'}, PEP respondió {status}")
        total += 1
        status, _ = pep_decision(env, None, entry["method"], concrete)
        if status == 204:
            failures.append(f"sin Bearer {entry['method']} {entry['path']}: el PEP permitió sin credencial de usuario")
    for method, path in NEGATIVE_PROBES:
        for role in ROLES:
            total += 1
            status, _ = pep_decision(env, tokens[role], method, path)
            if status != 403:
                failures.append(f"{role} {method} {path} (ruta NO registrada): esperado 403, PEP respondió {status}")
    for line in failures:
        print("FALLO  " + line)
    return {"decisions": total, "failures": len(failures), "clean": not failures}


# ── Usuarios de prueba ──────────────────────────────────────────────────────────────────────────────────────────
def profile_for(catalog, role):
    """Perfil del catálogo que contiene exactamente ese rol (el perfil individual del rol)."""
    for profile in catalog.get("profiles", []):
        if profile["roles"] == [role]:
            return profile["name"]
    return None


def provision_users(env, migrate_direct=False):
    kc = Keycloak(env)
    api = kc.client(API_CLIENT)
    _, _, api_roles = kc.call("GET", f"/clients/{api['id']}/roles")
    role_rep = {r["name"]: r for r in api_roles}
    evidence = {}
    for role, (username, email, user_uuid) in TEST_USERS.items():
        key = "LAB_USER_" + re.sub(r"[^A-Z0-9]", "_", username.upper()) + "_PASSWORD"
        _, _, users = kc.call("GET", f"/users?username={username}&exact=true")
        if not users:
            kc.call("POST", "/users", {
                "username": username, "enabled": True, "emailVerified": True, "email": email,
                "firstName": role.title(), "lastName": "UCO", "requiredActions": [],
                "attributes": {"idUsuario": [user_uuid.lower()]}})
            _, _, users = kc.call("GET", f"/users?username={username}&exact=true")
        uid = users[0]["id"]
        password = env.get(key) or L.random_secret()
        if not env.get(key):
            L.write_env_value(key, password)
            env[key] = password
        kc.call("PUT", f"/users/{uid}/reset-password", {"type": "password", "value": password, "temporary": False})
        kc.call("POST", f"/users/{uid}/role-mappings/clients/{api['id']}", [role_rep[role]])
        evidence[role] = {"username": username, "client_role": role}

    catalog = load_catalog()
    pdp = Pdp(env)
    app_id = env["ASISTENCIAS_PDP_APPLICATION_ID"]
    for role, (username, email, _) in TEST_USERS.items():
        key = "LAB_USER_" + re.sub(r"[^A-Z0-9]", "_", username.upper()) + "_PASSWORD"
        _, _, status, session, _ = L.bff_session(env, username, key)  # aprovisiona la identidad en el PDP
        evidence[role]["pdp_session_http"] = status
    users = pdp.paged("/api/v1/users")
    pdp_ids = {u["email"].lower(): u["id"] for u in users}
    for role, (username, email, _) in TEST_USERS.items():
        role_id = env["ASISTENCIAS_PDP_ROLE_" + role + "_ID"]
        user_id = pdp_ids.get(email.lower())
        if not user_id:
            raise SystemExit(f"{username}: el PDP no conoce al usuario tras la sesión BFF")
        profile = profile_for(catalog, role)
        profile_id = env.get("ASISTENCIAS_PDP_PROFILE_" + re.sub(r"[^A-Z0-9]", "_", (profile or "").upper()) + "_ID")
        direct = [a for a in pdp.paged(f"/api/v1/roles/{role_id}/assignments") if a.get("userId") == user_id]
        if profile and profile_id:
            held = [a for a in pdp.paged(f"/api/v1/profiles/{profile_id}/assignments") if a.get("userId") == user_id]
            if held:
                evidence[role]["profile_assignment"] = "exists"
            elif direct and not migrate_direct:
                # El PDP rechaza (409) un perfil que repite un rol ya asignado de forma directa: no se toca la
                # asignación existente sin autorización explícita.
                evidence[role]["profile_assignment"] = "pendiente: el rol está asignado de forma directa (--migrate-direct)"
            else:
                for assignment in direct:  # migración: se retira la directa y se asigna el perfil con el mismo rol
                    status, _ = pdp.call("DELETE", f"/api/v1/roles/{role_id}/assignments/{assignment['id']}")
                    evidence[role]["direct_assignment_revoked"] = f"HTTP {status}"
                status, body = pdp.call("POST", f"/api/v1/profiles/{profile_id}/assignments",
                                        {"profileId": profile_id, "userId": user_id, "applicationId": app_id})
                evidence[role]["profile_assignment"] = f"HTTP {status}"
                if status != 201:
                    if direct:  # reversión: el usuario nunca debe quedar sin rol
                        pdp.call("POST", f"/api/v1/roles/{role_id}/assignments",
                                 {"roleId": role_id, "userId": user_id, "applicationId": app_id})
                    raise SystemExit(f"perfil {profile} → {username}: HTTP {status} {body.get('code')}; "
                                     "asignación directa restaurada")
                direct = []
        if direct:
            evidence[role]["assignment"] = "direct exists"
        elif not (profile and profile_id):
            status, _ = pdp.call("POST", f"/api/v1/roles/{role_id}/assignments",
                                 {"roleId": role_id, "userId": user_id, "applicationId": app_id})
            evidence[role]["assignment"] = f"HTTP {status}"
    return evidence


def main():
    args = sys.argv[1:]
    commands = ("keycloak", "pdp", "users", "catalog", "verify", "verify-decisions")
    if not args or args[0] not in commands:
        raise SystemExit(__doc__)
    if args[0] == "catalog":  # offline: no necesita lab.env
        raise SystemExit(command_catalog())
    env = L.read_env()
    if args[0] == "pdp":
        result = provision_pdp(env, prune="--prune" in args)
    elif args[0] == "users":
        result = provision_users(env, migrate_direct="--migrate-direct" in args)
    elif args[0] == "verify":
        result = verify_pdp(env)
    elif args[0] == "verify-decisions":
        result = verify_decisions(env)
    else:
        result = provision_keycloak(env, enable_pdp_profile_sync="--enable-pdp-profile-sync" in args)
    print(json.dumps(result, indent=1, default=list))
    if args[0] in ("verify", "verify-decisions") and not result.get("clean"):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
