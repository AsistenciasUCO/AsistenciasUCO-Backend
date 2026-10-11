---
status: active
type: handoff
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---
> **Archivo de evidencia histórica (2026-10-09).** Registra decisiones y pruebas de la fase inicial. Desde entonces los PR #18, #19 y #20 se fusionaron a `develop` el 2026-10-10. La implementación UTC v2 de MAINT-003K sigue en una rama separada, todavía sin integración a `develop`; la DB correspondiente también sigue pendiente de PR y aprobación. Los estados `OPEN`, `NOT_READY`, `NOT_RUN` y las propuestas siguientes describen el momento en que se redactó este documento, no el estado actual de todos los repositorios. Ver `PR21_CONFLICT_RESOLUTION.md`.


# ACTA BACKEND_READY_FOR_FRONTEND

**Estado:** `NOT_READY` — `FRONTEND_BLOCKED_BY_BACKEND_CONTRACT`  
**Fecha:** 2026-10-09, America/Bogota  
**Responsable de validación:** Codex, revisión independiente local  
**Merge autorizado:** NO. Los PR permanecen abiertos/draft y requieren revisión manual del usuario.

## Identidad de entrega

| Pieza | Branch / SHA | PR | Estado |
|---|---|---|---|
| Paginación JPA | `jose-valencia/maint-001-jpa-pagination-safety` / `39e34c49ab7882696ab491e1441e45e51cbaf6d0` | [#18](https://github.com/alejovh12/AsistenciasUCO/pull/18) | OPEN, DRAFT, no merge |
| Caché de parámetros | `jose-valencia/maint-002-parameter-cache-key` / `3985b0acbfd357c800bba8e6ec54b7ef8aa0c56a` | [#19](https://github.com/alejovh12/AsistenciasUCO/pull/19) | OPEN, DRAFT, no merge |
| Codec/plan UTC | `jose-valencia/maint-003-backend-utc-pagination-readiness` / `c83442d5fef88e7c3d6856e4f498164b07518c73` | [#20](https://github.com/alejovh12/AsistenciasUCO/pull/20) | OPEN, DRAFT; codec sin consumidores |
| Validación documental | `codex/fase1-backend-validation`, base `c83442d5...` | n/a | worktree local, sin push/merge |
| Owner DB | `gestion-asistencia-db/develop` / `f2871a9564d6c4cc5abc3745854414243bfda238` | n/a | solo lectura; sin cambios |
| Base backend | `develop` / `bfc4fd3fee9fadefbb00fd6f4652ff1dc5423d69` | n/a | referencia común de los PR |

## Entorno y aislamiento

- Java: JDK 25 (`C:\Program Files\Java\jdk-25`).
- SQL Server: 2022 Developer, `16.0.4265.3`, contenedor local.
- Antes de probar se creó backup `COPY_ONLY` con checksum y `RESTORE VERIFYONLY` válido. La prueba usó la restauración aislada `gestionasistenciadb_fase1`.
- Base fuente antes/después: 3 identidades, 3 contextos, 24 parámetros, 3 sesiones. No cambió.
- Fixture solo en clone: 11 identidades, 12 contextos y 1 estudiante con 2 grupos. Después del suite de catálogo quedaron 0 parámetros/mensajes `IT-LB008-JPA06-*` residuales.
- No se ejecutó DDL, migración, conversión histórica, force-push ni merge.

## Evidencia ejecutada

| Gate | Ejecución | Fallos / errores / skips | Resultado |
|---|---|---|---|
| PR18 Java 25 `clean verify` | 1452 unit tests | 0 / 0 / 0 | PASS |
| PR18 `-Pintegration verify` SQL Server real | 192 IT, 30 clases | 0 / 0 / 0 | PASS |
| PR18 `CoreViewQueriesJpaParityIT` | 7 tests | 0 / 0 / 0 | PASS en fixture controlado |
| PR18 `EstudianteAcademicFilterJpaParityIT` | 3 tests | 0 / 0 / 0 | PASS |
| PR18 JaCoCo | LINE 92.53 %, BRANCH 80.97 % | gates 80/70 cumplidos | PASS |
| SQL directo paginación | total 11; páginas 5/5/1; unión distinta 11; 3 páginas | n/a | PASS |
| HTTP real PR18 | páginas 5/5/1, total 11, 9 filtros, `size` 1..100 | 0 fallos; inválidos `-1/0/101` → 400 | PASS funcional |
| JWT real Keycloak | anónimo 401, DOCENTE 403, ADMINISTRADOR 200 | COORDINADOR y ESTUDIANTE reales NOT_RUN | PARTIAL |
| PR19 Java 25 `clean verify` | 1447 unit tests | 0 / 0 / 0 | PASS |
| PR19 `-Pintegration verify` SQL Server real | 191 IT, 30 clases | 0 / 0 / 0 | PASS |
| PR19 `CatalogJpaParityIT` | 11 tests | 0 / 0 / 0 | PASS; hit/miss y cleanup real |
| PR19 `ParameterCatalogJpaBehaviorTest` | 10 tests | 0 / 0 / 0 | PASS; incluye colisión del par estructural |
| PR19 JaCoCo | LINE 92.50 %, BRANCH 80.70 % | gates 80/70 cumplidos | PASS |
| PR20 Java 25 `clean verify` | 1453 unit tests | 0 / 0 / 0 | PASS |
| PR20 `HttpUtcInstantCodecTest` | 7 tests | 0 / 0 / 0 | PASS unitario, no certifica HTTP/SQL |
| PR20 JaCoCo | LINE 90.26 %, BRANCH 80.25 % | gates 80/70 cumplidos | PASS |
| OpenAPI/Swagger local | 11 pruebas canónicas + 6 runtime/docs | 0 / 0 / 0 | PASS del contrato canónico actual; no cubre `/api/v1/estudiantes` |
| Sonar, CodeQL, Dependency Review, Trivy repo/image, ArchUnit y gates agregados | 9/9 check-runs SUCCESS en cada SHA final #18/#19/#20 | CLI locales no instalados | PASS remoto por SHA; local NOT_RUN |
| UTC v2 POST/GET/PATCH + SQL UTC | endpoint no aprobado/no implementado | NOT_RUN | BLOCKED_BY_CONTRACT |
| DST real HTTP/SQL | solo codec unitario; sin roundtrip | NOT_RUN | BLOCKED_BY_CONTRACT |

Los primeros intentos de PR19 que fallaron por ACL de un worktree gestionado y por bloqueo del pipe de auto-attach de Mockito no ejecutaron el suite y no se contabilizan como resultados del código. La ejecución válida se repitió fuera del sandbox y terminó `BUILD SUCCESS`.

## Contrato paginado certificado

`GET /api/v1/estudiantes` requiere Bearer y permite `COORDINADOR` o `ADMINISTRADOR`. La respuesta raíz es:

```text
{ items, totalItems, totalPages, page, size }
```

`page` es 0-based; defaults `page=0`, `size=20`; `size` permitido 1..100. Filtros ejecutados: `tipoIdentificacionId`, `numeroIdentificacion`, `nombre`, `correo`, `institucionId`, `facultadId`, `programaId`, `grupoId`, `activo`. `nombre` debe cumplir formato de nombre y `correo` debe ser email válido. El origen es `uv_estudiante_identidad` + `uv_usuario`; los cuatro filtros académicos usan `EXISTS` sobre `uv_estudiante`, evitando duplicar al estudiante multigrupo.

Totales HTTP observados por filtro, sobre el fixture aislado: tipo 11, número 1, nombre válido 8, correo válido 1, institución 11, facultad 11, programa 11, grupo 1 y activo 11. Los filtros académicos combinados también pasaron en `EstudianteAcademicFilterJpaParityIT`.

## Caracterización UTC

La DB y los SP declaran `datetime2` sin información de zona. Las 3 filas observadas no contienen nulos, rangos invertidos ni subminutos; sus duraciones son 120–180 minutos. Al compararlas con `uv_horario`, 0/3 encajan completamente como reloj local y 0/3 como UTC convertido a Bogotá. Sin evento externo conocido, el dictamen es:

```text
HISTORICAL_TZ_UNDETERMINED
```

No se autoriza `UPDATE`, `AT TIME ZONE` masivo ni reinterpretación de v1. La propuesta que debe aprobarse antes de implementar está en `UTC_API_V2_PROPOSAL.md`: v2 con offset obligatorio, normalización a UTC, salida `Z`, v1 intacta y horarios recurrentes fuera de la conversión.

## Revisión de PR

1. **P1 de certificación — PR18:** el test de páginas 0/1/2 compara JPA contra SQL correctamente, pero no exige `total >= 11` ni que la tercera página tenga datos. En la base fuente de 3 estudiantes pasaría con páginas 1 y 2 vacías; por sí solo no certifica tres páginas reales. Esta validación lo compensó con un fixture aislado 11/multigrupo, pero el PR debería incorporar una precondición/fixture reproducible antes de cierre.
2. **P1 contractual — PR18:** `/api/v1/estudiantes` figura en `HTTP_AS_IS_MATRIX.md`, pero no está descrito en el OpenAPI canónico Golden Path. Por eso el gate OpenAPI verde no valida sus nueve filtros, paginación ni DTO. Antes del handoff al frontend debe publicarse en el contrato autorizado o registrarse una decisión explícita de alcance contractual.
3. **P2 de gobernanza — PR18:** `MAINT-001/PLAN.md` conserva una cabecera de auditoría `DOCUMENTATION_ONLY` y luego añade microfases de implementación. El alcance efectivo está descrito, pero el registro de autorización debería quedar inequívoco antes del merge.
4. **Sin hallazgo bloqueante de código — PR19:** la clave `ParameterKey(group,key)` elimina la colisión por concatenación y los tests de caché/SQL real pasan. Queda fuera del alcance cambiar el catch amplio existente.
5. **P3 de higiene — PR20:** `git diff --check` detecta whitespace final en `.claude/skills/uco-backend-pagination-utc/SKILL.md:7`. No afecta runtime, pero debe limpiarse antes de merge.
6. **PR20 no es implementación UTC:** `HttpUtcInstantCodec` está inactivo y correctamente no cambia v1. POST/GET/PATCH v2, OpenAPI v2 y roundtrip SQL permanecen pendientes por diseño.

## Motivos del NOT_READY

- Los tres PR siguen draft y no están integrados.
- El contrato paginado probado todavía no forma parte del OpenAPI canónico.
- Falta aprobación explícita del contrato `UTC-D01..UTC-D09` y decisión del owner funcional/DB sobre históricos indeterminados.
- No existe API v2; por tanto POST/GET/PATCH, SQL UTC y DST end-to-end son `NOT_RUN`.
- RBAC con JWT real quedó certificado para ADMINISTRADOR/DOCENTE/anónimo, pero no para COORDINADOR/ESTUDIANTE por falta de fixtures E2E configurados. Los tests unitarios del filter chain no sustituyen esa evidencia real.
- La validación manual del usuario y la autorización de integración siguen pendientes.

## Rollback y pendientes

- Paginación/caché: revert individual del PR correspondiente; no hay rollback DB porque no se cambió schema ni la base fuente.
- UTC: no hay cambio runtime que revertir en PR20. El futuro micro-PR v2 debe ser reversible por commit y no puede incluir migración histórica.
- La copia `gestionasistenciadb_fase1` y el backup local se mantienen como evidencia recuperable; eliminarlos requiere una acción separada y explícita.

**Firma del usuario / autorización manual:** PENDIENTE.  
**Owner DB/funcional — interpretación histórica:** PENDIENTE.  
**Frontend handoff:** BLOQUEADO.  
**Autorización de merge:** NO.

---

## Actualización — 2026-10-09, segunda pasada (Claude Code)

**Estado se mantiene:** `NOT_READY` — `FRONTEND_BLOCKED_BY_BACKEND_CONTRACT`. **BACKEND_READY_FOR_FRONTEND = NO.**

### Identidad de entrega actualizada

| Pieza | Branch / SHA | Cambio en esta pasada |
|---|---|---|
| PR #18 | `jose-valencia/maint-001-jpa-pagination-safety` / `9ed690f` | `3b1ddb4` (P1 fixture) certificado; `7a5eceb` seed E2E 4 roles; `e7803ca` RED OpenAPI; `1c96162` oráculo; `d0468e3` contrato `GET /api/v1/estudiantes`; `9ed690f` evidencia |
| PR #20 | `jose-valencia/maint-003-backend-utc-pagination-readiness` / `b512bb4` | `0bd1119` whitespace P3; `b512bb4` guarda v1 + bloqueo `/api/v2/**` |
| RED UTC v2 | `jose-valencia/maint-003b-utc-v2-red` / `e85feff` | sin PR; 11 RED esperados |
| PR #19 | sin cambios (`3985b0a`) | — |

### Resueltos respecto de la primera pasada

1. **P1 certificación PR #18:** `3b1ddb4` exige `total >= 11`, tamaños 5/5/1 e IDs distintos. Contra la fuente de 3 estudiantes falla con mensaje accionable (control negativo); contra el clon pasa. `clean verify` 1452/0/0/0 y `-Pintegration verify` 192 IT/0/0/0 en `3b1ddb4`; 1458/0/0/0 y 192/0/0/0 en `d0468e3`.
2. **P1 contractual PR #18:** `GET /api/v1/estudiantes` publicado en el OpenAPI canónico (1.1.0) con RED previo y 23/0/0/0 tras el contrato. Sin cambios de runtime ni DB.
3. **JWT real COORDINADOR y ESTUDIANTE:** 42/42 casos HTTP PASS con los cuatro roles reales (detalle en `MAINT-001/VALIDATION.md`).
4. **P3 PR #20:** whitespace eliminado; `git diff --check` limpio en el rango del PR.

### Pendiente (motivos vigentes del NOT_READY)

- UTC-D06 sin decisión del owner DB/funcional; alternativas en [UTC_D06_HISTORICAL_PROVENANCE_OPTIONS](UTC_D06_HISTORICAL_PROVENANCE_OPTIONS.md). D01–D05/D07–D09 solo aceptadas como diseño inicial sujeto a pruebas.
- API v2 no implementada: POST/GET/PATCH v2, SQL UTC y DST end-to-end `NOT_RUN` por diseño. RED preparado ([diseño](UTC_V2_MICRO_PR_DESIGN.md)).
- PR #18, #19 y #20 abiertos, sin merge (no autorizado).
- P2 gobernanza PR #18: se añadió `MAINT-01C` con autorización y rollback; la cabecera `DOCUMENTATION_ONLY` original del PLAN se conserva como histórico y requiere revisión humana.
- Validación manual del usuario y firma de handoff.

### Rollback de esta pasada

- PR #18: `git revert 9ed690f d0468e3 1c96162 e7803ca 7a5eceb` (en ese orden) devuelve el contrato a 9 operaciones y el seed a 2 roles; ningún efecto runtime/DB. Los usuarios Keycloak locales `coordinador.prueba` y `estudiante.prueba` se eliminan por consola/Admin REST si se desea; sus valores viven solo en `infra/keycloak/.env` (ignorado).
- PR #20: `git revert b512bb4 0bd1119`.
- RED: borrar la rama `jose-valencia/maint-003b-utc-v2-red` (no tiene PR).
- DB: ninguna escritura en la fuente; el clon y su backup siguen como evidencia.

---

## ACTA MAINT-003C — 2026-10-09, revisión independiente (Claude Code)

**BACKEND_READY_FOR_FRONTEND = NO** — `FRONTEND_BLOCKED_BY_BACKEND_CONTRACT` / `UTC_D06_OWNER_DECISION_PENDING`.
**Merge autorizado:** NO. **UTC v2:** no implementado ni activado (sin decisión aprobada del owner DB/funcional).

### Identidad de entrega

| Pieza | Branch / SHA | Estado verificado |
|---|---|---|
| PR #18 paginación | `jose-valencia/maint-001-jpa-pagination-safety` / `7e2827d1aff87b5371c349fdc9cd73b72d14f3a9` | OPEN; 9/9 check-runs `success`; Sonar QG `OK` (new coverage 90.3 %) |
| PR #19 caché | `jose-valencia/maint-002-parameter-cache-key` / `3985b0acbfd357c800bba8e6ec54b7ef8aa0c56a` | OPEN, draft; 9/9 `success`; Sonar `OK` |
| PR #20 codec + guarda | `jose-valencia/maint-003-backend-utc-pagination-readiness` / `b512bb409c970654edb13355a72278fd3c93da42` | OPEN, draft; 9/9 `success`; Sonar `OK` |
| RED v2 (MAINT-003B) | `jose-valencia/maint-003b-utc-v2-red` / `e85feff3` | sin PR |
| RED D02 (MAINT-003C) | `jose-valencia/maint-003c-utc-v2-red-hardening` / `19b5a957c6368ca024f788cd0c0aee249aca96e1` | sin PR; 20 RED esperados |
| Propuesta owner (MAINT-003C) | `jose-valencia/maint-003c-utc-d06-owner-proposal` (este documento) | sin PR; solo docs |
| Owner DB | `johnjduque/gestion-asistencia-db` `f2871a9` | sin commits desde 2026-10-01; **sin decisión** |

### Resultado de la revisión

1. **PR #18:** checks del último commit verdes; clasificación de alcance de `MAINT-001/PLAN.md` coherente con el diff real (solo validador HTTP y adapter JPA en producción; sin DB/SP/vistas/POM/frontend). Omisiones P3: skill de persistencia, `OPENAPI_STANDARD.md`, `.env.example` no enumerados; MAINT-01B es un cambio HTTP acotado ya publicado en OpenAPI.
2. **UTC-D06:** dirección A+C+E correcta; 4 brechas P1 (valor para clasificación del owner, escrituras directas, atomicidad marca/horas, freeze DB-GP-001C). Propuesta definitiva: [UTC_D06_OWNER_DB_PROPOSAL](UTC_D06_OWNER_DB_PROPOSAL.md) + [borrador SQL no ejecutable](UTC_D06_SQL_DRAFT.md).
3. **UTC-D02:** el codec acepta offsets fuera de `Z|±HH:mm`, hora sin segundos y 9 decimales; su `IllegalArgumentException` acabaría en 500; el rango sobre instantes no estaba especificado. Propuesta: [UTC_D02_ERROR_CONTRACT_PROPOSAL](UTC_D02_ERROR_CONTRACT_PROPOSAL.md).
4. **Compatibilidad:** v1 intacto bajo la propuesta; dos superficies v1 adicionales con horas de sesión (`/api/v1/estudiante/materias/{id}/sesiones`, Excel de asistencia) quedan fuera de v2 y documentadas.

### Evidencia SQL (solo lectura, `gestionasistenciadb`, SQL Server 2022 `16.0.4265.3`)

`datetime2(7)` en `Sesion` y `DetalleAsistencia`; sin triggers/CHECK/CDC/temporal; 3 sesiones, 9 detalles; 3 rutas SP + 1 semilla escriben `Sesion`; 7 SP y 4 vistas la leen con listas explícitas; `TIEMPO/ALMACENAMIENTO_INSTANTES=UTC`, `ZONA_HORARIA_IANA=America/Bogota`, `ZONA_HORARIA_SQLSERVER=SA Pacific Standard Time`. Ninguna escritura, DDL ni conversión.

### Decisión del owner

| Rol | Decisión | Estado |
|---|---|---|
| Owner DB | marca de procedencia A/A2, work item post-freeze | **PENDIENTE** |
| Owner funcional | clasificación C por ID y entorno | **PENDIENTE** |
| Contratos | D01–D09, perfil/errores D02, forma E1 | **PENDIENTE** |

Sin estas firmas no se implementa ni se activa UTC v2. Por tanto siguen `NOT_RUN` por diseño: POST/GET/PATCH v2 con SQL Server, casos Bogotá/Londres/Berlín verano-invierno-DST, JWT real/ownership v2, OpenAPI v2, y los gates JaCoCo/Sonar/CodeQL/Trivy de un micro-PR v2 inexistente.

### Motivos vigentes del NO

- UTC-D06 y D02 sin firma; v2 inexistente (guarda PR #20 lo garantiza).
- PR #18, #19 y #20 abiertos; merge no autorizado.
- Validación manual del usuario pendiente.

### Rollback de MAINT-003C

Borrar las ramas `jose-valencia/maint-003c-utc-d06-owner-proposal` y `jose-valencia/maint-003c-utc-v2-red-hardening` (sin PR, sin efecto runtime). DB: sin escrituras.
