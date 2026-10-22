# Contrato Técnico (SDD): HU076 - Agregar nuevo programa académico a la facultad

## 1. Contrato de Base de Datos (SQL Server 2022)

### 1.1 Vistas de Lectura (`dbo.uv_programa` y `dbo.uv_auth_programa`)
Se preservan todas las columnas existentes en su orden original y se agregan al final los metadatos del tipo de programa mediante `LEFT JOIN dbo.uv_tipo_programa`:
- `id` (`UNIQUEIDENTIFIER`)
- `codigo` (`INT`)
- `nombrePrograma` (`VARCHAR(50)`)
- `idFacultad` (`UNIQUEIDENTIFIER`)
- `nombreFacultad` (`VARCHAR(50)`)
- `idCoordinador` (`UNIQUEIDENTIFIER`)
- `idUsuarioCoordinador` (`UNIQUEIDENTIFIER`)
- `nombreCoordinador` (`VARCHAR(203)`)
- `correoCoordinador` (`VARCHAR(150)`)
- `estaActivoCoordinador` (`BIT`)
- `idTipoPrograma` (`UNIQUEIDENTIFIER`)
- `nombreTipoPrograma` (`VARCHAR(50)`)

### 1.2 Procedimiento Almacenado Público (`dbo.usp_crear_programa_academico`)
- **Firma**:
  ```sql
  CREATE OR ALTER PROCEDURE [dbo].[usp_crear_programa_academico]
      @idPrograma UNIQUEIDENTIFIER = NULL,
      @codigo INT = NULL,
      @nombre VARCHAR(50),
      @idCoordinador UNIQUEIDENTIFIER,
      @idFacultad UNIQUEIDENTIFIER,
      @idTipoPrograma UNIQUEIDENTIFIER,
      @idCorrelacion UNIQUEIDENTIFIER,
      @idUsuarioEjecutor UNIQUEIDENTIFIER = NULL
  ```
- **Reglas de Negocio y Seguridad**:
  1. Validación de `@idCorrelacion` vía `dbo.usp_validar_id_correlacion_esta_presente_interno`.
  2. Validación *Fail-Closed* de `@idUsuarioEjecutor IS NULL` -> `GEN_002` (`idUsuarioEjecutor`).
  3. Validación RBAC vía `dbo.usp_validar_permiso_rbac_usuario_interno` exigiendo perfil `'DECANO'`.
  4. Validación de `@nombre` (`LEN(LTRIM(RTRIM(@nombre))) BETWEEN 3 AND 50`) -> `GEN_002` (`Nombre de Programa Academico`).
  5. Validación de existencia de `@idFacultad` en `dbo.uv_facultad` -> `GEN_001` (`Facultad`).
  6. Validación de titularidad jerárquica vía `dbo.usp_validar_titularidad_jerarquica_interno` (`@tipoEntidadPadre = 'FACULTAD'`, `@idEntidadPadre = @idFacultad`).
  7. Validación de presencia (`GEN_002`) y existencia (`GEN_001`) de `@idTipoPrograma` en `dbo.uv_tipo_programa`.
  8. Validación de presencia (`GEN_002`) y existencia (`GEN_001`) de `@idCoordinador` en `dbo.uv_coordinador_identidad`.
  9. Validación de unicidad de nombre en `dbo.uv_programa` (`LOWER(nombrePrograma) = LOWER(@nombreDef) AND id <> @idProgramaDefecto`) -> `GEN_003` (`Programa Academico`, `nombre`).
- **Result Set Estático (4 Columnas Estándar)**:
  - `idCorrelacion` (`UNIQUEIDENTIFIER`)
  - `mensajeUsuarioResultado` (`NVARCHAR(255)`)
  - `mensajeTecnicoResultado` (`NVARCHAR(500)`) — incluye `idPrograma=<UUID>` en caso de éxito (`PROG_001`)
  - `estadoResultado` (`BIT`)

## 2. Contrato de API REST (Spring Boot 3 / Java 21)

### 2.1 `GET /api/v1/decano/programas`
- **Seguridad**: Rol `DECANO`. Resuelve automáticamente la facultad titular del Decano autenticado (`RoleScopeResolutionPort.findFacultadIdByDecanoUsuario`).
- **Respuesta (`200 OK`)**: `ApiListResponse<ProgramaDTO>`
  ```json
  {
    "exito": true,
    "datos": [
      {
        "id": "uuid",
        "codigo": 101,
        "nombre": "Ingeniería de Sistemas",
        "idFacultad": "uuid",
        "nombreFacultad": "Ingenieria",
        "idCoordinador": "uuid",
        "nombreCoordinador": "Coordinador Sistemas",
        "correoCoordinador": "coord.sistemas@uco.edu.co",
        "idTipoPrograma": "uuid",
        "nombreTipoPrograma": "pregrado"
      }
    ],
    "total": 1
  }
  ```

### 2.2 `GET /api/v1/decano/tipos-programa`
- **Seguridad**: Rol `DECANO`.
- **Respuesta (`200 OK`)**: `ApiListResponse<TipoProgramaDTO>`
  ```json
  {
    "exito": true,
    "datos": [
      {
        "id": "b1a746c7-7069-4138-91dc-0a78201e7f01",
        "nombre": "pregrado"
      },
      {
        "id": "b1a746c7-7069-4138-91dc-0a78201e7f02",
        "nombre": "postgrado"
      }
    ],
    "total": 2
  }
  ```

### 2.3 `POST /api/v1/decano/programas`
- **Seguridad**: Rol `DECANO`.
- **Request Body (`CrearProgramaRequest`)**:
  ```json
  {
    "nombre": "Ingeniería de Software",
    "idTipoPrograma": "b1a746c7-7069-4138-91dc-0a78201e7f01",
    "idCoordinador": "ea5742c7-7069-4138-91dc-0a78201e7f01",
    "codigo": 205
  }
  ```
- **Respuesta Exitosa (`201 CREATED`)**: `ApiDataResponse<CrearProgramaDTO>`
- **Errores Controlados**:
  - `400 BAD_REQUEST` (`GEN_002` / `GEN_001`): Campos inválidos, longitud fuera de rango (`3..50`), o referencias inexistentes.
  - `403 FORBIDDEN` (`RBAC_001` / `SCOPE_001`): Actor sin rol Decano o sin facultad asignada.
  - `409 CONFLICT` (`GEN_003`): Nombre de programa académico duplicado.

## 3. Contrato Frontend (Angular 19)

- **Modelos (`role-management.model.ts`)**:
  - `ProgramaAcademicoItem`: `{ id, codigo, nombre, idFacultad, nombreFacultad, idCoordinador, nombreCoordinador, correoCoordinador, idTipoPrograma, nombreTipoPrograma }`
  - `TipoProgramaItem`: `{ id, nombre }`
  - `CreateProgramaPayload`: `{ nombre: string; idTipoPrograma: string; idCoordinador: string; codigo?: number }`
- **Servicio (`DeanManagementService`)**:
  - `getProgramas(): Observable<ProgramaAcademicoItem[]>`
  - `getTiposPrograma(): Observable<TipoProgramaItem[]>`
  - `createPrograma(payload: CreateProgramaPayload): Observable<ProgramaAcademicoItem>`
