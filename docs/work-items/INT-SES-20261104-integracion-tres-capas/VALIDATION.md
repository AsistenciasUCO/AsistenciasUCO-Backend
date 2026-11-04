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

## 9. Segunda jornada (2026-11-04, tras la autorización del owner DB comunicada por el usuario)

### 9.1 SQL Server — paquete B y pruebas SQL en el entorno autorizado

Paquete B aplicado y verificado: ver [DB_DEPLOY_PLAN.md](DB_DEPLOY_PLAN.md). Pruebas ejecutadas con `sqlcmd` dentro del
contenedor contra `gestionasistenciadb` (cada suite en una transacción con `ROLLBACK` final):

| Suite | Resultado |
|---|---|
| `test_ses01_ventana_asistencia.sql` | 4 `TEST_PASS` + 8 rechazos contractuales con el mensaje esperado (`SES_007` ×2, `SES_008` ×4, `SES_009` ×2) |
| `test_ses04_reclamo_justificada.sql` | 11 `TEST_PASS` (aprobar SJC→EX, auditoría, idempotencia, contradicción, rechazo conserva, no titular, sin detalle, presente, tras cierre) |
| `test_usp_precargar_sesiones_grupo.sql` | 16 `TEST_PASS` (creación, idempotencia, conserva asistencia/clasificación/historia, regeneración, ámbito, 8/12/16/18 semanas, mínimo privilegio) |
| `test_usp_precargar_sesiones_concurrency.ps1` | NOT_RUN aquí (escribe y limpia; el estándar DB prohíbe pruebas mutantes en esta instancia): cubierta por el CI de PR #83 |

Residuos: conteo + checksum de 10 tablas idénticos antes/después (incluida `AuditoriaEvento`).

### 9.2 PDP del laboratorio

`provision_asistencias.py verify` → faltaban 8 recursos y 13 concesiones del `access-catalog.json` de develop
(ADMINISTRADOR 7, DECANO 4, DOCENTE 2: `GET /docente/oferta-creacion-grupos`, `POST /grupos`). `pdp` (sin `--prune`) las
agregó; `verify` → 0 faltantes (queda una concesión ESTUDIANTE preexistente sobre un recurso sin endpoint, que no abre
acceso). `verify-decisions` → **1021 decisiones reales PEP→PDP, 0 fallos, `clean: true`** (ALLOW solo a los roles del
catálogo, DENY al resto).

### 9.3 Identidad de prueba `ana.gomez@uco.edu.co`

Existía en `dbo.Usuario` (`e1f2a3b4-…-0005`, estudiante del grupo 001) pero no en Keycloak. El modo efectivo es
**baseline**: realm `security-baseline` (19090), no `asistencias-uco` (legacy, inactivo). Se reutilizó la lógica oficial
`provision_asistencias.provision_users` restringida a Ana ([script](scripts/provision-identidad-estudiante-ana.py)), tras
comprobar que no existía identidad con ese username, correo ni `idUsuario`. Resultado: usuario con `idUsuario` correcto,
rol cliente `ESTUDIANTE` en `asistencias-api`, sesión BFF y perfil ESTUDIANTE en el PDP; contraseña aleatoria en
`lab.env` (fuera de Git, nunca impresa). JWT real: `aud` incluye `asistencias-api`, `idUsuario` correcto, sin roles de
realm. Aislamiento: `/auth/me` 200 con su id; solo su grupo y sus 2 reclamos; soporte de Carlos 404; rutas docente y
coordinador 403.

### 9.4 Correcciones publicadas (RED → GREEN → regresión)

| Defecto | Capa | Rama | Validación |
|---|---|---|---|
| Portal estudiante mostraba la hora UTC «sin zona» (APP-04) | BE + FE | `fix/app04-estudiante-utc-conteo-docente` (BE `ecf30291`, FE `77c9dbf`) | BE verify 3150/3150; FE verify 1504/1504 |
| «Estudiantes Registrados: 0» en grupos sin horario | BE + FE | misma rama | idem |

### 9.5 E2E navegador (segunda pasada)

Registros de prueba creados (dev): reclamos `b5973aa4…` (Carlos, aprobado) y `a64702f0…` (Ana, rechazado) sobre las filas
`SJC` de la sesión 3 del grupo 001; grupo **9106 «E2E-SES02 docente 20261104»** (6 sesiones). Se conservan como evidencia.

| Escenario | Resultado | Evidencia |
|---|---|---|
| APP-04 portal estudiante (Carlos y Ana) | PASS | «08:00 - 10:00»; wire `fechaHoraInicioUtc: …Z`, `estadoTemporal: CONFIRMADA`, v1 sin cambios |
| Estudiantes Registrados (docente) | PASS | 7 (grupo 001: 7 inscritos) |
| SES-04 radicar (Carlos, Ana) | PASS | «Reclamo enviado.»; estado `P` en DB |
| SES-04 bandeja docente | PASS | «2 Pendientes por Resolver» |
| SES-04 aprobar con doble clic | PASS | 1 solo `PATCH` 200; reclamo `A`; asistencia **`EX`, `asistio=0`**, 1 detalle; auditoría P→A, SJC→EX, actor docente |
| SES-04 misma decisión repetida | PASS | 200 sin nueva auditoría |
| SES-04 decisión contraria | PASS | 409 `ERR_RECLAMO_YA_RESUELTO` |
| SES-04 reclamo inexistente | PASS | 404 `ERR_RECLAMO_NO_EXISTE` (antes 400 con el SP viejo) |
| SES-04 rechazar (Ana) | PASS | `R`; asistencia original intacta (checksum idéntico) |
| SES-04 duplicados | PASS | 1 evento de auditoría por reclamo; `Asistencia`/`DetalleAsistencia` 6361 sin cambio |
| SES-05 tras aprobación | PASS | sesión 3 «0 / 7 · 1 justificada»; promedio 33 %→38 % por la fórmula vigente (decisión pendiente) |
| SES-02 docente: oferta de creación | PASS | 200 con ARQ-402 del programa (antes 403 del PEP) |
| SES-02 docente crea grupo con horario Viernes 18–20 | PASS | titular = la docente; 6 sesiones viernes 06/11–11/12, 23:00 UTC, `UTC_GENERADOR` |
| SES-01 escritura en sesión activa (E2E) | NOT_RUN | sin sesión activa con estudiantes de prueba; cubierto por `SES01_BULK_DURING` en SQL |
| SES-04 docente ajeno (E2E) | NOT_RUN | sin cuenta de laboratorio para otro docente; cubierto por `SES04_NON_OWNER` en SQL |

## 10. Clasificación final

| Corrección | Estado |
|---|---|
| APP-04 | **VALIDADO E2E** en docente, coordinador y estudiante (Bogotá/Londres; estudiante corregido en la rama `fix/app04-…`) |
| SES-01 | **VALIDADO E2E** (UI + Backend 409/403/404/400, concurrencia, F5) y barrera SQL **VALIDADA en el entorno integrado** (pruebas SQL con `SES_007/008/009`) |
| SES-02 | **VALIDADO E2E** vía coordinador y vía docente (creación, idempotencia, cambio futuro, UTC, cantidad real); conservación de sesiones con asistencia validada por SQL |
| SES-03 | **BLOQUEADO POR AUTORIZACIÓN** + **PENDIENTE DE DECISIÓN FUNCIONAL** ([propuesta](SES03_PROPUESTA.md)) |
| SES-04 | **VALIDADO E2E** (radicar, aprobar → EX/asistio=0/JUSTIFICADA, rechazar, idempotencia, conflicto 409, 404, auditoría, doble clic) |
| SES-05 | Columna Asistentes **VALIDADO E2E**; fórmula del promedio con EX **PENDIENTE DE DECISIÓN FUNCIONAL** |

Pendientes reales: constancia formal de la aprobación del owner DB para PR #81/#82/#83; decisión académica sobre EX en
el porcentaje; decisiones de SES-03; corrección de datos semilla anómalos (7 `SJC` en la sesión futura 3, `EX` con
`asistio=1`, reclamos contradictorios en `7f8e03d1`) — requiere decisión explícita, no se modificaron.
