---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# CONTRACT — HU061 / HU147 / HU153 / HU172 y P5

Listas: `ApiListResponse` `{ exitoso, datos[], total }`; registros: `ApiDataResponse` `{ exitoso, datos }`. Paginación `pagina` (≥ 1, por defecto 1) y `tamano` (1..100, por defecto 10).

| Método y endpoint | Roles (SecurityConfig / SP) | Parámetros | SP |
|---|---|---|---|
| `GET /api/v1/coordinador/consolidado-asistencia` | `/coordinador/**` · SP: `COORDINADOR`, `DECANO`, `ADMINISTRADOR` | `idPeriodoAcademico`, `idPrograma`, `idAsignatura`, `idDocente`, paginación | `usp_consultar_consolidado_asistencia_grupos_paginado` |
| `GET /api/v1/decano/consolidado-asistencia` | `/decano/**` · mismo SP | igual | igual |
| `GET /api/v1/admin/auditoria` | `/admin/**` · SP: `ADMINISTRADOR` | `desde`, `hasta` (ISO-8601 con zona), `accion`, `tipoRecurso`, `idRecurso`, `idUsuarioActor`, `resultado` (`SUCCESS`/`FAILURE`), `soloAsistencia` (por defecto `true`), paginación | `usp_consultar_auditoria_eventos_paginado` |
| `GET /api/v1/coordinador/componentes-formacion` | `/coordinador/**` · SP: `ADMINISTRADOR`, `DECANO`, `COORDINADOR` | — | `usp_consultar_componentes_formacion` |
| `POST /api/v1/admin/tipos-identificacion` | `/admin/**` · SP: `ADMINISTRADOR` | cuerpo `{ codigo, nombre }` → 201 con el tipo creado | `usp_guardar_tipo_identificacion` |
| `PUT /api/v1/admin/tipos-identificacion/{id}` | `/admin/**` · SP: `ADMINISTRADOR` | cuerpo `{ nombre }` → 200 con el tipo releído | `usp_guardar_tipo_identificacion` |
| `POST`/`PUT /api/v1/coordinador/planes-estudio/{id}/asignaturas…` (sin cambio de ruta) | sin cambio | cuerpo `{ codigo, nombre, creditos, semestreNumero, nombreArea, nombreComponente }` | `usp_crear_asignatura`, `usp_actualizar_asignatura` |

Ámbito del consolidado (forzado por el SP con `uv_auth_grupo`): el coordinador ve su programa, el decano su facultad y el administrador su institución.

## Elementos

- **Grupo consolidado:** `idGrupo, codigoGrupo, nombreGrupo, idPeriodoAcademico, nombrePeriodoAcademico, idPrograma, nombrePrograma, idAsignatura, nombreAsignatura, idDocente, nombreDocente, estudiantesActivos, sesionesEjecutadas, asistencias, faltasSinJustaCausa, excusas, porcentajeAsistencia, porcentajeInasistencia, estudiantesRiesgo, estudiantesPerdida`. Los porcentajes son sobre sesiones ejecutadas × estudiantes activos; un grupo sin estudiantes trae ceros.
- **Evento de auditoría:** `id, ocurridoEn, idActor, tipoActor, nombreActor, accion, tipoRecurso, idRecurso, resultado, idTraza, metodoHttp, ruta, estadoHttp, ipCliente, codigoError, metadatos, accionAsistencia`. Orden: más reciente primero.
- **Componente:** `idComponente, codigo, nombre, totalAsignaturas`.
- **Tipo de documento:** `id, tipoIdentificacion, nombre` (mismo DTO de `GET /api/v1/tipos-identificacion`).

## Reglas

- **Auditoría:** sin `hasta` se usa el instante actual; sin `desde`, 7 días antes de `hasta`. `desde` debe ser anterior a `hasta` y el rango no puede superar 90 días. `accion` se normaliza a mayúsculas; los filtros de texto se recortan y tienen máximo 120 caracteres.
- **Tipo de documento:** alta con código de 1 a 5 letras (se guarda en mayúsculas) y nombre de 3 a 50 caracteres; en la edición el código no se envía ni cambia.
- **Asignatura:** créditos obligatorios y > 0 al crear (al editar, nulo conserva el actual); área y componente por código o nombre exactos; semestre del plan; plan del programa del coordinador.

## Errores

| Caso | HTTP |
|---|---|
| Usuario sin el perfil del SP (`SEC_001`), plan de otro programa (`SEC_002`) o no autenticado | 403 |
| Paginación fuera de rango, rango de auditoría inválido, resultado fuera de contrato, código o nombre de tipo inválidos, créditos ≤ 0 (`ASI_001`), `GEN_002`, UUID o fecha inválidos | 400 |
| Área o componente inexistentes al guardar una asignatura, tipo de documento inexistente (`GEN_001`), plan inexistente (`PLA_002`), semestre fuera del plan (`PLA_004`) | 404 |
| Código o nombre de tipo de documento repetidos (`GEN_003`) | 409 |
| Fallo técnico de la DB | 500 |
