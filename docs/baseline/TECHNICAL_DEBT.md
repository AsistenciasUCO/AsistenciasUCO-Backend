---
status: active
type: ledger
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# Ledger único de deuda técnica

TD-001..TD-005 conservan los IDs del paquete; los demás consolidan la documentación previa. Una brecha TARGET no significa un defecto AS-IS. No reciclar IDs. Fecha de incorporación: 2026-09-20; responsable de seguimiento: backend-team, con los equipos DB/identidad/frontend/operaciones donde se indica. Evidencia externa no aportada se declara pendiente; no se inventan aprobaciones ni corridas.

Las propuestas Redis, RabbitMQ, CQRS, MinIO, SBOM y técnicas avanzadas se mantienen en [LINEA_BASE](LINEA_BASE.md) como evoluciones, no como deuda que obligue a introducirlas.

| ID | Descripción | Estado | Bloquea línea base |
|---|---|---|---|
| [TD-001](#td-001) | Piloto JDBC → JPA pendiente | CLOSED (LB-002.2) | no |
| [TD-002](#td-002) | Contrato OpenAPI no consolidado | CLOSED (LB-001C.1) | no |
| [TD-003](#td-003) | Realtime local efímero y por JVM | ABIERTA | sí, LB-005 |
| [TD-004](#td-004) | Storage local y ownership | ABIERTA | sí, LB-004 |
| [TD-005](#td-005) | Política temporal DB/API | CLOSED_FOR_GOLDEN_PATH (wire HTTP congelado en LB-001C.1; política global no cerrada) | no para Golden Path |
| [TD-006](#td-006) | Identificación numérica | ABIERTA | no, salvo alcance de identificación |
| [TD-007](#td-007) | Códigos DB machine-readable | ABIERTA | sí, si bloquea contrato de errores LB-001/002 |
| [TD-008](#td-008) | Validación runtime de sesión/asistencia | CLOSED — LB-003 | no |
| [TD-009](#td-009) | Estados de asistencia y registro individual | ABIERTA | sí, para contrato afectado; individual fuera del Golden Path |
| [TD-010](#td-010) | Auditoría SQL con DML directo | ABIERTA | no para LB-000; revisar release DB |
| [TD-011](#td-011) | Outbox y consistencia durable | ABIERTA | sí, LB-005 si exige durabilidad |
| [TD-012](#td-012) | DataSource específico por provider | ABIERTA | no, salvo incorporar otro provider |
| [TD-013](#td-013) | Provisioning institucional incompleto | ABIERTA | no para Golden Path; sí antes de liberar provisioning afectado |
| [TD-014](#td-014) | Confianza del correo en IdP | ABIERTA | no para LB-000; sí release de identidad |
| [TD-015](#td-015) | Consistencia DB/IdP y commands multi-adapter | ABIERTA | no para Golden Path; sí alcance de reconciliación |
| [TD-016](#td-016) | Autorización contextual por completar | ABIERTA | sí cuando la vertical afectada lo requiera |
| [TD-017](#td-017) | Consumo frontend/SSE | CLOSED_FOR_GOLDEN_PATH (MV-001 PASS reportado externamente) | no para Golden Path |
| [TD-018](#td-018) | Diagnóstico realtime en producción | ABIERTA | no para LB-000; sí release realtime |
| [TD-019](#td-019) | Contratos DB ausentes o limitados | PENDIENTE DE EVIDENCIA DB | no para LB-000; sí cada vertical dependiente |
| [TD-020](#td-020) | Hashing DB y autenticación histórica | PENDIENTE DE VALIDACIÓN | no para LB-000; sí release de credenciales |
| [TD-021](#td-021) | Serialización común de errores de seguridad | CLOSED (LB-001C.1) | no |
| [TD-022](#td-022) | Exposición Actuator y Prometheus | ABIERTA | sí, LB-006 |
| [TD-023](#td-023) | CI DB reproducible y gates remotos | OPEN / PARTIAL (LB-003: ruleset/required checks OBSERVED, pendiente run sobre checkout actual) | sí, LB-003/006 según alcance |
| [TD-024](#td-024) | Verificación histórica de uploads | CERRADA EN ÍNDICE LOCAL; HISTORIAL NO EVALUADO | no |
| [TD-025](#td-025) | Limpieza oportunista de helpers | ABIERTA | no |
| [TD-026](#td-026) | Nombre FeaturesBeansConfigTest | ABIERTA | no |
| [TD-027](#td-027) | Evidencia operacional Azure/telemetría | ABIERTA | sí, LB-006; según capacidad modificada |
| [TD-028](#td-028) | Conflicto guía de métodos HTTP | CLOSED (LB-001C.1, API_DESIGN_RULES) | no |
| [TD-029](#td-029) | Gate ArchUnit preexistente fallido | CERRADA (TECH-001) | no |
| [TD-030](#td-030) | VAL_003 usado para fallos de autorización/titularidad en SPs internos | RESOLVED BY FROZEN DB BASELINE (LB-001B.4) | no |
| [TD-031](#td-031) | Frontend: orden de interceptores puede anular el refresco silencioso tras un 401 HTTP | CERRADA (frontend LB-001B.1C) | no |
| [TD-032](#td-032) | Frontend: credencial por defecto embebida en el flujo de matrícula (SECURITY_FINDING) | CERRADA (frontend LB-001B.1C) | no |
| [TD-033](#td-033) | Frontend: fallos de carga de sesiones y del canal realtime no se muestran al usuario | CERRADA (frontend LB-001B.1C) | no |
| [TD-034](#td-034) | Código muerto: `SesionErrorCode.ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA` sin lanzador tras LB-001B.1 | ABIERTA | no |
| [TD-035](#td-035) | Frontend: `attendance.mapper.ts` mantiene un contrato paralelo (`studentFromDTO()`/`ClassSessionDTO`) con residuo `estado_asistencia \|\| 'AN'` | CERRADA (frontend LB-001B.1C) | no |
| [TD-036](#td-036) | Mapeo determinista de errores DB (`SEC_001/SEC_002/ATT_001-3/SES_003/SES_004/GEN_002`) sin señal de clasificación formal | CLOSED (LB-001B.4) | no |
| [TD-037](#td-037) | `SesionMateriaEstudianteSqlServerAdapter` lee `Sesion.fechaHoraInicio/fechaHoraFin` con `toLocalDateTime` dependiente de `TimeZone.getDefault()`, no UTC fijo | CLOSED (LB-001B.4) | no |
| [TD-038](#td-038) | `ReporteAsistenciaSqlServerAdapterTest` no asevera `fechaHoraInicio`/`fechaHoraFin`; no detectaría una regresión de `toLocalDateTimeUtc` a `toLocalDateTime` | CLOSED (LB-001B.4) | no |
| [TD-039](#td-039) | Firma exacta de parámetros de `usp_cerrar_sesion` no documentada en `DB_BASELINE_CONTRACT.md` | BLOCKED_BY_MISSING_EVIDENCE | no para Golden Path (endpoint fuera de alcance) |
| [TD-040](#td-040) | `uv_estudiante_identidad`/`uv_usuario` no documentadas en el contrato DB, usadas por `GET /api/v1/grupos/{grupoId}/estudiantes` | CLOSED — EVIDENCE_RESOLVED (LB-001B.4; salvedad: TD-046) | no |
| [TD-041](#td-041) | Rama DB origen `feat/db-golden-path-baseline-freeze` no fusionada a `main`/`develop` del repo DB | ABIERTA | sí, si el snapshot congelado se desactualiza antes del freeze de LB-001C |
| [TD-042](#td-042) | Test de serialización ISO-8601 UTC de `RealtimeEvent.occurredAt` sin contexto Spring real | CLOSED (LB-001B.4A, `RealtimeEventResponseSpringJsonTest` con `@SpringBootTest`) | no |
| [TD-043](#td-043) | NON_GOLDEN_DB_CONTRACT_DRIFT: 3 SP consumidos por el backend no existen en la DB oficial (`usp_sincronizar_usuario`, `usp_registrar_o_actualizar_plan_estudio`, `usp_registrar_estudiante_en_grupo_usuario_no_existente`) | ABIERTA / DEFERRED (LB-002.1C; `NOT_GREEN_TD043`) | no (fuera del Golden Path LB-001); sí antes de liberar esas features |
| [TD-044](#td-044) | 2 skips de `DocenteRepositorySqlServerIT` (`assumeTrue` por datos, sin fixture propio) | ABIERTA | no (fuera del Golden Path) |
| [TD-045](#td-045) | Riesgo de `CPI`/`CPVP` históricos en `RazonCausa` frente a la lectura fail-closed de estado | ABIERTA | no |
| [TD-046](#td-046) | Vistas `uv_estudiante_grupo`, `uv_estudiante_identidad`, `uv_usuario` sin documentar en `DB_BASELINE_CONTRACT.md` | ABIERTA (acción equipo DB) | no |
| [TD-047](#td-047) | `USU_001` sin mapeo formal DBCODE (cae en `ERR_DB_UNCLASSIFIED`) | ABIERTA | no (fuera del Golden Path) |
| [TD-048](#td-048) | `Sesion.nombre` es `nvarchar(50)` en DB pero el backend valida 1..150 (nombres de 51..150 fallarían en persistencia como error técnico) | CLOSED (LB-001B.4B) | no |
| [TD-049](#td-049) | `/usuarios/perfil` responde 501 | ABIERTA / OUT_OF_GOLDEN_PATH / NON_BLOCKING | no |
| [TD-050](#td-050) | Latencia de reconciliación realtime observada en MV-001 | ABIERTA / NON_BLOCKING | no |
| [TD-051](#td-051) | Webhook Azure con credencial default funcional insegura | RESUELTA (LB-001D.2) | no |
| [TD-052](#td-052) | Webhook Azure acepta credencial por URL/query | RESUELTA (LB-001D.2) | no |
| [TD-053](#td-053) | Cloud Integration Azure corre como test normal | RESUELTA (LB-001D.2) | no |
| [TD-054](#td-054) | Semántica Azure→realtime sin decisión explícita | DECISION_REQUIRED | no para documentación; sí para cambio realtime futuro |

## TD-001

- **Descripción:** Piloto JDBC → JPA pendiente. La persistencia vigente era JDBC; brecha respecto al objetivo, no un defecto funcional.
- **Impacto:** Medio.
- **Evidencia:** pom.xml; adapters persistence/sqlserver; work item [LB-002-jpa-incremental](../work-items/LB-002-jpa-incremental/).
- **Motivo:** LB-002 requería contrato y paridad antes de activar JPA junto al fallback JDBC.
- **Resolución esperada:** Golden Path con JPA/paridad y rollback certificados.
- **Bloquea línea base:** no (cerrada).
- **Estado:** **CLOSED (2026-09-29, LB-002.2).** Condición de cierre cumplida: JPA query pilot PASS
  (LB-002.0/LB-002.1); JPA query activation PASS en `local`/`dev` (LB-002.1B); JPA command pilot PASS
  para `registrarAsistenciasSesion` (LB-002.2A-C); paridad JDBC↔JPA en SQL Server real
  `PARITY_MISMATCHES = 0` (LB-002.2D); fallback JDBC preservado en Composition Root (selectores
  independientes, default `jdbc`, fail-closed); runtime local JPA (`query=jpa`, `command=jpa`) PASS;
  Golden Path manual PASS, incluyendo realtime (dos clientes SSE y reconexión, reconvergencia ~25 s)
  (LB-002.2E). No se afirma retiro de JDBC: los demás commands de `Asistencia` permanecen en JDBC,
  sin cambio. Ver [LB-002.2-FINAL-CLOSURE](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2-FINAL-CLOSURE.md).

## TD-002

- **Descripción:** Contrato OpenAPI no consolidado. Históricamente no existía spec aprobada ni gate.
- **Impacto:** Alto.
- **Evidencia:** contracts/openapi/README.md; .github/workflows/backend-ci.yml.
- **Motivo:** LB-001 debe reunir contrato/consumidores.
- **Resolución esperada:** Spec aprobada, compatibilidad y validación automatizada.
- **Bloquea línea base:** no; cerrada en LB-001C.1 para las 8 operaciones cubiertas.
- **Estado:** **CLOSED (2026-09-24, LB-001C.1).** OpenAPI 3.1.2 canónico en
  `docs/contracts/openapi/openapi-golden-path.yaml`, SHA-256 adyacente y gate en `mvn verify`
  mediante Swagger Parser + conformance controller/DTO. Alcance: 8 operaciones Golden Path; las
  operaciones fuera de alcance siguen gobernadas por AS-IS hasta contrato propio.

## TD-003

- **Descripción:** Realtime local efímero y por JVM. No distribuye entre réplicas, no replay/durabilidad; consumidor lento puede perder eventos y dropped mide rechazos globales.
- **Impacto:** Alto al escalar.
- **Evidencia:** [ReactorRealtimeAdapter](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/realtime/localsse/ReactorRealtimeAdapter.java); reactive-realtime.md.
- **Motivo:** Diseñar distribución solo tras ADR y necesidad operacional.
- **Resolución esperada:** Pruebas multi-instancia y recuperación según contrato aprobado.
- **Bloquea línea base:** sí, LB-005.
- **Estado:** ABIERTA.
- **Actualización LB-001B.3 (2026-09-23):** hallazgo re-confirmado formalmente por auditoría de LB-001B.3 (`CONTRACT_MATRIX.md#m-24`, `TASK_AUTORIZADA.md` §26 de ese work item — auditoría de "serverless readiness" del Golden Path). Mismo componente, mismo diagnóstico (`Sinks.many().multicast().directBestEffort()`, Javadoc propio ya lo declara "best-effort... efímero"); no se abre un ID nuevo para evitar duplicar deuda. Sin refactor en esa fase, conforme a `TASK_AUTORIZADA.md` §26 ("No implementar infraestructura serverless ahora... registrar hallazgos en deuda técnica").

## TD-004

- **Descripción:** Storage local y ownership. ArchivoController usa filesystem; FileStoragePort/caso de uso de storage faltan; /archivos/** solo exige autenticación.
- **Impacto:** Alto en serverless/seguridad.
- **Evidencia:** ArchivoController; [runtime-security-provider-architecture.md](../security/runtime-security-provider-architecture.md).
- **Motivo:** Necesita contrato funcional de propietario/recurso/grupo antes de puerto y adapter.
- **Resolución esperada:** InputPort/UseCase/Port con consumidor real, autorización contextual y almacenamiento adecuado.
- **Bloquea línea base:** sí, LB-004.
- **Estado:** ABIERTA.

## TD-005

- **Descripción:** Política temporal DB/API. Hay LocalDateTime en sesiones y timestamps de eventos Instant; UTC end-to-end no está acordado con DB.
- **Impacto:** Medio.
- **Evidencia:** application/features/sesion; application/secondaryports/realtime/RealtimeEvent.java.
- **Motivo:** No convertir horas existentes por inferencia.
- **Resolución esperada:** Contrato temporal por campo y tests con DB/frontend aprobados. LB-001B documenta el mapeo del consumer (`date/startTime/endTime` reciben el datetime completo) como [DR-003](../work-items/LB-001B-backend-frontend-asistencia/CONTRACT_MATRIX.md#dr-003--representación-temporal-de-sesión); no resuelve timezone.
- **Bloquea línea base:** sí, LB-001 si afecta sus campos.
- **Estado:** CLOSED_FOR_GOLDEN_PATH (LB-001C.1). Wire HTTP congelado; no equivale a política temporal global cerrada.
- **Actualización LB-001B.3 (2026-09-23, avance parcial, NO cierre):** `CONTRACT_FREEZE.md` §5 de ese work item introdujo `JdbcValueMapper.toLocalDateTimeUtc(Object)`, que decodifica `Sesion.fechaHoraInicio`/`fechaHoraFin` como UTC fijo ignorando `TimeZone.getDefault()` en el momento de la lectura, verificado con test real (no solo Javadoc). Aplicado y GREEN en 2 de 3 llamadores confirmados (`SesionRepositorySqlServerAdapter`, `ReporteAsistenciaSqlServerAdapter`); el tercero (`SesionMateriaEstudianteSqlServerAdapter`) permanece con el defecto original por un `TEST_CONTRACT_CONFLICT` sin resolver (ver [TD-037](#td-037)). La representación HTTP (wire format) no cambió, sigue `READY_FOR_OPENAPI_DECISION` conforme a la tarea de ese work item §22. **No se cierra esta deuda**: persistencia parcialmente resuelta, contrato HTTP sin decidir. Ver [LB-001B.3 CLOSURE](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md).
- **Actualización LB-001B.4 (2026-09-23):** para el Golden Path queda resuelta la persistencia/realtime: los lectores de `Sesion.fechaHoraInicio/fechaHoraFin` usan `toLocalDateTimeUtc` (TD-037 cerrada), `occurredAt` es `Instant` UTC con `Z` y `Horario` es hora local institucional ([contrato](../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md) §I). **No se declara cerrada una migración temporal global**: la representación HTTP final de `LocalDateTime` (sin zona) se congela en LB-001C. Ver [CLOSURE](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).
- **Actualización LB-001C.1 (2026-09-24):** el wire AS-IS de sesión queda congelado como ISO local
  sin offset, con semántica UTC persistida y extensión explícita en OpenAPI. Es
  `MIGRATION_CANDIDATE`, no RFC 3339 UTC fingido. La política temporal global continúa fuera de
  alcance; TD-005 ya no bloquea el contrato Golden Path.

## TD-006

- **Descripción:** Identificación numérica. numeroIdentificacion continúa Integer en Java; documento histórico describe INT DB. Migración String/VARCHAR exige contrato externo.
- **Impacto:** Medio.
- **Evidencia:** archive/pendientes-arquitectura.md; requests y DTO de usuario.
- **Motivo:** Mantener JSON NUMBER vigente; no afirmar tipo DB desplegado sin DDL.
- **Resolución esperada:** Contrato coordinado DB/HTTP/frontend y migración compatible.
- **Bloquea línea base:** no, salvo alcance de identificación.
- **Estado:** ABIERTA.

## TD-007

- **Descripción:** Códigos DB machine-readable. Clasificador depende de operación/mensajes; falta código estable junto a estadoResultado/mensajeUsuarioResultado/mensajeTecnicoResultado. Caso histórico GEN_001 en usp_validar_tipo_identificacion_exista_por_id_interno puede terminar ERR_DB_UNCLASSIFIED.
- **Impacto:** Alto diagnóstico.
- **Evidencia:** [DbFailureClassifier](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/support/error/DbFailureClassifier.java); archive/pendientes-arquitectura.md.
- **Motivo:** Requiere equipo DB, no inventar codigoResultado.
- **Resolución esperada:** Catálogo DB liberado y traducción/tests por código estable.
- **Bloquea línea base:** sí, si bloquea contrato de errores LB-001/002.
- **Estado:** ABIERTA.

## TD-008

- **Descripción:** Validación runtime de sesión/asistencia sobre la DB actual.
- **Impacto:** Alto mientras estuvo abierta.
- **Evidencia de cierre:** [LB-003 VALIDATION](../work-items/LB-003-quality-gate-golden-path/VALIDATION.md) registra la suite targeted sobre SQL Server 16.0 / `gestionasistenciadb`: 59 tests, 0 failures, 0 errors y 0 skips, con fixtures reales, firma Golden Path 16/16, write/read, paridad JDBC/JPA, atomicidad, concurrencia y realtime posterior al commit. [MV-006](MANUAL_VALIDATION_LEDGER.md) conserva la validación runtime/E2E manual de POST, readback, AN/SJC/EX, dos clientes SSE y reconvergencia HTTP.
- **Motivo del cierre:** se satisfizo la resolución esperada: IT real con fixtures, cero skips relevantes, versión DB y E2E trazables. La certificación no depende de inspección estática.
- **Resolución esperada:** cumplida por LB-003.
- **Bloquea línea base:** no; condición resuelta para LB-003.
- **Estado:** CLOSED — LB-003 (2026-09-29).

## TD-009

- **Descripción:** Estados de asistencia y registro individual. Java lote admite AN/SJC/EX. Históricos A/T y mapeo idEstadoAsistencia individual no tienen contrato DB inequívoco; registrarAsistencia lanza FeatureUnavailableException.
- **Impacto:** Alto.
- **Evidencia:** RegistroAsistenciaSesionDomain; [AsistenciaRepositorySqlServerAdapter](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/core/AsistenciaRepositorySqlServerAdapter.java); archive/pendientes-arquitectura.md.
- **Motivo:** No extrapolar enum de lote al registro individual.
- **Resolución esperada:** Contrato DB liberado y semántica individual acordada; paridad/negativos certificados. LB-001B añade el lado de **lectura**: `GET …/asistencias` devuelve `codigoRazonCausa` sin filtrar y el consumer solo admite AN/SJC/EX ([DR-006](../work-items/LB-001B-backend-frontend-asistencia/CONTRACT_MATRIX.md#dr-006--dominio-de-estado-en-lectura)).
- **Bloquea línea base:** sí, para contrato afectado; individual fuera del Golden Path.
- **Estado:** ABIERTA.

## TD-010

- **Descripción:** Auditoría SQL con DML directo. AuditEventJdbcRepository escribe/consulta dbo.AuditoriaEvento directamente, excepción conocida a SP público/views. Prioridad P1 histórica.
- **Impacto:** Alto.
- **Evidencia:** infrastructure/audit/adapter/sqlserver/AuditEventJdbcRepository.java; [infrastructure-structure.md](../architecture/infrastructure-structure.md).
- **Motivo:** Requiere SPs DB; nombres usp_RegistrarEventoAuditoria/usp_ConsultarEventoAuditoriaPorCorrelationId son propuestas, no objetos comprobados.
- **Resolución esperada:** Contratos públicos liberados y adapter con CanonicalStoredProcedureExecutor, pruebas de auditoría.
- **Bloquea línea base:** no para LB-000; revisar release DB.
- **Estado:** ABIERTA.

## TD-011

- **Descripción:** Outbox y consistencia durable. Outbox puede requerir estructura DB; hoy SQL/realtime no tienen atomicidad distribuida.
- **Impacto:** Medio.
- **Evidencia:** archive/pendientes-arquitectura.md; [reactive-realtime.md](../architecture/reactive-realtime.md).
- **Motivo:** Evolución condicionada por necesidades de entrega y equipo DB.
- **Resolución esperada:** ADR de garantías, contrato DB y pruebas de recuperación si se adopta.
- **Bloquea línea base:** sí, LB-005 si exige durabilidad.
- **Estado:** ABIERTA.

## TD-012

- **Descripción:** DataSource específico por provider. Solo SQL Server; antes de otro motor o provider no JDBC debe separarse ownership/config del DataSource.
- **Impacto:** Medio.
- **Evidencia:** [adapter-composition-standard.md](../architecture/adapter-composition-standard.md); infrastructure-structure.md.
- **Motivo:** No crear abstracción especulativa con un solo provider.
- **Resolución esperada:** Segundo provider no fuerza DataSource SQL Server y wiring probado.
- **Bloquea línea base:** no, salvo incorporar otro provider.
- **Estado:** ABIERTA.

## TD-013

- **Descripción:** Provisioning institucional incompleto. CrearCoordinadorUseCaseImpl y RegistrarDocenteDesdeUsuarioUseCaseImpl no llaman Identity; POST /usuarios provisiona siempre ESTUDIANTE y está bajo authenticated(). Falta política de quién puede provisionar cada rol.
- **Impacto:** Alto.
- **Evidencia:** keycloak-identity-provider.md; [runtime-security-provider-architecture.md](../security/runtime-security-provider-architecture.md).
- **Motivo:** Resolver contrato funcional/roles, sin inventar permisos.
- **Resolución esperada:** Flujos/roles autorizados acordados, implementados y E2E (MV-002).
- **Bloquea línea base:** no para Golden Path; sí antes de liberar provisioning afectado.
- **Estado:** ABIERTA.

## TD-014

- **Descripción:** Confianza del correo en IdP. Adapter marca emailVerified=true suponiendo correo institucional confiable.
- **Impacto:** Alto.
- **Evidencia:** [KeycloakIdentityProviderAdapter](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/identity/keycloak/KeycloakIdentityProviderAdapter.java); keycloak-identity-provider.md §3.2.
- **Motivo:** Confirmación funcional necesaria antes de producción.
- **Resolución esperada:** Política aprobada y pruebas de verificación acordes.
- **Bloquea línea base:** no para LB-000; sí release de identidad.
- **Estado:** ABIERTA.

## TD-015

- **Descripción:** Consistencia DB/IdP y commands multi-adapter. DB-first puede confirmar y fallar luego en Identity; no hay rollback distribuido. Drift username/correo/idUsuario requiere reconciliación; compensación del adapter solo cubre identidad nueva dentro de su operación.
- **Impacto:** Alto.
- **Evidencia:** [keycloak-identity-provider.md](../security/keycloak-identity-provider.md) §§3,7,8; archive/backend-roadmap.md.
- **Motivo:** Requiere estrategia transaccional/reintentos/idempotencia aprobada.
- **Resolución esperada:** Recuperación/reconciliación trazable sin borrar cuenta preexistente ni rollback ficticio.
- **Bloquea línea base:** no para Golden Path; sí alcance de reconciliación.
- **Estado:** ABIERTA.

## TD-016

- **Descripción:** Autorización contextual por completar. Revisar GET sesión por ID, docente por ID/asignaciones y estudiante por ID. Consulta sesiones por grupo ya aplica titularidad en código; falta evidencia runtime. ABAC más fino no implementado.
- **Impacto:** Alto.
- **Evidencia:** [runtime-security-provider-architecture.md](../security/runtime-security-provider-architecture.md); ConsultarSesionesPorGrupoUseCaseImpl.
- **Motivo:** No improvisar ownership en controller ni confundir Layer 1 con Layer 2.
- **Resolución esperada:** Reglas aprobadas y pruebas positivas/negativas por recurso.
- **Bloquea línea base:** sí cuando la vertical afectada lo requiera.
- **Estado:** ABIERTA.

## TD-017

- **Descripción:** Consumo frontend/SSE sin evidencia runtime. El frontend no está en este checkout; hasta LB-001B no se demostraba cliente Bearer, reconexión, refresh ni actualización UI. Polling periódico no es realtime.
- **Impacto:** Alto.
- **Evidencia:** archive/frontend-contract-alignment.md; [reactive-realtime.md](../architecture/reactive-realtime.md). **LB-001B (2026-09-20):** análisis estático del consumer (`AsistenciasUCO-Frontend@71ee6d3`) con Bearer por header, reconexión con backoff, refresh de token y recarga HTTP tras evento/reconexión; suite frontend 97/97 en verde; ver [CONTRACT_MATRIX](../work-items/LB-001B-backend-frontend-asistencia/CONTRACT_MATRIX.md) (C-007, C-009…C-011).
- **Motivo:** El análisis estático no sustituye una corrida E2E con `USE_MOCKS=false`; no afirmar que está probado en runtime.
- **Resolución esperada:** Compatibilidad HTTP/eventos y MV-001/E2E.
- **Bloquea línea base:** no para Golden Path; otros escenarios realtime conservan sus gates propios.
- **Estado:** **CLOSED_FOR_GOLDEN_PATH (2026-09-24).** MV-001 fue reportado PASS por el responsable
  al autorizar LB-001C.1: SSE entre navegadores, offline→online y reconciliación HTTP. El artefacto
  externo no está en este checkout; ver
  [ENTRY_EVIDENCE](../work-items/LB-001C-openapi-contract-first/ENTRY_EVIDENCE.md). No certifica
  multi-instancia ni durabilidad (TD-003 permanece abierta).

## TD-018

- **Descripción:** Diagnóstico realtime en producción. POST /realtime/emit restringido a ADMINISTRADOR; falta decidir conservar, condicionar por property o retirar.
- **Impacto:** Medio.
- **Evidencia:** [RealtimeEventsController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeEventsController.java); SecurityConfig.
- **Motivo:** Decisión previa a producción, no bypass de casos de uso.
- **Resolución esperada:** Política y tests del comportamiento elegido.
- **Bloquea línea base:** no para LB-000; sí release realtime.
- **Estado:** ABIERTA.

## TD-019

- **Descripción:** Contratos DB ausentes o limitados. Histórico: sin command público Horario; usp_crear_grupo no persiste aula; usp_actualizar_grupo no persiste cupoMaximo/aula; uv_grupo usa cantidadEstudiantes como capacidadMaximaPermitida aunque inicia en 0. Falta definir SolicitudMatricula, PrerrequisitoAsignatura, Sede/EspacioFisico, DELETE físico Asignatura y DTO/alcance de reclamos sobre uv_solicitud_revision_asistencia.
- **Impacto:** Alto en verticales afectadas.
- **Evidencia:** Matriz original backend-baseline-contract; archive/pendientes-arquitectura.md; controllers con [FeatureUnavailableException](../../src/main/java/co/edu/uco/asistenciasuco/application/exception/business/FeatureUnavailableException.java).
- **Motivo:** Son hallazgos previos DB, no DDL probado en LB-000; algunos endpoints siguen bloqueados.
- **Resolución esperada:** Equipo DB aporta contratos públicos/versiones; luego contrato funcional e IT por capacidad, confirmar firmas antes de release.
- **Bloquea línea base:** no para LB-000; sí cada vertical dependiente.
- **Estado:** PENDIENTE DE EVIDENCIA DB.

## TD-020

- **Descripción:** Hashing DB y autenticación histórica. Documento antiguo no demostraba hashing de dbo.Usuario.password (nvarchar(500) NOT NULL según contrato previo). Java ya usa PasswordEncoderPort y existe UsuarioPasswordHashSqlServerIT.
- **Impacto:** Alto.
- **Evidencia:** sqlserver-connection.md; [UsuarioPasswordHashSqlServerIT](../../src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/integration/UsuarioPasswordHashSqlServerIT.java); keycloak-identity-provider.md.
- **Motivo:** No afirmar password plano actual ni cerrar hallazgo solo por test existente.
- **Resolución esperada:** Verificar contrato DB/filas de prueba sin imprimir valores y corrida IT; política conjunta DB/IdP.
- **Bloquea línea base:** no para LB-000; sí release de credenciales.
- **Estado:** PENDIENTE DE VALIDACIÓN.

## TD-021

- **Descripción:** Serialización común de errores de seguridad. SecurityErrorResponseWriter y ApiErrorResponse requieren revisar consistencia contractual.
- **Impacto:** Medio.
- **Evidencia:** archive/backend-roadmap.md; ambos tipos de Infrastructure.
- **Motivo:** No cambiar formato de 401/403 durante reorganización.
- **Resolución esperada:** Contrato común aprobado y tests de compatibilidad.
- **Bloquea línea base:** no.
- **Estado:** **CLOSED (2026-09-24, LB-001C.1).** `SecurityErrorResponseWriter` serializa el mismo
  `ApiErrorResponse`; el OpenAPI reutiliza `ApiErrorResponse` para 401/403 y `mvn verify` pasó los
  tests RBAC/seguridad. No se modificó producción.

## TD-022

- **Descripción:** Exposición Actuator y Prometheus. SecurityConfig permite /actuator/** y application.yml expone health/prometheus; topología de despliegue determina restricciones.
- **Impacto:** Alto según exposición.
- **Evidencia:** [SecurityConfig](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/security/SecurityConfig.java); application.yml; archive/backend-roadmap.md.
- **Motivo:** No declarar producción segura por defaults locales.
- **Resolución esperada:** Decisión de despliegue y controles verificados sin romper scrape autorizado.
- **Bloquea línea base:** sí, LB-006.
- **Estado:** ABIERTA.

## TD-023

- **Descripción:** CI DB reproducible y gates remotos. Integration workflow es manual; DB externa sin provisioning versionado cross-repo. Rulesets y umbrales Sonar remotos no se prueban desde YAML.
- **Impacto:** Medio.
- **Evidencia:** [integration.yml](../../.github/workflows/integration.yml); backend-ci.yml; .github/CI.md.
- **Motivo:** No convertir integración en required check sin DB CI reproducible.
- **Resolución esperada:** CI DB controlada/versionada, evidencia remota y MV-004.
- **Bloquea línea base:** sí, LB-003/006 según alcance.
- **Estado:** OPEN / PARTIAL.
- **Actualización LB-003 (2026-09-29) — gobernanza remota OBSERVED, deuda NO cerrada:** para la
  revisión `0bfc02a` se observaron Backend CI/Quality Gate y SonarQube Cloud SUCCESS, Backend
  Security y CodeQL SUCCESS, y Dependency Review SUCCESS en el PR inmediato. Revisión humana
  autenticada de GitHub confirmó además el ruleset `Protect develop` (`status: active`,
  `target: refs/heads/develop`; PR requerido, `deletion`/`non-fast-forward` bloqueados, sin bypass)
  y sus 4 required status checks: `Backend Quality Gate`, `CodeQL Java Analysis`,
  `Dependency Review`, `SonarCloud Code Analysis`. Ruleset + required checks quedan **OBSERVED**.
  Lo pendiente para cerrar el gate LB-003 es la ejecución remota de esos 4 checks sobre el
  commit/PR que contenga los cambios actuales de LB-002/LB-003; los runs citados corresponden solo
  al commit base. [MV-004](MANUAL_VALIDATION_LEDGER.md) queda `PARTIAL / PENDING` y LB-003 queda
  `BLOCKED_PENDING_REMOTE_CI`. La deuda de CI DB reproducible/versionada se mantiene como pendiente
  posterior, previsiblemente LB-006, salvo nueva decisión normativa. Ver
  [LB-003 VALIDATION](../work-items/LB-003-quality-gate-golden-path/VALIDATION.md).

## TD-024

- **Descripción:** Verificación histórica de uploads. Runbook previo advertía posibles uploads/soportes ya indexados. git ls-files uploads/ está vacío en LB-000.
- **Impacto:** Bajo local; histórico no auditado.
- **Evidencia:** archive/repository-cleanup.md; inventario LB-000.
- **Motivo:** No borrar archivos ni reescribir historial; evaluar remoto si se acredita publicación sensible.
- **Resolución esperada:** Índice local ya limpio; análisis histórico solo si procede.
- **Bloquea línea base:** no.
- **Estado:** CERRADA EN ÍNDICE LOCAL; HISTORIAL NO EVALUADO.

## TD-025

- **Descripción:** Limpieza oportunista de helpers. ObjectHelper/CrosscuttingException se revisan solo al tocar código relacionado.
- **Impacto:** Bajo.
- **Evidencia:** archive/backend-roadmap.md.
- **Motivo:** Evitar refactor global fuera del alcance.
- **Resolución esperada:** Mejora justificada por necesidad y pruebas relevantes.
- **Bloquea línea base:** no.
- **Estado:** ABIERTA.

## TD-026

- **Descripción:** Nombre FeaturesBeansConfigTest. Prueba SqlServerCoreRepositoryAdapterConfiguration; nombre engañoso tras refactor.
- **Impacto:** Bajo.
- **Evidencia:** infrastructure/config/FeaturesBeansConfigTest.java; archive/infrastructure-refactor-mapping.md.
- **Motivo:** Cosmético, cero Java en LB-000.
- **Resolución esperada:** Nombre coherente cuando se autorice limpieza del test.
- **Bloquea línea base:** no.
- **Estado:** ABIERTA.

## TD-027

- **Descripción:** Evidencia operacional Azure/telemetría. Existen capabilities, adapters, Composition Root, caches e invalidación Azure documentados; no hay artefacto vigente que certifique el ambiente real completo.
- **Impacto:** Medio.
- **Evidencia:** [adapter-composition-standard.md](../architecture/adapter-composition-standard.md); application.yml; infra/observability.
- **Motivo:** No confundir implementación AS-IS con validación de ambiente.
- **Resolución esperada:** MV-003 con versión/ambiente y evidencia sanitizada.
- **Bloquea línea base:** sí, LB-006; según capacidad modificada.
- **Estado:** ABIERTA.

## TD-028

- **Descripción:** Conflicto guía de métodos HTTP. La guía prescribe POST para commands mientras la API vigente contiene PUT/PATCH/DELETE. CORS sí permite esos métodos.
- **Impacto:** Medio.
- **Evidencia:** [http-command-query-guidelines.md](../architecture/http-command-query-guidelines.md); SecurityConfig; HTTP_AS_IS_MATRIX.md.
- **Motivo:** CONTRACT_CONFLICT CF-001: no reinterpretar por cuenta del agente.
- **Resolución esperada:** Equipo de contrato decide política para nuevos commands y compatibilidad de los existentes.
- **Bloquea línea base:** no.
- **Estado:** **CLOSED (2026-09-24, LB-001C.1).** CF-001 fue resuelto por
  [API_DESIGN_RULES](../governance/API_DESIGN_RULES.md): recursos pragmáticos + business commands,
  PUT/DELETE nuevos restringidos con `METHOD_EXCEPTION`, y compatibilidad/migración explícita para
  métodos existentes. No se cambió ningún endpoint de producción.

## Navegación de evidencia

- [adapter-composition-standard.md](../architecture/adapter-composition-standard.md)
- [infrastructure-structure.md](../architecture/infrastructure-structure.md)
- [reactive-realtime.md](../architecture/reactive-realtime.md)
- [http-command-query-guidelines.md](../architecture/http-command-query-guidelines.md)
- [keycloak-identity-provider.md](../security/keycloak-identity-provider.md)
- [runtime-security-provider-architecture.md](../security/runtime-security-provider-architecture.md)
- [sqlserver-connection.md](../integration/sqlserver-connection.md)
- [HTTP_AS_IS_MATRIX.md](../contracts/HTTP_AS_IS_MATRIX.md)
- [FINDINGS.md](../work-items/LB-000-gobernanza-documentacion/FINDINGS.md)
- [INVENTORY.md](../work-items/LB-000-gobernanza-documentacion/INVENTORY.md)

## TD-029

- **Descripción:** GlobalExceptionHandler depende de MessageCatalogPort en constructor/campo/llamada, infringiendo ControllersMustDependOnlyOnInputPortsTest.
- **Impacto:** mvn verify falla antes del gate JaCoCo; no se puede declarar build verde/DONE integral.
- **Evidencia:** corrida previa LB-000 (920 tests, 1 failure), [VALIDATION](../work-items/LB-000-gobernanza-documentacion/VALIDATION.md), CF-002.
- **Motivo:** defecto/contradicción preexistente ajeno a la reorganización; el usuario prohíbe cambios Java/tests/gates aquí.
- **Resolución esperada:** tarea arquitectónica separada con requisito/contrato, corrección autorizada y verify completo sin relajar la regla para ocultar la dependencia.
- **Bloquea línea base:** sí, DoD integral de LB-000 y cambios posteriores que requieran build verde.
- **Estado:** CERRADA (2026-09-20, [TECH-001](../work-items/TECH-001-restaurar-gate-arquitectura/CLOSURE.md)): `GlobalExceptionHandler` usa `ResolverMensajeUsuarioInputPort`; ArchUnit PASS sin cambios a la regla; `mvn verify` BUILD SUCCESS (932 tests). CF-002 RESUELTO; `TECHNICAL_BUILD_GATE = PASS`.

## TD-030

- **Fecha / responsable:** 2026-09-20 / backend-team + equipo DB.
- **Descripción:** `usp_validar_titularidad_jerarquica_interno` y `usp_validar_permiso_rbac_usuario_interno` usan `VAL_003` para fallos de autorización/titularidad, pero `VAL_003` describe nombres y apellidos obligatorios.
- **Impacto:** Medio. Código de error semánticamente incorrecto; puede confundir el mapeo de errores y el contrato HTTP.
- **Evidencia:** observación reportada durante la certificación externa DB del Golden Path (TECH-001); no verificada en este repositorio (el esquema DB no está versionado aquí).
- **Motivo:** fuera del alcance de TECH-001; no se modifica DB ni contratos.
- **Resolución esperada:** definir códigos propios de autorización/titularidad en el contrato de errores y alinear DB/backend antes del freeze OpenAPI. Relacionada con [TD-007](#td-007).
- **Bloquea línea base:** sí, LB-001C (contrato de errores).
- **Estado:** RESOLVED BY FROZEN DB BASELINE (LB-001B.4, 2026-09-23). DR-009 pasa a RESOLVED.
- **Actualización LB-001B (2026-09-20):** sigue ABIERTA y bloquea el freeze completo del contrato de errores en LB-001C; se registra como [DR-009](../work-items/LB-001B-backend-frontend-asistencia/CONTRACT_MATRIX.md#dr-009--códigos-de-error-de-autorizacióntitularidad-td-030). No se resuelve en LB-001B.
- **Actualización LB-001B.3 (2026-09-23, NO se cierra):** el Bloqueo 1 de ese work item (`CONTRACT_MATRIX.md#m-19`, mapeo determinista de errores DB `SEC_001/SEC_002/ATT_001-3/SES_003/SES_004/GEN_002`) es la manifestación exacta de este mismo defecto estructural a nivel de contrato: sin código de catálogo estable garantizado por el result set público (4 columnas, sin quinto campo `codigo`), no se puede construir el mapeo `SEC_*`/`VAL_003` que `DR-009`/`TD-030` exigen. Declarado `CONTRACT_CONFLICT` por decisión explícita del usuario de dejarlo en espera; ver [TD-036](#td-036) (nueva, alcance más preciso de este mismo problema) y [LB-001B.3 CLOSURE](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md). Sigue **ABIERTA**, sin evidencia de resolución.
- **Actualización LB-001B.4 (2026-09-23):** el contrato DB congelado (SHA-256 `45e48c5a...9aec`) formaliza el canal `DBCODE` y los SP del Golden Path emiten `SEC_001/SEC_002` (autorización/titularidad) en lugar de `VAL_003`. El backend mapea `SEC_001/SEC_002/EST_004 -> 403/FORBIDDEN` (`ForbiddenException`), verificado con `AsistenciaRepositorySqlServerIT` 6/6 sobre DB real (docente ajeno -> `FORBIDDEN`). Ver [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md), [VALIDATION](../work-items/LB-001B.4-final-backend-contract-closure/VALIDATION.md).

## TD-031

- **Fecha / responsable:** 2026-09-20 / equipo frontend (hallazgo de LB-001B).
- **Descripción:** En el frontend `withInterceptors([correlation, auth, error])`: ante un 401 devuelto por el backend, `errorInterceptor` (el más cercano al backend) invoca `notifySessionExpired()` —que limpia el token y el refresh token en memoria y redirige a `/login`— antes de que `authInterceptor.handle401` intente renovar el token, por lo que el refresco silencioso tras 401 (commit `7d21f28`) no puede tener éxito. La renovación preventiva (`getValidAccessToken(30)`) no se ve afectada.
- **Impacto:** Medio (sesión expirada prematuramente ante un 401 recuperable).
- **Evidencia:** `src/app/app.config.ts` l.21; `src/app/core/interceptors/error.interceptor.ts` l.14-24; `auth.interceptor.ts` l.47-54,66-107; `auth.service.ts` l.439-454 (`AsistenciasUCO-Frontend@71ee6d3`). **Inferido por lectura estática; no ejecutado ni cubierto por specs** (no existe spec de interceptores).
- **Motivo:** LB-001B es `CONTRACT_ANALYSIS` y el frontend es READ-ONLY; no es una decisión contractual con el provider.
- **Resolución esperada:** confirmar con una prueba/ejecución en el frontend y, si se confirma, corregir el orden o la responsabilidad de `notifySessionExpired`; condición de cierre: spec que demuestre reintento tras 401 con refresh válido.
- **Bloquea línea base:** no para el contrato; sí antes de certificar autenticación E2E (LB-003).
- **Estado:** CERRADA (2026-09-22, frontend `LB-001B.1C-final-contract-cleanup`). Evidencia: `npm run verify` PASS 184/184; no queda como bloqueador de LB-001C.

## TD-032

- **Fecha / responsable:** 2026-09-20 / equipo frontend (hallazgo de LB-001B).
- **Descripción:** `SECURITY_FINDING` (tipo: credencial por defecto embebida en código cliente). El flujo de matrícula de estudiantes de la pantalla de asistencia asigna una contraseña por defecto cuando el docente no la indica. **El valor no se reproduce en este ledger.**
- **Impacto:** Medio (cuentas provisionadas con una credencial conocida por cualquiera que lea el bundle). Fuera del Golden Path; relacionada con [TD-013](#td-013) y [TD-020](#td-020).
- **Evidencia:** `src/app/features/attendance/attendance-control/attendance-control.component.ts`, método `onRegisterStudentSubmit` (`AsistenciasUCO-Frontend@71ee6d3`).
- **Motivo:** LB-001B no modifica el frontend; el valor no se copia por regla de secretos de [AGENTS](../../AGENTS.md).
- **Resolución esperada:** retirar la credencial por defecto (exigir contraseña o política de provisioning acordada con backend/IdP); condición de cierre: sin contraseña literal en el código y prueba que lo verifique.
- **Bloquea línea base:** no para el Golden Path; sí release del frontend.
- **Estado:** CERRADA (2026-09-22, frontend `LB-001B.1C-final-contract-cleanup`). Evidencia: `PASSWORD DEFAULT COUNT = 0`; los formularios exigen contraseña explícita y los tests de validación permanecen verdes.

## TD-033

- **Fecha / responsable:** 2026-09-20 / equipo frontend (hallazgo de LB-001B).
- **Descripción:** En `AttendanceControlComponent` un error al cargar las sesiones del grupo se resuelve con `sessions.set([])` sin aviso, y los estados realtime `UNAUTHORIZED`/`ERROR`/`DISCONNECTED` no se muestran a nadie (solo el servicio de sincronización se suscribe a `connectionState$`). Un 403/500 se ve como «sin sesiones» y la pérdida de actualizaciones en vivo es invisible.
- **Impacto:** Medio (oculta fallos de contrato/autorización al usuario y al diagnóstico).
- **Evidencia:** `attendance-control.component.ts` l.460-462; `attendance-realtime-sync.service.ts`; `fetch-sse-realtime-transport.ts` l.197-202 (`AsistenciasUCO-Frontend@71ee6d3`); ver [CONTRACT_MATRIX](../work-items/LB-001B-backend-frontend-asistencia/CONTRACT_MATRIX.md) F-08, F-14, C-009g.
- **Motivo:** LB-001B es análisis; el frontend es READ-ONLY.
- **Resolución esperada:** mostrar el error de carga y el estado de conexión; condición de cierre: specs que verifiquen aviso ante error de sesiones y ante estado `UNAUTHORIZED`/`ERROR`.
- **Bloquea línea base:** no para el contrato; sí certificación de UX del Golden Path (LB-003).
- **Estado:** CERRADA (2026-09-22, frontend `LB-001B.1C-final-contract-cleanup`). Evidencia: `npm run verify` PASS; la UX del Golden Path reporta fallos de carga/sincronización según los tests frontend vigentes.

## TD-034

- **Fecha / responsable:** 2026-09-22 / backend-team (hallazgo de LB-001B.1).
- **Descripción:** `SesionErrorCode.ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA` permanece declarada en `SesionErrorCode.java` pero, tras el retiro de `descripcion` del contrato de creación/actualización de `Sesion` (LB-001B.1: DR-001/DR-004 Opción B), ya no existe ningún `validarDescripcion(...)` en `CrearSesionDomain`/`ActualizarSesionDomain` que la lance. Confirmado por 03-tester-red (decisión de alcance: mantener la constante) y re-verificado por 05-auditor (`grep -rn "ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA" src/` solo encuentra la declaración).
- **Impacto:** Bajo. Código muerto, sin efecto funcional ni de seguridad; ruido de mantenimiento.
- **Evidencia:** `SesionErrorCode.java` línea 13; [LB-001B.1 AUDIT](../work-items/LB-001B.1-db-source-of-truth-cleanup/AUDIT.md) sección 9; [LB-001B.1 VALIDATION](../work-items/LB-001B.1-db-source-of-truth-cleanup/VALIDATION.md).
- **Motivo:** fuera de las rutas permitidas/alcance de LB-001B.1 (que retira campos del contrato, no códigos de error); retirar la constante requiere confirmar que ningún consumidor (catálogo de mensajes, frontend, tests) referencia su código machine-readable antes de eliminarla.
- **Resolución esperada:** tarea futura de limpieza menor que confirme ausencia de referencias externas y retire la constante junto con su entrada de catálogo de mensajes si aplica.
- **Bloquea línea base:** no.
- **Estado:** ABIERTA. **Work item relacionado:** [LB-001B.1](../work-items/LB-001B.1-db-source-of-truth-cleanup/CLOSURE.md).
- **Re-confirmación LB-001B.3 (2026-09-23):** `05-auditor` de ese work item re-encontró el mismo hallazgo de forma independiente (`grep -rn "ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA" src/` — solo la declaración, cero lanzadores) durante el barrido de auditoría §32. Mismo código muerto, sin cambio de estado; no se abre un ID nuevo para evitar duplicar la misma deuda. Ver [LB-001B.3 CLOSURE](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md).

## TD-035

- **Fecha / responsable:** 2026-09-22 / equipo frontend (hallazgo de LB-001B.1, confirmado primero en LB-001B.1A/1B del repo frontend).
- **Descripción:** `attendance.mapper.ts` (repo frontend) contiene **dos contratos paralelos**: (a) `fromGroupStudentsAndAttendances()`, el camino real del Golden Path, ya corregido (ausencia de asistencia ⇒ `null`, no `'AN'`, resuelto por DR-002 en LB-001B.1A); y (b) `studentFromDTO()`/`StudentAttendanceDTO`/`ClassSessionDTO` (contrato snake_case alterno, con `aula`, `tipo_sesion`, `estado_sesion`, `tema`, y el residuo `(dto.estado_asistencia as AttendanceStatus) || 'AN'` — el mismo patrón que DR-002 prohíbe en el camino real). Los consumidores de `studentFromDTO()`/`ClassSessionDTO` no fueron identificados en esta sesión backend.
- **Impacto:** Medio si algún flujo activo usa `studentFromDTO()`: reintroduciría por esa vía la síntesis ausencia→`AN` que el Golden Path ya corrigió. Bajo si el contrato paralelo está muerto/sin consumidor.
- **Evidencia:** `AsistenciasUCO-Frontend/src/app/core/mappers/attendance.mapper.ts` (línea ~49, `studentFromDTO()`), leído completo en la sesión de [LB-001B.1 PLAN](../work-items/LB-001B.1-db-source-of-truth-cleanup/PLAN.md) AS-IS #13; confirmado sin corrección por diseño en `LB-001B.1A CLOSURE.md` y en el `PLAN.md` de `LB-001B.1B-db-source-of-truth-cleanup` (repo frontend), ambos excluyéndolo explícitamente de su alcance.
- **Motivo:** decisión humana 2026-09-22 (sección de la tarea autorizada de LB-001B.1): no se corrige dentro de LB-001B.1 (backend) ni de LB-001B.1A/1B (frontend); se documenta como deuda para una tarea futura dedicada, que primero debe identificar consumidores reales de `studentFromDTO()`/`ClassSessionDTO`.
- **Resolución esperada:** identificar consumidores del contrato paralelo; si tiene uso activo, aplicar la misma corrección que DR-002 (ausencia ⇒ `null`, no `'AN'`) o retirar el contrato paralelo si está muerto; condición de cierre: sin residuo `|| 'AN'` alcanzable desde código en uso, con test que lo verifique.
- **Bloquea línea base:** no para el Golden Path; sí antes de certificar UX/datos del Golden Path (LB-003), si se confirma consumidor activo.
- **Estado:** CERRADA (2026-09-22, frontend `LB-001B.1C-final-contract-cleanup`). Evidencia: `ClassSessionDTO`, `studentFromDTO`, `studentToDTO`, `sessionFromDTO` y `sessionToDTO` eliminados; residuo `|| 'AN'` = 0; `npm run verify` PASS 184/184.

## TD-036

- **Fecha / responsable:** 2026-09-23 / backend-team, con decisión pendiente del equipo DB o del usuario sobre cómo exponer un canal determinista de errores.
- **Descripción:** El mapeo `DB semantic → backend ErrorDefinition → HTTP status → ApiErrorResponse.code` exigido para `SEC_001`, `SEC_002`, `ATT_001`, `ATT_002`, `ATT_003`, `SES_003`, `SES_004`, `GEN_002` no existe de forma determinista. `DbFailureClassifier`/`DbExceptionTranslator` son 100 % *pattern matching* de frases en español; ningún literal de código de catálogo (`SEC_*`/`ATT_*`/`SES_*`/`GEN_*`) aparece como señal de clasificación. El result set público del contrato DB congelado tiene exactamente 4 columnas (`idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado`), sin un quinto campo `codigo` garantizado. 8 de 11 códigos relevantes del Golden Path caen hoy en `ERR_DB_UNCLASSIFIED` → HTTP 500 en vez de 403/400/409, violando el requisito de no devolver 500 para RBAC/titularidad. `SES_001`, `RC_001`, `EST_004` funcionan hoy solo por coincidencia accidental de texto libre con patrones de otros dominios, sin garantía contractual.
- **Impacto:** Crítico. Toca prácticamente todo el Golden Path protegido (crear/actualizar sesión, batch de asistencia, RBAC/titularidad).
- **Evidencia:** [DbFailureClassifier](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/support/error/DbFailureClassifier.java); [DbExceptionTranslator](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/support/error/DbExceptionTranslator.java); [LB-001B.3 CONTRACT_MATRIX](../work-items/LB-001B.3-backend-db-alignment/CONTRACT_MATRIX.md#matriz) fila M-19 y tabla "Códigos/enums"; [LB-001B.3 CONTRACT_FREEZE](../work-items/LB-001B.3-backend-db-alignment/CONTRACT_FREEZE.md) §6.
- **Motivo:** `CONTRACT_CONFLICT` declarado explícitamente conforme a `AGENTS.md` §"Cuándo detenerse" — no se clasifica por heurística de texto frágil sin test contractual; decisión explícita del usuario (2026-09-23) de dejarlo en espera y continuar solo con el alcance ya desbloqueado de `LB-001B.3`.
- **Resolución esperada:** decisión humana sobre si se solicita al repo DB un canal estructurado adicional (p. ej. un quinto campo `codigo` en el result set público) o si se acepta el riesgo de texto libre con tests de regresión explícitos marcados como no-contractuales; luego construir la tabla de mapeo exigida y congelarla con tests.
- **Bloquea línea base:** sí, LB-001C (contrato de errores) — mismo bloqueo que [TD-030](#td-030)/DR-009, alcance más preciso.
- **Estado:** CLOSED (LB-001B.4, 2026-09-23). Sustituye el `CONTRACT_CONFLICT`.
- **Actualización LB-001B.4 (2026-09-23):** CERRADA. Parser estricto `DbTechnicalError` (`^DBCODE=([A-Z0-9_]+)\|(.*)$`) + `DbFailureClassifier` por código (`SEC_001/SEC_002/EST_004 -> FORBIDDEN`; `ATT_001-003/GEN_002/RC_001/SES_004 -> VALIDATION_ERROR`; `SES_001 -> RESOURCE_NOT_FOUND`; `SES_003 -> FEATURE_UNAVAILABLE`; desconocido/malformado -> `ERR_DB_UNCLASSIFIED`, fail-closed). Texto legacy solo si no hay marcador DBCODE. 959/959 tests; sin clasificación por texto en el Golden Path. `USU_001` sin mapeo formal: [TD-047](#td-047).

## TD-037

- **Fecha / responsable:** 2026-09-23 / backend-team (hallazgo de LB-001B.3, `04-implementador` + confirmado por `05-auditor`).
- **Descripción:** `SesionMateriaEstudianteSqlServerAdapter` sigue leyendo `Sesion.fechaHoraInicio`/`fechaHoraFin` con `JdbcValueMapper.toLocalDateTime(Object)` (dependiente de `TimeZone.getDefault()` en el momento de la lectura), no con el método dedicado `toLocalDateTimeUtc(Object)` introducido en la misma fase para los otros dos llamadores confirmados de este campo. El cambio de llamada se intentó y se revirtió en la misma sesión porque rompía `SesionMateriaEstudianteSqlServerAdapterTest.consultarSesionesMateria_mapea_proyeccion_completa` (fixture construye el `Timestamp` bajo zona ambiental no controlada, mismo defecto ya dictaminado para `SesionRepositorySqlServerAdapterTest`), un `TEST_CONTRACT_CONFLICT` que ni `CONTRACT_FREEZE.md` §5.4 ni el dictamen previo de `05-auditor` cubrieron.
- **Impacto:** Medio. Defecto de producción real y activo (no solo brecha de cobertura): en una máquina cuya zona horaria por defecto no sea UTC, la fecha/hora de sesión leída por este adaptador queda desplazada. Fuera del Golden Path de `TASK_AUTORIZADA.md` §7 (el endpoint que lo consume es `GET /api/v1/estudiante/materias/{materiaId}/sesiones`), pero dentro del alcance textual de la prohibición general de `ZoneId.systemDefault()`/equivalentes para `Sesion`.
- **Evidencia:** `SesionMateriaEstudianteSqlServerAdapter.java` líneas 43-44; [LB-001B.3 VALIDATION](../work-items/LB-001B.3-backend-db-alignment/VALIDATION.md) §7.2; [LB-001B.3 AUDIT](../work-items/LB-001B.3-backend-db-alignment/AUDIT.md) §2.5, §3.
- **Motivo:** `04-implementador` no modifica tests RED para forzar GREEN; revirtió el cambio de producción dependiente en vez de tocar `SesionMateriaEstudianteSqlServerAdapterTest.java`, conforme a su rol. Requiere que `02-contratos` autorice la misma corrección de fixture ya aplicada a `SesionRepositorySqlServerAdapterTest` (`TimeZone.setDefault(UTC)` antes de construir el `Timestamp`, restaurar en `finally`) y que `03-tester-red` la ejecute.
- **Resolución esperada:** corrección de fixture autorizada + re-aplicación del cambio de llamada a `toLocalDateTimeUtc` en `SesionMateriaEstudianteSqlServerAdapter.java`; condición de cierre: test GREEN con el mismo patrón de estabilidad UTC ya usado en `JdbcValueMapperTest`/`SesionRepositorySqlServerAdapterTest`.
- **Bloquea línea base:** no para el Golden Path; sí antes de certificar UTC end-to-end para todos los lectores de `Sesion.fechaHoraInicio`/`fechaHoraFin` (relacionado con [TD-005](#td-005)).
- **Estado:** CLOSED (LB-001B.4, 2026-09-23).
- **Actualización LB-001B.4 (2026-09-23):** CERRADA. `SesionMateriaEstudianteSqlServerAdapter` usa `toLocalDateTimeUtc`; el fixture se construye con `Timestamp.from(Instant)` (dictamen `TEST_CONTRACT_CONFLICT` en [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md)). 959/959 GREEN.

## TD-038

- **Fecha / responsable:** 2026-09-23 / backend-team (hallazgo de `05-auditor` en LB-001B.3).
- **Descripción:** `ReporteAsistenciaSqlServerAdapterTest.consultarReporteAsistenciaGrupo_mapea_fila_completa` construye un `Timestamp` bajo zona ambiental no controlada para `fechaHoraInicio`/`fechaHoraFin`, pero **no asevera el valor de esos campos** (solo asevera `codigoGrupo`, `numeroSesion`, `asistio`, `razonCausa`). El código de producción (`ReporteAsistenciaSqlServerAdapter`) ya es correcto — usa `toLocalDateTimeUtc` desde esta misma fase — pero el test no lo protege: si alguien revirtiera esa llamada a `toLocalDateTime`, esta suite no lo detectaría.
- **Impacto:** Bajo. No es un defecto de producción activo, es una brecha de cobertura de regresión.
- **Evidencia:** `ReporteAsistenciaSqlServerAdapterTest.java` líneas 43-44 (construcción) sin aserción de los campos temporales; [LB-001B.3 VALIDATION](../work-items/LB-001B.3-backend-db-alignment/VALIDATION.md) §7.2; [LB-001B.3 AUDIT](../work-items/LB-001B.3-backend-db-alignment/AUDIT.md) §3.
- **Motivo:** fuera del alcance de `CONTRACT_FREEZE.md` §5.4 de `LB-001B.3` (que solo autorizó corregir el fixture de `SesionRepositorySqlServerAdapterTest`); no reportado antes de la auditoría de esa fase.
- **Resolución esperada:** agregar aserción explícita de `fechaHoraInicio`/`fechaHoraFin` (con fixture bajo zona controlada, mismo patrón que los demás tests de estabilidad UTC) a `ReporteAsistenciaSqlServerAdapterTest`.
- **Bloquea línea base:** no.
- **Estado:** CLOSED (LB-001B.4, 2026-09-23).
- **Actualización LB-001B.4 (2026-09-23):** CERRADA. `ReporteAsistenciaSqlServerAdapterTest` incluido en el RED con fixture UTC estable; 959/959 GREEN.

## TD-039

- **Fecha / responsable:** 2026-09-23 / backend-team + equipo DB.
- **Descripción:** El contrato DB congelado (`DB_BASELINE_CONTRACT.md`) documenta el comportamiento de `usp_cerrar_sesion` (legacy no soportado: retorna `SES_003`, `estadoResultado = 0`, sin escrituras) pero **no documenta su firma exacta de parámetros**. El backend (`SesionRepositorySqlServerAdapter.SQL_CERRAR_SESION`) sigue enviando `@idSesion, @idDocente, @idCorrelacion, @idUsuarioEjecutor`; no se puede confirmar si `@idDocente` ahí es un ghost param real o si el SP legacy todavía lo requiere sin abrir el repo DB. El endpoint HTTP `POST /api/v1/sesiones/cierres` sigue vivo (no es código muerto) pero fuera del Golden Path de `TASK_AUTORIZADA.md` §7.
- **Impacto:** Medio. El comportamiento observable es correcto (el SP frozen garantiza que la llamada siempre falla con `SES_003`, nunca simula éxito), pero la firma real que el backend envía no está congelada ni verificada contra el contrato.
- **Evidencia:** `SesionRepositorySqlServerAdapter.java` líneas 53-59, 226-241; `SesionController.java` líneas 130-138; [LB-001B.3 CONTRACT_MATRIX](../work-items/LB-001B.3-backend-db-alignment/CONTRACT_MATRIX.md#matriz) fila M-03; [LB-001B.3 CONTRACT_FREEZE](../work-items/LB-001B.3-backend-db-alignment/CONTRACT_FREEZE.md) §3.
- **Motivo:** `BLOCKED_BY_MISSING_EVIDENCE` — no se puede saber sin abrir el repo DB, fuera de alcance de `LB-001B.3`. Clasificado `OUT_OF_TARGET`/`LEGACY_NOT_SUPPORTED` para el contrato target de `LB-001C`, sin tocar código ni eliminar el endpoint sin evidencia de ausencia de consumidores.
- **Resolución esperada:** equipo DB aporta la firma exacta documentada de `usp_cerrar_sesion`; luego decidir si `@idDocente` se retira del envío o si el SP legacy lo requiere tal cual.
- **Bloquea línea base:** no para el Golden Path (endpoint fuera de alcance); sí antes de incluir `POST /sesiones/cierres` en cualquier contrato congelado futuro.
- **Estado:** BLOCKED_BY_MISSING_EVIDENCE. **Work item relacionado:** [LB-001B.3](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md).

## TD-040

- **Fecha / responsable:** 2026-09-23 / backend-team + equipo DB.
- **Descripción:** `GrupoRepositorySqlServerAdapter.SQL_CONSULTAR_ESTUDIANTES_GRUPO` (usado por `GET /api/v1/grupos/{grupoId}/estudiantes`, endpoint del Golden Path) hace `INNER JOIN` contra `uv_estudiante_identidad` y `uv_usuario` para derivar `documento`, `correo` y `nombreCompleto`, en vez de usar `eg.nombreCompletoEstudiante` ya provisto por `uv_estudiante_grupo` (vista sí documentada). Ninguna de las dos vistas usadas por el `JOIN` (`uv_estudiante_identidad`, `uv_usuario`) está documentada en `DB_BASELINE_CONTRACT.md`, ni en el inventario de objetos ni en las proyecciones de lectura del Golden Path.
- **Impacto:** Alto. El endpoint SÍ está en el Golden Path de `TASK_AUTORIZADA.md` §7; no se puede confirmar `MATCH` ni `MISMATCH` de esta porción del contrato sin evidencia DB adicional.
- **Evidencia:** `GrupoRepositorySqlServerAdapter.java` líneas 127-143; [LB-001B.3 CONTRACT_MATRIX](../work-items/LB-001B.3-backend-db-alignment/CONTRACT_MATRIX.md#matriz) fila M-08.
- **Motivo:** `BLOCKED_BY_MISSING_EVIDENCE` — no se modifica sin evidencia; el comportamiento AS-IS permanece intacto (no se prohíbe seguir operando, solo se prohíbe modificar sin evidencia).
- **Resolución esperada:** equipo DB documenta `uv_estudiante_identidad`/`uv_usuario` en `DB_BASELINE_CONTRACT.md` (o su equivalente congelado); luego confirmar si el `JOIN` actual es correcto o si debe reemplazarse por `eg.nombreCompletoEstudiante`.
- **Bloquea línea base:** sí — está en el Golden Path; bloquea el freeze íntegro del contrato de lectura de estudiantes por grupo antes de `LB-001C`.
- **Estado:** CLOSED — EVIDENCE_RESOLVED (LB-001B.4, 2026-09-23), con salvedad TD-046.
- **Actualización LB-001B.4 (2026-09-23):** columnas de `uv_estudiante_grupo`, `uv_estudiante_identidad` y `uv_usuario` confirmadas contra la DB real (`GoldenPathSqlStoredProcedureContractIT` 16/16); el `SELECT` del adapter se ejecutó en solo lectura y conserva el conteo de `uv_estudiante_grupo` (2 = 2). **Salvedad del auditor:** `DB_BASELINE_CONTRACT.md` sigue sin documentar esas vistas; acción no bloqueante para el equipo DB en [TD-046](#td-046).

## TD-041

- **Fecha / responsable:** 2026-09-23 / backend-team + equipo DB.
- **Descripción:** La rama de origen del snapshot DB congelado (`feat/db-golden-path-baseline-freeze`, commit `99190f0...`, repo `gestion-asistencia-db`) no está fusionada a `main`/`develop` de ese repositorio, conforme a `docs/contracts/external/db/PROVENANCE.md`. Si esa rama recibe nuevos commits antes de que `LB-001C` congele el contrato definitivo, el snapshot ya importado (`DB_BASELINE_CONTRACT.md`, SHA-256 `1fd728e43d2bdbdc6b395bc5aad9021105117281c7c18b64afc39a6d45937103`) queda desactualizado.
- **Impacto:** Medio. Riesgo de trabajar sobre un contrato obsoleto sin darse cuenta, no un defecto actual.
- **Evidencia:** `docs/contracts/external/db/PROVENANCE.md`; [LB-001B.3 PLAN](../work-items/LB-001B.3-backend-db-alignment/PLAN.md) riesgo 1.
- **Motivo:** fuera del control del backend; requiere seguimiento del repo DB (equipo/owner externo a este checkout).
- **Resolución esperada:** confirmar con el equipo DB que la rama se fusionó (o re-capturar un nuevo snapshot con SHA-256 actualizado) antes de congelar `LB-001C`.
- **Bloquea línea base:** sí, si el snapshot se desactualiza antes del freeze de `LB-001C` — cualquier fase que dependa de un detalle no cubierto por el snapshot actual debe registrar `BLOCKED_BY_MISSING_EVIDENCE`, no asumir texto del repo DB no importado.
- **Estado:** ABIERTA (seguimiento externo). **Work item relacionado:** [LB-001B.3](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md).
- **Actualización LB-001B.4 (2026-09-23):** el snapshot vigente es `45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec` (repo DB, HEAD `99190f0...`); su origen sigue siendo `UNCOMMITTED_WORKTREE`. Sigue ABIERTA (seguimiento externo).

## TD-042

- **Fecha / responsable:** 2026-09-23 / backend-team (hallazgo de `03-tester-red`/`02-contratos` en LB-001B.3).
- **Descripción:** No existe ningún test que asegure que `RealtimeEvent.occurredAt` (tipo `Instant`, ya correcto) se serializa en HTTP/SSE como ISO-8601 UTC con sufijo `Z`. No hay ningún `JacksonOutputConfig`/customizer explícito de salida en `src/main/java`; el comportamiento real depende enteramente del autoconfigure default de Spring Boot. Punto **O** de `TASK_AUTORIZADA.md` §27 de `LB-001B.3`, evaluado pero no alcanzado en esa fase por riesgo de RED fabricado por el arnés de prueba (un `JsonMapper` "pelado" sin `ApplicationContext` real invierte el default `WRITE_DATES_AS_TIMESTAMPS`).
- **Impacto:** Bajo-medio. El tipo es correcto (`Instant`, MATCH), pero el contrato exige el test, no solo el tipo — sin evidencia de test, no hay garantía de regresión.
- **Evidencia:** `RealtimeEvent.java`; `RealtimeEventResponse.java`; [LB-001B.3 CONTRACT_MATRIX](../work-items/LB-001B.3-backend-db-alignment/CONTRACT_MATRIX.md#matriz) fila M-22; [LB-001B.3 TEST_PLAN](../work-items/LB-001B.3-backend-db-alignment/TEST_PLAN.md) sección "Punto O".
- **Motivo:** `MISSING_IN_BACKEND`, no `CONTRACT_CONFLICT` — el punto es opcional en el alcance de esa sesión y se priorizaron los 10 puntos obligatorios (A-H, N, P).
- **Resolución esperada:** crear el test de serialización solo con evidencia de un `ApplicationContext`/`@SpringBootTest` real, o confirmar/crear un `JacksonOutputConfig` de producción equivalente a `JacksonInputConfig` antes de escribir el test — no con un `JsonMapper` construido a mano.
- **Bloquea línea base:** no para el Golden Path funcional; sí antes de certificar el contrato realtime completo (`BACKEND_GOLDEN_PATH_CONTRACT.md`, punto M/N de `TASK_AUTORIZADA.md` §33).
- **Estado:** CLOSED (LB-001B.4A, 2026-09-24). Estado histórico: MISSING_IN_BACKEND. **Work item relacionado:** [LB-001B.3](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md).
- **Actualización LB-001B.4 (2026-09-23) — histórico:** se mantuvo abierta porque `RealtimeEventResponseTest` usa un `JsonMapper` construido a mano, insuficiente según esta deuda.
- **Actualización LB-001B.4A (2026-09-24) — CLOSED:** `RealtimeEventResponseSpringJsonTest` usa `@SpringBootTest` (`JacksonAutoConfiguration` + `JacksonInputConfig`) con el `JsonMapper` inyectado por Spring (y un segundo test con `JacksonJsonHttpMessageConverter`), y afirma `occurredAt` ISO-8601 con `Z`. No hay `spring.jackson.*` en recursos, por lo que el auto-config es el de producción. Cumple la "Resolución esperada". Salvedad: contexto Jackson mínimo, no la aplicación completa; suficiente para el criterio. Ver [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md).

## TD-043

- **Fecha / responsable:** 2026-09-23 / backend-team + equipo DB. **Clasificación:** `NON_GOLDEN_DB_CONTRACT_DRIFT`.
- **Descripción:** tres SP invocados por adapters del backend no existen en la DB oficial (`gestionasistenciadb`, esquema `dbo`, verificado por SELECT de solo lectura sobre `sys.objects`/`sys.parameters`; ver [CONTRACT_DECISION_TD043](../work-items/LB-001B.4-final-backend-contract-closure/CONTRACT_DECISION_TD043.md)). Ningún SP existente tiene firma y semántica inequívocamente equivalentes, por lo que no se mapea por parecido de nombre.

| Adapter consumidor | SP esperado por el backend | SP en DB oficial | Vertical afectada |
|---|---|---|---|
| `UsuarioRepositorySqlServerAdapter` (`SQL_SINCRONIZAR_USUARIO`) | `dbo.usp_sincronizar_usuario` | inexistente | sincronización de usuario (identidad/perfil) |
| `PlanEstudioSqlServerAdapter` (`registrarOActualizarPlanEstudio`) | `dbo.usp_registrar_o_actualizar_plan_estudio` | inexistente | registro/actualización de plan de estudio (académico) |
| `GrupoRepositorySqlServerAdapter` (`SQL_REGISTRAR_ESTUDIANTE`) | `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` | inexistente | registro de estudiante nuevo en grupo (usuario no existente) |

- **Impacto:** las tres operaciones fallarán en runtime contra la DB oficial. Fuera del Golden Path LB-001; el perfil de integración completo queda `NOT_GREEN` solo por esto.
- **Evidencia:** [CONTRACT_DECISION_TD043](../work-items/LB-001B.4-final-backend-contract-closure/CONTRACT_DECISION_TD043.md).
- **Motivo:** `NON_GOLDEN_DB_CONTRACT_DRIFT`; sin evidencia suficiente para mapear a SP existentes (`usp_sincronizar_usuario_interno`, `usp_registrar_estudiante_en_grupo`, etc. difieren en firma/semántica). No se modifica código ni DB.
- **Resolución esperada:** work item contractual propio (DB owner vs backend consumer) antes de liberar cada feature: crear el SP en DB o adaptar el backend, con decisión explícita de OWNER.
- **Bloquea línea base:** no para el Golden Path; sí para liberar las tres features anteriores.
- **Actualización LB-002.1 (2026-09-26) — nueva evidencia, deuda NO cerrada:** `.\mvnw.cmd -Pintegration verify` contra `sql_server_asistencias`/`gestionasistenciadb` (freeze DB desplegado) da 6 fallos: `SqlStoredProcedureContractIT` ×3 (los tres SP de esta deuda), `GrupoRepositorySqlServerIT` ×2 y `UsuarioPasswordHashSqlServerIT` ×1 (`Could not find stored procedure`, error 2812). Son **idénticos sobre `HEAD` limpio (`df66a67`) sin cambios JPA**: preexistentes al piloto JPA, sin regresión causada por LB-002.1. El perfil `-Pintegration` global continúa `NOT_GREEN_TD043`; no se convierte en PASS. Los ITs no se modificaron (sin `@Disabled`, `Assumptions`, exclusiones, mocks ni cambio de expected); no se tocó DB. Ver [LB-002.1-VALIDATION](../work-items/LB-002-jpa-incremental/LB-002.1-VALIDATION.md).
- **Actualización LB-002.1C (2026-09-27) — consolidación NON-GOLDEN, deuda DIFERIDA (solo documental; no resuelta):** por decisión humana de priorización, la remediación de TD-043 se **difiere** y el trabajo independiente del Golden Path continúa. Sin cambios de Java, tests, OpenAPI, DB ni frontend; sin nuevos RED ni investigación adicional. Los tres componentes conocidos (no se asume equivalencia por parecido de nombre; ver también la tabla de arriba):

```text
TD-043:                     NON_GOLDEN_DB_CONTRACT_DRIFT
STATUS:                     OPEN / DEFERRED
GLOBAL EFFECT:              mvn -Pintegration verify permanece NOT_GREEN_TD043
GOLDEN PATH EFFECT:         NONE
```

  - **A. STUDENT ENROLLMENT** — backend espera `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` (`GrupoRepositorySqlServerAdapter`, `SQL_REGISTRAR_ESTUDIANTE`). DB: SP legacy eliminado; el contrato público vigente (`dbo.usp_registrar_estudiante_en_grupo`) exige perfil `ESTUDIANTE` en `@idUsuarioEjecutor`. Evidencia: análisis y adaptaciones de `LB-002.1C-B1` disponibles en branch/work item; decisión de diseño público de matrícula vs auto-matrícula pendiente de consenso. Estado: **DEFERRED — B1/B2 EVIDENCE AVAILABLE**.
  - **B. USER SYNCHRONIZATION** — backend espera `dbo.usp_sincronizar_usuario` (`UsuarioRepositorySqlServerAdapter`, `SQL_SINCRONIZAR_USUARIO`). DB: objeto público ausente en el contrato congelado/vigente. Remediación futura: análisis dedicado de consumidor/contrato **antes** de implementar. No asumir que el SP interno (`usp_sincronizar_usuario_interno`) es un reemplazo público válido. Estado: **DEFERRED — ANALYSIS_REQUIRED**.
  - **C. STUDY PLAN COMMAND** — backend espera `dbo.usp_registrar_o_actualizar_plan_estudio` (`PlanEstudioSqlServerAdapter.registrarOActualizarPlanEstudio`). DB: objeto público ausente en el contrato congelado/vigente; el código consumidor sigue presente. Remediación futura: análisis dedicado de consumidor/contrato antes de implementar **o retirar**. Estado: **DEFERRED — ANALYSIS_REQUIRED**.

  **Sin falsas afirmaciones:** TD-043 permanece OPEN; `-Pintegration verify` global permanece `NOT_GREEN_TD043` (6 fallos idénticos, no re-ejecutados en esta actualización); los ITs no se deshabilitan y las expectativas contractuales no se borran ni se ocultan. Esto **no** significa LB-002 GLOBAL CLOSED, `-Pintegration` GREEN ni TD-043 RESOLVED.

  **Excepción de continuidad del Golden Path (decisión de planificación):** TD-043 es NON-GOLDEN y el Golden Path funcional de asistencia es independiente de estas tres capabilities. `NON-GOLDEN TD-043 REMEDIATION: DEFERRED` · `GOLDEN PATH ENGINEERING: AUTHORIZED TO CONTINUE` (por priorización humana). Siguiente objetivo autorizado: Golden Path `registrarAsistenciasSesion`, fase técnica **LB-002.2 — JPA COMMAND PILOT** (TD-043 permanece como excepción/deuda NON-GOLDEN reconocida). Ver [LINEA_BASE](LINEA_BASE.md).
- **Estado:** **OPEN / DEFERRED** — NON_GOLDEN_DB_CONTRACT_DRIFT (no resuelta; requiere work item propio si se decide resolver). **Work item relacionado:** [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CONTRACT_DECISION_TD043.md).

## TD-044

- **Fecha / responsable:** 2026-09-23 / backend-team (condición del auditor de LB-001B.4).
- **Descripción:** `DocenteRepositorySqlServerIT` omite 2 tests (`assumeTrue` por ausencia de datos). Prueban `consultarAsignacionesAcademicas` (`GET /api/v1/docentes/{docenteId}/asignaciones`), no `GET /api/v1/docente/horarios` (que usa `uv_horario_docente`, verificada por `GoldenPathSqlStoredProcedureContractIT`).
- **Impacto:** Bajo. Un skip no es PASS: esa consulta no está certificada contra DB real.
- **Evidencia:** [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md) (§Los 2 skips); [VALIDATION](../work-items/LB-001B.4-final-backend-contract-closure/VALIDATION.md).
- **Resolución esperada:** fixture autocontenido con prefijo `IT-LB001B4-`, sin `assumeTrue`, y limpieza verificable.
- **Bloquea línea base:** no (fuera del Golden Path); sí antes de certificar esa consulta.
- **Estado:** ABIERTA. **Work item relacionado:** [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).

## TD-045

- **Fecha / responsable:** 2026-09-23 / backend-team + equipo DB.
- **Descripción:** `dbo.RazonCausa` conserva códigos históricos (`CPI`, `CPVP`, entre otros). La escritura solo admite `AN/SJC/EX`; la lectura (`AsistenciaConsultadaEntity`) **falla cerrado** ante cualquier estado fuera de ese dominio (DR-006, Opción A). Una fila histórica en `uv_detalle_asistencia` haría fallar todo el listado `GET /api/v1/grupos/{grupoId}/asistencias`.
- **Impacto:** Medio latente. Hoy `uv_detalle_asistencia` tiene 0 filas; el riesgo aparece con datos históricos.
- **Evidencia:** [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md) (AN/SJC/EX); test RED de la entidad.
- **Resolución esperada:** decisión de contrato (migrar/mapear históricos en DB o definir tratamiento por fila) antes de cargar datos históricos; no relajar el fail-closed por suposición.
- **Bloquea línea base:** no; sí antes de cargar datos históricos de asistencia.
- **Estado:** ABIERTA. **Work item relacionado:** [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).

## TD-046

- **Fecha / responsable:** 2026-09-23 / equipo DB (con seguimiento backend-team).
- **Descripción:** salvedad del cierre de [TD-040](#td-040): las vistas `uv_estudiante_grupo`, `uv_estudiante_identidad` y `uv_usuario`, usadas por `GET /api/v1/grupos/{grupoId}/estudiantes`, existen y coinciden en columnas en la DB real, pero no están documentadas en `DB_BASELINE_CONTRACT.md` del repositorio DB.
- **Impacto:** Bajo. Contrato verificable solo por integración, no por documento.
- **Evidencia:** [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md) (TD-040).
- **Resolución esperada:** el equipo DB las documenta en su contrato y se re-importa el snapshot (nuevo SHA-256).
- **Bloquea línea base:** no.
- **Estado:** ABIERTA (acción externa, equipo DB). **Work item relacionado:** [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).

## TD-047

- **Fecha / responsable:** 2026-09-23 / backend-team + equipo DB.
- **Descripción:** el código `USU_001` no tiene mapeo formal DBCODE en `DbFailureClassifier`; cae en `ERR_DB_UNCLASSIFIED` (500, fail-closed). No forma parte del Golden Path.
- **Impacto:** Bajo. Comportamiento seguro pero no semántico para esa vertical.
- **Evidencia:** [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md) (DBCODE formal); `DbFailureClassifier`.
- **Resolución esperada:** definir HTTP/código semántico de `USU_001` en el contrato de la vertical de usuarios.
- **Bloquea línea base:** no (fuera del Golden Path).
- **Estado:** ABIERTA. **Work item relacionado:** [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).

## TD-048

- **Fecha / responsable:** 2026-09-24 / backend-team + equipo DB (hallazgo F-4A-1 del auditor de LB-001B.4A).
- **Descripción:** `Sesion.nombre` es `nvarchar(50) NOT NULL` según la evidencia congelada ([LB-001B.1 PLAN](../work-items/LB-001B.1-db-source-of-truth-cleanup/PLAN.md), tabla "DB SESSION CONTRACT"), pero el backend valida 1..150 en Crear, Actualizar y la entidad de lectura (antes Crear 5..100). Un `nombre` de 51..150 caracteres pasa la validación y fallaría en el `INSERT`/`UPDATE` como error técnico de persistencia en lugar de 400 `VALIDATION_ERROR` sobre `nombre`.
- **Impacto:** Bajo-medio. Defecto demostrado por documentación, no por IT (no se ejecutó contra DB). `DB_BASELINE_CONTRACT.md` no documenta longitudes.
- **Evidencia:** [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md) (LB-001B.4A, F-4A-1).
- **Resolución esperada:** 02-contratos decide el máximo (propuesta 50, a confirmar con el equipo DB); luego RED (151 → 51), ajuste de validadores/entidad/mensaje y documentación en `BACKEND_GOLDEN_PATH_CONTRACT.md`. No restaurar 5..100 (sin respaldo en DB).
- **Bloquea línea base:** no.
- **Estado:** **CLOSED (2026-09-24, LB-001B.4B) — backend aligned to frozen DB NVARCHAR(50).** Decisión humana: máx. 50. `Sesion.nombre` 1..50 en `CrearSesionRequestValidator`, `CrearSesionDomain`, `ActualizarSesionDomain` y `SesionConsultadaEntity`; contrato actualizado en `BACKEND_GOLDEN_PATH_CONTRACT.md`. Evidencia: [AUDIT 4B](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md) (PASS, sin bloqueantes), [VALIDATION](../work-items/LB-001B.4-final-backend-contract-closure/VALIDATION.md) (970 tests, 0F/0E/0S). TD-034 sigue abierta (no bloqueante). **Work item relacionado:** [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).

## TD-049

- **Fecha / responsable:** 2026-09-24 / backend-team + equipo DB.
- **Descripción:** `GET/PUT /api/v1/usuarios/perfil` permanecen fuera del Golden Path y responden
  501 por capacidad pública no implementada/confirmada.
- **Impacto:** Funcional para la vertical de perfil; no afecta las 8 operaciones LB-001C.1.
- **Evidencia:** estado de entrada reportado en
  [ENTRY_EVIDENCE](../work-items/LB-001C-openapi-contract-first/ENTRY_EVIDENCE.md) y tests de
  `UsuarioController` dentro de `mvn verify`.
- **Resolución esperada:** contrato/consumer/DB propios antes de implementar o publicar en OpenAPI.
- **Bloquea línea base:** no para Golden Path.
- **Estado:** ABIERTA / OUT_OF_GOLDEN_PATH / NON_BLOCKING.

## TD-050

- **Fecha / responsable:** 2026-09-24 / backend-team + frontend.
- **Descripción:** MV-001 observó latencia de reconciliación realtime; HTTP recupera el estado y
  sigue siendo source of truth.
- **Impacto:** Experiencia de usuario/operación; no se reportó pérdida de consistencia.
- **Evidencia:** estado de entrada reportado en
  [ENTRY_EVIDENCE](../work-items/LB-001C-openapi-contract-first/ENTRY_EVIDENCE.md). No hay métrica o
  traza sanitizada adjunta en este checkout.
- **Resolución esperada:** medir percentiles y separar latencia SSE, reconexión y recarga HTTP en
  un work item de observabilidad/realtime; no introducir broker/cache por suposición.
- **Bloquea línea base:** no.
- **Estado:** ABIERTA / NON_BLOCKING.

## TD-051

- **Fecha / responsable:** 2026-09-26 / backend-team + seguridad.
- **Clasificación:** `SECURITY_FINDING`, prioridad alta.
- **Descripción:** webhook credential currently has an unsafe functional default.
- **Impacto:** una configuración omitida puede dejar habilitada una credencial conocida por el código/configuración distribuida.
- **Evidencia:** `SecurityConfig` y configuración runtime; se registra archivo/tipo, nunca el valor.
- **Motivo para no resolver ahora:** LB-001D.1 es documentation/governance only; producción/tests/config están prohibidos.
- **Resolución esperada:** LB-001D.2 elimina el default funcional, exige configuración fail-fast/disabled segura y agrega pruebas negativas sin debilitar la autenticación del webhook.
- **Bloquea línea base:** no (resuelta).
- **Estado:** RESUELTA en LB-001D.2 (2026-09-26): `@Value("${app.security.azure-events.webhook-token:}")` y `webhook-token: ${AZURE_EVENTGRID_WEBHOOK_TOKEN:}` sin valor por defecto; credencial vacía/ausente/en blanco → 401 fail-closed. Evidencia: `AzureWebhookUnconfiguredSecurityChainTest`, `AzureEventGridAuthFilterTest`. **Work item:** [LB-001D.2](../work-items/LB-001D-governance-hardening/LB-001D.2-REPORT.md).

## TD-052

- **Fecha / responsable:** 2026-09-26 / backend-team + seguridad.
- **Clasificación:** `SECURITY_FINDING`, prioridad alta.
- **Descripción:** credential may be accepted through URL query parameter.
- **Impacto:** una credencial en URL puede quedar expuesta en historial, proxies, access logs o evidencia.
- **Evidencia:** `AzureEventGridAuthFilter`; no se registra el valor de ninguna credencial.
- **Motivo para no resolver ahora:** LB-001D.1 no modifica código ni tests.
- **Resolución esperada:** LB-001D.2 acepta la credencial únicamente por el header operacional aprobado, agrega negativos de query y verifica logs/evidencia sanitizados.
- **Bloquea línea base:** no (resuelta).
- **Estado:** RESUELTA en LB-001D.2 (2026-09-26): se eliminó `request.getParameter("token")`; la credencial se acepta solo por `aeg-sas-token`, con comparación de digests SHA-256 vía `MessageDigest.isEqual`. Evidencia: query-only (`token`, `access_token`, `key`) → 401 en filtro y en cadena real. **Work item:** [LB-001D.2](../work-items/LB-001D-governance-hardening/LB-001D.2-REPORT.md).

## TD-053

- **Fecha / responsable:** 2026-09-26 / backend-team + testing.
- **Clasificación:** `TEST_CLASSIFICATION_DEBT`.
- **Descripción:** `AzureCloudIntegrationE2ETest` consume Azure real, pero su nombre `*Test` lo sitúa en la suite normal en lugar de un perfil Cloud Integration explícito.
- **Impacto:** `mvn verify` puede depender de red, identidad y recursos Azure; un fallo ambiental se mezcla con unit/component y el build deja de ser reproducible.
- **Evidencia:** clase de test y convenciones Surefire/Failsafe del `pom.xml`.
- **Motivo para no resolver ahora:** renombrar/reconfigurar tests o POM está prohibido en LB-001D.1.
- **Resolución esperada:** LB-001D.2 aísla Azure real mediante perfil/comando explícito, mantiene unit/component sin cloud y registra ambiente/evidencia sanitizada.
- **Bloquea línea base:** no (resuelta).
- **Estado:** RESUELTA en LB-001D.2 (2026-09-26) por clasificación/perfil, sin skips: `AzureCloudIntegrationE2ETest` → `AzureCloudIntegrationIT` (Failsafe, solo lectura, endpoints por `AZURE_KEYVAULT_ENDPOINT`/`AZURE_APPCONFIG_ENDPOINT`), excluido de `-Pintegration` y ejecutado únicamente con `-Pazure-integration`; `mvn verify` no lo ejecuta (`CloudIntegrationClassificationTest` lo protege). Esto no cierra MV-003. **Work item:** [LB-001D.2](../work-items/LB-001D-governance-hardening/LB-001D.2-REPORT.md).

## TD-054

- **Fecha / responsable:** 2026-09-26 / backend-team + contratos/realtime.
- **Clasificación:** `DECISION_REQUIRED`.
- **Descripción:** el procesamiento Azure publica hoy algunos `RealtimeEvent`, mientras el SSE académico entrega solo eventos con `payload.grupo`; cambios de catálogo/configuración no necesariamente tienen ese scope.
- **Impacto:** intención y consumidor del evento no están definidos; podría ser invalidación interna, señal distribuida o evento cliente.
- **Evidencia:** `ProcesarEventoAzureUseCaseImpl`, `LocalSseRealtimeStreamGateway`, [DR-AZ-001](../work-items/LB-001D-governance-hardening/LB-001D.1-DECISIONS.md#dr-az-001).
- **Motivo para no resolver ahora:** la tarea prohíbe decidir por intuición o cambiar realtime.
- **Resolución esperada:** decisión explícita A/B/C, contrato/consumidores y tests antes de cualquier cambio realtime.
- **Bloquea línea base:** no para LB-001D.1; sí para un cambio realtime relacionado.
- **Estado:** DECISION_REQUIRED. **Work item:** [LB-001D.1](../work-items/LB-001D-governance-hardening/LB-001D.1-PLAN.md).
