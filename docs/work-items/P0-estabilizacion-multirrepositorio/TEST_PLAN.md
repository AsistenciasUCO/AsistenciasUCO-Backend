# TEST PLAN — P0: Sincronización multirrepositorio y auditoría arquitectónica

## Alcance de prueba

P0 valida evidencia y límites de ejecución; no cambia comportamiento de producción. El resultado de cada comprobación se clasifica como `PASS`, `FAIL`, `NOT_RUN` o `BLOCKED_BY_MISSING_EVIDENCE`.

## REQUISITO → EVIDENCIA

| ID | Requisito observable | Comprobación | Estado inicial |
|---|---|---|---|
| P0-INV-001 | Los cuatro ámbitos tienen snapshot/remoto/branch/commit o ausencia explícita | Git local + API de repositorios/PRs; no leer secretos | En curso |
| P0-ARCH-001 | Coordinador conserva Clean Architecture y composition root explícito | `rg` de paquetes, imports, wiring y ArchUnit existente | En curso |
| P0-CON-001 | Rutas evaluadas existen en OpenAPI/HTTP AS-IS o quedan clasificadas como no contractuales | Comparación con OpenAPI canónico y `HTTP_AS_IS_MATRIX` | En curso |
| P0-SEC-001 | No hay PEP/selector/filtro duplicado inventado por la auditoría | `pom.xml`, `src/main`, `src/test`, configuración de seguridad y CI | En curso |
| P0-PER-001 | Persistencia coincide con ADR/JPA-07 y contratos DB observados | repositories, `EntityManager`, contratos SP y evidencia LB-008 | En curso |
| P0-GATE-001 | El gate local tiene comando, fecha, salida y exit code | `mvnw.cmd -B -ntp clean verify`; no inferir PASS | PASS backend; ver `VALIDATION.md` |
| P0-EXT-001 | Integraciones obligatorias no ejecutadas se declaran `NOT_RUN` | SQL Server/Keycloak/Azure/MinIO/fixtures | NOT_RUN/BLOCKED; ver `VALIDATION.md` |

## RED_SNAPSHOT

`NOT_APPLICABLE`: el work item está limitado a `CONTRACT_ANALYSIS` y documentación. No se modifica código ni se crea un test RED de comportamiento. Todo cambio posterior requiere un work item hijo con snapshot de tests aprobado antes de implementación.

## Secuencia de validación

1. Capturar estado Git y snapshots sin exponer secretos.
2. Comparar contratos canónicos con consumidores/proveedores reales observables.
3. Ejecutar validaciones estáticas y el gate backend autorizado.
4. Repetir solo los comandos cuyo entorno esté disponible; registrar skips y limitaciones.
5. Emitir hallazgos y decisión de DoR/`READY_FOR_P1_MINIO` sin convertir `NOT_RUN` en `PASS`.

## Resultado — 2026-10-18

La suite backend y los gates de arquitectura/OpenAPI/JaCoCo pasan fuera del sandbox. La validación frontend oficial conserva el estado versionado de MAINT-004: builds PASS, Karma/ChromeHeadless bloqueado y `verify` no certificable. No se ejecutó integración contra SQL Server, MinIO, Keycloak, Azure ni fixtures E2E del P0.

## Addendum de ejecución — 2026-10-20

La copia F1 preservada del frontend ejecutó `npm run test:ci -- --no-progress` con Chrome Headless 155: `807/807 SUCCESS`, exit code `0`. La salida reportó statements `87.76%`, branches `70.68%` y lines `88.99%`. También pasaron los builds development y production. Este resultado es específico de esa copia y no convierte en PASS la integración con backend/DB ni la copia OneDrive modificada.

El worktree backend limpio de `origin/develop` (`5c06087d`) ejecutó `.\mvnw.cmd -B -ntp verify` con Java 25: `1581` tests, 0 fallos, 0 errores, 0 skips, JaCoCo line `90.85%` y branch `79.97%`, exit code `0`, `BUILD SUCCESS`. El resultado aplica a ese snapshot limpio, no al checkout raíz que conserva cambios locales.

El inventario de remotos, worktrees, PRs y límites de integración está en `CONSOLIDATION_AUDIT_2026-10-20.md`. SQL Server, Docker, MinIO, ClamAV, Keycloak, Azure y E2E reales permanecen `NOT_RUN` o `BLOCKED_BY_ENVIRONMENT`; no se convierten en PASS por evidencia de build/frontend.

## Criterio de cierre

P0 no puede cerrarse como funcional si falta un provider obligatorio, un contrato está en conflicto, el gate aplicable es rojo, la evidencia está incompleta o la integración externa requerida quedó `NOT_RUN`. Puede cerrarse como auditoría documental con bloqueos explícitos y micro-work-items trazables.
