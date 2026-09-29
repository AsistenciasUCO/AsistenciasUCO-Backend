# PLAN — LB-003: Quality Gate Golden Path

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-09-29 / 01-planificador (cierre documental de LB-002 y preparación de LB-003) / rama `jose-valencia/lb-002.2a-jpa-command-plan`.
- Objetivo y criterios de aceptación: certificar integralmente la [Definition of Done](../../baseline/DEFINITION_OF_DONE.md) del Golden Path de asistencia (registrar en lote + consultar + realtime) ya congelado por LB-001 y con persistencia híbrida JDBC/JPA cerrada por LB-002, antes de continuar hacia stateless/serverless, realtime distribuido o cloud (LB-004/005/006). Criterio de aceptación: cada área de la tabla DoD queda registrada como PASS, FAIL o NO APLICA con motivo, sin ningún check obligatorio en `NOT_RUN` presentado como PASS.
- Skills y fuentes autoritativas: [uco-baseline](../../../.claude/skills/uco-baseline/SKILL.md), [uco-testing](../../../.claude/skills/uco-testing/SKILL.md), [uco-contratos](../../../.claude/skills/uco-contratos/SKILL.md), [uco-seguridad](../../../.claude/skills/uco-seguridad/SKILL.md), [uco-observabilidad](../../../.claude/skills/uco-observabilidad/SKILL.md), [uco-realtime](../../../.claude/skills/uco-realtime/SKILL.md); [AGENTS.md](../../../AGENTS.md); [SOURCE_OF_TRUTH](../../governance/SOURCE_OF_TRUTH.md); [DEFINITION_OF_READY](../../governance/DEFINITION_OF_READY.md); [DEFINITION_OF_DONE](../../baseline/DEFINITION_OF_DONE.md); [LINEA_BASE](../../baseline/LINEA_BASE.md); [GOLDEN_PATH_ASISTENCIA](../../baseline/GOLDEN_PATH_ASISTENCIA.md); [TECHNICAL_DEBT](../../baseline/TECHNICAL_DEBT.md); [MANUAL_VALIDATION_LEDGER](../../baseline/MANUAL_VALIDATION_LEDGER.md); [TESTING_STANDARD](../../testing/TESTING_STANDARD.md); [BACKEND_GOLDEN_PATH_CONTRACT](../../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md); [openapi-golden-path.yaml](../../contracts/openapi/openapi-golden-path.yaml).

**Este documento es PLAN/DoR inicial. No implementa producción, no ejecuta `verify`/`-Pintegration`, no modifica tests ni código.**

## AS-IS certificado heredado de LB-001/LB-002

| Área | AS-IS certificado | Fuente |
|---|---|---|
| Contrato HTTP/OpenAPI | OpenAPI 3.1.2 canónico, 9 operaciones, SHA `72a3097b...6da54`; contrato backend SHA `9b4830b...8c62`; Swagger UI runtime PASS | [LB-001C CLOSURE](../LB-001C-openapi-contract-first/CLOSURE.md) |
| Contrato DB consumido | Snapshot congelado SHA `45e48c5a...9aec` (`gestionasistenciadb`) | [LB-001B.4 CLOSURE](../LB-001B.4-final-backend-contract-closure/CLOSURE.md) |
| Persistencia asistencia | `AsistenciaRepositoryHybridSqlServerAdapter`: query y command (`registrarAsistenciasSesion`) seleccionables JDBC/JPA; paridad SQL Server real `PARITY_MISMATCHES = 0`; fallback JDBC preservado; TD-001 `CLOSED` | [LB-002.2-FINAL-CLOSURE](../LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2-FINAL-CLOSURE.md), [TECHNICAL_DEBT#TD-001](../../baseline/TECHNICAL_DEBT.md#td-001) |
| Runtime manual local | `query=jpa`/`command=jpa` en `local`; Golden Path manual PASS; realtime dos clientes SSE PASS; reconexión PASS (~25 s); HTTP fuente de verdad | [LB-002.2E-CLOSURE](../LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2E-CLOSURE.md), MV-006 |
| Seguridad | `SEC_001/SEC_002/EST_004 -> 403`; DBCODE determinista; RBAC docente/coordinador/administrador; ownership en use case | [TECHNICAL_DEBT#TD-030](../../baseline/TECHNICAL_DEBT.md#td-030), [TECHNICAL_DEBT#TD-036](../../baseline/TECHNICAL_DEBT.md#td-036) |
| Build/gates (última corrida certificada, LB-002.2C/D) | `clean verify` 1426 tests 0F/0E/0S; JaCoCo LINE 90.63 % / BRANCH 81.17 %; ArchUnit PASS; Enforcer Java 25 PASS | [LB-002.2-FINAL-CLOSURE §Cobertura y gates](../LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2-FINAL-CLOSURE.md) |
| Integración global | `-Pintegration verify` `NOT_GREEN_TD043` (6 fallos preexistentes, no Golden Path) | [TECHNICAL_DEBT#TD-043](../../baseline/TECHNICAL_DEBT.md#td-043) |
| Realtime | SSE local por JVM, best-effort, sin distribución/durabilidad (deuda conocida, no defecto del Golden Path) | [TECHNICAL_DEBT#TD-003](../../baseline/TECHNICAL_DEBT.md#td-003) |

No se re-ejecuta ninguno de estos gates en este PLAN; se citan como evidencia previa, no como corrida de esta sesión.

## Definition of Done aplicable

La tabla completa de [DEFINITION_OF_DONE.md](../../baseline/DEFINITION_OF_DONE.md) aplica íntegra a LB-003 por ser una fase de certificación (no `DOCUMENTATION_ONLY`/`CONTRACT_ANALYSIS` puro): alcance/contrato, RED→GREEN, build, arquitectura, coverage, persistencia/providers, seguridad, operación, CI, documentación, higiene de evidencia y cierre. LB-003 no introduce comportamiento nuevo; certifica el ya construido. Cada fila deberá cerrar con PASS/FAIL/NO APLICA con motivo cuando LB-003 se ejecute; este PLAN no ejecuta esas certificaciones.

## Alcance por dimensión (a certificar en LB-003, no en este PLAN)

- **Seguridad positiva y negativa:** 401/403 por endpoint del Golden Path (autenticado sin rol, rol sin ownership, docente ajeno a la sesión/grupo); 2xx solo con rol y ownership correctos. Evidencia previa parcial: `AsistenciaRepositorySqlServerIT` (docente ajeno → FORBIDDEN), pero sin matriz negativa completa consolidada en un solo documento.
- **Errores seguros:** ningún 500 para `SEC_001/SEC_002/ATT_001-3/SES_003/SES_004/GEN_002` (ya resuelto por TD-036, CLOSED); confirmar que la matriz sigue vigente tras LB-002 (sin cambios de clasificador en LB-002).
- **correlationId:** presente en request/response/log/evento; confirmar propagación end-to-end incluyendo el candidato JPA del command (mismo `idCorrelacion` en el SP, ya validado por paridad en LB-002.2D).
- **Auditoría:** `AuditEventJdbcRepository` sigue escribiendo directo a `dbo.AuditoriaEvento` (TD-010, ABIERTA, no bloqueante para LB-000 pero relevante para certificar auditoría del Golden Path en LB-003).
- **Write → read (consistencia tras escritura):** `POST /asistencias/lote` → `GET /grupos/{grupoId}/asistencias` debe reflejar el registro inmediatamente vía HTTP, para ambos providers de persistencia (JDBC y JPA). Evidencia previa: readback JDBC↔JPA con paridad 0 mismatches (LB-002.2D) y readback manual (LB-002.2E); falta consolidar como caso de aceptación único de LB-003.
- **Realtime:** `ASISTENCIAS_SESION_ACTUALIZADAS` tras escritura exitosa; dos clientes SSE; evidencia manual PASS (MV-006). TD-003 (SSE por JVM, sin distribución) permanece como límite conocido, no defecto.
- **Recuperación HTTP tras reconexión:** reconexión SSE con reconvergencia observada ~25 s vía relectura HTTP como fuente de verdad (MV-006). LB-003 debe decidir si ese tiempo es aceptable para el DoD o si se documenta como límite con TD-050 (latencia de reconciliación, ABIERTA/NON_BLOCKING).
- **E2E:** Golden Path completo (autenticación → lote → SSE → consulta) con JDBC y con JPA activo; evidencia manual disponible (MV-001, MV-006); sin corrida automatizada E2E en CI.
- **Contratos OpenAPI:** parser/`$ref`/SHA/conformance ya PASS en LB-001C; LB-003 debe reconfirmar que no hubo drift tras LB-002 (LB-002 no tocó HTTP/OpenAPI, según su CLOSURE).
- **SQL Server integration:** `-Pintegration verify` sigue `NOT_GREEN_TD043`; LB-003 debe decidir si certifica el Golden Path con integración targeted (ya 59/59 PASS en LB-002.2D) como suficiente, dado que el perfil global depende de TD-043 (NON-GOLDEN).
- **ArchUnit:** PASS heredado (Domain/Application sin JPA fuera de `..sqlserver.jpa..`, sin `@Transactional`/`JpaTransactionManager`); confirmar que las reglas de aislamiento (`JpaCommandIsolationRulesTest`, visible en el work item activo) siguen verdes.
- **JaCoCo:** LINE ≥80 %/BRANCH ≥70 % exigidos por `pom.xml`; última cifra certificada 90.63 %/81.17 % (LB-002.2C/D); re-certificar tras cualquier cambio de LB-003.
- **CI:** checks remotos descritos en `.github/CI.md`; MV-004 (CI remoto y Rulesets) sigue `PENDIENTE`.
- **Evidencia manual:** MV-001 (Golden Path frontend/SSE, `PASS_REPORTED_EXTERNAL`) y MV-006 (JPA runtime + realtime, `PASS_REPORTED_EXTERNAL`) disponibles; MV-002/003/004 siguen `PENDIENTE`.

## Deuda que puede bloquear LB-003

| ID | Descripción resumida | Estado actual | Clasificación para el DoR de LB-003 | Motivo |
|---|---|---|---|---|
| [TD-008](../../baseline/TECHNICAL_DEBT.md#td-008) | Validación runtime de sesión/asistencia sin corrida certificada contra la DB actual al momento de su apertura (LB-000) | ABIERTA | **NON-BLOCKER para iniciar LB-003** | La propia ficha declara `Bloquea línea base: sí, LB-003`, pero la resolución esperada ("IT real con fixtures, cero skips relevantes, versión DB y E2E trazables") es precisamente el trabajo que LB-003 debe producir como parte de su certificación E2E/SQL Server real. Exigir la corrida *antes* de planificar LB-003 invierte el orden: LB-003 es el vehículo de cierre de TD-008, no una fase bloqueada por él. Bloquea, en cambio, **declarar LB-003 `DONE`** sin esa corrida. Evidencia parcial ya existe post-LB-000 (`GoldenPathSqlStoredProcedureContractIT` 16/16, `AsistenciaRepositorySqlServerIT` 6/6, paridad LB-002.2D) que LB-003 debe consolidar, no repetir desde cero. |
| [TD-023](../../baseline/TECHNICAL_DEBT.md#td-023) | CI DB reproducible y gates remotos; integration workflow manual, DB externa sin provisioning versionado cross-repo | ABIERTA | **NON-BLOCKER para iniciar LB-003 / DEFERRED para su alcance de CI** | La ficha marca `Bloquea línea base: sí, LB-003/006 según alcance`. El alcance de CI-DB-reproducible-en-pipeline es una evolución de infraestructura (ver [DEFINITION_OF_READY](../../governance/DEFINITION_OF_READY.md) — `INFRASTRUCTURE` requiere su propio PLAN/TEST_PLAN). LB-003 puede certificar el Golden Path con la evidencia manual/targeted-integration ya disponible (ejecutada por el usuario, documentada en VALIDATION) sin depender de que el CI reproduzca la DB automáticamente. La resolución completa de TD-023 (MV-004, CI remoto) queda `DEFERRED` dentro de LB-003 o se desacopla a LB-006 si excede su alcance; no impide planificar ni comenzar LB-003. |
| [TD-043](../../baseline/TECHNICAL_DEBT.md#td-043) | `NON_GOLDEN_DB_CONTRACT_DRIFT`: 3 SP consumidos por el backend no existen en la DB oficial | **OPEN / DEFERRED** (sin cambios) | **DEFERRED (ya clasificada)** | Clasificación normativa vigente sin cambios: `Bloquea línea base: no para el Golden Path; sí para liberar las tres features anteriores`. Las tres capabilities afectadas (sincronización de usuario, plan de estudio, inscripción de estudiante nuevo) están fuera del Golden Path de asistencia que LB-003 certifica. No se reclasifica ni se resuelve en este PLAN; sigue como excepción NON-GOLDEN reconocida, igual que en LB-002.1C/LB-002.2. |

No se resuelve ninguna de las tres deudas en este documento. La tabla responde únicamente a si bloquean el **inicio** de LB-003 (DoR), no si LB-003 puede cerrarse sin abordarlas: TD-008 y TD-023 deben tener resultado registrado (PASS/FAIL/NO APLICA con motivo) antes de que LB-003 se declare `DONE`, conforme al DoD.

## Clase de cambio y alcance de rutas (de este PLAN)

- Change class: DOCUMENTATION_ONLY.
- PRIMARY_VARIABLE (una sola): preparar el DoR de LB-003 (planificación), sin tocar código ni tests.
- ALLOWED_PATHS: `docs/work-items/LB-003-quality-gate-golden-path/**` (este documento y futuros TEST_PLAN/VALIDATION/CLOSURE de LB-003, cuando se autoricen).
- FORBIDDEN_PATHS: `src/main/**`, `src/test/**`, `pom.xml`, SQL/DB, frontend, configuración runtime (`application*.yml`, `.env*`), workflows CI, y cualquier documento normativo fuera de lo estrictamente necesario para registrar este PLAN (los ajustes de cierre de LB-002/TD-001/MV-006/LINEA_BASE/GOLDEN_PATH_ASISTENCIA se hicieron en la tarea de cierre que antecede a este PLAN, no aquí).
- CONTRACTS: ninguno modificado; solo referenciados como AS-IS.
- PROVIDERS: ninguno modificado.
- EXTERNAL_ENVIRONMENT: no requerido para este PLAN (no se ejecuta `verify` ni `-Pintegration` desde este documento).
- SECURITY_IMPACT: ninguno (documental).
- OBSERVABILITY_IMPACT: ninguno (documental).
- TEST_LEVEL_REQUIRED: NO APLICA (PLAN, no implementación).
- ROLLBACK: eliminar/corregir este archivo si se detecta un AS-IS incorrecto; no hay cambio de código que revertir.
- CONSUMERS: ninguno afectado.
- STOP_CONDITIONS: `CONTRACT_CONFLICT`/`TEST_CONTRACT_CONFLICT`/`BLOCKED_BY_MISSING_EVIDENCE` si aparece contradicción entre fuentes activas al certificar en la fase de implementación de LB-003 (no detectada en este PLAN).

## Alcance

Producir el PLAN/DoR inicial de LB-003 (esta tarea) para que una sesión de implementación futura, ya autorizada, pueda decidir REQUIREMENT → CONTRACT → TEST_PLAN → RED → GREEN → VALIDATE por cada área de la DoD listada arriba.

## No alcance

- No se ejecuta `mvn verify` ni `-Pintegration verify` desde este documento.
- No se escribe código de producción ni de test.
- No se modifican OpenAPI, contratos DB, seguridad, realtime ni observabilidad.
- No se resuelve TD-008, TD-023 ni TD-043.
- No se inicia la implementación de LB-003.

## Archivos afectados

- NUEVOS: este `PLAN.md`.
- EXISTENTES: ninguno modificado por este documento (los cambios de cierre de LB-002/TD-001/MV-006/LINEA_BASE/GOLDEN_PATH_ASISTENCIA pertenecen a la tarea de cierre documental que antecede a este PLAN, registrada en sus propios documentos).

## Contratos y consumidores afectados

Ninguno modificado. DOMAIN/HTTP/PERSISTENCE/SECURITY/REALTIME se citan solo como AS-IS heredado (ver tabla arriba); sin cambio de compatibilidad.

## Riesgos y dependencias

- Riesgo: iniciar LB-003 asumiendo que TD-008/TD-023 ya están resueltas por evidencia previa de LB-002, sin registrar explícitamente el PASS/FAIL de cada check de la DoD. Mitigación: LB-003 debe producir su propio TEST_PLAN/VALIDATION que cite y, donde falte, complete esa evidencia.
- Riesgo: TD-043 se confunda con un bloqueador del Golden Path. Mitigación: mantener la clasificación NON-GOLDEN vigente, ya reafirmada por LB-002.1C/LB-002.2.
- Dependencia: disponibilidad de SQL Server real y ambiente `local` para cualquier corrida de integración/E2E que LB-003 planifique ejecutar (no en este PLAN).

## Test plan

NO APLICA en este documento (PLAN/DoR, sin implementación). LB-003 deberá producir su propio `TEST_PLAN.md` cuando se autorice su implementación.

## Rollback y stop conditions

Sin código ni configuración que revertir. Si una fuente activa referenciada aquí resulta contradictoria al pasar a implementación, registrar `CONTRACT_CONFLICT`/`BLOCKED_BY_MISSING_EVIDENCE` en el work item de LB-003 conforme a [AGENTS.md](../../../AGENTS.md) §"Cuándo detenerse".

## Deuda conocida y validación manual

Ver tabla "Deuda que puede bloquear LB-003" arriba (TD-008, TD-023, TD-043) y evidencia manual heredada (MV-001, MV-006). MV-002/003/004 siguen `PENDIENTE`, sin relación directa obligatoria con el Golden Path de asistencia, pero relevantes si LB-003 amplía su certificación a esas verticales.

## Definition of Ready

```text
LB-003 DEFINITION_OF_READY:
READY

LB-003 STATUS:
NOT_STARTED
```

**Justificación:** este PLAN es de clase `DOCUMENTATION_ONLY`/planificación, por lo que no exige build base verde ni contrato aprobado nuevo (ver [DEFINITION_OF_READY](../../governance/DEFINITION_OF_READY.md) §"READY para CONTRACT_ANALYSIS", aplicado por analogía a la planificación). Se cumplen sus condiciones: AS-IS heredado de LB-001/LB-002 identificado con evidencia versionada (tabla arriba); Golden Path definido ([GOLDEN_PATH_ASISTENCIA.md](../../baseline/GOLDEN_PATH_ASISTENCIA.md)); ningún secreto en la evidencia; cero `CONTRACT_CONFLICT` relevante abierto para el alcance de asistencia. Las únicas deudas con `Bloquea línea base: sí, LB-003` (TD-008, TD-023) son, por su propia "Resolución esperada", el contenido mismo del trabajo de certificación que LB-003 debe realizar — no precondiciones externas a resolver antes de planificarlo — y TD-043 ya está clasificada `NON_GOLDEN`/`OPEN / DEFERRED` sin cambios. Por lo tanto: **LB-003 puede planificarse y su DoR de planificación es READY**; la READY para **implementación** (según la sección correspondiente de [DEFINITION_OF_READY](../../governance/DEFINITION_OF_READY.md)) queda pendiente de un TEST_PLAN/contrato aprobado propio de LB-003, que este documento no produce. **No se implementa LB-003 en esta tarea.**
