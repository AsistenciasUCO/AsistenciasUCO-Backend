---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-21
---

# VALIDATION — HU035 + HU036: Bandeja y detalle de solicitudes de revisión del docente

> Este documento se completa por capa. **Sección vigente: Base de Datos.** Backend y Frontend: `NOT_RUN` (fuera de esta fase).

## 1. Base de Datos (`GestioAsistenciaDB`, rama `feature/HU035-HU036-bandeja-detalle-reclamos-docente`)

### 1.1 Baseline (antes de cambios)

| Campo | Valor |
|---|---|
| Comando | `.\test_summary.ps1 -ContainerName "sql_server_asistencias"` (contenedor `sql_server_asistencias`, BD `gestionasistenciadb`) |
| Exit code | `1` (`DB GATE INCOMPLETE`) |
| Causa | `test_contract_closure` falla con `TEST FAILED: PUBLIC_CANONICAL_RESULTSET usp_consultar_asignaturas_paginado canonical_columns=13` (Msg 52009). `sqlcmd -b` aborta el lote en ese punto, por lo que el resto de la suite (incluidos tests posteriores) no se ejecuta y el resumen reporta `FAILED=24`, `CRITICAL_MISSING=203`, `SQL_ERROR_COUNT=1`. |
| Relación con HU035/HU036 | Ninguna: el fallo afecta a `usp_consultar_asignaturas_paginado` y existe antes de tocar el repositorio (BD local posiblemente desalineada del repositorio). No se corrige aquí. |
| Salida completa | Capturada localmente (scratchpad de la sesión); no versionada. |

Consecuencia: la suite completa **no puede** demostrar regresión cero en este entorno; la regresión se evalúa comparando contra este baseline y ejecutando por separado los tests relacionados (ver 1.4).

### 1.2 RED_SNAPSHOT

| Campo | Valor |
|---|---|
| Base commit (DB) | `585f7f07180569b3b83a3b34711c6a29147a25b5` (`develop`) |
| Archivo de test | `GestioAsistenciaDB/test/test_usp_consultar_detalle_solicitud_revision_docente.sql` |
| SHA-256 | `EDD7E1B118CC9309CA5C0CA553FCA3A41692FDBAE369DFD21783A415D18C84FB` |
| Comando | `docker cp` del archivo al contenedor y `docker exec sql_server_asistencias /opt/mssql-tools18/bin/sqlcmd -S localhost -d gestionasistenciadb -U sa -P <pwd> -C -b -h -1 -i /tmp/hu35_test.sql` |
| Exit code | `1` |
| Fallo observado | `Msg 2812, Level 16, State 62, Line 71: Could not find stored procedure 'dbo.usp_consultar_detalle_solicitud_revision_docente'.` |
| Fallo esperado (TEST_PLAN) | `Could not find stored procedure 'dbo.usp_consultar_detalle_solicitud_revision_docente'` — coincide. |

El fixture (sesión, asistencias, detalle `SJC`, dos solicitudes, soporte `ATTACHED`) se insertó correctamente antes del `EXEC`; el fallo es exclusivamente por la ausencia del SP.

Nota de diseño de test (DB-07): `CK_ArchivoSoporte_EstadoVinculo` impide vincular a una solicitud un archivo cuyo estado no sea `ATTACHED`, de modo que un soporte `DRAFT` no puede coexistir con una solicitud. El caso DB-07 se implementa cambiando, dentro de la transacción de fixture (con `ROLLBACK`), el parámetro `ARCHIVO.ESTADO_ADJUNTO`; el test comprueba además que el parámetro no quedó modificado tras el `ROLLBACK`.

### 1.3 GREEN

| Paso | Comando | Resultado |
|---|---|---|
| Despliegue seed | `docker cp` de `16_hu035_parametro_estado_adjunto.sql` + `sqlcmd -b -i` (dos ejecuciones) | Exit `0` ambas; `(1 rows affected)`; `COUNT(*)` de `ARCHIVO.ESTADO_ADJUNTO` = `1` (idempotente) |
| Despliegue SP | `docker cp` de `usp_consultar_detalle_solicitud_revision_docente.sql` + `sqlcmd -b -i` | Exit `0` |
| Test | mismo comando del RED sobre el mismo archivo | Exit `0`; `TEST_PASS:HU035_HU036_DB01_DETALLE_TITULAR`, `..._DB02_DB03_DB04_AISLAMIENTO`, `..._DB05_PARAMETROS_NULOS`, `..._DB06_SIN_DETALLE_NI_SOPORTE`, `..._DB07_ESTADO_ADJUNTO_PARAMETRIZADO`, `..._DB08_TRANCOUNT_ZERO` |
| Integridad del test | `Get-FileHash -Algorithm SHA256` tras GREEN | `EDD7E1B118CC9309CA5C0CA553FCA3A41692FDBAE369DFD21783A415D18C84FB` (idéntico al RED; el test no se modificó) |

GREEN obtenido en la primera ejecución tras desplegar seed y SP. El test cubre además el orden y los nombres exactos de columnas del resultset (`sys.dm_exec_describe_first_result_set_for_object`), conforme a CONTRACT 2.1.

### 1.4 Regresión

| Verificación | Resultado |
|---|---|
| `.\test_summary.ps1` tras los cambios | Exit `1` (igual que el baseline). `PASSED=42`, `FAILED=24`, `CRITICAL_MISSING=203`, `SQL_ERROR_COUNT=1`, `PUBLIC_SP_MISSING=0`: **idénticos al baseline**. Misma causa de aborto (`PUBLIC_CANONICAL_RESULTSET usp_consultar_asignaturas_paginado`). Única diferencia en la salida: `REPO_PUBLIC_SP_COUNT` 37 → 38 e `INSTANCE_PUBLIC_SP_COUNT` 55 → 56 (el SP nuevo existe en repositorio y en instancia). |
| Test de HU035/HU036 dentro de `test_suite.sql` | `NOT_RUN` como parte de la suite: el lote aborta en `test_contract_closure` (baseline rojo) antes de llegar a la línea `:r` nueva. Se ejecutó de forma independiente (1.3). |
| `test/test_lb004d_support_binding.sql` (R2 no modificado), ejecución independiente | Exit `1`: `TEST FAILED: LB004D_W2_NOT_ENROLLED`. Falla en un caso de radicación W2, que no usa los objetos de este work item (solo se agregaron un SP nuevo y una fila de parámetro). **No se verificó contra un baseline previo** (no se ejecutó antes de los cambios); se reporta como hallazgo sin confirmar su preexistencia. |
| Archivos prohibidos | Sin cambios en `schema/tables/`, SP/vistas/seeds existentes, `docs/contracts/` ni manifiestos LB-004D. |

### 1.5 Desviaciones y pendientes

- Baseline de la BD local rojo por causas ajenas (ver 1.1); regresión completa de la suite no demostrable en este entorno hasta alinear la BD con el repositorio.
- DB-07 implementado mediante cambio transaccional del parámetro (ver nota en 1.2) porque la restricción `CK_ArchivoSoporte_EstadoVinculo` impide el escenario "soporte DRAFT vinculado a solicitud".
- La captura completa de salidas (baseline y posterior) se guardó en el scratchpad de la sesión, no se versiona.
- Backend y Frontend: `NOT_RUN`.

## Auditoría DB (fase 05) — agente principal, 2026-10-21

| Comprobación | Resultado |
|---|---|
| SP vs CONTRACT 2.1 (parámetros, columnas, orden, LEFT JOIN, titularidad, solo lectura, sin literales) | PASS (revisión manual de `usp_consultar_detalle_solicitud_revision_docente.sql`) |
| Seed `16_hu035_parametro_estado_adjunto.sql` idempotente (MERGE) y sin tocar archivos del manifiesto LB-004D | PASS |
| `test_suite.sql`: solo línea `:r` agregada | PASS (`git diff`) |
| SHA-256 del test tras GREEN = RED (`EDD7E1B1…84FB`) | PASS — test no modificado después de RED |
| Re-ejecución independiente del test | PASS, exit 0, 6 `TEST_PASS` |
| DB-07 implementado vía cambio transaccional del parámetro | ACEPTADO: la restricción `CK_ArchivoSoporte_EstadoVinculo` hace imposible el escenario literal; el test sigue refutando un SP que ignore el estado del soporte |
| Baseline rojo (`test_contract_closure`, `usp_consultar_asignaturas_paginado`) | Causa probable: **deriva de la BD local**. La instancia tiene 89 SP y el repo 70; `usp_consultar_asignaturas_paginado` existe en la instancia pero ya no en el repositorio. Pendiente: reconstrucción limpia para confirmar (`BLOCKED_BY_ENVIRONMENT` hasta decisión del owner) |
| `LB004D_W2_NOT_ENROLLED` | No atribuible a HU035/HU036 (no usa sus objetos); se re-evalúa tras la reconstrucción limpia |

Dictamen DB: **GREEN funcional aprobado**; gate de regresión de suite completa **pendiente** de la reconstrucción limpia.

### Reconstrucción limpia de la BD local (autorizada por el owner, 2026-10-21)

| Paso | Comando | Resultado |
|---|---|---|
| Drop + create | `sqlcmd -Q "ALTER DATABASE ... SINGLE_USER ...; DROP DATABASE gestionasistenciadb; CREATE DATABASE gestionasistenciadb;"` | exit 0 |
| Deploy | `.\deploy_schema.ps1 -ContainerName sql_server_asistencias` (rama `feature/HU035-HU036-...`) | exit 0; incluye `16_hu035_parametro_estado_adjunto.sql` y el SP nuevo |
| Manifiesto | `.\scripts\freeze-manifest.ps1 -Mode Verify` | `FREEZE_MANIFEST_OK entries=10` |
| Gate sin fixture LB-004D ni clasificación | `.\test_summary.ps1` | exit 1: `PUBLIC_CANONICAL_RESULTSET usp_consultar_detalle_solicitud_revision_docente bad column 1` (Msg 52009) |
| Gate como en CI (fixture LB-004D + clasificación temporal del SP nuevo como `READ_QUERY_RESULTSET` en `test/test_contract_closure.sql`, revertida después) | `prepare_lb004d_fixture.ps1` → `test_summary.ps1` → `cleanup_lb004d_fixture.ps1` | **exit 0, `DB GATE PASS`, `PASSED=254`, `FAILED=0`, `CRITICAL_MISSING=0`, `SQL_ERROR_COUNT=0`**; 6 `TEST_PASS:HU035_HU036_*` dentro de la suite |

Conclusiones:
- El baseline rojo previo (`usp_consultar_asignaturas_paginado`, 89 SP en instancia vs 70 en repo) era **deriva de la BD local**; desaparece con la reconstrucción.
- `LB004D_W2_NOT_ENROLLED` se debía a no preparar el fixture LB-004D (paso obligatorio en CI); con el fixture pasa.
- **Pendiente de aprobación (scope):** agregar `usp_consultar_detalle_solicitud_revision_docente` a la lista normativa de exclusiones `READ_QUERY_RESULTSET` de `test/test_contract_closure.sql` (3 cláusulas `NOT IN` + comentario), siguiendo el precedente de `usp_consultar_grupos_paginado` (commit `b7667b6`) y de LB-004D (commit `231be58`). Sin ese cambio el gate de la DB queda rojo. El repositorio conserva el archivo original.

---

## 2. Backend (`GestioAsistenciaBackend`, rama `feature/HU035-HU036-bandeja-detalle-reclamos-docente`)

> Capa implementada por un subagente (Sonnet) bajo TDD con RED congelado. Sin commits ni push. Fecha: 2026-10-21.

### 2.1 Baseline (antes de cambios)

| Campo | Valor |
|---|---|
| Base commit (Backend) | `4ff37ffadec64495f51459535f5fb83f9d5de3b6` (`develop`) |
| Corrida 1 | `.\mvnw.cmd -q test` — exit `1`: `Tests run: 1727, Failures: 1, Errors: 70`. Causa: **clases de test obsoletas en `target/test-classes`** (compiladas por el IDE con ECJ el 2026-10-21 15:48, `Unresolved compilation problem: The declared package ... jpa.repository does not match the expected package ... jpa`) que Maven no recompiló, más `SwaggerUiEnabledRuntimeTest` (404) por el mismo estado de `target/`. No relacionado con HU035/HU036. |
| Corrida 2 (baseline válido) | `.\mvnw.cmd clean test` — exit `0`: **`Tests run: 1657, Failures: 0, Errors: 0, Skipped: 0`**, `BUILD SUCCESS` (incluye ArchUnit). |
| Acción | `target/` es artefacto ignorado por git; se regeneró con `clean`. Ningún archivo versionado fue tocado para obtener el baseline. |

### 2.2 RED_SNAPSHOT

| Campo | Valor |
|---|---|
| Base commit | `4ff37ffadec64495f51459535f5fb83f9d5de3b6` |
| Comando | `.\mvnw.cmd -q test-compile` |
| Exit code | `1` (`COMPILATION ERROR`, 176 líneas `[ERROR] ...java`) |
| Fallo observado | `package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.* does not exist` y `cannot find symbol` para `DetalleSolicitudRevisionDocenteProjection`, `ConsultarDetalleReclamoDocenteInputPort`, `DetalleReclamoDocenteDTO`, `SpDetalleSolicitudRevisionDocenteEntity` y `consultarDetalleSolicitudDocente` |
| Fallo esperado (TEST_PLAN) | Compilación: clases del caso de uso, proyección, entidad SP y método de puerto inexistentes — coincide. |

Como el módulo de tests no compila, CT-01..04 (ruta `GET /reclamos/{id}` inexistente) quedan RED por compilación en la misma corrida; SEC-01 se agregó como guarda de no regresión (el matcher existente ya cubre la ruta) y se espera verde en GREEN.

| Archivo de test | SHA-256 (RED, congelado) | Casos |
|---|---|---|
| `src/test/java/.../application/features/docente/consultardetallereclamo/ConsultarDetalleReclamoDocenteFeatureTest.java` (NUEVO) | `5E54E3651CEF79DA5D0F13628BD44F010E78A6C202828466F73C0B2FD59A9926` | UC-01..UC-06 (+ validación de nulos, causal con `asistio` sin razón, soporte) |
| `src/test/java/.../infrastructure/adapter/primary/controller/docente/DocentePortalControllerTest.java` (EXISTENTE, se agregan casos; el constructor del controlador recibe el nuevo puerto) | `2BA18F7B65645F63A4242CDAE70516486C264DC1176AA5ABBC2B4976372F8899` | CT-01..CT-04 (+ caso sin causal/soporte) |
| `src/test/java/.../infrastructure/adapter/primary/security/RbacSecurityFilterChainTest.java` (EXISTENTE, se agregan casos y ruta en el probe) | `3F8203A6FDF93AA96D6889F2B29F12F6580A67A0090FE347777E9D1C0ED88925` | SEC-01 (DOCENTE 200 / ESTUDIANTE 403 / COORDINADOR 403 / sin token 401) |
| `src/test/java/.../jpa/SolicitudRevisionDocenteDetalleJpaRepositoryTest.java` (NUEVO) | `DFA869A69E6B62C3B9D4809243A2F0E10A60F7CEBCDFB187503FC5ED8307392B` | Adapter con mocks: parámetros del `EXEC`, `@idCorrelacion` del contexto (IT-03 a nivel de adapter), UTC solo con procedencia, traducción de fallo técnico |
| `src/test/java/.../jpa/SolicitudRevisionDocenteDetalleSqlIT.java` (NUEVO, `@Tag("integration")`) | `07D3C79C2D86D8CF2D3CFC513849E71F435DDEDE3F2DB24E829791CAE4F72DFD` | IT-01, IT-02, IT-03 contra SQL Server real |

Notas de diseño de tests:
- CT-02: un `id` no UUID produce `400` con `code = INVALID_REQUEST` (comportamiento global de `GlobalExceptionHandler#handleMethodArgumentTypeMismatch`, igual en todos los endpoints con `@PathVariable UUID`), no `VALIDATION_ERROR` como indica literalmente CONTRACT 3.2. El estado `400` se cumple; el nombre del código difiere. Ver desviación D-01.
- IT-03 verifica el `@idCorrelacion` en dos niveles: el unit del adapter comprueba el valor enviado; el IT comprueba que sin correlación en el contexto el adapter falla (no genera ni quema una).

### Clasificación aprobada en `test_contract_closure.sql` (owner, 2026-10-21)

- Cambio aplicado en el repositorio: `usp_consultar_detalle_solicitud_revision_docente` agregado a las 3 cláusulas `NOT IN` de la regla `PUBLIC_CANONICAL_RESULTSET` y al comentario de exclusiones normativas (`git diff --stat`: 1 archivo, +5/−3).
- Gate como en CI (`prepare_lb004d_fixture.ps1` → `test_summary.ps1` → `cleanup_lb004d_fixture.ps1`): **exit 0, `DB GATE PASS`, `PASSED=254`, `FAILED=0`, `CRITICAL_MISSING=0`, `SQL_ERROR_COUNT=0`.**
- Dictamen DB: **PASS** (funcional y regresión de suite completa).

### 2.3 GREEN

Los comandos se ejecutaron en una **copia aislada** del árbol de fuentes (sin `.git`, con su propio `target/`) porque en el `target/` del repositorio hay un `spring-boot:run` ajeno en ejecución (PID 28380, desde 16:58) y una corrida de Maven sobre el `target/` compartido perdió clases (`ClassNotFoundException`) a mitad de ejecución. Las fuentes de la copia son idénticas a las del repositorio en el momento de la corrida.

| Paso | Comando | Resultado |
|---|---|---|
| Unit + ArchUnit | `.\mvnw.cmd test` | exit `0`; **`Tests run: 1682, Failures: 0, Errors: 0, Skipped: 0`** (baseline 1657 + 25 nuevos); `BUILD SUCCESS` |
| Quality gates | `.\mvnw.cmd verify` | exit `0`; mismos 1682 tests; `All coverage checks have been met.` (JaCoCo bundle: **LINE 91,10 %**, **BRANCH 80,26 %**, gates 80/70); ArchUnit dentro de los 1682 sin fallos |
| IT HU036 contra SQL Server real | `.\mvnw.cmd -Pintegration -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -Dit.test=SolicitudRevisionDocenteDetalleSqlIT verify` (`SPRING_DATASOURCE_*` por variables de entorno hacia `gestionasistenciadb` local) | exit `0`; **`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`** (IT-01 con causal/estado/estudiante/grupo/soporte, sin detalle ni soporte, IT-02 docente ajeno/estudiante/id inexistente, IT-03 correlación) |
| Limpieza del IT | `SELECT COUNT(*)` de filas `IT-HU036-%`, `IT-LB0022D-%` | `0`, `0`, `0` |
| Integridad de tests RED | `Get-FileHash -Algorithm SHA256` tras GREEN | Los 5 hashes de 2.2 coinciden (ningún test RED se modificó) |

Cobertura de las clases nuevas (unit, sin IT): `ConsultarDetalleReclamoDocente*` y `DetalleReclamoDocenteHttpMapper` 100 % de líneas; mapper de aplicación 10/12 ramas; `SolicitudRevisionDocenteJpaRepository` 77/77 líneas, 5/6 ramas. `SpDetalleSolicitudRevisionDocenteEntity` (28 getters) solo se ejercita en el IT.

`NOT_RUN`: `.\mvnw.cmd verify -Pintegration` completo (resto de ITs del proyecto, no relacionados con este work item); E2E con token Keycloak (`scratch/test_hu035_hu036.ps1`, corresponde al agente principal).

### 2.4 Modificación de test existente post-RED

| Archivo | Cambio | Justificación |
|---|---|---|
| `src/test/java/.../openapi/OpenApiGoldenPathConformanceTest.java` | Se agrega `mock(ConsultarDetalleReclamoDocenteInputPort.class)` al construir `DocentePortalController` | El constructor del controlador recibe el nuevo puerto; el cambio es solo de compilación, no altera aserciones. El archivo está fuera de `ALLOWED_PATHS` literal (`**/docente/**`, `**/jpa/**`); sin él el módulo de tests no compila. |

`DocentePortalControllerTest` también se actualizó en la construcción del controlador (mismo motivo), dentro del RED congelado de 2.2 (su hash ya incluye ese cambio).

### 2.5 Desviaciones y hallazgos

- **D-01 (contrato vs plataforma):** CONTRACT 3.2 indica `400 VALIDATION_ERROR` para `id` no UUID, pero `GlobalExceptionHandler` responde `400` con `code = INVALID_REQUEST` para todo `@PathVariable UUID` mal formado. CT-02 verifica `400` + `INVALID_REQUEST`. Cambiar el handler (global, fuera de ALLOWED_PATHS) alteraría otros endpoints; requiere decisión del owner.
- **D-02 (OpenAPI):** el código de `src/main` no usa anotaciones `@Operation`/`@ApiResponse` (el contrato HTTP vive en el YAML Golden Path, WORM, y AGENTS prohíbe anotaciones como fuente primaria). No se agregaron; el endpoint nuevo no está en `openapi-golden-path.yaml` (fuera de rutas permitidas).
- **D-03 (reutilización):** `ConsultarReclamosDocenteMapper#estadoLegible` pasó de `private` a `public` y se agregó `urlSoporte(UUID)` para no duplicar la traducción P/A/R ni el prefijo de descarga (cambio en `features/docente/consultarreclamos`, sin cambio de comportamiento).
- **D-04 (entorno):** `target/` del repositorio contenía clases de test obsoletas compiladas por el IDE; se ejecutó `mvnw clean` (artefacto ignorado por git). Había un `spring-boot:run` ajeno sobre ese `target/`; el `clean` pudo afectarle la carga de clases.
- **D-05 (hallazgo ajeno):** `SecurityConfig.java` aparece modificado en el árbol de trabajo (cambio en `/api/v1/admin/sedes|areas`). **No fue modificado por esta capa.** La copia aislada de las corridas de 2.3 incluyó ese cambio; ningún test falló por él.

## Auditoría Backend (fase 05) — agente principal, 2026-10-21

| Comprobación | Resultado |
|---|---|
| Hashes de los 5 tests RED tras GREEN | PASS — coinciden con §2 (`5E54E365…`, `2BA18F7B…`, `3F8203A6…`, `DFA869A6…`, `07D3C79C…`) |
| Orden RED → GREEN | PASS — tests escritos 18:36–18:57, producción 19:00–19:03 (marcas de tiempo de archivo) |
| Flujo Controller → InputPort → UseCase (POJO, wiring `@Bean`) → SecondaryPort → `SolicitudRevisionDocenteJpaRepository` | PASS (revisión manual) |
| Use case: `ForbiddenException` sin docente; `ResourceNotFoundException` si vacío | PASS |
| Adapter: `EXEC` nombrado, `CorrelationIdContext.require()`, UTC solo con procedencia | PASS |
| Response vs CONTRACT 3.2 (campos, `causalInasistencia`, `soporteAdjunto`) | PASS |
| `OpenApiGoldenPathConformanceTest` modificado post-RED | ACEPTADO — solo agrega mock del nuevo InputPort al constructor; sin cambio de aserciones |
| `SecurityConfig.java` modificado (GET `/admin/sedes/**`, `/admin/areas/**` para DECANO/COORDINADOR) | **AJENO al work item** — el subagente declara no haberlo hecho; coincide con un `spring-boot:run` externo activo. No se revierte sin decisión del owner; **no debe entrar en el commit de esta HU** |
| D-01: 400 `INVALID_REQUEST` (no `VALIDATION_ERROR`) para id no UUID | Contrato 3.2 erróneo respecto del comportamiento global AS-IS de `GlobalExceptionHandler`; requiere enmienda del CONTRACT (pendiente owner) |
| D-02: sin `@Operation`/`@ApiResponse` | El código no usa anotaciones OpenAPI; el YAML Golden Path es WORM. Requiere enmienda del CONTRACT y registro del endpoint en `HTTP_AS_IS_MATRIX.md` (pendiente owner) |
| `verify -Pintegration` completo | NOT_RUN (solo IT dirigido: 4/4 PASS) — ejecutar antes del PR |
| E2E-01 con token Keycloak | NOT_RUN — se ejecuta tras el frontend |

Dictamen Backend: **GREEN funcional aprobado** con enmiendas de contrato D-01/D-02 pendientes y gates `-Pintegration` completo + E2E pendientes.

---

## 3. Frontend (`GestioAsistenciaFrontend`, base commit `ea1e537c09938c674a7766ddde5c10f4c8249b73`)

### 3.1 Baseline (antes de cambios)

| Campo | Valor |
|---|---|
| Comando | `npm run test:ci` (Karma, ChromeHeadlessCI, cobertura) |
| Resultado | 821 tests: 820 SUCCESS, 1 FAILED — exit code 1 |
| Fallo preexistente (ajeno) | `DeanInfraestructuraComponent carga áreas, sedes y espacios de la facultad y permite filtrar`: `Expected one matching request for criteria "Match URL: http://localhost:8080/api/v1/admin/espacios-fisicos", found none.` Proviene de cambios sin commit del usuario en `src/app/features/dean/dean-infraestructura/*` (fuera de este work item; no se toca). |
| Cobertura global | Statements 87.69 %, Branches 70.59 %, Functions 86.41 %, Lines 88.94 % |

### 3.2 RED_SNAPSHOT (congelado antes de escribir código de producción)

| Campo | Valor |
|---|---|
| Base commit | `ea1e537c09938c674a7766ddde5c10f4c8249b73` |
| Archivos de test | `src/app/core/services/attendance-claim.service.spec.ts`, `src/app/features/teacher/teacher-claims/teacher-claims.component.spec.ts` |
| SHA-256 `attendance-claim.service.spec.ts` | `29a1432e80e17995d94169c25452a3b853912c07a298e633981b8a801a5471c4` |
| SHA-256 `teacher-claims.component.spec.ts` | `a4568055683c466a8ffffb7409f63cdff191729f33aeae8f29ee4e760399a269` |
| Comando | `npm run test:ci` |
| Exit code | 1 |
| Fallo | Compilación TypeScript del bundle de tests: 29 errores `TS` (ver tabla). Ningún test se ejecuta mientras el contrato no existe. |

| Caso | Fallo RED (excerpt) |
|---|---|
| FE-01, FE-02 (servicio) | `TS2345: Argument of type '{ pagina: number; tamano: number; }' is not assignable to parameter of type 'string'` (el servicio solo acepta `docenteId?: string` y no envía params) |
| FE-04, FE-05, FE-07 (servicio) | `TS2551: Property 'getDetalleReclamoDocente' does not exist on type 'AttendanceClaimService'` |
| FE-01..FE-06, FE-08 (componente) | `TS2339: Property 'total' / 'cambiarPagina' / 'cambiarTamano' / 'pagina' / 'tamano' does not exist on type 'TeacherClaimsComponent'`; `TS2305: no exported member 'DetalleReclamoDocenteItem'` |

Nota: el RED es de compilación (el contrato añade API pública nueva). FE-08 (sin polling) y la parte de aserciones de causal/filtro/contador no pueden ejecutarse hasta compilar; el componente AS-IS las incumpliría igualmente (filtro `selectedEstado` en `computed` no reactivo, contador sobre la página, estado original "No informado").

### 3.3 Modificaciones a tests existentes por cambio formal del contrato (CONTRACT §4.1)

| Archivo | Test | Expectativa anterior | Expectativa nueva | Cláusula |
|---|---|---|---|---|
| `attendance-claim.service.spec.ts` | `getReclamosDocente consulta la lista y conserva el fallback 501` (renombrado `... y propaga el 501 sin enmascararlo`) | Con 501 el observable emitía `datos: []` exitoso | El 501 se propaga como error (`status === 501`); además la petición lleva `pagina`/`tamano` | CONTRACT §4.1: "Se elimina el enmascaramiento de 501/404 como éxito" y envío de query params |
| `attendance-claim.service.spec.ts` | `conserva el fallback de módulo no disponible para 501` (renombrado `propaga el 501 del módulo de reclamos en lugar de simular una lista vacía exitosa`) | `result.datos` igual a `[]` | No hay emisión `next`; error con `status === 501` | CONTRACT §4.1 |
| `attendance-claim.service.spec.ts` | Helper `cargar()` del bloque "reclamos del docente (UTC v2)" | Llamaba `getReclamosDocente()` y esperaba la URL sin query | Llama `getReclamosDocente({ pagina: 1, tamano: 10 })` y casa por `r.url`; **las aserciones de mapeo no cambian** | CONTRACT §4.1 (firma con filtro obligatorio) |
| `teacher-claims.component.spec.ts` | `beforeEach` del describe `TeacherClaimsComponent` | Spy sin `getDetalleReclamoDocente`; mock de lista sin `total` | Spy con `getDetalleReclamoDocente` (devuelve detalle con id `rec-1`) y mock con `total: 1` | CONTRACT §4.1/§4.3 (`ApiListResponse` exige `total`; abrir solicitud consulta el detalle). Ninguna aserción existente cambió. |

### 3.4 GREEN

| Campo | Valor |
|---|---|
| Comando | `npm run test:ci` |
| Resultado | 847 tests: 846 SUCCESS, 1 FAILED — exit code 1 |
| Único fallo | El mismo fallo preexistente y ajeno de `DeanInfraestructuraComponent` (cambios sin commit del usuario); ningún test de este work item falla. Tests nuevos: 26 (821 → 847). |
| Integridad RED | SHA-256 de ambos specs tras GREEN idénticos a §3.2 (`29a1432e…`, `a4568055…`) |
| Cobertura de archivos del work item (lcov) | `teacher-claims.component.ts` líneas 111/117, ramas 40/51; `attendance-claim.service.ts` líneas 35/35, ramas 8/12; `claim.mapper.ts` líneas 5/6, ramas 36/54 |
| Cobertura global | Statements 87.80 %, Branches 70.89 %, Lines 89.05 % (baseline 87.69 / 70.59 / 88.94) |
| `npm run verify` | exit 1 únicamente por el paso `test:ci` (fallo ajeno de Dean). Los pasos siguientes se ejecutaron por separado: `npm run coverage:realtime:check` PASSED (LINE 95.06 %, BRANCH 84.52 %); `npm run build -- --configuration=production` exit 0, 0 errores (incluye los archivos Dean en curso del usuario). |

### 3.5 Desviaciones y hallazgos (Frontend)

- **F-01 (compatibilidad de firma):** `getReclamosDocente` tiene dos sobrecargas: `(filtro)` → `ApiListResponse` (contrato §4.1) y `()` → `ApiResponse<SolicitudRevisionItem[]>` (primera página, tamaño 10). Motivo: `attendance-control.component.ts` y `overview.component.ts` (fuera de ALLOWED_PATHS) invocan el método sin argumentos y sus specs mockean sin `total`.
- **F-02 (riesgo en consumidores fuera de alcance):** al eliminar el enmascaramiento 501/404, `overview.component.ts` (`getReclamosDocente().subscribe({ next })`, sin handler de error) recibirá un error no manejado si el endpoint responde 501/404; `attendance-control.component.ts` ya tiene handler (`[]`). Además `overview` sigue contando pendientes sobre la primera página (mismo defecto que AC-04, fuera de alcance). Pendiente de decisión del owner / HU separada.
- **F-03:** `IdentificadorInvalidoError` ahora usa el mensaje genérico `Identificador inválido.` (se reutiliza para el id de solicitud); ningún test dependía del texto anterior.
- **F-04:** se retiró del listado la etiqueta "Falla: No informado" (el listado no trae la causal; la causal real se muestra solo en la vista de resolución con el detalle). La vista de resolución muestra `causalInasistencia.nombre` y "(Asistió/No asistió)" si `asistio` no es nulo, o "Sin registro de asistencia" si es `null`.
- **F-05:** si tras recargar la página actual queda vacía (p. ej. al resolver la última solicitud de una página) el componente retrocede a la última página con datos.
- **F-06:** la búsqueda textual sigue siendo local sobre la página cargada (no existe parámetro de búsqueda en el contrato); su estado pasó a `signal` para ser reactivo bajo OnPush.
- **F-07:** el mensaje del error de detalle usa `ApiErrorResponse.message` del backend; sin envelope usa `getApiErrorMessage` (por código).

`NOT_RUN`: E2E con backend/Keycloak reales (E2E-01), prueba visual en navegador del flujo bandeja → detalle (no se levantó el stack); `ng test` no cubre estilos Tailwind.

## Auditoría Frontend (fase 05) — agente principal, 2026-10-21

| Comprobación | Resultado |
|---|---|
| Hashes de specs RED tras GREEN (`29A1432E…`, `A4568055…`) | PASS — idénticos |
| Archivos ajenos (`dean-faculty`, `dean-infraestructura`) | Sin tocar por el subagente (cambios previos del owner u otra sesión); **no deben entrar en el commit de esta HU** |
| Servicio: query params `pagina`/`tamano`/`estado`, sin enmascarar 501/404, detalle con `isUuid` y sin toast global duplicado | PASS (revisión del diff) |
| Patrones prohibidos en lo agregado (`any`, `setInterval`, `@Input/@Output`, `localStorage`, `bypassSecurityTrust*`) | PASS (0 coincidencias); el único `ngModel` restante es el textarea de resolución preexistente |
| Cambios de specs existentes por el contrato (fallback 501 → propagación) | ACEPTADO — justificado por CONTRACT §4.1, registrado en §3.3 |
| Sobrecarga `getReclamosDocente()` sin filtro para `overview` y `attendance-control` | ACEPTADO como compatibilidad. **Deuda:** `overview.component.ts` cuenta pendientes sobre la primera página (mismo defecto que AC-04) y su `subscribe` no maneja errores |
| `test:ci` 847 / 846 OK / 1 fallo | El fallo es `DeanInfraestructuraComponent`, ya presente en el baseline (821/820) por cambios ajenos sin commit; ningún test de HU035/HU036 falla |
| Build de producción | PASS (exit 0) |
| Gate realtime | PASS (líneas 95,06 % / ramas 84,52 %) |
| Prueba visual y E2E-01 | NOT_RUN |

Dictamen Frontend: **GREEN funcional aprobado**. `npm run verify` queda rojo solo por un test ajeno (Dean) que debe resolverse fuera de esta HU.

## Integración completa `-Pintegration` (2026-10-21)

- Resultado: `Tests run: 235, Failures: 19, Errors: 9, Skipped: 4`, BUILD FAILURE; JaCoCo: coverage checks met.
- HU035/HU036: `SolicitudRevisionDocenteDetalleSqlIT` 4/4 PASS; `SoporteArchivoSqlIT` 11/11 PASS.
- Clases con fallos (pendiente de triage contra baseline `develop`):
  - co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo.ArchivoUploadDownloadFlowIT
  - co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.malwarescan.clamav.ClamAvMalwareScanAdapterIT
  - co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CoreViewQueriesJpaParityIT
  - co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.SesionGrupoCommandsSpParityIT
  - co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio.MinioFileStorageAdapterIT
- Hipótesis no verificada: MinIO/ClamAV no levantados localmente (MinioFileStorageAdapterIT 10/10) y fixture MAINT-003G no cargado (Msg 8624).
- E2E-01: NOT_RUN.

### Triage de los fallos de `-Pintegration` (agente principal, 2026-10-21)

| Clase | Causa verificada | Relación con HU035/HU036 |
|---|---|---|
| `ClamAvMalwareScanAdapterIT` | Faltaban `CLAMAV_HOST`/`CLAMAV_PORT` en el entorno de la corrida. Con las variables (`127.0.0.1:3310`) **pasa** | Ninguna (ambiente) |
| `MinioFileStorageAdapterIT`, `ArchivoUploadDownloadFlowIT` | Primero faltaba `MINIO_*`; con las variables, MinIO responde `InvalidAccessKeyId`: el MinIO activo (`minio_asistencias`, proyecto compose `asistencias-uco-infra`) no tiene provisionado el usuario técnico de aplicación definido en `infra/files/.env` | Ninguna (aprovisionamiento local de MinIO) |
| `CoreViewQueriesJpaParityIT` | Exige un fixture con ≥11 estudiantes (MAINT-003H), que no se cargó porque `prepare_java_integration_fixture.ps1` aborta antes | Ninguna (fixture) |
| `SesionGrupoCommandsSpParityIT.GRP_04` | `ConflictException` al registrar un estudiante en grupo: estado de datos sin el fixture MAINT-003G/H | Ninguna (no se tocó Grupo) |
| Fixture MAINT-003G | `Msg 8624` (Internal Query Processor Error) en `DELETE r FROM SolicitudRevisionAsistencia r JOIN Asistencia JOIN Sesion`, reproducible aislado. Involucra `FK_ArchivoSoporte_SolicitudRevisionAsistencia` + índice único filtrado `UX_ArchivoSoporte_Solicitud` (LB-004D, congelados) en SQL Server 2022 CU26 | Preexistente desde LB-004D; candidato a deuda técnica nueva |

Nota de ambiente: un intento de `docker compose -f infra/files/compose.yaml up --no-deps minio-init` creó por error la red `asistencias-files-backend`, el volumen vacío `asistencias-minio-data` y el contenedor detenido `asistencias-files-minio-init-1` (proyecto `asistencias-files`), sin afectar `minio_asistencias`. Pendiente de limpieza manual por el owner.

## E2E-01 — backend real + Keycloak + SQL Server (agente principal, 2026-10-21)

- Backend: `scripts\run-local.ps1` sobre la rama `feature/HU035-HU036-bandeja-detalle-reclamos-docente` (con variables `MINIO_*`/`CLAMAV_*` del entorno local de archivos). Keycloak `:8081` realm `asistencias-uco`. Usuarios: `maria.rostagno` (DOCENTE titular), `carlos.zapata` (ESTUDIANTE), `coordinador.sistemas` (COORDINADOR).
- Script: `scratch/test_hu035_hu036.ps1`. **Resultado: `E2E RESULT: PASS` — 17/17**, exit 0.

| Verificación | Resultado |
|---|---|
| Radicación real por el estudiante (fixture) y persistencia en SQL | PASS |
| `GET /docente/reclamos?estado=P&pagina=1&tamano=5`: 200, `total ≥ 1`, ≤ 5 filas, solicitud `PENDIENTE` presente | PASS |
| Estado inválido → 400 | PASS |
| `GET /docente/reclamos/{id}`: 200, `codigoEstado=P`/`PENDIENTE`, causal `SJC` con `asistio=false`, sesión correcta, sin correo | PASS |
| UUID inexistente → 404 `RESOURCE_NOT_FOUND`; `abc` → 400 `INVALID_REQUEST` | PASS |
| ESTUDIANTE → 403; COORDINADOR → 403; sin token → 401 | PASS |
| Limpieza sin residuos (workaround de TD-059); FK `enabled` y `trusted` después | PASS |

Limitaciones:
- El 404 por solicitud de **otro docente** no se probó en E2E porque no hay un segundo docente en Keycloak. Está cubierto por DB-02 (T-SQL) e IT-02 (SQL Server real).
- La primera corrida dio 16/17: la limpieza falló por TD-059 (Msg 8624). Se diagnosticó y se corrigió la limpieza del script; la segunda corrida completa dio 17/17.
- Validación visual en navegador: NOT_RUN.

### Ambiente de archivos (MinIO) — corrección durante la prueba manual del owner (2026-10-21)

- Síntoma reportado por el owner: `500 INTERNAL_ERROR` al subir y descargar soportes (`correlationId bb2f8f3a-…`).
- Causa:
  - El contenedor `minio_asistencias` (proyecto compose `asistencias-uco-infra`) se recreó a las 20:22 hora local y quedó sin usuario de aplicación: 0 usuarios y bucket `asistencias-soportes` con 0 objetos.
  - Además, el backend de la E2E se había levantado con las credenciales de `infra/files/.env` en lugar de `AsistenciasUCO-Infra/.env`.
  - No es un defecto de HU035/HU036.
- Corrección, autorizada por el owner:
  - Backend reiniciado con las variables `MINIO_*`/`CLAMAV_*` de `AsistenciasUCO-Infra/.env`.
  - Ejecución de `/usr/local/bin/minio-init` (el mismo script versionado en `infra/files/minio/minio-init.sh`, idempotente) dentro de `minio_asistencias`: crea el usuario técnico y la política `asistencias-backend` de mínimo privilegio, y deja el bucket privado.
- Verificación:
  - `POST /api/v1/archivos/subir` como estudiante: **201**.
  - `GET /api/v1/archivos/{fileId}` como propietario: **200**, con SHA-256 idéntico al original.
- Residuos:
  - El archivo previo `14db0355-…` (`incapacidad_medica.pdf`) conserva su metadata `ATTACHED` en SQL, pero sus bytes no existen en el MinIO recreado, así que no puede descargarse.
  - Queda un borrador de prueba `f3d05ef6-…` (DRAFT, propietario `carlos.zapata`).
