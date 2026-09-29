---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2C — VALIDATION (interpretación de la evidencia)

Interpreta [GREEN_SNAPSHOT](GREEN_SNAPSHOT.md). Nivel de confianza: **implementado y validado por unit/component/ArchUnit/OpenAPI en un `clean verify` hermético**. **No** es validación contra SQL Server real de la equivalencia JDBC↔JPA (eso es LB-002.2D).

## Qué se validó

| Área | Evidencia | Resultado |
|---|---|---|
| RED congelado intacto | 5/5 SHA-256 iguales a RED_SNAPSHOT, recalculados tras el `clean verify` final | PASS |
| RED → GREEN | Acceptance RED-A..D (6) + caracterización RED-E/F (3) | 9/9 |
| Tests nuevos 2.2C | 126 tests (adapter JPA 41, condición EMF 29, Composition Root 21, validador 20, ArchUnit 9, routing híbrido 6) | 126/126 |
| Gate global | `clean verify`: 1426 tests, 0 failures/errors/skipped; Enforcer Java 25; JaCoCo LINE 90.63 % / BRANCH 81.17 % (umbrales 80/70) | PASS |
| Arquitectura | ArchUnit verde; Domain/Application sin JPA/Hibernate/Spring Data; candidato solo en `..sqlserver.jpa..`; sin `@Transactional`/`JpaTransactionManager`/`getTransaction`/`joinTransaction`/`doWork`/`unwrap` | PASS |
| Contratos HTTP | OpenAPI conformance/validation/Swagger 16/16; hashes de OpenAPI, contrato backend y contrato DB sin cambio | PASS |
| Alcance | Solo 5 archivos tracked autorizados + archivos nuevos; sin cambios en Domain, Application, puerto, controllers, OpenAPI, pom, SecurityConfig, SP/DB, frontend, YAML local/dev | PASS |

## Cambios de producción (resumen)

- `AsistenciaCommandPersistence` (nuevo): abstracción interna del command de lote.
- `AsistenciaJpaCommandPersistence` (nuevo): `EntityManager` + `StoredProcedureQuery`, binding **posicional** 1..4 (hallazgo U-03), un EM por invocación cerrado con try-with-resources, sin transacción JPA, catch acotado (`PersistenceException | IllegalStateException | IllegalArgumentException`) solo alrededor de la ejecución; el validador corre fuera del catch, de modo que un 403/400 de negocio nunca se degrada a 500.
- `CanonicalProcedureResultValidator` (nuevo): cardinalidad 1, eco de correlación y delegación **exclusiva** de la clasificación en `DbExceptionTranslator` (sin duplicar `DBCODE`).
- `JpaCapabilityRequiredCondition` (nueva): EMF único si `query=jpa OR command=jpa`, misma regla de parseo que los selectors; valor inválido en cualquiera falla el arranque (fail-closed).
- `SqlServerCoreRepositoryAdapterConfiguration`: segundo selector independiente, matriz 2×2, un solo `AsistenciaRepositoryPort`; `(jdbc,jdbc)` devuelve el adapter JDBC desnudo (0 EMF).
- `AsistenciaRepositoryHybridSqlServerAdapter`: constructor de 3 argumentos; el de 2 se conserva; solo `registrarAsistenciasSesion` se enruta al command seleccionado.
- `SqlServerJpaAsistenciaQueryAdapterConfiguration`: condición del EMF sustituida por `JpaCapabilityRequiredCondition` (sin renombrar la clase).
- `application.yml`, `.env.example`: propiedad/variable nueva con default `jdbc`.

## Rollback

`APP_ADAPTERS_PERSISTENCE_ASISTENCIA_COMMAND_PROVIDER=jdbc` (o quitar la variable) + reinicio. Como el default es `jdbc` y ningún perfil activa `jpa` para el command, el código nuevo es inerte sin configuración explícita. Revertir el working tree restaura el baseline; ningún dato, esquema ni SP cambia.

## Autoauditoría (checklist §23)

| Riesgo revisado | Resultado |
|---|---|
| Selector que migra otros commands | No: los demás delegan a `commands` (JDBC); CMD-CFG-007 |
| Segundo EMF / transacción JPA oculta | No: 0/1 EMF por matriz; sin `JpaTransactionManager`; ArchUnit + CMD-CFG-006 |
| Binding nominal residual / parámetro posicional equivocado | No: 1 idSesion, 2 JSON, 3 idCorrelacion, 4 idUsuarioEjecutor (CMD-ADP-005); guardia de orden con SQL real **existe** (CMD-PAR-013) pero **no se ejecutó** |
| EntityManager sin cerrar | try-with-resources; CMD-ADP-012/013 |
| Catch demasiado amplio | Acotado; CMD-ADP-014 |
| Duplicación de DBCODE | No: solo `DbExceptionTranslator` |
| Cambio HTTP indirecto | No: OpenAPI y hashes intactos |
| Perfil convertido en provider / selector fail-open / JPA por defecto | No: default `jdbc`, local/dev `command=jdbc`, valor inválido falla el arranque (CMD-CFG-008/011) |
| Logs con JSON/PII | El log solo emite operación y correlationId; CMD-ADP-011 |
| Tests cosméticos / RED alterado | No: 0 tests tracked editados; RED hash-idéntico |
| Documentos que afirman más de lo validado | Ver «Qué NO se validó» |

No se detectó ninguna regresión real de 2.2C; **no se aplicó ninguna corrección** durante la validación final.

## Riesgos residuales

1. **Equivalencia con SQL Server real no demostrada**: el comportamiento del binding, result sets con update counts y autocommit descansa en la feasibility de 2.2B (`JpaAttendanceStoredProcedureFeasibilityIT`, ejecutada en 2.2B) y en tests con fakes; la paridad de extremo a extremo (CMD-PAR-001..018) es 2.2D.
2. **Orden posicional**: un cambio futuro de `parameter_id` del SP rompería el binding en silencio (3 de 4 son UUID); lo protege CMD-PAR-013, aún no ejecutado como gate.
3. **Concurrencia y fugas de conexión** (CMD-PAR-012/014) sin evidencia en esta fase.
4. **TD-043** sigue impidiendo un `-Pintegration verify` global verde; no se intentó resolver.
5. La activación en local/dev/prod del command JPA no está decidida ni implementada.

## Qué NO se validó

- Paridad JDBC↔JPA en SQL Server real (CMD-PAR-*): `NOT_RUN — LB-002.2D`.
- Realtime con adapter JPA real (CMD-RT-002/003) y contexto Spring real sin `JpaTransactionManager` con DB (parte IT de CMD-CFG-006): `NOT_RUN`.
- E2E HTTP + SSE + frontend: `NOT_RUN — LB-002.2E`.
- `-Pintegration verify` y `-Pazure-integration`: no ejecutados (TD-043 = OPEN / DEFERRED; `GLOBAL_INTEGRATION_PROFILE = NOT_GREEN_TD043`).
- Rendimiento, timeouts y comportamiento bajo carga.

## Siguiente gate

LB-002.2D — paridad en SQL Server real (`AsistenciaCommandJpaParityIT`, `AsistenciaCommandRealtimeIT`) previa revisión humana independiente de 2.2C. No iniciada.
