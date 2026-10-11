---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-001 / PR #18 — validación

## Previo
PR #17 SEC-002 fusionado correctamente a `develop` en `bfc4fd3fee9fadefbb00fd6f4652ff1dc5423d69`.
PR #18 `f86cb74e1abf033850cf7550b2152d8023949d80` anterior a esta adenda: Backend CI, Backend Security y Backend Deep Security Scan **SUCCESS** (GitHub Actions). Los tests nuevos del presente cambio **no** están incluidos en esos runs.

## Alcance de esta revisión
Se contrastó con `arquisoft-backend-develop(1).zip` proporcionado por el profesor, los contratos actuales JPA y las vistas SQL físicas de `johnjduque/gestion-asistencia-db` (develop). Lectura estática; no se ejecutó SQL Server, Docker ni Java 25 aquí.

- Adaptador paginado ya disponible sobre vistas: PASS lectura de código.
- HTTP overflow guard: CODE_PREPARED / TEST_NOT_RUN.
- Nuevos tests unit HTTP validator (3) + SQL Server IT (1 con 3 páginas): TEST_SOURCE_PREPARED, NOT_RUN.
- SQL test con fixture de identidad >10 y multigrupo: PENDIENTE evidencia real.
- Sonar, JUnit, ArchUnit, CodeQL, Trivy para SHA nuevo: NOT_RUN hasta Actions.
- DB migrations: NONE; contrato físico existente intacto.
- Riesgo detectado: test IT anterior compara todo un conjunto con página de 100; si fixture >100 requiere actualizar oráculo del test de forma contractual, no limitar los datos de producción.

## Cierre
No declarar nuevos tests GREEN antes de GitHub Actions. No hacer merge automático. La fase funcional DB para soportes LB-004 permanece independiente.

## Ajuste adicional al oráculo SQL Server

En `CoreViewQueriesJpaParityIT.estudiantes_conservan_paginacion_detalle_contextos_y_not_found`, comparar los primeros 100 registros de la vista con la página `size=100`, conservando `assertEquals(before.size(), page.totalItems())`. El assert anterior comparaba erróneamente **todos** los registros con una única página y fallaría al superar 100 estudiantes. Se trata de un ajuste en la prueba que preserva la semántica, sin modificación de producción. La nueva prueba de páginas 0/1/2 cubre slicing por SQL Server real. Estado de nueva ejecución: NOT_RUN hasta nuevo run GitHub.

## 2026-10-09 — corrección P1: paridad no vacía y tres páginas reales

Revisión Codex detectó que `estudiantes_paginados_desde_vistas_sql_server_conservan_conteo_y_orden_en_tres_paginas` podía quedar verde con 0 filas y páginas vacías (oráculo SQL y JPA coincidían vacíamente). Se añadió **prerrequisito explícito >=11 identidades distintas** en la vista, tamaños esperados por página 0/1/2, recuento observado y no duplicación entre las tres páginas. Esto convierte la ausencia de fixture en fallo accionable, NO skip ni éxito falso. Requiere SQL Server aislado con al menos 11 estudiantes, uno multigrupo, antes de `-Pintegration`; no ejecutar sobre DB de producción, no añadir seeds a migraciones compartidas.

**Evidencia anterior suministrada por Codex**: clon `gestionasistenciadb_fase1` con 11 identidades, 12 contextos, una identidad en dos grupos; pruebas SQL PR18 192 IT / 0 errores / 0 skips, páginas 5/5/1 y 11 identidades únicas. La evidencia corresponde al SHA anterior `39e34c49` y no acredita todavía el commit P1. Ejecutar nueva suite en clon luego de revalidar fixture; mantener PR draft y sin merge hasta reportar nuevo SHA/GREEN.

## 2026-10-09 — certificación local del commit P1 `3b1ddb4f` y microfase OpenAPI

Entorno: Windows 11, JDK 25 (`C:\Program Files\Java\jdk-25`), SQL Server 2022 en contenedor local, clon aislado `gestionasistenciadb_fase1` (`APP_DATABASE_EXPECTED_NAME` apuntado al clon). Credenciales cargadas desde `.env` local sin imprimirse. Ningún skip se cuenta como PASS.

### Fixture del clon (solo lectura antes/después)

| Medida | Fuente `gestionasistenciadb` | Clon `gestionasistenciadb_fase1` |
|---|---:|---:|
| Identidades `uv_estudiante_identidad` | 3 | 11 (11 IDs distintos) |
| Contextos `uv_estudiante` | 3 | 12 |
| Estudiantes multigrupo | 0 | 1 (2 grupos) |
| Páginas size 5 (filas 0/1/2) | n/a | 5 / 5 / 1, unión distinta 11, 3 páginas |
| `CatalogoParametro` / `Sesion` | 24 / 3 | 24 / 3 |
| Residuos `IT-LB008-JPA06-*` tras IT | n/a | 0 / 0 / 0 |

### Ejecuciones Maven

| SHA | Comando | Resultado |
|---|---|---|
| `3b1ddb4f` | focales: `-Pintegration -Dit.test=CoreViewQueriesJpaParityIT,EstudianteAcademicFilterJpaParityIT -Dtest=ConsultarEstudiantesPaginationValidatorTest,EstudianteJpaQueryContractTest verify` | exit 0; unit 13/0/0/0; IT 10/0/0/0 |
| `3b1ddb4f` | control negativo: test de tres páginas contra la **fuente** (3 estudiantes) | exit 1 esperado: `requiere fixture aislada con >=11 estudiantes distintos; encontrados: 3` — ya no pasa en vacío |
| `3b1ddb4f` | `clean verify` | exit 0; 1452/0/0/0; JaCoCo unit LINE 90.28 % / BRANCH 80.50 % |
| `3b1ddb4f` | `-Pintegration verify` | exit 0; unit 1452/0/0/0; IT 192/0/0/0 (30 clases); JaCoCo combinado 92.53 % / 80.97 % |
| `d0468e3f` | `clean verify` | exit 0; 1458/0/0/0; JaCoCo unit 90.28 % / 80.50 % |
| `d0468e3f` | `-Pintegration verify` | exit 0; unit 1458/0/0/0; IT 192/0/0/0; JaCoCo combinado 92.53 % / 80.97 % |

JaCoCo calculado sumando `target/site/jacoco/jacoco.csv`; los gates del `pom.xml` (80/70) reportaron `All coverage checks have been met`.

### HTTP con JWT real (Keycloak local, backend jar `3b1ddb4f` + clon)

Usuarios E2E reales para los cuatro roles (`seed-e2e-users.ps1` ampliado en `7a5eceb`; idUsuario de `dbo.Usuario` real para COORDINADOR y ESTUDIANTE). El seed verificó `idUsuario`, `aud=asistencias-api` y el rol en el token emitido. Tokens nunca impresos.

| Caso | Esperado | Obtenido |
|---|---|---|
| anónimo / firma alterada | 401 | 401 `UNAUTHORIZED` / 401 |
| DOCENTE, ESTUDIANTE (lista), ESTUDIANTE (detalle) | 403 | 403 `FORBIDDEN` ×3 |
| ESTUDIANTE con `size=0` | 403 (autorización antes que validación) | 403 |
| COORDINADOR y ADMINISTRADOR defaults | 200, page 0, size 20, total 11 | 200 ×2 |
| COORDINADOR y ADMINISTRADOR page 0/1/2 size 5 | 5/5/1, total 11, 3 páginas, 11 IDs distintos | PASS ambos roles |
| page 3 (más allá del final) | 200, items vacío, total 11 | PASS ambos roles |
| COORDINADOR detalle del multigrupo | 200 | 200 |
| 9 filtros + combinado `grupoId+programaId+nombre` | total = oráculo SQL del clon | 11, 1, 8, 1, 11, 11, 11, 1, 11 y 1: PASS |
| `size=0,-1,101`, `page=-1`, `page=2147483647&size=100` | 400 `VALIDATION_ERROR` | PASS |
| `size=abc`, `page=abc`, `grupoId=not-a-uuid` | 400 `INVALID_REQUEST` | PASS |
| `correo` inválido, `nombre` con `<script>` o dígitos | 400 `VALIDATION_ERROR` | PASS |
| `size=1`, `size=100` | 200 | PASS |

Total: 42 casos PASS, 0 FAIL (más 4 filas informativas de token). Un primer intento usó `nombre=Estudiante01` en el filtro combinado y obtuvo 400: era entrada inválida del script (el validador rechaza dígitos por diseño); se corrigió el caso y se añadió como caso 400 explícito.

### Microfase contract-first OpenAPI (`GET /api/v1/estudiantes`)

1. `e7803ca` RED: `OpenApiEstudiantesDirectoryConformanceTest` (6) + mapeo congelado a 10 operaciones. Corrida: 17 tests, 7 fallos, todos por path/schema ausentes.
2. `1c96162` corrección del oráculo: Swagger Parser 3.1 inlina el `$ref` de `items`; el test acepta ref o forma idéntica a `EstudianteResumen`. Sin cambiar expectativas contractuales.
3. `d0468e3` contrato: path, nueve filtros opcionales, `page` (0-based, default 0, límite `page*size <= 2147483647`), `size` 1..100 default 20, `x-roles` COORDINADOR/ADMINISTRADOR, Bearer, `X-Correlation-Id`, 200/400/401/403/500 y esquemas `EstudiantePagina`/`EstudianteResumen` (NOT NULL según `sys.columns` de las vistas). `info.version` 1.1.0, SHA-256 regenerado. OpenAPI + Swagger runtime: 23/0/0/0.

No se modificaron controller, validator, JPA, SQL ni esquema DB para acomodar el contrato: describe el AS-IS observado por HTTP.

**Estado:** `PR18_LOCAL_GREEN_REAL_SQL_AND_JWT / CI remoto en el informe de fase / merge NO autorizado`.
