# VALIDATION — P0: Sincronización multirrepositorio y auditoría arquitectónica

Fecha de corte: 2026-10-18 (America/Bogota)

## Dictamen

`AUDIT_STATUS = PARTIAL_WITH_BLOCKERS`

`READY_FOR_P1_MINIO = NO`

La línea base backend quedó reproducible y el hallazgo de seguridad de ownership de asignaturas tiene una mitigación Application validada. El P0 transversal no puede declararse listo para MinIO porque no existe evidencia ejecutada contra SQL Server/DB real, MinIO, fixtures E2E ni una suite funcional frontend GREEN. La defensa equivalente dentro de los providers SQL sigue pendiente del owner DB.

## Inventario y sincronización

| Ámbito | Snapshot observado | Remoto / estado | Acción realizada |
|---|---|---|---|
| Backend | `develop` / `72312d7c60449cd0118b8d32b8da02ad1eb0074d` | `origin/develop` en el mismo commit; `https://github.com/alejovh12/AsistenciasUCO.git` | `fetch --all --prune` y `merge --ff-only origin/develop`; se preservaron cambios locales. |
| Frontend | `jose-valencia/maint-004-frontend-utc-v2-integration` / `b4368d1e9aaf533db0fccf652bf33f282ee039a0` | tracking limpio; `https://github.com/shesho22/AsistenciasUCO-Frontend.git` | Solo lectura; no se escribió la copia bajo OneDrive. |
| DB | `develop` / `f2871a9564d6c4cc5abc3745854414243bfda238` | `origin/develop` = `e2486194d42c6c7a649882a6957273742aa26536`; local detrás 15, sin commits locales | Solo lectura; no se sincronizó ni se alteró la copia externa. |
| Infra | No hay repositorio separado localizado | El backend contiene `infra/`; no hay remoto independiente comprobado | No se inventó un repositorio ni se modificó infraestructura externa. |

El estado backend al cierre conserva como cambios preexistentes del usuario los scripts/ejemplos de Keycloak y los work items de paginación/E2E. No se descartaron, reescribieron, fusionaron ni publicaron esos cambios.

## Auditoría de arquitectura y contratos

- El flujo Coordinador conserva slices verticales, puertos neutrales y wiring explícito. Los tests ArchUnit del gate global pasan.
- La Application no importa infraestructura ni frameworks; la composición concreta queda en `CoordinadorWiringConfiguration`.
- La persistencia observada sigue JPA-only conforme a ADR-003/ADR-004 y al addendum vigente de LB-008: no se agregó JDBC directo, JPA starter nuevo ni SQL.
- El contrato HTTP/OpenAPI canónico no fue ampliado. No se inventaron paginación, filtros, docentes, grupos, periodos ni endpoints de matrícula que no estuvieran autorizados por las fuentes activas.
- No se encontró starter, selector o wiring PEP en `pom.xml`, `src/main` o `src/test`. PEP queda clasificado como TARGET/propuesta futura, no como AS-IS.
- El frontend contiene consumidores Coordinador, el transporte SSE autenticado y utilidades de zona horaria; esto es evidencia de implementación, no certificación de integración contra backend/DB reales.

## Hallazgo confirmado y mitigación

`SECURITY_FINDING — HIGH — coordinator subject mutation ownership`

Antes del cambio, el controller propagaba actor solo al alta; update enviaba `null` y toggle no llevaba actor. En la DB oficial, `usp_crear_asignatura` recibe ejecutor, mientras `usp_actualizar_asignatura` y `usp_toggle_estado_asignatura` exponen solo el identificador de asignatura/correlación y no muestran titularidad jerárquica en sus scripts públicos.

Mitigación aplicada en el work item hijo `P0-SEC-001-coordinador-asignatura-ownership`:

- update y toggle reciben el usuario resuelto del JWT;
- Application resuelve el programa institucional del actor;
- se valida pertenencia del plan y de la asignatura antes de invocar `AsignaturaCommandPort`;
- los casos cross-program y actor ausente fallan cerrado con `ForbiddenException` y cero llamadas al command;
- no cambian ruta, payload, código HTTP, OpenAPI ni SQL.

Esta mitigación no sustituye una defensa DB para callers distintos del backend. El owner DB debe abrir un work item contractual separado si requiere agregar ejecutor/titularidad a los providers.

## Validación ejecutada

### Backend

Comando final, fuera del sandbox por requerir loopback para fake Keycloak/ClamAV:

```text
.\mvnw.cmd -B -ntp clean verify "-DargLine=@{argLine} -javaagent:C:/Users/josev/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar"
exit code: 0
BUILD SUCCESS
Tests run: 1544, Failures: 0, Errors: 0, Skipped: 0
JaCoCo LINE: 90.66%; BRANCH: 80.54%; All coverage checks have been met.
```

La expansión `@{argLine}` fue necesaria para conservar simultáneamente el agente JaCoCo y el agente Mockito requerido por el runtime Java 25. Maven ejecutó con Java 25 y el proyecto exige `[25,26)`.

Validación objetivo del hijo:

```text
.\mvnw.cmd -B -ntp "-Djacoco.skip=true" "-DargLine=-javaagent:C:/Users/josev/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar" "-Dtest=GestionarAsignaturaUseCaseImplTest,CoordinadorPortalControllerTest,CoordinadorWiringConfigurationTest" test
exit code: 0
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

El RED previo quedó capturado en el `TEST_PLAN.md` del hijo: `test-compile` terminó con exit code 1 antes de implementar por las firmas de actor/puertos/wiring faltantes.

`git diff --check` terminó sin salida ni errores.

### Ejecuciones que no son PASS

- Un primer `clean verify` dentro del sandbox terminó con 1544 tests sin fallos pero 61 errores de entorno por permisos de loopback hacia fake Keycloak/ClamAV. Se clasifica `BLOCKED_BY_ENVIRONMENT`, no como fallo funcional.
- La repetición escalada fuera del sandbox fue la evidencia final PASS anterior.
- `-Pintegration verify` no se ejecutó: no hay evidencia de SQL Server controlado y fixtures autorizados en este workspace.
- No se ejecutaron MinIO, Azure real, Keycloak real ni los recorridos E2E 300/1000/5000.

### Frontend

La validación versionada en el repositorio oficial (`docs/work-items/MAINT-004-frontend-backend-db-alignment/VALIDATION.md`, HEAD `e2d4cdb7` al momento de ese registro) informa:

- build development y production: PASS;
- Karma/ChromeHeadless: bloqueado durante el arranque;
- `npm run verify`: no certificable porque se detuvo en `test:ci`;
- `coverage:realtime:check`: no PASS al no existir `lcov.info`;
- no hubo E2E real backend/DB.

En esta auditoría no se modificó ni se reejecutó la copia externa. Por las reglas de evidencia, el frontend queda `AMARILLO/BLOCKED`, no GREEN.

## Pendientes bloqueantes antes de P1

1. Owner DB: decidir y versionar la defensa de ownership para update/toggle, o aceptar formalmente la mitigación Application y su límite.
2. Sincronizar `gestion-asistencia-db` desde `origin/develop` en un checkout autorizado, después de revisar sus 15 commits pendientes.
3. Ejecutar `-Pintegration verify` contra SQL Server controlado con fixtures sanitizados y verificar no-escritura cross-program.
4. Repetir `npm run test:ci`, cobertura realtime y `npm run verify` en un host donde ChromeHeadless pueda iniciar.
5. Confirmar el repositorio Infra oficial o dejar la ausencia como decisión documentada del owner.
6. Ejecutar los smoke/E2E y la validación manual de contratos solo con servicios y datos controlados.

No se creó PR ni commit de publicación en esta etapa: el backend comparte un worktree con cambios locales del usuario y el P0 transversal aún tiene bloqueos de evidencia. No se hizo merge, force-push ni operación destructiva.
