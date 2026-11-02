---
status: active
type: evidence
scope: backend+frontend+infra
owner: backend-team
last-reviewed: 2026-11-02
---

# Security Baseline 0.1.7 → 0.1.8 y login real de AsistenciasUCO — evidencia 2026-11-02

Sin secretos, tokens, cookies ni contraseñas. Los valores viven en `%USERPROFILE%\.asistencias-security-lab\lab.env` y
en el `.env` del backend (fuera de Git). Sin commit, push ni merge.

## A. Versiones

| Componente | Antes | Después (ejecutándose) |
|---|---|---|
| `davidalzate/security-keycloak-local` | 0.1.7 (`f93e4229…`) | 0.1.8 `sha256:0a9363bea4cb086fe7fa504cea6a671be7b0ae0fc940a0ea471dc01078f7fa06` (Keycloak 26.7.2) |
| `davidalzate/security-pdp` | 0.1.7 (`06e90120…`) | 0.1.8 `sha256:29cf1c31b26661b853b6c870613ffbc967e75833077cc31db721a7b46e36ccb9` |
| `davidalzate/security-pep` | 0.1.7 (`1b5ed8a6…`) | 0.1.8 `sha256:e869002dde3cbe3415a4960199d15642d86752248d2722a534e6698ee4df711d` |
| `davidalzate/security-opa` | 0.1.7 (`e04d92d6…`) | 0.1.8 `sha256:8fe077cc267c41a7e6a27c663c5a33d024f9418ca753f1f3e3ac1bf1e4a1c620` |
| `redis:7.4-alpine` | 7.4.11 | 7.4.12 (tag flotante de terceros; lo trajo `compose pull`) |
| `surrealdb/surrealdb:v3.3.0` | sin cambio (contenedor no recreado) | sin cambio |

Digests idénticos a Docker Hub; imágenes creadas 05:24–05:26Z tras el run exitoso `Publish security Docker
distribution` del tag `v0.1.8` (05:23:51Z). Fuentes: `securityBaseline` `a099dd54ef889c4936e37f4582b23cf09912c7ec`
(clonado solo como código en `Front_Asistecias\Frontend_Seguridad\securityBaseline`, `core.longpaths=true`),
`securityBaseline-vue` `6a10dd2e89dcfd3b56d50547b8c2c2900064f4b4` y `security-ui` `c7647a3fe4c266e5498a9c4ad7dc62ea00da1cf3`
(solo `fetch`; sus cambios locales intactos). `distribution/compose.yaml` no cambia entre 0.1.7 y 0.1.8.

## B. Respaldo y persistencia

Carpeta: `%USERPROFILE%\.asistencias-security-lab\backups\pre-0.1.8-20261102-0112\` (incluye `ROLLBACK.md`).

- En caliente: `surreal export` ns/db `pdp` (6,6 MB), `redis-cli --rdb` (`redis-check-rdb` OK, DBSIZE 0), metadatos
  `docker inspect` sin valores de entorno, `compose.yaml`, override externo y `lab.env` previos.
- En frío (`compose stop`, sin `down`): tgz de los volúmenes `keycloak-data`, `surrealdb-data`, `redis-data` con sha256
  en `manifest-cold.json`.
- Restauración de prueba en volúmenes temporales `sbverify-*` (eliminados después): `kc.sh export` 0.1.7 del realm
  `security-baseline` con usuarios (31 usuarios incl. cuentas de servicio, 13 clientes); SurrealDB arrancado sobre la
  copia con conteos por tabla idénticos al export en caliente (security_user 28, assignment 43, profile_assignment 22,
  protected_resource 216, role 10, application 2, access_event 12.372). El chequeo AOF de Redis sobre la copia no se
  evaluó (montaje `:ro`); Redis estaba vacío.
- Parches de los cambios locales sin commit: `backend-local-changes.patch`, `frontend-local-changes.patch`.
- Inventario Keycloak+PDP (`inventory-before.json` / `inventory-after-upgrade.json`): **idéntico** tras recrear el stack.
  El volumen `keycloak-data` conserva su propio `import/security-baseline-realm.json` y `--import-realm` omite realms
  existentes: el realm no se reemplazó.

## C. Cambios aplicados

- `infra/security-baseline/compose.yaml`: cabecera 0.1.8 y `PDP_LOGIN_ALLOWED_RETURN_ORIGINS` + `PDP_CORS_ALLOWED_ORIGINS`
  en el propio compose (antes los inyectaba `Frontend_Seguridad\compose.security-baseline.override.yaml`). Mismo proyecto,
  volúmenes, puertos 19090/18080/18081, red e IP fija.
- `lab.env`: `SECURITY_VERSION=0.1.8` (resto intacto); `lab.env.example` alineado.
- Realm (migración aditiva e idempotente; segunda ejecución = "nada que hacer"): scope `security-baseline-app` (tenant,
  sub, audiencia PDP) tomado literalmente del realm oficial 0.1.8; mapper `email verified` en `email`; mappers `full name`,
  `username`, `given name`, `family name` en `profile`; `security-baseline-app` como default scope de `asistencias-uco-spa`
  (conserva `security-baseline-claims`). Sin cambios en usuarios, roles, asignaciones, secretos ni otros clientes.
- No se habilitó `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED`, no se concedieron roles ni se podó el PDP.
- `infra/security-baseline/scripts/start-backend-baseline.ps1`: rechaza jars sin `BOOT-INF`, ejecuta siempre una copia
  inmutable en `%LOCALAPPDATA%\AsistenciasUCO\backend-run` y acepta `-JarPath` (ver D).

## D. Causa raíz del timeout de `/api/v1/auth/me`

El backend PID 5876 (arrancado 01/11 22:38 con `java -jar target\AsistenciasUCO-0.0.1-SNAPSHOT.jar`) siguió vivo después
de que un `mvnw package/verify` (23:11) **sobrescribiera ese jar** con uno plano (sin `BOOT-INF`, `repackage` fallido). El
cargador de Spring Boot lee clases bajo demanda: las rutas ya cargadas (health) seguían respondiendo, pero toda ruta que
necesitaba clases nuevas fallaba. Evidencia JFR (`jdk.JavaErrorThrow`) en `GET /api/v1/auth/me` sin token:
`NoClassDefFoundError: ch/qos/logback/core/status/ErrorStatus` (desde `AuthorizationFilter` → mensaje de seguridad → WARN)
y `NoClassDefFoundError: io/opentelemetry/sdk/common/internal/ExceptionAttributeResolver$AttributeSetter`. El `Error`
rompe incluso el registro del error y el socket queda abierto sin respuesta (decenas de conexiones `CLOSE_WAIT`, ningún
hilo vivo atendiéndolas; `jdk.trackAllThreads` está desactivado, por eso `Thread.print` no mostraba nada). El rollover de
logs de medianoche murió por la misma causa (último evento 23:59:59.727). El mismo jar recién construido en un proceso
nuevo respondía `/auth/me` → 401 en 14 ms. No hay defecto en el código de `/auth/me`.

Corrección: el lanzador nunca ejecuta `target\*.jar` (copia inmutable). Prueba: con el backend nuevo corriendo, `mvnw
verify` reescribió `target\*.jar` (repackage OK) y el backend siguió respondiendo `/auth/me` → 401 en 13 ms.

## E. Pruebas (2026-11-02)

| Prueba | Resultado |
|---|---|
| Backend `.\mvnw.cmd -B verify` (ruta normal) | `PASS`: 2.988/0/0/0, ArchUnit incluido, "All coverage checks have been met", BUILD SUCCESS |
| Backend `verify` en `target/sb018-build` (aislado) | 2.970 OK + 18 ArchUnit `FAIL` por artefacto de ruta (`DoNotIncludeTests` reconoce `/target/test-classes/`); ArchUnit en ruta normal 98/0 |
| Frontend `npm run verify` | `PASS`: 1.359/1.359, cobertura L 92,21 % / B 75,87 %, realtime L 95,06 % / B 84,52 %, build producción |
| `provision_asistencias.py verify` | deriva **preexistente** (idéntica antes/después): 1 concesión ESTUDIANTE sobrante en `GET /estudiante/materias/{materiaId}/prerrequisitos` y 3 recursos huérfanos; no se podó |
| `verify-decisions` (tokens PKCE del SPA migrado, renovados cada 200 s) | `PASS` 973 decisiones / 0 fallos (sin renovar: 214 × 401 por expiración de 300 s del token) |
| `e2e_baseline.py` (8080) | 17/18; `--pep-down` 19/20 con "PEP caído → 503 sin bypass" y "PEP recuperado → 200" |
| `/auth/me` sin token / Bearer inválido / con token ADMINISTRADOR | 401 (28 ms) / 401 / 200 con `idUsuario` y `roles=[ADMINISTRADOR]` |

Fallo E2E restante: DECANO `GET /api/v1/decano/programas` → 403 del **backend** (el PEP decide 204):
`ProgramaAcademicoRules` no resuelve la facultad de `uco-decano` (`idFacultad` nulo en SQL). Dato de SQL Server, fuera de
alcance; no se modificó.

## F. E2E real en navegador (Authorization Code + PKCE, usuario `uco-administrador`, credenciales tecleadas por el usuario)

| Paso | Resultado |
|---|---|
| `localhost:4200/login` renderiza; botón institucional → Keycloak (login BFF del PDP) → retorno → PKCE SPA | `PASS` |
| `/auth/me` 200 → navegación del Router a `/app/admin/decanos`; panel con datos | `PASS` |
| Consola sin errores críticos; sin solicitudes pendientes | `PASS` (solo 501 `FEATURE_UNAVAILABLE` conocidos de `admin/sedes` y `admin/auditoria`) |
| Token solo en memoria (`localStorage`/`sessionStorage` vacíos, sin cookies de la SPA) | `PASS` |
| Menú: dashboard, facultades, indicadores, sedes, áreas, periodos, sistema, seguridad, decanos (Router, mismo documento) | `PASS`; Security UI montado (Bearer al PDP: 2 administradores, 51 asignaciones, 6 roles, 5 perfiles, 161 recursos) |
| Recarga real (documento nuevo) | `PASS`: reautenticación silenciosa, `/auth/me` 200, misma ruta |
| PEP detenido + recarga | `PASS`: `/auth/me` 503 → login con "No se pudo establecer conexión…" y reintento; panel no accesible |
| PEP recuperado + reintento | `PASS`: vuelve al panel por SSO |
| Logout | `PASS`: `/login`; enlace directo al panel → `/login`; Keycloak vuelve a pedir credenciales |
| Nuevo login tras logout (credenciales tecleadas por el usuario) | `PASS`: documento nuevo, `/auth/me` 200 (02:12:32), panel con datos |

Roles DOCENTE, COORDINADOR, DECANO y ESTUDIANTE: verificados a nivel API (decisiones PEP/PDP y E2E por rol). No se
verificaron en la UI (requieren sus credenciales).

## G. Pendientes / hallazgos

- Latencia PDP 0.1.8: ~650 ms por decisión en serie, ~2 s con 6 concurrentes, CPU en reposo (0.1.7: mediana ~405 ms por
  petición). Ráfagas del panel superaron el timeout de lectura PEP→PDP (5 s) → `Enforcement no disponible` → 503
  (fail-closed correcto; observado 01:55:48 en `admin/facultades` y `admin/sedes`). Para Seguridad UCO; no se subieron timeouts.
- DECANO sin facultad en SQL (D/E). Deriva PDP preexistente (poda requiere autorización).
- `SECURITY_FINDING`: dos claves `SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL*` de `lab.env` aparecieron en la salida
  de una herramienta del agente durante el inventario. Recomendado rotarlas.
- Redis tag flotante (`7.4-alpine`): fijar digest si se quiere reproducibilidad.

## H. Lado DECANO (2026-11-02, sesión real en el navegador integrado con una cuenta DECANO que sí tiene facultad)

Recorrido por el Router (mismo documento) de las 11 rutas del menú + perfil; cada pantalla renderiza datos y todas sus
llamadas responden 200 en el log del backend, salvo `GET /api/v1/admin/sedes` → 501 `FEATURE_UNAVAILABLE` (conocido; la
tarjeta muestra "Total Espacios Físicos: No disponible", sin pantalla rota).

| Ruta | Llamadas observadas (todas 200) |
|---|---|
| `/app/decano/indicadores` | `decano/indicadores`, `decano/aforo-grupos`, `decano/comparativa-programas` |
| `/app/decano/facultad` | `decano/indicadores`, `coordinador/periodos-academicos`, `admin/areas`, `decano/estructura`, `decano/grupos` |
| `/app/decano/grupos` | `decano/grupos` |
| `/app/decano/programas` | `decano/programas`, `decano/coordinadores`, `decano/estructura`, `tipos-programa`, `modalidades-programa`, `jornadas-programa`, `decano/planes-estudio/{planId}/asignaturas` |
| `/app/decano/periodos` | `coordinador/periodos-academicos` |
| `/app/decano/infraestructura` | `admin/areas` 200, `admin/sedes` 501 (conocido) |
| `/app/decano/coordinadores` | `decano/coordinadores`, `decano/programas`, `tipos-identificacion` |
| `/app/decano/estudiantes` | `decano/estudiantes` (5.007 estudiantes, paginado); "Ver ficha" → `decano/estudiantes/{id}` 200 y panel de ficha |
| `/app/decano/estructura` | `decano/estructura` |
| `/app/decano/cronograma` | `decano/sesiones` |
| `/app/perfil` | `usuarios/perfil` |

Guardas (navegación directa): `/app/admin/decanos` y `/app/coordinador/indicadores` redirigen a `/app/decano/coordinadores`;
una ruta inexistente (`/app/coordinador/dashboard`) cae en el comodín → `/login` (comportamiento previo del router; la sesión
SSO no se pierde). La renovación silenciosa del token (`prompt=none`, ~5 min) interrumpe un clic en curso pero vuelve a la
misma ruta.

API real con la cuenta semilla `uco-decano` (token PKCE; `scripts` en scratchpad): `/auth/me` roles=[DECANO]; **todo
`decano/*` → 403** (`FORBIDDEN`/`KPI_003`): esa cuenta (Juan Carlos Gómez Pérez, "Sin Facultad Asignada") no tiene facultad
en SQL, así que el caso de uso no resuelve su ámbito. No es el PEP (decide 204; 973/0). Escrituras con cuerpo inválido
(`POST/PUT/PATCH decano/programas`, `PUT/PATCH decano/coordinadores`, `identidad/cuentas/acceso/*`) → 400 `VALIDATION_ERROR`
sin mutar datos; `POST decano/coordinadores` → 403 por la misma causa. DOCENTE y ESTUDIANTE → 403 en `decano/*`
(lectura y escritura); Bearer inválido → 401. No se ejecutaron escrituras válidas (alta/edición de programas o
coordinadores) para no modificar datos de desarrollo.

Acción pendiente (datos, owner DB): asignar facultad al decano semilla `uco-decano` (idUsuario `e1f2a3b4-…-0001`).

## I. Lado COORDINADOR (2026-11-02, sesión real en el navegador integrado; cuenta con programa "Ingenieria de Sistemas")

Menú: dashboard, docentes, planes de estudio, períodos, indicadores, estudiantes, grupos, materias, asistencias, perfil.
Todas las pantallas renderizan datos y todas sus llamadas responden 200 en el log del backend:

| Ruta / acción | Llamadas (200) |
|---|---|
| `/app/dashboard` | `coordinador/docentes`, `coordinador/planes-estudio`, `coordinador/estudiantes` |
| `/app/coordinador/docentes` (+ "Ver ficha e historial") | `docentes/resumen`, `tipos-vinculacion`, `tipos-identificacion`, `coordinador/docentes`, `docentes/{id}/ficha` |
| `/app/coordinador/planes-estudio` y subrutas `malla`, `editar`, `nuevo`, `asignaturas/nueva` | `coordinador/planes-estudio`, `planes-estudio/{id}/asignaturas` (formularios abiertos, sin enviar) |
| `/app/coordinador/periodos` | `coordinador/periodos-academicos` |
| `/app/coordinador/indicadores` | `coordinador/indicadores`, `coordinador/alertas-tempranas` |
| `/app/coordinador/estudiantes` | `GET /api/v1/estudiantes` (padrón paginado) |
| `/app/coordinador/grupos` ("Nuevo grupo" abre el formulario con asignaturas, períodos, docentes y franjas horarias; no se envió) | `grupos`, `coordinador/planes-estudio`, `coordinador/periodos-academicos`, `coordinador/docentes` |
| `/app/coordinador/materias` | `grupos`, `grupos/{id}/estudiantes`, `planes-estudio/{id}/asignaturas` |
| `/app/coordinador/asistencias` ("Ver sesiones" → "Ver asistencia") | `coordinador/grupos`, `coordinador/grupos/{id}/sesiones`, `coordinador/sesiones/{id}/asistencias` |
| `/app/perfil` | `usuarios/perfil` |

API real con la cuenta semilla `uco-coordinador` (token PKCE): `/auth/me` roles=[COORDINADOR]; los endpoints de alcance de
programa (`coordinador/indicadores|docentes|estudiantes|grupos|planes-estudio…`) → 403 `FORBIDDEN`, el mismo patrón que el
decano semilla (los casos de uso exigen resolver el programa del coordinador, mensaje
"No fue posible resolver el programa del coordinador autenticado."; la cuenta semilla no lo tiene resuelto). Los
endpoints sin alcance de programa responden 200 (`grupos`, `docentes`, `estudiantes`, `periodos-academicos`,
`configuracion/registro-estudiante`, `usuarios/perfil`, `realtime/status`, tipos). Escrituras con cuerpo vacío →
400 (`coordinador/docentes` POST/PUT/PATCH, `grupos` POST/PUT, cuentas de acceso) sin mutar datos. DOCENTE, ESTUDIANTE y
DECANO → 403 en `coordinador/*` (lectura y escritura); Bearer inválido → 401. No se ejecutaron escrituras válidas.

Funciones no implementadas en el backend (501 `FEATURE_UNAVAILABLE`, ya conocidas; la interfaz no las invoca en los
recorridos): `GET/PATCH coordinador/solicitudes-matricula`, `POST coordinador/grupos/{id}/estudiantes`,
`DELETE coordinador/grupos/{id}/estudiantes/{id}`.

Pendiente de datos (owner DB): resolver el programa de la cuenta semilla `uco-coordinador`, igual que la facultad del
decano semilla.

## J. Lado DOCENTE (2026-11-02, sesión real en el navegador integrado: "Docente UCO", grupo "Grupo 001 - Arq Software")

Menú: Panel, Mis Grupos, Mis Horarios, Perfil. Todo responde 200 en el log del backend (`docente/asignaturas`,
`docente/horarios`, `docente/reclamos`, `usuarios/perfil`, `grupos/{id}/estudiantes|asistencias`, `v2/sesiones/grupo/{id}`,
`sesiones/grupo/{id}`, `realtime/stream` SSE, `configuracion/registro-estudiante`).

| Pantalla / acción (solo apertura, nada guardado) | Resultado |
|---|---|
| Mis Grupos → tarjeta del grupo → `/app/asistencia?courseId=…` (2 sesiones, asistencia promedio 50 %, 1 alerta) | PASS |
| "Info & Alumnos" (7 matriculados, "Reenviar enlace") | PASS (campos HORARIO, AULA y DOCENTE TITULAR vacíos, ver K) |
| "Reclamos" del grupo → "Abrir Bandeja Completa" → `/app/docente/reclamos` → "Revisar en Detalle" (reclamo aprobado, soporte adjunto) | PASS (diálogo del grupo vacío, ver K); descarga del soporte NO probada (no se descargan archivos sin permiso) |
| "Tomar Asistencia" (selector de sesión, 7 estudiantes, estados, Todos presentes/ausentes) | PASS; "Guardar Asistencia" NO pulsado |
| "+ Nueva Sesión" (zona `America/Bogota`), "+ Matricular", "QR / PIN Matrícula" | PASS (formularios/diálogos abren; no se enviaron) |
| "Proyectar QR" | abre, pero es una maqueta (ver K) |
| Renovación silenciosa del token (`prompt=none`, ~5 min) a mitad de una acción | PASS: vuelve a la misma pantalla con el rol DOCENTE |

API real con `uco-docente` (token PKCE): `/auth/me` roles=[DOCENTE]; 17 GET → 200 (incluido el reporte Excel, solo estado);
escrituras con cuerpo vacío sobre IDs inexistentes → 400 (`POST asistencias`, `asistencias/lote`, `sesiones`, `v2/sesiones`,
`PUT/PATCH sesiones`, `PATCH docente/reclamos`, `PATCH docente/solicitudes-ingreso`, `POST …/retiro`, `POST grupos/{id}/estudiantes`,
`POST sesiones/cierres`) sin mutar datos. `PATCH docente/sesiones/{id}/cancelar` → 501 `FEATURE_UNAVAILABLE` (no implementado;
`features.sessionCancelEnabled=false`, la UI no lo invoca). Propiedad: sobre un grupo de otro docente → 403 en
`grupos/{id}/estudiantes|asistencias` y `v2/sesiones/grupo/{id}`. ESTUDIANTE, COORDINADOR, DECANO y ADMINISTRADOR → 403 en
`docente/horarios`, `docente/reclamos`, `POST asistencias/lote` y `POST sesiones`; Bearer inválido → 401.

## K. Hallazgos de la revisión DOCENTE/COORDINADOR (preexistentes, no regresiones de 0.1.8)

1. **Zona horaria incoherente entre pantallas.** Las sesiones están en UTC (`2026-08-17T13:00:00Z`). El docente (contrato v2 UTC,
   `session-timezone.service`) las muestra a las 08:00–10:00 hora Bogotá; el coordinador (`/coordinador/grupos/{id}/sesiones`
   devuelve `LocalDateTime` sin zona y `coordinator-attendance` lo pinta con el pipe `date`) muestra 13:00–15:00, es decir, la
   hora UTC como si fuera local (+5 h). Requiere decisión contractual (v2 UTC también para coordinador).
2. **"Proyectar QR" es una maqueta.** `attendance-projection-modal` no recibe PIN ni token reales: PIN fijo por defecto
   `392-817`, QR SVG decorativo y cuenta atrás simulada; no llama a `GET /api/v1/sesiones/{id}/qr-token` (existe en el catálogo).
   Un docente podría creer que es un código de acceso real. `sessionQrEnabled=false` solo oculta el QR de otras pantallas.
3. **Bandera obsoleta `teacherClaimsEnabled=false`** ("backend responde 501"): el backend ya sirve `docente/reclamos` (200, 7
   reclamos del docente), pero el diálogo "Reclamos" del grupo siempre muestra 0 y el contador de pendientes queda en 0; la
   bandeja completa sí funciona.
4. **"Mis Grupos"/"Mis Horarios" dependen de `docente/horarios`**, que es vacío porque el grupo no tiene franjas: "Estudiantes
   Registrados 0" (el grupo tiene 7), "0 Horas / Semana" y, en "Info & Alumnos", HORARIO/AULA/DOCENTE TITULAR vacíos.
   Se corrige asignando horario al grupo (flujo F1 del coordinador) o derivando el conteo de `grupos/{id}/estudiantes`.
5. `GET docente/grupos/{grupoAjeno}/solicitudes-ingreso` responde 200 con lista vacía en vez de 403 (no filtra datos; la
   respuesta es inconsistente con el resto de recursos del grupo, que sí dan 403).

## L. Lado ESTUDIANTE (2026-11-02, sesión real en el navegador integrado: "Estudiante UCO" = Carlos Andres Zapata Gomez)

Menú: Panel, Mis Horarios, Mis Materias, Mis Reclamos, Oferta de Grupos, Solicitudes de Ingreso, Perfil. Las 7 pantallas
renderizan y todas sus llamadas responden 200 (`estudiante/horarios|materias|reclamos|oferta-academica|solicitudes-matricula`,
`estudiante/materias/{idAsignatura}/sesiones`, `usuarios/perfil`).

| Prueba (sin crear ni modificar datos) | Resultado |
|---|---|
| Mis Materias → "Ver Detalle" (2 sesiones, solicitud de revisión aprobada, "Volver") | PASS |
| Mis Reclamos: 5 reclamos; filtros Todos / Pendientes (0) / Aprobadas (3) / Rechazadas (2) | PASS |
| "Auto-Registro Asistencia" con PIN inválido | PASS: `POST estudiante/asistencia-qr` 400 → "No fue posible completar la operación", sin registrar asistencia |
| "Solicitar ingreso" con un grupo inexistente | PASS: error controlado "…no fue encontrado en el sistema"; no se creó solicitud |
| "Matricularse con Código" con código falso | error controlado "Esta funcionalidad no está disponible por el momento" (`POST estudiante/matricular-grupo` → 501, ver M.3) |
| Renovación silenciosa del token a mitad de una acción | PASS: vuelve a la misma ruta con rol ESTUDIANTE |

API real con `uco-estudiante`: `/auth/me` roles=[ESTUDIANTE]; todos los GET propios → 200; escrituras con cuerpo vacío:
`asistencia-qr` 400, `solicitudes-matricula` 400, `…/{id}/retiro` (inexistente) 404, `asistencias/revisiones` 400. Aislamiento:
`grupos/{id}/estudiantes|asistencias`, `v2/sesiones/grupo/{id}`, `estudiantes`, `docentes`, `grupos` → 403;
`configuracion/registro-estudiante` → 403 (solo COORDINADOR/DOCENTE, correcto). DOCENTE, COORDINADOR, DECANO y ADMINISTRADOR
→ 403 en `estudiante/*` (lectura y escritura). Bearer inválido → 401.

## M. Hallazgos del lado ESTUDIANTE (preexistentes, no regresiones de 0.1.8)

1. **`POST /api/v1/archivos/subir` con un cuerpo que no es multipart responde 500 `INTERNAL_ERROR`.** El log muestra
   `MultipartException: Current request is not a multipart request` sin tratar en `GlobalExceptionHandler`; debería ser 400/415.
   Efecto: un cliente mal formado dispara un error de servidor (y un ERROR con stack en logs). No expone datos.
2. **Perfil: "Documento: Sin registrar"** aunque `GET /usuarios/perfil` devuelve `numeroIdentificacion` (1017223344 en la semilla,
   y el docente lo ve en el padrón). Causa: `features.userProfileEndpointEnabled` (`USER_PROFILE_ENDPOINT_ENABLED`) está apagado por
   defecto con el comentario obsoleto "responde 501"; `AuthService` no fusiona el perfil de la DB. Misma clase de bandera
   obsoleta que `teacherClaimsEnabled` (sección K.3).
3. **"Matricularse con Código" (Mis Materias) llama a `POST /estudiante/matricular-grupo`, que el backend no implementa (501).**
   El botón sigue visible y falla con un mensaje claro, sin romper. La vía que sí funciona es "Solicitudes de Ingreso"
   (`POST estudiante/solicitudes-matricula`). Igual, `POST/DELETE estudiante/reclamos` son 501; la UI crea reclamos con
   `POST asistencias/revisiones` (validación 400, implementado).
4. **Hora de las sesiones: tercera presentación.** El estudiante ve "13:00 - 15:00 (sin zona)" (UTC etiquetado como "sin zona"),
   el coordinador 13:00 sin etiqueta y el docente 08:00 (America/Bogota). Misma causa que K.1: la API v1 devuelve fecha sin zona
   y el reclamo v2 trae `fechaSesionUtc` con `Z`.
5. **Datos vacíos o "No informado"**: horario ("No hay clases registradas"), docente, aula y horario asignado salen vacíos o
   "No informado" porque el grupo no tiene franjas (K.4); la asistencia acumulada/promedio muestra "No disponible – el sistema aún
   no expone tu asistencia consolidada" (limitación declarada; el estudiante sí tiene registros: 1 justificada y otro en estado
   "no disponible").
6. **Oferta de Grupos vacía** ("No hay grupos ofertados para tus programas") con `oferta-academica` → `[]`, aunque en el mismo
   programa existen otros grupos habilitados (p. ej. FIX-PAG-300, 276/300). Falta confirmar con negocio/DB si debería ofertarlos
   (regla de elegibilidad, período vigente o grupos de fixture); no se puede concluir desde la interfaz.

## N. Lado ADMINISTRADOR (2026-11-02, sesión real en el navegador integrado: "Administrador UCO")

Menú: Panel, Facultades, Indicadores, Decanos, Sedes, Áreas, Períodos, Sistema, Seguridad, Perfil. Las 10 pantallas renderizan y sus
llamadas responden 200 (`admin/indicadores|tendencia-ausentismo|comparativa-facultades|facultades|decanos|instituciones|areas|parametros`,
`coordinador/periodos-academicos`, `docentes`, `usuarios/perfil`), salvo `admin/sedes` y `admin/auditoria` → 501 (ver O.2).

| Prueba (sin guardar nada) | Resultado |
|---|---|
| Formularios de alta: Nueva Facultad (inline), Nueva Sede, Nueva área (modal, código 3 caracteres/nombre 3-50), Nombrar Decano Existente (5 coincidencias), Registrar Nuevo Decano | PASS (abren; no se enviaron) |
| Sección Seguridad (Security UI, Shadow DOM): Resumen, Personas 51, Roles 6, Perfiles 5, Recursos 161, Administradores 2 | PASS: `security/summary|roles|profiles|resources|administrators` → 200 contra el PDP con la sesión BFF |
| Renovación silenciosa del token a mitad de un recorrido | PASS: vuelve a la misma ruta con rol ADMINISTRADOR |
| Botón "Guardar" de un parámetro global | NO ejecutado: el clasificador de permisos bloqueó la acción (escritura sobre configuración real); se evaluó con código y con sondas de API (O.1) |

API real con `uco-administrador`: `/auth/me` roles=[ADMINISTRADOR]; 19 GET (16 → 200; `sedes`, `espacios-fisicos`, `auditoria` → 501).
Escrituras con cuerpo vacío sobre IDs inexistentes: áreas, facultades, decanos (POST), tipos-programa y períodos (POST/PUT) → 400;
DELETE período y `cierre-masivo` → 404; sin mutar datos. DECANO, COORDINADOR, DOCENTE y ESTUDIANTE → 403 en `admin/*` (GET y
escrituras); Bearer inválido → 401.

## O. Hallazgos del lado ADMINISTRADOR (preexistentes salvo O.4)

1. **Parámetros globales: "Guardar" no puede funcionar y avisa mal.** El servicio del frontend envía `PATCH /admin/parametros/{id}`
   (`admin-management.service.ts`, sin manejo de error propio); el backend/OpenAPI/catálogo solo declaran `PUT /admin/parametros/{id}`,
   y ese `PUT` responde 501 `FEATURE_UNAVAILABLE`. El `PATCH` no está en el catálogo y el PEP lo bloquea con 403 (fail-closed correcto);
   el interceptor global lo traduce a "Acceso denegado: No cuenta con los permisos necesarios…", engañoso porque el administrador sí
   tiene permiso. El valor editado queda en pantalla sin guardarse. Afecta a los 34 parámetros (p. ej. `ESTADO_ADJUNTO`, `TAMANIO_MAXIMO_BYTES`,
   `DIAS_LIMITE_JUSTIFICACION`).
2. **Funciones de administración no implementadas (501 `FEATURE_UNAVAILABLE`)** con pantalla/botón visible: sedes (GET/POST/PUT/PATCH),
   espacios físicos (GET/POST/PUT), instituciones (POST/PUT/PATCH toggle), `admin/auditoria` (GET), `PATCH admin/decanos/{id}/toggle`
   (botón "Desactivar" de Decanos) y `PUT admin/parametros/{id}`. Efectos en la UI: Sedes muestra "Total sedes 0 / No se encontraron
   sedes. Prueba con otros términos de búsqueda" (parece vacío, en realidad no disponible), "Trazabilidad y Auditoría (0)" y la consola
   registra un ERROR 501 en cada visita a Áreas y Sedes/Sistema/Decano-infraestructura. "Desactivar" decano no se pulsó.
3. **Cierre masivo y simulador**: `POST admin/cierre-masivo` con cuerpo vacío → 404 `RESOURCE_NOT_FOUND` (no 400); no se ejecutó el
   simulador ni el cierre real (operación masiva sobre datos reales).
4. **SECURITY_FINDING (PDP 0.1.8, para Seguridad UCO).** Mejora respecto a 0.1.7: las rutas administrativas por aplicación
   (`security/summary|roles|administrators|resources`) ya responden **403 `NOT_AUTHORIZED_TO_ADMINISTER`** a DECANO, COORDINADOR, DOCENTE y
   ESTUDIANTE (solo el administrador de la aplicación obtiene 200). Residual: `GET /api/v1/users` y `GET /api/v1/applications` siguen
   respondiendo **200 a cualquier Bearer con audiencia del PDP**; con un token de ESTUDIANTE se listan los 28 usuarios del inquilino con
   `name`, `email`, `lastLoginAt`, `provider` y `tenantId`, y las 2 aplicaciones con `baseUrl`. Debe tratarse como exposición de datos
   personales entre roles. `docs/security/PDP_ADMIN_API_AUTHORIZATION_FINDING.md` describe el estado 0.1.7 y debe actualizarse con este
   resultado antes de cerrar.
