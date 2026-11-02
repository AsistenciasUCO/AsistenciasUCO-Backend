"""Diagnostico acotado de la ruptura a 50 simultaneas: escala PDP-solo vs PEP->PDP y CPU durante la carga. Solo lectura."""
import concurrent.futures
import json
import subprocess
import sys
import threading
import time
import urllib.request

sys.path.insert(0, r"C:\Users\josev\AsistenciasUCO\AsistenciasUCO\infra\security-baseline\scripts")
import lab_pdp as L  # noqa: E402
import provision_asistencias as P  # noqa: E402
import spa_token  # noqa: E402

env = L.read_env()
APP = env["ASISTENCIAS_PDP_APPLICATION_ID"]
tok = spa_token.get_token(env, "uco-administrador")["access_token"]
PATHS = ["/api/v1/admin/decanos", "/api/v1/admin/facultades", "/api/v1/admin/indicadores"]


def summary(_):
    req = urllib.request.Request(f"{L.PDP}/api/v1/applications/{APP}/security/summary", headers={"Authorization": "Bearer " + tok})
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            r.read()
            st = r.status
    except urllib.error.HTTPError as e:
        st = e.code
    return st, (time.perf_counter() - start) * 1000


def decision(i):
    req = urllib.request.Request(L.PEP + "/internal/v1/embedded-access-decisions", method="POST",
                                 data=json.dumps({"applicationName": P.APP_NAME, "environment": "dev", "path": PATHS[i % 3], "method": "GET"}).encode(),
                                 headers={"Content-Type": "application/json", "Authorization": "Bearer " + tok,
                                          "X-Application-Credential": env["SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS"], "X-Request-Id": "diag"})
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            r.read()
            st = r.status
    except urllib.error.HTTPError as e:
        st = e.code
    return st, (time.perf_counter() - start) * 1000


def pct(v, q):
    v = sorted(v)
    return round(v[min(len(v) - 1, int(round(q * (len(v) - 1))))])


def run(name, fn, n):
    peak = {}
    stop = threading.Event()

    def sampler():
        proc = subprocess.Popen(["docker", "stats", "--format", "{{.Name}} {{.CPUPerc}}"], stdout=subprocess.PIPE, text=True)
        while not stop.is_set():
            line = proc.stdout.readline()
            if not line:
                break
            parts = line.split()
            if len(parts) == 2 and "security-lab" in parts[0]:
                svc = parts[0].split("security-lab-")[1].rsplit("-1", 1)[0]
                try:
                    peak[svc] = max(peak.get(svc, 0.0), float(parts[1].strip("%")))
                except ValueError:
                    pass
        proc.kill()

    t = threading.Thread(target=sampler, daemon=True)
    t.start()
    time.sleep(2.5)
    start = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(n) as pool:
        res = list(pool.map(fn, range(n)))
    wall = (time.perf_counter() - start) * 1000
    time.sleep(1.5)
    stop.set()
    codes = {}
    for st, _ in res:
        codes[st] = codes.get(st, 0) + 1
    ms = [m for st, m in res if st in (200, 204)]
    print(f"{name:<22} n={n:<3} codes={codes} p50={pct(ms, .5) if ms else None} p95={pct(ms, .95) if ms else None} max={round(max(m for _, m in res))} wall={round(wall)}ms "
          f"=> {round(wall / n)} ms/solicitud (serie efectiva) | CPU pico %: {peak}", flush=True)
    time.sleep(4)


for n in (1, 5, 20):
    run(f"PDP-solo resumen x{n}", summary, n)
for n in (1, 5, 20, 40):
    run(f"PEP->PDP decision x{n}", decision, n)
