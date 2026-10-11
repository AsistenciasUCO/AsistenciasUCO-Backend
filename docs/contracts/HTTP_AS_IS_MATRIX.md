---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Matriz HTTP AS-IS

Movida y contrastada desde `docs/backend-baseline-contract.md`. **Evidencia estática de implementación**, no OpenAPI aprobado, no certificación de DB desplegada ni E2E. Las filas con `**` agrupan familias y NO son paths literales ni garantía de todos los verbos; el inventario exacto de mappings aparece al final.

`IMPLEMENTATION_AVAILABLE_PENDING_E2E`: camino implementado, sin corrida E2E acreditada aquí. `BLOCKED_BY_DB`/`READY_WITH_DB_LIMITATION`: clasificación heredada de capacidad no disponible o contrato externo pendiente, no prueba de que el esquema actual carezca de ella. La presencia de una ruta no implica funcionalidad operativa.

Los SP/views de las filas son contratos consumidos/reportados por Java; las firmas liberadas requieren evidencia del equipo DB. Los 11 pendientes DB de la matriz anterior están conservados en [TD-019](../baseline/TECHNICAL_DEBT.md#td-019), el contrato individual/estados en [TD-009](../baseline/TECHNICAL_DEBT.md#td-009). Seguridad normativa: [runtime-security](../security/runtime-security-provider-architecture.md); RBAC efectivo: [SecurityConfig](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/security/SecurityConfig.java), además de los checks contextuales de cada flujo.

## Matriz funcional conservada

| Endpoint | Rol funcional (ver RBAC efectivo) | UseCase | SecondaryPort | Vista/SP DB | Estado | Bloqueo si aplica | Contrato HTTP principal |
|---|---|---|---|---|---|---|---|
| `GET /api/v1/admin/decanos` | ADMINISTRADOR | Consultar decanos | `DecanoQueryPort` | `uv_decano` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<DecanoDTO>` |
| `POST /api/v1/admin/decanos` | ADMINISTRADOR | Crear decano | `DecanoCommandPort` | `usp_crear_decano` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Validar firma DB en E2E controlado | `CrearDecanoRequest` |
| `GET /api/v1/admin/parametros` | ADMINISTRADOR | Consultar parametros | `ParametroQueryPort` | `uv_parametro` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<ParametroDTO>` |
| `GET /api/v1/admin/instituciones` | ADMINISTRADOR | Consultar instituciones | `InstitucionQueryPort` | `uv_institucion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<InstitucionDTO>` |
| `GET /api/v1/admin/facultades` | ADMINISTRADOR | Consultar facultades | `FacultadQueryPort` | `uv_facultad` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<FacultadDTO>` |
| `GET /api/v1/admin/areas` | ADMINISTRADOR | Consultar areas | `AreaQueryPort` | `uv_area` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<AreaDTO>` |
| `POST /api/v1/admin/cierre-masivo` | ADMINISTRADOR | Ejecutar cierre masivo | `CierrePeriodoCommandPort` | `usp_ejecutar_cierre_masivo_periodo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Validar firma DB en E2E controlado | `CierreMasivoRequest` |
| `POST/PUT/PATCH /api/v1/admin/instituciones/**` | ADMINISTRADOR | Mutar institucion |  |  | BLOCKED_BY_DB | No hay command publico confirmado | `FeatureUnavailableException` |
| `POST/PUT/PATCH /api/v1/admin/facultades/**` | ADMINISTRADOR | Mutar facultad |  |  | BLOCKED_BY_DB | No hay command publico confirmado | `FeatureUnavailableException` |
| `POST/PUT/PATCH /api/v1/admin/areas/**` | ADMINISTRADOR | Mutar area |  |  | BLOCKED_BY_DB | No hay command publico confirmado | `FeatureUnavailableException` |
| `/api/v1/admin/sedes/**` | ADMINISTRADOR | Sedes |  |  | BLOCKED_BY_DB | Sede no esta modelada actualmente | `FeatureUnavailableException` |
| `/api/v1/admin/espacios-fisicos/**` | ADMINISTRADOR | Espacios fisicos |  |  | BLOCKED_BY_DB | EspacioFisico no esta modelado actualmente | `FeatureUnavailableException` |
| `GET /api/v1/decano/coordinadores` | DECANO / ADMINISTRADOR | Consultar coordinadores por facultad | `CoordinadorQueryPort` | `uv_coordinador` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idFacultad` autenticada | `ApiListResponse<CoordinadorDTO>` |
| `POST /api/v1/decano/coordinadores` | DECANO / ADMINISTRADOR | Crear coordinador | `CoordinadorCommandPort` | `usp_crear_coordinador` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idFacultad` autenticada | `CrearCoordinadorRequest` |
| `PATCH /api/v1/decano/coordinadores/{id}/estado` | DECANO / ADMINISTRADOR | Toggle coordinador |  |  | BLOCKED_BY_DB | No hay command publico especifico confirmado | `FeatureUnavailableException` |
| `GET /api/v1/coordinador/planes-estudio` | COORDINADOR / ADMINISTRADOR | Consultar planes | `PlanEstudioQueryPort` | `uv_plan_estudio` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `ApiListResponse<PlanEstudioDTO>` |
| `GET /api/v1/coordinador/asignaturas` | COORDINADOR / ADMINISTRADOR | Consultar asignaturas | `AsignaturaQueryPort` | `uv_asignatura` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `ApiListResponse<AsignaturaDTO>` |
| `GET /api/v1/coordinador/periodos-academicos` | COORDINADOR / ADMINISTRADOR | Consultar periodos | `PeriodoAcademicoQueryPort` | `uv_periodo_academico` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<PeriodoAcademicoDTO>` |
| `GET /api/v1/coordinador/estudiantes` | COORDINADOR / ADMINISTRADOR | Consultar estudiantes programa | `EstudianteProgramaQueryPort` | `uv_estudiante_programa`, `uv_estudiante_identidad`, `uv_usuario` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `ApiListResponse<EstudianteProgramaDTO>` |
| `POST/PUT /api/v1/coordinador/planes-estudio/**` | COORDINADOR / ADMINISTRADOR | Mutar planes | `PlanEstudioCommandPort` | `usp_registrar_o_actualizar_plan_estudio` (envia `idUsuarioEjecutor`; DB revalida COORDINADOR + titularidad, MAINT-003H) | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `GuardarPlanEstudioRequest` |
| `POST/PUT/PATCH /api/v1/coordinador/**/asignaturas/**` | COORDINADOR / ADMINISTRADOR | Mutar asignaturas | `AsignaturaCommandPort` | `usp_crear_asignatura`, `usp_actualizar_asignatura`, `usp_toggle_estado_asignatura` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | DELETE fisico sigue bloqueado por DB | `GuardarAsignaturaRequest` |
| `GET /api/v1/docente/horarios` | DOCENTE | Consultar horario docente | `HorarioDocenteQueryPort` | `uv_horario_docente` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por docente autenticado | `ApiListResponse<HorarioDocenteDTO>` |
| `GET /api/v1/docente/asignaturas` | DOCENTE | Consultar asignaturas docente | `AsignaturaDocenteQueryPort` | `uv_docente` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por docente autenticado | `ApiListResponse<AsignaturaDocenteDTO>` |
| `GET /api/v1/estudiante/horarios` | ESTUDIANTE | Consultar horario estudiante | `HorarioEstudianteQueryPort` | `uv_horario_estudiante` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por estudiante autenticado | `ApiListResponse<HorarioEstudianteDTO>` |
| `GET /api/v1/estudiante/materias` | ESTUDIANTE | Consultar materias | `MateriaEstudianteQueryPort` | `uv_estudiante_grupo`, `uv_grupo`, `uv_asignatura` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por estudiante autenticado | `ApiListResponse<MateriaEstudianteDTO>` |
| `GET /api/v1/estudiante/materias/{materiaId}/sesiones` | ESTUDIANTE | Consultar sesiones materia | `SesionMateriaEstudianteQueryPort` | `uv_sesion`, `uv_estudiante_grupo`, `uv_grupo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por estudiante autenticado | `ApiListResponse<SesionMateriaEstudianteDTO>` |
| `GET /api/v1/estudiante/materias/{materiaId}/prerrequisitos` | ESTUDIANTE | Prerrequisitos |  |  | BLOCKED_BY_DB | PrerrequisitoAsignatura no esta modelado | `FeatureUnavailableException` |
| `POST /api/v1/usuarios` | AUTENTICADO | Provisionar usuario | `UsuarioRepositoryPort`, `IdentityProviderPort` | `usp_sincronizar_usuario`, Keycloak Admin API | IMPLEMENTATION_AVAILABLE_PENDING_E2E | No crea perfil estudiante directo | `CrearUsuarioRequest` |
| `GET /api/v1/usuarios/perfil` | AUTENTICADO | Consultar perfil |  | `uv_usuario` | BLOCKED_BY_DB | Pendiente crear vertical query typed | `FeatureUnavailableException` |
| `PUT /api/v1/usuarios/perfil` | AUTENTICADO | Actualizar perfil |  |  | BLOCKED_BY_DB | No hay command publico confirmado para update parcial seguro | `FeatureUnavailableException` |
| `POST /api/v1/sesiones` | DOCENTE | Crear sesion | `SesionRepositoryPort` | `usp_crear_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope docente resuelto desde JWT | `CrearSesionRequest` |
| `GET /api/v1/sesiones/{sesionId}` | AUTENTICADO | Consultar sesion | `SesionRepositoryPort` | `uv_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiDataResponse<SesionConsultadaDTO>` |
| `PUT /api/v1/sesiones/{sesionId}` | DOCENTE | Actualizar sesion | `SesionRepositoryPort` | `usp_actualizar_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope docente resuelto desde JWT | `ActualizarSesionRequest` |
| `POST /api/v1/sesiones/cierres` | DOCENTE | Cerrar sesion | `SesionRepositoryPort` | `usp_cerrar_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope docente resuelto desde JWT | `CerrarSesionRequest` |
| `POST /api/v1/sesiones/grupo/{grupoId}/generacion` | DOCENTE | Generar sesiones grupo | `SesionRepositoryPort` | `usp_generar_sesiones_grupo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | No hay loops/INSERTs en Java | Path `grupoId` |
| `GET /api/v1/sesiones/grupo/{grupoId}` | DOCENTE | Consultar sesiones por grupo | `SesionRepositoryPort` | `uv_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope docente resuelto desde JWT via `InstitutionalScopePort` | `ApiListResponse<SesionConsultadaDTO>` |
| `GET /api/v1/grupos/{grupoId}/reportes/asistencia-excel` | DOCENTE / COORDINADOR / ADMINISTRADOR | Reporte asistencia | `ReporteAsistenciaQueryPort` | `uv_sesion`, `uv_estudiante_grupo`, `uv_estudiante_identidad`, `uv_usuario`, `uv_asistencia`, `uv_detalle_asistencia` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Sin reglas inventadas de aprobacion | XLSX |
| `POST /api/v1/asistencias/lote` | DOCENTE | Registro masivo | `AsistenciaRepositoryPort` | `usp_registrar_asistencias_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | JSON serializado con ObjectMapper | `RegistrarAsistenciasSesionRequest` |
| `POST /api/v1/estudiante/asistencia-qr` | ESTUDIANTE | Registro autonomo | `AsistenciaRepositoryPort` | `usp_registrar_asistencia_estudiante_autonomo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | `idEstudiante` resuelto desde JWT | `RegistrarAsistenciaQrRequest` |
| `POST /api/v1/asistencias/revisiones` | ESTUDIANTE | Radicar revision | `AsistenciaRepositoryPort` | `usp_radicar_solicitud_revision_asistencia` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | `idEstudiante` resuelto desde JWT | `SolicitarRevisionAsistenciaRequest` |
| `PATCH /api/v1/docente/reclamos/{id}` | DOCENTE | Resolver revision | `AsistenciaRepositoryPort` | `usp_resolver_solicitud_revision_asistencia` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | `idDocente` resuelto desde JWT | `ResolverReclamoRequest` |
| `GET /api/v1/sesiones/{sesionId}/qr-token` | DOCENTE | Exponer codigo QR/PIN |  | `uv_sesion.codigo` | READY_WITH_DB_LIMITATION | Pendiente definir respuesta publica para codigo administrado por DB | `FeatureUnavailableException` |
| `GET /api/v1/realtime/stream?grupoId={UUID}` | Autenticado + titularidad docente en gateway | Realtime scopeado por grupo | `InstitutionalScopePort` (via `RealtimeStreamGateway`) |  | IMPLEMENTATION_AVAILABLE_PENDING_E2E | `grupoId` obligatorio; titularidad validada una vez al suscribirse, filtrado en memoria | SSE (`ASISTENCIAS_SESION_ACTUALIZADAS`) |

| `GET /api/v1/grupos/{grupoId}/asistencias?sesionId={UUID}` | DOCENTE / COORDINADOR / ADMINISTRADOR | Consultar asistencias por grupo | `AsistenciaRepositoryPort`, `InstitutionalScopePort` | `uv_detalle_asistencia`, `uv_asistencia`, `uv_estudiante_grupo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Titularidad si usuario resuelve docente; sesionId opcional | `ApiListResponse<AsistenciaConsultadaDTO>` |
| `POST /api/v1/asistencias/consultas/grupo` | DOCENTE / COORDINADOR / ADMINISTRADOR | Consulta legacy | mismos puertos de consulta | mismas views | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Compatibilidad; mismo caso de uso | `ConsultarAsistenciasPorGrupoRequest` |
| `POST /api/v1/asistencias` | DOCENTE | Registro individual | `AsistenciaRepositoryPort` | sin command individual inequívoco | BLOCKED_BY_DB | Adapter lanza `FeatureUnavailableException` | `RegistrarAsistenciaRequest` |
| `GET /api/v1/realtime/status` | AUTENTICADO | Estado local | gateway interno | no aplica | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Contador por JVM | `ApiDataResponse`, activeSubscribers/status |
| `POST /api/v1/realtime/emit` | ADMINISTRADOR | Diagnóstico | gateway interno | no aplica | IMPLEMENTATION_AVAILABLE_PENDING_E2E | TD-018, no caso de uso productivo | `ApiDataResponse` |

## Evidencia del Golden Path

El [Golden Path](../baseline/GOLDEN_PATH_ASISTENCIA.md) enlaza controllers, DTO, use cases, puertos, adapter, gateway e IT. [REALTIME_EVENT_STANDARD](REALTIME_EVENT_STANDARD.md) contiene el evento. No se agrega paginación, sort ni formato temporal a partir de un objetivo futuro.

## Inventario exacto de mappings de controllers

Extraído de annotations presentes; excluye endpoints de framework (Actuator). Sirve como evidencia de rutas, no como contrato de éxito/seguridad. El símbolo enlazado permite inspeccionar request/response e Input Ports, y seguir sus casos de uso/adapters. Los handlers con `FeatureUnavailableException` continúan siendo rutas existentes no operativas; no se eliminan del inventario.

| Método | Path observado | Controller fuente |
|---|---|---|
| GET | `/api/v1/admin/decanos` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/admin/decanos` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PATCH | `/api/v1/admin/decanos/{id}/toggle` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| GET | `/api/v1/admin/sedes` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/admin/sedes` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PUT | `/api/v1/admin/sedes/{id}` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PATCH | `/api/v1/admin/sedes/{id}/estado` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| GET | `/api/v1/admin/espacios-fisicos` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/admin/espacios-fisicos` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PUT | `/api/v1/admin/espacios-fisicos/{id}` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PATCH | `/api/v1/admin/espacios-fisicos/{id}/estado` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| GET | `/api/v1/admin/parametros` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PUT | `/api/v1/admin/parametros/{id}` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| GET | `/api/v1/admin/auditoria` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/admin/cierre-masivo` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| GET | `/api/v1/admin/instituciones` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/admin/instituciones` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PUT | `/api/v1/admin/instituciones/{id}` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PATCH | `/api/v1/admin/instituciones/{id}/toggle-estado` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| GET | `/api/v1/admin/facultades` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/admin/facultades` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PUT | `/api/v1/admin/facultades/{id}` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PATCH | `/api/v1/admin/facultades/{id}/estado` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| GET | `/api/v1/admin/areas` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/admin/areas` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PUT | `/api/v1/admin/areas/{id}` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| PATCH | `/api/v1/admin/areas/{id}/estado` | [AdminPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java) |
| POST | `/api/v1/archivos/subir` | [ArchivoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/archivo/ArchivoController.java) |
| GET | `/api/v1/archivos/{nombreArchivo:.+}` | [ArchivoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/archivo/ArchivoController.java) |
| POST | `/api/v1/asistencias` | [AsistenciaController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaController.java) |
| POST | `/api/v1/asistencias/revisiones` | [AsistenciaController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaController.java) |
| POST | `/api/v1/asistencias/lote` | [AsistenciaController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaController.java) |
| GET | `/api/v1/sesiones/{sesionId}/qr-token` | [AsistenciaQrController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaQrController.java) |
| POST | `/api/v1/estudiante/asistencia-qr` | [AsistenciaQrController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaQrController.java) |
| GET | `/api/v1/grupos/{grupoId}/asistencias` | [AsistenciaQueryController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaQueryController.java) |
| POST | `/api/v1/asistencias/consultas/grupo` | [AsistenciaQueryController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaQueryController.java) |
| GET | `/api/v1/coordinador/docentes` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| POST | `/api/v1/coordinador/docentes` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PATCH | `/api/v1/coordinador/docentes/{id}/estado` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| GET | `/api/v1/coordinador/planes-estudio` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| POST | `/api/v1/coordinador/planes-estudio` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PUT | `/api/v1/coordinador/planes-estudio/{id}` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PATCH | `/api/v1/coordinador/planes-estudio/{id}/toggle-estado` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| POST | `/api/v1/coordinador/planes-estudio/{id}/semestres` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| DELETE | `/api/v1/coordinador/planes-estudio/{id}/semestres/{numero}` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| GET | `/api/v1/coordinador/planes-estudio/{id}/asignaturas` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| POST | `/api/v1/coordinador/planes-estudio/{id}/asignaturas` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PUT | `/api/v1/coordinador/planes-estudio/{planId}/asignaturas/{asigId}` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| DELETE | `/api/v1/coordinador/planes-estudio/{planId}/asignaturas/{asigId}` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| GET | `/api/v1/coordinador/asignaturas` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PATCH | `/api/v1/coordinador/asignaturas/{id}/estado` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| GET | `/api/v1/coordinador/periodos-academicos` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| POST | `/api/v1/coordinador/periodos-academicos` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PUT | `/api/v1/coordinador/periodos-academicos/{id}` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PATCH | `/api/v1/coordinador/periodos-academicos/{id}/estado` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| GET | `/api/v1/coordinador/estudiantes` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| GET | `/api/v1/coordinador/solicitudes-matricula` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| PATCH | `/api/v1/coordinador/solicitudes-matricula/{id}` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| POST | `/api/v1/coordinador/grupos/{grupoId}/estudiantes` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| DELETE | `/api/v1/coordinador/grupos/{grupoId}/estudiantes/{estudianteId}` | [CoordinadorPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java) |
| GET | `/api/v1/decano/coordinadores` | [DecanoPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/decano/DecanoPortalController.java) |
| PATCH | `/api/v1/decano/coordinadores/{id}/estado` | [DecanoPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/decano/DecanoPortalController.java) |
| POST | `/api/v1/decano/coordinadores` | [DecanoPortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/decano/DecanoPortalController.java) |
| GET | `/api/v1/docentes` | [DocenteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocenteController.java) |
| GET | `/api/v1/docentes/{docenteId}` | [DocenteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocenteController.java) |
| POST | `/api/v1/docentes/consultas/id` | [DocenteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocenteController.java) |
| GET | `/api/v1/docentes/{docenteId}/asignaciones` | [DocenteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocenteController.java) |
| POST | `/api/v1/docentes/consultas/asignaciones` | [DocenteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocenteController.java) |
| POST | `/api/v1/docentes` | [DocenteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocenteController.java) |
| POST | `/api/v1/docentes/asignaciones/grupo` | [DocenteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocenteController.java) |
| GET | `/api/v1/docente/reclamos` | [DocentePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocentePortalController.java) |
| PATCH | `/api/v1/docente/reclamos/{id}` | [DocentePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocentePortalController.java) |
| GET | `/api/v1/docente/horarios` | [DocentePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocentePortalController.java) |
| PATCH | `/api/v1/docente/sesiones/{sesionId}/cancelar` | [DocentePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocentePortalController.java) |
| GET | `/api/v1/docente/asignaturas` | [DocentePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/DocentePortalController.java) |
| GET | `/api/v1/estudiantes` | [EstudianteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudianteController.java) |
| GET | `/api/v1/estudiantes/{estudianteId}` | [EstudianteController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudianteController.java) |
| GET | `/api/v1/estudiante/materias` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| GET | `/api/v1/estudiante/horarios` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| GET | `/api/v1/estudiante/materias/{materiaId}/sesiones` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| GET | `/api/v1/estudiante/materias/{materiaId}/prerrequisitos` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| GET | `/api/v1/estudiante/reclamos` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| POST | `/api/v1/estudiante/reclamos` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| DELETE | `/api/v1/estudiante/reclamos/{id}` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| POST | `/api/v1/estudiante/solicitudes-matricula` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| POST | `/api/v1/estudiante/matricular-grupo` | [EstudiantePortalController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/estudiante/EstudiantePortalController.java) |
| GET | `/api/v1/grupos` | [GrupoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/grupo/GrupoController.java) |
| POST | `/api/v1/grupos` | [GrupoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/grupo/GrupoController.java) |
| PUT | `/api/v1/grupos/{id}` | [GrupoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/grupo/GrupoController.java) |
| GET | `/api/v1/grupos/{grupoId}/estudiantes` | [GrupoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/grupo/GrupoController.java) |
| POST | `/api/v1/grupos/{grupoId}/estudiantes` | [GrupoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/grupo/GrupoController.java) |
| DELETE | `/api/v1/grupos/{grupoId}/estudiantes/{estudianteId}` | [GrupoController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/grupo/GrupoController.java) |
| GET | `/api/v1/grupos/{grupoId}/reportes/asistencia-excel` | [ReporteAsistenciaController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/reporte/ReporteAsistenciaController.java) |
| POST | `/api/v1/sesiones` | [SesionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/sesion/SesionController.java) |
| GET | `/api/v1/sesiones/grupo/{grupoId}` | [SesionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/sesion/SesionController.java) |
| POST | `/api/v1/sesiones/grupo/{grupoId}/generacion` | [SesionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/sesion/SesionController.java) |
| GET | `/api/v1/sesiones/{sesionId}` | [SesionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/sesion/SesionController.java) |
| POST | `/api/v1/sesiones/consultas` | [SesionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/sesion/SesionController.java) |
| POST | `/api/v1/sesiones/cierres` | [SesionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/sesion/SesionController.java) |
| PUT | `/api/v1/sesiones/{sesionId}` | [SesionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/sesion/SesionController.java) |
| GET | `/api/v1/tipos-identificacion` | [TipoIdentificacionController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/tipoidentificacion/TipoIdentificacionController.java) |
| GET | `/api/v1/usuarios/perfil` | [UsuarioController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/usuario/UsuarioController.java) |
| PUT | `/api/v1/usuarios/perfil` | [UsuarioController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/usuario/UsuarioController.java) |
| POST | `/api/v1/usuarios` | [UsuarioController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/usuario/UsuarioController.java) |
| GET | `/api/v1/realtime/stream` | [RealtimeEventsController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeEventsController.java) |
| GET | `/api/v1/realtime/status` | [RealtimeEventsController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeEventsController.java) |
| POST | `/api/v1/realtime/emit` | [RealtimeEventsController](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeEventsController.java) |
