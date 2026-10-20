"""LB-004 — E2E HTTP real de soportes: backend + Keycloak + MinIO + ClamAV.

Ejecuta la matriz de subida/descarga contra un backend real con Bearer emitidos por un Keycloak
local. No usa mocks. Nunca imprime tokens, passwords, access keys ni object keys: solo nombres de
checks, codigos HTTP, fileId y SHA-256 de fixtures sinteticos.

Uso (python -I recomendado):
  files_storage_e2e.py provision-users --keycloak-env infra/keycloak/.env --secrets <files.env>
  files_storage_e2e.py main          --keycloak-env ... --secrets ... --state <state.json>
  files_storage_e2e.py after-restart --keycloak-env ... --secrets ... --state <state.json>
  files_storage_e2e.py clamav-down   --keycloak-env ... --secrets ...
  files_storage_e2e.py minio-down    --keycloak-env ... --secrets ... --state <state.json>

Exit code: 0 todos PASS, 1 algun FAIL, 2 ambiente incompleto (BLOCKED).
"""

import argparse
import hashlib
import json
import os
import random
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

EICAR = b"X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"
MAX_FILE_SIZE = 5 * 1024 * 1024
UPLOAD_FIELDS = {"fileId", "nombre", "url", "tamanio"}
# Identidades dedicadas a este E2E (no se tocan las cuentas E2E de otros flujos):
# (prefijo en el archivo de secretos, username, rol cliente de la API).
DEDICATED_USERS = [
    ("FILES_E2E_ESTUDIANTE1_", "files.e2e.estudiante1", "ESTUDIANTE"),
    ("FILES_E2E_ESTUDIANTE2_", "files.e2e.estudiante2", "ESTUDIANTE"),
    ("FILES_E2E_ADMIN_", "files.e2e.admin", "ADMINISTRADOR"),
    # LB-004D: un docente que NO es titular del grupo del estudiante (R1 debe devolverle 404).
    ("FILES_E2E_DOCENTE2_", "files.e2e.docente2", "DOCENTE"),
]
# Fragmentos que nunca deben aparecer en una respuesta HTTP (internals del provider).
FORBIDDEN_FRAGMENTS = ["asistencias-soportes", "soportes/", "minio", "clamav", ":19000", ":13310", ":9000", ":3310"]

RESULTS = []


class Blocked(Exception):
    pass


# ---------------------------------------------------------------- env / secrets

def load_env(path):
    values = {}
    if not path or not os.path.isfile(path):
        raise Blocked("archivo de entorno no encontrado: " + str(path))
    with open(path, encoding="utf-8") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def require(env, key):
    value = env.get(key, "")
    if not value:
        raise Blocked("falta la variable " + key)
    return value


# ---------------------------------------------------------------- http

def http(method, url, headers=None, body=None, timeout=60):
    request = urllib.request.Request(url, data=body, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, dict(response.headers.items()), response.read()
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers.items()), error.read()


def header(headers, name):
    for key, value in headers.items():
        if key.lower() == name.lower():
            return value
    return None


def form(url, fields):
    body = urllib.parse.urlencode(fields).encode()
    return http("POST", url, {"Content-Type": "application/x-www-form-urlencoded"}, body)


def user_token(kc, username, password):
    url = kc["server"] + "/realms/" + kc["realm"] + "/protocol/openid-connect/token"
    status, _, body = form(url, {"grant_type": "password", "client_id": kc["frontend_client"],
                                 "username": username, "password": password})
    if status != 200:
        raise Blocked("Keycloak no emitio token para un usuario E2E (HTTP %d)" % status)
    return json.loads(body)["access_token"]


def admin_token(kc):
    url = kc["server"] + "/realms/master/protocol/openid-connect/token"
    status, _, body = form(url, {"grant_type": "password", "client_id": "admin-cli",
                                 "username": kc["admin_user"], "password": kc["admin_password"]})
    if status != 200:
        raise Blocked("Keycloak no emitio token admin de bootstrap (HTTP %d)" % status)
    return json.loads(body)["access_token"]


def admin_api(kc, token, method, path, payload=None):
    headers = {"Authorization": "Bearer " + token}
    body = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(payload).encode()
    status, response_headers, raw = http(method, kc["server"] + "/admin/realms/" + kc["realm"] + path, headers, body)
    parsed = json.loads(raw) if raw and raw[:1] in (b"{", b"[") else None
    return status, response_headers, parsed


def multipart(filename, content_type, content):
    boundary = "----lb004" + uuid.uuid4().hex
    head = ("--%s\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"%s\"\r\n"
            "Content-Type: %s\r\n\r\n" % (boundary, filename, content_type)).encode()
    tail = ("\r\n--%s--\r\n" % boundary).encode()
    return "multipart/form-data; boundary=" + boundary, head + content + tail


def upload(base, token, filename, content_type, content):
    ctype, body = multipart(filename, content_type, content)
    headers = {"Content-Type": ctype}
    if token:
        headers["Authorization"] = "Bearer " + token
    return http("POST", base + "/api/v1/archivos/subir", headers, body)


def download(base, token, file_id):
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    return http("GET", base + "/api/v1/archivos/" + file_id, headers)


# ---------------------------------------------------------------- fixtures (sinteticos)

def pdf_with_embedded(payload):
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R /Names << /EmbeddedFiles << /Names [(soporte.txt) 4 0 R] >> >> >>",
        b"<< /Type /Pages /Kids [] /Count 0 >>",
        b"<< /Type /EmbeddedFile /Length " + str(len(payload)).encode() + b" >>\nstream\n" + payload + b"\nendstream",
        b"<< /Type /Filespec /F (soporte.txt) /EF << /F 3 0 R >> >>",
    ]
    out = b"%PDF-1.4\n"
    offsets = []
    for index, obj in enumerate(objects, 1):
        offsets.append(len(out))
        out += str(index).encode() + b" 0 obj\n" + obj + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 " + str(len(objects) + 1).encode() + b"\n0000000000 65535 f \n"
    for offset in offsets:
        out += ("%010d 00000 n \n" % offset).encode()
    out += (b"trailer\n<< /Size " + str(len(objects) + 1).encode() + b" /Root 1 0 R >>\nstartxref\n"
            + str(xref).encode() + b"\n%%EOF\n")
    return out


def random_with_magic(magic, size, seed):
    generator = random.Random(seed)
    data = bytearray(generator.getrandbits(8) for _ in range(size))
    data[:len(magic)] = magic
    return bytes(data)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


# ---------------------------------------------------------------- checks

def check(name, ok, detail=""):
    RESULTS.append({"check": name, "result": "PASS" if ok else "FAIL", "detail": detail})
    print("%s: %s%s" % (name, "PASS" if ok else "FAIL", (" (" + detail + ")") if detail else ""))
    return ok


def json_body(raw):
    try:
        return json.loads(raw)
    except ValueError:
        return {}


def no_internal_leak(raw):
    text = raw.decode("utf-8", "replace").lower()
    return not any(fragment in text for fragment in FORBIDDEN_FRAGMENTS)


def expect_error(name, response, status, code=None):
    got_status, headers, raw = response
    body = json_body(raw)
    ok = got_status == status and (code is None or body.get("code") == code) and no_internal_leak(raw)
    return check(name, ok, "HTTP %d code=%s" % (got_status, body.get("code")))


def expect_upload_ok(name, response, expected_size):
    status, _, raw = response
    body = json_body(raw)
    data = body.get("datos") or body.get("data") or {}
    file_id = data.get("fileId", "")
    ok = (status == 201 and set(data.keys()) == UPLOAD_FIELDS
          and data.get("url") == "/api/v1/archivos/" + str(file_id)
          and int(data.get("tamanio", -1)) == expected_size
          and no_internal_leak(raw))
    try:
        uuid.UUID(str(file_id))
    except ValueError:
        ok = False
    check(name, ok, "HTTP %d fileId=%s" % (status, file_id))
    return str(file_id) if ok else None


def expect_download_identical(name, response, original, content_type):
    status, headers, raw = response
    cache = (header(headers, "Cache-Control") or "").lower()
    ok = (status == 200 and raw == original
          and (header(headers, "Content-Type") or "").startswith(content_type)
          and (header(headers, "X-Content-Type-Options") or "").lower() == "nosniff"
          and (header(headers, "Content-Disposition") or "").startswith("inline; filename=")
          and "no-store" in cache)
    return check(name, ok, "HTTP %d sha256=%s" % (status, sha256(raw)[:16]))


# ---------------------------------------------------------------- context

def context(args):
    kc_env = load_env(args.keycloak_env)
    secrets_env = load_env(args.secrets)
    kc = {
        "server": kc_env.get("KEYCLOAK_SERVER_URL", "http://127.0.0.1:8081").rstrip("/"),
        "realm": kc_env.get("KC_REALM", "asistencias-uco"),
        "frontend_client": kc_env.get("KC_FRONTEND_CLIENT_ID", "asistencias-uco-frontend"),
        "api_client": kc_env.get("KC_API_CLIENT_ID", "asistencias-api"),
        "admin_user": kc_env.get("KC_BOOTSTRAP_ADMIN_USERNAME", ""),
        "admin_password": kc_env.get("KC_BOOTSTRAP_ADMIN_PASSWORD", ""),
    }
    users = {}
    for role in ("DOCENTE", "COORDINADOR"):
        users[role] = (require(kc_env, "E2E_%s_USERNAME" % role), require(kc_env, "E2E_%s_PASSWORD" % role))
    for alias, prefix in (("ESTUDIANTE", "FILES_E2E_ESTUDIANTE1_"), ("ESTUDIANTE2", "FILES_E2E_ESTUDIANTE2_"),
                          ("ADMIN", "FILES_E2E_ADMIN_"), ("DOCENTE2", "FILES_E2E_DOCENTE2_")):
        if not secrets_env.get(prefix + "USERNAME"):
            raise Blocked("falta la identidad dedicada %s; ejecute provision-users" % alias)
        users[alias] = (secrets_env[prefix + "USERNAME"], secrets_env[prefix + "PASSWORD"])
    return kc, users


def tokens_for(kc, users):
    return {role: user_token(kc, username, password) for role, (username, password) in users.items()}


# ---------------------------------------------------------------- phases

def provision_user(kc, token, secrets_env, prefix, default_username, role_name, client_uuid):
    username = secrets_env.get(prefix + "USERNAME") or default_username
    password = secrets_env.get(prefix + "PASSWORD") or (secrets.token_urlsafe(18) + "Aa1!")
    id_usuario = secrets_env.get(prefix + "ID_USUARIO") or str(uuid.uuid4())
    status, _, found = admin_api(kc, token, "GET", "/users?exact=true&username=" + urllib.parse.quote(username))
    if status != 200:
        raise Blocked("no fue posible consultar usuarios (HTTP %d)" % status)
    if found:
        user_id = found[0]["id"]
        print("%sUSER: OK (existente)" % prefix)
    else:
        status, headers, _ = admin_api(kc, token, "POST", "/users", {
            "username": username, "enabled": True, "emailVerified": True,
            "email": username + "@example-e2e.uco.edu.co", "firstName": "Files", "lastName": "E2E",
            "attributes": {"idUsuario": [id_usuario]}})
        if status != 201:
            raise Blocked("no fue posible crear %s (HTTP %d)" % (username, status))
        user_id = header(headers, "Location").rstrip("/").split("/")[-1]
        print("%sUSER: CREATED" % prefix)
    admin_api(kc, token, "PUT", "/users/" + user_id + "/reset-password",
              {"type": "password", "value": password, "temporary": False})
    status, _, user = admin_api(kc, token, "GET", "/users/" + user_id)
    if (user.get("attributes") or {}).get("idUsuario") != [id_usuario]:
        user.setdefault("attributes", {})["idUsuario"] = [id_usuario]
        admin_api(kc, token, "PUT", "/users/" + user_id, user)
    status, _, role = admin_api(kc, token, "GET", "/clients/" + client_uuid + "/roles/" + role_name)
    if status != 200:
        raise Blocked("rol %s no existe en la API" % role_name)
    admin_api(kc, token, "POST", "/users/" + user_id + "/role-mappings/clients/" + client_uuid, [role])
    print("%sROLE_%s: OK" % (prefix, role_name))
    if secrets_env.get(prefix + "USERNAME"):
        return ""
    return "%sUSERNAME=%s\n%sPASSWORD=%s\n%sID_USUARIO=%s\n" % (
        prefix, username, prefix, password, prefix, id_usuario)


def phase_provision(args):
    kc_env = load_env(args.keycloak_env)
    require(kc_env, "KC_BOOTSTRAP_ADMIN_USERNAME")
    require(kc_env, "KC_BOOTSTRAP_ADMIN_PASSWORD")
    kc = {
        "server": kc_env.get("KEYCLOAK_SERVER_URL", "http://127.0.0.1:8081").rstrip("/"),
        "realm": kc_env.get("KC_REALM", "asistencias-uco"),
        "api_client": kc_env.get("KC_API_CLIENT_ID", "asistencias-api"),
        "admin_user": kc_env["KC_BOOTSTRAP_ADMIN_USERNAME"],
        "admin_password": kc_env["KC_BOOTSTRAP_ADMIN_PASSWORD"],
    }
    secrets_env = load_env(args.secrets)
    token = admin_token(kc)
    status, _, clients = admin_api(kc, token, "GET", "/clients?clientId=" + urllib.parse.quote(kc["api_client"]))
    if status != 200 or not clients:
        raise Blocked("cliente de API no encontrado en Keycloak")
    pending = ""
    for prefix, username, role_name in DEDICATED_USERS:
        pending += provision_user(kc, token, secrets_env, prefix, username, role_name, clients[0]["id"])
    if pending:
        with open(args.secrets, "a", encoding="utf-8", newline="\n") as handle:
            handle.write(pending)
        print("DEDICATED_SECRETS: STORED (valores no impresos)")
    return 0


def phase_main(args):
    kc, users = context(args)
    if "ESTUDIANTE2" not in users:
        raise Blocked("falta el segundo estudiante; ejecute provision-users")
    tokens = tokens_for(kc, users)
    base = args.base_url.rstrip("/")
    owner = tokens["ESTUDIANTE"]
    state = {"files": []}

    valid_pdf = pdf_with_embedded(b"Soporte valido LB-004 " + uuid.uuid4().hex.encode())

    # --- autenticacion (Layer 1)
    expect_error("AUTH_UPLOAD_WITHOUT_BEARER_401", upload(base, None, "s.pdf", "application/pdf", valid_pdf), 401)
    expect_error("AUTH_DOWNLOAD_WITHOUT_BEARER_401", download(base, None, str(uuid.uuid4())), 401)
    expect_error("AUTH_INVALID_BEARER_401", download(base, "no-es-un-jwt", str(uuid.uuid4())), 401)

    # --- subida y descarga del propietario
    file_id = expect_upload_ok("STUDENT_UPLOAD_PDF_201", upload(base, owner, "soporte revision.pdf", "application/pdf", valid_pdf), len(valid_pdf))
    if file_id:
        state["files"].append({"fileId": file_id, "sha256": sha256(valid_pdf), "contentType": "application/pdf"})
        response = download(base, owner, file_id)
        expect_download_identical("OWNER_DOWNLOAD_200_SAME_SHA256", response, valid_pdf, "application/pdf")
        check("OWNER_DOWNLOAD_FILENAME_SANITIZED",
              header(response[1], "Content-Disposition") == 'inline; filename="soporte_revision.pdf"')

        # --- ownership: 404 indistinguible de inexistente
        missing = download(base, owner, str(uuid.uuid4()))
        missing_body = json_body(missing[2])
        for role in ("ESTUDIANTE2", "DOCENTE", "COORDINADOR", "ADMIN"):
            foreign = download(base, tokens[role], file_id)
            body = json_body(foreign[2])
            check("FOREIGN_%s_404_INDISTINGUISHABLE" % role,
                  foreign[0] == 404 and missing[0] == 404 and body.get("code") == missing_body.get("code")
                  and body.get("message") == missing_body.get("message") and no_internal_leak(foreign[2]),
                  "HTTP %d code=%s" % (foreign[0], body.get("code")))
        expect_error("NONEXISTENT_FILE_404", missing, 404)
        expect_error("MALFORMED_FILE_ID_404", download(base, owner, "no-es-uuid"), 404)

    # --- seguridad de contenido
    expect_error("MALWARE_EICAR_IN_PDF_400", upload(base, owner, "infectado.pdf", "application/pdf", pdf_with_embedded(EICAR)), 400, "VALIDATION_ERROR")
    expect_error("EXE_RENAMED_AS_PDF_400", upload(base, owner, "programa.pdf", "application/pdf", b"MZ\x90\x00" + os.urandom(512)), 400)
    expect_error("MIME_MISMATCH_400", upload(base, owner, "soporte.pdf", "image/png", valid_pdf), 400)
    expect_error("EMPTY_FILE_400", upload(base, owner, "vacio.pdf", "application/pdf", b""), 400)
    expect_error("DISALLOWED_EXTENSION_400", upload(base, owner, "nota.txt", "text/plain", b"hola"), 400)
    expect_error("PATH_TRAVERSAL_FILENAME_400", upload(base, owner, "../../etc/soporte.pdf", "application/pdf", valid_pdf), 400)
    oversized = random_with_magic(b"%PDF-1.4\n", MAX_FILE_SIZE + 1, 41)
    expect_error("OVERSIZE_5MIB_PLUS_1_400", upload(base, owner, "grande.pdf", "application/pdf", oversized), 400, "VALIDATION_ERROR")

    # --- limites, compresion y tipos
    cases = [
        ("EXACT_5MIB_PDF", "maximo.pdf", "application/pdf", random_with_magic(b"%PDF-1.4\n", MAX_FILE_SIZE, 42)),
        ("COMPRESSIBLE_PDF", "compresible.pdf", "application/pdf",
         pdf_with_embedded(b"Contenido de soporte altamente compresible LB-004. " * 6000)),
        ("PNG", "foto.png", "image/png", random_with_magic(b"\x89PNG\r\n\x1a\n", 64 * 1024, 43)),
        ("JPEG", "foto.jpg", "image/jpeg", random_with_magic(b"\xff\xd8\xff\xe0", 64 * 1024, 44)),
    ]
    for label, filename, content_type, content in cases:
        case_id = expect_upload_ok(label + "_UPLOAD_201", upload(base, owner, filename, content_type, content), len(content))
        if case_id:
            state["files"].append({"fileId": case_id, "sha256": sha256(content), "contentType": content_type})
            expect_download_identical(label + "_DOWNLOAD_SAME_SHA256", download(base, owner, case_id), content, content_type)

    with open(args.state, "w", encoding="utf-8") as handle:
        json.dump(state, handle, indent=2)
    return summarize()


def phase_after_restart(args):
    kc, users = context(args)
    tokens = tokens_for(kc, users)
    base = args.base_url.rstrip("/")
    with open(args.state, encoding="utf-8") as handle:
        state = json.load(handle)
    check("STATE_HAS_FILES", bool(state.get("files")), "%d archivos" % len(state.get("files", [])))
    for item in state.get("files", []):
        status, headers, raw = download(base, tokens["ESTUDIANTE"], item["fileId"])
        check("AFTER_RESTART_OWNER_200_SAME_SHA256 " + item["fileId"],
              status == 200 and sha256(raw) == item["sha256"], "HTTP %d" % status)
    if state.get("files"):
        foreign = download(base, tokens["ESTUDIANTE2"], state["files"][0]["fileId"])
        check("AFTER_RESTART_FOREIGN_STILL_404", foreign[0] == 404, "HTTP %d" % foreign[0])
    return summarize()


def phase_clamav_down(args):
    kc, users = context(args)
    token = user_token(kc, *users["ESTUDIANTE"])
    base = args.base_url.rstrip("/")
    response = upload(base, token, "soporte.pdf", "application/pdf", pdf_with_embedded(b"clamav caido"))
    expect_error("CLAMAV_DOWN_UPLOAD_FAILS_CLOSED_500", response, 500, "INTERNAL_APPLICATION_ERROR")
    return summarize()


def phase_minio_down(args):
    kc, users = context(args)
    token = user_token(kc, *users["ESTUDIANTE"])
    base = args.base_url.rstrip("/")
    with open(args.state, encoding="utf-8") as handle:
        state = json.load(handle)
    response = upload(base, token, "soporte.pdf", "application/pdf", pdf_with_embedded(b"minio caido"))
    expect_error("MINIO_DOWN_UPLOAD_500_NO_LEAK", response, 500)
    if state.get("files"):
        response = download(base, token, state["files"][0]["fileId"])
        expect_error("MINIO_DOWN_DOWNLOAD_500_NO_LEAK", response, 500)
    return summarize()


def summarize():
    failed = [r for r in RESULTS if r["result"] != "PASS"]
    print("SUMMARY: %d checks, %d PASS, %d FAIL" % (len(RESULTS), len(RESULTS) - len(failed), len(failed)))
    return 1 if failed else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("phase", choices=["provision-users", "main", "after-restart", "clamav-down", "minio-down"])
    parser.add_argument("--keycloak-env", required=True)
    parser.add_argument("--secrets", required=True)
    parser.add_argument("--state", default="files-e2e-state.json")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    args = parser.parse_args()
    phases = {
        "provision-users": phase_provision,
        "main": phase_main,
        "after-restart": phase_after_restart,
        "clamav-down": phase_clamav_down,
        "minio-down": phase_minio_down,
    }
    try:
        return phases[args.phase](args)
    except Blocked as blocked:
        print("BLOCKED: " + str(blocked))
        return 2


if __name__ == "__main__":
    sys.exit(main())
