---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2B — RED SNAPSHOT (ACCEPTANCE RED congelado)

```text
ACCEPTANCE_RED: CONFIRMED
```

## Congelación

El RED queda **congelado**. Un implementador posterior **NO puede**: modificar expectativas, eliminar tests, `@Disabled`arlos, cambiar asserts, cambiar propiedades ni relajar el comportamiento para conseguir GREEN. Si el RED necesita cambiar: `TEST_CONTRACT_CONFLICT` + revisión humana (dictamen del auditor). Los tests unitarios de `AsistenciaJpaCommandPersistence`, `CanonicalProcedureResultValidator` y `JpaCapabilityRequiredCondition` se añadirán en 2.2C, tests-first, tras congelar sus firmas, **sin** modificar ni debilitar este RED.

## Identificación

| Campo | Valor |
|---|---|
| BASE_GIT | `0bfc02a58a8abc894841cc1e9b31c002a716554e` (rama `jose-valencia/lb-002.2a-jpa-command-plan`); sin commit de 2.2B (los archivos son nuevos, sin commitear) |
| Fecha | 2026-09-28 |
| Baseline `clean verify` previo | PASS — [BASELINE.md](BASELINE.md): 1291 tests, 0 fallos, LINE 90.53 % / BRANCH 80.81 %, ejecutado **antes** de añadir cualquier test |
| Feasibility previo | PASS — [FEASIBILITY.md](FEASIBILITY.md): `JPA_STORED_PROCEDURE_FEASIBILITY: PASS`, sin `STOP: NO_CLEAN_JPA_PATH` |
| Producción tocada | NO. Solo archivos NUEVOS bajo `src/test` y docs del work item (`git diff` de archivos rastreados: vacío) |
| Regla Q-1 | Solo costuras productivas existentes; sin production skeletons; sin imports de `AsistenciaJpaCommandPersistence`/`CanonicalProcedureResultValidator`/`JpaCapabilityRequiredCondition`; sin reflexión para preguntar por clases futuras; sin `assertTrue(false)`; ningún test espera `ClassNotFoundException` |

## Archivos y SHA-256

SHA-256 del contenido con finales de línea LF (forma canónica del repositorio, `*.java text eol=lf`; el working tree en Windows usa CRLF): `sed 's/\r$//' <archivo> | sha256sum`.

| Rol | Archivo (`src/test/java/co/edu/uco/asistenciasuco/…`) | SHA-256 |
|---|---|---|
| **ACCEPTANCE RED** | `infrastructure/config/adapters/persistence/sqlserver/AsistenciaCommandProviderAcceptanceRedTest.java` | `d19d00e289d9f58ad273af003ecd77d002f8b924b96e8740345675685eefe7b6` |
| CARACTERIZACIÓN (PASS) | `infrastructure/config/adapters/persistence/sqlserver/AsistenciaCommandProviderCharacterizationTest.java` | `a83b5740164544da1081c6d4a4dd7033387f49d5ba262c09c72c1bb3c8c524bb` |
| Soporte del RED-B (conteo de definiciones EMF) | `infrastructure/config/adapters/persistence/sqlserver/EntityManagerFactoryDefinitionProbe.java` | `a08a76a07a5137094580af7f3187a50ea860bc049f18562d9d34b02e38a452c8` |
| Feasibility IT (no RED) | `infrastructure/adapter/secondary/persistence/sqlserver/jpa/JpaAttendanceStoredProcedureFeasibilityIT.java` | `b96b944c0c85801b690ec2c79f3d3df9894ffceeda4951938523f3830bb94be2` |
| Soporte de fixture (no RED) | `infrastructure/adapter/secondary/persistence/sqlserver/jpa/AttendanceCommandFixture.java` | `c2d92ceb97f7715fa423203a9c251bc33299379864b391b1f7cd16890a882a99` |

El soporte del RED-B forma parte del congelamiento: cambiarlo altera lo que mide RED-B.

## Comando y resultado

```powershell
.\mvnw.cmd test "-Dtest=AsistenciaCommandProviderAcceptanceRedTest,AsistenciaCommandProviderCharacterizationTest"
```

| Campo | Valor |
|---|---|
| Entorno | Sin SQL Server / Azure (hermético) |
| Fecha/hora | 2026-09-28 01:04:04 → 01:04:30 -05:00 |
| Exit code | **1** (esperado: RED) |
| Tests | 9 ejecutados · **3 passed** (caracterización) · **6 failed** (RED) · 0 errors · 0 skipped |
| Fallos esperados | 6 (RED-A, RED-B, RED-C×3 valores, RED-D) |
| **Fallos inesperados** | **0** |

Comprobación de no-regresión colateral (`.\mvnw.cmd clean verify -Dmaven.test.failure.ignore=true`, 2026-09-28 01:05:03 → 01:07:36): **1300** tests (= 1291 del baseline + 9 nuevos), **6 failures** (exactamente los RED-A/B/C×3/D), 0 errors, 0 skipped; `All coverage checks have been met`. Ningún test preexistente cambió de estado. Nota: `.\mvnw.cmd clean verify` **sin** esa opción termina en exit 1 mientras el RED esté congelado y hasta 2.2C; ese exit 1 es consecuencia buscada de un RED confirmado, no una regresión.

## Detalle de cada test

| ID | Tipo | Propiedades | Estado hoy | Motivo exacto del fallo (por COMPORTAMIENTO) | Por qué es el fallo esperado |
|---|---|---|---|---|---|
| RED-A | RED | `query=jdbc`, `command=jpa` | **FAIL** | `Wanted but not invoked: entityManagerFactory.createEntityManager()` — `Actually, there were zero interactions with this mock.` | La propiedad de command se ignora: `registrarAsistenciasSesion` sigue por JDBC y el EMF nunca se toca |
| RED-B | RED | `query=jdbc`, `command=jpa` | **FAIL** | `command=jpa exige el EntityManagerFactory aunque query=jdbc. Definiciones EMF: 0 ==> expected: <1> but was: <0>` | La condición del EMF depende exclusivamente de `query=jpa` (D3 aprobado exige `query=jpa OR command=jpa`) |
| RED-C `[1..3]` | RED | `command=valor-invalido` / `hibernate` / `""` | **FAIL** ×3 | `Un valor no soportado del command provider debe fallar el arranque. ==> expected: not <null>` (`getStartupFailure()` es `null`) | La propiedad `asistencia-command-provider` aún no se interpreta ⇒ el contexto arranca (D2 exige fail-closed nombrando la propiedad) |
| RED-D | RED | `query=jdbc`, `command=jpa` | **FAIL** | La parte de query pasa (JDBC, EMF sin uso); luego `Wanted but not invoked: entityManagerFactory.createEntityManager()` — el command sigue en JDBC | Los selectors deben ser independientes: query JDBC + command JPA en un único `AsistenciaRepositoryPort`; hoy el command no obedece a su selector |
| RED-E | CARACTERIZACIÓN | sin `command` | **PASS** | — | El default sin propiedad es command JDBC (el cambio futuro no debe alterarlo); EMF sin interacciones |
| RED-F | CARACTERIZACIÓN | `query=jpa`, sin `command` | **PASS** | — | Estado actual de local/dev: query JPA (vía EMF) + command JDBC (sin tocar el EMF) |
| (técnica RED-B) | CARACTERIZACIÓN | `query=jdbc`→0, sin propiedades→0, `query=jpa`→1 | **PASS** | — | Demuestra que la medición de RED-B cuenta bien la presencia/ausencia de EMF; RED-B falla por la condición, no por un defecto de la medición |

Falsos rojos descartados: ninguno de los 6 fallos se debe a una clase inexistente, a DB/red ausente, a un mock mal configurado ni a `assertTrue(false)`. En una iteración previa de RED-B la medición instanciaba el `LocalContainerEntityManagerFactoryBean` (Hibernate intentaba arrancar con un `DataSource` falso) y la caracterización de la técnica dio `ERROR`; se sustituyó por conteo de definiciones sin `refresh()` (`EntityManagerFactoryDefinitionProbe`) **antes** de congelar.

## Satisfacibilidad (revisión previa al congelamiento)

Cada RED es alcanzable con la implementación aprobada sin tocar el test: RED-A/D — el candidato abre un EM del EMF por invocación (D1/D4); RED-B — la condición `query=jpa OR command=jpa` (D3) mantiene la definición `entityManagerFactory` en `SqlServerJpaAsistenciaQueryAdapterConfiguration`; RED-C — el parseo fail-closed del selector nombra la propiedad (D2, mismo patrón que el selector de query). Si una implementación aprobada no pudiera satisfacer alguno por diseño, sería `TEST_CONTRACT_CONFLICT`, no motivo para editarlo.

## Lo que este RED NO cubre (diferido, sin debilitarlo)

Matriz completa `CMD-ADP/PAR/RT/ARCH` de [TEST_PLAN](TEST_PLAN.md) que depende de clases productivas inexistentes: `AsistenciaJpaCommandPersistenceTest`, `CanonicalProcedureResultValidatorTest`, `JpaCapabilityRequiredConditionTest`, `AsistenciaRepositoryHybridCommandRoutingTest`, `JpaCommandIsolationRulesTest`, `AsistenciaCommandJpaParityIT`, `AsistenciaCommandRealtimeIT`. Se escribirán tests-first en 2.2C.
