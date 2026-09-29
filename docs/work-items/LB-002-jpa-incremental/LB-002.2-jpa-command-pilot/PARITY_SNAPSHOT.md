---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2D — PARITY SNAPSHOT

```text
PARITY_TESTS_EXECUTED:   21 (AsistenciaCommandJpaParityIT) + 3 (TransactionBoundary) + 3 (Concurrency) = 27
PARITY_MISMATCHES:       0
DB:                      sql_server_asistencias / gestionasistenciadb (freeze DB desplegado, real)
QUERY_PROVIDER:          jdbc (constante, aisla la variable principal)
COMMAND_PROVIDER:        jpa (candidato bajo prueba, resuelto por Composition Root)
```

Metodología: SESSION A (fila `Sesion` dedicada) ejecuta el baseline JDBC construido manualmente
(`new AsistenciaRepositorySqlServerAdapter(namedJdbc, procedureExecutor)`); SESSION B (fila `Sesion`
distinta) ejecuta el candidato JPA real resuelto por el Composition Root
(`app.adapters.persistence.asistencia-command-provider=jpa`, `@Autowired AsistenciaRepositoryPort`).
JDBC y JPA nunca comparten sesión. Cada escenario hace doble aserción: (A) `Outcome(JDBC) ==
Outcome(JPA)` campo a campo (impreso como línea `PARITY|scenario=...|MISMATCH=n`), y (B) el oráculo
contractual absoluto (verdad de DB vía `sys.columns`/tablas base, nunca solo "ambos hicieron lo mismo").

## CMD-PAR-001 .. 018

| Escenario | Estado | JDBC outcome | JPA outcome | Oráculo absoluto | Mismatch |
|---|---|---|---|---|---|
| CMD-PAR-001 (AN/SJC/EX) | EXECUTED | sin excepción, 3 headers, 3 detalles | idéntico | AN presente=true; SJC/EX presente=false; estados exactos | 0 |
| CMD-PAR-002 (roundtrip EX) | EXECUTED | EX/presente=false vía tabla base, query JDBC y query JPA | idéntico | EX nunca reconstruido como SJC en ninguna de las 3 lecturas | 0 |
| CMD-PAR-003 (docente ajeno) | EXECUTED | `ForbiddenException`/`FORBIDDEN`, 0 filas | idéntico | 0 headers, 0 detalles | 0 |
| CMD-PAR-004 (estado ABC) | EXECUTED | `ValidationException`/`VALIDATION_ERROR`, 0 filas | idéntico | `dbo.RazonCausa` cardinalidad y conteo `ABC` sin cambio | 0 |
| CMD-PAR-005 (rollback AN/ABC/EX) | EXECUTED | `ValidationException`, 0 filas | idéntico | ni AN ni EX sobreviven | 0 |
| CMD-PAR-006 lista vacía | EXECUTED | rechazo (DBCODE=ATT_002) | idéntico | 0 filas | 0 |
| CMD-PAR-006 estudiante duplicado | EXECUTED | rechazo (DBCODE=ATT_003) | idéntico | 0 filas | 0 |
| CMD-PAR-006 fuera del grupo | EXECUTED | `ForbiddenException`/`FORBIDDEN` (DBCODE=EST_004) | idéntico | 0 filas | 0 |
| CMD-PAR-006 sesión inexistente | EXECUTED | `ResourceNotFoundException`/`RESOURCE_NOT_FOUND` (DBCODE=SES_001) | idéntico | 0 filas | 0 |
| CMD-PAR-006 matrícula inactiva | EXECUTED | `ForbiddenException`/`FORBIDDEN` | idéntico | 0 filas; catálogo `EstadoEstudianteGrupo` tenía `CVP/CI/F` además de `A` en la DB desplegada, por lo que fue posible construir el escenario determinista (no `NOT_OBSERVABLE`) | 0 |
| CMD-PAR-007 (Usuario.id vs Docente.id) | EXECUTED | Docente.id → rechazo técnico (`DatabaseOperationException`/`ERR_DB_UNCLASSIFIED`); Usuario.id → éxito | idéntico en ambos sub-casos | Docente.id≠Usuario.id verificado antes; 0 filas con Docente.id, 1 fila con Usuario.id | 0 |
| CMD-PAR-008 (usuarioEjecutor=null) | EXECUTED | `ValidationException`/`VALIDATION_ERROR` | idéntico | 0 filas | 0 |
| CMD-PAR-008 (idSesion=null) | EXECUTED | `DatabaseOperationException` (técnica: `TechnicalException`, no `ApplicationException`) — un UUID null no tiene tipo SQL inferible a nivel de driver; ambos providers delegan la misma conversión | idéntico (misma clase, mismo código técnico) | clasificación técnica preservada igual en ambos providers; nunca una excepción cruda sin envolver | 0 |
| CMD-PAR-009 (idempotencia) | EXECUTED | 2ª llamada no duplica: 3 headers, 3 detalles | idéntico | estado final idéntico tras 1ª y 2ª llamada | 0 |
| CMD-PAR-010 (frontera transaccional) | EXECUTED (IT separado `AsistenciaCommandTransactionBoundaryIT`, pool=1) | n/a (solo candidato JPA, baseline no aplica a este escenario) | éxito: `@@TRANCOUNT=0`, autocommit=true, visible desde conexión independiente; rechazo: 0 filas, `@@TRANCOUNT=0`; 10 rechazos + 1 éxito sin residual | sin transacción JPA exterior en ningún caso | 0 |
| CMD-PAR-011 (JSON > 4000) | EXECUTED | rechazo (`ForbiddenException`/`FORBIDDEN`, nunca ATT_001) | idéntico | longitud real del payload > 4000 (no se imprime el payload); nunca truncamiento | 0 |
| CMD-PAR-012 (concurrencia) | EXECUTED (IT separado `AsistenciaCommandConcurrencyIT`, 4 hilos, `CyclicBarrier`) | JDBC: sin duplicados, sin excepción cruda | JPA: sin duplicados, sin excepción cruda; estado final convergente == JDBC | 1 header + 1 detalle por estudiante tras 4 llamadas concurrentes idénticas | 0 |
| CMD-PAR-013 (firma/orden) | EXECUTED | `sys.parameters` orden 1:@idSesion:uniqueidentifier, 2:@asistenciaJSON:nvarchar, 3:@idCorrelacion:uniqueidentifier, 4:@idUsuarioEjecutor:uniqueidentifier | n/a (verificación de contrato DB, no de comportamiento por provider) | coincide exactamente con el binding productivo (`AsistenciaJpaCommandPersistence`, registro posicional 1..4) | 0 |
| CMD-PAR-014 (fuga de conexiones) | EXECUTED (solo candidato JPA) | n/a | 50 rechazos + 1 éxito adicional: `activeConnections=0`, `threadsAwaitingConnection=0` en todo momento | el éxito adicional funciona tras los 50 rechazos | 0 |
| CMD-PAR-015 (readback JDBC/JPA) | EXECUTED (dentro de CMD-PAR-002) | lectura JDBC y lectura JPA sobre la misma fila persistida por JPA | idéntico | campo a campo (`estado`, `presente`) | 0 |
| CMD-PAR-016 (correlación) | EXECUTED | ninguna ejecución produjo `ERR_DB_CANONICAL_CONTRACT` (el eco de correlación es exigido idénticamente por `CanonicalStoredProcedureExecutor` y `CanonicalProcedureResultValidator`, ambos delegan en `DbExceptionTranslator`) | idéntico | ausencia de `ERR_DB_CANONICAL_CONTRACT` en ambos providers | 0 |
| CMD-PAR-016 (auditoría) | NOT_OBSERVABLE | — | — | este arnés llama a `AsistenciaRepositoryPort` directamente (sin HTTP/controller); la escritura en `dbo.AuditoriaEvento` la realiza el interceptor de auditoría a nivel de controller, no el repositorio. No existe una forma contractual segura de observarlo desde este nivel sin modificar Application/HTTP (fuera de alcance de LB-002.2D) | n/a |
| CMD-PAR-017 (lote parcial 2/3) | EXECUTED | 2 filas exactas, el tercero SIN fila (nunca AN por defecto) | idéntico | ausencia de fila ≠ AN | 0 |
| CMD-PAR-018 (upsert AN→EX) | EXECUTED | 1 sola fila final, estado EX, presente=false | idéntico | sin duplicados | 0 |

**Criterio principal: `PARITY_MISMATCHES = 0`.** Ningún escenario obligatorio quedó `NOT_RUN`. Solo
el sub-punto de auditoría de CMD-PAR-016 es `NOT_OBSERVABLE`, justificado arriba (permitido
explícitamente por el TEST_PLAN para esta excepción).

## REALTIME SNAPSHOT (CMD-RT-001..004)

| Escenario | Estado | Resultado |
|---|---|---|
| CMD-RT-001 (arnés backend, no SSE/browser) | PASS | `AsistenciaCommandRealtimeIT` usa `RegistrarAsistenciasSesionUseCaseImpl` REAL (sin modificar), construido con el `AsistenciaRepositoryPort` candidato JPA real (Composition Root, `command-provider=jpa`) y un `RealtimePublisherPort` spy de test (Mockito puro, no bean de Spring — evita romper el wiring del gateway SSE local) |
| CMD-RT-002 (éxito → 1 evento, payload canónico) | PASS | exactamente 1 `publish()`; `type=ASISTENCIAS_SESION_ACTUALIZADAS`; `payload={grupo, sesion, totalRegistros=3}`; en el instante exacto de `publish()` las 3 filas YA eran visibles en SQL Server (persistencia antes de publicación) |
| CMD-RT-003 (2 fallos → 0 eventos) | PASS | docente ajeno (`ForbiddenException`) y estado ABC (`ValidationException`, rechazado en el constructor del Domain antes de llegar al repositorio): en ambos, `realtimePublisherPort.publish(...)` nunca se invoca; 0 filas persistidas |
| CMD-RT-004 (E2E SSE/browser) | NOT_RUN — LB-002.2E | DB + HTTP siguen siendo source of truth en esta fase; SSE sigue siendo señal de invalidación, no se certifica aquí |

## Precondición contractual (sección 7)

`GoldenPathSqlStoredProcedureContractIT` ejecutado de forma dirigida contra `dbo.usp_registrar_asistencias_sesion`
antes del arnés principal: 16/16 tests, 0 failures — confirma 4 parámetros, nombres, tipos y `parameter_id`
exactos antes de construir cualquier evidencia de paridad.
