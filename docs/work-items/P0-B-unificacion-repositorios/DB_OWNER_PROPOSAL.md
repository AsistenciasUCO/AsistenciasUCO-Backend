# Propuesta para owner DB — ownership de asignaturas por programa

Fecha de evidencia: 2026-10-18  
Work item: P0-B — Unificación de repositorios, sincronización automatizada y cierre

## Resultado comprobado en backend

El caso de uso `GestionarAsignaturaUseCaseImpl` resuelve el programa del coordinador autenticado mediante `InstitutionalScopePort` y aplica la pertenencia antes de delegar la mutación:

- `crear`: rechaza un plan de otro programa y no invoca `AsignaturaCommandPort`.
- `actualizar`: rechaza una asignatura cuyo programa no coincide y no invoca `AsignaturaCommandPort`.
- `toggleEstado`: rechaza una asignatura fuera del programa y no invoca `AsignaturaCommandPort`.

La prueba automatizada `GestionarAsignaturaUseCaseImplTest` cubre esos escenarios. Validación ejecutada:

```text
.\mvnw.cmd '-Dtest=GestionarAsignaturaUseCaseImplTest,AsignaturaJpaCommandPatternTest' test
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Esto certifica el comportamiento del caso de uso y el patrón JPA simulado; no certifica el permiso en SQL Server real.

## Riesgo SQL observado, separado del código de aplicación

La inspección de los contratos versionados en `AsistenciasUCO/gestion-asistencia-db` muestra:

| Procedimiento | Evidencia estática | Riesgo |
|---|---|---|
| `dbo.usp_crear_asignatura` | Recibe `@idUsuarioEjecutor` y valida perfil `COORDINADOR`, pero el contrato visible no valida que el ejecutor sea dueño del programa del `@idPlanEstudio`. | Un coordinador válido podría intentar crear en un plan de otro programa si el procedimiento se invoca fuera del caso de uso. |
| `dbo.usp_actualizar_asignatura` | No recibe `@idUsuarioEjecutor`; comprueba existencia y actualiza por `@idAsignatura`. | La base no puede revalidar ownership del actor en la frontera pública actual. |
| `dbo.usp_toggle_estado_asignatura` | No recibe `@idUsuarioEjecutor`; cambia estado por `@idAsignatura`. | La base no puede revalidar ownership del actor en la frontera pública actual. |

La evidencia no implica que exista una explotación ejecutada: la prueba controlada contra SQL Server quedó `NOT_RUN` porque el puerto local 1433 no estaba escuchando y Docker no pudo consultarse por `Access is denied`. No se alteraron datos, contenedores ni procedimientos.

## Propuesta contractual para el owner DB

Abrir un work item de base de datos antes de tocar el esquema o los procedimientos:

1. Congelar una matriz de ownership para `programa → planEstudio → semestrePlanEstudio → asignatura`.
2. Decidir si los tres procedimientos públicos deben recibir `@idUsuarioEjecutor` (o un parámetro equivalente) y conservar el envelope canónico de cuatro columnas.
3. Validar en SQL que el actor tenga perfil `COORDINADOR` y que su programa coincida con el programa derivado del plan/asignatura objetivo.
4. Definir el código/mensaje catalogado para rechazo de ownership y su traducción REST, sin inventar un código durante P0-B.
5. Añadir a `test_suite.sql` un escenario con dos programas y dos coordinadores que pruebe crear, actualizar y toggle cruzados, incluyendo invariantes de rollback y ausencia de cambios.
6. Actualizar firmas, contratos, backend adapters y pruebas coordinadamente mediante Expand & Contract. El backend no debe cambiar su contrato hasta que el owner DB publique la decisión y el contrato versionado.

Estado de esta propuesta: `PROPOSAL_ONLY`; no autoriza cambios SQL ni del contrato público.
