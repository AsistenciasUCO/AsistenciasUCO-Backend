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
- Código certificado: `8ac519ae` (`test(v2): restore Sonar new-code coverage gate`)
- Arnés E2E: `5ab8e8f` (`test(e2e): report connection failures safely`)
- El commit documental posterior será el SHA que GitHub evalúe para el cierre de CI.

## Resultado local

- `clean verify`: 1.534/1.534, 0 fallos, 0 errores, 0 skips.
- `-Pintegration clean verify`: 198/198, 0 fallos, 0 errores, 0 skips.
- JaCoCo: 92,78 % líneas y 80,96 % ramas.
- ArchUnit y contratos OpenAPI: verdes localmente.
- E2E JWT real: 28/28 en cada JVM UTC, America/Bogota y Europe/Berlin; 84/84 total.
- SQL: solo `sql_server_asistencias` / `gestionasistenciadb`; residuo final `E2E-sesiones = 0`.

## Reconciliación aplicada

Se combinaron manualmente OpenAPI y su hash, `HttpUtcInstantCodec`, la paridad JPA de vistas, el conformance test y la validación histórica. `SesionUtcActivationGuardTest` se fusionó automáticamente. Se conservaron paginación PR #18, caché PR #19, correcciones Sonar PR #20, documentación PR #21 y UTC v2. No se eliminaron pruebas ni se aplicaron resoluciones masivas `ours`/`theirs`.

El arreglo final `JacksonTemporalConfiguration` vive en Infrastructure y fija el wire ISO de `LocalDateTime` para MVC; no cambia Domain/Application, el esquema DB ni la compatibilidad de sesiones v1. `8ac519ae` recupera la cobertura nueva de Sonar con pruebas de comportamiento y mantiene la decisión CSRF stateless auditada; `5ab8e8f` hace explícito el error real del arnés cuando no existe respuesta HTTP.

## DB y riesgos restantes

La compatibilidad técnica del contrato DB del PR #36 fue comprobada contra la base local; su merge a `develop` sigue pendiente y pertenece al owner DB. PR #22 no modifica esquema, procedimientos ni permisos.

Los checks remotos quedaron verdes sobre `a991c6e5b7c1cda25f66a2046902971144f3744f`: Backend CI/Quality Gate `38133949718`, Backend Security `38133949740` y Backend Deep Security Scan `38133949769`, todos con `success`; SonarCloud, CodeQL, Dependency Review, Trivy y ArchUnit están incluidos. PR #22 queda listo para revisión final, abierto y sin merge a `develop`.

Fuera de alcance: frontend, despliegue productivo, merge a `develop`, borrado de ramas y ampliación de funcionalidades específicas de Keycloak.
