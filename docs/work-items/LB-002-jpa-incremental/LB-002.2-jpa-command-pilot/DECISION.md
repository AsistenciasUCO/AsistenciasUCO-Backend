---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2A — DECISION (propuesta para revisión humana)

Estado: **REVISADO — `LB-002.2A: APPROVED_FOR_2_2B`** (revisión humana registrada el 2026-09-28 al iniciar 2.2B; ver §Resultado de la revisión). La aprobación habilita 2.2B (baseline + feasibility + acceptance RED), **no** la implementación productiva. Cada decisión separa evidencia AS-IS de recomendación TARGET. Las hipótesis que solo SQL Server real puede resolver se marcan `U-xx` y se resuelven como **JPA STORED PROCEDURE FEASIBILITY PROBES** (no RED) en 2.2B ([PLAN §Incógnitas](PLAN.md#incognitas-abiertas-u-xx)).

## Resultado de la revisión (2.2A → 2.2B)

| Decisión | Resultado | Precisión vinculante |
|---|---|---|
| D1 | APPROVED | `EntityManager` + `StoredProcedureQuery`. |
| D2 | APPROVED | `app.adapters.persistence.asistencia-command-provider`, valores futuros `jdbc \| jpa`, default futuro `jdbc`. **Durante LB-002.2 gobierna EXCLUSIVAMENTE `registrarAsistenciasSesion`**; los demás commands de Asistencia siguen JDBC. Ampliar la semántica exige fase posterior explícita y nueva evidencia. R-13 cerrado: se conserva el nombre. |
| D3 | APPROVED | Un solo EMF cuando `query=jpa OR command=jpa`. |
| D4 | APPROVED AS TARGET TO VALIDATE | Target: no introducir transacción JPA exterior al SP. **No se afirma aún que Hibernate lo soporte**; se demuestra con SQL Server real (probe U-01). |
| D5 | APPROVED WITH CLARIFICATION | Sin result set canónico / 0 filas ⇒ `ERR_DB_CANONICAL_CONTRACT` (semántica del baseline). Fallo técnico verdadero de Hibernate/JPA ⇒ `DATABASE_OPERATION_ERROR`. Una `IllegalStateException` de navegación del result set no puede convertir automáticamente una violación del contrato canónico en un 500 técnico distinto del baseline (el paso 7 de D5 debe distinguirlas). |
| D6 | APPROVED | Rollback: `APP_ADAPTERS_PERSISTENCE_ASISTENCIA_COMMAND_PROVIDER=jdbc` + reinicio. |
| Q-1 | RESOLVED | El ACCEPTANCE RED usa costuras productivas existentes; sin production skeletons; sin tests que importen clases futuras. |
| Q-2 | RESOLVED | No se modifica `AsistenciaRepositorySqlServerIT`; fixture compartido = soporte NUEVO bajo `src/test` o copia mínima. |

## Resultado de la revisión (2.2B → 2.2C)

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

### Estado de implementación en LB-002.2C

Verificado por unit/component/ArchUnit en el `clean verify` final ([GREEN_SNAPSHOT](GREEN_SNAPSHOT.md)); la equivalencia con SQL Server real sigue pendiente (2.2D).

| ID | Estado en 2.2C | Evidencia |
|---|---|---|
| D1 | IMPLEMENTED_AND_VERIFIED | `AsistenciaJpaCommandPersistence` con `EntityManager` + `StoredProcedureQuery`, binding posicional 1..4; sin `@Procedure` ni Spring Data (ArchUnit `JpaCommandIsolationRulesTest`) |
| D2 | IMPLEMENTED_AND_VERIFIED | Selector `asistencia-command-provider` (default `jdbc`, fail-closed), gobierna solo `registrarAsistenciasSesion`; local/dev = `jdbc` |
| D3 | IMPLEMENTED_AND_VERIFIED | `JpaCapabilityRequiredCondition`; EMF 0/1/1/1 según matriz 2×2 (`JpaCapabilityRequiredConditionTest`, con la configuración real y `ApplicationContextRunner`) |
| D4 | IMPLEMENTED_AND_VERIFIED | Sin `@Transactional`/`JpaTransactionManager`/`getTransaction`/`joinTransaction`; EM cerrado por try-with-resources. Demostración con SQL real: probe U-01 de 2.2B |
| D5 | IMPLEMENTED_AND_VERIFIED | `CanonicalProcedureResultValidator` + `DbExceptionTranslator`; 0/≠1 filas y correlación ajena ⇒ `ERR_DB_CANONICAL_CONTRACT`; fallo técnico ⇒ `DATABASE_OPERATION_ERROR` |
| D6 | IMPLEMENTED_AND_VERIFIED | Rollback por variable + reinicio (CMD-CFG-012 en tests de Composition Root); documentado en `.env.example` y VALIDATION |

Las secciones D1–D6 de abajo conservan el texto de la propuesta original; donde difieren de esta tabla, **prevalece la tabla**.

```text
D1  MECANISMO JPA:          EntityManager + StoredProcedureQuery (NO Spring Data @Procedure)                      [APPROVED]
D2  SELECTOR DEL COMMAND:   app.adapters.persistence.asistencia-command-provider = jdbc | jpa (default jdbc, fail-closed);
                            gobierna EXCLUSIVAMENTE registrarAsistenciasSesion en LB-002.2                          [APPROVED]
D3  EntityManagerFactory:   UN solo EMF; se crea si query=jpa OR command=jpa (misma regla de parseo que los selectors) [APPROVED]
D4  TRANSACCIÓN:            TARGET: sin transacción JPA exterior al SP (autocommit, como el baseline); a validar (U-01) [APPROVED AS TARGET TO VALIDATE]
D5  TRADUCCIÓN DE ERRORES:  DbExceptionTranslator (público); sin result set canónico ⇒ ERR_DB_CANONICAL_CONTRACT;
                            fallo técnico verdadero ⇒ DATABASE_OPERATION_ERROR                                     [APPROVED WITH CLARIFICATION]
D6  ROLLBACK:               APP_ADAPTERS_PERSISTENCE_ASISTENCIA_COMMAND_PROVIDER=jdbc + reinicio                    [APPROVED]
```

## D1. Mecanismo JPA: `EntityManager`/`StoredProcedureQuery` vs Spring Data `@Procedure`

[JDBC_TO_JPA §3.4](../../../persistence/JDBC_TO_JPA.md) permite ambos y exige justificar. La elección se decide por lo que el SP **realmente devuelve y cómo falla**, no por cantidad de código.

### Hechos AS-IS que condicionan la decisión

| Hecho | Evidencia |
|---|---|
| El SP devuelve **un result set de una fila y 4 columnas en orden exacto** (`idCorrelacion`, `mensajeUsuarioResultado`, `mensajeTecnicoResultado`, `estadoResultado`). El **fallo de negocio viaja dentro de esa fila** (`estadoResultado=0` + `DBCODE=`), no como excepción SQL. | [DB_BASELINE_CONTRACT §Public SP Result](../../../contracts/external/db/DB_BASELINE_CONTRACT.md); [CanonicalProcedureResultMapper](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/support/procedure/CanonicalProcedureResultMapper.java) |
| El baseline exige: exactamente 1 fila, `idCorrelacion` devuelto == el del request, y luego traduce por `DBCODE`. | [CanonicalStoredProcedureExecutor.execute](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/support/procedure/CanonicalStoredProcedureExecutor.java) |
| Firma real verificada contra SQL Server: `@idSesion uniqueidentifier, @asistenciaJSON nvarchar, @idCorrelacion uniqueidentifier, @idUsuarioEjecutor uniqueidentifier` (este último con `= NULL`). | `GoldenPathSqlStoredProcedureContractIT` y `SqlStoredProcedureContractIT` (tipos y `parameter_id`); el `.md` del contrato DB solo nombra los 4 parámetros |
| El SP gestiona su propia transacción; el JDBC baseline corre en autocommit (sin `@Transactional`/`TransactionOperations` en el adapter de asistencia). | Javadoc de `AsistenciaRepositorySqlServerIT`; `AsistenciaRepositorySqlServerAdapter` |
| ArchUnit prohíbe cualquier dependencia de `org.springframework.data.repository.Repository`; la auto-config de Spring Data JPA está excluida; LB-002.1 decidió `SPRING DATA REPOSITORY: NO`. | `JpaIsolationRulesTest.jpa_no_expone_api_de_escritura_via_spring_data_repository`; `AsistenciasUcoApplication`; [LB-002.1-DECISION](../LB-002.1-DECISION.md) |

### Comparación

| Criterio | `@Procedure` (Spring Data) | `EntityManager`/`StoredProcedureQuery` |
|---|---|---|
| Lectura del result set canónico de 4 columnas | `@Procedure` mapea OUT params/escalar/entidad. Para leer una fila arbitraria hay que declarar un `@NamedStoredProcedureQuery` sobre una `@Entity`, es decir **inventar una entidad para un resultado que no es tabla**. | Lee las filas como `Object[]` en el orden contractual y las convierte al `CanonicalProcedureResult` ya existente. |
| Fallo de negocio in-band (`estadoResultado=0`) | El valor de retorno ya viene "interpretado"; no hay punto natural para aplicar cardinalidad/correlación/`DbExceptionTranslator`. | Control total del post-proceso; mismo pipeline que el baseline. |
| Result sets vs. update counts previos (si el SP no usa `SET NOCOUNT ON`) | Sin control. | `execute()` / `hasMoreResults()` / `getUpdateCount()` permiten avanzar hasta el result set (equivale a `executeQuery` de JDBC). **Incógnita U-02.** |
| Tipado de parámetros (UUID, `null` en `idUsuarioEjecutor`) y binding con nombre | Derivado de la firma del método. | `registerStoredProcedureParameter(name, type, IN)` explícito. **Incógnitas U-03/U-04.** |
| Sin transacción (igual que JDBC) | El repositorio Spring Data suele exigir/abrir transacción; requeriría `JpaTransactionManager` (hoy excluido a propósito). | EM de aplicación sin transacción, creado y cerrado por invocación (patrón ya usado por `AsistenciaJpaQueryPersistence`). |
| Compatibilidad con reglas vigentes | **Viola** ArchUnit `Repository`, revierte LB-002.1 y reintroduce auto-config JPA/`JpaTransactionManager`. | Cumple: solo `jakarta.persistence` dentro de `..sqlserver.jpa..`. |
| Testabilidad | Difícil de aislar sin contexto Spring Data. | Costura unitaria (EMF mockeable, sin certificar) + IT real; mismo patrón que la query. |

### Decisión propuesta

**`EntityManager` + `StoredProcedureQuery`**, encapsulado en `AsistenciaJpaCommandPersistence` (NUEVA, Infrastructure, paquete `..sqlserver.jpa`), detrás de una abstracción interna `AsistenciaCommandPersistence` (NUEVA, Infrastructure, paquete `..sqlserver.core`, un solo método `registrarAsistenciasSesion`), simétrica a `AsistenciaQueryPersistence`. **No** es un Application Port.

`@Procedure` no se descarta por preferencia sino porque **no puede** entregar el contrato de resultado canónico sin fabricar una entidad y sin salir de las reglas de aislamiento vigentes.

### STOP asociado

Si `StoredProcedureQuery` **no puede** (a) ejecutarse sin transacción activa **o** (b) leer el result set canónico preservando posición y tipos **o** (c) enlazar `null`/UUID sin diferir del baseline, y la única salida es duplicar reglas en Java, envolver el SP en una transacción distinta o usar `Session.doWork` (JDBC disfrazado), se declara `STOP: NO_CLEAN_JPA_PATH`. No se sustituye por JDBC etiquetado como JPA.

## D2. Selector del command y su relación con el selector de query

### AS-IS

`app.adapters.persistence.asistencia-query-provider` (`jdbc` default; `jpa` en perfiles `local`/`dev`) solo gobierna `consultarAsistenciasPorGrupo`; **todos** los commands van a JDBC. `SqlServerCoreRepositoryAdapterConfiguration.asistenciaRepositoryPort` elige con un `switch` sobre ese único selector ([código](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/adapters/persistence/sqlserver/SqlServerCoreRepositoryAdapterConfiguration.java)).

### Propuesta

```yaml
app:
  adapters:
    persistence:
      asistencia-query-provider:   ${APP_ADAPTERS_PERSISTENCE_ASISTENCIA_QUERY_PROVIDER:jdbc}     # existente
      asistencia-command-provider: ${APP_ADAPTERS_PERSISTENCE_ASISTENCIA_COMMAND_PROVIDER:jdbc}   # NUEVO
```

- Valores `jdbc | jpa`, `trim` + minúsculas, **misma rutina de parseo** que el selector de query; valor vacío/desconocido → `IllegalStateException` al arrancar (fail-closed). Sin propiedad → `jdbc`.
- Independientes: matriz 2×2. Con `command=jdbc` el comportamiento es **idéntico al actual**; con ambos `jdbc` se devuelve el `AsistenciaRepositorySqlServerAdapter` sin envoltorio (regresión cero en el default).

| query | command | `AsistenciaRepositoryPort` compuesto |
|---|---|---|
| jdbc | jdbc | `AsistenciaRepositorySqlServerAdapter` (sin cambios) |
| jpa | jdbc | híbrido: query JPA; commands JDBC (**estado actual de local/dev**) |
| jdbc | jpa | híbrido: query JDBC; `registrarAsistenciasSesion` JPA; demás commands JDBC |
| jpa | jpa | híbrido: query JPA; `registrarAsistenciasSesion` JPA; demás commands JDBC |

- Sigue existiendo **un único** `AsistenciaRepositoryPort`; una invocación usa un solo provider; **sin dual-write ni shadow write**. `profile != provider`: no se añade la propiedad a `application-local.yml`/`application-dev.yml` en este piloto; el default del command permanece `jdbc` y cualquier activación por perfil es una fase posterior autorizada aparte.
- Implementación prevista sin tocar el baseline: `AsistenciaRepositoryHybridSqlServerAdapter` gana un **constructor adicional** (3 argumentos: baseline JDBC, `AsistenciaQueryPersistence`, `AsistenciaCommandPersistence`); el constructor de 2 argumentos actual se conserva (delegando el command al baseline) para que sus tests existentes no cambien. El JDBC adapter no se modifica.

### Alcance del nombre (riesgo R-13) — RESUELTO: se conserva el nombre; alcance = solo `registrarAsistenciasSesion` durante LB-002.2

`asistencia-command-provider` (nombre pedido para el TARGET) sugiere "todos los commands de asistencia", pero `jpa` migrará **solo** `registrarAsistenciasSesion`; `registrarAsistencia` (no soportado), `registrarAsistenciaAutonoma`, `solicitarRevisionAsistencia` y `resolverSolicitudRevisionAsistencia` siguen en JDBC.

- **Recomendado:** conservar el nombre, documentar el alcance en YAML/Javadoc/`adapter-composition-standard` y **probarlo** (CMD-CFG-007: con `command=jpa`, los otros 4 commands no tocan el EMF).
- Alternativa: nombre acotado (`asistencia-registro-lote-provider`); más honesto, pero se aparta del vocabulario solicitado. Cambiar el nombre no altera el resto del diseño.

## D3. Estrategia del `EntityManagerFactory`

### Problema AS-IS (verificado)

[`SqlServerJpaAsistenciaQueryAdapterConfiguration`](../../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/adapters/persistence/sqlserver/SqlServerJpaAsistenciaQueryAdapterConfiguration.java) tiene `@ConditionalOnProperty(asistencia-query-provider=jpa)`. Con `query=jdbc, command=jpa` el bean `entityManagerFactory` **no existe** y `ObjectProvider.getObject()` fallaría al arrancar (o, peor, el command quedaría atado a activar la query JPA). Además `@ConditionalOnProperty` compara el texto crudo, mientras el parser del selector acepta `"  JPA "`: hoy un valor con espacios pasa el parser pero no crea el EMF (fallo de arranque poco claro); con dos selectors el desajuste se duplicaría.

### Alternativas

| Alt. | Descripción | Veredicto |
|---|---|---|
| A | Segundo EMF dedicado al command | Rechazada: dos bootstraps de Hibernate, ambigüedad de `EntityManagerFactory` (`ObjectProvider.getObject()` con 2 beans falla), más memoria/arranque, sin beneficio (el command no usa entidades). |
| **B** | **El mismo EMF existente, con condición `query=jpa OR command=jpa`** implementada como `Condition` propia que reutiliza el parseo de los selectors | **Recomendada.** Un EMF, una regla de parseo, default (`jdbc`,`jdbc`) sin Hibernate (igual que hoy), cambios mínimos. |
| C | Rehabilitar `HibernateJpaAutoConfiguration` de Boot | Rechazada: reintroduce `JpaTransactionManager` (reemplazaría al `JdbcTransactionManager`), repositorios y métricas Hibernate; revierte LB-002.1 D2. |
| D | Módulo/`shared:jpa` común | Rechazada: [JDBC_TO_JPA §5](../../../persistence/JDBC_TO_JPA.md) pide dos consumidores reales de una abstracción; aquí se comparte un solo bean de configuración, no una abstracción. |

### Decisión propuesta (B)

- Mantener la clase `SqlServerJpaAsistenciaQueryAdapterConfiguration` (sin renombrarla en el piloto para no tocar sus tests; se actualiza su Javadoc para declarar que es una capability JPA compartida por query **y** command) y sustituir su `@ConditionalOnProperty` por `@Conditional(JpaCapabilityRequiredCondition.class)` (NUEVA).
- La condición devuelve `true` si cualquiera de los dos selectors parsea a `JPA`; propaga `IllegalStateException` si alguno es inválido (fail-closed, mensaje único con el nombre de la propiedad).
- Mismos `HIBERNATE_SAFE_PROPERTIES` (`hbm2ddl=none`, `show_sql=false`, validation `none`), mismo `DataSource`, mismos 3 tipos gestionados (vistas de solo lectura; el command no necesita entidades). Sigue **sin** `JpaTransactionManager`.
- El nombre de la unidad de persistencia (`asistencia-query`) puede mantenerse; renombrarlo es cosmético y queda fuera del piloto.
- Invariantes a probar: `(jdbc,jdbc)` → 0 EMF; cualquier `jpa` → exactamente 1 EMF; valor inválido en cualquiera → falla el arranque; sigue existiendo solo `JdbcTransactionManager`.

## D4. Frontera transaccional

**TARGET a validar — no hecho comprobado: Hibernate 7.2 soportaría la condición solo si el probe U-01 con SQL Server real lo demuestra.** **Sin transacción JPA** (sin `@Transactional`, sin `EntityManager.getTransaction()`, sin `JpaTransactionManager`): el baseline corre en autocommit y el SP gestiona su propia transacción (los ITs actuales lo declaran). Envolverlo en una transacción externa anidaría el `BEGIN TRAN` del SP y podría cambiar `@@TRANCOUNT`, la semántica de rollback y `XACT_ABORT`. **El cuerpo del SP no puede inspeccionarse en esta microfase** (el repositorio DB queda fuera de esta fase: no se abre ni se consulta), por lo que la única postura defendible es reproducir la condición del baseline y demostrar la equivalencia con los tests de atomicidad. Incógnita **U-01**: que Hibernate 7.2 permita ejecutar `StoredProcedureQuery` sin transacción activa; si exige transacción y eso altera la semántica del SP → `STOP: NO_CLEAN_JPA_PATH`.

Un `EntityManager` por invocación, cerrado en `try-with-resources`, para que el `Connection` vuelva al pool aun en fallo (la conexión sale del mismo `DataSource`; no hay adquisición anidada en el mismo hilo).

## D5. Traducción de resultados y errores

Pipeline previsto (espejo exacto del baseline, sin duplicar reglas de negocio):

1. `dto == null` → `CrosscuttingException("El dominio para registrar asistencias por sesion es obligatorio.")` **antes** de abrir el EM (mismo texto).
2. `idCorrelacion = CorrelationIdContext.require()` (misma excepción si falta).
3. Serializar registros a `[{"idEstudiante":"<UUID>","estado":"AN|SJC|EX"}]`. El baseline usa `Map.of`, cuyo **orden de claves no es determinista entre JVM**: el contrato exige el *shape*, no el orden de claves, así que la paridad se define sobre **JSON parseado**, y el candidato emite orden fijo (`idEstudiante`, `estado`). El estado se envía tal cual llega (`dto.registros()[i].estado()`); el candidato **no** normaliza ni valida (esa es responsabilidad de Application y del SP).
4. `StoredProcedureQuery` sobre `dbo.usp_registrar_asistencias_sesion`, 4 parámetros `IN` con nombre (`idSesion`, `asistenciaJSON`, `idCorrelacion`, `idUsuarioEjecutor`) y tipos `UUID`/`String`/`UUID`/`UUID`; `idUsuarioEjecutor` se envía **tal cual** aunque sea `null` (la DB responde `GEN_002`). **CORREGIDO por la decisión vinculante U-03 (2.2B → 2.2C):** binding **POSICIONAL** obligatorio — `1 = idSesion (UUID)`, `2 = asistenciaJSON (String, nvarchar(max))`, `3 = idCorrelacion (UUID)`, `4 = idUsuarioEjecutor (UUID)` con `registerStoredProcedureParameter(1..4, …)` y `setParameter(1..4, …)`. La evidencia SQL Server real demostró que Hibernate emite `{call …(?,?,?,?)}` y que el orden de registro —no el nombre— gobierna el binding; un binding nominal daría una falsa sensación de protección (3 de 4 son UUID). El orden proviene de `sys.parameters.parameter_id`, se congela en código y se protege con CMD-PAR-013; no se consulta metadata DB en runtime. (Texto original descartado: "preferencia por binding con nombre + fallback posicional".)
5. Avanzar hasta el primer result set (tolerando update counts previos, **U-02**) y mapear filas `Object[]` → `CanonicalProcedureResult` con `JdbcValueMapper` (tolera `UUID`/`String`/`Boolean`/`Number`).
6. **Nueva** clase `CanonicalProcedureResultValidator` (Infrastructure, `support.procedure`) aplica lo que hoy hace el executor tras la consulta: cardinalidad exacta 1 (`ERR_DB_CANONICAL_CONTRACT`), `idCorrelacion` igual al del contexto (`ERR_DB_CANONICAL_CONTRACT`) y `DbExceptionTranslator.throwIfFailed(...)` (público) → `ForbiddenException`/`ResourceNotFoundException`/`ConflictException`/`ValidationException`/`DatabaseOperationException`. `DbFailureClassifier` (package-private) sigue siendo la **única** fuente de clasificación.
7. **(Aclarado en la revisión)** La ausencia de result set canónico (0 filas o ningún result set) se traduce a `ERR_DB_CANONICAL_CONTRACT` (baseline), aun si Hibernate la señala con una `IllegalStateException`/`NoResultException`-like de navegación; solo fallos técnicos verdaderos (`PersistenceException`, error de conexión/driver, binding) → `DATABASE_OPERATION_ERROR`. El algoritmo debe distinguirlos por **estado observado** (`hasMoreResults`/`getUpdateCount`), no por el tipo de excepción. Texto original de la propuesta: `PersistenceException | IllegalStateException | IllegalArgumentException` → log `SQL operation failed. operation=registrarAsistenciasSesion, correlationId=…` (sin payload) y `DatabaseOperationException(DATABASE_OPERATION_ERROR, "No fue posible ejecutar el procedimiento almacenado.", causa)`, mismo texto y código que el baseline.

**Por qué un validador nuevo y no tocar el executor:** el baseline debe permanecer intacto durante el piloto ([PLAN §Invariantes](PLAN.md#invariantes)). El costo es ~15 líneas de lógica espejo; se mitiga con tests unitarios del validador y paridad real. Alternativa (refactor del executor para delegar en el validador) queda diferida al paso de retiro JDBC, no al piloto.

## D6. Rollback

`APP_ADAPTERS_PERSISTENCE_ASISTENCIA_COMMAND_PROVIDER=jdbc` (o ausencia de la variable) y reinicio de la instancia (el selector se evalúa al construir el contexto; no hay conmutación en caliente). No requiere rollback de DB, datos, API ni frontend porque el SP y el contrato son los mismos y no existe dual-write. El adapter JDBC, `CanonicalStoredProcedureExecutor` y sus tests permanecen. Rollback de código: revertir los commits de la fase.

## LB-002.2D — Resultado real (paridad SQL Server real)

Confirmado con evidencia real contra `gestionasistenciadb` (freeze DB desplegado), no solo el TARGET
de D4:

- **D4 (frontera transaccional):** confirmado. `AsistenciaCommandTransactionBoundaryIT` (pool=1)
  demuestra `@@TRANCOUNT=0` y `autocommit=true` tras éxito y tras rechazo, y visibilidad inmediata
  desde una conexión independiente. Sin `JpaTransactionManager`, sin transacción JPA exterior.
- **U-01 (transacción):** confirmado con el candidato real (no solo el probe de 2.2B).
- **U-02 (result set tras update counts):** confirmado (`CMD-PAR-001`).
- **U-03 (binding posicional, no por nombre):** confirmado por `CMD-PAR-013` contra `sys.parameters`
  real (orden 1..4 exacto) usando el candidato productivo real, no solo el probe.
- **U-04 (null en `uniqueidentifier`):** confirmado. `usuarioEjecutor=null` → `ValidationException`
  idéntica en ambos providers. `idSesion=null` → ambos providers lanzan la MISMA
  `DatabaseOperationException` técnica (no de negocio); el baseline JDBC comparte esa clasificación,
  por lo que no es un defecto del candidato (ver LB-002.2D-VALIDATION).
- **U-05 (JSON > 4000):** confirmado, sin truncamiento, mismo rechazo funcional en ambos providers.

Detalle completo por escenario en [PARITY_SNAPSHOT](PARITY_SNAPSHOT.md) y
[LB-002.2D-VALIDATION](LB-002.2D-VALIDATION.md). `PARITY_MISMATCHES = 0`. Sin correcciones de
producción durante 2.2D.
