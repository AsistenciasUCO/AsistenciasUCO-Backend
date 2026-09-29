---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2B — JPA STORED PROCEDURE FEASIBILITY

Etapa **FEASIBILITY** ([TEST_PLAN §Revisión 2.2A](TEST_PLAN.md)). Responde si Hibernate 7.2 / Jakarta Persistence / `StoredProcedureQuery` pueden consumir el SP congelado `dbo.usp_registrar_asistencias_sesion` contra **SQL Server real**. **No certifica el command JPA**: solo la viabilidad del mecanismo. La paridad JDBC↔JPA sigue siendo `CMD-PAR-*` (2.2D).

```text
JPA_STORED_PROCEDURE_FEASIBILITY: PASS   (con un HALLAZGO de diseño en U-03; ningún STOP activo)
```

## Entorno y comandos

| Campo | Valor |
|---|---|
| Motor | SQL Server real, contenedor `sql_server_asistencias` (Up), base `gestionasistenciadb`, freeze DB desplegado. Sin H2, sin mock de `EntityManager`/repositorio. Credenciales cargadas con `scripts/load-env.ps1` (`.env` local, no versionado; ningún valor en esta evidencia) |
| Precondición Golden Path | `. .\scripts\load-env.ps1; .\mvnw.cmd -Pintegration test-compile failsafe:integration-test failsafe:verify "-Dit.test=GoldenPathSqlStoredProcedureContractIT"` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`, exit 0 (2026-09-28 00:52–00:53 -05:00). Incluye la firma de `usp_registrar_asistencias_sesion`: `@idSesion uniqueidentifier`, `@asistenciaJSON nvarchar`, `@idCorrelacion uniqueidentifier`, `@idUsuarioEjecutor uniqueidentifier` (nombres, tipos y orden) → `GOLDEN_PATH_SP_CONTRACT: PASS` |
| Probe | `JpaAttendanceStoredProcedureFeasibilityIT` (NUEVO, solo `src/test`, `@Tag("integration")`) + `AttendanceCommandFixture` (soporte NUEVO; copia mínima del fixture de `AsistenciaRepositorySqlServerIT`, que **no** se modificó — Q-2) |
| Comando | `. .\scripts\load-env.ps1; .\mvnw.cmd -Pintegration test-compile failsafe:integration-test failsafe:verify "-Dit.test=JpaAttendanceStoredProcedureFeasibilityIT"` |
| Resultado final | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`, exit 0, `BUILD SUCCESS` (2026-09-28 00:59:19–01:00:01 -05:00) |
| EMF | El real de la aplicación, activado en el test con `asistencia-query-provider=jpa` (única forma aprobada hoy de construirlo). Perfiles productivos sin cambios. `hikari.maximum-pool-size=1` **solo en el probe** (permite observar la única conexión y detectar fugas) |
| Salida sensible | El payload JSON y los IDs no se loguean; las observaciones usan el prefijo `[FEASIBILITY-OBS]`. Hibernate solo muestra la sintaxis `{call ...(?,?,?,?)}` (sin valores) |

Historial honesto del probe: una corrida intermedia falló (13/14) cuando se añadió el registro reordenado de U-03; ese fallo **fue el hallazgo** (ver U-03) y el test se reformuló como caracterización del hallazgo, no se silenció.

## Resultados

| ID | Pregunta | Estado | Evidencia concreta |
|---|---|---|---|
| **U-01** | ¿`StoredProcedureQuery` ejecuta el SP sin transacción JPA exterior? | **PASS** | Sin `@Transactional`, sin `getTransaction().begin()`, sin `JpaTransactionManager`: `txActiveBefore=false`, `txActiveAfterExecute=false`, `isJoinedToTransaction=false`. El SP completa (`estadoResultado=true`, eco de correlación correcto). **Desde otra conexión** (`DriverManager`, `LOCK_TIMEOUT 5000`), *antes* de cerrar el EM, se ven los 3 detalles → commit inmediato del SP. Tras el éxito y tras un rechazo (`RC_001`): `autocommit=true`, `@@TRANCOUNT=0` en la única conexión del pool, Hikari `activeConnections=0`, 0 filas tras el rechazo (rollback conservado). **Control negativo**: abriendo *deliberadamente* una transacción exterior, la otra conexión queda **bloqueada/timeout** (`-1`) y el rollback exterior deja 0 filas ⇒ el probe SÍ detecta una transacción exterior; su ausencia en el camino sin transacción es evidencia real, no vacua. Condición D4 **demostrada** con SQL Server real para este SP |
| **U-02** | ¿Se alcanza de forma determinista la fila canónica si hay update counts previos? | **PASS** | SP real: traza `[execute=true, RS(rows=1,canonical=1), hasMoreResults=false, UC(-1)=fin]` — el SP emite el result set canónico directamente (no emite update counts previos; el algoritmo `execute()/getResultList()/getUpdateCount()/hasMoreResults()` llega igualmente a la fila). **Control determinista** (`sp_executesql`, sin tocar la DB) con update count previo: `[execute=false, UC(2), hasMoreResults=true, RS(rows=1,canonical=1), hasMoreResults=false, UC(-1)=fin]` ⇒ el update count **no** oculta la fila canónica. **Control D5** (`SET NOCOUNT ON` sin result set): `[execute=false, UC(-1)=fin]`, 0 filas, **sin excepción** ⇒ la ausencia de canal canónico es observable por estado (`hasMoreResults`/`getUpdateCount`) y distinguible de un fallo técnico; debe mapearse a `ERR_DB_CANONICAL_CONTRACT` (no éxito) |
| **U-03** | ¿Binding por nombre funciona con Hibernate + SQL Server? | **PASS con HALLAZGO** (fallback posicional viable) | Con nombres `idSesion`, `asistenciaJSON`, `idCorrelacion`, `idUsuarioEjecutor` registrados en el orden de `sys.parameters`, el SP ejecuta con éxito. Con el binding **posicional** (1..4) también. **Hallazgo:** Hibernate emite `{call dbo.usp_registrar_asistencias_sesion(?,?,?,?)}` — los nombres **no** llegan al SQL; con los mismos 4 parámetros *registrados en otro orden* el SP recibe argumentos cruzados y responde `estadoResultado=false, DBCODE=USU_001` sin excepción (3 de 4 son UUID: el tipo no protege). Es decir, el "binding por nombre" de Hibernate/mssql-jdbc **equivale a binding por orden de registro**. Consecuencia para el diseño: el candidato debe registrar SIEMPRE en el orden exacto de `sys.parameters` (guardia `CMD-PAR-013`, ya planificada) y el nombre no es una protección contra reordenamientos. Sin STOP (U-03 no lo tenía). Tipo `@asistenciaJSON` = `nvarchar(max)` (`max_length=-1`) |
| **U-04** | ¿`null` en `uniqueidentifier` se comporta como en JDBC? | **PASS** | `idUsuarioEjecutor=null`: baseline JDBC ⇒ `ValidationException(VALIDATION_ERROR)`; JPA ⇒ fila canónica con `DBCODE=GEN_002`, traducida con el mismo `DbExceptionTranslator` ⇒ `ValidationException(VALIDATION_ERROR)`; idéntico; sin excepción cruda de driver/Hibernate; 0 filas persistidas en ambas sesiones. Complementario `idSesion=null`: baseline ⇒ `DatabaseOperationException`; JPA ⇒ `DBCODE=VAL_001` ⇒ `DatabaseOperationException` (mismo tipo/código) |
| **U-05** | ¿`asistenciaJSON` > 4000 caracteres llega sin truncamiento? | **PASS** | Payload de 8471 caracteres (1 estudiante válido + 120 UUID no matriculados). Baseline JDBC ⇒ `ForbiddenException(FORBIDDEN)`; JPA ⇒ `DBCODE=EST_004` ⇒ `ForbiddenException(FORBIDDEN)`. **No** `ATT_001` (no hay JSON roto ni truncamiento); 0 filas persistidas. Parámetro `nvarchar(max)` |

## Observaciones adicionales (contrato del resultado y recursos)

| Observación | Valor |
|---|---|
| U-07 — tipos Java de las 4 columnas (`Object[]`) | `[String, String, String, Boolean]` (`idCorrelacion` llega como `String`, no `UUID`; `JdbcValueMapper.toUuid`/`toBoolean` los convierten sin problema) |
| Correlación / cardinalidad | Eco de `idCorrelacion` == enviado en todos los casos; exactamente 1 fila canónica por invocación |
| Cierre del `EntityManager` | `em.isOpen()==false` tras `close()` en cada llamada |
| Fuga de conexión | Pool=1: 25 rechazos + 1 éxito consecutivos sin bloqueo; `activeConnections=0`; `@@TRANCOUNT=0`; autocommit `true` |
| Residuos | `@AfterEach` verifica `residual == residualBefore` (cleanup FK-safe); sin DDL |
| Sintaxis emitida | `{call dbo.usp_registrar_asistencias_sesion(?,?,?,?)}` (posicional) |

## Stop conditions evaluadas

| Condición | Estado |
|---|---|
| U-01 exige transacción exterior incompatible | No — funciona sin transacción y confirma de inmediato |
| U-02 no recupera el canal canónico | No |
| U-04 cambia semántica de `null` | No — idéntica al baseline |
| U-05 trunca | No |
| Requiere `Session.doWork`/unwrap JDBC | No — todo con `StoredProcedureQuery` puro |
| Requiere cambiar SP / DB / Application / Port / HTTP / frontend | No |
| TD-043 cruza el Golden Path | No — el SP existe y su IT de contrato es 16/16 |

`STOP: NO_CLEAN_JPA_PATH` **no** se activa.

## Límites de estos resultados

- Los probes certifican **viabilidad del mecanismo**, no el command candidato ni la paridad completa (`CMD-PAR-001..018`, pendientes de 2.2D con el candidato real).
- El SP real no emite update counts previos; su tolerancia se demuestra con el control `sp_executesql` (mismo mecanismo Hibernate). Si el SP cambiara a emitir counts, el algoritmo del probe los recorre.
- U-06 (excepción exacta de Hibernate ante ausencia de resultado) queda resuelta parcialmente: con el algoritmo por estado no se produce excepción (0 filas), lo que satisface D5 sin depender del tipo de excepción. Su fijación unitaria queda para 2.2C.
- U-08 (auditoría observable por `idCorrelacion`) no se abordó (2.2D).
- TD-043 `OPEN / DEFERRED`; `GLOBAL_INTEGRATION_PROFILE = NOT_GREEN_TD043`. Esta ejecución fue **dirigida** y no dice nada del perfil global.
