# Contrato Técnico (SDD): HU062 / HU089 & HU091 — Directorio y Vinculación de Docentes en Coordinación

## 1. Alcance y Trazabilidad
- **HU062 / HU089**: Consultar directorio de profesores adscritos al programa y carga académica (`GET /api/v1/coordinador/docentes`).
- **HU091**: Vincular nuevo docente al programa desde Coordinación y sincronizar cuenta IAM en Keycloak (`POST /api/v1/coordinador/docentes`).
- **Custodia Jerárquica**: Exclusivo del rol `COORDINADOR` autenticado y titular de un Programa Académico (`InstitutionalScopePort.findProgramaIdByCoordinadorUsuario`).

---

## 2. Contrato de Base de Datos (SQL Server 2022+)

### 2.1 Procedimiento Almacenado Público (`dbo.usp_crear_docente`)
- **Firma**:
  ```sql
  CREATE OR ALTER PROCEDURE [dbo].[usp_crear_docente]
  (
      @idDocente               UNIQUEIDENTIFIER = NULL,
      @numeroIdentificacion    INT,
      @primerNombre            NVARCHAR(50),
      @segundoNombre           NVARCHAR(50)     = NULL,
      @primerApellido          NVARCHAR(50),
      @segundoApellido         NVARCHAR(50)     = NULL,
      @correo                  NVARCHAR(100),
      @idPrograma              UNIQUEIDENTIFIER,
      @password                NVARCHAR(500),
      @idCorrelacion           UNIQUEIDENTIFIER,
      @idUsuarioEjecutor       UNIQUEIDENTIFIER = NULL
  )
  ```
- **Reglas de Negocio, Seguridad y Atomicidad**:
  1. Validación de presencia de `@idCorrelacion` vía `dbo.usp_validar_id_correlacion_esta_presente_interno`.
  2. Validación *Fail-Closed* de `@idUsuarioEjecutor IS NULL` -> `GEN_002` (`idUsuarioEjecutor`).
  3. Validación RBAC vía `dbo.usp_validar_permiso_rbac_usuario_interno` exigiendo `@codigoPerfilRequerido = 'COORDINADOR'`.
  4. Validación de existencia de `@idPrograma` en `dbo.uv_programa` -> `PROG_001` (`Programa`).
  5. Validación de titularidad jerárquica vía `dbo.usp_validar_titularidad_jerarquica_interno` (`@tipoEntidadPadre = 'PROGRAMA'`, `@idEntidadPadre = @idPrograma`).
  6. Control estricto de *Transaction Ownership* (`@@TRANCOUNT` inicial, `BEGIN TRANSACTION` o `SAVE TRANSACTION usp_crear_docente`).
  7. Gestión reactiva de identidad en `dbo.uv_usuario` (búsqueda por `correo` o `numeroIdentificacion`; si no existe, invoca `dbo.usp_sincronizar_usuario_interno` con tipo de documento `CC`).
  8. Registro idempotente en `dbo.Docente (id, usuario)` (si el usuario aún no está en `dbo.Docente`, inserta con `@idDocente` o `NEWID()`).
  9. Mensaje de éxito `GEN_004` (`Docente`).
- **Result Set Estático Canónico (4 Columnas)**:
  1. `idCorrelacion` (`UNIQUEIDENTIFIER`)
  2. `mensajeUsuarioResultado` (`NVARCHAR(4000)`)
  3. `mensajeTecnicoResultado` (`NVARCHAR(4000)`)
  4. `estadoResultado` (`BIT`)

---

## 3. Contrato de API REST (Spring Boot 4.0.6 / Java 25)

### 3.1 `GET /api/v1/coordinador/docentes`
- **Seguridad**: Rol `COORDINADOR`. Resuelve automáticamente el programa del Coordinador autenticado (`InstitutionalScopePort.findProgramaIdByCoordinadorUsuario`).
- **Respuesta (`200 OK`)**: `ApiListResponse<DocenteProgramaDTO>`
  ```json
  {
    "exitoso": true,
    "datos": [
      {
        "id": "uuid",
        "idUsuario": "uuid",
        "numeroIdentificacion": "1017001",
        "nombreCompleto": "Pedro Pérez",
        "correo": "pperez@uco.edu.co",
        "idPrograma": "uuid",
        "nombrePrograma": "Ingeniería de Sistemas",
        "totalGruposAsignados": 2,
        "estaActivoDocente": true
      }
    ],
    "total": 1
  }
  ```

### 3.2 `POST /api/v1/coordinador/docentes`
- **Seguridad**: Rol `COORDINADOR`.
- **Request Body (`CrearDocenteRequest`)**:
  ```json
  {
    "numeroIdentificacion": "1017888777",
    "primerNombre": "Esteban",
    "segundoNombre": "",
    "primerApellido": "Quintero",
    "segundoApellido": "",
    "correo": "esteban.quintero@uco.edu.co",
    "password": "OpcionalPassword123!"
  }
  ```
- **Comportamiento**:
  1. Resuelve `idPrograma` desde `InstitutionalScopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId)` (`403 FORBIDDEN` si no tiene programa asignado).
  2. Codifica credencial inicial (o genera credencial institucional por defecto `UcoDocente_<numeroId>!` si no se envía `password` explícito) cuando el usuario no existe aún en base de datos.
  3. Ejecuta `DocenteProgramaCommandPort.crearDocente(...)` (`EXEC dbo.usp_crear_docente`).
  4. Sincroniza la cuenta en Keycloak mediante `IdentityProviderPort.crearCuenta` con `InstitutionalRole.DOCENTE`.
- **Respuesta Exitosa (`200 OK`)**: `ApiDataResponse<Void>` (`{ "exitoso": true, "datos": null }`).

---

## 4. Contrato Frontend (Angular 19 / Signals)
- **Servicio (`CoordinatorManagementService`)**:
  - `getDocentes(): Observable<ApiResponse<DocenteItem[]>>`: Consume `GET /api/v1/coordinador/docentes`, deduplica determinísticamente por `item.id` usando `Map<string, any>` y normaliza `nombres`, `apellidos`, `nombreCompleto`, `numeroIdentificacion`, `correo`, `departamento`, `especialidad`, `totalGruposAsignados` y `estado` (`ACTIVO` / `INACTIVO`).
  - `createDocente(nuevoDocente)`: Consume `POST /api/v1/coordinador/docentes`.
- **Componente (`CoordinatorDocentesComponent`)**:
  - Mantiene `ChangeDetectionStrategy.OnPush` y señales nativas (`signal`, `computed`).
  - Al registrar o vincular un docente exitosamente, recarga o actualiza la lista reactiva de docentes.
