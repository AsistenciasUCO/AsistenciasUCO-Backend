---
status: active
type: evidence
scope: backend+frontend+infra
owner: backend-team
last-reviewed: 2026-11-02
---

# Security Baseline 0.1.8 → 0.1.9: rendimiento y auditoría de regresión — evidencia 2026-11-02

Sin secretos, tokens, cookies, contraseñas ni datos personales. Sin commit, push ni merge. No se sobrescribe la evidencia de
[SEC-BASELINE-018](../SEC-BASELINE-018-upgrade-login/VALIDATION.md) (que sigue siendo la referencia de 0.1.8).
Convención de la evidencia: **[M]** medido en este laboratorio · **[O]** observación directa · **[H]** hipótesis sin verificar ·
**[P]** dato publicado por el proveedor (no medido aquí).

## 1. Veredicto

**Security Baseline 0.1.9 está instalado, saludable y sin pérdida de datos; la latencia mejora de forma medible (−47 % a −88 % en p95); los 503
por ráfagas moderadas desaparecen; fail-closed, login y panel se conservan. NO se declara resuelto el problema de 503 en general:** con 50
decisiones simultáneas directas al PEP siguen fallando 67 de 150 (45 %) [M]. Causa medida: la decisión PEP→PDP se serializa a ≈68 ms y el PEP
corta a los 3 s (`pep.pdp.timeout=3s`), lo que fija un techo de ≈44 decisiones en vuelo. Sin regresiones críticas introducidas por 0.1.9. No fue
necesario el rollback.

## 2. Versiones, SHAs y digests

| Componente | v0.1.8 (antes) | v0.1.9 (ejecutándose) |
|---|---|---|
| `securityBaseline` (tag) | `a099dd54ef889c4936e37f4582b23cf09912c7ec` | `2ede2aa42e326cb85349558ff3e3023148d2237e` (verificado; run `Publish security Docker distribution` del tag: success) |
| `securityBaseline-vue` | v0.1.8 `6a10dd2e…` (sin cambio; no existe v0.1.9) | sin cambio |
| `security-ui` | v0.1.8 `c7647a3f…` (sin cambio; no existe v0.1.9) | sin cambio |
| `davidalzate/security-keycloak-local` | `sha256:0a9363bea4cb…` (Keycloak 26.7.2) | `0.1.9` `sha256:375d5da4edcaa36da39f1fe64a54293518770c635e555f9459314d2152fb408e` |
| `davidalzate/security-pdp` | `sha256:29cf1c31b266…` | `0.1.9` `sha256:049db9baaa8f1451656f95d1c39bf3452d0ebb13dd231f65b5665bbb1a2ed76d` |
| `davidalzate/security-pep` | `sha256:e869002dde3c…` | `0.1.9` `sha256:4287de61ee62d3f6ab4292071e901063cc41c2867cf17636fbaaaff89649e160` |
| `davidalzate/security-opa` | `sha256:8fe077cc267c…` | `0.1.9` `sha256:981c47fe2e1ba1916976c8f0fe3d8287a13f32f39e73b6e4a7b791acca5d8dfb` |
| Redis | `redis:7.4-alpine` → `sha256:8da19c7399dc…` (7.4.12) | `redis:7.4.12-alpine` → **misma imagen** `8da19c7399dc…` |
| SurrealDB | `v3.3.0` | `v3.3.0` (contenedor no recreado) |

Los digests ejecutándose coinciden con Docker Hub. Backend de AsistenciasUCO: rama `fix/ident001-confirmar-correo-baseline-20261101` sobre `b288ca51`, con
cambios locales sin commit (IDENT-001, lanzador, compose, docs).

## 3. Qué cambia 0.1.9 [diff `v0.1.8..v0.1.9`, 9 archivos, +399/−14]

1. `perf(persistence)`: el PDP se autentica contra SurrealDB **una vez** (`/signin`, token de sesión en memoria con renovación 60 s antes de expirar, un
   solo signin compartido, reintento ante 401) en lugar de usuario/contraseña en **cada** consulta (`SurrealTokenAuthentication`,
   `SurrealDbConfiguration`). Solo código del PDP; PEP, OPA, Keycloak y el realm no cambian de código.
2. `distribution/compose.yaml`: pasa `PDP_CORS_ALLOWED_ORIGINS` y `PDP_LOGIN_ALLOWED_RETURN_ORIGINS` al PDP y fija Redis `7.4.12-alpine`.
3. Documentación (benchmark del proveedor en `pdp/docs/ai-harness/CHECKPOINT.md`). Ningún endpoint retirado; el starter Java no cambia.

## 4. Cambios aplicados en AsistenciasUCO

| Archivo | Cambio |
|---|---|
| `infra/security-baseline/compose.yaml` | cabecera 0.1.9; `redis:7.4-alpine` → `redis:7.4.12-alpine` (misma imagen). **Nada más**: ya llevaba CORS/orígenes de retorno de 0.1.8 |
| `infra/security-baseline/lab.env.example` | `SECURITY_VERSION=0.1.9` |
| `%USERPROFILE%\.asistencias-security-lab\lab.env` (fuera de Git) | `SECURITY_VERSION=0.1.8` → `0.1.9`; el resto intacto |

Se conservan `name: asistencias-security-lab`, los tres volúmenes, la red `asistencias-security-lab-net`, los puertos 19090/18080/18081 y los orígenes
`http://localhost:4200`, `http://127.0.0.1:4200`, `:5173`. No se tocó el realm ni SQL Server. Recreados: keycloak, opa, pdp, pep y redis; surrealdb no.

## 5. Respaldo, inventario y datos

Carpeta `%USERPROFILE%\.asistencias-security-lab\backups\pre-0.1.9-20261102-0315\` (incluye `ROLLBACK-0.1.9-a-0.1.8.md`).

- En caliente: `surreal export` ns/db `pdp` (7,9 MB), `redis-cli --rdb` (`redis-check-rdb` OK), `docker inspect` sin entorno, compose y `lab.env` previos.
- En frío (`compose stop`, sin `down`): tgz de los tres volúmenes con sha256 en `manifest-cold.json`; **restauración de prueba** en volúmenes temporales
  `sbverify-*` (eliminados): SurrealDB con conteos idénticos al export en caliente y Keycloak con 31 usuarios / 13 clientes.
- **Inventario Keycloak + PDP antes/después: 0 diferencias** [M]: 29 usuarios (API; 31 en export con cuentas de servicio), 13 clientes, scopes, roles de `asistencias-api`
  (ADMINISTRADOR/COORDINADOR/DECANO/DOCENTE/ESTUDIANTE con los mismos miembros), 161 recursos, concesiones por rol (118/38/68/45/28), 5 perfiles y
  asignaciones.
- SurrealDB (conteos tras el respaldo): application 2, assignment 43, external_identity 28, profile 5, profile_assignment 22, protected_resource 216, role 10,
  security_user 28, tenant 3; `access_event` crece con cada decisión (14.723 al respaldar) — crecimiento legítimo, no pérdida.
- Security UI: Personas 51 · Roles 6 · Perfiles 5 · Recursos 161 · Administradores 2 (idéntico a 0.1.8).

**Rollback.** Documentado en `ROLLBACK-0.1.9-a-0.1.8.md`. Comprobado [M]: las 4 imágenes 0.1.8 siguen locales y `SECURITY_VERSION=0.1.8 docker compose … config --images`
resuelve a `0.1.8` con el mismo compose; la restauración de datos desde el respaldo se verificó en volúmenes temporales. **No se ejecutó el rollback real
(no hizo falta).** Como 0.1.9 no cambia esquema ni realm, el rollback normal es solo cambiar `SECURITY_VERSION` y `up -d`.

## 6. Rendimiento medido (mismo método antes/después)

Método [M]: `evidence/sb_bench.py` (token PKCE real de `uco-administrador`, GET/decisiones de solo lectura; calentamiento 5/3/3/3; tiempo por petición con
`perf_counter`; timeout de 15 s; 3 s de pausa entre ráfagas; 3 repeticiones por ráfaga del PEP y 2 por las del backend). Host Windows 11, Docker Desktop 16 CPU / 7,6 GiB,
sin límites de CPU/memoria en los contenedores. 0.1.8 se midió con el PDP en marcha ~2 h (JIT caliente) y 0.1.9 ~3 min después de arrancar con 5 peticiones de calentamiento, por lo que la comparación
**favorece a 0.1.8** (conservadora para 0.1.9). Total ≈ 560 peticiones por versión; sin carga sostenida. Datos crudos: `evidence/bench-v018.json`, `evidence/bench-v019.json`.

| Escenario | n | v0.1.8 p50/p95/máx (ms) | v0.1.8 éxitos | v0.1.8 errores | v0.1.9 p50/p95/máx (ms) | v0.1.9 éxitos | v0.1.9 errores | p95 Δ |
|---|---:|---|---:|---|---|---:|---|---:|
| S1 Decisión PEP individual | 30 | 535 / 738 / 754 | 30/30 | — | 142 / 168 / 181 | 30/30 | — | −77 % |
| S2 Resumen de seguridad (PDP) | 20 | 267 / 378 / 378 | 20/20 | — | 26 / 47 / 48 | 20/20 | — | −88 % |
| S3 `/auth/me` autenticado | 30 | 472 / 714 / 727 | 30/30 | — | 151 / 231 / 240 | 30/30 | — | −68 % |
| S4 Cadena completa `admin/decanos` (Spring→PEP→PDP→SQL) | 15 | 452 / 474 / 478 | 15/15 | — | 138 / 156 / 159 | 15/15 | — | −67 % |
| S5 PEP, 5 simultáneas | 15 | 1074 / 1152 / 1156 | 15/15 | — | 535 / 553 / 561 | 15/15 | — | −52 % |
| S5 PEP, 20 simultáneas | 60 | 3991 / 4300 / 4335 | 24/60 | 503 ×36 | 1876 / 2287 / 2295 | **60/60** | — | −47 % |
| S5 PEP, 50 simultáneas | 150 | 4086 / 5155 / 5210 | 14/150 | 503 ×136 | 4048 / 4494 / 4532 | 83/150 | **503 ×67** | −13 % |
| S6 Backend `admin/decanos`, 5 simult. | 10 | 780 / 864 / 864 | 10/10 | — | 490 / 920 / 920 | 10/10 | — | +7 % (n=10; un valor atípico; ruido) |
| S6 Backend `admin/decanos`, 20 simult. | 40 | 2894 / 3482 / 3553 | 35/40 | 503 ×5 | 1585 / 1623 / 1630 | **40/40** | — | −53 % |
| S6b Backend `auth/me`, 5 simult. | 10 | 833 / 1142 / 1142 | 10/10 | — | 354 / 390 / 390 | 10/10 | — | −66 % |
| S6b Backend `auth/me`, 20 simult. | 40 | 2824 / 3263 / 3325 | 40/40 | — | 1460 / 1495 / 1498 | 40/40 | — | −54 % |

Códigos 401/403/429/500: ninguno en estos escenarios (solo 204/200 y los 503 indicados). Timeouts del cliente (15 s): 0.

**Páginas (navegador integrado, panel visible)** [M]:

| Medida | v0.1.8 | v0.1.9 |
|---|---|---|
| Carga de `/app/admin/decanos`: las 4 peticiones simultáneas de la SPA, observadas por el backend (n=10 / n=17 cargas) | p50 1.129–1.192 ms, p95 1.709–1.758 ms | p50 398–407 ms, p95 523–550 ms |
| Tiempo de la petición `admin/decanos` en el navegador (Resource Timing, n=15) | no medido en 0.1.8 | p50 226 ms, p95 431 ms |
| `GET security/summary` desde Security UI (navegador, n=5) | no medido en el navegador (script: p50 267 ms, p95 378 ms) | p50 20 ms, p95 37 ms |
| `/auth/me` tras el login | 724–1.366 ms (log) | 206 ms (log) |
| Contenido de las 10 pantallas del administrador (longitud de texto) | idéntico | idéntico |

**Referencias publicadas por Seguridad-UCO [P] (no son resultados de este sistema):** decisión individual p95 438→88 ms; resumen 772→65 ms; 20 simultáneas 1.500→360 ms; 50
simultáneas 3.500→750 ms. Frente a ellas, en este laboratorio la mejora es real pero **menor**: decisión individual p95 738→168 ms (≈ −77 %, vs. −80 % publicado), resumen 378→47 ms (−88 %,
similar al −92 % publicado), 20 simultáneas 4.300→2.287 ms (−47 %, vs. −76 % publicado) y 50 simultáneas sin llegar a 750 ms (4.494 ms, con errores). Los números absolutos del proveedor
y los nuestros no son comparables (distinto equipo/metodología).

### 6.1 Diagnóstico de los 503 que persisten a 50 simultáneas

`evidence/sb_diag_burst.py` [M] (CPU muestreada **durante** la carga):

| Prueba | wall | ms por solicitud efectivos | CPU pico PDP |
|---|---:|---:|---:|
| PDP solo (resumen) ×20 | 158 ms | 8 | 34 % |
| PEP→PDP decisión ×1 / ×5 / ×20 / ×40 | 111 / 361 / 1.351 / 2.705 ms | 111 / 72 / 68 / 68 | 10 % / 37 % / 103 % / 127 % |

- Las rutas del PDP que no pasan por el PEP escalan en paralelo [M]; la decisión PEP→PDP **se serializa a ≈68 ms/decisión** (~15 decisiones/s) sin importar la concurrencia [M].
- El PDP llega a ≈1 núcleo de 16 (103–127 %) mientras el resto de servicios está ocioso; SurrealDB ≤ 65 %, Redis, OPA y Keycloak < 5 % [M]. Ningún contenedor tiene límites.
- El PEP corta la llamada al PDP a los **3 s** (`pep.pdp.timeout=3s`, `connect-timeout=1s`; código fuente `application.properties` del tag) [M/fuente]: 3 s / 68 ms ≈ **44 decisiones en vuelo**. Con 50 simultáneas
  (3,4 s serializadas más variación) las últimas expiran y el PEP responde 503 (fail-closed correcto). Los ~4 s observados incluyen la validación del token en el PEP.
- [H] La serialización podría deberse a la escritura de la evidencia de auditoría por decisión (`access_event`, una fila por decisión: 14.469 → 14.723 entre los dos respaldos en caliente) o a un único hilo/bloqueo en la ruta de decisión del PDP; **no se verificó** (el código no
  muestra `synchronized`, semáforos ni `.block()` en el PDP salvo el inicializador de esquema). Debe confirmarlo Seguridad UCO.
- El timeout de 3 s del PEP **no se modificó**.

## 7. Auditoría de hallazgos anteriores

| ID | Hallazgo | Estado en v0.1.8 | Resultado en v0.1.9 | Evidencia | Clasificación | Acción recomendada |
|---|---|---|---|---|---|---|
| H-01 | Latencia PDP | ≈650 ms por decisión (script: p50 535 ms, p95 738 ms) | p50 142 ms, p95 168 ms; resumen p50 26 ms | §6 S1/S2 [M] | **MEJORADO** | Aceptar; pedir a Seguridad UCO el objetivo del proveedor (p95 88 ms) |
| H-02 | 503 por concurrencia | 20 simult. → 60 % de 503; 50 → 91 % | 20 simult. → 0 errores (PEP y backend); 50 directas al PEP → 45 % de 503 | §6 S5/S6, §6.1 [M] | **MEJORADO** (no resuelto del todo) | Escalar a Seguridad UCO la serialización de la decisión PDP (§6.1); no subir `pep.pdp.timeout` sin justificarlo; limitar el paralelismo en el cliente si hace falta |
| H-03 | `uco-decano` 403 (sin facultad en SQL) | 403 `FORBIDDEN`/`KPI_003` en todo `decano/*` | Igual; E2E `DECANO GET decano/programas` → 403; `uco-coordinador` igual (sin programa) | e2e 17/18 y 19/20, `decano_api` 18×403 [M] | **PERSISTENTE** | Owner DB: asignar facultad/programa a las cuentas semilla (no se modificó) |
| H-04 | Concesión sobrante ESTUDIANTE | 1 concesión + 3 recursos huérfanos | Idéntica: `SOBRA ESTUDIANTE GET /estudiante/materias/{materiaId}/prerrequisitos`; sin concesiones nuevas (conteos 118/38/68/45/28 sin cambio) | `provision_asistencias.py verify` [M] | **PERSISTENTE** | Revocar con `--prune` solo con autorización específica |
| H-05 | Credenciales PEP expuestas en una salida de herramienta | Dos `*_PEP_APPLICATION_CREDENTIAL*` expuestas | Sin cambio; no rotadas (no se reprodujeron valores en esta sesión) | `lab.env` (nombres), §9 | **PERSISTENTE** (riesgo abierto) | Rotar con el procedimiento del §9 en ventana de mantenimiento |
| H-06 | Endpoints 501 | `admin/sedes`, `admin/auditoria` y otros (13 en admin, 3 coordinador, 1 docente, 3 estudiante) | Mismo conjunto exacto de 501 y el mismo 500 (`archivos/subir`); **ningún 5xx nuevo** | sondas por rol (`admin` 13, `coord` 3, `docente` 1, `estudiante` 3+1) y log del backend [M] | **PERSISTENTE** (conocidos; sin regresión) | Mantener separados de regresiones; implementarlos o ocultar su UI (F-05/F-11) |
| H-07 | Pruebas por rol | 973 decisiones/0 fallos; E2E 17/18; `--pep-down` 19/20 | 973/0 (5 renovaciones de token en vez de 10: la corrida terminó en ~la mitad de tiempo); E2E 17/18; `--pep-down` 19/20; único fallo = H-03 | `verify-decisions`, `e2e_baseline.py` [M] | **PERSISTENTE** (sin regresión; 1 fallo conocido) | Cerrar H-03 |
| H-08 | Login y panel admin | Login, panel, 9 secciones, Security UI, F5, logout, nuevo login OK | Idéntico: login real (`/auth/me` 206 ms), 10 pantallas con el mismo contenido, Security UI 51/6/5/161/2, F5 conserva ruta, logout → `/login`, enlace directo no reabre sesión, nuevo login OK | §5, §6 páginas [M/O] | **RESUELTO** (conforme; sin regresión) | — |
| H-09 | Fail-closed | PEP caído → `/auth/me` 503 + pantalla recuperable | Idéntico: `/auth/me` 503 (9 ms) con el PEP detenido, pantalla "No se pudo establecer conexión…" + reintento, sin acceso; al restaurar, reintento → panel | `--pep-down` y UI [M/O] | **RESUELTO** (conforme; conservado) | — |
| H-10 | Integridad de datos | Keycloak 31 (export)/29 (API), Security UI 6/5/161 | 0 diferencias de inventario; Security UI 51/6/5/161/2 | §5 [M] | **RESUELTO** (conforme) | — |
| F-01 | "Proyectar QR" es una maqueta (PIN fijo) | presente | presente (no se tocó el frontend) | SEC-018 §K.2 | **PERSISTENTE** | Frontend: conectar a `qr-token` o retirar la acción |
| F-02 | Hora de sesión desfasada 5 h entre docente/coordinador/estudiante | presente | presente | SEC-018 §K.1/§M.4 | **PERSISTENTE** | Decisión contractual: v2 UTC también para coordinador/estudiante |
| F-03 | Banderas obsoletas `teacherClaimsEnabled`, `USER_PROFILE_ENDPOINT_ENABLED` | presente | presente | SEC-018 §K.3/§M.2 | **PERSISTENTE** | Frontend: actualizar banderas/comentarios |
| F-04 | `archivos/subir` no multipart → 500 | presente | presente (`POST` con JSON → 500 `INTERNAL_ERROR`) | `estudiante_api`, log [M] | **PERSISTENTE** | Backend: mapear `MultipartException` a 400/415 |
| F-05 | Parámetros globales: frontend `PATCH` vs backend `PUT` (501) y aviso engañoso | presente | presente | SEC-018 §O.1 | **PERSISTENTE** | Alinear verbo/contrato e implementar `PUT`; manejar el error |
| F-06 | `GET /users` y `GET /applications` del PDP abiertos a cualquier Bearer | presente (28 usuarios con nombre/correo/último acceso visibles a ESTUDIANTE) | **Idéntico**; rutas por aplicación siguen en 403 `NOT_AUTHORIZED_TO_ADMINISTER` | `pdp_admin_authz.py` [M] | **PERSISTENTE** (`SECURITY_FINDING`, Seguridad UCO) | Pedir restricción al administrador de alguna aplicación (ADR-036) |
| F-07 | Oferta de Grupos vacía; horarios/aula "No informado" | presente | presente | SEC-018 §M.5/§M.6 | **NO VERIFICADO** (requiere regla de negocio/DB) | Owner DB/negocio |
| N-01 | Techo de ≈44 decisiones en vuelo (serialización ≈68 ms + `pep.pdp.timeout=3s`) | no caracterizado (el PDP más lento ocultaba el techo) | caracterizado en §6.1 | §6.1 [M] + [H] sobre la causa | **NUEVO** (límite descubierto; no regresión) | Reportar al proveedor; documentar el límite operativo |
| N-02 | PEP registra `PDP readiness failure` al arrancar mientras el PDP aún levanta | no observado | observado una vez en el arranque (08:23:05Z), transitorio (PDP healthy a los segundos) | `docker logs` pep [O] | **NUEVO** (observación menor) | Ninguna; el `depends_on` del PDP es `service_started` |
| N-03 | S6 `admin/decanos` 5 simult.: p95 920 vs 864 ms | — | +7 % con n=10 (un atípico); p50 −37 % | §6 [M] | **NO VERIFICADO** (ruido estadístico) | Repetir con más muestras si interesa |
| N-04 | Redis fijado `7.4.12-alpine` | tag flotante | misma imagen `8da19c73…`; pin adoptado | §2 [M] | **MEJORADO** (reproducibilidad) | — |

## 8. Pruebas funcionales y E2E

| Prueba | Resultado |
|---|---|
| Backend `.\mvnw.cmd -B verify` (ruta normal) | `PASS`: 2.988/0/0/0, ArchUnit incluido, "All coverage checks have been met", BUILD SUCCESS. El `repackage` reescribió `target/*.jar` con el backend vivo: el backend (copia fija) siguió respondiendo `/auth/me` 401 en 4 ms |
| Frontend `npm run verify` | `PASS`: 1.359/1.359; cobertura L 92,21 % / B 75,87 %; realtime L 95,06 % / B 84,52 %; build de producción |
| `/auth/me` sin token / Bearer inválido | 401 en 4 ms |
| `/auth/me` con token válido | 200, `ADMINISTRADOR` (206 ms tras el login) |
| Authorization Code + PKCE real (navegador) | `PASS` (login, retorno, `/app/admin/decanos`) |
| Security UI con Bearer de la SPA | `PASS` (resumen, Personas 51, Roles 6, Perfiles 5, Recursos 161, Administradores 2) |
| RBAC: 973 decisiones PEP/PDP | `PASS` 973/0 |
| Sondas de API por rol (admin, decano, coordinador, docente, estudiante) | mismos códigos que 0.1.8; denegaciones cruzadas 403; Bearer inválido 401; sin 5xx nuevos |
| F5 real | `PASS`: vuelve a la ruta con datos |
| Logout y nuevo login | `PASS` (logout → `/login`; enlace directo sin reautenticación silenciosa; nuevo login con credenciales tecleadas por el usuario) |
| PEP caído y recuperación | `PASS` (503 + pantalla recuperable; reintento tras restaurar) |
| Sin pantalla en blanco ni solicitudes colgadas; tokens fuera de `localStorage`/`sessionStorage` | `PASS` (almacenamiento vacío; sin cadenas JWT) |

## 9. H-05 — procedimiento propuesto para rotar las credenciales del PEP (NO ejecutado)

Credenciales afectadas (solo nombres): `SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL` (aplicación de laboratorio `LAB_PDP_APPLICATION_ID`, no la usa AsistenciasUCO) y
`SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS` (aplicación `asistencias-uco`, **la usa el backend**).

1. Ventana de mantenimiento corta; detener solo el backend local (lanzador de `infra/security-baseline/scripts/start-backend-baseline.ps1`). PEP/PDP siguen arriba.
2. Respaldar `lab.env` (copia fuera de Git) y confirmar que el respaldo SurrealDB de §5 está vigente.
3. Rotar con la sesión del administrador del PDP: `POST /api/v1/applications/{id}/credential-rotations` (el mismo endpoint que usa `provision_asistencias.py` al aprovisionar). **[NO VERIFICADO]** si la
   credencial anterior se invalida de inmediato o con gracia; por eso el backend debe estar detenido durante la rotación (fail-closed 503 mientras tanto).
4. Escribir el valor nuevo **directamente** en `lab.env` (`write_env_value`), sin imprimirlo ni pasarlo por chat/herramientas; la aplicación de laboratorio se rota igual o se retira si ya no se usa.
5. Iniciar el backend con el lanzador (carga `lab.env` sin imprimirlo) y verificar: `/auth/me` 401 sin token y 200 con token; `provision_asistencias.py verify-decisions` 973/0; E2E de roles.
6. Confirmar con el log del PEP que la credencial anterior es rechazada. Si algo falla, restaurar el `lab.env` respaldado (solo sirve si la credencial anterior sigue vigente).
7. Registrar fecha y resultado sin valores. Además, revisar que ningún archivo de evidencia conserve las credenciales anteriores.

## 10. Recomendaciones priorizadas

| Prioridad | Recomendación |
|---|---|
| **Alta** | Rotar las dos credenciales del PEP (H-05) con el procedimiento del §9. |
| **Alta** | Seguridad UCO: restringir `GET /users` y `GET /applications` (F-06; exposición de datos personales entre roles). |
| **Alta** | Seguridad UCO: explicar/eliminar la serialización de la decisión PDP (≈68 ms) o ampliar la capacidad; documentar el techo (≈44 en vuelo con `pep.pdp.timeout=3s`) (N-01/H-02). |
| **Media** | Asignar facultad/programa a `uco-decano`/`uco-coordinador` en SQL (H-03); decidir la concesión sobrante (H-04). |
| **Media** | Corregir el contrato de parámetros (F-05), `archivos/subir` 500 (F-04) y la maqueta de QR (F-01); decisión de zona horaria (F-02). |
| **Baja** | Banderas obsoletas (F-03); 501 visibles en UI (H-06); oferta de grupos (F-07). |

## 11. Limitaciones

- Un solo equipo y una sola corrida por versión (n por escenario indicado); sin intervalos de confianza. 0.1.9 se midió con calentamiento menor que 0.1.8.
- Latencias de página 0.1.8 en navegador no disponibles; se usan las del backend durante cargas de UI como sustituto.
- No se probaron escrituras válidas (alta/edición/revocación) ni el botón "Guardar" de parámetros (bloqueado por el clasificador de permisos en la sesión anterior), ni el simulador/cierre masivo.
- La causa de la serialización del PDP es una hipótesis (§6.1). La mejora publicada por el proveedor no se pudo reproducir en magnitud (§6).
- Rollback real no ejecutado; solo verificados respaldo restaurable e imágenes/configuración 0.1.8.
