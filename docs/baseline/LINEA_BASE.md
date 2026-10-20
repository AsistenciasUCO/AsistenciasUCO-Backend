---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# LÃ­nea base tÃ©cnica activa

La vertical patrÃ³n es [asistencia en lote + consulta + realtime](GOLDEN_PATH_ASISTENCIA.md), decidida en [ADR-001](../adr/ADR-001-golden-path-asistencia.md). Esta es la secuencia normativa activa; el detalle histÃ³rico permanece en los work items y cierres enlazados.

## Estado de fases

| Fase | Alcance | Estado |
|---|---|---|
| LB-000 | Gobernanza y documentaciÃ³n | **CLOSED** â governance complete; build gate restaurado por TECH-001 |
| LB-001 | Golden Path + Contract First | **CLOSED / FROZEN** â DBâbackendâfrontend alineados; OpenAPI/Swagger/contrato congelados |
| LB-001D | Governance hardening | **CLOSED / FROZEN** â seguridad webhook y aislamiento cloud integration cerrados; MV-003 sigue parcial |
| LB-002 | Piloto JDBC â JPA | **CLOSED / FROZEN** â query JPA + command JPA `registrarAsistenciasSesion`, paridad SQL Server real, fallback JDBC preservado |
| LB-003 | Quality Gate Golden Path | **CLOSED / PASS** â local quality gate, SQL Server targeted y CI remoto del PR #14 verdes |
| LB-004 | Stateless / Serverless readiness | **IN_PROGRESS — LB-004C (2026-10-18): STORAGE FOUNDATION MinIO+ClamAV CERTIFICADA con proveedores reales y E2E HTTP con Bearer; REVIEW_BINDING BLOCKED_BY_DB_CONTRACT; no DONE** ([LB-004C](../work-items/LB-004-stateless-serverless-readiness/LB-004C-EXECUTION_REPORT.md), [PAUSE](../work-items/LB-004-stateless-serverless-readiness/PAUSE.md)) |
| LB-008 | Migración persistencia JPA-only | **JPA-07 PASS / CLOSED; JPA-06 PASS / CLOSED; DIRECT_JDBC_IN_SRC_MAIN = 0; TD-010 RESOLVED; TD-055 RESOLVED; PASS (TD-039, TD-043, TD-046, TD-047, TD-056, TD-057 y TD-058 CLOSED 2026-10-07; TD-044 no bloqueante)**; integración global GREEN (188 ITs, 0 fallos, 1 skip TD-044) ([ADR-003](../adr/ADR-003-jpa-only-persistence.md), [ADR-004](../adr/ADR-004-jpa-repository-architecture.md)) |
| LB-005 | Realtime distribuido | NOT_STARTED |
| LB-006 | IaC / CD / cloud | NOT_STARTED |
| LB-007 | Replicar patrÃ³n | NOT_STARTED |

Ninguna fase autoriza automÃ¡ticamente la siguiente. No combinar JPA, negocio, contrato, realtime y cloud en una sola tarea sin lÃ­mites.

## Golden Path actual

```text
POST /api/v1/asistencias/lote
  -> AsistenciaController
  -> RegistrarAsistenciasSesionInputPort / UseCase
  -> AsistenciaRepositoryPort
  -> AsistenciaRepositoryPort
  -> AsistenciaJpaRepository (@Repository JPA-only, LB-008 JPA-06A)
       -> AsistenciaJpaRepository: 4 commands por createNativeQuery("EXEC â¦")
  -> dbo.usp_registrar_asistencias_sesion
  -> RealtimePublisherPort
  -> SSE local

GET /api/v1/grupos/{grupoId}/asistencias
  -> AsistenciaRepositoryPort
  -> AsistenciaJpaRepository (JPA-only)
```

La DB sigue siendo la autoridad contractual del Golden Path. JPA no cambia el contrato HTTP, Application/Domain ni el Stored Procedure congelado.

## Providers AS-IS (LB-008, JPA-01 COMMANDS)

| Perfil | Query asistencia | Commands de asistencia |
|---|---|---|
| todos | JPA | JPA |

No hay selectores `jdbc|jpa`, adapter hÃ­brido ni fallback JDBC productivo en Asistencia. El orÃ¡culo JDBC existe solo en tests de paridad (`AsistenciaJdbcBaselineOracle`).

## Evidencia consolidada

### LB-001 â contrato

- DBâbackend Golden Path alineado: [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).
- OpenAPI/Contract First + Swagger UI: [LB-001C](../work-items/LB-001C-openapi-contract-first/CLOSURE.md).

### LB-002 â JPA incremental

- JPA query pilot/activation: `LB-002.1` / `LB-002.1B`.
- JPA command `registrarAsistenciasSesion`: [LB-002.2 final closure](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2-FINAL-CLOSURE.md).
- SQL Server parity: `PARITY_MISMATCHES=0`.
- Runtime local manual: query JPA + command JPA PASS.
- Realtime dos clientes + reconexiÃ³n/reconvergencia ~25 s; HTTP/DB source of truth.

### LB-003 â Quality Gate Golden Path

- `clean verify`: **1426 tests**, 0 failures/errors/skips.
- JaCoCo: **LINE 91.52 % / BRANCH 81.17 %**.
- ArchUnit: **85/85 PASS**.
- OpenAPI: **16/16 PASS**.
- SQL Server Golden Path targeted: **59/59 PASS**, 0 skips, 0 mismatches.
- Seguridad, errores seguros, correlation, write/read y realtime: PASS.
- PR #14 / SHA `e92afb73221ac3a967e63c673312d3961cfb0688`: required checks `Backend Quality Gate`, `CodeQL Java Analysis`, `Dependency Review`, `SonarCloud Code Analysis` PASS; check adicional CodeQL PASS.
- SonarCloud: Quality Gate passed, 0 Security Hotspots, 100 % coverage on new code, 0 % duplication on new code. Los 37 new issues reportados por Sonar quedan como backlog de calidad; no se ocultan.

Ver [LB-003 CLOSURE](../work-items/LB-003-quality-gate-golden-path/CLOSURE.md) y [REMOTE_CI_EVIDENCE](../work-items/LB-003-quality-gate-golden-path/REMOTE_CI_EVIDENCE.md).

## Deuda relevante para la secuencia

| ID | Estado | Impacto sobre fases |
|---|---|---|
| TD-001 | CLOSED â LB-002.2 | no bloquea |
| TD-003 | OPEN | bloquea LB-005 (realtime distribuido) |
| TD-004 | PARTIAL | storage foundation (MinIO+ClamAV, ownership tÃ©cnico estudiante) cerrada; sigue bloqueando LB-004 la lectura docente/coordinador/administrador y el binding DB completo |
| TD-008 | CLOSED â LB-003 | no bloquea |
| TD-010 | OPEN | no bloquea LB-003; revisar release DB |
| TD-011 | OPEN | relevante para LB-005 si exige durabilidad |
| TD-022 | OPEN | bloquea LB-006 segÃºn exposiciÃ³n Actuator/Prometheus |
| TD-023 | **OPEN / PARTIAL** | LB-003 remote gates satisfechos; pendiente solo CI DB reproducible/versionada para LB-006 salvo nueva decisiÃ³n |
| TD-027 | OPEN | bloquea LB-006 segÃºn capacidad modificada |
| TD-043 | **CLOSED (2026-10-07)** | providers de matrícula, sincronización de usuario y PlanEstudio presentes en la DB final; integración global GREEN |
| TD-044 | **OPEN / NON_BLOCKING** | 2 skips conocidos fuera de Asistencia; no invalida JPA-01 ni bloquea JPA-02A |
| TD-049 | OPEN / OUT_OF_GOLDEN_PATH | no bloquea Golden Path |
| TD-050 | OPEN / NON_BLOCKING | latencia de reconciliaciÃ³n realtime |
| TD-055 | ABIERTA â LB-008 | JDBC residual en `src/main` (6 archivos tras JPA-06: auditoria TD-010 y soporte de procedimientos JPA-07); bloquea cerrar LB-008 |
| TD-056 | **CLOSED (2026-10-07)** | cerrada por la alineación final de la DB: garantiza atomicidad de Asistencia + DetalleAsistencia; el Backend consume ese contrato final |
| TD-057 | **CLOSED (2026-10-07)** | cerrada por la alineación final de la DB (catálogo `dbo.Estado` y SP de solicitudes de revisión); el Backend consume ese contrato final |
| TD-058 | **CLOSED (2026-10-07)** | cerrada por la alineación final de la DB (`usp_ejecutar_cierre_masivo_periodo`); el Backend consume ese contrato final |
| TD-039 | **CLOSED (2026-10-07)** | cerrada por la alineación final de la DB; el Backend consume ese contrato final |
| TD-046 | **CLOSED (2026-10-07)** | cerrada por la alineación final de la DB; el Backend consume ese contrato final |

El ledger completo y la historia por ID permanecen en [TECHNICAL_DEBT.md](TECHNICAL_DEBT.md).

## CI y gobernanza remota

Ruleset `Protect develop` observado activo:

- PR requerido.
- deletion / non-fast-forward bloqueados.
- sin bypass.
- required checks: Backend Quality Gate, CodeQL Java Analysis, Dependency Review, SonarCloud Code Analysis.

Para LB-003, esos checks pasaron sobre el PR #14 y el SHA actual certificado. [MV-004](MANUAL_VALIDATION_LEDGER.md) queda PASS para esta validaciÃ³n remota.

TD-023 permanece abierta Ãºnicamente por la ausencia de CI DB reproducible/versionada cross-repo; esa parte se difiere a LB-006 salvo nueva decisiÃ³n normativa.

## InstrucciÃ³n activa

**LB-003 estÃ¡ CLOSED / PASS.**

La fase normativa activa es:

```text
LB-008 â MigraciÃ³n de persistencia JPA-only
STATUS: IN_PROGRESS â JPA-06 (NOT_CLOSED: catalogs PASS, audit BLOCKED_BY_TD010)
PRIMARY_VARIABLE: PERSISTENCE_PROVIDER = JPA_ONLY
JPA-00: PASS
JPA-01 FOUNDATION: PASS
JPA-01 COMMANDS: PASS
JPA-02A: PASS
JPA-02B: PASS_WITH_SCOPED_DB_BLOCKER
JPA-03: PASS_WITH_SCOPED_DB_BLOCKER
JPA-04: PASS / CLOSED
JPA-05: PASS / CLOSED
JPA-06A: PASS / CLOSED (2026-10-06)
JPA-06: NOT_CLOSED / BLOCKED_BY_TD010 (catalogs PASS; audit BLOCKED_BY_TD010_DECISION)
JPA-07: NOT_STARTED (no iniciar automaticamente)
ASISTENCIA_PERSISTENCE_PROVIDER: JPA_ONLY
BOOTSTRAP: SPRING_BOOT_JPA_STANDARD
TRANSACTION_MANAGER: JpaTransactionManager
TRANSACTION_MANAGER_PARITY: PASS
DIRECT_JDBC_ASISTENCIA: 0
DIRECT_JDBC_GLOBAL_BEFORE_JPA06: 9
DIRECT_JDBC_GLOBAL_CURRENT: 6
GLOBAL_INTEGRATION: NOT_GREEN_TD043
NEXT_MICROPHASE: JPA-06 — CATALOGS + AUDIT + AUXILIARY PERSISTENCE
SCOPE: JPA-04 CLOSED; JPA-05 PASS / CLOSED
```

El bloque anterior es el estado vigente. Las secciones posteriores que describen JPA-02A/JPA-02B
como siguientes pasos son `HISTORICAL SNAPSHOT`; conservan la trazabilidad de entrada de esas
microfases y no reabren JPA-02B ni JPA-03.

JPA-01 cerrÃ³ causalmente con los cuatro commands de Asistencia migrados, queries JPA-only,
`StoredProcedureQuery = 0`, JDBC directo de Asistencia en `src/main = 0` y runtime hÃ­brido de
Asistencia = 0. El gate dirigido final fue **83/83 IT PASS** sobre SQL Server real. El
`clean verify -Pintegration` global conserva 6 fallos preexistentes de TD-043 y 2 skips de TD-044;
por eso permanece `NOT_GREEN_TD043`, no `PASS`. La excepciÃ³n de continuidad versionada en el work
item autorizÃ³ JPA-01 y TD-043/TD-044 no invalidan su cierre causal.

JPA-02A: **PASS en gate causal (2026-10-05).** El bootstrap JPA es el estÃ¡ndar de Spring Boot: un Ãºnico
`EntityManagerFactory` administrado por Boot, `EntityManager` compartido de Spring Data, entity scanning del
paquete base y `JpaTransactionManager` como TM. El EMF manual del piloto, las exclusiones de auto-configuraciÃ³n
JPA y `SharedEntityManagerCreator` quedan retirados de `src/main`. El global `-Pintegration` conserva el mismo
conjunto de fallos TD-043 que antes (`NOT_GREEN_TD043`). LB-008 no estÃ¡ cerrada: quedan 44 archivos con JDBC
directo fuera de Asistencia, todos con microfase asignada en
[JDBC_RESIDUAL_INVENTORY](../work-items/LB-008-jpa-only-persistence-migration/JDBC_RESIDUAL_INVENTORY.md).
El criterio global `DIRECT_JDBC_IN_SRC_MAIN = 0` pertenece al cierre de LB-008, no al DoD de JPA-02A.

**Snapshot histórico de entrada JPA-02B:** quedó inicialmente
`READY_WITH_SCOPED_DB_BLOCKER / IMPLEMENTATION_NOT_STARTED`. Su alcance eran siete
commands: cuatro de SesiÃ³n (`usp_crear_sesion`, `usp_actualizar_sesion`, `usp_cerrar_sesion`,
`usp_generar_sesiones_grupo`) y tres de Grupo (`usp_crear_grupo`, `usp_actualizar_grupo`,
`usp_registrar_estudiante_en_grupo_usuario_no_existente`). Los seis primeros tienen provider DB
disponible. El Ãºltimo conserva `CODE_MIGRATION_STATUS = JPA_REQUIRED` y
`DB_PROVIDER_STATUS = MISSING / BLOCKED_TD043`; no se sustituye por otro SP. Las queries
`uv_sesion` y `uv_grupo` permanecen temporalmente en JDBC y siguen asignadas a JPA-04.

DecisiÃ³n humana posterior a LB-002 (reuniÃ³n con el profesor): JPA/Hibernate es la Ãºnica API de persistencia del cÃ³digo productivo; JDBC directo sale del cÃ³digo del backend. Ver [ADR-003](../adr/ADR-003-jpa-only-persistence.md) y [LB-008](../work-items/LB-008-jpa-only-persistence-migration/PLAN.md).

LB-004 queda pausada por esta prioridad, sin revertirse:

```text
LB-004 â Stateless / Serverless readiness
STATUS: PAUSED / FROZEN_BY_JPA_MIGRATION â STORAGE_FOUNDATION: GREEN ; REVIEW_BINDING: BLOCKED_BY_DB_CONTRACT ; FULL_E2E: PENDING
```

Detalle: [PAUSE](../work-items/LB-004-stateless-serverless-readiness/PAUSE.md).

El texto siguiente describe LB-004 tal como quedÃ³ congelada.

Objetivo de LB-004: demostrar que el Golden Path puede operar sin depender de filesystem o heap de una Ãºnica rÃ©plica; externalizar storage/config/secrets cuando aplique y evaluar cache solo con necesidad medida. TD-004 es el bloqueo principal conocido.

LB-004A fue revisada humanamente con `ASSESSMENT: APPROVED`. LB-004B.0/B.1 congelaron ownership,
identidad de objeto (`fileId`) y contrato HTTP conceptual en
[LB-004 stateless/serverless readiness](../work-items/LB-004-stateless-serverless-readiness/PLAN.md).
LB-004B.2 recibiÃ³ una decisiÃ³n humana explÃ­cita que reemplazÃ³ el provider (`AZURE_BLOB_STORAGE` â
`MINIO`) y activÃ³ malware scanning obligatorio (ClamAV) â ver
[PROFESSOR_DECISION](../work-items/LB-004-stateless-serverless-readiness/PROFESSOR_DECISION.md) â y
ejecutÃ³ REDâGREEN de la storage foundation: `ArchivoController` ya no toca filesystem, los bytes
viven en MinIO (Docker local), el malware scanning es obligatorio y fail-closed, y el ownership
tÃ©cnico del estudiante propietario estÃ¡ implementado y probado (unit, ArchUnit, integraciÃ³n real
contra MinIO/ClamAV). `REVIEW_BINDING` (binding completo `fileId â revisiÃ³n â sesiÃ³n â grupo â
docente`) permanece `BLOCKED_BY_DB_CONTRACT` (`DR-LB004-DB-002`), aislado y sin bloquear el cierre de
esta microfase. Detalle: [LB-004B.2-VALIDATION](../work-items/LB-004-stateless-serverless-readiness/LB-004B.2-VALIDATION.md).

## Referencias activas

- [Golden Path](GOLDEN_PATH_ASISTENCIA.md)
- [Definition of Ready](../governance/DEFINITION_OF_READY.md)
- [Definition of Done](DEFINITION_OF_DONE.md)
- [Technical Debt](TECHNICAL_DEBT.md)
- [Manual Validation Ledger](MANUAL_VALIDATION_LEDGER.md)
- [LB-002.2 Final Closure](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2-FINAL-CLOSURE.md)
- [LB-003 Closure](../work-items/LB-003-quality-gate-golden-path/CLOSURE.md)

### Actualización LB-008 JPA-06 (2026-10-05)

- **Estado:** JPA-06 **NOT_CLOSED / BLOCKED_BY_TD010**. Catálogos de mensaje y parámetro **PASS** (JPA); auditoría
  **BLOCKED_BY_TD010_DECISION**. JPA-06 no cierra mientras la auditoría dependa de TD-010.
- **Catálogos:** `SqlServerMessageCatalogAdapter`, `SqlServerParameterCatalogAdapter` y su configuración usan
  `MessageCatalogJpaRepository` / `ParameterCatalogJpaRepository` (`EntityManager` + JPQL) sobre `uv_mensaje_usuario`, `uv_mensaje_tecnico` y `uv_parametro`.
  Entidades: `UvMensajeUsuarioEntity` y `UvMensajeTecnicoEntity` (nuevas); `UvParametroEntity` reutilizada.
- **Paridad:** `CatalogJpaParityIT` 11/11 PASS contra SQL Server real (oráculo JDBC BEFORE en `src/test`).
- **Reducción JDBC:** `DIRECT_JDBC_GLOBAL` 9 → 6 (6 = auditoría y soporte de procedimientos).
- **Auditoría:** sin implementación. Ver [TD-010](TECHNICAL_DEBT.md#td-010) y
  [CONTRACT_MATRIX](../work-items/LB-008-jpa-only-persistence-migration/CONTRACT_MATRIX.md).
- **Siguiente:** no iniciar JPA-07 automáticamente. JPA-06 cierra solo cuando TD-010 se resuelva y la auditoría migre.

### Actualización LB-008 JPA-05 (2026-10-05)

- **Estado:** JPA-05 **PASS / CLOSED**. Lecturas académicas, autorización y reporte en JPA;
  `DIRECT_JDBC_JPA05_SCOPE = 0`. `uv_estudiante_programa` alineada por DB (`EstudiantePrograma.id`, RESOLVED).
- Documentación de cierre consolidada en el [TEST_PLAN](../work-items/LB-008-jpa-only-persistence-migration/TEST_PLAN.md)
  y en la [VALIDATION](../work-items/LB-008-jpa-only-persistence-migration/VALIDATION.md). `GLOBAL_INTEGRATION = NOT_GREEN_TD043`.
- Snapshot histórico: JPA-05 `NOT_CLOSED` (bloqueo por identidad de vista) queda superado por la alineación DB.

### Actualización LB-008 JPA-04 (2026-10-05)

- **Estado:** JPA-04 **PASS / CLOSED**. Sesión, Grupo, Usuario, Docente, Estudiante y
  TipoIdentificación leen las nueve vistas core mediante entidades `@Entity @Immutable` y JPQL
  tipado. `CORE_QUERY_DIRECT_JDBC = 0`.
- **Paridad:** `CoreViewQueriesJpaParityIT` 6/6 PASS contra SQL Server real; columnas, conteos,
  orden, UTC, null, paginación y not-found coinciden JDBC BEFORE = JPA AFTER.
- **Gates:** `clean verify` 1375/1375 PASS; ArchUnit y OpenAPI PASS; LINE 87.24 %, BRANCH 78.18 %.
  Integración global 159 IT, 6 fallos TD-043 preexistentes y 2 skips TD-044; cero fallo causal JPA-04.
- **Reducción JDBC:** 43 → 29 archivos reales; siete `RowMapper` core retirados. JPA-03 queda
  congelada, TD-043/TD-044/TD-058 permanecen abiertas.
- **Siguiente microfase:** **JPA-06 — CATALOGS + AUDIT + AUXILIARY PERSISTENCE**,
  `NOT_STARTED`. No se inicia automáticamente.

### Actualización LB-008 JPA-03 (2026-10-05)

- **Estado:** JPA-03 **PASS_WITH_SCOPED_DB_BLOCKER**. Código 8/8 en JPA (`CODE_MIGRATION_STATUS = JPA`). `ACADEMIC_USER_COMMAND_DIRECT_JDBC = 0`.
- **Providers disponibles (6/6):** error parity, paridad de éxito y efectos DB JDBC BEFORE = JPA AFTER en SQL Server real (`AcademicUserCommandsSuccessParityIT` 6/6; `AcademicUserCommandsSpParityIT` 8/8). Fixtures autocontenidos `IT-LB008-JPA03-*`, limpieza en `@AfterEach` y residuos = 0.
- **Providers ausentes (2/2):** characterization `SP_NOT_IN_DB` equivalente. TD-043 OPEN para `usp_registrar_o_actualizar_plan_estudio` y `usp_sincronizar_usuario`.
- **Transacciones:** matriz certificada para la frontera de producción (`TARGET_OUTER_TX = NO`). Ramas de savepoint y rollback intermedio no probadas y no declaradas.
- **Integración global:** `NOT_GREEN_TD043`. 153 ITs, 6 fallos idénticos por nombre a la línea base (TD-043), sin fallo causal nuevo. Unitarios y `clean verify`: 1395/0, BUILD SUCCESS. ArchUnit PASS. OpenAPI 16/16. Cobertura LINE 91.65 %, BRANCH 80.58 % (reporte `-Pintegration`).
- **Deuda nueva:** [TD-058](TECHNICAL_DEBT.md#td-058). `usp_ejecutar_cierre_masivo_periodo` cae al período más reciente cuando el código no coincide. OPEN / HIGH / DB_OWNER. No bloquea JPA-03; bloquea el uso operativo seguro del cierre masivo.
- **Snapshot al cierre JPA-03:** JPA-04 quedó `READY`; fue implementada y cerrada posteriormente,
  según la actualización JPA-04 anterior.

### ActualizaciÃ³n LB-008 JPA-02B (2026-10-05)

- **Estado:** JPA-02B **PASS_WITH_SCOPED_DB_BLOCKER**. Commands de SesiÃ³n 4/4 y Grupo 3/3 usan JPA (`EntityManager` + `createNativeQuery("EXEC â¦")` + `ProcedureResultMapper` + `ProcedureResultValidator`). NingÃºn command core usa JDBC.
- **Providers:** seis disponibles con paridad BEFORE = AFTER en SQL Server (`SesionGrupoCommandsSpParityIT` 11/11, lÃ­neas idÃ©nticas). `usp_registrar_estudiante_en_grupo_usuario_no_existente`: `CODE_MIGRATION_STATUS = JPA`, `DB_PROVIDER_STATUS = MISSING` (TD-043 OPEN). No se sustituyÃ³ por otro SP.
- **Transacciones:** la frontera `TransactionOperations` de Grupo se conserva; SesiÃ³n no tiene transacciÃ³n exterior.
- **IntegraciÃ³n global:** `NOT_GREEN_TD043`, sin cambio. 139 ITs, 6 fallos idÃ©nticos por nombre a la lÃ­nea base y sin causa nueva de JPA-02B. Unitarios 1371/0. ArchUnit 100/100. OpenAPI 16/16. LINE 88.43 %, BRANCH 78.65 %.
- **Snapshot histórico JPA-02B:** el siguiente paso era cerrar JPA-03. JPA-03 y JPA-04 ya fueron
  cerradas posteriormente; TD-043 sigue abierto para los providers ausentes.

### Actualización LB-008 JPA-07 (2026-10-06)

- **Cleanup final de JDBC.** `DIRECT_JDBC_IN_SRC_MAIN = 0` (búsqueda por `JdbcTemplate`, `NamedParameterJdbc*`, `RowMapper`,
  `ResultSet`, `java.sql`, `CanonicalStoredProcedureExecutor`, `JdbcValueMapper`, `CanonicalProcedureResultMapper` y
  `SqlServerProcedureSupportConfiguration`: 0 coincidencias en `src/main`).
- Retirados de `src/main`; oráculos migrados a `src/test` como baseline: `CanonicalJdbcBaselineExecutor`,
  `CanonicalJdbcBaselineResultMapper`, `JdbcBaselineValueMapper`, `JdbcBaselineTestConfiguration`.
- Selector `app.adapters.persistence.provider` retirado (enum de un solo valor). Repositories SQL Server JPA sin
  `@ConditionalOnProperty` de persistencia; catálogos y auditoría conservan sus selectores reales.
- `spring-boot-starter-jdbc` retirado como dependencia explícita: llega transitivo desde `spring-boot-starter-data-jpa`
  (verificado con `dependency:tree`). `mssql-jdbc` se mantiene runtime.
- Retoques de clean code: `AuditEventJpaRepository.metadataFromJson` privado (test por `findLatestByCorrelationId`);
  `MessageCatalogJpaRepository.findTechnicalMessage` privado (paridad por `getTechnicalMessage`).
- ArchUnit: `Uv*Entity` → `@Immutable` por convención; entidades no-`Uv` sin `@Immutable`; toda `@Entity` bajo Infrastructure JPA.
- Estado: `JPA-07 = PASS / CLOSED`. Ver [JDBC_RESIDUAL_INVENTORY](../work-items/LB-008-jpa-only-persistence-migration/JDBC_RESIDUAL_INVENTORY.md)
  y [VALIDATION](../work-items/LB-008-jpa-only-persistence-migration/VALIDATION.md).

### LB-008 — addendum FINAL de alineación DB/Backend (2026-10-07)

Los bloques anteriores que mencionan TD-043 abierto o `NOT_GREEN_TD043` son históricos. Estado vigente:

- **Estado vigente:** `TD-039 = CLOSED`, `TD-043 = CLOSED`, `TD-046 = CLOSED`, `TD-047 = CLOSED`, `TD-056 = CLOSED`, `TD-057 = CLOSED`, `TD-058 = CLOSED` (TD-039/046/056/057/058 cerradas mediante la alineación/corrección final de la DB; el Backend ahora consume ese contrato final); `DIRECT_JDBC_IN_SRC_MAIN = 0`; `BACKEND_DIRECT_INTERNAL_SP = 0`;
  providers DB de matricula (`usp_registrar_estudiante_en_grupo`), sincronizacion de usuario (`usp_sincronizar_usuario`) y
  PlanEstudio (`usp_registrar_o_actualizar_plan_estudio`) = `PRESENT`; commands JPA de SP = `JpaProcedureExecutor`;
  manejo tecnico generico de queries JPA = `JpaQueryExecutor`.

- Identidad de matrícula alineada con la DB (solo correo, solo documento o usuarios distintos → `ERR_IDENTIDAD_USUARIO_CONFLICTO`); `GEN_001` ya no se clasifica como tipo de identificación inexistente (→ `ERR_DB_UNCLASSIFIED`); DBCODE formales completados y `VAL_007` resuelto por operación.
- Validación: `clean verify` BUILD SUCCESS (1308 tests / 0 fallos) y `-Pintegration clean verify` BUILD SUCCESS (188 ITs / 0 fallos / 1 skip TD-044). Detalle en [VALIDATION](../work-items/LB-008-jpa-only-persistence-migration/VALIDATION.md).
- Observación DB: el catálogo `dbo.Estado` ya contiene `P`/`A`/`R` y la DB final cerró TD-057 (junto con TD-039/046/056/058); el Backend consume ese contrato final.
