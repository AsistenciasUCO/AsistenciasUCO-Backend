# PR #21 — reconciliación contra develop (2026-10-10)

## Identidad y alcance
- Branch PR: `codex/fase1-backend-validation` (padre histórico `27e6ed00f8c05eb348d9c547c291a9647ddc6b5f`).
- Integración base: `develop` en `535ba38f597d9eaa1185fadee2d08dc700002679` (PR #18, #19 y #20 fusionados).
- Objetivo: resolver diferencias documentales y de código, sin degradar el códec UTC, la paginación JPA ni la caché JPA. No se hace merge del PR #21 a develop en esta resolución.

## Resolución de conflictos y conservación
1. `HttpUtcInstantCodec.java` y `HttpUtcInstantCodecTest.java`: conservar la versión más reciente de `develop` procedente del PR #20, incluidas las simplificaciones de Sonar y el test de precisión. No restaurar la variante obsoleta del PR #21.
2. `EstudianteJpaRepository`, validación y contratos OpenAPI: mantener las correcciones de PR #18 presentes en `develop`.
3. `ParameterCatalogJpaRepository` y su prueba: mantener `ParameterKey` estructural procedente de PR #19.
4. `SKILL.md`, `PLAN.md`, `TEST_PLAN.md`, `PROMPT_CODEX_BACKEND.md` y guardas v1 del PR #20: mantener la versión más reciente de `develop`, sin retirar tests ni reducir alcance.
5. `BACKEND_TO_FRONTEND_HANDOFF.md`: conservar el contenido que ya estaba en develop y agregar las referencias del PR #21 a la evidencia y decisiones de 2026-10-09.
6. `VALIDATION.md`: conservar íntegro el documento de develop y anexar las dos pasadas de validación del PR #21. La evidencia 2026-10-09 no se presenta como una prueba recién repetida.
7. Cuatro documentos de diseño/evidencia exclusivos del PR #21: conservar íntegros y etiquetar como instantáneas históricas, porque los planes de UTC-D06 evolucionaron después.

## Estado del entregable
- PR #18: merge `38184c833c8d90cb5efe2e383cae800b5d015a4e`.
- PR #19: merge `b68d76425733d7adc941120bcccb96c0d4fc1e3c`.
- PR #20: merge `535ba38f597d9eaa1185fadee2d08dc700002679`.
- MAINT-003K backend (`6814e87...`) y DB (`d66f223...`): fuera del PR #21; no asumidos integrados ni certificados en CI remoto.
- Ningún archivo de producción, test, esquema SQL o proveedor de identidad se elimina o altera por esta resolución.

## Revalidación requerida después del commit
- GitHub Actions: Backend CI (Java 25, Surefire, JaCoCo, SonarCloud), Backend Security (CodeQL, Dependency Review) y Backend Deep Security Scan (ArchUnit, Trivy repositorio/imagen). Informar los resultados del nuevo SHA, sin heredar PASS de otros commits.
- Tests de SQL Server con `-Pintegration`: no ejecutados automáticamente por esta resolución documental; solicitar ejecución real contra un SQL Server controlado y reportar skips.
- `git diff --check` y revisión de comparación vs develop para verificar sólo cambios documentales.

## Nota de vigencia
Los archivos de 2026-10-09 incluidos en este PR conservan su valor de trazabilidad, pero NO son el acta final de readiness del backend UTC v2. Para release debe prevalecer la evidencia posterior MAINT-003K y la validación de su PR/DB.
