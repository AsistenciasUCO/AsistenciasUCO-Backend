---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-02
---

# CONTRACT — HU112 / HU128 / HU143 / HU157

Todos los endpoints son `GET` y de solo lectura. Listas: `ApiListResponse` `{ exitoso, datos[], total }`; ficha: `ApiDataResponse` `{ exitoso, datos }`. Paginación `pagina` (≥ 1, por defecto 1) y `tamano` (1..100, por defecto 10). `busqueda` ≤ 100 caracteres; se recorta y, si queda vacía, se ignora.

| Endpoint | Roles (SecurityConfig / SP) | Filtros | SP |
|---|---|---|---|
| `/api/v1/decano/programas-creditos` | `/decano/**` · SP: `DECANO` | `busqueda` (nombre del programa o INP), `estaActivo` | `usp_consultar_programas_creditos_facultad_paginado` |
| `/api/v1/decano/docentes` | `/decano/**` · SP: `DECANO` | `busqueda` (nombre o documento), `idPrograma`, `estaActivo` (usuario) | `usp_consultar_docentes_facultad_paginado` |
| `/api/v1/decano/asignaturas` | `/decano/**` · SP: `ADMINISTRADOR`, `DECANO` | `busqueda` (código o nombre), `estaActiva` | `usp_buscar_asignaturas_paginado` |
| `/api/v1/admin/asignaturas` | `/admin/**` · SP: `ADMINISTRADOR`, `DECANO` | `busqueda`, `idFacultad`, `estaActiva` | `usp_buscar_asignaturas_paginado` |
| `/api/v1/admin/estudiantes` | `/admin/**` · SP: `ADMINISTRADOR` | `busqueda` (nombre o documento), `idFacultad`, `estaActivo` | `usp_buscar_estudiantes_institucion_paginado` |
| `/api/v1/admin/estudiantes/{estudianteId}/historial` | `/admin/**` · SP: `ADMINISTRADOR`, `DECANO` | — | `usp_consultar_historial_estudiante` |
| `/api/v1/decano/estudiantes/{estudianteId}/historial` | `/decano/**` · SP: `ADMINISTRADOR`, `DECANO` | — | `usp_consultar_historial_estudiante` |

Ámbito (forzado por el SP): el DECANO solo ve su facultad y el ADMINISTRADOR, su institución.

## Elementos

- **Programa y créditos (una fila por programa y plan):** `idPrograma, nombrePrograma, nombreTipoPrograma, nombreModalidad, nombreJornada, programaActivo, idPlanEstudio, inpPlanEstudio, planActivo, totalSemestres, totalAsignaturas, totalCreditos`. Un programa sin plan trae `idPlanEstudio`/`inpPlanEstudio` en `null` y los totales en 0. Los totales cuentan todas las asignaturas del plan (activas o no), igual que la malla.
- **Docente:** `idDocente, numeroIdentificacion, nombreCompleto, correo, activo, programas, totalProgramas, totalGrupos` (programas con vínculo activo en la facultad).
- **Asignatura:** `idAsignatura, codigo, nombre, creditos, nombreArea, nombreComponente, codigoSemestre, idPlanEstudio, inpPlanEstudio, idPrograma, nombrePrograma, idFacultad, nombreFacultad, activa`.
- **Estudiante (admin):** `idEstudiante, numeroIdentificacion, nombreCompleto, correo, activo, facultades, programas, totalGrupos`.
- **Ficha unificada:** `idEstudiante, tipoIdentificacion, numeroIdentificacion, nombreCompleto, correo, activo, matriculas[ { idFacultad, nombreFacultad, idPrograma, nombrePrograma, inpPlanEstudio, idPeriodoAcademico, codigoPeriodo, nombrePeriodo, idGrupo, codigoGrupo, idAsignatura, nombreAsignatura, codigoEstadoEstudiante, nombreEstadoEstudiante, resumen { sesionesEjecutadas, asistencias, faltasSinJustaCausa, excusas, porcentajeInasistencia, codigoPermanencia } } ]`. Las matrículas vienen ordenadas por período, de la más reciente a la más antigua. Una matrícula sin grupo trae el grupo, el período y `resumen` en `null`.

## Errores

| Caso | HTTP |
|---|---|
| Usuario sin el perfil que exige el SP (`SEC_001`) o no autenticado | 403 |
| Historial de un estudiante fuera del ámbito o inexistente | 404 |
| Paginación fuera de rango (caso de uso o `PAG_001`), búsqueda > 100, `VAL_001`, UUID inválido | 400 |
| Fallo técnico de la DB | 500 (`DatabaseOperationException`) |
