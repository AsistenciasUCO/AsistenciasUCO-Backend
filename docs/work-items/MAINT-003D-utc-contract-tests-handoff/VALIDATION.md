---
status: CODEC_IMPLEMENTED_V2_BLOCKED_BY_DB_GATE
work-item: MAINT-003D
date: 2026-10-09
---
# MAINT-003D — Validación de la ejecución (CODEX_EXECUTION)

**Veredicto:** codec estricto D02 implementado y validado. **API v2 (POST/GET/PATCH), persistencia de
procedencia y OpenAPI v2 NO implementados: `BLOCKED_BY_MISSING_EVIDENCE` (gate DB, paso 2).**
`V2_IMPLEMENTATION_APPROVED=NO`, `SQL_INTEGRATION_VERIFIED=NO` para v2, `BACKEND_READY_FOR_FRONTEND=NO`.
No se declara GREEN ni READY.

## 1. Repositorios y SHA
| Repo | Ref | SHA |
|---|---|---|
| Backend `alejovh12/AsistenciasUCO` | base `origin/jose-valencia/maint-003d-utc-contract-tests-handoff` | `a495ba7` |
| Backend | merge 003C RED local `jose-valencia/maint-003c-utc-v2-red-hardening` | `19b5a95` → merge en esta rama |
| Backend | merge 003C docs local `jose-valencia/maint-003c-utc-d06-owner-proposal` | `32dc74e` → merge `a4cbd79` |
| Backend | tests ejecutados sobre | `a4cbd79` + cambio de codec y notas de esta entrega (commit posterior) |
| DB `johnjduque/gestion-asistencia-db` | `develop` (clon local + `ls-remote`) | `f2871a9564d6c4cc5abc3745854414243bfda238` |
| Manifiesto owner funcional | — | **no existe** |
| Frontend | — | no tocado (fuera de alcance de esta rama) |

## 2. Gate DB (paso 2) — NO cumplido
Buscado y no encontrado:
- Repo DB `develop`: `docs/work-items/` solo contiene `DB-GP-001B`, `DB-GP-001C` (freeze) y `LB-002.1C-A`; ningún work item pos-DB-GP-001C. `git grep -i procedencia` sin resultados en `develop`, `main`, `sergio`, `fix_db_contrato-_almacenamiento`, `fix_sincronizar_asistencia`, PR #33 y #34.
- DB viva local (`gestionasistenciadb`, SQL Server 2022, solo lectura): `dbo.Sesion` = `id, nombre, numero, codigo, numeroSemana, grupo, fechaHoraInicio datetime2(7), fechaHoraFin datetime2(7)`; **sin columna de procedencia**; 0 módulos (`sys.sql_modules`) referencian `procedenciaTemporal`; 3 sesiones (siguen sin clasificar).
- Firmas owner DB / owner funcional / owner contratos: no localizadas.

Consecuencias (sin activar nada):
- `SesionUtcActivationGuardTest` se conserva (3/3 PASS); ningún controller mapea `/api/v2`.
- `SesionV2HttpContractRedTest` (13) y `OpenApiSesionesV2ContractRedTest` (3 de 4) siguen RED **por diseño**: requieren el controller y el OpenAPI v2, que dependen del gate. No se deshabilitan.
- GET v2 / `estadoTemporal` / lectura de procedencia: imposible sin la columna; no se mapea JPA a una columna inexistente.
- Quién debe aportarlo: owner DB (work item + SQL real), owner funcional (manifiesto), owner contratos (firma D01–D09).

## 3. Integración MAINT-003B / MAINT-003C
- 003B ya era la base (RED `e85feff`, guard `b512bb4`).
- 003C RED (`19b5a95`): merge sin conflictos; añade `HttpUtcInstantCodecStrictProfileRedTest` (8) y 5 casos en `SesionV2HttpContractRedTest`. Ningún test modificado ni eliminado.
- 003C docs (`32dc74e`): merge sin conflictos; solo documentación. `UTC_D06_SQL_DRAFT.md` queda como documento no ejecutable, no como migración.

## 4. Cambio de código
`HttpUtcInstantCodec.toUtcDatabaseDateTime`: patrón léxico D02
`^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,7})?(?:Z|[+-]\d{2}:\d{2})$` + `ISO_OFFSET_DATE_TIME` con
`ResolverStyle.STRICT`; sin `trim`; precisión 0..7 sin redondeo; normaliza el offset a UTC sin zona de JVM;
mensajes sin eco del valor. El codec sigue **sin cablear** (ni v1 ni v2). v1 sin cambios.

## 5. `TEST_CONTRACT_CONFLICT` abierto
`HttpUtcInstantCodecStrictProfileRedTest.d02d04RejectsFractionalSecondsInsteadOfRoundingInDatetime2` (003C)
exige rechazar `2026-07-15T14:00:00.5Z`; D02 de MAINT-003D, HTTP-08 y
`HttpUtcInstantCodecD02ContractRedTest.retainsExactlySevenDecimalPlacesWithoutRounding` exigen aceptar 1..7
decimales. El codec implementa D02. La aserción 003C con 9 decimales sí pasa; la de `.5Z` falla. Por decisión
del usuario (2026-10-09) el test se conserva intacto y en RED. Resolución: owner de contratos + tester.

## 6. Ejecuciones reales (Windows, JDK 25 `C:\Program Files\Java\jdk-25`, 2026-10-09)
| # | Comando | Resultado |
|---|---|---|
| R0 | `mvnw -Dtest=HttpUtcInstantCodec*Test,SesionV2HttpContractRedTest,SesionUtcActivationGuardTest,OpenApiSesionesV2ContractRedTest test` (antes del codec) | 40 tests, 23 fallos (línea base RED) |
| R1 | mismo filtro de codec + guard (después del codec) | 23 tests, 1 fallo (`.5Z`, §5) |
| R2 | `mvnw -B -ntp clean verify` | **exit 1**: 1486 tests, 17 fallos, 0 errores, 0 skips |
| R3 | `mvnw -B -ntp clean verify -Dmaven.test.failure.ignore=true` (solo para medir cobertura) | 1486 / 17 fallos / 0 / 0; JaCoCo "All coverage checks have been met" |
| R4 | `mvnw -B -ntp -Pintegration -Dtest=NoSuchUnitTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true verify` con `.env` local, SQL Server 2022 real | **exit 0**: 191 ITs, 0 fallos, 0 errores, 1 skip |
| R5 | tests del codec con `-DargLine=-Duser.timezone=` `Europe/Berlin`, `America/Bogota`, `Europe/London` | 20 tests cada uno, 1 fallo (`.5Z`), resultados idénticos |

Detalle de los 17 fallos de R2: `SesionV2HttpContractRedTest` 13 (sin controller v2, §2), `OpenApiSesionesV2ContractRedTest` 3 (sin OpenAPI v2, §2), `HttpUtcInstantCodecStrictProfileRedTest` 1 (§5).

Focales: codec original `HttpUtcInstantCodecTest` **7/7**; `HttpUtcInstantCodecD02ContractRedTest` **5/5** (antes 2/5); `HttpUtcInstantCodecStrictProfileRedTest` **7/8** (antes 4/8); guard v1 + bloqueo v2 **3/3**.
Cobertura R3: LINE **90,26 %** (7832/8677), BRANCH **80,27 %** (1851/2306); `HttpUtcInstantCodec` 52/52 instrucciones, 6/6 ramas. ArchUnit (`architecture.*`): 82/82.
R4: `SesionGrupoCommandsSpParityIT` 12/12 (escritura v1 de sesiones), `GoldenPathSqlStoredProcedureContractIT` 16/16. Skip único: `DocenteRepositorySqlServerIT` (`assumeTrue`: "No hay docente con multiples asignaciones para esta base local"), preexistente y ajeno a UTC.

## 7. NOT_RUN (no son PASS)
- POST/GET/PATCH v2, 401/403 v2 con JWT Keycloak real, SQL exacto de procedencia, rollback DB, DST en SQL: bloqueados por §2.
- SonarCloud, CodeQL, Trivy: checks remotos; no se ha abierto PR de esta rama.
- Frontend (`FRONTEND_HANDOFF.md`): fuera de alcance hasta `BACKEND_READY_FOR_FRONTEND`.

## 8. Siguiente paso
Tras el work item DB publicado y ejecutado con SQL real, el manifiesto y las firmas: sustituir el guard por uno que exija procedencia (sin borrarlo), implementar el controller v2 y la lectura de procedencia, versionar el OpenAPI y volver a ejecutar R2 y R4 con los casos HTTP/READ/DB/AUTH de `ACCEPTANCE_MATRIX.md`. Antes, resolver §5.
