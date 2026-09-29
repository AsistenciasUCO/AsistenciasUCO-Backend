---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2 — PLAN: JPA COMMAND PILOT `registrarAsistenciasSesion` (microfase 2.2A: Definition of Ready)

```text
MICROFASE:        LB-002.2A — DoR / análisis / planificación. NO implementa, NO crea el candidato JPA.
CHANGE_CLASS:     DOCUMENTATION_ONLY en 2.2A  ·  PERSISTENCE_MIGRATION en las microfases posteriores (aún NO autorizadas)
PRIMARY_VARIABLE: mecanismo de persistencia de AsistenciaRepositoryPort.registrarAsistenciasSesion(...): JDBC → JPA
BASE_GIT:         HEAD 0bfc02a58a8abc894841cc1e9b31c002a716554e (== ref local origin/develop; sin fetch), rama jose-valencia/lb-002.2a-jpa-command-plan
DoR 2.2A:         READY_FOR_REVIEW (documental) → REVIEW RESULT: APPROVED_FOR_2_2B (ver §Revisión humana de LB-002.2A)
DoR implementación: NOT_READY (ver §Definition of Ready)
MICROFASE ACTIVA: NINGUNA — LB-002.2 CERRADO (2.2A/2.2B/2.2C/2.2D/2.2E CLOSED tras revisión humana). Ninguna fase posterior se inicia automáticamente.
ESTADO 2.2C:      IMPLEMENTED_AND_VALIDATED / CLOSED (clean verify 1426 tests, LINE 90.63 % / BRANCH 81.17 %; ver GREEN_SNAPSHOT, VALIDATION, REPORT, CLOSURE).
ESTADO 2.2D:      CLOSED — paridad JDBC↔JPA REAL contra `gestionasistenciadb` (freeze DB desplegado): 21+3+3+3 tests nuevos (CMD-RT-004 cosmético eliminado por corrección de revisión humana), PARITY_MISMATCHES=0; ver PARITY_SNAPSHOT, LB-002.2D-VALIDATION, LB-002.2D-REPORT, LB-002.2D-CLOSURE, LB-002.2D-EXECUTION-EVIDENCE.
ESTADO 2.2E:      CLOSED — activación runtime local (`query=jpa`, `command=jpa`) validada manualmente por el usuario contra SQL Server real y frontend real (Golden Path completo, sin error de runtime JPA); ver LB-002.2E-E2E, LB-002.2E-CLOSURE.
ESTADO 2.2 (AGREGADO): CLOSED — JPA_COMMAND_FULLY_VALIDATED: YES (paridad SQL Server real + activación runtime + validación manual del Golden Path). Demás commands de `Asistencia` siguen en JDBC, sin cambio. TD-043 OPEN / DEFERRED, sin cambios. Ver LB-002.2-FINAL-CLOSURE.
COMMIT / PUSH:    NO (cierre documental de esta sesión; sin commit/push)
```

## Revisión humana de LB-002.2A (registrada al iniciar 2.2B)

```text
REVIEW RESULT: LB-002.2A → APPROVED_FOR_2_2B
```

| ID | Resultado | Precisión de la revisión |
|---|---|---|
| D1 | APPROVED | `EntityManager` + `StoredProcedureQuery`. |
| D2 | APPROVED | Nombre `app.adapters.persistence.asistencia-command-provider`; valores futuros `jdbc \| jpa`; default futuro `jdbc`. **Durante LB-002.2 este selector gobierna EXCLUSIVAMENTE `registrarAsistenciasSesion`.** Los demás commands de Asistencia continúan JDBC. Ampliar la semántica del selector a otro command exige una fase posterior explícita y nueva evidencia. (Cierra R-13: se conserva el nombre.) |
| D3 | APPROVED | Un solo `EntityManagerFactory` cuando `query=jpa OR command=jpa`. |
| D4 | APPROVED AS TARGET TO VALIDATE | El target es **no** introducir una transacción JPA exterior al SP. **No se declara todavía** que Hibernate soporte esta condición: debe demostrarse con SQL Server real (probe U-01). |
| D5 | APPROVED WITH CLARIFICATION | Ausencia de result set canónico debe mantener la semántica del baseline: `0 filas canónicas` **o** `sin result set canónico` ⇒ `ERR_DB_CANONICAL_CONTRACT`. Un fallo técnico verdadero de Hibernate/JPA ⇒ `DATABASE_OPERATION_ERROR`. Una `IllegalStateException` específica de navegación del result set **no** puede convertir automáticamente una violación del contrato canónico en un 500 técnico distinto del baseline. |
| D6 | APPROVED | Rollback futuro: `APP_ADAPTERS_PERSISTENCE_ASISTENCIA_COMMAND_PROVIDER=jdbc` + reinicio. |
| Q-1 | RESOLVED | El ACCEPTANCE RED de 2.2B usa **costuras productivas YA EXISTENTES**. No se crean production skeletons vacíos. No hay tests que importen `AsistenciaJpaCommandPersistence`, `CanonicalProcedureResultValidator` ni `JpaCapabilityRequiredCondition` hasta congelar sus firmas; sus tests unitarios se añadirán después, sin modificar ni debilitar el Acceptance RED aprobado. |
| Q-2 | RESOLVED | **No** se modifica `AsistenciaRepositorySqlServerIT` para extraer helpers. Si los nuevos IT necesitan fixture compartido: soporte NUEVO bajo `src/test`, o copia del mínimo indispensable. No se refactoriza ninguna prueba JDBC certificada durante el piloto. |
| U-01..U-05 | RECLASIFICADAS | No son "RED": son **JPA STORED PROCEDURE FEASIBILITY PROBES** (resultado `PASS \| FAIL \| STOP: NO_CLEAN_JPA_PATH \| BLOCKED_BY_ENVIRONMENT`). No se fabrica un fallo para llamarlo RED. |

## Revisión humana de LB-002.2B (registrada al iniciar 2.2C)

```text
REVIEW RESULT: LB-002.2B → APPROVED_FOR_2_2C
BASELINE:                        PASS
GOLDEN_PATH_SP_CONTRACT:         PASS
JPA_STORED_PROCEDURE_FEASIBILITY: PASS
ACCEPTANCE_RED:                  CONFIRMED
NO_CLEAN_JPA_PATH:               NOT ACTIVATED
IMPLEMENTATION_READY:            YES  (microfase 2.2C autorizada)
```

| ID | Resultado | Precisión vinculante |
|---|---|---|
| U-01 | PASS / RESOLVED | `StoredProcedureQuery` ejecuta sin transacción JPA exterior. |
| U-02 | PASS / RESOLVED | La fila canónica es alcanzable con `execute()/getResultList()/getUpdateCount()/hasMoreResults()`, con y sin update counts previos; la ausencia de canal canónico es observable por estado (0 filas, sin excepción). |
| U-03 | **RESOLVED WITH FINDING** | Hibernate emite `{call dbo.usp_registrar_asistencias_sesion(?,?,?,?)}`: los nombres registrados **no** gobiernan el binding; **gobierna el orden de registro**. Decisión vinculante: el candidato usa binding **POSICIONAL** (1 `idSesion` UUID, 2 `asistenciaJSON` String/`nvarchar(max)`, 3 `idCorrelacion` UUID, 4 `idUsuarioEjecutor` UUID). Sin binding nominal productivo. Fuente del orden: `sys.parameters.parameter_id`; se congela en código y se protege por IT (CMD-PAR-013), sin consultar metadata DB en runtime. |
| U-04 | PASS / RESOLVED | `null` en `uniqueidentifier` se comporta como en el baseline (`GEN_002`). |
| U-05 | PASS / RESOLVED | `nvarchar(max)`: JSON > 4000 caracteres llega sin truncar. |
| D4 | **VALIDATED IN LB-002.2B** | `StoredProcedureQuery` funciona sin transacción JPA exterior. **No** introducir `@Transactional`, `EntityTransaction.begin` ni `JpaTransactionManager`. |
| R-06 | ACTUALIZADO | Cobertura vigente medida en el HEAD de 2.2B ([BASELINE](BASELINE.md)): LINE **90.53 %**, BRANCH **80.81 %**. El 70.50 % ya no es la cobertura actual (era de la fase 1B). Sigue vigilándose en cada `clean verify`. |

### Separación explícita de etapas de prueba

| Etapa | Qué es | Puede terminar en |
|---|---|---|
| BASELINE | `clean verify` sin DB/Azure antes de añadir cualquier test | `PASS \| BASELINE_REGRESSION \| ENVIRONMENT_BLOCKER` |
| FEASIBILITY | Probes contra SQL Server real (`JpaAttendanceStoredProcedureFeasibilityIT`, test-only, sin candidato productivo) que responden si Hibernate/Jakarta/`StoredProcedureQuery` consumen el SP congelado | `PASS \| FAIL \| STOP: NO_CLEAN_JPA_PATH \| BLOCKED_BY_ENVIRONMENT` |
| ACCEPTANCE RED | Tests sobre comportamiento observable del sistema actual y costuras existentes, que fallan porque el command provider JPA independiente aún no existe; más caracterización PASS separada | RED confirmado (fallo por comportamiento, no por clase inexistente) |
| GREEN | Implementación mínima sin tocar el RED (microfase 2.2C) | — |
| VALIDATION | `clean verify` + ITs dirigidos + paridad (2.2D) | — |

Resultados de 2.2B en: [BASELINE.md](BASELINE.md), [FEASIBILITY.md](FEASIBILITY.md), [RED_SNAPSHOT.md](RED_SNAPSHOT.md).

Decisiones de diseño y alternativas: [DECISION](DECISION.md). Matriz de pruebas: [TEST_PLAN](TEST_PLAN.md). Fase previa: [LB-002.1B-REPORT](../LB-002.1B-REPORT.md). Autorización del objetivo: [LINEA_BASE](../../../baseline/LINEA_BASE.md) (fila LB-002) y [TD-043 §LB-002.1C](../../../baseline/TECHNICAL_DEBT.md#td-043).

## Objetivo y business capability

**Capability:** registrar en lote la asistencia de una sesión (AN/SJC/EX) — `POST /api/v1/asistencias/lote`, comando del Golden Path ([GOLDEN_PATH_ASISTENCIA](../../../baseline/GOLDEN_PATH_ASISTENCIA.md)).

**Objetivo del piloto (2.2B en adelante):** demostrar que el mismo `dbo.usp_registrar_asistencias_sesion` puede invocarse desde Infrastructure con JPA (`EntityManager`/`StoredProcedureQuery`) con comportamiento observable equivalente al baseline JDBC, seleccionable y reversible por Composition Root, sin cambiar HTTP, Application, Domain, DB ni el SP.

**No es** "SP → reglas de negocio Java". El SP se conserva. No se duplica en Java ninguna de estas reglas: validación del lote (JSON, array, no vacío, shape exacto, sin duplicados), atomicidad, ownership/titularidad (`SEC_001/SEC_002`), matrícula activa (`EST_004`), catálogo AN/SJC/EX (`RC_001`), mapeo `asistio`, idempotencia, concurrencia, rollback SQL.

## Variable principal, baseline y target

| | Baseline (se conserva todo el piloto) | Target del piloto |
|---|---|---|
| Ruta | `AsistenciaRepositorySqlServerAdapter.registrarAsistenciasSesion` → `CanonicalStoredProcedureExecutor.execute` → `EXEC dbo.usp_registrar_asistencias_sesion @idSesion=…, @asistenciaJSON=…, @idCorrelacion=…, @idUsuarioEjecutor=…` → SQL Server | `AsistenciaJpaCommandPersistence` (NUEVA) → `EntityManager.createStoredProcedureQuery("dbo.usp_registrar_asistencias_sesion")` con los mismos 4 parámetros → **el mismo SP** → SQL Server |
| Selección | default `jdbc` | `app.adapters.persistence.asistencia-command-provider=jpa` (default `jdbc`) |
| Traducción | `DbExceptionTranslator` vía executor | `DbExceptionTranslator` vía `CanonicalProcedureResultValidator` (NUEVA) |

## Alcance

- Candidato JPA de **un** command; selector independiente del de query; EMF reutilizable; validador de resultado canónico; tests RED→GREEN (unit/config/ArchUnit) y **paridad en SQL Server real**; documentación y evidencia del work item.
- Default del command = `jdbc` durante todo el piloto; rollback practicable a JDBC.

## Fuera de alcance

Cambios en DB/SP/tablas/vistas/parámetros/códigos; frontend; OpenAPI/HTTP/DTO/controllers/Input Ports/Use Case/Domain/`AsistenciaRepositoryPort`; `SecurityConfig`; `pom.xml` (JPA ya está); migrar otros commands (`registrarAsistenciaAutonoma`, `solicitarRevisionAsistencia`, `resolverSolicitudRevisionAsistencia`, `registrarAsistencia`); `uv_auth_*`, `SESSION_CONTEXT`, `usp_consultar_grupos_paginado`; activar el command JPA en `local`/`dev`/prod (fase posterior aparte); retirar JDBC; dual-write/shadow write; Redis, RabbitMQ, WebFlux, serverless, IaC; **resolver TD-043**; Azure integration; refactors no relacionados.

## Fuentes de verdad leídas

`AGENTS.md`; [LINEA_BASE](../../../baseline/LINEA_BASE.md); [TECHNICAL_DEBT](../../../baseline/TECHNICAL_DEBT.md) (TD-043); [GOLDEN_PATH_ASISTENCIA](../../../baseline/GOLDEN_PATH_ASISTENCIA.md); [JDBC_TO_JPA](../../../persistence/JDBC_TO_JPA.md); [ADR-002](../../../adr/ADR-002-jpa-incremental.md); [BACKEND_GOLDEN_PATH_CONTRACT](../../../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md); [DB_BASELINE_CONTRACT](../../../contracts/external/db/DB_BASELINE_CONTRACT.md); [OpenAPI canónico](../../../contracts/openapi/openapi-golden-path.yaml); [adapter-composition-standard](../../../architecture/adapter-composition-standard.md); evidencia LB-002.0/1/1B/1C; código y tests citados en cada sección.

Hashes verificados en 2.2A (`sha256sum`, solo lectura), idénticos a los congelados:

```text
DB_BASELINE_CONTRACT.md            45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec
openapi-golden-path.yaml           72a3097bbae2a296ed6690bf89c239f56697764f25100b930d941a699736da54
BACKEND_GOLDEN_PATH_CONTRACT.md    9b4830b468b58b49793658454a7b34deec20d2d9681943aa8f1e3942b15d8c62
```

## AS-IS comprobado en código

| Hecho | Evidencia |
|---|---|
| Puerto: `void registrarAsistenciasSesion(RegistrarAsistenciasSesionRepositoryDTO)`; DTO = `(UUID sesion, List<RegistroAsistenciaSesionRepositoryDTO> registros, UUID usuarioEjecutor)` | [AsistenciaRepositoryPort](../../../../src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/repository/AsistenciaRepositoryPort.java) |
| Baseline: `dto==null` → `CrosscuttingException("El dominio para registrar asistencias por sesion es obligatorio.")`; llama al executor con operación `registrarAsistenciasSesion`, params `idSesion=dto.sesion()`, `asistenciaJSON=serializarRegistros`, `idCorrelacion=CorrelationIdContext.require()`, `idUsuarioEjecutor=dto.usuarioEjecutor()` (sin transformar, incluso `null`) | [AsistenciaRepositorySqlServerAdapter](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/core/AsistenciaRepositorySqlServerAdapter.java) |
| JSON: `List<Map<String,String>>` con `Map.of("idEstudiante", uuid.toString(), "estado", estado)` serializado con Jackson 3 (`tools.jackson`); **el orden de claves de `Map.of` no es determinista entre JVM** | mismo archivo, `serializarRegistros` |
| Executor: `jdbcOperations.query(sql, params, mapper)`; exige `results.size()==1` (`ERR_DB_CANONICAL_CONTRACT`), `idCorrelacion` devuelto == contexto (`ERR_DB_CANONICAL_CONTRACT`), luego `DbExceptionTranslator.throwIfFailed`; `DataAccessException` → `DatabaseOperationException(DATABASE_OPERATION_ERROR, "No fue posible ejecutar el procedimiento almacenado.")` | [CanonicalStoredProcedureExecutor](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/support/procedure/CanonicalStoredProcedureExecutor.java) |
| Traducción: `DbTechnicalError` (`^DBCODE=([A-Z0-9_]+)\|(.*)$`) → `DbFailureClassifier` (package-private) → `SEC_001/SEC_002/EST_004`→Forbidden; `ATT_001/002/003, GEN_002, RC_001, SES_004`→Validation; `SES_001`→NotFound; `SES_003`→FeatureUnavailable; desconocido→`ERR_DB_UNCLASSIFIED` (500). `DbExceptionTranslator.throwIfFailed` es `public static` | [support/error](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/support/error/DbExceptionTranslator.java) |
| Firma real del SP (tipos y orden por `parameter_id`): `@idSesion uniqueidentifier`, `@asistenciaJSON nvarchar`, `@idCorrelacion uniqueidentifier`, `@idUsuarioEjecutor uniqueidentifier` (con `= NULL`). El `.md` del contrato DB solo nombra los 4 parámetros; tipos/orden constan en ITs de contrato | `GoldenPathSqlStoredProcedureContractIT`, `SqlStoredProcedureContractIT` |
| Transacción: el adapter/Use Case no usan `@Transactional`/`TransactionOperations`; el SP gestiona su transacción; commit ocurre dentro del SP | Javadoc del Use Case y de `AsistenciaRepositorySqlServerIT` |
| Use Case: resolver grupo de la sesión → `InstitutionalScopePort.canDocenteAccessGrupo` (403 si no) → `registrarAsistenciasSesion` → **solo después** `RealtimePublisherPort.publish(ASISTENCIAS_SESION_ACTUALIZADAS)` | [RegistrarAsistenciasSesionUseCaseImpl](../../../../src/main/java/co/edu/uco/asistenciasuco/application/features/asistencia/registrarasistenciassesion/usecase/impl/RegistrarAsistenciasSesionUseCaseImpl.java); test `ejecucion_exitosa_persiste_y_luego_publica_en_ese_orden_exacto` |
| Composition Root: un único `AsistenciaRepositoryPort`; `switch` sobre `asistencia-query-provider`; `JPA` → `AsistenciaRepositoryHybridSqlServerAdapter(jdbc, AsistenciaJpaQueryPersistence)`; **todos los commands, JDBC** | [SqlServerCoreRepositoryAdapterConfiguration](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/adapters/persistence/sqlserver/SqlServerCoreRepositoryAdapterConfiguration.java) |
| EMF: creado solo con `query=jpa` (`@ConditionalOnProperty`); `hbm2ddl=none`, `show_sql=false`; sin `JpaTransactionManager`; auto-config JPA de Boot excluida; 3 entidades de vista `@Immutable` | [SqlServerJpaAsistenciaQueryAdapterConfiguration](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/adapters/persistence/sqlserver/SqlServerJpaAsistenciaQueryAdapterConfiguration.java), `AsistenciasUcoApplication` |
| Config: `application.yml` default `jdbc`; `application-local.yml`/`application-dev.yml` fijan `jpa` para la query; no hay `application-prod.yml` | `src/main/resources` |
| Tests existentes relevantes: `AsistenciaRepositorySqlServerIT` (6, SQL real: AN/SJC/EX, roundtrip EX, usuario ejecutor, docente ajeno, `ABC`, lote mixto atómico), `AsistenciaRepositorySqlServerAdapterTest`, `AsistenciaQueryJpaParityIT` (patrón de paridad), `AsistenciaQueryProviderCompositionRootTest`/`ActivationTest`, `SqlServerJpaAsistenciaQueryConfigurationTest`, `JpaIsolationRulesTest`, `RegistrarAsistenciasSesionUseCaseImplTest` | `src/test/**` |

**Hallazgo documental (no conflicto):** el OpenAPI declara `x-idempotency: NOT_DEFINED` (no hay `Idempotency-Key` HTTP) mientras el contrato DB exige escritura "atómica e idempotente". Son ámbitos distintos: la idempotencia **DB** (mismo lote repetido ⇒ mismo estado persistido, una cabecera y un detalle por `(estudianteGrupo, sesion)`) debe preservarse y se prueba; la HTTP sigue sin definirse y no se toca.

## Diseño TARGET

```text
Controller → InputPort → UseCase ─(sin cambios)→ AsistenciaRepositoryPort
                                                    │
                       Composition Root (selectors independientes: query-provider, command-provider)
                                                    │
   (jdbc,jdbc) → AsistenciaRepositorySqlServerAdapter                       ← default, sin envoltorio
   otro caso   → AsistenciaRepositoryHybridSqlServerAdapter
                     ├─ query   → AsistenciaQueryPersistence   (JDBC baseline | AsistenciaJpaQueryPersistence)
                     ├─ command → AsistenciaCommandPersistence (JDBC baseline | AsistenciaJpaCommandPersistence)   ← NUEVO
                     └─ demás commands → AsistenciaRepositorySqlServerAdapter (siempre JDBC)
```

- **Mecanismo (D1):** `EntityManager`/`StoredProcedureQuery`, no `@Procedure`; razones en [DECISION D1](DECISION.md#d1-mecanismo-jpa-entitymanagerstoredprocedurequery-vs-spring-data-procedure).
- **Selectors (D2):** [DECISION D2](DECISION.md#d2-selector-del-command-y-su-relación-con-el-selector-de-query): `asistencia-command-provider=jdbc|jpa`, default `jdbc`, fail-closed, independiente, matriz 2×2, `profile != provider`.
- **EMF (D3):** un solo EMF, activo con `query=jpa OR command=jpa` mediante `JpaCapabilityRequiredCondition` (NUEVA); `(jdbc,jdbc)` no arranca Hibernate. [DECISION D3](DECISION.md#d3-estrategia-del-entitymanagerfactory).
- **Transacción (D4), errores (D5), rollback (D6):** ver DECISION. Resumen: sin transacción JPA (autocommit como el baseline), un EM por invocación cerrado en `try-with-resources`, traducción por `DbExceptionTranslator` sin tocar el executor, rollback por variable de entorno + reinicio.

## Contrato DB preservado (inmutable)

`dbo.usp_registrar_asistencias_sesion(@idSesion, @asistenciaJSON, @idCorrelacion, @idUsuarioEjecutor)` verificado documentalmente contra [DB_BASELINE_CONTRACT §Attendance Golden Path](../../../contracts/external/db/DB_BASELINE_CONTRACT.md): request de 4 campos; ítem JSON exacto `{"idEstudiante": UUID, "estado": "AN|SJC|EX"}`; JSON no nulo/válido/array top-level; lote no vacío; objeto con exactamente esas dos claves; sin duplicados; lote parcial permitido; omitido ⇒ sin registro automático (**ausencia ≠ AN**); matrícula activa `EstudianteGrupo.estado.codigo='A'`; docente ejecutor titular; validar todo antes de escribir; escritura atómica e idempotente; concurrencia ⇒ una cabecera y un detalle; `AN→asistio=1`, `SJC→0`, `EX→0`; `idUsuarioEjecutor=NULL` ⇒ `GEN_002`; canal `DBCODE=<code>|<detalle>` interno. Códigos: `ATT_001/002/003`, `RC_001`, `SES_001`, `SES_004`, `GEN_002`, `EST_004`, `SEC_001/002`. Fuentes consistentes entre sí (contrato `.md`, adapter JDBC, ITs de firma): **no hay `CONTRACT_CONFLICT`**. Brecha menor registrada: tipos/orden de parámetros no están en el `.md` (sí en los ITs de contrato) — se re-ejecuta `GoldenPathSqlStoredProcedureContractIT` como precondición de 2.2B/2.2C.

## Contrato HTTP y Application preservados

`POST /api/v1/asistencias/lote`: request (`sesionId`, `registros[{estudianteId, estado}]`), `201 ApiMessageResponse`, errores y OpenAPI **sin cambios**. Sin cambios en controllers, DTO HTTP, Input Ports, Use Case, Domain ni `AsistenciaRepositoryPort`. **Cero** tipos JPA/Hibernate/Spring Data en Domain/Application (ArchUnit vigente `JpaIsolationRulesTest`). La autorización sigue en Application (`InstitutionalScopePort`); no se mueve al repository; el SP repite `SEC_002` como defensa en profundidad.

## Invariantes

1. Puerto, Use Case, HTTP, OpenAPI, DB y SP intactos; hashes iguales al final de cada microfase.
2. Un provider por invocación; sin dual-write ni shadow write productivo; `profile != provider`.
3. Default del command `jdbc`; valor desconocido falla el arranque.
4. `AsistenciaRepositorySqlServerAdapter`, `CanonicalStoredProcedureExecutor`, `DbFailureClassifier`/`DbExceptionTranslator` (lógica) y sus tests existentes **no se modifican ni se borran**; los tests existentes no se editan para hacerlos pasar.
5. `idUsuarioEjecutor` proviene del principal autenticado (Application) y viaja sin transformación; el candidato nunca lo deriva ni lo sustituye.
6. Ausencia de fila ≠ AN; el candidato no sintetiza estados.
7. Publicación SSE solo tras persistencia exitosa; SSE no es fuente de verdad; sin transacción distribuida SQL↔SSE.
8. Sin `SESSION_CONTEXT`, sin `uv_auth_*`, sin `JpaTransactionManager`, sin `ddl-auto` distinto de `none`, `open-in-view=false`.
9. Ningún secreto, ni el payload JSON, ni IDs de estudiantes en logs.

## Seguridad

Sin cambios de roles (DOCENTE) ni de scope. Riesgos específicos del candidato: (a) sustituir/omitir `usuarioEjecutor` ⇒ bypass de titularidad (cubierto por CMD-PAR-007/008); (b) filtrar en logs el payload o mensajes de Hibernate con SQL/parámetros (cubierto por CMD-ADP-011); (c) exponer excepciones JPA al HTTP (se traducen a `DatabaseOperationException` genérica). No se usan credenciales nuevas; el EMF reutiliza el `DataSource` existente.

## Observabilidad

`correlationId` obligatorio (`CorrelationIdContext.require()`), enviado como `@idCorrelacion` y verificado contra el eco del SP; logs con el mismo formato y operación (`registrarAsistenciasSesion`) que el baseline, sin payload; `show_sql=false`; sin métricas Hibernate (auto-config excluida); sin nuevos timeouts (el baseline no define). Opcional no bloqueante: una línea INFO al construir el contexto con `query=…, command=…` para verificar el rollback en operación (sin datos sensibles).

## Realtime

Sin cambios: el Use Case publica `ASISTENCIAS_SESION_ACTUALIZADAS` (`{grupo, sesion, totalRegistros}`) solo tras retorno exitoso del port; falla ⇒ no publica. La migración de adapter no debe alterar esa semántica; se verifica con los tests unitarios existentes (regresión, no modificados) y un IT con publisher espía sobre SQL Server real ([TEST_PLAN §D](TEST_PLAN.md#d-realtime-semántica-de-publicación)).

## Fixtures (resumen; detalle en TEST_PLAN)

SQL Server real `sql_server_asistencias` / `gestionasistenciadb` con freeze desplegado (precondición del usuario: `test_summary.ps1` PASS). Fixture autocontenido con prefijo propio, sin `assumeTrue`, sin H2 ni mocks del SUT, cleanup FK-safe en `finally`. Paridad: **dos sesiones aisladas del mismo grupo** (A para JDBC, B para JPA) con estado inicial idéntico; comparación de resultado observable normalizado **y** contra oráculo contractual absoluto (para no aceptar "ambos mal igual"). Nunca se ejecutan ambos caminos sobre la misma petición.

## Estrategia de fases (propuesta; ninguna se inicia automáticamente)

| Microfase | Contenido | Clase |
|---|---|---|
| **2.2A** (esta) | DoR: PLAN, DECISION, TEST_PLAN | DOCUMENTATION_ONLY |
| 2.2B | BASELINE `clean verify` en el HEAD de partida → FEASIBILITY probes U-01..U-05 (SQL real, test-only) → ACCEPTANCE RED sobre costuras existentes + caracterización + `RED_SNAPSHOT`. Solo archivos NUEVOS bajo `src/test` y docs del work item; sin producción | tester RED |
| 2.2C | Implementación mínima GREEN (sin tocar RED) | PERSISTENCE_MIGRATION |
| 2.2D | Validación: `clean verify`; ITs dirigidos SQL real; paridad; `VALIDATION.md` | validación |
| 2.2E | (Solo con decisión aparte) activar `command=jpa` en `local`/`dev` + E2E HTTP con SSE | CONFIG_ACTIVATION |
| Cierre | `CLOSURE.md`, DoD, TD/MV, actualización de `LINEA_BASE`/`JDBC_TO_JPA` | cierre |

## Archivos que probablemente se modificarán después (propuesta, no ejecutada)

**EXISTENTES** (rutas comprobadas):

- `src/main/java/…/infrastructure/config/adapters/persistence/sqlserver/SqlServerCoreRepositoryAdapterConfiguration.java` — segundo selector, composición 2×2.
- `src/main/java/…/infrastructure/config/adapters/persistence/sqlserver/SqlServerJpaAsistenciaQueryAdapterConfiguration.java` — condición del EMF y Javadoc.
- `src/main/java/…/sqlserver/core/AsistenciaRepositoryHybridSqlServerAdapter.java` — constructor adicional (el de 2 args se conserva).
- `src/main/resources/application.yml` — `asistencia-command-provider` con default `jdbc`. (`application-local.yml`/`-dev.yml` **no** cambian en el piloto.)
- `.env.example` — línea comentada de la nueva variable.
- Docs: `docs/architecture/adapter-composition-standard.md`, `docs/persistence/JDBC_TO_JPA.md`, `docs/baseline/LINEA_BASE.md`, `docs/baseline/TECHNICAL_DEBT.md` (solo evidencia en TD-043), `AsistenciasUcoApplication` (comentario). Skill `uco-persistencia` solo si falta una regla.

**NUEVOS** (propuesta de nombres, no definitivos): `sqlserver/core/AsistenciaCommandPersistence`; `sqlserver/jpa/AsistenciaJpaCommandPersistence`; `sqlserver/jpa/…SesionJsonSerializer` (o método privado); `sqlserver/support/procedure/CanonicalProcedureResultValidator`; `config/…/JpaCapabilityRequiredCondition`; tests listados en el TEST_PLAN.

**NO se modifican:** `pom.xml`, `AsistenciaRepositoryPort`, Application, Domain, controllers, OpenAPI, `SecurityConfig`, DB, frontend, `AsistenciaRepositorySqlServerAdapter`, `CanonicalStoredProcedureExecutor`, tests existentes.

## Incognitas abiertas (U-xx)

**Reclasificación (revisión 2.2A):** U-01..U-05 son **JPA STORED PROCEDURE FEASIBILITY PROBES**, no RED (resultado `PASS | FAIL | STOP: NO_CLEAN_JPA_PATH | BLOCKED_BY_ENVIRONMENT`). Solo un probe contra SQL Server real las resuelve; ninguna se "asume" en el diseño. Cada una tiene su test en el TEST_PLAN y, si falla sin salida limpia, su STOP. U-06..U-08 se resuelven en microfases posteriores (dependen de colaboradores productivos aún inexistentes).

| ID | Incógnita | Riesgo si falla | Resuelve | STOP |
|---|---|---|---|---|
| U-01 | Hibernate 7.2 ejecuta `StoredProcedureQuery` sin transacción activa (autocommit, como JDBC) | Exigir transacción anida el `BEGIN TRAN` del SP y altera rollback/`@@TRANCOUNT` | CMD-PAR-001/010 | Sí (`NO_CLEAN_JPA_PATH`) |
| U-02 | Result set canónico accesible si el SP emite update counts previos (sin `SET NOCOUNT ON`) | Fila canónica perdida ⇒ falso `ERR_DB_CANONICAL_CONTRACT` | CMD-PAR-001 | Sí |
| U-03 | Binding con nombre (`@param`) resuelto por `mssql-jdbc`/Hibernate (y coste de metadata) | **RESUELTA CON HALLAZGO (2.2B):** el orden de registro gobierna; binding POSICIONAL vinculante, guardado por CMD-PAR-013 | CMD-PAR-001, CMD-PAR-013 | No |
| U-04 | `null` en parámetros `uniqueidentifier` (`idUsuarioEjecutor`, `idSesion`) se enlaza como en JDBC | Error de conversión implícita en vez de `GEN_002` | CMD-PAR-008 | Sí si difiere del baseline |
| U-05 | `nvarchar` (¿`max`?) acepta JSON > 4000 caracteres sin truncar | Truncamiento silencioso de lotes grandes | CMD-PAR-011 | Sí |
| U-06 | Excepción exacta de Hibernate cuando no hay result set / 0 filas | Traducción distinta de `ERR_DB_CANONICAL_CONTRACT` | CMD-ADP-008 (unit) | No (se fija por mapeo) |
| U-07 | Tipos Java de las 4 columnas en `Object[]` (`String`/`UUID`, `Boolean`) | `ClassCastException`/falso fallo | CMD-ADP-007 + CMD-PAR-001 | No (`JdbcValueMapper` tolerante) |
| U-08 | Auditoría DB observable por `idCorrelacion` (contrato: el Golden Path "no duplica auditoría dentro del SP") | Aserción sin sustento | CMD-PAR-016 | No: `NOT_OBSERVABLE`, no PASS |

## Riesgos

| ID | Riesgo | Mitigación / detección |
|---|---|---|
| R-01 | Candidato altera atomicidad por añadir transacción | D4; CMD-PAR-004/005/010 |
| R-02 | Divergencia de traducción de errores JPA vs JDBC | Validador + `DbExceptionTranslator`; CMD-ADP-005..010, CMD-PAR-003..006 |
| R-03 | JSON distinto (orden/forma) | Paridad sobre JSON parseado y oráculo de shape; CMD-ADP-002/003 |
| R-04 | EMF ausente con `query=jdbc, command=jpa` | D3; CMD-CFG-004 |
| R-05 | EMF duplicado o `JpaTransactionManager` reaparece | CMD-CFG-005/006 |
| R-06 | Cobertura BRANCH (**vigente 80.81 % / LINE 90.53 %** en el HEAD de 2.2B; el 70.50 % de la fase 1B ya no es la cobertura actual) cae por código nuevo | Tests de comportamiento para toda rama nueva; sin tests cosméticos; verificar en cada `clean verify` |
| R-07 | Fuga de conexiones si falla el candidato | `try-with-resources`; CMD-PAR-014 |
| R-08 | Fixtures dejan residuos en DB compartida | Prefijo propio, cleanup en `finally`, verificación de conteo final |
| R-09 | TD-043 enmascara resultados (`-Pintegration verify` global rojo) | ITs dirigidos; reporte honesto; nunca "PASS global" |
| R-10 | Concurrencia: dos escrituras sobre la misma sesión/estudiante | CMD-PAR-012 (contrato: una cabecera, un detalle) |
| R-11 | Log de payload/IDs o SQL de Hibernate | CMD-ADP-011 |
| R-12 | Baseline `clean verify` en `0bfc02a` **no re-ejecutado** en 2.2A | Precondición P-2 de 2.2B |
| R-13 | Nombre `asistencia-command-provider` sugiere alcance mayor al real | **CERRADO por decisión del revisor:** se conserva el nombre; durante LB-002.2 gobierna solo `registrarAsistenciasSesion`. Documentar + CMD-CFG-007 |
| R-14 | `Map.of` no determinista produce falsos rojos en comparación literal | Comparar JSON parseado; candidato con orden fijo |
| R-15 | Ejecutar `-Pintegration verify` con selección dirigida puede requerir ajustar el gate JaCoCo del subconjunto | Confirmar en 2.2B; no relajar gates del `clean verify` normal |

## Gates (para las microfases posteriores)

- **Normal:** `.\mvnw.cmd clean verify` → exit 0; tests unit/ArchUnit/OpenAPI verdes; JaCoCo LINE ≥ 80 % y BRANCH ≥ 70 %; **sin depender de SQL Server ni Azure**.
- **Integración SQL Server:** `.\mvnw.cmd -Pintegration verify` (Failsafe `**/*IT.java`, `application-integration`). **Hoy `GLOBAL_INTEGRATION_PROFILE = NOT_GREEN_TD043`** (6 fallos preexistentes ajenos: `SqlStoredProcedureContractIT`×3, `GrupoRepositorySqlServerIT`×2, `UsuarioPasswordHashSqlServerIT`×1, por SP inexistentes de TD-043). No se declara PASS global mientras siga así. Los ITs del command (`AsistenciaRepositorySqlServerIT`, `GoldenPathSqlStoredProcedureContractIT` y los nuevos) se ejecutan de forma dirigida y reportan su propio resultado sin ocultar el global. Comando dirigido a confirmar en 2.2B (p. ej. selección Failsafe `-Dit.test=…`).
- Azure integration: fuera de alcance.

## TD-043 (explícitamente fuera de alcance)

Las tres SP ausentes (`usp_sincronizar_usuario`, `usp_registrar_o_actualizar_plan_estudio`, `usp_registrar_estudiante_en_grupo_usuario_no_existente`) **no cruzan** el command: `usp_registrar_asistencias_sesion` existe, su firma está verificada y `AsistenciaRepositorySqlServerIT` (6/6) y `GoldenPathSqlStoredProcedureContractIT` (16/16) son Golden Path. No se corrige, no se deshabilita ningún IT, no se cambian expectativas, no se declara GREEN el perfil global. Si en 2.2B/C apareciera un cruce real ⇒ STOP.

## Stop conditions

Evaluadas en 2.2A — **ninguna activa**:

| Condición | Estado 2.2A |
|---|---|
| `CONTRACT_CONFLICT` / `TEST_CONTRACT_CONFLICT` sin resolución | No detectado (fuentes DB/backend/IT consistentes; ver §Contrato DB) |
| DB snapshot insuficiente / firma distinta entre fuentes | No: firma idéntica en `.md`, adapter y ITs (tipos/orden solo en ITs: brecha menor registrada) |
| `AsistenciaRepositoryPort` requeriría cambiar | No: `registrarAsistenciasSesion(dto)` sirve tal cual |
| JPA obliga a cambiar HTTP | No |
| JPA obliga a duplicar reglas DB en Application | No: SP y `DbExceptionTranslator` se reutilizan |
| Sin forma limpia de mantener DBCODE/errores | No: `DbExceptionTranslator` es público; clasificación única |
| Candidato necesitaría dual-write | No |
| Se requeriría modificar DB / frontend | No |
| TD-043 cruza el command | No |
| Baseline Git distinto del esperado | No (`0bfc02a…` == `origin/develop` local) |

Activas en microfases posteriores: `STOP: NO_CLEAN_JPA_PATH` si U-01/U-02/U-04/U-05 fallan sin salida limpia; cualquier hallazgo que exija tocar puerto/Application/DB/SP/HTTP; cualquier necesidad de dual-write; `TEST_CONTRACT_CONFLICT` no resuelto; regresión de gates (LINE/BRANCH) no atribuible.

## Definition of Ready

**2.2A → `READY_FOR_REVIEW`** (documental): objetivo, alcance, fuentes, AS-IS con evidencia, TARGET, riesgos, rollback y stop conditions definidos; hashes verificados; sin `CONTRACT_CONFLICT`/`BLOCKED_BY_MISSING_EVIDENCE` relevante.

**Implementación (2.2B en adelante) → `NOT_READY`** hasta cumplir todas:

- P-1 Aprobación humana versionada de PLAN, DECISION y TEST_PLAN (decisión, responsable, referencia), incluidas la decisión pendiente del nombre del selector (R-13) y las decisiones de proceso Q-1 (forma del RED para colaboradores nuevos) y Q-2 (soporte de fixture) del TEST_PLAN.
- P-2 `clean verify` verde re-ejecutado en el HEAD de partida (el estado registrado en LINEA_BASE, 1067 tests, es de la fase 1B).
- P-3 SQL Server real `sql_server_asistencias`/`gestionasistenciadb` con freeze desplegado y `test_summary.ps1` PASS (ejecuta el usuario); `GoldenPathSqlStoredProcedureContractIT` re-ejecutado.
- P-4 Autorización explícita de la microfase siguiente (ninguna fase inicia sola).

## Definition of Done futuro (LB-002.2, no cumplido)

DoD única ([DEFINITION_OF_DONE](../../../baseline/DEFINITION_OF_DONE.md)) más: RED aprobado con `RED_SNAPSHOT` y sin modificar tras GREEN; `clean verify` exit 0 con LINE ≥ 80 % / BRANCH ≥ 70 %; ArchUnit verde (Application/Domain sin JPA); paridad SQL real JDBC vs JPA con **0 mismatches** en todos los escenarios del TEST_PLAN y oráculo contractual cumplido; default `command-provider=jdbc` probado; rollback a JDBC probado (Composition Root y arranque real); fail-closed probado; sin dual-write; commands restantes en JDBC; hashes OpenAPI/backend/DB iguales; DB/frontend/SP/puerto/Application/Domain/HTTP sin cambios; TD-043 declarada `OPEN` y `GLOBAL_INTEGRATION_PROFILE` reportado tal cual; `VALIDATION.md` y `CLOSURE.md`; JDBC **no** retirado. Estados finales solo con evidencia: `NOT_RUN` no es `PASS`; E2E frontend `NOT_RUN` mientras no se ejecute.
