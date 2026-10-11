# MAINT-003K — Handoff de reconciliación

Fecha: 2026-10-11

## Destino

- Repositorio: `alejovh12/AsistenciasUCO`
- Rama: `jose-valencia/maint-003k-backend-only-release-gate`
- PR: [#22](https://github.com/alejovh12/AsistenciasUCO/pull/22)
- Base: `develop`
- Merge a `develop`: **no realizado**

## SHAs

- `origin/develop` usado para reconciliar: `108937cdb2cd0ca1a35b7349fbd0696721b39499`
- Merge normal de reconciliación: `0114939d877f8a6a91b7118af98bf4f7c29c1796`
- Código funcional certificado: `b4c6596a496b4a77ca74b24f6b0e5912506ae445`
- El commit documental posterior debe ser el SHA que GitHub evalúe para el cierre de CI.

## Resultado local

- `clean verify`: 1.520/1.520, 0 fallos, 0 errores, 0 skips.
- `-Pintegration clean verify`: 198/198, 0 fallos, 0 errores, 0 skips.
- JaCoCo: 91,38 % líneas y 79,16 % ramas.
- ArchUnit y contratos OpenAPI: verdes localmente.
- E2E JWT real: 28/28 en cada JVM UTC, America/Bogota y Europe/Berlin; 84/84 total.
- SQL: solo `sql_server_asistencias` / `gestionasistenciadb`; residuo final `E2E-sesiones = 0`.

## Reconciliación aplicada

Se combinaron manualmente OpenAPI y su hash, `HttpUtcInstantCodec`, la paridad JPA de vistas, el conformance test y la validación histórica. `SesionUtcActivationGuardTest` se fusionó automáticamente. Se conservaron paginación PR #18, caché PR #19, correcciones Sonar PR #20, documentación PR #21 y UTC v2. No se eliminaron pruebas ni se aplicaron resoluciones masivas `ours`/`theirs`.

El arreglo final `JacksonTemporalConfiguration` vive en Infrastructure y fija el wire ISO de `LocalDateTime` para MVC; no cambia Domain/Application, el esquema DB ni la compatibilidad de sesiones v1.

## DB y riesgos restantes

La compatibilidad técnica del contrato DB del PR #36 fue comprobada contra la base local; su merge a `develop` sigue pendiente y pertenece al owner DB. PR #22 no modifica esquema, procedimientos ni permisos.

Los checks remotos Backend Quality Gate, SonarCloud, CodeQL, Dependency Review, Trivy y ArchUnit aún deben verificarse sobre el SHA final publicado. Hasta esa comprobación, `BACKEND_CI_GREEN` permanece `PENDING` y MAINT-003K no se declara cerrado.

Fuera de alcance: frontend, despliegue productivo, merge a `develop`, borrado de ramas y ampliación de funcionalidades específicas de Keycloak.
