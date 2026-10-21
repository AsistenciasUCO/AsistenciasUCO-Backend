---
status: active
type: work-item
scope: backend-and-local-verification
owner: backend-team
last-reviewed: 2026-10-20
---

# P0-C — Estabilización técnica y cierre operativo

## Objetivo

Restaurar gates reproducibles de verificación local para el backend Java 25 y
el frontend Angular, y ejecutar los smoke checks reales disponibles. No se
cambia el contrato HTTP/OpenAPI, SQL, roles, PEP ni se fusionan ramas
compartidas.

## Clase y variable principal

- Change class: `REFACTOR`.
- `PRIMARY_VARIABLE`: `LOCAL_VERIFICATION_STABILITY`.

## AS-IS comprobado

- Backend: `mvnw.cmd verify` llegó a compilar Java 25, pero Surefire no produjo
  resumen y dejó `target/surefire-reports/*dumpstream` por un error de canal
  attach de JVM tras el auto-attach de Mockito.
- Frontend: los bundles compilan y la cobertura realtime pasa; ChromeHeadlessCI
  no capturó el navegador en 2 reintentos de 60 segundos.
- Logging P0-B: helper aislado PASS, pero aún falta arranque real.
- SQL Server/Keycloak/Docker deben comprobarse antes de ejecutar integración o
  E2E; no se inventan credenciales ni se cargan secretos en evidencia.

## TARGET y criterios de aceptación

1. Maven identifica Java 25, ejecuta Surefire sin auto-attach no controlado,
   conserva JaCoCo y termina con exit code verificable.
2. Karma usa un launcher ChromeHeadless reproducible, con perfil/puertos
   efímeros y sin desactivar tests.
3. `npm run verify` registra cada subcomando real.
4. Si los servicios locales están disponibles, Spring Boot y Angular arrancan,
   se comprueba autenticación/roles/sesión y se detienen limpiamente.
5. Las rutas candidatas a retiro quedan clasificadas con tamaño, refs y
   exclusividades; no se borran sin autorización explícita.

## Rutas permitidas

- `pom.xml`, `.mvn/**` y `scripts/**` solo para estabilización de ejecución.
- `docs/work-items/P0-C-estabilizacion-cierre/**`.
- En la copia frontend, únicamente `karma.conf.js`, scripts de verificación y
  `docs/work-items/P0-C-chromeheadless/**` si la evidencia justifica el cambio.

## Rutas prohibidas

`src/main/**`, SQL, contratos OpenAPI, `.env`/secretos, datos persistentes,
Keycloak compartido, merges, force-push, ramas remotas borradas y eliminación
de carpetas/worktrees.

## Dependencias y rollback

- Maven/Surefire/Mockito/JaCoCo y Chrome/Angular/Karma son dependencias de
  tooling; el provider SQL/Keycloak/MinIO sigue siendo externo.
- Rollback: revertir solo commits P0-C; conservar los commits P0-B y los
  cambios locales preexistentes.
- Stop conditions: fallo contractual, test RED ambiguo, proceso huérfano,
  servicio externo no controlado o cualquier necesidad de descartar estado.

## Definition of Ready

`READY_WITH_SCOPED_EXTERNAL_BLOCKERS` para tooling local y validaciones
independientes. `NOT_READY_FOR_FUNCTIONAL_CLOSURE` mientras el ambiente real no
permita ejecutar los recorridos obligatorios.
