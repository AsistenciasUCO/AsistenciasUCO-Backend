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
- Commit publicado y evaluado por GitHub: `8d49a3fc2f638fa752f3a86bd062c9ffed6681a8`.
- Diagnóstico funcional: [BACKLOG_ADMINISTRATIVO.md](BACKLOG_ADMINISTRATIVO.md).

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

El PR #36 de `johnjduque/gestion-asistencia-db` está merged en `develop` con `e2486194d42c6c7a649882a6957273742aa26536`. La compatibilidad fue comprobada contra el contenedor existente `sql_server_asistencias` y la base `gestionasistenciadb`, sin reconstruir ni alterar la base oficial. PR #22 no modifica esquema, procedimientos ni permisos.

La auditoría reversible de `usp_crear_facultad` confirmó actualización por ID/nombre y el defecto de alta SQL 515 por columnas `NOT NULL`; el hallazgo y el backlog de admin/perfil están documentados en [BACKLOG_ADMINISTRATIVO.md](BACKLOG_ADMINISTRATIVO.md).

Los checks remotos del SHA final fueron verdes: Backend CI/Quality Gate `38177603461`, Backend Security `38177603437` y Backend Deep Security Scan `38177603442`; SonarCloud muestra 0 issues abiertos/confirmados, CodeQL y Dependency Review, Trivy y ArchUnit terminaron en `success`. PR #22 queda abierto y sin merge a `develop`.

Fuera de alcance: frontend, despliegue productivo, merge a `develop`, borrado de ramas, implementación de módulos administrativos/perfil y ampliación de funcionalidades específicas de Keycloak.
