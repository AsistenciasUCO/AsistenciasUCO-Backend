---
status: active
type: contract-spec
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# CONTRACT — HU050.2: Contratos de Invocación JPA de SPs Paginados

Fase 02-contratos.

## 1. Contratos de Base de Datos Involucrados

Los procedimientos almacenados invocados son de tipo READ_QUERY_RESULTSET (conjunto de filas de dominio + 	otalRegistros). No forman parte de comandos de mutación canónicos de 4 columnas.

### 1.1 dbo.usp_consultar_grupos_paginado
- **Parámetros (sys.parameters.parameter_id):**
  1. @idUsuarioEjecutor (UNIQUEIDENTIFIER)
  2. @idCorrelacion (UNIQUEIDENTIFIER)
  3. @idPeriodoAcademico (UNIQUEIDENTIFIER = NULL)
  4. @idAsignatura (UNIQUEIDENTIFIER = NULL)
  5. @filtroTexto (NVARCHAR(100) = NULL)
  6. @numeroPagina (INT = 1)
  7. @tamanoPagina (INT = 10)
- **Columnas de Resultado:**
  id, codigo, 
ombre, idAsignatura, 
ombreAsignatura, idDocente, capacidadMaximaPermitida, estudiantesActivos, cuposDisponibles, grupoEstaHablitado, echaInicioPeriodoAcademico, echaFinPeriodoAcademico, 	otalRegistros.
- **Mapeo Target:** GrupoRepositoryProjection.

### 1.2 dbo.usp_consultar_estudiantes_grupo_paginado
- **Parámetros (sys.parameters.parameter_id):**
  1. @idUsuarioEjecutor (UNIQUEIDENTIFIER)
  2. @idCorrelacion (UNIQUEIDENTIFIER)
  3. @idGrupo (UNIQUEIDENTIFIER)
  4. @filtroTexto (NVARCHAR(100) = NULL)
  5. @idEstadoEstudiante (UNIQUEIDENTIFIER = NULL)
  6. @numeroPagina (INT = 1)
  7. @tamanoPagina (INT = 10)
- **Columnas de Resultado (20):**
  idEstudianteGrupo, idEstudiante, idUsuario, 	ipoIdentificacionId, 
umeroIdentificacion, primerApellido, segundoApellido, primerNombre, segundoNombre, 
ombreCompleto, correo, idEstadoEstudiante, codigoEstadoEstudiante, 
ombreEstadoEstudiante, estaActivoUsuario, 	otalFallas, 	otalJustificadas, 	otalAsistencias, 	otalRegistrosAsistencia, 	otalRegistros.
- **Mapeo Target:** EstudianteGrupoRepositoryProjection.

### 1.3 dbo.usp_consultar_sesiones_grupo_paginado
- **Parámetros (sys.parameters.parameter_id):**
  1. @idUsuarioEjecutor (UNIQUEIDENTIFIER)
  2. @idCorrelacion (UNIQUEIDENTIFIER)
  3. @idGrupo (UNIQUEIDENTIFIER)
  4. @fechaDesde (DATETIME2 = NULL)
  5. @fechaHasta (DATETIME2 = NULL)
  6. @numeroSemana (INT = NULL)
  7. @numeroPagina (INT = 1)
  8. @tamanoPagina (INT = 10)
- **Columnas de Resultado (11):**
  id, idGrupo, codigoGrupo, 
ombreGrupo, 
ombre, 
umero, codigo, 
umeroSemana, echaHoraInicio, echaHoraFin, 	otalRegistros.
- **Mapeo Target:** SesionRepositoryProjection.
