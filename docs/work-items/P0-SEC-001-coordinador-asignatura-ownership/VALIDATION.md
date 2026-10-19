# VALIDATION — P0-SEC-001: Ownership de mutaciones de asignatura

Fecha de corte: 2026-10-18 (America/Bogota)

## Resultado

`APPLICATION_MITIGATION = GREEN`

`FUNCTIONAL_CLOSURE = NOT_CLOSED`

La defensa Application está implementada y validada. El cierre funcional completo permanece abierto porque el contrato SQL de update/toggle pertenece al owner DB y la integración SQL Server controlada no se ejecutó.

## RED → GREEN

### RED capturado antes de implementar

```text
.\mvnw.cmd -B -ntp -DskipTests test-compile
exit code: 1
```

El compilador evidenció las incompatibilidades esperadas: constructor sin `AsignaturaQueryPort`, `PlanEstudioQueryPort` e `InstitutionalScopePort`; `toggleEstado` sin actor; wiring incompleto y expectativas del controller sin propagación del actor.

### GREEN objetivo

```text
.\mvnw.cmd -B -ntp "-Djacoco.skip=true" "-DargLine=-javaagent:C:/Users/josev/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar" "-Dtest=GestionarAsignaturaUseCaseImplTest,CoordinadorPortalControllerTest,CoordinadorWiringConfigurationTest" test
exit code: 0
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

Casos cubiertos: actor ausente, plan ajeno, asignatura ajena en alta/update/toggle, actor propagado desde controller y composición completa de puertos.

## Gate global

```text
.\mvnw.cmd -B -ntp clean verify "-DargLine=@{argLine} -javaagent:C:/Users/josev/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar"
exit code: 0
BUILD SUCCESS
Tests run: 1544, Failures: 0, Errors: 0, Skipped: 0
JaCoCo LINE: 90.66%; BRANCH: 80.54%.
```

`git diff --check`: PASS.

## Integración y límite residual

`-Pintegration verify`: `NOT_RUN`, por falta de SQL Server controlado y fixtures sanitizados.

La DB oficial observada en `origin/develop` contiene `usp_crear_asignatura` con ejecutor y RBAC, pero los scripts públicos observados de `usp_actualizar_asignatura` y `usp_toggle_estado_asignatura` no reciben ejecutor ni exponen validación de titularidad. Este work item no cambia SQL; queda pendiente una decisión versionada del owner DB.

## Archivos de comportamiento modificados

- `src/main/java/.../gestionarasignatura/primaryports/GestionarAsignaturaInputPort.java`
- `src/main/java/.../gestionarasignatura/primaryports/interactor/GestionarAsignaturaInteractor.java`
- `src/main/java/.../gestionarasignatura/usecase/GestionarAsignaturaUseCase.java`
- `src/main/java/.../gestionarasignatura/usecase/impl/GestionarAsignaturaUseCaseImpl.java`
- `src/main/java/.../controller/coordinador/CoordinadorPortalController.java`
- `src/main/java/.../wiring/CoordinadorWiringConfiguration.java`
- tests equivalentes del caso de uso, controller y wiring.

No se modificaron OpenAPI, SQL, esquema DB, frontend, PEP, seguridad global, secretos ni rutas públicas.
