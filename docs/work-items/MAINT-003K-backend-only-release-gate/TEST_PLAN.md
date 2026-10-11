# MAINT-003K — Tests y oráculos de salida

**Estado:** EJECUTADO el 2026-10-11 contra el contenedor existente `sql_server_asistencias` / `gestionasistenciadb`, Java 25, código certificado `8ac519ae` y arnés E2E `5ab8e8f`, sobre el merge reconciliado `0114939d877f8a6a91b7118af98bf4f7c29c1796`.
Evidencia y cifras: [VALIDATION.md](VALIDATION.md). JVM probadas: `UTC`, `America/Bogota`, `Europe/Berlin`.

| Caso | Configuración | Oráculo | Resultado |
|---|---|---|---|
| TIM-01 | SQL `datetime2(7)` = 2026-09-14 13:00:00.1234567; JVM UTC | GET v1 devuelve el literal, sin desplazamiento | PASS (E2E `TIM-v1` + ITs de paridad) |
| TIM-02 | MISMA fila; JVM America/Bogota | mismo literal | PASS |
| TIM-03 | MISMA fila; JVM Europe/Berlin | mismo literal | PASS |
| TIM-04 | `UvSesionEntity` mapeado Hibernate | `fechaHoraInicio/Fin` son `LocalDateTimeJdbcType`, no `Date/Timestamp` | PASS (`SesionLocalClockProjectionTest`) |
| TIM-04b | Otros lectores de Sesión (materia del estudiante, reporte de asistencia) | `LocalDateTime` literal; ejecutan contra SQL real | PASS tras corrección (antes: `Missing constructor` en runtime) |
| TIM-05 | UTC_V2 confirmado `2026-09-14T13:00:00.1234567Z` | GET v2 siempre devuelve Z exacta en las 3 JVM | PASS (28/28 por JVM) |
| TIM-06 | sesión histórica `procedenciaTemporal=NULL` (creada por SP v1) | GET v2 conserva fila, horas NULL, INDETERMINADA | PASS |
| TIM-07 | v2 POST `2042-07-15T20:00:00.1234567-05:00` | SQL UTC `2042-07-16 01:00:00.1234567`, marca `UTC_V2` | PASS |
| TIM-08 | v2 PATCH mismo instante con offset Berlín +02:00 | SQL UTC exacto, sin doble conversión; GET v2 Z idéntica | PASS |
| TIM-09 | naive, 8 decimales, offset sin minutos | 400 | PASS (3 casos) |
| TIM-10 | v1 consultas con UUID y grupo inexistente | mismo not-found/empty que baseline | PASS (ITs de paridad) |
| AUTH-01 | sin bearer | 401 | PASS (POST/GET v2 y GET v1) |
| AUTH-02 | rol ESTUDIANTE | 403 | PASS |
| AUTH-03 | DOCENTE ajeno consulta/cambia sesión | 403, sin mutación | PASS (POST, GET grupo, GET id, PATCH, GET v1) |
| AUTH-04 | DOCENTE titular | POST→GET→PATCH→GET, id estable, UTC preciso | PASS |
| AUTH-05 | actor en el body | 400 `FIELD_UNKNOWN` | PASS |
| GATE-01 | v2 deshabilitado | con JWT 404 (POST y GET); sin JWT 401 (el filtro de seguridad precede a MVC); v1 sigue 200 | PASS |
| GATE-02 | v2 habilitado contra DB sin contrato D06 (backup pre-D06 restaurado) | el arranque aborta (`IllegalStateException … UTC-D06-POST-FREEZE`) | PASS |
| COMP-01 | OpenAPI canónico: 10 operaciones v1 + 4 v2, incluida la consulta paginada de estudiantes | rutas/métodos/esquemas | PASS (`OpenApiGoldenPathConformanceTest` 9/9, `OpenApiEstudiantesDirectoryConformanceTest` 6/6, `…ValidationTest` 2/2, `…SesionesV2ContractRedTest` 4/4) |
| COMP-02 | PUT v2 | 405 | PASS |
| REG-01 | Surefire + Failsafe SQL/Keycloak/MinIO existentes | 0 fallos, 0 errores, 0 skips | PASS: 1534/1534 unit + 198/198 IT |
| REG-02 | JaCoCo | líneas ≥80 %, ramas ≥70 % | PASS: 92,78 % / 80,96 % |
| REG-03 | ArchUnit + guards | sin violaciones | PASS (`CleanArchitectureRulesTest` 20, `JpaRepositoryArchitectureRulesTest` 3) |
| REG-04 | CI HEAD `146e1e4ac35921a40fe41c89d92f2685b50ceb12` | checks exigidos | **PASS**: Backend CI 38133562756, Backend Security 38133562754 y Backend Deep Security Scan 38133562680; todos `success` |
| VOL-01 | ITs de listado de estudiantes con 5003 estudiantes sintéticos | recorren todas las páginas; oráculo SQL == concatenación de páginas | PASS tras volver los ITs independientes del volumen |
| ORD-01 | reporte de asistencia con estudiantes homónimos | orden total `(numero, inicio, nombre, ei.id, eg.id)` idéntico a JDBC | PASS (3 JVM) |

**Advertencia:** `SesionLocalClockProjectionTest` prueba el mapper aislado; TIM-01..03 se demuestran con SQL real (ITs de paridad ×3 JVM y E2E HTTP ×3 JVM). No se sustituyó la DB real por H2 ni mocks.

Comandos ejecutados (Java 25; `.env` cargado en el proceso):
`./mvnw -B -ntp clean verify` y `./mvnw -B -ntp -Pintegration clean verify`; matriz E2E: `-Duser.timezone=<zona>` + `--server.port=18188 --app.sesiones.v2.enabled=true` y `scripts/e2e/utc-real-jwt-e2e.ps1 -BaseUrl http://127.0.0.1:18188 -TimeZoneLabel <zona>`.

La matriz HTTP real produjo `28/28` comprobaciones en cada JVM (`UTC`, `America/Bogota`, `Europe/Berlin`), 84/84 total. El harness conserva las fechas JSON como strings ISO para no convertirlas accidentalmente a `DateTime` con la cultura de PowerShell.

La ejecución Maven usó `JAVA_TOOL_OPTIONS` con el agente Mockito ya presente en el repositorio local de dependencias para evitar el auto-attach incompatible de Surefire con Windows/JDK 25; no se modificó `pom.xml`, código productivo ni tests para ello.
