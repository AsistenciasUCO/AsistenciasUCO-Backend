---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2 — TEST PLAN: JPA COMMAND PILOT `registrarAsistenciasSesion`

Microfase 2.2A: **solo diseño de pruebas; no se escribe ni ejecuta ninguna**. Secuencia posterior: REQUIREMENT → CONTRACT → TEST_PLAN → RED (2.2B) → GREEN (2.2C) → VALIDATE (2.2D) ([TESTING_STANDARD](../../../testing/TESTING_STANDARD.md)). Fuentes: [PLAN](PLAN.md), [DECISION](DECISION.md), [DB_BASELINE_CONTRACT §Attendance Golden Path](../../../contracts/external/db/DB_BASELINE_CONTRACT.md), [BACKEND_GOLDEN_PATH_CONTRACT](../../../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md), [OpenAPI canónico](../../../contracts/openapi/openapi-golden-path.yaml), patrón de [LB-002.1 TEST-PLAN](../LB-002.1-TEST-PLAN.md).

## Revisión 2.2A (APPROVED_FOR_2_2B) — etapas de prueba

```text
REVIEW RESULT: LB-002.2A → APPROVED_FOR_2_2B
```

Las pruebas de LB-002.2 se separan explícitamente en cinco etapas; **no se mezclan ni se llaman RED entre sí**:

| Etapa | Microfase | Contenido | Resultado posible |
|---|---|---|---|
| BASELINE | 2.2B | `.\mvnw.cmd clean verify` sin DB/Azure, **antes** de añadir cualquier test. Ver [BASELINE.md](BASELINE.md) | PASS · `BASELINE_REGRESSION` · `ENVIRONMENT_BLOCKER` |
| FEASIBILITY | 2.2B | Probes **U-01..U-05** con `JpaAttendanceStoredProcedureFeasibilityIT` (test-only, SQL Server real, sin candidato productivo). Certifican viabilidad del mecanismo, **no** el command. Ver [FEASIBILITY.md](FEASIBILITY.md) | PASS · FAIL · `STOP: NO_CLEAN_JPA_PATH` · `BLOCKED_BY_ENVIRONMENT` |
| ACCEPTANCE RED | 2.2B | RED-A..RED-D (fallan por comportamiento ausente) sobre costuras **existentes**; RED-E/RED-F son CARACTERIZACIÓN (deben PASS). Ver [RED_SNAPSHOT.md](RED_SNAPSHOT.md) | RED confirmado / no confirmado |
| GREEN | 2.2C | Implementación mínima sin tocar el RED; tests unitarios de colaboradores nuevos escritos tests-first tras congelar firmas | — |
| VALIDATION | 2.2D | `clean verify`, ITs dirigidos, paridad SQL real (`CMD-PAR-*`), `VALIDATION.md` | — |

**Decisiones de proceso resueltas:**

- **Q-1 (RESOLVED):** el ACCEPTANCE RED usa solo costuras productivas ya existentes (propiedades, `SqlServerCoreRepositoryAdapterConfiguration`, EMF condicional, puerto compuesto). No hay production skeletons ni tests que importen `AsistenciaJpaCommandPersistence`, `CanonicalProcedureResultValidator` o `JpaCapabilityRequiredCondition`. Sin `assertTrue(false)`, sin reflexión para "preguntar si existe la clase", sin `ClassNotFoundException` como contrato: un RED válido falla por **comportamiento observable** del sistema actual.
- **Q-2 (RESOLVED):** `AsistenciaRepositorySqlServerIT` no se modifica; fixture compartido = soporte nuevo bajo `src/test` o copia mínima.
- **U-01..U-05** dejan de ser "RED": son feasibility probes. U-06 (excepción exacta sin result set), U-07 (tipos de columnas: se observa también en el probe) y U-08 siguen para 2.2C+.
- **Alcance de 2.2B sobre la matriz:** los ~90 escenarios `CMD-ADP/PAR/RT/ARCH` siguen siendo el contrato de LB-002.2, pero 2.2B **no** los materializa cuando dependen de clases productivas inexistentes. Los tests unitarios de `AsistenciaJpaCommandPersistence`, `CanonicalProcedureResultValidator` y `JpaCapabilityRequiredCondition` se escriben tests-first en 2.2C tras autorizar sus firmas, sin modificar el RED congelado.
- **D5 (clarificación):** `CMD-ADP-008` cubre 0 filas y "sin result set" ⇒ `ERR_DB_CANONICAL_CONTRACT`; un fallo técnico verdadero ⇒ `DATABASE_OPERATION_ERROR`; una `IllegalStateException` de navegación no degrada una violación del contrato canónico a 500 técnico.
- **D4 (a validar):** `CMD-PAR-010` y el probe U-01 son quienes demuestran o refutan la ausencia de transacción exterior; nada la presupone.

## Revisión 2.2B (APPROVED_FOR_2_2C)

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

Efecto en la matriz: **CMD-PAR-013** declara que nombres/tipos/**orden** de `sys.parameters` son contrato obligatorio SIEMPRE (no solo un "fallback posicional"); **CMD-ADP-005** verifica el binding **posicional** 1..4; los `CMD-ADP/CFG/ARCH` aplicables a GREEN se materializan en 2.2C (unit/config/ArchUnit); `CMD-PAR-*`, `CMD-RT-002/003` y CMD-CFG-006 con SQL Server real siguen siendo 2.2D.

**Cómo leer la columna "Implementación incorrecta que atrapa":** es la respuesta concreta a "¿qué código equivocado haría fallar este test?". Si no se puede escribir una, el test no entra.

**Niveles:** **U** unit (fakes/mocks; *no certifica JPA*), **C** component (`ApplicationContextRunner`), **A** ArchUnit, **IT** SQL Server real (`-Pintegration`, sin H2, sin mock del repositorio ni del `EntityManager`).

Convención de IDs: `CMD-CFG` configuración/selector, `CMD-ADP` contrato del adapter, `CMD-PAR` paridad SQL real, `CMD-RT` realtime, `CMD-ARCH` arquitectura, `CMD-REG` regresión. `U-xx` = incógnita del PLAN que el test resuelve.

## Ejecución en LB-002.2C (solo lo realmente ejecutado)

Evidencia en [GREEN_SNAPSHOT](GREEN_SNAPSHOT.md): `clean verify` 1426 tests, 0 fallos/errores/skips; LINE 90.63 % / BRANCH 81.17 %.

| Grupo | Clase de test (Surefire) | IDs cubiertos (referenciados en el código del test) | Estado |
|---|---|---|---|
| CMD-CFG | `AsistenciaCommandProviderCompositionRootTest` (21), `JpaCapabilityRequiredConditionTest` (29), `AsistenciaRepositoryHybridCommandRoutingTest` (6) | CFG-001/002/003/004/005/007/008/009/010/011/012/013 (unit/component con fakes) | EJECUTADO — PASS |
| CMD-CFG-006 | parte de contexto real con DB (IT) | — | NOT_RUN — 2.2D (la parte unit/config se ejecuta; ver `AsistenciaCommandProviderCompositionRootTest`) |
| CMD-ADP | `AsistenciaJpaCommandPersistenceTest` (41), `CanonicalProcedureResultValidatorTest` (20) | ADP-001/002/005/007/008/010/011/012/013/014 (mocks; **no certifican** SQL real) | EJECUTADO — PASS |
| CMD-ADP-003/004/006/009 | — | sin clase que los referencie por ID en 2.2C; no se marcan como ejecutados | NO VERIFICADO POR ID |
| CMD-ARCH | `JpaCommandIsolationRulesTest` (9) + `JpaIsolationRulesTest` existente | ARCH-001/002/003 | EJECUTADO — PASS |
| CMD-REG | `clean verify` completo, hashes de contratos | REG-001 (parte unit), REG-003, REG-004, REG-005 | EJECUTADO — PASS |
| CMD-PAR-001..009,011,013,014,016..018 | `AsistenciaCommandJpaParityIT` (21 tests) | LB-002.2D, SQL Server real | **EJECUTADO — PASS, 0 mismatches** |
| CMD-PAR-010 | `AsistenciaCommandTransactionBoundaryIT` (3 tests, pool=1) | LB-002.2D, SQL Server real | **EJECUTADO — PASS** |
| CMD-PAR-012 | `AsistenciaCommandConcurrencyIT` (3 tests, 4 hilos) | LB-002.2D, SQL Server real | **EJECUTADO — PASS** |
| CMD-PAR-013 (firma/orden) | `JpaAttendanceStoredProcedureFeasibilityIT` + `AsistenciaCommandJpaParityIT` | guardia de orden posicional | **EJECUTADO — PASS** (`-Pintegration`, LB-002.2D) |
| CMD-PAR-015 | dentro de CMD-PAR-002 (`AsistenciaCommandJpaParityIT`) | readback JDBC+JPA | **EJECUTADO — PASS** |
| CMD-PAR-016 (correlación) | `AsistenciaCommandJpaParityIT` | ausencia de `ERR_DB_CANONICAL_CONTRACT` en ambos providers | **EJECUTADO — PASS** |
| CMD-PAR-016 (auditoría) | — | arnés llama al repositorio directo, sin interceptor de auditoría de controller | **NOT_OBSERVABLE** (justificado, LB-002.2D-VALIDATION) |
| CMD-RT-001/002/003 | `AsistenciaCommandRealtimeIT` (3 tests) | LB-002.2D, Use Case real + JPA candidato real | **EJECUTADO — PASS** |
| CMD-RT-004 / E2E | — (sin test JUnit; corrección de revisión humana eliminó el placeholder cosmético) | — | NOT_RUN — 2.2E |
| CMD-REG-001/002 (IT) | `AsistenciaRepositorySqlServerIT`, `GoldenPathSqlStoredProcedureContractIT` | LB-002.2D, ejecución dirigida | **EJECUTADO — PASS** (sin modificar estas clases) |

TD-043 permanece `OPEN / DEFERRED`; ver [LB-002.2D-REPORT](LB-002.2D-REPORT.md) para el resultado real
de `GLOBAL_INTEGRATION_PROFILE` tras esta fase.

## Integration Requirement

- REAL_PROVIDER_REQUIRED: **YES** (todo `CMD-PAR-*` y `CMD-RT-002/003`).
- ENVIRONMENT: SQL Server `sql_server_asistencias` / `gestionasistenciadb` con freeze DB desplegado y `test_summary.ps1` PASS (lo ejecuta el usuario). Sin credenciales en el repositorio: se cargan con el mecanismo local existente.
- MOCK_SUFFICIENT: **NO** para certificar el command. Los `CMD-ADP-*` (mocks) solo verifican wiring/traducción técnica; **no** certifican el candidato ni la equivalencia con SQL Server.
- WHY: `EntityManager`/`StoredProcedureQuery` mockeados no ejecutan el SP ni el binding de tipos, ni el transaccionamiento real, ni el result set canónico ([JDBC_TO_JPA §Paso D](../../../persistence/JDBC_TO_JPA.md)).
- Un IT obligatorio `NOT_RUN` **no** es `PASS`; `GLOBAL_INTEGRATION_PROFILE` sigue `NOT_GREEN_TD043` y se reporta tal cual.

## A. Unit / Configuration (`CMD-CFG-*`)

Contexto de ejecución: `ApplicationContextRunner` con el patrón de [`AsistenciaQueryProviderCompositionRootTest`](../../../../src/test/java/co/edu/uco/asistenciasuco/infrastructure/config/adapters/persistence/sqlserver/AsistenciaQueryProviderCompositionRootTest.java) (JDBC/EMF como fakes). Prop. = `app.adapters.persistence.asistencia-command-provider`.

| ID | Requirement | Scenario / Action | Observable | Expected | Implementación incorrecta que atrapa | Nivel |
|---|---|---|---|---|---|---|
| CMD-CFG-001 | Default = JDBC | Sin propiedad; invocar `registrarAsistenciasSesion` | qué fake se invoca; EMF | JDBC (`usp_registrar_asistencias_sesion` por `NamedParameterJdbcOperations`); EMF sin interacciones; el bean es el adapter JDBC sin envoltorio | Default `jpa`; Hibernate/EMF arrancando sin pedirlo; envoltorio que altera el default | C |
| CMD-CFG-002 | `jdbc` explícito = default | `command=jdbc` (con `query=jdbc` y `query=jpa`) | idem | JDBC para el command en ambos casos | `jdbc` ignorado cuando `query=jpa`; acoplar command a query | C |
| CMD-CFG-003 | `jpa` enruta **solo** el command | `command=jpa`, `query=jdbc`; invocar command y query | EMF usado por el command; JDBC.query del SP nunca; la query sigue por JDBC | command→persistencia JPA; query→JDBC; **un solo** `AsistenciaRepositoryPort` | Command sigue en JDBC; `command=jpa` activa también la query JPA; dos beans de puerto | C |
| CMD-CFG-004 | EMF disponible con `command=jpa` aunque `query=jdbc` | Contexto con `DataSource` fake, `query=jdbc`, `command=jpa` | existencia del bean `entityManagerFactory` | existe; el puerto se construye | `@ConditionalOnProperty` solo sobre query (bug AS-IS) ⇒ `NoSuchBeanDefinitionException`/arranque roto | C |
| CMD-CFG-005 | Un solo EMF y ninguno en el default | `(jdbc,jdbc)`, `(jpa,jdbc)`, `(jdbc,jpa)`, `(jpa,jpa)` | `getBeansOfType(EntityManagerFactory)` | 0, 1, 1, **1** | Segundo EMF para el command; EMF siempre creado; EMF ausente con algún `jpa` | C |
| CMD-CFG-006 | Sin `JpaTransactionManager` | Contexto real con `command=jpa` | beans `TransactionManager` | solo `JdbcTransactionManager` | Rehabilitar auto-config JPA de Boot ⇒ TM que reemplaza al JDBC (cambia semántica del resto de commands) | IT (contexto real, patrón `AsistenciaQueryProviderContextIT`) |
| CMD-CFG-007 | El selector solo migra `registrarAsistenciasSesion` | `command=jpa`; invocar `registrarAsistencia`, `registrarAsistenciaAutonoma`, `solicitarRevisionAsistencia`, `resolverSolicitudRevisionAsistencia` | interacciones con EMF | 0; delegan al JDBC (el primero mantiene `FeatureUnavailableException`) | Migrar otros commands por arrastre del nombre "command-provider" | C |
| CMD-CFG-008 | Fail-closed | `command=hibrido-inventado`, `command=""`, `command=hibernate`; y `query` inválido con `command=jpa` | `getStartupFailure()` y mensaje | falla el arranque; el mensaje nombra la propiedad; sin caer a otro provider | Caer a JDBC/JPA silenciosamente; parseo laxo | C |
| CMD-CFG-009 | Una sola regla de parseo | `"  JPA "`, `"Jpa"`, `"jdbc "` en ambos selectors | selector vs. condición del EMF | ambos coinciden (`JPA`/`JDBC`); nunca "el parser dice JPA y el EMF no existe" | Condición del EMF con comparación cruda distinta del parser | C + U (`JpaCapabilityRequiredConditionTest`) |
| CMD-CFG-010 | Independencia 2×2 | 4 combinaciones | tecnología observada de query y de command | tabla de [DECISION D2](DECISION.md#d2-selector-del-command-y-su-relacion-con-el-selector-de-query) | Un solo `switch` que acopla ambos selectors; combinación `(jdbc,jpa)` no soportada | C |
| CMD-CFG-011 | Perfil ≠ provider | Config REAL (`application.yml` + perfil `local`/`dev`/sin perfil) | tecnología del command | **jdbc** en los tres (la query sigue `jpa` en local/dev, `jdbc` sin perfil) | Fijar `command=jpa` en `application-local/dev.yml` | C (patrón [`AsistenciaQueryProviderActivationTest`](../../../../src/test/java/co/edu/uco/asistenciasuco/infrastructure/config/adapters/persistence/sqlserver/AsistenciaQueryProviderActivationTest.java)) |
| CMD-CFG-012 | Rollback por variable | perfil `local` + `APP_ADAPTERS_PERSISTENCE_ASISTENCIA_COMMAND_PROVIDER=jdbc` tras `=jpa` | routing | JDBC; el env gana; roll-forward a `jpa` vuelve a JPA | Propiedad mal enlazada al env; rollback que exige código | C |
| CMD-CFG-013 | El híbrido enruta cada método al delegado correcto y sin transformar | Los 6 métodos del puerto con DTO/`null` | delegado + argumento idéntico (misma instancia) | query→query; command de lote→command persistence; otros→baseline | Delegar todo al baseline; clonar/alterar el DTO | U (`AsistenciaRepositoryHybridCommandRoutingTest`) |
| CMD-CFG-014 | Compatibilidad con lo existente | Suites actuales de query/selector sin editar | resultados | siguen verdes | "Arreglar" tests existentes para acomodar el cambio | C (regresión = CMD-REG-004) |

## B. Contrato del adapter JPA (`CMD-ADP-*`) — mocks, **no certifican**

Se prueba `AsistenciaJpaCommandPersistence`/`CanonicalProcedureResultValidator` con `EntityManagerFactory`/`EntityManager`/`StoredProcedureQuery` como fakes. Complementan, jamás sustituyen, a `CMD-PAR-*`.

| ID | Requirement | Scenario / Action | Observable | Expected | Implementación incorrecta que atrapa | Nivel |
|---|---|---|---|---|---|---|
| CMD-ADP-001 | Precondición equivalente al baseline | `registrarAsistenciasSesion(null)` | excepción; interacción con EMF | `CrosscuttingException("El dominio para registrar asistencias por sesion es obligatorio.")`; **no** se crea EM | NPE/IAE; abrir EM antes de validar; otro texto | U |
| CMD-ADP-002 | JSON contractual exacto | lote AN/SJC/EX de 3 estudiantes; capturar el valor del parámetro `asistenciaJSON` y **parsearlo** | JSON parseado | array top-level; cada elemento tiene **exactamente** las claves `idEstudiante` (UUID en texto canónico) y `estado`; mismo orden que el DTO; sin claves extra | Añadir `observacion`/`asistio`; UUID en otro formato; objeto en vez de array; reordenar registros | U |
| CMD-ADP-003 | Equivalencia semántica con el baseline | mismo DTO por el adapter JDBC (executor mockeado, captura `MapSqlParameterSource`) y por el candidato | JSON **parseado** de ambos | iguales estructuralmente; **no** se compara texto literal (el baseline usa `Map.of`, orden de claves no determinista entre JVM) | Comparación literal (falso rojo intermitente); serializador con forma distinta | U |
| CMD-ADP-004 | Sin normalizar ni validar en Java | estados `"ABC"`, `" an "`, `"ex"` y lista vacía tal como llegan | JSON emitido | valores **idénticos** a los del DTO; la decisión (`RC_001`, etc.) queda en el SP | Candidato que hace `toUpperCase`/filtra/rechaza estados (duplica reglas DB/Application) | U |
| CMD-ADP-005 | SP y parámetros correctos | invocación con `usuarioEjecutor=U`, `sesion=S`, correlación `C` en `CorrelationIdContext` | nombre del SP; parámetros registrados y valores | SP `dbo.usp_registrar_asistencias_sesion`; binding **POSICIONAL** 1..4 en este orden: `1=idSesion=S` (UUID), `2=asistenciaJSON` (String), `3=idCorrelacion=C` (UUID), `4=idUsuarioEjecutor=U` (UUID); ningún parámetro nominal | Otro SP; intercambiar `idSesion`/`idCorrelacion`; usar `Docente.id`; correlación generada nueva | U |
| CMD-ADP-006 | `usuarioEjecutor` sin transformar | `usuarioEjecutor = null` | valor enlazado | `null` (la DB responde `GEN_002`); sin default, sin sustitución | Rellenar con UUID cero/otro usuario ⇒ evita `GEN_002` | U |
| CMD-ADP-007 | Mapeo tolerante del resultado | fila `Object[]` con `idCorrelacion` como `String` y como `UUID`; `estadoResultado` como `Boolean`, `Number` 1/0, `String` | `CanonicalProcedureResult` | valores correctos en todas las variantes; `estadoResultado` desconocido/`null` ⇒ fallo (fail-closed, igual que `JdbcValueMapper.toBoolean`) | `ClassCastException`; tratar `null` como éxito (**U-07**) | U |
| CMD-ADP-008 | Cardinalidad del canal canónico | 0 filas, 2 filas, y "sin result set" (excepción de Hibernate) | excepción | `DatabaseOperationException` con `ERR_DB_CANONICAL_CONTRACT` en 0 y 2 filas; para "sin result set", el mismo código (**U-06**) | Tomar la primera fila ignorando el resto; `NoSuchElementException` cruda; éxito silencioso con 0 filas | U (`CanonicalProcedureResultValidatorTest`) |
| CMD-ADP-009 | Correlación devuelta ≠ enviada | fila con `idCorrelacion` distinto | excepción | `DatabaseOperationException(ERR_DB_CANONICAL_CONTRACT)` | No validar el eco ⇒ el command "pasa" con una respuesta ajena | U |
| CMD-ADP-010 | Paridad de traducción de errores por `DBCODE` | filas idénticas `estadoResultado=0` con `DBCODE` = `SEC_001`, `SEC_002`, `EST_004`, `ATT_001`, `ATT_002`, `ATT_003`, `GEN_002`, `RC_001`, `SES_004`, `SES_001`, `SES_003`, `XYZ_999`, marcador malformado y texto legacy; cada una por (a) el executor JDBC con `NamedParameterJdbcOperations` mockeado y (b) el validador JPA | clase y `code` de la excepción | **idénticos** en (a) y (b): Forbidden, Validation, NotFound, FeatureUnavailable, `ERR_DB_UNCLASSIFIED` (500) | Reimplementar la clasificación en el candidato; mapear `SEC_002`→404; inferir por texto cuando hay `DBCODE` | U (parametrizado) |
| CMD-ADP-011 | Logging seguro | fallo técnico y rechazo de negocio con lote de UUIDs conocidos | salida de log capturada | contiene operación y `correlationId`; **no** contiene el JSON, IDs de estudiantes, `idUsuarioEjecutor`, SQL con parámetros ni mensajes de causa | Loguear `e.getMessage()`/payload/parámetros (PII/identificadores) | U (`OutputCapture`) |
| CMD-ADP-012 | Fallo técnico ⇒ excepción genérica equivalente | `PersistenceException`, `IllegalStateException`, `IllegalArgumentException` desde EM/`StoredProcedureQuery` | excepción; `EntityManager.close()` | `DatabaseOperationException(DATABASE_OPERATION_ERROR, "No fue posible ejecutar el procedimiento almacenado.", causa)`; EM cerrado | Propagar la excepción de Hibernate al HTTP; no cerrar EM ⇒ fuga | U |
| CMD-ADP-013 | Sin transacción JPA | éxito y fallo | interacciones con `EntityManager` | `getTransaction()`/`joinTransaction()` **nunca**; EM cerrado tras éxito | Envolver en transacción ⇒ anida el `BEGIN TRAN` del SP (**U-01**, riesgo R-01) | U (+ CMD-PAR-010 real) |
| CMD-ADP-014 | No se traga excepciones de negocio | validador lanza `ForbiddenException` dentro del bloque protegido | tipo de excepción | `ForbiddenException` intacta (no `DatabaseOperationException`) | `catch (RuntimeException)` demasiado amplio que degrada 403→500 | U |

## C. SQL Server real: paridad JDBC baseline vs JPA candidato (`CMD-PAR-*`)

### Diseño del arnés (`AsistenciaCommandJpaParityIT`, NUEVO, sufijo `IT`)

- Contexto `@SpringBootTest` con `command=jpa` (y query en JDBC para no mezclar variables; la lectura JPA se compara aparte en CMD-PAR-015). El **baseline** se construye manualmente con los mismos beans (`NamedParameterJdbcOperations` + `CanonicalStoredProcedureExecutor` → `AsistenciaRepositorySqlServerAdapter`); el **candidato** es el `AsistenciaRepositoryPort` compuesto por el Composition Root (prueba también el wiring).
- Fixture autocontenido con prefijo propio (p. ej. `IT-LB002-2-`), sin `assumeTrue`, sin H2, sin mocks del SUT; se reutiliza el enfoque de `AsistenciaRepositorySqlServerIT` (metadata `sys.columns`, INSERT temporal validado, cleanup FK-safe en `@AfterEach`/`finally`). Ese IT **no se modifica**. Se decide en 2.2B si el soporte de fixture se copia o se extrae a una utilidad de test común (Q-2).
- **Dos sesiones aisladas del mismo grupo** con el mismo estado inicial y los mismos estudiantes: sesión A ⇒ baseline JDBC; sesión B ⇒ candidato JPA. Nunca ambos caminos sobre la misma petición; ningún dual-write. Para escenarios que exigen estado previo, ambas sesiones se siembran con la **misma** llamada JDBC (setup, no SUT).
- Resultado observable normalizado por escenario: `Outcome = { clase de excepción, code, conjunto persistido {estudiante → (estado, presente)}, conteos Asistencia/DetalleAsistencia de la sesión, cardinalidad de RazonCausa }` (se excluyen IDs y marcas de tiempo propios de cada sesión). **Aserción doble:** `Outcome(JDBC) == Outcome(JPA)` **y** `Outcome(JPA)` cumple el **oráculo contractual absoluto** de la fila (evita aceptar "ambos equivocados por igual").
- Lectura de estado con tabla/vistas por `JdbcTemplate` (verdad de DB), no solo por el puerto.

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected (oráculo contractual + paridad) | Implementación incorrecta que atrapa | Nivel |
|---|---|---|---|---|---|---|---|---|
| CMD-PAR-001 | Lote válido AN/SJC/EX | 3 estudiantes activos, docente titular | sesión A y B vacías | JDBC en A; JPA en B con `AN,SJC,EX` | excepción; filas; `asistio`; `codigoRazonCausa` | sin excepción; 3 filas por sesión; `AN→asistio=1`; `SJC→0`; `EX→0`; `Outcome` idéntico. Resuelve **U-02/U-03/U-07** (el SP respondió, eco de correlación coincide) | Binding de nombre roto; result set perdido por update counts; `asistio` invertido; escribir solo el primer registro | IT |
| CMD-PAR-002 | Roundtrip exacto de `EX` | ídem | ídem | JPA `EX` y relectura (`uv_detalle_asistencia`, puerto) | `estado`, `presente` | `EX` con `presente=false`; **≠ `SJC`** | Serializar `EX` como `SJC`; colapsar estados | IT |
| CMD-PAR-003 | Docente ajeno | ejecutor = `Usuario.id` de otro docente | ídem | ambos caminos | excepción; filas | `ForbiddenException`, `code=FORBIDDEN`; **0** cambios | Sustituir ejecutor; capturar `SEC_002` como 500; escritura parcial | IT |
| CMD-PAR-004 | Estado inválido `ABC` | un registro `ABC` | conteo `RazonCausa` previo | ambos | excepción; `RazonCausa` | `ValidationException`, `VALIDATION_ERROR`; 0 cambios; `RazonCausa` (código `ABC` y total) sin cambios | Normalizar `ABC`→algún estado; crear razón dinámica; ignorar el registro y guardar el resto | IT |
| CMD-PAR-005 | Lote mixto ⇒ rollback total | `AN`, `ABC`, `EX` | sesión vacía | ambos | filas | `ValidationException`; **0** filas (ni AN ni EX) | Transacción/loop por registro en Java (parcial); commit anticipado | IT |
| CMD-PAR-006 | Catálogo de rechazos DB sin escritura parcial | (a) lista vacía `ATT_002`; (b) estudiante duplicado `ATT_003`; (c) estudiante que no pertenece al grupo `EST_004` junto a válidos; (d) sesión inexistente `SES_001`; (e) matrícula inactiva `EST_004` (**condicional** a que el fixture pueda crearla; si no, `NOT_RUN` documentado) | ídem | ambos | excepción; filas | (a)(b) `ValidationException`; (c)(e) `ForbiddenException`; (d) `ResourceNotFoundException`; **0** filas en cada caso; mismo `code` en ambos caminos | Deduplicar/filtrar en Java; `SES_001` como 500; escribir los válidos | IT (parametrizado) |
| CMD-PAR-007 | `Usuario.id` como ejecutor, no `Docente.id` | `Docente.id ≠ Usuario.id` (assert previo) | ídem | ejecutor=`Docente.id` ⇒ rechazo; ejecutor=`Usuario.id` ⇒ éxito | excepción; filas | rechazo con 0 filas; luego 3 filas | Derivar/convertir el ejecutor; ejecutar como otro usuario | IT |
| CMD-PAR-008 | Binding de `null` en `uniqueidentifier` (**U-04**) | `usuarioEjecutor=null` (y, aparte, `sesion=null`) | ídem | ambos | excepción; filas | mismo resultado que el baseline: `ValidationException`/`GEN_002`, 0 cambios (el baseline es el oráculo para `sesion=null`) | `null` enlazado como `varbinary` ⇒ error de conversión (500) en lugar de `GEN_002`; sustituir `null` | IT |
| CMD-PAR-009 | Idempotencia DB | mismo lote enviado 2 veces | sesión vacía | 2× cada camino | conteos | 2ª llamada sin error; **una** cabecera y **un** detalle por `(estudianteGrupo, sesion)`; estado igual tras 1ª y 2ª; `Outcome` idéntico | Insert ciego en cada llamada (duplicados); reintento propio en Java | IT |
| CMD-PAR-010 | Autocommit / frontera transaccional (**U-01**) | éxito y fallo | pool con `maximum-pool-size=1` (propiedad de test) | JPA éxito; JPA rechazo; luego `SELECT @@TRANCOUNT` y `autocommit` por `JdbcTemplate` (misma conexión); y, en otro test con pool >1, lectura de las filas desde **otra** conexión | `@@TRANCOUNT`, autocommit, visibilidad | `@@TRANCOUNT=0`, autocommit `true`; filas confirmadas visibles inmediatamente desde otra conexión; tras rechazo, 0 filas | Transacción JPA abierta sin cierre (conexión "sucia"); `BEGIN TRAN` anidado; commit diferido | IT |
| CMD-PAR-011 | Lote > 4000 caracteres sin truncar (**U-05**) | 1 estudiante válido + ≥ 100 UUID de estudiantes **no** matriculados (JSON ≫ 4000 caracteres) | ídem | ambos | `code` | ambos `ForbiddenException` (`EST_004`); **nunca** `ATT_001` (JSON inválido ⇒ evidencia de truncamiento) | Parámetro tipado con longitud fija (truncado ⇒ JSON inválido ⇒ `ATT_001`) | IT |
| CMD-PAR-012 | Concurrencia sobre misma sesión/estudiante | 4 hilos, mismo lote, misma sesión | sesión vacía | ejecutar concurrente en cada camino (hilos con su propio `CorrelationIdContext`) | conteos finales; tipos de excepción | una cabecera y un detalle por estudiante; toda excepción es una traducida (`ApplicationException`/`DatabaseOperationException`), ninguna cruda; mismo conjunto de resultados admitidos que el baseline | Candidato que serializa/serializa mal (duplicados); excepciones crudas de Hibernate | IT |
| CMD-PAR-013 | Firma del SP vs. binding (guardia **U-03**) | leer `sys.parameters` del SP (`parameter_id`) | freeze desplegado | comparar con la lista de parámetros que registra el candidato | nombres/tipos/orden | idéntico en nombres, tipos y **orden** (`parameter_id` 1..4): contrato obligatorio siempre (el binding es posicional; el orden de registro gobierna) | Renombrar/reordenar parámetros (3 de 4 son UUID: un swap se "acepta" sin este test) | IT |
| CMD-PAR-014 | Sin fuga de conexiones | 50 rechazos (docente ajeno) + éxitos, pool pequeño | Hikari `maximum-pool-size` bajo | secuencia por JPA | `HikariPoolMXBean` (activas/en espera) | activas=0 al final; siguiente éxito funciona | EM sin cerrar en el camino de excepción | IT |
| CMD-PAR-015 | Query/readback equivalente | tras cada escenario de éxito | ambos estados | leer con query JDBC **y** query JPA | filas por estudiante | ambas lecturas iguales entre sí y a la tabla; ausencia de fila ≠ `AN` para estudiantes omitidos | Lectura obsoleta; sintetizar `AN` | IT |
| CMD-PAR-016 | Correlación / auditoría (**U-08**) | éxito | `CorrelationIdContext=C` | JPA | (a) éxito ⇒ el SP devolvió `C`; (b) si `dbo.AuditoriaEvento` es observable por correlación, conteo por `C` en JDBC vs JPA | (a) implícito en éxito; (b) igualdad de conteos; si no observable ⇒ registrar `NOT_OBSERVABLE`, **no** `PASS` | Enviar otra correlación; afirmar auditoría sin evidencia | IT |
| CMD-PAR-017 | Lote parcial / ausencia ≠ AN | 2 de 3 estudiantes | sesión vacía | ambos | filas | exactamente 2 filas; el omitido **sin** fila | Registrar `AN` por defecto a los omitidos | IT |
| CMD-PAR-018 | Cambio de estado (upsert) | `AN` luego `EX` para el mismo estudiante | 1ª carga previa igual en A y B | 2ª llamada por cada camino | fila | una fila; `EX/false`; sin duplicado | Insert en vez de upsert | IT |

## D. Realtime: semántica de publicación

| ID | Requirement | Scenario | Observable | Expected | Implementación incorrecta que atrapa | Nivel |
|---|---|---|---|---|---|---|
| CMD-RT-001 | Regresión del Use Case (no se modifica) | `RegistrarAsistenciasSesionUseCaseImplTest` completo | resultados actuales | verdes sin editar: persiste→publica en orden, fallo⇒no publica, docente ajeno⇒no persiste ni publica, sesión inexistente, propaga `usuarioEjecutor` | Mover publicación antes de persistir; publicar tras excepción | U (regresión CMD-REG-004) |
| CMD-RT-002 | Publica **exactamente después** de persistir (adapter JPA real) | Use Case real + port compuesto con `command=jpa` + `InstitutionalScopePort` permisivo + publisher **espía** que, al recibir `publish`, consulta la DB | eventos capturados | 1 evento `ASISTENCIAS_SESION_ACTUALIZADAS` con `{grupo, sesion, totalRegistros=N}`; **en el momento del `publish` las N filas ya están visibles** | Publicar antes del commit; publicar sin filas | IT (`AsistenciaCommandRealtimeIT`) |
| CMD-RT-003 | Fallo de persistencia ⇒ **no** publica | mismo arnés; el ejecutor no es titular (rechazo DB `SEC_002`) y, aparte, estado `ABC` | eventos; filas | `ForbiddenException` / `ValidationException`; **0** eventos; 0 filas | Publicar en `finally`; capturar la excepción del adapter y seguir | IT |
| CMD-RT-004 | SSE no es fuente de verdad ni hay transacción SQL↔SSE | (E2E HTTP + SSE) | — | **No aplica en 2.2B–D**; se difiere a 2.2E (activación en local/dev). Sin evento no se pierde dato (HTTP es la verdad) | Convertir SSE en requisito de la persistencia | `NOT_RUN` (planificado) |

## E. Arquitectura y regresión

| ID | Requirement | Scenario / Action | Expected | Implementación incorrecta que atrapa | Nivel |
|---|---|---|---|---|---|
| CMD-ARCH-001 | Domain/Application sin JPA | reglas existentes `JpaIsolationRulesTest` (sin editar) | 0 dependencias de `jakarta.persistence`, `org.hibernate`, `org.springframework.data` | `EntityManager`/`StoredProcedureQuery` filtrado a Application/puertos | A |
| CMD-ARCH-002 | Abstracción del command libre de JPA | `..sqlserver.core..` (incl. `AsistenciaCommandPersistence` y el híbrido) | sin dependencias JPA; JPA solo en `..sqlserver.jpa..` y Composition Root | Tipos JPA en la interfaz interna | A (`JpaCommandIsolationRulesTest`) |
| CMD-ARCH-003 | Sin transacción JPA ni Spring Data | `..sqlserver.jpa..` no depende de `org.springframework.transaction.annotation.Transactional` ni de `Repository`; no hay nuevas `@Entity` | cumple | `@Transactional` en el candidato (D4); reintroducir Spring Data | A |
| CMD-REG-001 | Baseline JDBC intacto | `AsistenciaRepositorySqlServerIT` (6) y `AsistenciaRepositorySqlServerAdapterTest` sin editar; `command=jdbc` (default) | verdes | Refactor del baseline que cambie su comportamiento | IT + U |
| CMD-REG-002 | Firma del SP intacta | `GoldenPathSqlStoredProcedureContractIT` (dirigido) | verde | Suposición de firma distinta | IT |
| CMD-REG-003 | Contratos congelados | `sha256` del contrato DB, OpenAPI y contrato backend | `45e48c5a…9aec`, `72a3097b…da54`, `9b4830b4…8c62` sin cambios; tests de conformance OpenAPI del `verify` | Cambio contractual accidental | Doc/hash + U |
| CMD-REG-004 | Nada existente se edita para pasar | `git diff` de `src/test/**` de la fase | solo **archivos nuevos** de test; cero modificaciones a tests existentes | Modificar expectativas/`@Disabled`/exclusiones para esconder fallos (TD-043 incluido) | Revisión de diff |
| CMD-REG-005 | Normal verify hermético | `.\mvnw.cmd clean verify` sin variables de DB/Azure | exit 0; LINE ≥ 80 %, BRANCH ≥ 70 % | Test unitario que exige SQL Server; ramas nuevas sin cubrir | Gate |

## Negative / Boundary Coverage

Cubierto: `dto=null`; `usuarioEjecutor=null`; `sesion=null`; estados fuera de dominio y con espacios/minúsculas (pasan tal cual al SP); lista vacía; duplicados; estudiante ajeno; matrícula inactiva (condicional); sesión inexistente; docente ajeno; `Docente.id` por `Usuario.id`; JSON grande; concurrencia; reintento idempotente; correlación ajena; cardinalidad 0/2; excepciones técnicas; valor inválido del selector; combinaciones de selectors. **NO APLICA:** JSON inválido/no-array/objeto con claves extra (`ATT_001`) — el port solo emite JSON bien formado; ese rechazo pertenece a los ITs de DB y no es alcanzable por este adapter sin duplicar reglas; sesión de otro grupo no autorizada por Application (la cubre el Use Case, `CMD-RT-001`).

## Side Effects / Rollback

Cada IT registra lo que crea y lo elimina FK-safe en `finally` (hijos primero); al final se verifica que el conteo de filas con prefijo del test sea 0 y que `RazonCausa` no cambió. Sin DDL, sin tocar filas productivas, sin modificar DB. Rollback de la fase: revertir commits; el default (`jdbc`) hace que el código nuevo sea inerte sin configuración.

## ACCEPTANCE RED de 2.2B (revisado; sustituye la propuesta original de 2.2A)

Solo sobre **costuras productivas existentes** (Q-1 resuelto). Los archivos concretos y su SHA-256 quedan en [RED_SNAPSHOT](RED_SNAPSHOT.md).

| ID | Tipo | Propiedades | Expected FUTURO | AS-IS (falla por…) |
|---|---|---|---|---|
| RED-A | RED | `command=jpa`, `query=jdbc` | `registrarAsistenciasSesion` usa JPA | la propiedad se ignora y el command sigue JDBC |
| RED-B | RED | `query=jdbc`, `command=jpa` | exactamente 1 `EntityManagerFactory` | el EMF depende solo de `query=jpa` ⇒ 0 EMF |
| RED-C | RED | `command=valor-invalido` | fallo de arranque fail-closed | la propiedad no se interpreta ⇒ el contexto arranca |
| RED-D | RED | independencia `query=jdbc`/`command=jpa` | query JDBC + command JPA | command permanece JDBC (routing) |
| RED-E | CARACTERIZACIÓN (PASS) | sin propiedad `command` | default = command JDBC (el cambio futuro no debe alterarlo) | — |
| RED-F | CARACTERIZACIÓN (PASS) | `query=jpa`, sin `command` | estado local/dev actual: query JPA + commands JDBC | — |

Grupos de tests de la matriz completa (`CMD-CFG/ADP/PAR/RT/ARCH`) que se **difieren** a 2.2C+ por depender de clases productivas inexistentes: `AsistenciaJpaCommandPersistenceTest`, `CanonicalProcedureResultValidatorTest`, `JpaCapabilityRequiredConditionTest`, `AsistenciaRepositoryHybridCommandRoutingTest`, `JpaCommandIsolationRulesTest`, `AsistenciaCommandJpaParityIT`, `AsistenciaCommandRealtimeIT`.

Reglas: el RED debe fallar por **comportamiento ausente**, nunca por falta de DB/red ni por "la clase nueva no existe"; un IT sin entorno es `NOT_RUN`, no RED válido. El RED queda congelado: el implementador no puede modificar expectativas, eliminar tests, `@Disabled`, cambiar asserts/propiedades ni relajar comportamiento; un cambio posterior es `TEST_CONTRACT_CONFLICT` + revisión humana.

## RED_SNAPSHOT

**NO APLICA en 2.2A** (documentación pura). Se completa en 2.2B en [RED_SNAPSHOT.md](RED_SNAPSHOT.md) (base commit, archivos, SHA-256, comando, exit code, fallos esperados/inesperados, baseline previo, feasibility).

## Congelación

Pendiente: revisor/decisión de aprobación de este TEST_PLAN y hash/commit del RED en 2.2B. Un cambio posterior a un RED aprobado es `TEST_CONTRACT_CONFLICT` con dictamen del auditor.

## Integración y ejecución

- Comando normal: `.\mvnw.cmd clean verify` (sin DB).
- Comando de integración: `. .\scripts\load-env.ps1; .\mvnw.cmd -Pintegration verify` (perfil `integration`, `application-integration`). Hoy termina **NOT_GREEN** por [TD-043](../../../baseline/TECHNICAL_DEBT.md#td-043) (6 fallos preexistentes, ajenos). Los ITs del command se ejecutan **dirigidos** (selección Failsafe por clase; comando exacto a confirmar en 2.2B) y se informan por separado; el resultado global se reporta sin maquillaje. Skips: revisar y listar; un `assumeTrue` que omita un IT obligatorio del command **es fallo**, no skip aceptable.
- Autorización del ambiente: la habilita el usuario (freeze desplegado y `test_summary.ps1` PASS); sin credenciales en documentación ni logs.
- Azure integration: fuera de alcance.

## Falsos positivos que deben evitarse

- Aceptar `Outcome(JDBC)==Outcome(JPA)` sin oráculo contractual (ambos podrían fallar igual).
- Comparar el JSON como texto literal (`Map.of` es no determinista) o, al contrario, no comparar su forma.
- Considerar `PASS` un IT que corrió el camino JDBC porque `command=jpa` no se aplicó (verificar por evidencia de routing: EMF usado / sentencia `{call …}` de Hibernate en `sys.dm_exec_query_stats`, como en LB-002.1B).
- Usar `EntityManager`/repositorio mockeado como prueba de integración; usar H2.
- Comparar solo por `toString`; reordenar resultados en producción para "estabilizar" la prueba.
- Tests que confirman la implementación ya escrita; asserts cosméticos para subir JaCoCo; `@Disabled`/exclusiones para esconder TD-043.
- Declarar `NOT_OBSERVABLE` (auditoría, matrícula inactiva) como `PASS`.
- Escribir el mismo fixture con dos caminos sobre la misma petición (dual-write encubierto).
