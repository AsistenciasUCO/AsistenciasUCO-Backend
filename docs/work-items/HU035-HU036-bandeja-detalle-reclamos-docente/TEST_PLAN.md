# TEST_PLAN — HU035 + HU036: Bandeja y detalle de solicitudes de revisión del docente

## Fuentes

- Requisito: HU035 / HU036 en `AsistenciasUCO-Infra/docs/user-stories/HU-CATALOG.md`; criterios AC-01..AC-09 del [PLAN](PLAN.md).
- Contrato: [CONTRACT.md](CONTRACT.md) (estado `PROPOSED`; este plan se congela junto con la aprobación del contrato).
- DB: `usp_consultar_solicitudes_revision_docente` (R2, ya probado en `GestioAsistenciaDB/test/test_lb004d_support_binding.sql`) y el nuevo `usp_consultar_detalle_solicitud_revision_docente`.
- Seguridad: `SecurityConfig` (`/api/v1/docente/**` → `DOCENTE`), `AuthenticatedUserResolver`, `InstitutionalScopePort`.
- Estándares: [TESTING_STANDARD](../../testing/TESTING_STANDARD.md), [DEFINITION_OF_DONE](../../baseline/DEFINITION_OF_DONE.md).

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| DB-01 | AC-05/06 | Detalle del titular | Fixture: docente T, grupo de T, estudiante A con asistencia `SJC` y solicitud `P` con soporte adjunto | `EXEC detalle @idSolicitud=req, @idUsuarioEjecutor=userT` | Resultset | 1 fila; `codigoEstado='P'`, `codigoRazonCausa='SJC'`, `asistio=0`, `idArchivoSoporte=file`, `nombreEstudiante` no vacío | SP que no une `DetalleAsistencia`/`RazonCausa` o que no filtra el soporte adjunto | T-SQL |
| DB-02 | AC-01/05 | Solicitud de otro docente | Docente T2 sin el grupo | `EXEC` con `userT2` | Resultset | 0 filas | Titularidad validada solo por rol o ausente | T-SQL |
| DB-03 | AC-01 | Ejecutor es el estudiante | Usuario de A | `EXEC` con `userA` | Resultset | 0 filas | Filtro por "usuario participante" en vez de titular | T-SQL |
| DB-04 | AC-05 | Solicitud inexistente | `NEWID()` | `EXEC` | Resultset | 0 filas, sin error | `THROW` ante id desconocido (filtraría existencia) | T-SQL |
| DB-05 | Contrato 2.1 | Parámetros nulos | — | `@idSolicitud=NULL` / `@idUsuarioEjecutor=NULL` / `@idCorrelacion=NULL` | Error | `THROW 51000` con mensaje de catálogo | Aceptar nulos y devolver todo | T-SQL |
| DB-06 | Contrato 2.1 | Sin detalle ni soporte | Solicitud sin `DetalleAsistencia` ni archivo | `EXEC` | Resultset | 1 fila con `codigoRazonCausa`, `asistio`, `idArchivoSoporte` NULL | `INNER JOIN` que pierde la solicitud | T-SQL |
| DB-07 | D3 | Soporte no adjunto | Archivo con estado distinto al parámetro `ESTADO_ADJUNTO` | `EXEC` | Resultset | `idArchivoSoporte` NULL | Ignorar el estado del archivo | T-SQL |
| DB-08 | Constitución 2.02 | Sin transacciones huérfanas | Tras DB-01..07 | `SELECT @@TRANCOUNT` | Valor | 0 | Transacción abierta en SP de lectura | T-SQL |
| UC-01 | AC-05 | Use case delega con el actor | Docente resuelto | `execute(id, actor)` | Llamada al puerto | Puerto invocado con `id` y `actor` exactos | Usar otro id o el docente del request | Unit |
| UC-02 | AC-05 | Puerto sin resultado | Puerto devuelve `Optional.empty()` | `execute` | Excepción | `ResourceNotFoundException` | Devolver objeto vacío o null | Unit |
| UC-03 | AC-07 | Usuario no es docente | `findDocenteIdByUsuario` vacío | `execute` | Excepción | `ForbiddenException`; puerto **no** invocado | Consultar antes de validar el rol institucional | Unit |
| UC-04 | AC-06 | Mapeo de estado | Proyección `codigoEstado` P/A/R | `execute` | DTO | `estadoSolicitud` PENDIENTE/APROBADA/RECHAZADA | Exponer el código crudo o el nombre en minúsculas | Unit |
| UC-05 | Contrato 3.2 | UTC v2 | `procedencia` NULL vs `UTC_V2` | `execute` | DTO | `fechaSesionUtc` null vs instante | Afirmar UTC sin procedencia | Unit |
| UC-06 | Contrato 3.2 | Causal ausente | Proyección sin razón causa | `execute` | DTO | `causalInasistencia == null` | Objeto con campos vacíos | Unit |
| CT-01 | AC-05 | Controller 200 | InputPort devuelve detalle | `GET /api/v1/docente/reclamos/{id}` | HTTP | 200, `exitoso=true`, `datos.id`, `datos.causalInasistencia.codigo` | Envoltorio incorrecto (`ApiListResponse`) | Controller (MockMvc) |
| CT-02 | AC-05 | id no UUID | — | `GET .../reclamos/abc` | HTTP | 400 `INVALID_REQUEST` (enmienda E-01) | 500 por conversión no manejada | Controller |
| CT-03 | AC-05 | No encontrado | InputPort lanza `ResourceNotFoundException` | `GET` | HTTP | 404 `RESOURCE_NOT_FOUND` | 403 o 500 | Controller |
| CT-04 | AC-05 | Actor del JWT | — | `GET` | Argumento capturado | Actor = `requireAuthenticatedUserId()` | Tomar actor de query/header | Controller |
| SEC-01 | AC-07 | Rol ESTUDIANTE / COORDINADOR / sin token | Filtro de seguridad real | `GET .../reclamos/{id}` | HTTP | 403 / 403 / 401 | Ruta fuera del matcher `DOCENTE` | Security (RbacSecurityFilterChainTest) |
| IT-01 | AC-01/05/06 | Repositorio JPA contra SQL Server | Fixture DB-01 | `consultarDetalleSolicitudDocente(req, userT)` | Proyección | Campos mapeados por nombre exacto, `totalRegistros` no aplica, causal presente | Mapeo por posición o nombres de columna distintos al contrato | IT (Failsafe, SQL Server real) |
| IT-02 | AC-01 | Otro docente vía JPA | Fixture | `consultar...(req, userT2)` | Proyección | `Optional.empty()` | Filtrado en Java en lugar de SQL | IT |
| IT-03 | Observabilidad | Correlación | `CorrelationIdContext` fijado | Ejecutar | Parámetro enviado | `@idCorrelacion` = id de contexto | Correlación generada o quemada | IT |
| FE-01 | AC-02 | Bandeja paginada | Servicio mock con `total=25` | Cargar | Petición HTTP | `GET /docente/reclamos?pagina=1&tamano=10`; `PaginationComponent.totalItems=25` | Pedir sin params o paginar en cliente | Karma |
| FE-02 | AC-03/08 | Filtro reactivo | Componente OnPush | Cambiar `estadoFiltro` a `P` | Petición y DOM | Nueva petición con `estado=P` y `pagina=1`; lista actualizada | `computed` sobre campo no-signal (filtro sin efecto) | Karma |
| FE-03 | AC-04 | Contador de pendientes | Backend `total` de `estado=P` = 7, página con 3 pendientes | Cargar | DOM | Muestra 7 | Contar sobre la página cargada | Karma |
| FE-04 | AC-05/06/09 | Abrir detalle | Detalle con causal `SJC` | Click en solicitud | Petición y DOM | `GET /docente/reclamos/{id}`; se muestra "sin justa causa"; no aparece "No informado" | Construir detalle solo con la fila | Karma |
| FE-05 | AC-05 | Detalle 404 | Backend 404 | Click | DOM / toast | Mensaje del backend (`message`); vuelve a bandeja | Silenciar el error o mensaje inventado en cliente | Karma |
| FE-06 | Contrato 4.1 | Error de bandeja | Backend 500/501 | Cargar | Estado de vista | Estado de error visible; no `exitoso:true` falso | Enmascarar fallo como lista vacía | Karma |
| FE-07 | Contrato 4.1 | id inválido | `id='x'` | `getDetalleReclamoDocente('x')` | HTTP | Ninguna petición emitida | Llamar `/reclamos/x` | Karma |
| FE-08 | AGENTS | Sin polling | — | Inspección de spec con `jasmine.clock` | Peticiones en el tiempo | Ninguna petición sin interacción | `setInterval`/`timer` | Karma |
| E2E-01 | AC-01..07 | Flujo real | Backend :8080, Keycloak :8081, fixture con solicitud radicada por HU010 | Script `scratch/test_hu035_hu036.ps1` con token `docente1` y token de otro docente/estudiante | HTTP + SQL | 200 bandeja con `total≥1`; 200 detalle con causal; 404 para otro docente; 403 estudiante; 401 sin token; SQL confirma datos | Pasar solo con mocks | E2E |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: **YES** (SQL Server para SP y JPA; Keycloak para E2E).
- ENVIRONMENT: Docker `sql_server_asistencias`, `asistencias-uco-security-keycloak-1`; backend local :8080.
- MOCK_SUFFICIENT: **NO** para DB/JPA/seguridad; sí para unit del use case y specs Karma.
- WHY: el contrato depende de nombres de columna, joins y titularidad en SQL que un mock no valida.

## Negative / Boundary Coverage

Nulos (DB-05), inexistente (DB-04, CT-03), ajeno (DB-02, IT-02, E2E), rol incorrecto/sin token (SEC-01), id malformado (CT-02, FE-07),
sin detalle/soporte (DB-06, UC-06), soporte no adjunto (DB-07), límites de paginación de la bandeja (ya cubiertos por R2 y `ConsultarReclamosDocenteFeatureTest`; se reejecutan sin modificar).

## Side Effects / Rollback

Consultas puras: ningún test debe observar escrituras. Los fixtures T-SQL se ejecutan dentro de `BEGIN TRANSACTION … ROLLBACK` y verifican `@@TRANCOUNT = 0` al final.
Los IT usan el fixture versionado del repositorio DB (`prepare_java_integration_fixture.ps1` o equivalente documentado en VALIDATION) y su limpieza.

## Pruebas RED requeridas

| Archivo (NUEVO salvo indicación) | Fallo RED esperado |
|---|---|
| `GestioAsistenciaDB/test/test_usp_consultar_detalle_solicitud_revision_docente.sql` | `Could not find stored procedure 'dbo.usp_consultar_detalle_solicitud_revision_docente'` |
| `src/test/.../docente/consultardetallereclamo/ConsultarDetalleReclamoDocenteFeatureTest.java` | Compilación: clases del caso de uso inexistentes |
| `src/test/.../controller/docente/DocentePortalControllerTest.java` (EXISTENTE, se agregan casos CT-01..04) | 404/405 en `GET /reclamos/{id}` por ruta inexistente |
| `src/test/.../security/RbacSecurityFilterChainTest.java` (EXISTENTE, casos SEC-01) | Debe pasar ya en RED (matcher existente): se documenta como guarda de no regresión, no como RED funcional |
| `src/test/.../jpa/SolicitudRevisionDocenteDetalleSqlIT.java` | Método de puerto inexistente / SP inexistente |
| `GestioAsistenciaFrontend/.../teacher-claims.component.spec.ts` y `attendance-claim.service.spec.ts` (EXISTENTES, casos FE-01..08) | Peticiones sin query params, método `getDetalleReclamoDocente` inexistente, filtro no reactivo |

## RED_SNAPSHOT

| Campo | Valor |
|---|---|
| Base commit | Backend `4ff37ff`, DB `585f7f0`, Frontend `ea1e537` (a confirmar al crear ramas) |
| Archivos de test | Tabla anterior |
| SHA-256 o commit del estado RED | Pendiente (commit `test(...)` por repositorio antes de GREEN) |
| Comando | `test_suite`/sqlcmd; `.\mvnw.cmd test` y `.\mvnw.cmd verify -Pintegration`; `npm run test:ci` |
| Exit code | Pendiente |
| Fallo esperado | Ver tabla anterior |

## Congelación

Se congela con la aprobación del CONTRACT. Tras el commit RED, cualquier cambio de test exige `TEST_CONTRACT_CONFLICT` registrado en VALIDATION.

## Integración y E2E

SQL Server 2022 en Docker; Keycloak 26 con usuarios semilla (`docente1`, otro docente, `estudiante1`); fixture de solicitud creado por HU010
(`POST /api/v1/asistencias/revisiones`) o por script SQL transaccional. Skips no permitidos sin registro en VALIDATION.

## Falsos positivos que deben evitarse

- No mockear el repositorio JPA para "probar" titularidad: la titularidad se prueba en DB-02/03 e IT-02.
- No validar solo `status 200`: verificar campos de causal y soporte.
- No copiar el JSON de respuesta de una implementación terminada como expectativa; las expectativas salen de este contrato.
- FE-02 debe fallar con el componente AS-IS (filtro en `computed` sobre campo mutable).
