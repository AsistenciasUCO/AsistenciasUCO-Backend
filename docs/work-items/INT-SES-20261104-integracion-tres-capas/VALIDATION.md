---
status: active
type: evidence
scope: integration
owner: backend-team
last-reviewed: 2026-11-04
---

# Integración SES-01/02/04 + APP-04 en las tres capas — evidencia (2026-11-04)

Alcance: integración técnica sobre los `develop` vigentes; sin merges automáticos; MessageUcoLab **no** se inició.
Plan de alineación SQL: [DB_DEPLOY_PLAN.md](DB_DEPLOY_PLAN.md). Propuesta SES-03: [SES03_PROPUESTA.md](SES03_PROPUESTA.md).

## 1. Sincronización

| Capa | develop | Método | Notas |
|---|---|---|---|
| Backend `alejovh12/AsistenciasUCO` | `1959d431` | fast-forward (25 commits) | `tmp/ses01-ses04-combinado` solo tenía merges ya contenidos en develop |
| Frontend `shesho22/AsistenciasUCO-Frontend` | `f0cfe5d` | fast-forward (4) | |
| DB `johnjduque/gestion-asistencia-db` | `11d7d50` | fast-forward (4) | `9656de5` (docs) sigue solo en la rama feature |

## 2. Hallazgos de entorno (no de código)

- **Backend no compilaba sin credenciales**: `messageucolab-spring-boot-starter:0.1.1` vive en GitHub Packages (401).
  Se creó `~/.m2/settings.xml` (servidor `github`) que lee `MESSAGEUCOLAB_PACKAGES_USER/TOKEN` del entorno, los mismos
  nombres del CI; el PAT lo configuró el usuario. Sin cambios al pom ni al componente de mensajes.
- **Frontend no compilaba**: faltaba instalar `minifront-mensajes` (dep. de MSG-002, pública, fijada por commit en el lockfile).
  `npm install` sin cambios en `package-lock.json`.
- **MessageUcoLab no es requisito de arranque**: el `.env` local usa `APP_ADAPTERS_MESSAGE_CATALOG_PROVIDER=sqlserver`
  (provider oficial); el adapter MessageUcoLab es `@ConditionalOnProperty(provider=messageucolab)`. El proxy
  `/messageucolab` de `ng serve` apunta a un gateway no iniciado: solo afecta a la administración de mensajes.

## 3. Datos académicos (solo lectura)

0 sesiones duplicadas/solapadas/sin grupo, 0 asistencias huérfanas/duplicadas/de otro grupo, 0 reclamos huérfanos.
Anomalías (todas en fixtures/seed, **no corregidas**, requieren decisión):

| Hallazgo | Registros | Origen |
|---|---|---|
| Sesión fuera del período (2040-01-15) | `8244100e…` (FIX-PAG-300, SES-05) | fixture |
| `procedenciaTemporal` NULL → `INDETERMINADA` (SES_009) | `3eab8ff3…`, `8244100e…` | fixture |
| `EX` con `asistio = 1` (resolución previa a SES-04) | asistencia `7f8e03d1…` | seed grupo 001 |
| `SJC` con `asistio = 1` | asistencia `fb714bb8…` | seed grupo 001 |
| 4 reclamos sobre la misma asistencia (2 estados distintos) | asistencia `7f8e03d1…` | seed grupo 001 |

## 4. Pruebas automatizadas

| Suite | Resultado |
|---|---|
| Frontend develop `npm run verify` | 1500/1500, realtime coverage PASSED, build prod OK |
| Frontend `fix/ses05-ex-no-presencial` `npm run verify` | 1504/1504 (RED 4 → GREEN), stmts 91.72 % / branches 77.11 %, build prod OK |
| Backend develop `1959d431` `mvnw -B -ntp verify` | 3144/3144, 0 fallos/errores/skips, ArchUnit OK, «All coverage checks have been met» |
| Backend `-Pintegration` | NOT_RUN en esta fase |
| DB | CI oficial del PR #83 (`2c54f31`): 429 PASS / 0 fallos. Suite local contra `sql_server_asistencias`: NOT_RUN (política) |

## 5. Entorno E2E

Backend `1959d431` en 8080 con `start-backend-baseline.ps1 -Provider baseline` (health UP, 401 sin token, CORS 4200 OK,
verificador de esquema UTC v2 OK, catálogo de mensajes `sqlserver`); SPA (develop + `fix/ses05`) en 4200 (`ng serve`);
SQL 1433, Keycloak 19090, PEP 18081, PDP 18080, MinIO 9000, ClamAV 3310 ya activos, sin reinicios. MessageUcoLab: no iniciado.
`JAVA_TOOL_OPTIONS=-Djdk.net.hosts.file=<copia regenerada>` (las IP Azure del `jvm-hosts.txt` del laboratorio cambiaron).

## 6. E2E navegador — DOCENTE (`maria.rostagno@uco.edu.co`, grupo 001)

Login manual del usuario. Escrituras API dirigidas con el Bearer capturado en la página (nunca expuesto).

| Escenario | Resultado | Evidencia |
|---|---|---|
| Grupos asignados | PASS | `/docente/asignaturas` 200: grupo 001 |
| Sesiones reales (no 16) | PASS | 3 sesiones = 3 filas `Sesion` |
| Horario local Bogotá | PASS | 13:00–15:00 UTC → 08:00–10:00 |
| APP-04 Europe/London (preferencia, retirada al final) | PASS | agosto 14:00–16:00 (BST), diciembre 13:00–15:00 (GMT) |
| SES-01 UI sesión cerrada / futura | PASS | aviso contractual; 0 controles de escritura habilitados; F5 no reabre |
| SES-01 API cerrada | PASS | 409 `ERR_SESION_CERRADA` |
| SES-01 API futura | PASS | 409 `ERR_SESION_NO_INICIADA` |
| SES-01 API sesión de otro docente (incl. indeterminada) | PASS | 403 `ERR_DOCENTE_SIN_TITULARIDAD_SESION` (403 antes que 409) |
| SES-01 API inexistente / inválida | PASS | 404 `ERR_SESION_NO_EXISTE`; 400 `ERR_ESTADO_ASISTENCIA_INVALIDO`, `ERR_SESION_ASISTENCIA_REQUERIDA` |
| SES-01 5 POST concurrentes a sesión cerrada | PASS | 5 × 409; checksum de `DetalleAsistencia` idéntico antes/después |
| SES-01 sesión activa → registro permitido | NOT_RUN | no hay sesión activa; no se crean sesiones en datos semilla |
| SES-01 barrera SQL (`SES_007/008/009`) | PENDIENTE DE DESPLIEGUE | paquete B |
| SES-05 columna Asistentes | PASS | sesiones 1 y 2: «0 / 7 · 1 justificada» |
| SES-04 bandeja docente | PASS | 7 reclamos = 7 filas; «0 Pendientes» |
| SES-04 acción inválida / sin acción | PASS | 400 `ERR_ACCION_REVISION_INVALIDA` / `ERR_ACCION_REVISION_REQUERIDA` |
| SES-04 reclamo inexistente → 404 | FAIL (DB vieja) | SP vivo pre-SES-04 lanza mensaje mal formado → 400 `VALIDATION_ERROR`; con paquete B → `REV_005` |
| SES-04 aprobar/rechazar/idempotencia/conflicto | PENDIENTE DE DESPLIEGUE | sin reclamos PENDIENTE y SP vivo pre-SES-04 |
| SES-02 crear grupo con horario (F3+F2) | BLOQUEADO POR AUTORIZACIÓN | `GET /docente/oferta-creacion-grupos` 403 del PEP: el PDP del laboratorio no tiene 8 recursos del catálogo develop; `provision_asistencias.py pdp` bloqueado por la política de permisos del agente |

## 7. E2E navegador — ESTUDIANTE (`carlos.zapata@uco.edu.co`)

| Escenario | Resultado | Evidencia |
|---|---|---|
| Login, materias inscritas | PASS | 1 materia (grupo 001) |
| Horario semanal | PASS | vacío = grupo 001 sin filas `Horario` |
| Sesiones visibles | PASS | 3 sesiones del grupo |
| Estado JUSTIFICADA tras reclamo aprobado (SES-04) | PASS | sesión 2: `EX`/`asistio=0` → «JUSTIFICADA», «Solicitud de revisión: aprobada» |
| Mis reclamos con resultado y respuesta | PASS | 5/5 propios (los 2 de Ana no aparecen), soporte descargable |
| Soporte propio / ajeno | PASS | 200 / 404 `RESOURCE_NOT_FOUND` (no revela existencia) |
| Rutas de DOCENTE y registro de asistencia | PASS | 403 |
| Grupo no matriculado | PASS | 200 con 0 sesiones |
| Horas de sesión del estudiante en zona local (APP-04) | PENDIENTE | muestra «13:00 - 15:00 (sin zona)»: el endpoint del estudiante no expone UTC/procedencia (bloque estudiante de APP-04 aún no implementado) |

Hallazgos de datos (estudiante): sesión 1 «Estado no disponible» porque la fila heredada es `EX` con `asistio=1`;
**sesión 3 futura (2026-12-09) tiene 7 asistencias `SJC`** (registradas antes de SES-01): el estudiante ve AUSENTE y
el botón «Radicar Reclamo» para una clase no dictada. Propuesta: borrar esas 7 filas de prueba de la sesión futura
o reclasificarlas, con aprobación del owner DB (no se modificó nada).

## 8. E2E navegador — COORDINADOR (`coordinador.sistemas@uco.edu.co`)

Registro de prueba creado (dev, etiquetado): grupo **9105 «E2E-SES02 integracion 20261104»** (`d0c1b037-1db6-470b-ab65-16ee0a7ad309`),
docente Maria Rostagno, sin estudiantes. Se conserva como evidencia; retirarlo requiere decisión del owner DB.

| Escenario | Resultado | Evidencia |
|---|---|---|
| Docentes del programa | PASS | 4 docentes y grupos asignados = DB |
| Grupos y horarios | PASS | 5 grupos y franjas = filas `Horario` |
| SES-02 crear grupo con horario Martes 18:00–20:00 | PASS | «Grupo guardado.»; `Sesion` 17 → 23: 6 martes 10/11–15/12 (fin del período), 23:00 UTC, `UTC_GENERADOR`; cantidad variable, no 16 |
| SES-02 idempotencia (guardar sin cambios) | PASS | 6 sesiones, mismos id (checksum idéntico) |
| SES-02 cambio de horario futuro → Jueves | PASS | 6 sesiones movidas a jueves 05/11–10/12; total 23 (sin duplicados) |
| Sesiones programadas en Angular | PASS | «Sesiones de E2E-SES02»: 6 filas, 18:00–20:00 «America/Bogota (UTC-05:00)» |
| Permisos: registrar asistencia / resolver reclamo / portal estudiante | PASS | 403 en los tres |
| SES-02 conservación de sesiones manuales/históricas/con asistencia | CI | cubierto por la suite DB de PR #83; sin casos E2E (requiere sesiones con asistencia en el grupo de prueba) |
| SES-02 precarga desde el DOCENTE (F3) | BLOQUEADO POR AUTORIZACIÓN | PDP del laboratorio sin el recurso; ver §6 |

## 9. Clasificación final

| Corrección | Estado |
|---|---|
| APP-04 | **VALIDADO E2E** docente y coordinador (Bogotá/Londres); bloque estudiante **PENDIENTE** (endpoint sin UTC/procedencia) |
| SES-01 | **VALIDADO E2E** en UI + Backend (409/403/404/400, concurrencia, F5, sin mutación); barrera SQL **PENDIENTE DE DESPLIEGUE** (paquete B); sesión activa NOT_RUN |
| SES-02 | **VALIDADO E2E** vía coordinador (creación, idempotencia, cambio futuro, UTC, cantidad real); guardia de rango `c98ab41` **PENDIENTE DE DESPLIEGUE**; vía docente **BLOQUEADO POR AUTORIZACIÓN** (PDP) |
| SES-03 | **BLOQUEADO POR AUTORIZACIÓN** + **PENDIENTE DE DECISIÓN FUNCIONAL** ([propuesta](SES03_PROPUESTA.md)) |
| SES-04 | Lectura y visibilidad **VALIDADO E2E** (JUSTIFICADA al estudiante, bandeja docente, 400); aprobar/rechazar/idempotencia/conflicto/404 **PENDIENTE DE DESPLIEGUE** (paquete B, sin aprobación del owner DB) |
| SES-05 | Columna Asistentes **IMPLEMENTADO Y VALIDADO** (FE 1504/1504 + E2E); fórmula del promedio con EX **PENDIENTE DE DECISIÓN FUNCIONAL** |

Hallazgos adicionales: «Estudiantes Registrados: 0» en el portal docente (la respuesta `/docente/asignaturas` no
trae conteo; el grupo tiene 7) — fuera del alcance SES. Datos semilla heredados: REC-F2447234 APROBADA con asistencia
`SJC`/`asistio=1`; asistencia `7f8e03d1` con reclamos APROBADA y RECHAZADA.
