# TEST_PLAN — P0-G: recuperación funcional y centralización de asistencias

## Fuentes

`PLAN.md`, `AGENTS.md`, `P0-E/VALIDATION.md`, `P0-D`, `DB_BASELINE_CONTRACT.md`, `PROFESSOR_DECISION.md`, API/OpenAPI canónicos y los commits/ref listados en `CONTRACT_MATRIX.md`.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| P0G-T01 | Preservar historia | Refs candidato/oficial | Refs existentes | Resolver commits y comparar nombres | SHA y árbol disponibles | No se reescribe ni elimina historia | Mezclar líneas o perder trabajo local | Reconciliación |
| P0G-T02 | Backend sano | Suite actual | Java 25 y Maven wrapper | `mvnw verify` | Surefire/JaCoCo | 0 fail/error/skip y gates OK | Declarar sano con pruebas omitidas | Backend |
| P0G-T03 | Frontend trazable | Suite actual | Checkout actual intacto | `npm run test:ci -- --no-progress` | ChromeHeadless y total | Registrar resultado actual, sin atribuirlo al candidato | Confundir evidencia histórica con estado actual | Frontend |
| P0G-T04 | Contracto DB | W1/W2/R1/R2 | SQL Server local activo | Leer catálogo/metadata | SP, parámetros y columnas | W1/W2 4 columnas; R1/R2 clasificados como query | Forzar consultas a contrato de mutación | Integración DB |
| P0G-T05 | Providers reales | MinIO/ClamAV/Keycloak/SQL | Contenedores y puertos activos | Preflight/health | Puertos y estado | Disponibilidad observada sin imprimir secretos | Certificar con mocks o valores expuestos | Infra |
| P0G-T06 | Flujo completo | estudiante → reclamo → soporte → docente | Contrato/fixture/rollback aprobados | Ejecutar E2E mutante | HTTP, DB, storage y ownership | PASS solo con evidencia actual completa | Llamar PASS a una prueba parcial/histórica | E2E |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: `YES` para DB, storage, ClamAV, Keycloak y E2E; `NO` solo para pruebas unitarias ya existentes.
- ENVIRONMENT: stack local oficial; puertos 1433, 8081, 9000, 3310, 8080, 4200.
- MOCK_SUFFICIENT: `NO` para disponibilidad o contrato del provider.
- WHY: el objetivo es reproducibilidad funcional multi-repositorio y un mock no prueba el wiring, permisos, SQL ni la entrega real.

## Negative / Boundary Coverage

- Ownership docente/coordinador y respuestas 403.
- Archivo inexistente/ilegible, soporte privado y ClamAV no disponible.
- Paginación en 300/1000/5000 y grupos/sesiones existentes.
- Resultset W1/W2 incorrecto o R1/R2 interpretado como mutación.
- Checkout con modificaciones locales y ausencia de `infra/files/.env`.

## Side Effects / Rollback

No se ejecuta E2E mutante en esta fase. Si se autoriza, requiere IDs de fixture, limpieza/rollback, permisos documentados y separación de datos de usuario.

## Pruebas RED requeridas

`NO APLICA` a una implementación: este work item recupera evidencia y congela decisiones. La ausencia de P0-F se registra como evidencia faltante, no como fallo funcional inventado.

## RED_SNAPSHOT

`NO APLICA`: no se modifica código ni contrato en este work item.

## Congelación

La congelación queda condicionada a la decisión del owner DB sobre D1/ONE_TABLE, autoridad del remoto y clasificación de R1/R2.

## Integración y E2E

Los E2E históricos de P0-E y la evidencia 807/807 del frontend se conservan como históricos. El flujo completo de reclamos/soportes queda `NOT_RUN` en P0-G hasta aprobación y fixture.

## Falsos positivos que deben evitarse

- No usar una suite frontend actual para afirmar compatibilidad con el backend B1 candidato.
- No usar un mock de MinIO/ClamAV para certificar provider.
- No usar presencia de una SP para afirmar aprobación contractual.
- No convertir una consulta de 10/25 columnas en una mutación de 4 columnas por conveniencia.
