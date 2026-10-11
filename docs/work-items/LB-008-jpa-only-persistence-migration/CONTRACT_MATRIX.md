# CONTRACT MATRIX — LB-008

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md).
No contiene secretos. Contrato inventariado el 2026-10-03; JPA-01 y JPA-02A consolidados el
2026-10-05; slice JPA-02B congelado documentalmente el 2026-10-05.

## Evidencia

| Sistema | Repo / versión | Archivo / objeto | Hash / commit | Autoridad |
|---|---|---|---|---|
| Backend | `AsistenciasUCO`, rama `jose-valencia/lb-004-stateless-serverless-readiness` | `src/main/java` | HEAD `0b7905cdba54189bbabe7dd3ea14b66e14bd0c2d` (con cambios LB-004 sin consolidar, 33 entradas en `git status`) | CONSUMER |
| DB | `gestion-asistencia-db`, rama `develop` | `schema/stored-procedures`, `schema/views`, `schema/tables`, `docs/contracts/DB_BASELINE_CONTRACT.md` | HEAD `f2871a9564d6c4cc5abc3745854414243bfda238`, `git status` limpio. Ruta local: `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db` | OWNER (contrato de persistencia) |
| Snapshot backend ZIP | `2fc9be69ad43d2948dd31fe8dcba8956b215f80e521e83d3f6da6ca04b191289` | **VERIFICADO**: `sha256sum AsistenciasUCO___7.zip` coincide | — | — |
| Snapshot DB ZIP | esperado `acccb378a92b7fbda2024e7cd639055ae498fa48ea7261f6b8db1e941ad735a6` | actual `acccb378a92b7fbda2024e7cd639055ae498fa48ea7261f6b8db1e941ad735a6` — **MATCH / VERIFICADO** | — | — |

El hash del snapshot DB fue verificado y coincide. La comparación de contratos se hizo además contra el árbol Git del repo DB en HEAD `f2871a9…` (coincide con el recibido). El bloqueo `BLOCKED_BY_MISSING_EVIDENCE: DB ZIP HASH` queda retirado.

Versión desplegada comprobada en la ejecución JPA-01: SQL Server 16.0 Developer, contenedor
`sql_server_asistencias`, DB `gestionasistenciadb`. Gate del repositorio DB: 157 ejecutadas, 156
PASS, 0 FAIL, 1 skip permitido. Ver [VALIDATION](VALIDATION.md).

## Matriz de stored procedures consumidos por el backend

Leyenda: MATCH = nombre y parámetros Java ⊆ firma DB, y resultado canónico comprobado; MISSING_IN_PROVIDER = referenciado por Java y documentación DB, sin objeto en `schema/`.

Comparación de nombres de parámetros: extracción mecánica del bloque `EXEC dbo.<nombre>` en Java frente a la cabecera `CREATE OR ALTER PROCEDURE` en DB. Tipos y nullability: verificados para los 4 SP de Asistencia (sección siguiente); para el resto solo nombres, no tipos (pendiente en JPA-02B/03).

| # | SP (consumido) | Backend consumer | Estado | Evidencia / nota |
|---|---|---|---|---|
| 1 | `dbo.usp_registrar_asistencias_sesion` | `AsistenciaJpaCommandPersistence` (runtime); `AsistenciaJdbcBaselineOracle` solo test | **MATCH / JPA_ONLY** | Params Java = DB (asistenciaJSON, idCorrelacion, idSesion, idUsuarioEjecutor). SELECT canónico. `SET NOCOUNT ON`. Transacción condicional. |
| 2 | `dbo.usp_registrar_asistencia_estudiante_autonomo` | `AsistenciaJpaCommandPersistence` (runtime); `AsistenciaJdbcBaselineOracle` solo test | **MATCH / JPA_ONLY** | Params Java = DB (codigoVerificacion, idCorrelacion, idEstudiante, idSesion, idUsuarioEjecutor). SELECT canónico. `SET NOCOUNT ON`. Sin transacción propia. |
| 3 | `dbo.usp_radicar_solicitud_revision_asistencia` | `AsistenciaJpaCommandPersistence` (runtime); `AsistenciaJdbcBaselineOracle` solo test | **MATCH / JPA_ONLY** | Params Java = DB (categoria, idCorrelacion, idEstudiante, idSesion, idUsuarioEjecutor, justificacion, soporteNombre, soporteUrl). SELECT canónico. `BEGIN/COMMIT/ROLLBACK TRANSACTION` interno. |
| 4 | `dbo.usp_resolver_solicitud_revision_asistencia` | `AsistenciaJpaCommandPersistence` (runtime); `AsistenciaJdbcBaselineOracle` solo test | **MATCH / JPA_ONLY** | Params Java = DB (accion, idCorrelacion, idDocente, idSolicitud, idUsuarioEjecutor, respuestaDocente). SELECT canónico. Transacción interna. |
| 5 | `dbo.usp_actualizar_asignatura` | `AsignaturaJpaCommandPersistence` (runtime) vía `AsignaturaSqlServerAdapter` | **MATCH / JPA** | 9 params = firma DB. Éxito certificado en JPA-03 (ASG_SUCCESS_02) frente a baseline JDBC `AcademicUserJdbcBaselineOracle`. |
| 6 | `dbo.usp_crear_asignatura` | `AsignaturaJpaCommandPersistence` (runtime) vía `AsignaturaSqlServerAdapter` | **MATCH / JPA** | 10 params incl. idUsuarioEjecutor = firma DB. Éxito certificado (ASG_SUCCESS_01). |
| 7 | `dbo.usp_toggle_estado_asignatura` | `AsignaturaJpaCommandPersistence` (runtime) vía `AsignaturaSqlServerAdapter` | **MATCH / JPA** | idAsignatura, idCorrelacion. Éxito certificado (ASG_SUCCESS_03). |
| 8 | `dbo.usp_actualizar_grupo` | `GrupoJpaCommandPersistence` (runtime) vía `GrupoRepositorySqlServerAdapter` | **MATCH / JPA** | codigo, cupoMaximo, idCorrelacion, idDocente, idGrupo, idUsuarioEjecutor, nombre. Migrado en JPA-02B; éxito certificado en JPA-02B. |
| 9 | `dbo.usp_crear_grupo` | `GrupoJpaCommandPersistence` (runtime) vía `GrupoRepositorySqlServerAdapter` | **MATCH / JPA** | codigo, idAsignatura, idCorrelacion, idDocente, idGrupo, idPeriodoAcademico, idUsuarioEjecutor, nombre. Migrado en JPA-02B. |
| 10 | `dbo.usp_actualizar_sesion` | `SesionJpaCommandPersistence` (runtime) vía `SesionRepositorySqlServerAdapter` | **MATCH / JPA** | fechaHoraFin, fechaHoraInicio, idCorrelacion, idSesion, idUsuarioEjecutor, nombre. Migrado en JPA-02B. |
| 11 | `dbo.usp_crear_sesion` | `SesionJpaCommandPersistence` (runtime) vía `SesionRepositorySqlServerAdapter` | **MATCH / JPA** | fechaHoraFin, fechaHoraInicio, idCorrelacion, idGrupo, idUsuarioEjecutor, nombre. Migrado en JPA-02B. |
| 12 | `dbo.usp_cerrar_sesion` | `SesionJpaCommandPersistence` (runtime) vía `SesionRepositorySqlServerAdapter` | **MATCH / JPA** | idCorrelacion, idDocente, idSesion, idUsuarioEjecutor. Ver TD-039 (documentación de firma). Migrado en JPA-02B. |
| 13 | `dbo.usp_generar_sesiones_grupo` | `SesionJpaCommandPersistence` (runtime) vía `SesionRepositorySqlServerAdapter` | **MATCH / JPA** | idCorrelacion, idGrupo, idUsuarioEjecutor. Migrado en JPA-02B. |
| 14 | `dbo.usp_crear_coordinador` | `CoordinadorJpaCommandPersistence` (runtime) vía `CoordinadorSqlServerAdapter` | **MATCH / JPA** | 12 params = firma DB. Éxito certificado (COO_SUCCESS) frente a baseline JDBC. |
| 15 | `dbo.usp_crear_decano` (2 usos) | `DecanoJpaCommandPersistence` (runtime) vía `DecanoSqlServerAdapter` | **MATCH / JPA** | 12 params = firma DB (`tipoIdentificacionId` no se envía: lo resuelve el SP). Éxito certificado (DEC_SUCCESS). |
| 16 | `dbo.usp_ejecutar_cierre_masivo_periodo` | `CierrePeriodoJpaCommandPersistence` (runtime) vía `CierrePeriodoSqlServerAdapter` | **MATCH / JPA** | codigoPeriodo, idActor, idCorrelacion, idUsuarioEjecutor. Éxito certificado con fixture aislado (CIE_SUCCESS). Riesgo de resolución de periodo: TD-058. |
| 17 | `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` | `GrupoJpaCommandPersistence` (runtime) vía `GrupoRepositorySqlServerAdapter` | **MISSING_IN_PROVIDER** | Código migrado (`JPA`); sin objeto en `schema/`. No se sustituye por `usp_registrar_estudiante_en_grupo_autonomo`/`_interno`. TD-043. |
| 18 | `dbo.usp_registrar_o_actualizar_plan_estudio` | `PlanEstudioJpaCommandPersistence` (runtime) vía `PlanEstudioSqlServerAdapter` | **MISSING_IN_PROVIDER** | Sin objeto en `schema/`; documentado en `DOCUMENTACION_PROCEDIMIENTOS_ORQUESTADORES.md`. TD-043. |
| 19 | `dbo.usp_sincronizar_usuario` | `UsuarioJpaCommandPersistence` (runtime) vía `UsuarioRepositorySqlServerAdapter` | **MISSING_IN_PROVIDER** | Sin objeto con ese nombre. Existe `dbo.usp_sincronizar_usuario_interno`, pero **no es equivalente**: usa parámetros OUTPUT (`@mensajeUsuarioResultado`, `@mensajeTecnicoResultado`, `@estadoResultado`) y no el SELECT canónico. Decisión de mapeo requerida por el owner DB. TD-043. |

Nota: `usp_registrar_estudiante_en_grupo_autonomo` y `usp_registrar_docente_en_grupo_autonomo` existen en DB (commit `f2871a9`) y **no** son consumidos por el backend AS-IS.

### Clasificación de los tres SP no resueltos

| SP | Estado | Owner | Consumer | Opciones | Decisión requerida |
|---|---|---|---|---|---|
| `usp_registrar_estudiante_en_grupo_usuario_no_existente` | MISSING_IN_PROVIDER | DB | GrupoRepositorySqlServerAdapter | (a) el owner DB publica el SP con ese nombre y firma; (b) el backend migra a `usp_registrar_estudiante_en_grupo_autonomo` (requiere contrato y pruebas nuevas); (c) capacidad se retira del alcance | Decisión humana + owner DB. No se elige por suposición. |
| `usp_registrar_o_actualizar_plan_estudio` | MISSING_IN_PROVIDER | DB | PlanEstudioSqlServerAdapter | (a) publicar; (b) retirar la capacidad | Decisión humana + owner DB. |
| `usp_sincronizar_usuario` | MISSING_IN_PROVIDER | DB | UsuarioRepositorySqlServerAdapter | (a) publicar contrato canónico; (b) mapear a `_interno` con OUTPUT (nuevo adapter y pruebas); (c) retirar | Decisión humana + owner DB. |

Mientras no haya decisión, la ejecución/certificación real de esas capacidades permanece
bloqueada. Su `CODE_MIGRATION_STATUS` sigue siendo `JPA_REQUIRED`: no se conserva JDBC como
excepción ni se inventa un provider. Todo lo demás continúa; TD-043 no bloquea globalmente
JPA-02B ni JPA-03.

## Slice contractual JPA-02B — CORE COMMANDS

Estado documental en el freeze original: `READY_WITH_SCOPED_DB_BLOCKER / IMPLEMENTATION_NOT_STARTED`.
Estado posterior: `PASS_WITH_SCOPED_DB_BLOCKER`; esta sección se conserva como snapshot de entrada.

| Subvertical | Command | Provider DB | CODE_MIGRATION_STATUS | DB_PROVIDER_STATUS |
|---|---|---|---|---|
| JPA-02B.1 Sesión | `dbo.usp_crear_sesion` | disponible | JPA_REQUIRED | AVAILABLE |
| JPA-02B.1 Sesión | `dbo.usp_actualizar_sesion` | disponible | JPA_REQUIRED | AVAILABLE |
| JPA-02B.1 Sesión | `dbo.usp_cerrar_sesion` | disponible | JPA_REQUIRED | AVAILABLE |
| JPA-02B.1 Sesión | `dbo.usp_generar_sesiones_grupo` | disponible | JPA_REQUIRED | AVAILABLE |
| JPA-02B.2 Grupo | `dbo.usp_crear_grupo` | disponible | JPA_REQUIRED | AVAILABLE |
| JPA-02B.2 Grupo | `dbo.usp_actualizar_grupo` | disponible | JPA_REQUIRED | AVAILABLE |
| JPA-02B.2 Grupo | `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` | ausente | JPA_REQUIRED | MISSING / BLOCKED_TD043 |

No existe decisión que autorice sustituir el último SP por
`dbo.usp_registrar_estudiante_en_grupo`, `_autonomo`, `_interno` ni otro objeto. Las queries de
`dbo.uv_sesion` y `dbo.uv_grupo` quedan fuera de JPA-02B y continúan asignadas a JPA-04.

### TRANSACTION_MATRIX JPA-02B (entrada)

Inspección estática del consumer backend y de los scripts del owner DB en
`gestion-asistencia-db` `develop@f2871a9564d6c4cc5abc3745854414243bfda238`:

| Command | SP_MANAGES_TRANSACTION | CURRENT_OUTER_TX | TARGET_OUTER_TX | PARITY |
|---|---|---|---|---|
| `dbo.usp_crear_sesion` | CONDITIONAL: transacción propia sin outer; savepoint con outer | NO | NO, sujeto a evidencia | NOT_RUN |
| `dbo.usp_actualizar_sesion` | NO | NO | NO, sujeto a evidencia | NOT_RUN |
| `dbo.usp_cerrar_sesion` | NO | NO | NO, sujeto a evidencia | NOT_RUN |
| `dbo.usp_generar_sesiones_grupo` | YES: `BEGIN/COMMIT/ROLLBACK` propio | NO | NO, sujeto a evidencia | NOT_RUN |
| `dbo.usp_crear_grupo` | NO | YES (`TransactionOperations.execute`) | YES hasta dictamen de paridad | NOT_RUN |
| `dbo.usp_actualizar_grupo` | NO | YES (`TransactionOperations.execute`) | YES hasta dictamen de paridad | NOT_RUN |
| `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` | UNKNOWN: provider ausente | YES (`TransactionOperations.execute`) | YES; no retirar automáticamente | BLOCKED_TD043 |

`TARGET_OUTER_TX` conserva la frontera observable actual como punto de partida. JPA-02B debe
demostrar si los límites de Grupo son necesarios o accidentales antes de retirarlos; esta matriz
documental no declara `PARITY = PASS`.

### SP referenciados solo en tests

`SqlStoredProcedureContractIT` y `GoldenPathSqlStoredProcedureContractIT` contienen nombres de SP para contrato. No añaden consumidores productivos.

## Stored procedures de Asistencia (detalle requerido)

Para cada SP: nombre; parámetros IN (tipo, nullability); transacción; efectos; SELECT final; resultado canónico; consumer; estado.

| Nombre | Parámetros IN (tipo, nullability) | Transacción | Efectos secundarios | SELECT final | Consumer | Estado |
|---|---|---|---|---|---|---|
| `usp_registrar_asistencias_sesion` | `@idSesion UNIQUEIDENTIFIER` (req.), `@asistenciaJSON NVARCHAR(MAX)` (req.), `@idCorrelacion UNIQUEIDENTIFIER` (req.), `@idUsuarioEjecutor UNIQUEIDENTIFIER = NULL` (NULL se rechaza funcionalmente, GEN_002) | Condicional: transacción propia sin outer tx; savepoint con outer tx | Escribe asistencias del lote (estados AN/SJC/EX) | `idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado` (1 fila) | `AsistenciaJpaCommandPersistence`; oráculo JDBC solo test | **MATCH / JPA_ONLY** |
| `usp_registrar_asistencia_estudiante_autonomo` | `@idEstudiante UNIQUEIDENTIFIER`, `@idSesion UNIQUEIDENTIFIER`, `@codigoVerificacion NVARCHAR(50)`, `@idCorrelacion UNIQUEIDENTIFIER`, `@idUsuarioEjecutor UNIQUEIDENTIFIER = NULL` | Sin transacción explícita; escritura delegada a `usp_sincronizar_asistencia_estudiante_interno` (sin transacción) | Registra asistencia `AN` del estudiante | igual | `AsistenciaJpaCommandPersistence`; oráculo JDBC solo test | **MATCH / JPA_ONLY** (atomicidad garantizada por la DB final: TD-056 CLOSED) |
| `usp_radicar_solicitud_revision_asistencia` | `@idEstudiante`, `@idSesion`, `@categoria NVARCHAR(50)`, `@justificacion NVARCHAR(MAX)`, `@soporteNombre NVARCHAR(250)`, `@soporteUrl NVARCHAR(500)`, `@idCorrelacion`, `@idUsuarioEjecutor = NULL` | `BEGIN TRANSACTION` … `COMMIT`/`ROLLBACK` | Crea solicitud de revisión | igual | `AsistenciaJpaCommandPersistence`; oráculo JDBC solo test | **MATCH / JPA_ONLY** |
| `usp_resolver_solicitud_revision_asistencia` | `@idSolicitud`, `@idDocente`, `@accion NVARCHAR(20)`, `@respuestaDocente NVARCHAR(MAX)`, `@idCorrelacion`, `@idUsuarioEjecutor = NULL` | `BEGIN TRANSACTION` … `COMMIT`/`ROLLBACK` | Resuelve solicitud | igual | `AsistenciaJpaCommandPersistence`; oráculo JDBC solo test | **MATCH / JPA_ONLY** |

Todos los SP: `SET NOCOUNT ON` presente (conteo 1 por archivo). Resultado canónico: `idCorrelacion` = variable `@idCorrelacionDefecto` (UNIQUEIDENTIFIER), `mensajeUsuarioResultado`/`mensajeTecnicoResultado` = NVARCHAR(4000), `estadoResultado` = BIT.

Los cuatro commands cerraron con un único patrón runtime:

```text
EntityManager
→ createNativeQuery(EXEC)
→ named parameters
→ getResultList
→ ProcedureResultMapper
→ ProcedureResultValidator
```

`StoredProcedureQuery` se conserva solo como evidencia histórica de LB-002. No es TARGET ni queda
en `src/main` de Asistencia.

Interpretación `AN/SJC/EX` vs estados Java: el SP normaliza `TRIM+UPPER` y rechaza otros valores con `RC_001`. Java no normaliza (`serializarRegistros` envía el valor tal cual). Comportamiento idéntico en ambos providers porque el SP es la única autoridad.

## Vistas consumidas (26)

Para JPA-04/05/06. Columnas usadas por consumidor: pendiente de enumeración completa por vista (se hará en la microfase que la migre). Columna candidata `@Id` y entidad propuesta:

| Vista | Consumer (Java) | @Id candidata | Entidad JPA propuesta | Estado |
|---|---|---|---|---|
| `uv_asistencia` | `AsistenciaJpaQueryPersistence` (JPA-only), `ReporteAsistenciaSqlServerAdapter` (JDBC residual de reporting) | `id` | `UvAsistenciaEntity` (existente) | Asistencia JPA-only; reporting queda JPA-05 |
| `uv_detalle_asistencia` | `AsistenciaJpaQueryPersistence` (JPA-only), `ReporteAsistenciaSqlServerAdapter` (JDBC residual de reporting) | `id` | `UvDetalleAsistenciaEntity` (existente) | Asistencia JPA-only; reporting queda JPA-05 |
| `uv_estudiante_grupo` | AsistenciaQueryPersistence (JPA), GrupoRepositorySqlServerAdapter, InstitutionalScope, MateriaEstudiante, SesionMateriaEstudiante | `id` | `UvEstudianteGrupoEntity` (existente, solo asistencia) | Piloto LB-002.1; resto JPA-04/05 |
| `uv_area` | AreaSqlServerAdapter | `id` | `UvAreaEntity` | JPA / JPA-05 CLOSED |
| `uv_asignatura` | AsignaturaSqlServerAdapter, MateriaEstudianteSqlServerAdapter | `id` | `UvAsignaturaEntity` | JPA-05 |
| `uv_coordinador` | CoordinadorSqlServerAdapter, InstitutionalScope | `id` | `UvCoordinadorEntity` | JPA-05 (autorización: fuera de LB-002.1) |
| `uv_coordinador_identidad` | InstitutionalScope | `id` | `UvCoordinadorIdentidadEntity` | JPA-05 (autorización) |
| `uv_decano` | DecanoSqlServerAdapter, InstitutionalScope | `id` | `UvDecanoEntity` | JPA-05 |
| `uv_decano_identidad` | InstitutionalScope | `id` | `UvDecanoIdentidadEntity` | JPA-05 (autorización) |
| `uv_docente` | Asignaciones académicas, AsignaturaDocente, DocenteRepository, InstitutionalScope | `id` | `UvDocenteEntity` | JPA-04/05 |
| `uv_docente_identidad` | DocenteRepository, InstitutionalScope | `id` | `UvDocenteIdentidadEntity` | JPA-04/05 |
| `uv_estudiante` | Estudiante*, MateriaEstudiante, SesionMateriaEstudiante, InstitutionalScope, GrupoRepository | `id` | `UvEstudianteEntity` | JPA-04/05 |
| `uv_estudiante_identidad` | EstudiantePrograma, InstitutionalScope, EstudianteRepository, GrupoRepository, ReporteAsistencia | `id` | `UvEstudianteIdentidadEntity` | JPA-04/05 |
| `uv_estudiante_programa` | EstudiantePrograma | `id` | `UvEstudianteProgramaEntity` | JPA-05 |
| `uv_facultad` | FacultadSqlServerAdapter | `id` | `UvFacultadEntity` | JPA / JPA-05 CLOSED |
| `uv_grupo` | GrupoRepositoryProjection, MateriaEstudiante, SesionMateriaEstudiante, InstitutionalScope, GrupoRepository | `id` | `UvGrupoEntity` | JPA-04 |
| `uv_horario_docente` | HorarioDocenteSqlServerAdapter | `id` | `UvHorarioDocenteEntity` | JPA-05 |
| `uv_horario_estudiante` | HorarioEstudianteSqlServerAdapter | `id` | `UvHorarioEstudianteEntity` | JPA-05 |
| `uv_institucion` | InstitucionSqlServerAdapter | `id` | `UvInstitucionEntity` | JPA / JPA-05 CLOSED |
| `uv_parametro` | ParametroSqlServerAdapter (academic, JPA / JPA-05 CLOSED) y `SqlServerParameterCatalogAdapter` (catálogo) | `id` | `UvParametroEntity` (reutilizada, mismo contrato) | JPA / JPA-05 CLOSED (academic) y JPA / JPA-06 CLOSED (catálogo) |
| `uv_mensaje_usuario` | `SqlServerMessageCatalogAdapter` | `id` (`codigo` UNIQUE en la tabla base) | `UvMensajeUsuarioEntity` (NUEVA, `@Entity @Immutable`) | JPA / JPA-06 CLOSED |
| `uv_mensaje_tecnico` | `SqlServerMessageCatalogAdapter` | `id` (`codigo` UNIQUE en la tabla base) | `UvMensajeTecnicoEntity` (NUEVA, `@Entity @Immutable`) | JPA / JPA-06 CLOSED |
| `uv_periodo_academico` | PeriodoAcademicoSqlServerAdapter | `id` | `UvPeriodoAcademicoEntity` | JPA-05 |
| `uv_plan_estudio` | PlanEstudioSqlServerAdapter | `id` | `UvPlanEstudioEntity` | JPA-05 (depende de TD-043 para escritura) |
| `uv_semestre_plan_estudio` | AsignaturaSqlServerAdapter | `id` | `UvSemestrePlanEstudioEntity` | JPA-05 |
| `uv_sesion` | SesionMateriaEstudiante, SesionRepository, ReporteAsistencia, JdbcValueMapper (helper) | `id` | `UvSesionEntity` | JPA-04 |
| `uv_tipo_identificacion` | TipoIdentificacionRepositorySqlServerAdapter, DbFailureClassifier | `id` | `UvTipoIdentificacionEntity` | JPA-04 |
| `uv_usuario` | EstudiantePrograma, InstitutionalScope, EstudianteRepository, GrupoRepository, UsuarioRepository, ReporteAsistencia | `id` | `UvUsuarioEntity` | JPA-04/05 |

Notas:
- `uv_auth_*` (`uv_auth_asistencia`, `uv_auth_coordinador`, etc.) existen en DB. El backend no los consume en código productivo (el literal `uv_auth_` aparece como prefijo en `InstitutionalScope`). Fuera de alcance de LB-002.1 por decisión LB-002.0.
- `@Id` candidata: `id` existe en las vistas revisadas; la verificación de unicidad por vista queda para la microfase que la migre.
- Ninguna vista consume `SESSION_CONTEXT`, `usp_consultar_grupos_paginado` ni paginación (DR-010).

### Estado por consumidor (separación CORE / OTHER, 2026-10-05)

La tabla anterior es histórica y se conserva sin cambios. Desde JPA-04, el estado de cada vista debe leerse
separando el consumidor core (ya JPA-only) de los demás consumidores (migran en JPA-05). Recalculado con
`grep` sobre `src/main/java`.

| Vista | CORE CONSUMER STATUS | OTHER CONSUMERS STATUS (actualizado tras JPA-05 CLOSED) |
|---|---|---|
| `uv_usuario` | `UsuarioRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED (lecturas) | `EstudianteProgramaSqlServerAdapter`, `InstitutionalScopeSqlServerAdapter`, `ReporteAsistenciaSqlServerAdapter`: JPA / JPA-05 CLOSED |
| `uv_docente` | `DocenteRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED | `AsignaturaDocenteSqlServerAdapter`: JPA / JPA-05 CLOSED |
| `uv_docente_identidad` | `DocenteRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED | `InstitutionalScopeSqlServerAdapter`: JPA / JPA-05 CLOSED |
| `uv_estudiante` | `EstudianteRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED | Sin consumidor JDBC en `src/main` (solo entidad `UvEstudianteEntity`) |
| `uv_estudiante_identidad` | `EstudianteRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED | `EstudianteProgramaSqlServerAdapter`: JPA / JPA-05 CLOSED; `InstitutionalScopeSqlServerAdapter`: JPA / JPA-05 CLOSED; `ReporteAsistenciaSqlServerAdapter`: JPA / JPA-05 CLOSED |
| `uv_estudiante_grupo` | `GrupoRepositorySqlServerAdapter` (`consultarEstudiantesGrupo`): JPA / JPA-04 CLOSED | `MateriaEstudianteSqlServerAdapter`, `SesionMateriaEstudianteSqlServerAdapter`, `InstitutionalScopeSqlServerAdapter`, `ReporteAsistenciaSqlServerAdapter`: JPA / JPA-05 CLOSED |
| `uv_grupo` | `GrupoRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED | `MateriaEstudianteSqlServerAdapter`, `SesionMateriaEstudianteSqlServerAdapter`, `InstitutionalScopeSqlServerAdapter`: JPA / JPA-05 CLOSED |
| `uv_sesion` | `SesionRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED (UTC certificado) | `SesionMateriaEstudianteSqlServerAdapter`, `ReporteAsistenciaSqlServerAdapter`: JPA / JPA-05 CLOSED (UTC preservado) |
| `uv_tipo_identificacion` | `TipoIdentificacionRepositorySqlServerAdapter`: JPA / JPA-04 CLOSED | Sin consulta: `DbFailureClassifier` solo referencia el nombre en una lista de clasificación de errores |

## Catálogos

| Consumer | Fuente AS-IS | Vista contractual DB | Columnas usadas | Estado |
|---|---|---|---|---|
| `SqlServerMessageCatalogAdapter` (código de mensaje usuario) | `dbo.CatalogoMensajeUsuario` (tabla) WHERE `codigo = :codigo AND estaActivo = 1` | `dbo.uv_mensaje_usuario` (`WHERE estaActivo = 1`) | `codigo`, `contenido`, `estaActivo` | **MATCH**. `UNIQUE(codigo)` garantiza determinismo de `TOP 1`. **JPA / JPA-06 CLOSED** (`UvMensajeUsuarioEntity` + `CatalogJpaQueryPersistence`). |
| `SqlServerMessageCatalogAdapter` (código de mensaje técnico) | `dbo.CatalogoMensajeTecnico` (tabla) WHERE `codigo = :codigo AND estaActivo = 1` | `dbo.uv_mensaje_tecnico` | `codigo`, `contenido`, `estaActivo` | **MATCH**. **JPA / JPA-06 CLOSED** (`UvMensajeTecnicoEntity`). |
| `SqlServerParameterCatalogAdapter` | `dbo.CatalogoParametro` (tabla) WHERE `grupo = :grupo AND clave = :clave AND estaActivo = 1` | `dbo.uv_parametro` | `grupo`, `clave`, `valor`, `estaActivo` | **MATCH**. `UNIQUE(grupo, clave)` garantiza determinismo. **JPA / JPA-06 CLOSED** (`UvParametroEntity` reutilizada). |

Hallazgo: los catálogos AS-IS leían **tablas**, no vistas, aunque existen vistas contractuales equivalentes. JPA-06 migra a
la vista. La vista añade solo `WHERE estaActivo = 1`, que el adapter ya aplicaba.

### Estado JPA-06 — catálogos (evidencia 2026-10-05)

- **Cambio de semántica declarado (decisión de planificación):** el AS-IS usaba `WITH (NOLOCK)` sobre tablas base. Las
  vistas se consultan sin hint, así que la lectura pasa a `READ COMMITTED`. Efecto observable: no hay lecturas sucias
  de filas no confirmadas. Los catálogos se escriben solo por seed/administración, por lo que el riesgo es nulo en
  práctica. Requiere confirmación humana registrada en el work item.
- **Cache (sin cambio):** `ConcurrentHashMap` sin TTL ni límite; solo se cachean resultados positivos; `clearCache()`
  vacía ambos mapas. Sin warm-up en arranque. `CatalogInvalidationPort` no se modifica.
- **Fallback:** no hay fallback JDBC. Un error de persistencia se traduce a `MessageCatalogException` /
  `ParameterCatalogException` con causa original.
- **Paridad:** `CatalogJpaParityIT` 11/11 PASS contra SQL Server real (BEFORE = oráculo JDBC `CatalogJdbcBaseline`).

Auditoría (`AuditEventJdbcRepository`) queda fuera de JPA-06 por `BLOCKED_BY_TD010_DECISION`.

## Auditoría

| Elemento | AS-IS | DB | Estado |
|---|---|---|---|
| `dbo.AuditoriaEvento` | `AuditEventJdbcRepository`: `INSERT` directo y `SELECT TOP 1 … FROM dbo.AuditoriaEvento` | `schema/tables/AuditoriaEvento.sql` existe. `DB_BASELINE_CONTRACT.md` describe el shape como **immutable** (sin auto-migración). **No hay SP público de escritura de auditoría.** `usp_ejecutar_cierre_masivo_periodo` escribe en la tabla internamente. | **BLOCKED_BY_TD010_DECISION** (owner: equipo DB). El contrato describe la forma pero no autoriza el `INSERT` directo desde el backend ni define un SP público de auditoría. TD-010. |

Acción: no se modela `AuditoriaEvento` como entidad de escritura en JPA-06 hasta que el owner DB decida (a) acceso directo autorizado o (b) SP público de auditoría. Lectura `SELECT TOP 1` pendiente de la misma decisión.

### Análisis de gobernanza para TD-010 (JPA-06, 2026-10-05)

| Criterio | Evidencia | Resuelve A/B |
|---|---|---|
| Contrato DB (`DB_BASELINE_CONTRACT.md`) | `AuditoriaEvento` "immutable por contrato" (shape y no auto-migración). Ninguna cláusula autoriza ni prohíbe DML directo del backend; "Auditoria DB mantiene shape" no define el canal de escritura. | No |
| Objetos DB reales (`gestion-asistencia-db`, HEAD `f2871a9`) | No existe SP público de auditoría. No hay `TRIGGER` ni `DENY` sobre `AuditoriaEvento`: la inmutabilidad no está aplicada por la DB, solo declarada. | No |
| Patrón DB existente | `usp_ejecutar_cierre_masivo_periodo` escribe `dbo.AuditoriaEvento` con `INSERT` directo desde un SP. Es patrón del owner DB, no autorización del backend. | No |
| ADR-003 | Exige un patrón único de commands de SP (`createNativeQuery("EXEC …")`) y no decide la auditoría. La DB es de solo lectura para la migración. | No |
| TD-010 | "No se decide A/B por inferencia." Requiere `DECISION_REQUIRED` del owner DB. | No |

Conclusión: **`JPA06_AUDIT_STATUS = BLOCKED_BY_TD010_DECISION`**. Ninguna opción se implementa. No se crea RED de auditoría
(solo entra al RED tras la decisión). No se modifica la DB.

## Inventario JDBC baseline

Generado el 2026-10-03 desde `src/main/java` con búsqueda de `JdbcTemplate|NamedParameterJdbc*|RowMapper|ResultSet|PreparedStatement|CallableStatement|MapSqlParameterSource|org.springframework.jdbc|java.sql.`:

- **47 archivos** con tokens.
- **2 falsos positivos revisados a mano:** `jpa/AsistenciaJpaCommandPersistence.java` (`hasResultSet`, variable local) y `jpa/entity/UvDetalleAsistenciaEntity.java` (comentario).
- **45 archivos reales** que importan o usan la API JDBC/`java.sql`. Esta es la línea base de `DIRECT_JDBC_FILES_BASELINE`.

Lista de los 45 (ruta bajo `src/main/java/co/edu/uco/asistenciasuco/`):

- `infrastructure/adapter/secondary/catalog/sqlserver/SqlServerMessageCatalogAdapter.java`
- `infrastructure/adapter/secondary/catalog/sqlserver/SqlServerParameterCatalogAdapter.java`
- `infrastructure/adapter/secondary/persistence/sqlserver/academic/` — 16 archivos: `AreaSqlServerAdapter`, `AsignaturaDocenteSqlServerAdapter`, `AsignaturaSqlServerAdapter`, `CierrePeriodoSqlServerAdapter`, `CoordinadorSqlServerAdapter`, `DecanoSqlServerAdapter`, `EstudianteProgramaSqlServerAdapter`, `FacultadSqlServerAdapter`, `HorarioDocenteSqlServerAdapter`, `HorarioEstudianteSqlServerAdapter`, `InstitucionSqlServerAdapter`, `MateriaEstudianteSqlServerAdapter`, `ParametroSqlServerAdapter`, `PeriodoAcademicoSqlServerAdapter`, `PlanEstudioSqlServerAdapter`, `SesionMateriaEstudianteSqlServerAdapter`
- `infrastructure/adapter/secondary/persistence/sqlserver/authorization/InstitutionalScopeSqlServerAdapter.java`
- `infrastructure/adapter/secondary/persistence/sqlserver/core/` — `AsistenciaRepositorySqlServerAdapter`, `DocenteRepositorySqlServerAdapter`, `EstudianteRepositorySqlServerAdapter`, `GrupoRepositorySqlServerAdapter`, `SesionRepositorySqlServerAdapter`, `TipoIdentificacionRepositorySqlServerAdapter`, `UsuarioRepositorySqlServerAdapter`
- `infrastructure/adapter/secondary/persistence/sqlserver/reporting/ReporteAsistenciaSqlServerAdapter.java`
- `infrastructure/adapter/secondary/persistence/sqlserver/support/mapping/` — 8 archivos: `DocenteAsignacionAcademicaRepositoryRowMapper`, `DocenteIdentidadRepositoryRowMapper`, `EstudianteContextoAcademicoRepositoryRowMapper`, `EstudianteResumenRepositoryRowMapper`, `GrupoRepositoryRowMapper`, `JdbcValueMapper` (importa `java.sql.Date`, `java.sql.Timestamp`, `java.sql.Time`), `TipoIdentificacionRepositoryRowMapper`, `UsuarioIdentidadRepositoryRowMapper`
- `infrastructure/adapter/secondary/persistence/sqlserver/support/procedure/` — `CanonicalProcedureResultMapper` (RowMapper), `CanonicalStoredProcedureExecutor` (NamedParameterJdbcOperations)
- `infrastructure/audit/adapter/sqlserver/AuditEventJdbcRepository.java`
- `infrastructure/config/adapters/persistence/sqlserver/` — 7 configuraciones: `SqlServerAcademicAdapterConfiguration`, `SqlServerAuditSupportConfiguration`, `SqlServerCatalogAdapterConfiguration`, `SqlServerCoreRepositoryAdapterConfiguration`, `SqlServerProcedureSupportConfiguration`, `SqlServerReportAdapterConfiguration`, `SqlServerSecurityScopeAdapterConfiguration`

Nota de clasificación: las configuraciones de wiring instancian `NamedParameterJdbcOperations`/`JdbcTemplate` y se retiran cuando su adapter se migre (JPA-07).

Se conserva como [INVENTARIO SQL](../../integration/repository-mock-inventory.md) referencia; no se duplica aquí.

### Estado posterior a JPA-01

- `DIRECT_JDBC_ASISTENCIA = 0` en `src/main`.
- `DIRECT_JDBC_GLOBAL_BASELINE = 44` archivos reales, todos residuales fuera de Asistencia.
- `StoredProcedureQuery = 0` en `src/main` de Asistencia.
- runtime híbrido y selectores de Asistencia = 0.

El conteo 45 anterior es la baseline histórica de JPA-00; 44 es el conteo certificado después de
retirar `AsistenciaRepositorySqlServerAdapter` de producción. No se exige 0 global en JPA-02A.

La clasificación archivo a archivo (ACCESS_TYPE, DB_OBJECT, TARGET_JPA_PATTERN, TRANSACTION_BOUNDARY,
BLOCKER y TARGET_MICROPHASE) de los 44 está en [JDBC_RESIDUAL_INVENTORY](JDBC_RESIDUAL_INVENTORY.md).
Ningún archivo queda sin microfase asignada. Este documento conserva la lista JPA-00 como histórica.

## Transacciones

Verificado contra los scripts reales del repo DB (`schema/stored-procedures/*.sql`, HEAD `f2871a9`, 2026-10-04). Cada SP se evalúa por separado; no existe una afirmación genérica para los cuatro.

| SP | SP_MANAGES_TRANSACTION | CURRENT_OUTER_TX | TARGET_OUTER_TX | PARITY | Evidencia del SP |
|---|---|---|---|---|---|
| `dbo.usp_registrar_asistencias_sesion` | **CONDICIONAL**: si `@@TRANCOUNT = 0` abre `BEGIN TRANSACTION` propia (`@transaccionPropia = 1`) y cierra por `XACT_STATE()`; si hay transacción externa usa `SAVE TRANSACTION sp_asistencias_sesion` (savepoint). Equivale a "YES" cuando no hay transacción externa, que es el caso de ambos lados. | NO | NO | YES (ownership: propia, sin externa) | Líneas 51–52, 273–324, 342–356 |
| `dbo.usp_registrar_asistencia_estudiante_autonomo` | **NO**. Ningún `BEGIN`/`COMMIT`/`ROLLBACK` en su cuerpo. Escribe mediante `usp_sincronizar_asistencia_estudiante_interno` (`INSERT dbo.Asistencia` + `INSERT/UPDATE dbo.DetalleAsistencia`), que tampoco abre transacción. | NO | NO | YES | Líneas 113–120; interno líneas 140–159 |
| `dbo.usp_radicar_solicitud_revision_asistencia` | **YES**: `BEGIN TRANSACTION` propio (tras validaciones) → `INSERT dbo.Asistencia` opcional + `INSERT dbo.SolicitudRevisionAsistencia` → `COMMIT`; `CATCH` con `ROLLBACK` si `@@TRANCOUNT > 0`. | NO | NO | YES | Líneas 128–151, 163–165 |
| `dbo.usp_resolver_solicitud_revision_asistencia` | **YES**: `BEGIN TRANSACTION` propio → `UPDATE dbo.SolicitudRevisionAsistencia` (+ `UPDATE dbo.DetalleAsistencia` si APROBADA) → `COMMIT`; `CATCH` con `ROLLBACK`. | NO | NO | YES | Líneas 119–133, 146–148 |

Reglas derivadas:

- `CURRENT_OUTER_TX = NO` en los cuatro: ningún `@Transactional`, `TransactionTemplate` ni `TransactionOperations` envuelve estos commands en el adapter JDBC ni en la Application. Verificado por `grep` sobre `src/main`.
- `TARGET_OUTER_TX = NO`: el candidato JPA no añade `@Transactional`, `EntityTransaction` ni `JpaTransactionManager` (ArchUnit `JpaCommandIsolationRulesTest` lo protege). Se mantiene la frontera transaccional existente, no una nueva.
- **Hallazgo histórico (2026-10-04)**: `usp_registrar_asistencia_estudiante_autonomo` no garantizaba atomicidad entre `Asistencia` y `DetalleAsistencia` ([TD-056](../../baseline/TECHNICAL_DEBT.md#td-056)). **Estado vigente (2026-10-07): TD-056 = CLOSED**; la alineación/corrección final de la DB garantiza esa atomicidad y el Backend consume ese contrato final.
- Ningún command se bloquea por `BLOCKED_BY_TRANSACTION_EVIDENCE`: la evidencia coincide con `CURRENT_OUTER_TX = TARGET_OUTER_TX = NO` en los cuatro.

## Decisiones requeridas

| ID | Mismatch / punto | OWNER | CONSUMER | Opciones | Decisión / aprobador / referencia |
|---|---|---|---|---|---|
| D-LB008-01 | `usp_registrar_estudiante_en_grupo_usuario_no_existente` ausente | DB | backend (Grupo) | ver tabla de 3 SP | PENDIENTE (TD-043) |
| D-LB008-02 | `usp_registrar_o_actualizar_plan_estudio` ausente | DB | backend (PlanEstudio) | ver tabla | PENDIENTE (TD-043) |
| D-LB008-03 | `usp_sincronizar_usuario` ausente; `_interno` no equivalente | DB | backend (Usuario) | ver tabla | PENDIENTE (TD-043) |
| D-LB008-04 | Acceso directo a `dbo.AuditoriaEvento` desde backend | DB | backend (Audit) | (a) autorizar DML directo documentado; (b) SP público de escritura/lectura | PENDIENTE (TD-010) |

## Bloqueos

- `DECISION_REQUIRED`: D-LB008-01 a D-LB008-04.
- `BLOCKED_BY_BASELINE_FAILURE` (primera ejecución 2026-10-04, HISTÓRICO): resuelto por
  `READY_WITH_APPROVED_BASELINE_EXCEPTION` (PLAN, DoR). El global `-Pintegration` sigue `NOT_GREEN_TD043`
  (6 fallos preexistentes, 2 skips TD-044); la baseline dirigida de Asistencia está verde (ver VALIDATION).
- `DECISION_REQUIRED` D-LB008-04 (TD-010) sigue abierta: no afecta a JPA-01.
- `BLOCKED_BY_TRANSACTION_EVIDENCE`: ningún command. La evidencia de transacciones coincide en los cuatro SP (ver Transacciones).
- TD-043: `OPEN / DEFERRED / PREEXISTING / SCOPED_BLOCKER`; mantiene el global
  `NOT_GREEN_TD043`, pero no invalida JPA-01/JPA-02A ni bloquea globalmente JPA-02B. En esta
  microfase bloquea solo la ejecución/certificación de
  `usp_registrar_estudiante_en_grupo_usuario_no_existente`.
- TD-044: `OPEN / NON_BLOCKING`; 2 skips fuera de Asistencia.
- TD-056: histórico `OPEN / PREEXISTING / NON_BLOCKING / DB CONTRACT-BEHAVIOR`. **Vigente: `CLOSED` (2026-10-07)**, cerrada por la alineación final de la DB.
- TD-057: histórico `OPEN / HIGH / DB_OWNER / PREEXISTING / NON_CAUSAL_TO_JPA` (**vigente: `CLOSED` 2026-10-07**, cerrada por la alineación final de la DB). `dbo.Estado` desplegado solo
  contiene `A`/`R`, mientras los SP buscan `P`/`PEND`, `APRO`, `RECH`; el fallback
  `TOP 1 ORDER BY id` deja solicitudes en `A` incluso en rechazo. JDBC y JPA presentan el mismo
  comportamiento: **NO ES REGRESIÓN JPA**. No bloquea JPA-02A, sí declarar correcto el flujo.

## Cierre causal JPA-01

```text
JPA-00 = PASS
JPA-01 FOUNDATION = PASS
JPA-01 COMMANDS = PASS
ASISTENCIA COMMANDS = JPA_ONLY
ASISTENCIA QUERIES = JPA_ONLY
JPA-01 FINAL TARGETED GATE = 83/83 PASS
GLOBAL INTEGRATION = NOT_GREEN_TD043
```

## Tipos y campos (Asistencia, verificados)

| Campo | DB type / nullability | Java type / nullability | Semántica | Status |
|---|---|---|---|---|
| `idSesion` | UNIQUEIDENTIFIER, NOT NULL en la firma (sin default) | `UUID` (`dto.sesion()`) | sesión de clase | MATCH |
| `asistenciaJSON` | NVARCHAR(MAX), NOT NULL (valida `ISJSON` = array) | `String` serializado | lote de estudiantes | MATCH |
| `idCorrelacion` | UNIQUEIDENTIFIER, NOT NULL (`usp_validar_id_correlacion_esta_presente_interno`) | `UUID` (`CorrelationIdContext.require()`) | correlación | MATCH |
| `idUsuarioEjecutor` | UNIQUEIDENTIFIER, NULL permitido en firma; NULL rechazado funcionalmente (GEN_002) | `UUID` | ejecutor | MATCH |
| `estadoResultado` | BIT | `Boolean` / `Number` 0-1 | estado funcional | MATCH |
| `mensajeUsuarioResultado` / `mensajeTecnicoResultado` | NVARCHAR(4000) | `String` (nullable) | mensajes | MATCH |

## Códigos y enums

- Estados de asistencia aceptados por DB: `AN`, `SJC`, `EX` (TRIM+UPPER); otros → `RC_001`.
- Códigos de error del catálogo: `GEN_002`, `ATT_001`, `VAL_007`, `GEN_004`, `SYS_001` (solo referencias a `usp_obtener_mensaje_catalogo`).

## Error semantics

- Fallo funcional SP (`estadoResultado = 0`): se traduce por `DbExceptionTranslator.throwIfFailed` (único clasificador).
- Fallo técnico JPA/SQL (conexión, timeout, SP inexistente, query inválida): `DatabaseOperationException` (`DATABASE_OPERATION_ERROR`).
- Violación de forma del resultado: `ERR_DB_CANONICAL_CONTRACT`.

## Tiempo / UTC

`fechaHoraInicio`/`fechaHoraFin` (DATETIME2) se interpretan como UTC por contrato (comentario en `JdbcValueMapper`, LB-001B.3). La migración JPA no aplica zona horaria local. `AuditoriaEvento.occurredAt` es `DATETIMEOFFSET` en UTC `+00:00`.

## Contrato congelado

- HTTP/OpenAPI: sin cambios.
- DB: sin cambios (READ ONLY para LB-008).
- Los contratos de los 4 SP de Asistencia se consideran congelados para JPA-01 (MATCH verificado).

## Resultado JPA-02B (2026-10-05) — evidencia real

Commands de Sesión y Grupo migrados a `EntityManager + createNativeQuery("EXEC …") + ProcedureResultMapper + ProcedureResultValidator`. Las queries `uv_sesion` y `uv_grupo` siguen en JDBC temporal hasta JPA-04.

### TRANSACTION_MATRIX_FINAL

Verificada contra `schema/stored-procedures` del repositorio DB (`develop@f2871a9`) y contra el código antes/después.

| Command | SP_MANAGES_TRANSACTION (verificado en SQL) | CURRENT_OUTER_TX (BEFORE) | TARGET_OUTER_TX | CURRENT_OUTER_TX (AFTER) | PARITY |
|---|---|---|---|---|---|
| `usp_crear_sesion` | CONDITIONAL: `IF @@TRANCOUNT = 0 BEGIN TRANSACTION` si no hay transacción externa; `SAVE TRANSACTION usp_crear_sesion` si la hay | NO | NO | NO | PASS (SES_01, SES_02) |
| `usp_actualizar_sesion` | NO (sin `TRAN`) | NO | NO | NO | PASS (SES_03, SES_04) |
| `usp_cerrar_sesion` | NO (sin `TRAN`) | NO | NO | NO | PASS (SES_05, SES_06) |
| `usp_generar_sesiones_grupo` | YES: `BEGIN TRANSACTION` (l.176), `COMMIT TRANSACTION` (l.262), `ROLLBACK` en CATCH (l.281) | NO | NO | NO | PASS (SES_07) |
| `usp_crear_grupo` | NO (sin `TRAN`) | YES (`TransactionOperations.execute`) | YES | YES (`TransactionOperations` conservado) | PASS (GRP_01, GRP_02) |
| `usp_actualizar_grupo` | NO (sin `TRAN`) | YES | YES | YES | PASS (GRP_03) |
| `usp_registrar_estudiante_en_grupo_usuario_no_existente` | UNKNOWN: provider ausente (TD-043) | YES | YES | YES | BLOCKED_TD043 (GRP_04: misma clasificación técnica BEFORE/AFTER) |

Decisión sobre la frontera exterior de Grupo: **CONSERVAR**. No hay evidencia de que `TransactionOperations` sea accidental (CONTRACT_MATRIX, PLAN JPA-02B). Se mantiene en los tres commands. Su retiro requeriría RED/GREEN y paridad de commit/rollback que esta microfase no ejecutó.

Limitación declarada: el rollback atómico de Grupo no tiene escenario IT dedicado en esta microfase. La frontera se conserva, así que no cambia; la evidencia de rollback explícito queda pendiente.

### Paridad BEFORE/AFTER (SQL Server real)

Misma IT (`SesionGrupoCommandsSpParityIT`, sobre puertos) ejecutada contra el baseline JDBC (BEFORE) y contra el candidato JPA (AFTER). Las 11 líneas `PARITY_OUTCOME` son idénticas (comparación `diff` sin diferencias).

| Escenario | Excepción | Código | Efecto DB (delta) |
|---|---|---|---|
| SES_01 crear sesión válida | — | — | +1 |
| SES_02 crear con fin < inicio | ValidationException | VALIDATION_ERROR | 0 |
| SES_03 actualizar sesión existente | — | — | +1 |
| SES_04 actualizar sesión inexistente | ResourceNotFoundException | RESOURCE_NOT_FOUND | 0 |
| SES_05 cerrar con docente titular | FeatureUnavailableException | FEATURE_UNAVAILABLE | 0 |
| SES_06 cerrar con docente ajeno | FeatureUnavailableException | FEATURE_UNAVAILABLE | 0 |
| SES_07 generar sin ejecutor | ValidationException | VALIDATION_ERROR | 0 |
| GRP_01 crear grupo sin ejecutor | ValidationException | VALIDATION_ERROR | 0 |
| GRP_02 crear grupo con coordinador | — | — | +1 |
| GRP_03 actualizar grupo inexistente | DatabaseOperationException | ERR_DB_UNCLASSIFIED | 0 |
| GRP_04 registrar estudiante (SP ausente, TD-043) | DatabaseOperationException | DATABASE_OPERATION_ERROR | 0 |

Nota: `SES_05`/`SES_06` devuelven `FEATURE_UNAVAILABLE` desde el SP (mensaje funcional), y `GRP_03` devuelve `ERR_DB_UNCLASSIFIED`. Son el baseline JDBC tal cual; no se corrigieron ni se reinterpretaron.

### Contrato de SP verificado

Las seis firmas disponibles coinciden con los parámetros de los commands (`@idSesion`, `@idDocente`, `@idCorrelacion`, `@idUsuarioEjecutor`, etc.). Todas terminan en `SELECT idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado`. `usp_registrar_estudiante_en_grupo_usuario_no_existente` sigue sin existir en `schema/`: no se sustituyó por `usp_registrar_estudiante_en_grupo*`.

### Conteo JDBC posterior

| Métrica | BEFORE | AFTER |
|---|---|---|
| `JDBC_COMMAND_PATHS_SESION` | 4 | 0 |
| `JDBC_COMMAND_PATHS_GRUPO` | 3 | 0 |
| `JPA_COMMAND_PATHS_SESION` | 0 | 4 |
| `JPA_COMMAND_PATHS_GRUPO` | 0 | 3 |
| `DIRECT_JDBC_GLOBAL_FILES` (archivos reales) | 44 | 44 |

`DIRECT_JDBC_GLOBAL_FILES` no baja porque `SesionRepositorySqlServerAdapter` y `GrupoRepositorySqlServerAdapter` conservan sus consultas JDBC hasta JPA-04. Ningún archivo nuevo usa JDBC: `SesionJpaCommandPersistence` y `GrupoJpaCommandPersistence` tienen 0 tokens JDBC.

## JPA-03 — matriz transaccional y contrato de parametros (2026-10-05)

Fuente: definicion real de cada SP en `gestionasistenciadb` (`OBJECT_DEFINITION`, solo lectura). Los scripts DB no estan en este workspace; no se infiere desde el repositorio. `CURRENT_OUTER_TX` verificado: el unico `TransactionOperations` de `src/main` es el preexistente de Grupo; ninguna capa de Academic/Usuario tiene transaccion exterior.

| COMMAND | SP_MANAGES_TRANSACTION (DB) | CURRENT_OUTER_TX | TARGET_OUTER_TX | PARITY (evidencia) |
|---|---|---|---|---|
| `dbo.usp_crear_asignatura` | NO (sin BEGIN/COMMIT/ROLLBACK en el cuerpo) | NO | NO | ASG_01 identico (error funcional). Exito NOT_RUN |
| `dbo.usp_actualizar_asignatura` | NO | NO | NO | ASG_02 identico. Exito NOT_RUN |
| `dbo.usp_toggle_estado_asignatura` | NO | NO | NO | ASG_03 identico. Exito NOT_RUN |
| `dbo.usp_ejecutar_cierre_masivo_periodo` | YES: `BEGIN TRANSACTION` incondicional bajo `estadoResultado = 1`, `COMMIT`, `CATCH` con `ROLLBACK` si `@@TRANCOUNT > 0` | NO | NO | CIE_01 identico. **Camino BEGIN no alcanzado (NOT_RUN)** |
| `dbo.usp_crear_coordinador` | CONDITIONAL: `SET @conteoTransaccionesInicial = @@TRANCOUNT`; `BEGIN TRANSACTION`; `SAVE TRANSACTION usp_crear_coordinador`; `XACT_STATE()` y `ROLLBACK` a savepoint si `estadoResultado = 0`; `THROW` en CATCH | NO | NO | COO_01 identico (error). Exito NOT_RUN |
| `dbo.usp_crear_decano` | CONDITIONAL, mismo patron que coordinador | NO | NO | DEC_01 identico (error). Exito NOT_RUN |
| `dbo.usp_registrar_o_actualizar_plan_estudio` | UNKNOWN: SP ausente (`sys.procedures` sin filas) | NO | NO | PLA_01 identico (clasificacion equivalente, TD-043) |
| `dbo.usp_sincronizar_usuario` | UNKNOWN: SP ausente (TD-043). No se sustituye por el procedimiento interno | NO | NO | USU_01 identico (clasificacion equivalente, TD-043) |

Reglas de la matriz:

- `TARGET_OUTER_TX = NO` en los 8: no se agrega `@Transactional`, `TransactionOperations` ni `EntityTransaction`. Se conserva la frontera existente.
- Los SP siguen administrando su propia transaccion; JPA la invoca igual que JDBC, sin transaccion exterior.

### Contrato de parametros (verificado contra `sys.parameters`)

| SP | Parametros | Resultado |
|---|---|---|
| `usp_crear_asignatura` | `idAsignatura, codigo, nombre, creditos, idPlanEstudio, semestreNumero, nombreArea, nombreComponente, idCorrelacion, idUsuarioEjecutor` | MATCH (10/10) |
| `usp_actualizar_asignatura` | `idAsignatura, codigo, nombre, creditos, idPlanEstudio, semestreNumero, nombreArea, nombreComponente, idCorrelacion` | MATCH (9/9) |
| `usp_toggle_estado_asignatura` | `idAsignatura, idCorrelacion` | MATCH (2/2) |
| `usp_ejecutar_cierre_masivo_periodo` | `codigoPeriodo, idActor, idCorrelacion, idUsuarioEjecutor` | MATCH (4/4) |
| `usp_crear_coordinador` | 12 parametros (incl. `idCorrelacion`, `idUsuarioEjecutor`) | MATCH (12/12) |
| `usp_crear_decano` | 12 parametros (sin `idTipoIdIdentificacion`, contrato vigente) | MATCH (12/12) |
| `usp_registrar_o_actualizar_plan_estudio` | `idPlanEstudio, idPrograma, codigo, nombre, idCorrelacion` | SP_NOT_IN_DB (TD-043) |
| `usp_sincronizar_usuario` | `idTipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, primerNombre, segundoNombre, correo, password, idCorrelacion` | SP_NOT_IN_DB (TD-043) |

Respuesta de SP: los 6 SP disponibles terminan en el `SELECT` canonico (`idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado`). Ningun command migrado usa `OUTPUT`. El `OUTPUT` de `usp_sincronizar_usuario_interno` no se usa.

## JPA-03 — evidencia de exito y matriz transaccional cerrada (2026-10-05)

Fuente: `OBJECT_DEFINITION` de cada SP y ejecucion real en `gestionasistenciadb` (SQL Server) con fixtures autocontenidos (`IT-LB008-JPA03-*`). Las filas de la seccion "JPA-03 — matriz transaccional y contrato de parametros" se conservan como evidencia de la fase previa al exito; esta seccion las completa.

| COMMAND | SUCCESS_EXECUTED (JDBC oraculo / JPA) | DB_EFFECTS (JPA = JDBC) | SP_TX | OUTER_TX | ROLLBACK / CLEANUP |
|---|---|---|---|---|---|
| `usp_crear_asignatura` | SI / SI | INSERT `Asignatura` (1 fila; codigo, nombre, creditos, area, componente, SPE de la semilla, estado 1). Sin `SemestrePlanEstudio` nuevo | NO | NO | Sin rollback. Limpieza por prefijo y por `Asignatura.codigo` |
| `usp_actualizar_asignatura` | SI / SI | UPDATE `Asignatura` codigo, nombre, creditos. Area, componente y SPE NO cambian | NO | NO | Sin rollback. Limpieza de la fila del fixture |
| `usp_toggle_estado_asignatura` | SI / SI | UPDATE `Asignatura.estado` 1 → 0 | NO | NO | Sin rollback. Limpieza de las dos filas |
| `usp_ejecutar_cierre_masivo_periodo` | SI / SI | UPDATE `EstudianteGrupo.estado` (A → CI para 3 inasistencias; A → F para el resto), UPDATE `Grupo` contadores (`cantidadEstudiantesFinalizaron`, `cantidadEstudiantesCancelaronAutomaticamente`), INSERT `AuditoriaEvento` (`CIERRE_MASIVO_PERIODO`, metadata `{"procesados":2,"reprobadosPorFallas":1}`) | YES (BEGIN/COMMIT en el camino de exito) | NO | Sin rollback probado (camino CATCH no ejecutado). Limpieza: `AuditoriaEvento` del periodo, detalles, asistencias, sesiones, EG, grupo, periodo |
| `usp_crear_coordinador` | SI / SI | INSERT `Usuario`, INSERT `Coordinador`, UPDATE `Programa.coordinador` (restaurado en el test). Sin fila de auditoria | CONDITIONAL (en runtime sin transaccion exterior, usa `BEGIN TRANSACTION` propia y `COMMIT`) | NO | Savepoint y rollback NO probados (solo se alcanza con transaccion exterior). Limpieza: restaura `Programa.coordinador`, borra `Coordinador` y `Usuario` del fixture |
| `usp_crear_decano` | SI / SI | INSERT `Usuario`, INSERT `Decano`, UPDATE `Facultad.decano` (restaurado en el test) | CONDITIONAL (igual que coordinador) | NO | Savepoint y rollback NO probados. Limpieza: restaura `Facultad.decano`, borra `Decano` y `Usuario` del fixture |

Reglas:

- `TARGET_OUTER_TX = NO` se mantiene en los seis. El test no envuelve ningun call en `@Transactional`.
- No se declara rollback intermedio no probado. El camino de savepoint existe en el SP y no se certifica.
- Los dos providers ausentes (`usp_registrar_o_actualizar_plan_estudio`, `usp_sincronizar_usuario`) quedan `DB_PROVIDER_STATUS = MISSING` y `TD043 = OPEN`. Su caracterizacion (provider ausente, clasificacion equivalente) se mantiene; no tienen camino de exito.

### Paridad de exito y efectos (JDBC BEFORE vs JPA AFTER)

| Escenario | JDBC_SUCCESS_OUTCOME | JPA_SUCCESS_OUTCOME | DB_EFFECTS_EQUAL |
|---|---|---|---|
| ASG_SUCCESS_01 | estado=true; mensaje normalizado; correlacion=OK | igual | true |
| ASG_SUCCESS_02 | estado=true; mensaje normalizado; correlacion=OK | igual | true |
| ASG_SUCCESS_03 | estado=true; mensaje normalizado; correlacion=OK | igual | true |
| CIE_SUCCESS | estado=true; mensaje normalizado; correlacion=OK | igual | true |
| COO_SUCCESS | estado=true; mensaje normalizado; correlacion=OK | igual | true |
| DEC_SUCCESS | estado=true; mensaje normalizado; correlacion=OK | igual | true |

Los IDs generados no se comparan literalmente; se comparan estados, mensajes sin IDs, relaciones, conteos y delta por tabla.

### Cierre de la matriz de contrato

- Consumidores runtime: los 8 command paths (`*JpaCommandPersistence`), `CODE_MIGRATION_STATUS = JPA 8/8`.
- Providers DB: `AVAILABLE = 6` con paridad de exito, `MISSING = 2` con caracterizacion.
- SPs sin consumidor productivo JDBC: `CanonicalStoredProcedureExecutor` queda con 0 consumidores productivos; se mantiene como oraculo de test (JPA-07).

## JPA-04 — CORE VIEW QUERIES (freeze 2026-10-05)

Fuente versionada: repo DB `develop@f2871a9564d6c4cc5abc3745854414243bfda238`. Fuente live:
`gestionasistenciadb`, consulta de solo lectura a `sys.columns`/`sys.types` y conteos de identidad.

| CAPABILITY | QUERY_METHOD | VIEW(S) | COLUMNS_USED | SQL TYPES / NULLABILITY relevante | ID STRATEGY | TARGET |
|---|---|---|---|---|---|---|
| Sesión | `consultarSesion`, `consultarSesionesPorGrupo` | `uv_sesion` | id, idGrupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin | uuid/int/nvarchar/datetime2; todas NOT NULL | `id` simple, PK `Sesion.id`, live 2/2 distinct | `UvSesionEntity` + JPQL |
| Grupo | `consultarGrupos` | `uv_grupo` | id, codigo, nombre, idAsignatura, nombreAsignatura, idDocente, capacidadMaximaPermitida, estudiantesActivos, cuposDisponibles, grupoEstaHablitado, fechas | `cuposDisponibles` nullable; resto NOT NULL | `id` simple, PK `Grupo.id`, live 1/1 | `UvGrupoEntity` + JPQL |
| Grupo | `consultarEstudiantesGrupo` | `uv_estudiante_grupo`, `uv_estudiante_identidad`, `uv_usuario` | IDs, número, nombre, correo, códigos/nombres estado | NOT NULL en DB viva/metadata | `uv_estudiante_grupo.id`, identidades por `id` | joins JPQL planos |
| Usuario | tres búsquedas | `uv_usuario` | id, idTipoIdentificacion, numeroIdentificacion, primerNombre, primerApellido, correo | NOT NULL | `id` simple, PK `Usuario.id`, live 6/6 | `UvUsuarioEntity` + JPQL |
| Docente | identidad y asignaciones | `uv_docente_identidad`, `uv_docente` | columnas completas consumidas por las proyecciones | NOT NULL | identidad: `id`; asignación: `idGrupo` (PK Grupo), live 1/1 | dos entities planas + JPQL |
| Estudiante | listado, filtros y detalle | `uv_estudiante_identidad`, `uv_usuario`, `uv_estudiante` | identidad/usuario y contexto académico completo | NOT NULL | identidad: `id`; contexto: `@IdClass(id,idGrupo)`, live 2 filas/0 duplicados | entities + JPQL dinámico tipado |
| TipoIdentificación | `consultarTiposIdentificacion` | `uv_tipo_identificacion` | id, tipoIdentificacion, nombre | NOT NULL | `id` simple, PK tabla, live 5/5 | entity + JPQL |

La unicidad `(id,idGrupo)` de `uv_estudiante` está comprobada en el estado desplegado y deriva del
join conceptual estudiante–grupo, pero `EstudianteGrupo` no declara unique constraint sobre esas
dos columnas. Si el owner DB permite duplicados futuros, se activa `BLOCKED_BY_VIEW_IDENTITY` para
esa vista; no se inventa una identidad técnica en backend.

### Resultado de paridad JPA-04 (2026-10-05)

| Capability | JDBC BEFORE | JPA AFTER | Resultado |
|---|---|---|---|
| Sesión | `uv_sesion`, orden por fecha/número/id, `toLocalDateTimeUtc` | `UvSesionEntity` + JPQL; fecha leída como `Date` JPA y proyectada por instante UTC | MATCH; sin shift |
| Grupo | `uv_grupo` y joins estudiante/identidad/usuario | `UvGrupoEntity` / `UvEstudianteGrupoEntity` + joins JPQL | MATCH; `cuposDisponibles` null preservado |
| Usuario | correo trim/case-insensitive, id e identificación | `UvUsuarioEntity` + tres queries tipadas | MATCH; `Optional.empty` preservado |
| Docente | identidad y asignaciones ordenadas | `UvDocenteIdentidadEntity` / `UvDocenteEntity` | MATCH; identidad `idGrupo` estable |
| Estudiante | listado paginado, count y detalle académico distinct | entities de identidad/usuario/contexto + JPQL dinámico | MATCH; orden/count/contextos preservados |
| TipoIdentificación | listado ordenado por tipo | `UvTipoIdentificacionEntity` | MATCH |

Evidencia: `CoreViewQueriesJpaParityIT` 6/6 PASS sobre `gestionasistenciadb`, comparando todas las
columnas consumidas. La prueba incluye IDs inexistentes para Sesión, Usuario, Docente y Estudiante;
no se observó `NoResultException`. No hubo cambios en OpenAPI, puertos, Domain/Application, schema,
views ni SP. `CORE_QUERY_DIRECT_JDBC = 0`.

## JPA-05 — MATRIZ DE QUERIES (pre-RED, 2026-10-05)

Fuente: `src/main` recalculado (no inventario histórico). CURRENT_SQL literal queda copiado en el oráculo
JDBC de `src/test` (`AcademicQueryJdbcBaselineOracle`, `AuthorizationReportJdbcBaselineOracle`), método por método.
`ORDER BY` se copia explícitamente a JPQL. Nulabilidad verificada en `sys.columns` de `gestionasistenciadb`.
Convención de mapeo JDBC: `toBoolean(null)=false`; `toBoolean(int)=(v==1)`; `toString(int)=String.valueOf`.

TARGET_QUERY por defecto: JPQL con `EntityManager.createQuery(...)`, parámetros nombrados, `getResultList()` y
constructor projection `select new ...QueryRow(...)`. Sin `createNativeQuery` ni `StoredProcedureQuery`.

| CAPABILITY | METHOD | CURRENT_SQL | VIEW_OR_TABLE | COLUMNS_USED | ORDER_BY | FILTERS | NULLABILITY | CURRENT_MAPPER | TARGET_ENTITY | TARGET_QUERY | IDENTITY |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Area | `consultarAreas` | SELECT id,nombre | uv_area | id,nombre | nombre,id | ninguno | NN | JdbcValueMapper | UvAreaEntity (NUEVA) | JPQL + AreaQueryRow | `id` (Area PK) |
| Institucion | `consultarInstituciones` | SELECT 4 cols | uv_institucion | id,nombre,estaActivaInstitucion(bit),estaActivaTextoInstitucion | nombre,id | ninguno | NN | JdbcValueMapper | UvInstitucionEntity (NUEVA) | JPQL + InstitucionQueryRow | `id` (Institucion PK) |
| Parametro | `consultarParametros` | SELECT 7 cols | uv_parametro | id,grupo,clave,valor,tipoDato,valorDefecto,estaActivo(bit) | grupo,clave,id | vista: estaActivo=1 | NN | JdbcValueMapper | UvParametroEntity (NUEVA) | JPQL + ParametroQueryRow | `id` (CatalogoParametro PK; live 24/24) |
| AsignaturaDocente | `consultarAsignaturasDocente(idDocente)` | SELECT DISTINCT 6 cols | uv_docente | idAsignatura,nombreAsignatura,idGrupo,nombreGrupo,idPrograma,nombrePrograma | nombreAsignatura,nombreGrupo | id=:idDocente | NN | JdbcValueMapper | UvDocenteEntity (EXISTE, @Id=idGrupo) | `select distinct new` + AsignaturaDocenteQueryRow | `idGrupo` (certificado en JPA-04) |
| MateriaEstudiante | `consultarMateriasEstudiante(idEstudiante)` | SELECT DISTINCT a.id,a.nombre,g.id,g.nombre; JOIN eg→g→a | uv_estudiante_grupo, uv_grupo, uv_asignatura | eg.idEstudiante,eg.idGrupo; g.id,g.idAsignatura,g.nombre; a.id,a.nombre | a.nombre,g.nombre | eg.idEstudiante=:idEstudiante | NN | JdbcValueMapper | UvEstudianteGrupoEntity, UvGrupoEntity (EXISTEN), UvAsignaturaEntity (NUEVA) | `select distinct new` + MateriaEstudianteQueryRow | PK simple `id` en cada entidad |
| EstudiantePrograma | `consultarEstudiantesPorPrograma(idPrograma)` | JOIN ep→ei→u | uv_estudiante_programa, uv_estudiante_identidad, uv_usuario | ep.id,ei.idUsuario,ei.numeroIdentificacion,ei.nombreCompleto,u.correo,ep.idPrograma,ep.nombrePrograma | ei.nombreCompleto,ep.id | ep.idPrograma=:idPrograma | NN | JdbcValueMapper | UvEstudianteProgramaEntity (NUEVA, @Immutable) | JPQL + EstudianteProgramaQueryRow (`numeroIdentificacion` int→String) | `id` (EstudiantePrograma PK; RESOLVED, DB JPA-05) |
| HorarioDocente | `consultarHorarioDocente(idDocente)` | SELECT 10 cols | uv_horario_docente | todas | dia,horaInicio,nombreMateria | idDocente=:idDocente | horaInicio/horaFin/totalEstudiantes NULLABLE; horas son varchar(5) | JdbcValueMapper (LocalTime desde String) | UvHorarioDocenteEntity (NUEVA) | JPQL + HorarioDocenteQueryRow | `id` (Horario PK; 1 grupo y 1 docente por horario; live 1/1) |
| HorarioEstudiante | `consultarHorarioEstudiante(idEstudiante)` | SELECT 10 cols | uv_horario_estudiante | todas | dia,horaInicio,nombreMateria | idEstudiante=:idEstudiante | horaInicio/horaFin NULLABLE; docente NN | JdbcValueMapper | UvHorarioEstudianteEntity (NUEVA, @IdClass) | JPQL + HorarioEstudianteQueryRow | `(id,idEstudiante)` (ver IDENTITY_EVIDENCE) |
| SesionMateria | `consultarSesionesMateria(idEst,idAsig)` | SELECT 10 cols; JOIN eg, g | uv_sesion, uv_estudiante_grupo, uv_grupo | s.id,nombre,numero,codigo,numeroSemana,idGrupo,codigoGrupo,nombreGrupo,fechaHoraInicio,fechaHoraFin; eg.idEstudiante,eg.idGrupo; g.id,g.idAsignatura | s.numero,s.fechaHoraInicio | eg.idEstudiante=:idEst AND g.idAsignatura=:idAsig | NN | toLocalDateTimeUtc (UTC) | UvSesionEntity, UvEstudianteGrupoEntity, UvGrupoEntity (EXISTEN) | JPQL + SesionMateriaQueryRow; UTC vía mapper certificado | `s.id` (uv_sesion, live 2/2) |
| Periodo | `consultarPeriodosAcademicos` | SELECT 8 cols | uv_periodo_academico | id,idInstitucion,nombreInstitucion,nombre,codigo,fechaInicio,fechaFin,anio | nombre,id | ninguno | NN | JdbcValueMapper (LocalDate) | UvPeriodoAcademicoEntity (NUEVA) | JPQL + PeriodoAcademicoQueryRow | `id` |
| Periodo | `consultarPeriodoAcademicoPorId(id)` | mismo SELECT | uv_periodo_academico | igual | (sin ORDER) | id=:id | NN | JdbcValueMapper | UvPeriodoAcademicoEntity | JPQL + setMaxResults(1) → Optional | `id` |
| Decano | `consultarDecanos` | SELECT 7 cols | uv_decano | id,idUsuario,numeroIdentificacion,nombreCompleto,idFacultad,nombreFacultad,estaActivoDecano(int) | nombreCompleto,id | ninguno | NN | JdbcValueMapper (int→bool ==1) | UvDecanoEntity (NUEVA, @IdClass) | JPQL + DecanoQueryRow | `(id,idFacultad)` (1:N estructural vía uv_facultad.idDecano) |
| Coordinador | `consultarCoordinadoresPorFacultad(idFacultad)` | SELECT 7 cols | uv_coordinador | id,idUsuario,numeroIdentificacion,nombreCompleto,idPrograma,nombrePrograma,estaActivoCoordinador(int) | nombrePrograma,nombreCompleto,id | idFacultad=:idFacultad | NN | JdbcValueMapper | UvCoordinadorEntity (NUEVA, @IdClass) | JPQL + CoordinadorQueryRow | `(id,idPrograma)` (1:N estructural vía uv_programa.idCoordinador) |
| Facultad | `consultarFacultades` | SELECT 8 cols | uv_facultad | id,nombreFacultad,idInstitucion,nombreInstitucion,idDecano(NULL),nombreCompletoDecano(NULL),estaActivaFacultad,estaActivaTextoFacultad | nombreInstitucion,nombreFacultad,id | ninguno | idDecano/nombreCompletoDecano NULLABLE | JdbcValueMapper | UvFacultadEntity (NUEVA) | JPQL + FacultadQueryRow | `id` (Facultad PK; LEFT JOIN a decano PK) |
| Facultad | `consultarFacultadPorId(id)` | mismo SELECT | uv_facultad | igual | (sin ORDER) | id=:id | igual | JdbcValueMapper | UvFacultadEntity | JPQL + setMaxResults(1) | `id` |
| PlanEstudio | `consultarPlanesPorPrograma(idPrograma)` | SELECT 7 cols | uv_plan_estudio | id,idPrograma,nombrePrograma,inp(int),estaActivoPlanEstudio(int),estaActivoTextoPlanEstudio,justificacionEstado | nombrePrograma,inp,id | idPrograma=:idPrograma | NN | JdbcValueMapper (inp→String) | UvPlanEstudioEntity (NUEVA) | JPQL + PlanEstudioQueryRow | `id` (PlanEstudio PK) |
| Asignatura | `consultarAsignaturasPorPrograma(idPrograma)` | SELECT 15 cols; JOIN sp | uv_asignatura, uv_semestre_plan_estudio | todas las de uv_asignatura + sp.idPlanEstudio, sp.idPrograma | nombre,codigo,id | sp.idPrograma=:idPrograma | NN | JdbcValueMapper | UvAsignaturaEntity, UvSemestrePlanEstudioEntity (NUEVAS) | JPQL + AsignaturaQueryRow | `id` (Asignatura PK; joins PK-PK) |
| Asignatura | `consultarAsignaturasPorPlan(idPlanEstudio)` | igual | igual | igual | codigoSemestre,nombre,codigo,id | sp.idPlanEstudio=:idPlanEstudio | NN | JdbcValueMapper | igual | JPQL + AsignaturaQueryRow | `id` |
| Authorization | `findUsuarioIdById` | SELECT TOP 1 id WHERE id | uv_usuario | id | — | id=:usuarioId | NN | JdbcValueMapper | UvUsuarioEntity (EXISTE) | JPQL setMaxResults(1) → Optional | `id` |
| Authorization | `findUsuarioIdByEmail` | SELECT TOP 1 id WHERE LOWER(correo)=LOWER(:email) | uv_usuario | id,correo | — | LOWER(correo)=LOWER(:email) | correo NN | JdbcValueMapper | UvUsuarioEntity | JPQL `lower(u.correo) = lower(:email)`, setMaxResults(1) | `id` |
| Authorization | `findDocenteIdByUsuario` | SELECT TOP 1 id | uv_docente_identidad | id,idUsuario | — | idUsuario=:usuarioId | NN | JdbcValueMapper | UvDocenteIdentidadEntity (EXISTE) | JPQL setMaxResults(1) | `id` (Docente PK) |
| Authorization | `findEstudianteIdByUsuario` | SELECT TOP 1 id | uv_estudiante_identidad | id,idUsuario | — | idUsuario=:usuarioId | NN | JdbcValueMapper | UvEstudianteIdentidadEntity (EXISTE) | JPQL setMaxResults(1) | `id` (Estudiante PK) |
| Authorization | `findProgramaIdByCoordinadorUsuario` | SELECT TOP 1 idPrograma | uv_coordinador | idPrograma,idUsuario,estaActivoCoordinador(int) | — | idUsuario=:u AND estaActivoCoordinador=1 | NN | JdbcValueMapper | UvCoordinadorEntity (NUEVA) | JPQL setMaxResults(1) | `(id,idPrograma)` |
| Authorization | `findCoordinadorIdByUsuario` | SELECT TOP 1 id | uv_coordinador_identidad | id,idUsuario | — | idUsuario=:usuarioId | NN | JdbcValueMapper | UvCoordinadorIdentidadEntity (NUEVA) | JPQL setMaxResults(1) | `id` (Coordinador PK) |
| Authorization | `findFacultadIdByDecanoUsuario` | SELECT TOP 1 idFacultad | uv_decano | idFacultad,idUsuario,estaActivoDecano(int) | — | idUsuario=:u AND estaActivoDecano=1 | NN | JdbcValueMapper | UvDecanoEntity (NUEVA) | JPQL setMaxResults(1) | `(id,idFacultad)` |
| Authorization | `findDecanoIdByUsuario` | SELECT TOP 1 id | uv_decano_identidad | id,idUsuario | — | idUsuario=:usuarioId | NN | JdbcValueMapper | UvDecanoIdentidadEntity (NUEVA) | JPQL setMaxResults(1) | `id` (Decano PK) |
| Authorization | `canDocenteAccessGrupo` | COUNT(1) uv_grupo WHERE id=:g AND idDocente=(SELECT TOP 1 id FROM uv_docente_identidad WHERE idUsuario=:u) | uv_grupo, uv_docente_identidad | g.id,g.idDocente; di.id,di.idUsuario | — | ver TARGET | NN | queryCount | UvGrupoEntity, UvDocenteIdentidadEntity | **una sentencia nativa** (`EntityManager.createNativeQuery`, SQL original con `TOP 1`, parámetros nombrados); sin docente → 0; sin ventana TOCTOU (LB-008 JPA-05 PARTE C) | `id` |
| Authorization | `canEstudianteAccessGrupo` | COUNT(1) uv_estudiante_grupo WHERE idGrupo=:g AND idEstudiante=(SELECT TOP 1 id FROM uv_estudiante_identidad WHERE idUsuario=:u) | uv_estudiante_grupo, uv_estudiante_identidad | eg.idGrupo,eg.idEstudiante; ei.id,ei.idUsuario | — | ver TARGET | NN | queryCount | UvEstudianteGrupoEntity, UvEstudianteIdentidadEntity | **una sentencia nativa** igual que docente (sin ventana TOCTOU) | `id` |
| Authorization | `canCoordinadorAccessPrograma` | COUNT(1) uv_coordinador WHERE idUsuario=:u AND idPrograma=:p AND estaActivoCoordinador=1 | uv_coordinador | idUsuario,idPrograma,estaActivoCoordinador(int) | — | 3 filtros | NN | queryCount | UvCoordinadorEntity | JPQL `select count(c)` | `(id,idPrograma)` |
| Authorization | `canDecanoAccessFacultad` | COUNT(1) uv_decano WHERE idUsuario=:u AND idFacultad=:f AND estaActivoDecano=1 | uv_decano | idUsuario,idFacultad,estaActivoDecano(int) | — | 3 filtros | NN | queryCount | UvDecanoEntity | JPQL `select count(d)` | `(id,idFacultad)` |
| Reporting | `consultarReporteAsistenciaGrupo(idGrupo)` | SELECT 11 cols; JOIN s→eg→ei→u; LEFT JOIN a; LEFT JOIN da | uv_sesion, uv_estudiante_grupo, uv_estudiante_identidad, uv_usuario, uv_asistencia, uv_detalle_asistencia | s.codigoGrupo,nombreGrupo,numero,nombre,fechaHoraInicio,fechaHoraFin,idGrupo,id; eg.id,idGrupo,idEstudiante; ei.numeroIdentificacion,nombreCompleto,idUsuario; u.correo,id; a.id,idEstudianteGrupo,idSesion; da.asistio,nombreRazonCausa,idAsistencia | s.numero,s.fechaHoraInicio,ei.nombreCompleto | s.idGrupo=:idGrupo | asistio/razonCausa NULL si LEFT JOIN vacío | JdbcValueMapper + UTC | UvSesionEntity, UvEstudianteGrupoEntity, UvEstudianteIdentidadEntity, UvUsuarioEntity, UvAsistenciaEntity, UvDetalleAsistenciaEntity (+nombreRazonCausa) | JPQL + ReporteAsistenciaQueryRow; UTC vía mapper certificado | `id` simple en cada entidad; `eg.id` |

### BLOCKED_BY_VIEW_IDENTITY — EstudiantePrograma: RESOLVED (DB JPA-05)

- Causa previa: `uv_estudiante_programa` unía `EstudiantePrograma ep JOIN uv_estudiante e`. `uv_estudiante` tiene una fila por (estudiante, grupo), así que un estudiante en N grupos expandía `ep.id` en N filas.
- Corrección (DB, work item `docs/work-items/LB-008-JPA05-db-view-identity-alignment/`): la vista une `uv_estudiante_identidad` (una fila por estudiante). Shape público sin cambios; sin cambios de tabla, SP ni wire contract.
- Identidad: `EstudiantePrograma.id` (`PK_EstudiantePrograma`, ya existente). No se agregaron columnas ni IDs artificiales.
- Invariante certificada por el gate DB (`VIEW_ESTUDIANTE_PROGRAMA_IDENTITY/CARDINALITY/DEPENDENCY`): una fila de vista por `EstudiantePrograma.id`.
- Backend: `UvEstudianteProgramaEntity` (`@Entity @Immutable`, sin `@IdClass`, sin relaciones, sin escritura), `EstudianteProgramaJpaQueryPersistence` (JPQL con constructor projection), `EstudianteProgramaSqlServerAdapter` como delegador fino.
- Estado: `DIRECT_JDBC_JPA05_SCOPE` de EstudiantePrograma = 0.

### IDENTITY_EVIDENCE (vistas nuevas, 2026-10-05, `gestionasistenciadb`)

| Vista | ID_STRATEGY | Evidencia |
|---|---|---|
| uv_area, uv_institucion, uv_periodo_academico, uv_plan_estudio, uv_semestre_plan_estudio, uv_facultad, uv_asignatura, uv_horario_docente, uv_parametro, uv_coordinador_identidad, uv_decano_identidad | `id` | PK de la tabla base (Area, Institucion, PeriodoAcademico, PlanEstudio, SemestrePlanEstudio, Facultad, Asignatura, Horario, CatalogoParametro, Coordinador, Decano); joins a dimensiones por PK; live total = distinct id |
| uv_coordinador | `@IdClass(id,idPrograma)` | `uv_coordinador_identidad JOIN uv_programa ON ci.id = pr.idCoordinador` puede repetir `ci.id` con varios programas; live (id,idPrograma) 1/1 |
| uv_decano | `@IdClass(id,idFacultad)` | `uv_decano_identidad JOIN uv_facultad ON di.id = f.idDecano` puede repetir `di.id`; live (id,idFacultad) 1/1 |
| uv_horario_estudiante | `@IdClass(id,idEstudiante)` | `Horario JOIN Grupo JOIN EstudianteGrupo`: `h.id` se repite por estudiante. Live: 2 filas, 1 `id` distinto, (id,idEstudiante) 2/2. `EstudianteGrupo` solo tiene PK `id` (sin unique `(grupo,estudiante)`): la unicidad es evidencia viva, no constraint. Si aparece un duplicado → `BLOCKED_BY_VIEW_IDENTITY` para esta capacidad. |
| uv_estudiante_programa | JPA / LB-008 JPA-05 CLOSED (identidad `EstudiantePrograma.id`, RESOLVED) | ver sección anterior |

Vistas existentes reutilizadas (sin nueva entidad): `UvDocenteEntity`, `UvDocenteIdentidadEntity`,
`UvEstudianteIdentidadEntity`, `UvEstudianteGrupoEntity`, `UvGrupoEntity`, `UvSesionEntity`,
`UvUsuarioEntity`, `UvAsistenciaEntity`, `UvDetalleAsistenciaEntity` (esta última recibe la columna
`nombreRazonCausa` que exige el reporte; no se duplica la entidad).

## JPA-06A — Architecture contract

`D-JPA06A-01` (decisión humana, 2026-10-05): Infrastructure JPA repositories use `@Repository` and implement
Application secondary ports directly. Manual composition-root registration is not required for persistence
repositories discovered by Spring. Thin `SqlServerAdapter -> JpaPersistence` chains are removed when they add no
semantic value. Esta decisión cambia estructura/nombres/wiring, no contratos DB ni observables.

Contrato congelado del refactor: mismos 19 SP, 26 views, parámetros, JPQL/SQL, `ProcedureResult`, traducción de
errores, transaction matrix, UTC, null, ordering, authorization, reporte y cache de catálogos. Auditoría TD-010
permanece fuera.

### Cierre JPA-06A (2026-10-06)

- Contrato DB sin cambios: 19 SP, 26 views, parámetros, SQL/JPQL, `ProcedureResult`, traducción de errores,
  transaction matrix, UTC, null, ordering, autorización, reporte y cache de catálogos.
- Paridad SQL Server real: 86/86 PASS en las 11 clases de paridad.
- Delta técnico no contractual: repositories `public class` (sin `final`) por el proxy CGLIB de `@Repository`
  (ver ADR-004, enmienda). No altera firmas de Port ni observables.
- Integración global: 6 fallos, todos TD-043 (3 SP ausentes); 0 causales de JPA-06A.

## ADDENDUM FINAL — alineacion contractual DB/Backend (2026-10-07)

Los bloques anteriores que declaran TD-043 OPEN o providers `MISSING` son historicos (microfases pasadas). Estado vigente:

- **Estado vigente:** `TD-043 = CLOSED`, `TD-047 = CLOSED`; `DIRECT_JDBC_IN_SRC_MAIN = 0`; `BACKEND_DIRECT_INTERNAL_SP = 0`;
  providers DB de matricula (`usp_registrar_estudiante_en_grupo`), sincronizacion de usuario (`usp_sincronizar_usuario`) y
  PlanEstudio (`usp_registrar_o_actualizar_plan_estudio`) = `PRESENT`; commands JPA de SP = `JpaProcedureExecutor`;
  manejo tecnico generico de queries JPA = `JpaQueryExecutor`.

| Contrato | Resultado vigente |
|---|---|
| Identidad de matricula (`usp_registrar_estudiante_en_grupo`) | correo+documento del mismo usuario → reutiliza; ninguno existe → usuario nuevo; solo correo, solo documento o usuarios distintos → `IDN_001` / `ERR_IDENTIDAD_USUARIO_CONFLICTO` (Application ya lo valida antes del command) |
| `usuarioEjecutor` de matricula | Obligatorio en `RegistrarEstudianteDomain`/`RegistrarEstudianteRepositoryDTO`; RBAC DOCENTE/COORDINADOR + titularidad validados por el SP |
| PlanEstudio (escritura) | `registrarOActualizarPlanEstudio(idPlanEstudio, idPrograma, Integer inp, idUsuarioEjecutor)` (MAINT-003H: se agrega el ejecutor, revalidado en DB); sin `codigo`/`nombre` en la vertical de escritura |
| `GEN_001` | Codigo generico de existencia (tipo de identificacion, facultad, perfil, periodo...). Se clasifica `ERR_DB_UNCLASSIFIED`; no se infiere la entidad |
| DBCODE formales anadidos | `ERR_UNICIDAD_DOCUMENTO`, `USU_002`, `HOR_001` → Conflict; `VAL_001..005` → `VALIDATION_ERROR`; `VAL_006` → `CONFLICT`; `ERR_CUPO_INFERIOR_OCUPACION` → `CommonErrorCode.CONFLICT` (`ConflictException`, HTTP 409: no se puede reducir la capacidad del grupo por debajo de su ocupación actual); `ERR_PROGRAMA_FACULTAD_INCONSISTENTE` → `CommonErrorCode.VALIDATION_ERROR` (`ValidationException`, HTTP 400: combinación Programa/Facultad inválida); `USU_001`, `IDN_001`, `EST_001`, `PLA_001`, `PROG_001`, `PER_001`, `CAT_001`, `SYS_001` con test formal |
| `VAL_007` (depende de `operation`) | `crearUsuario`/`sincronizarUsuario` → `ERR_PASSWORD_POLITICA_INVALIDA`; `registrarAsistenciaAutonoma` → `VALIDATION_ERROR`; `resolverSolicitudRevisionAsistencia` → `FORBIDDEN`; otra operacion → `ERR_DB_UNCLASSIFIED` |
| Catalogo `dbo.Estado` (observacion) | La DB final contiene `P`/`A`/`R`: solicitud nueva `P`, `APROBADA` → `A`, `RECHAZADA` → `R`. Las ITs de paridad se alinearon; TD-057 = CLOSED (cerrada por la alineación final de la DB; el Backend consume ese contrato final) |

## Estado vigente de deudas DB (2026-10-07)

`TD-039 = CLOSED`, `TD-043 = CLOSED`, `TD-046 = CLOSED`, `TD-047 = CLOSED`, `TD-056 = CLOSED`, `TD-057 = CLOSED`, `TD-058 = CLOSED`.
TD-039 / TD-046 / TD-056 / TD-057 / TD-058 fueron cerradas mediante la alineación/corrección final de la DB, y el Backend ahora consume ese contrato final. Las referencias anteriores a `OPEN` en este documento son historia por fecha.
