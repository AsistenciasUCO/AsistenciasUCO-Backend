"""Banco de pruebas de rendimiento acotado (mismo metodo antes/despues). Sin tokens/secretos en la salida.

Uso: python -X utf8 -I sb_bench.py <etiqueta> <salida.json>
Escenarios (todo GET/decision, de solo lectura):
  S1  PEP decision individual (secuencial, 5 calentamientos + 30 muestras, 6 rutas del administrador)
  S2  PDP resumen de seguridad (GET security/summary con Bearer del administrador; 3 calentamientos + 20)
  S3  Backend /auth/me autenticado (secuencial, 3 + 30)
  S4  Backend cadena completa Spring->PEP->PDP->SQL (GET admin/decanos; secuencial 3 + 15)
  S5  PEP rafagas simultaneas de 5, 20 y 50 (3 repeticiones cada una, 3 s de pausa; timeout 15 s por peticion)
  S6  Backend cadena completa en rafagas de 5 y 20 (2 repeticiones; GET admin/decanos y auth/me)
Limites: maximo ~560 peticiones en total; sin cargas sostenidas ni sobre SQL mas alla de lecturas pequenas.
"""
import concurrent.futures
import json
import statistics
import subprocess
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, r"C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra\security-baseline\scripts")
import lab_pdp as L  # noqa: E402
import provision_asistencias as P  # noqa: E402
import spa_token  # noqa: E402

label, out_path = sys.argv[1], sys.argv[2]
env = L.read_env()
APP = env["ASISTENCIAS_PDP_APPLICATION_ID"]
BACKEND = "http://127.0.0.1:8080"
PATHS = ["/api/v1/admin/decanos", "/api/v1/admin/facultades", "/api/v1/admin/indicadores",
         "/api/v1/admin/tendencia-ausentismo", "/api/v1/admin/comparativa-facultades", "/api/v1/admin/instituciones"]
TIMEOUT = 15
results = {"label": label, "started": time.strftime("%Y-%m-%dT%H:%M:%S%z"), "scenarios": {}}
token_state = {"t": None, "at": 0}


def token():
    if token_state["t"] is None or time.time() - token_state["at"] > 150:
        token_state["t"] = spa_token.get_token(env, "uco-administrador")["access_token"]
        token_state["at"] = time.time()
    return token_state["t"]


def timed(fn):
    start = time.perf_counter()
    try:
        status = fn()
    except urllib.error.HTTPError as error:
        status = error.code
    except Exception as error:  # timeout / conexion
        status = "TIMEOUT" if "timed out" in str(error).lower() else "ERR"
    return status, (time.perf_counter() - start) * 1000


def http_get(url, headers):
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
        response.read()
        return response.status


def pep(path):
    def call():
        req = urllib.request.Request(
            L.PEP + "/internal/v1/embedded-access-decisions", method="POST",
            data=json.dumps({"applicationName": P.APP_NAME, "environment": "dev", "path": path, "method": "GET"}).encode(),
            headers={"Content-Type": "application/json", "Authorization": "Bearer " + token(),
                     "X-Application-Credential": env["SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS"],
                     "X-Request-Id": "bench"})
        with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
            response.read()
            return response.status
    return call


def pdp_summary():
    return http_get(f"{L.PDP}/api/v1/applications/{APP}/security/summary", {"Authorization": "Bearer " + token()})


def backend(path):
    return lambda: http_get(BACKEND + path, {"Authorization": "Bearer " + token()})


def summarize(samples, wall_ms=None):
    ok = [ms for st, ms in samples if st in (200, 204)]
    codes = {}
    for st, _ in samples:
        codes[str(st)] = codes.get(str(st), 0) + 1
    all_ms = sorted(ms for _, ms in samples)

    def pct(values, q):
        if not values:
            return None
        values = sorted(values)
        return round(values[min(len(values) - 1, int(round(q * (len(values) - 1))))], 1)

    return {"n": len(samples), "ok": len(ok), "codes": codes, "p50_ms": pct(ok, 0.50), "p95_ms": pct(ok, 0.95),
            "max_ms": round(max(all_ms), 1) if all_ms else None, "mean_ms": round(statistics.mean(ok), 1) if ok else None,
            "wall_ms": round(wall_ms, 1) if wall_ms else None}


def sequential(name, fns, warm, note):
    for fn in fns[:warm]:
        timed(fn)
    samples = [timed(fn) for fn in fns[warm:]]
    results["scenarios"][name] = dict(summarize(samples), note=note, warmup=warm)
    print(name, results["scenarios"][name], flush=True)


def burst(name, fn_factory, sizes, reps, note):
    for size in sizes:
        samples, walls = [], []
        for rep in range(reps):
            token()
            start = time.perf_counter()
            with concurrent.futures.ThreadPoolExecutor(size) as pool:
                futures = [pool.submit(timed, fn_factory(i)) for i in range(size)]
                samples.extend(f.result() for f in futures)
            walls.append((time.perf_counter() - start) * 1000)
            time.sleep(3)
        key = f"{name}_x{size}"
        results["scenarios"][key] = dict(summarize(samples, statistics.mean(walls)), note=note, reps=reps, concurrency=size)
        print(key, results["scenarios"][key], flush=True)
        snap = subprocess.run(["docker", "stats", "--no-stream", "--format", "{{.Name}} cpu={{.CPUPerc}} mem={{.MemUsage}}"],
                              capture_output=True, text=True).stdout
        results["scenarios"][key]["docker_stats_after"] = [l for l in snap.splitlines() if "security-lab" in l]


token()
sequential("S1_pep_decision", [pep(PATHS[i % 6]) for i in range(35)], 5, "POST embedded-access-decisions, 6 rutas rotadas")
sequential("S2_pdp_security_summary", [pdp_summary for _ in range(23)], 3, "GET security/summary con Bearer del administrador")
sequential("S3_backend_auth_me", [backend("/api/v1/auth/me") for _ in range(33)], 3, "GET /api/v1/auth/me autenticado")
sequential("S4_backend_admin_decanos", [backend("/api/v1/admin/decanos") for _ in range(18)], 3, "Spring->PEP->PDP->SQL")
burst("S5_pep_burst", lambda i: pep(PATHS[i % 6]), [5, 20, 50], 3, "decisiones PEP simultaneas")
burst("S6_backend_admin_decanos_burst", lambda i: backend("/api/v1/admin/decanos"), [5, 20], 2, "cadena completa simultanea")
burst("S6b_backend_auth_me_burst", lambda i: backend("/api/v1/auth/me"), [5, 20], 2, "auth/me simultaneo")
results["finished"] = time.strftime("%Y-%m-%dT%H:%M:%S%z")
with open(out_path, "w", encoding="utf-8") as handle:
    json.dump(results, handle, indent=2, ensure_ascii=False)
print("OK ->", out_path)
