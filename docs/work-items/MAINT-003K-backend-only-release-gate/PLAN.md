---
status: BACKEND_LOCAL_GREEN_CI_PENDING
date: 2026-10-11
base_branch: origin/develop
scope: BACKEND_AND_LOCAL_DEV_SQL_CONSOLIDATION
---
# MAINT-003K — Cierre estricto del backend y consolidación SQL local de desarrollo (sin frontend)

## Alcance innegociable (ampliado el 2026-10-10 — ver [DB_DEV_ADDENDUM.md](DB_DEV_ADDENDUM.md))
Esta fase realiza **revisión, corrección y certificación del código de backend** y, por addendum, la **consolidación SQL local de desarrollo**: una sola base principal `gestionasistenciadb` en el contenedor `sql_server_asistencias`, desplegada solo desde scripts versionados del repositorio DB (`johnjduque/gestion-asistencia-db`, rama `feat/maint-003k-db-dev-consolidation`), con fixtures sintéticos y retiro de los temporales.
Sigue prohibido: frontend, Redis, cambios en MinIO, cambio de proveedor de identidad (se mantiene la validación JWT, roles institucionales, ownership y RBAC; no se refactoriza lo específico de Keycloak), merge a `develop`, borrado de ramas, despliegue a producción.
*(Redacción original, ya superada por el addendum: «No tocar … despliegue DB principal, limpieza de datos».)*

## Evidencia de partida
- `git fetch origin --prune` ejecutado; `origin/develop` estaba en `108937cdb2cd0ca1a35b7349fbd0696721b39499`.
- La reconciliación se publicó localmente mediante merge normal en `0114939d877f8a6a91b7118af98bf4f7c29c1796`; la corrección final de wire v1 quedó en `b4c6596a496b4a77ca74b24f6b0e5912506ae445`. No se usó `reset`, `ours`, `theirs`, force-push ni se eliminaron ramas.
- El merge conserva paginación del PR #18, caché del #19, correcciones Sonar del #20 y documentación del #21, además de UTC v2 y MAINT-003K.
- En SQL Server `datetime2(7)` no contiene offset. El mapper de consulta nunca debe aplicar `Date.toInstant()` ni `ZoneId.systemDefault()`; el registro v1 carece de procedencia, lo cual es distinto de que la consulta deba alterar sus campos.
- PR objetivo: [#22](https://github.com/alejovh12/AsistenciasUCO/pull/22), destino `develop`; no se hará merge desde esta tarea.

## Hipótesis de defecto prioritario (resultado)
En JVM Bogotá, `java.util.Date` + `toInstant().atZone(UTC)` convertía 14:00 SQL en 19:00. **Confirmado y certificado corregido** para `uv_sesion` (MAINT-003I) y **encontrado el mismo patrón, ya roto en runtime**, en dos lectores más (`SesionMateriaQueryRow`, `ReporteAsistenciaQueryRow`): corregidos en esta fase. V2 no estaba afectada.

## Gates de cierre
1. Inventario `git status`, HEAD, JDK, perfiles Maven, `app.sesiones.v2.enabled` y conector SQL real. No reset/clean destructivo.
2. Unit Maven `./mvnw -B -ntp clean verify` con JaCoCo ON; ArchUnit, OpenAPI checks y hashes; línea ≥80 %, ramas ≥70 %.
3. Integration Java `./mvnw -B -ntp -Pintegration clean verify` contra la DB D06 + CC-003G-01 (ahora la principal de desarrollo); 198 ITs, 0 failures/errors y 0 skips; sin tests desactivados.
4. Lectura v1 con 3 zonas JVM y 100 ns exactos; JDBC vs JPA; v2 GET Z confirmado/NULL indeterminado; POST/PATCH v2 y equivalencia Bogotá/Berlín.
5. Seguridad HTTP JWT real: 401 sin bearer, 403 rol incorrecto, 403 docente sin titularidad, docente dueño 2xx, actor solo de JWT, login SQL mínimo.
6. Flag `app.sesiones.v2.enabled`: OFF por defecto, con y sin token, fail-fast con DB incompatible.
7. Residuos UTC `Date/Timestamp` en otros lectores de Sesión: solo se corrigen bugs demostrados con test, conservando el wire v1.
8. Matriz OpenAPI: 10 operaciones v1 invariantes + 4 v2; 405 en PUT v2; checksum.
9. SonarCloud, CodeQL, Trivy/CI de la rama HEAD si hay PR/runner; si no, `NOT_RUN`.
10. `VALIDATION.md` y `TEST_PLAN.md` con SHA exacto, comandos, cifras reales, caveats y decisión.

## Criterio de éxito
`BACKEND_LOCAL_GREEN` requiere Maven clean verify e integration completos, regresiones de timezone, seguridad, JaCoCo y OpenAPI con resultados reales. `BACKEND_CI_GREEN` solo tras ejecutar CI sobre el SHA final con checks requeridos.

## Estados independientes (ejecución del 2026-10-11)

| Estado | Valor | Evidencia |
|---|---|---|
| `BACKEND_LOCAL_GREEN` | **PASS** sobre el código funcional `b4c6596a496b4a77ca74b24f6b0e5912506ae445` y su merge `0114939d877f8a6a91b7118af98bf4f7c29c1796` | [VALIDATION.md](VALIDATION.md) §2–§9 |
| `DEV_DB_CONSOLIDATED` | **PASS (local)** | repo DB: `docs/work-items/MAINT-003K-DB-DEV-CONSOLIDATION/VALIDATION.md` |
| `DB_PR_READY` | certificación técnica observada en `sql_server_asistencias/gestionasistenciadb`; merge del PR #36 sigue fuera de este PR | [DB_DEV_ADDENDUM.md](DB_DEV_ADDENDUM.md) y [VALIDATION.md](VALIDATION.md) §8 |
| `BACKEND_CI_GREEN` | **PENDING** | debe verificarse en PR #22 sobre el SHA publicado; no se presume por la ejecución local |

Gates 1–8 y 10 ejecutados con resultados reales. Gate 9 queda pendiente hasta que GitHub publique el resultado del último SHA del PR #22; SonarCloud, CodeQL, Dependency Review y Trivy no se declaran PASS por ejecución local. Defectos corregidos sin borrar pruebas ni relajar cobertura: [VALIDATION.md](VALIDATION.md) §6.
