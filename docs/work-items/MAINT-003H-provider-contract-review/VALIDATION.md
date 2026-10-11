---
status: VALIDATED_LOCAL_ISOLATED_PENDING_REMOTE_CI_AND_DB_APPROVAL
work_item: MAINT-003H / CC-003G-01
date: 2026-10-10
---

# MAINT-003H — Validacion (evidencia real)

Todo contra un contenedor SQL Server 2022 Developer **aislado y nuevo** (`127.0.0.1:14334`), desplegado desde cero desde el repo DB, y contra el
Keycloak local del proyecto. No se uso la DB principal, no hubo merge, no se desplego a produccion y no se imprimieron credenciales.

## Commits exactos ejecutados

| Repo | SHA | Arbol |
|---|---|---|
| Backend (`jose-valencia/maint-003h-provider-contract-impl`) | `78e1c6fa0eab70627d7b2110d826da94d9326978` | `src/` y `pom.xml` identicos al commit; solo habia cambios de documentacion sin commitear (`docs/**`). `git diff 78e1c6f..HEAD -- src pom.xml` esta vacio. |
| DB (`feat/cc-003g-01-public-user-plan-providers`) | `0749c3a1af77ec6879efc70d5e7396054ff58fb1` | limpio |
| Baseline backend (antes) | `e45c36a` (hijo de `30fcd5e`; solo agrega tests MAINT-003G) | — |
| Baseline DB publicado (antes) | `3842f70` | — |

## Resultados

| # | Comprobacion | Comando / entorno | Resultado |
|---|---|---|---|
| 1 | Tests unitarios + ArchUnit + JaCoCo, **Java 25** (`java version "25" 2025-09-16 LTS`, JDK por `JAVA_HOME`) | `.\mvnw.cmd -B -Pintegration clean verify` | Surefire **1499 tests, 0 failures, 0 errors, 0 skipped** (el informe previo de MAINT-003G reporta 1495; esta fase agrega `PLA_PAT_004`, la prueba de propagacion del ejecutor y el caso `GEN_003`), `BUILD SUCCESS` |
| 2 | Integracion Failsafe contra SQL Server real | misma corrida | **196 tests, 0 failures, 0 errors, 0 skipped** (original: 191 / 3 failures / 0 / 3 skips) |
| 3 | Gates JaCoCo del `pom.xml` (LINE >= 80 %, BRANCH >= 70 %) | misma corrida | `All coverage checks have been met`. Cobertura medida: **LINE 91,31 % (8221/9003), BRANCH 78,87 % (1904/2414)** — no inferior a la reportada antes (91,06 % / 78,77 %) |
| 4 | Quality gate SQL (repo DB, despliegue limpio) | `deploy_schema.ps1` + `test_summary.ps1` | **219 ejecutadas, 218 PASS, 0 FAILED, 1 skip permitido** (`XACT_STATE_MINUS_ONE_RUNTIME`, igual que el baseline: 179/178/0/1), `DB GATE PASS` |
| 5 | Regresion UTC-D06 SQL | dentro del gate (22 ids `UTC_D06_*`) | PASS, incluidos `_RUNTIME_PERMISSIONS`, `_DIRECT_UPDATE_REJECTED`, `_CONCURRENT_V2_CREATE`; `FREEZE_MANIFEST_OK` sin cambios |
| 6 | Regresion UTC v2 HTTP real (JWT de Keycloak local, backend real arrancado por el harness con **login SQL de minimo privilegio**, `app.sesiones.v2.enabled=true`, puerto 18081) | [evidence/utc_e2e_harness.ps1](evidence/utc_e2e_harness.ps1) sobre el jar de `78e1c6f` | JVM en zona por defecto del host (Bogota): **20/20 PASS**; JVM `-Duser.timezone=UTC`: **21/21 PASS**. El **jar baseline** previo da los mismos resultados (20/20 y 21/21) |

### Detalle de la regresion UTC v2 (jar de `78e1c6f`, DB aislada desplegada desde `0749c3a`)

401 sin JWT; POST v2 Bogota `-05:00` (201); listado por grupo; GET tras POST conserva `2042-07-15T14:00:00.1234567Z` y
`2042-07-15T15:30:00.7654321Z` con `procedenciaTemporal=UTC_V2`; PATCH Berlin `+02:00` (200) y GET devuelve `2042-07-16T14:00:00Z` /
`2042-07-16T16:30:00.25Z`; 403 para DOCENTE no titular (GET, PATCH, POST) y para rol no DOCENTE; PUT v2 -> 405; POST sin offset -> 400;
v1 sigue funcionando (201), su GET conserva el formato local sin offset, una sesion creada por v1 es `INDETERMINADA` en v2; el login de minimo
privilegio escribe auditoria y las sesiones de prueba quedan en la DB objetivo. Los usuarios temporales de Keycloak (0 restantes), el login SQL
(0 restantes) y las sesiones de prueba se eliminaron al terminar. El harness rechaza correr si el puerto ya esta ocupado por otro proceso y
verifica que el listener sea el backend que el mismo lanzo.

**Corridas descartadas (honestidad):** las primeras ejecuciones del harness contestaron en realidad desde un backend antiguo de una sesion
previa (PID 23100, puerto 18080, ligado a otra DB aislada) porque el harness aun no validaba el listener. Se detecto, se descartaron sus
resultados, se retiraron de esa otra DB (`utc_d06_sqltest`) las 10 sesiones `E2E-CC003G01-*` que habian quedado (las filas de auditoria
append-only no se tocaron) y se repitio todo en el puerto 18081 con el harness corregido. El proceso antiguo del puerto 18080 **no se detuvo**.

### Observacion (NO causada por este cambio, no corregida aqui)

Con la JVM en la zona por defecto de este host (Bogota, UTC-5) `GET /api/v1/sesiones/{id}` devuelve +5 h respecto al valor almacenado: una
sesion v2 almacenada `2042-07-16 14:00:00` se lee `2042-07-16T19:00:00` y una sesion v1 enviada con `08:00:00` (almacenada `08:00:00`) se lee
`13:00:00`. Con `-Duser.timezone=UTC` la lectura v1 es exacta (`14:00:00` / `08:00:00`). El **jar baseline** previo se comporta igual en ambos
casos, por lo que no es una regresion de MAINT-003H: la lectura v1 depende de la zona horaria de la JVM (causa probable, no diagnosticada a fondo:
`UvSesionEntity` mapea `java.util.Date` y `CoreViewJpaProjectionMapper.toUtcLocalDateTime` lo interpreta como UTC). v2 no esta afectado (sus instantes son exactos en ambas zonas).
Recomendacion: desplegar la JVM en UTC o abrir un work item UTC para fijar la lectura v1.

## Sin falsos PASS / no ejecutado

- SonarCloud, CodeQL y los checks remotos de GitHub **no** se ejecutaron localmente (`NOT_RUN`); corren en CI al abrir los PR Draft.
- La DB institucional/staging y la aprobacion del owner DB/seguridad: `NOT_RUN` (decision humana). No se cierra TD-043.
- La primera corrida con el IT nuevo fallo (1 failure propio del test nuevo: lectura HQL obsoleta en el mismo contexto de persistencia); se
  corrigio el test (`78e1c6f`) y se repitio la verificacion completa; no se tocaron las aserciones de ningun test preexistente.
- `UsuarioPlanEstudioProvidersSqlServerIT` no cubre programa ajeno, rollback forzado, concurrencia ni login restringido (requieren DDL/DML de
  fixture o dos sesiones): esos casos estan en el gate SQL del repo DB.

## Reconciliacion de worktrees (sin perdida de cambios)

- `e45c36a` (backend) y `3842f70` (DB) estaban solo locales: se publicaron con push **fast-forward sin force** a
  `jose-valencia/maint-003f-utc-v2-implementation` (`30fcd5e..e45c36a`) y `feat/utc-d06-post-freeze` (`6946a73..3842f70`).
- Worktrees nuevos y separados para el trabajo de esta fase: `.workspace/m3h` (backend, desde `e45c36a` + merge de `bee75c7`) y
  `.workspace/db-cc003g01` (DB, desde `3842f70`). `.workspace/m3d` y `.workspace/db-utc-d06` quedaron intactos (con sus `target/` y evidencias).
