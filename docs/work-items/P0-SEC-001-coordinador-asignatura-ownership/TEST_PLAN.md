# TEST PLAN — P0-SEC-001: Ownership de mutaciones de asignatura del Coordinador

## Requirement → contract → test

| ID | Requisito | Test previsto |
|---|---|---|
| SEC-001-01 | Actor no resuelto no puede mutar | Application test: `ForbiddenException`, zero interactions con command. |
| SEC-001-02 | Coordinador de otro programa no puede crear en plan ajeno | Application test con `PlanEstudioQueryPort` fuera de scope, zero command. |
| SEC-001-03 | Coordinador de otro programa no puede actualizar plan/asignatura ajena | Application test, `ForbiddenException`, zero command. |
| SEC-001-04 | Coordinador de otro programa no puede hacer toggle de asignatura ajena | Application test, `ForbiddenException`, zero command. |
| SEC-001-05 | Actor autenticado se propaga en update y toggle | Controller test captura DTO / segundo argumento; actor igual al resolver JWT. |
| SEC-001-06 | Mutación propia conserva el command y la ruta existente | Application/controller tests, command invocado una vez con argumentos actuales. |
| SEC-001-07 | La composición entrega todos los puertos | Wiring test y `mvnw verify`; no se agregan beans manuales de repositorio. |

## RED_SNAPSHOT

Capturado el 2026-10-18 antes de modificar producción:

```text
.\mvnw.cmd -B -ntp -DskipTests test-compile
exit code: 1
```

El compilador reportó los contratos todavía ausentes: constructor de `GestionarAsignaturaUseCaseImpl` sin puertos de consulta/scope, `toggleEstado` sin actor, wiring incompleto y aserciones de controller incompatibles con la propagación del actor. Este snapshot no se obtuvo modificando los tests para acomodar la implementación.

## Integración

`-Pintegration verify` debe ejecutarse solo con SQL Server controlado y fixtures sanitizados. Si el provider real no está disponible, registrar `NOT_RUN`; nunca convertirlo en PASS. La prueba DB de no-escritura cross-program es obligatoria para cerrar el hardening completo, aunque la defensa Application pueda cerrarse por separado como mitigación.

## Resultado — 2026-10-18

La suite unitaria objetivo y el gate global quedaron GREEN; la integración SQL Server y la prueba de no-escritura cross-program permanecen `NOT_RUN`. Ver `VALIDATION.md` del work item.
