# CONTRACT SPECIFICATION — HU050: registrarEstudianteEnGrupo

## 1. Identidad y Autoridad
- **Historia de Usuario:** HU050
- **Work Item:** HU050-matricula-manual-docente
- **Estado:** FORMALIZED (CONTRACT_FROZEN)
- **Ámbito:** Persistencia (GestioAsistenciaBackend) y Frontend (GestioAsistenciaFrontend).

---

## 2. Definición Contractual del Procedimiento Almacenado

### 2.1 Nombre Canónico
`dbo.usp_registrar_estudiante_en_grupo`

### 2.2 Parámetros del Procedimiento
| Parámetro | Tipo SQL | Modo | Obligatorio | Descripción |
|---|---|---|---|---|
| `@idGrupo` | UNIQUEIDENTIFIER | IN | SÍ | ID del grupo al cual se inscribe el estudiante. |
| `@numeroIdentificacion` | INT | IN | SÍ | Documento de identidad del alumno. |
| `@primerNombre` | NVARCHAR(50) | IN | SÍ | Primer nombre. |
| `@segundoNombre` | NVARCHAR(50) | IN | NO (NULL) | Segundo nombre opcional. |
| `@primerApellido` | NVARCHAR(50) | IN | SÍ | Primer apellido. |
| `@segundoApellido` | NVARCHAR(50) | IN | NO (NULL) | Segundo apellido opcional. |
| `@correo` | NVARCHAR(100) | IN | SÍ | Correo electrónico institucional. |
| `@password` | NVARCHAR(500) | IN | SÍ | Hash de contraseña para usuarios nuevos. |
| `@idCorrelacion` | UNIQUEIDENTIFIER | IN | SÍ | Trazabilidad distribuida W3C. |
| `@idUsuarioEjecutor` | UNIQUEIDENTIFIER | IN | SÍ | ID del docente/usuario autenticado que ejecuta la matrícula. |
| `@idTipoIdIdentificacion` | UNIQUEIDENTIFIER | IN | NO (NULL) | UUID del tipo de identificación en `dbo.TipoIdentificacion` (default 'CC'). |

### 2.3 Respuesta Canónica de Persistencia (4 Columnas)
Retorna exactamente un ResultSet con:
- `idCorrelacion` (UNIQUEIDENTIFIER)
- `mensajeUsuarioResultado` (NVARCHAR)
- `mensajeTecnicoResultado` (NVARCHAR)
- `estadoResultado` (BIT / Boolean): 1 para éxito, 0 para error.

---

## 3. Contrato de la API HTTP (Backend ↔ Frontend)

### 3.1 Endpoint
`POST /api/v1/grupos/{grupoId}/estudiantes`
- **Roles Autorizados:** `DOCENTE`, `COORDINADOR`, `ADMINISTRADOR`.
- **Payload (`RegistrarEstudianteRequest`):**
  - `tipoIdentificacionId`: UUID (obligatorio).
  - `numeroIdentificacion`: Integer (obligatorio).
  - `primerNombre`: String (obligatorio).
  - `segundoNombre`: String (opcional).
  - `primerApellido`: String (obligatorio).
  - `segundoApellido`: String (opcional).
  - `correo`: String (obligatorio).
  - `password`: String (opcional).
  - `estudianteId`: UUID (opcional si ya está registrado en UCO).

### 3.2 Respuesta
- HTTP Status: `201 Created`
- Body: `RegistrarEstudianteResultadoDTO` `{ mensaje: string }`
