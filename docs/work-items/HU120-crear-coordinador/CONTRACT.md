# 📑 CONTRACT SPECIFICATION — HU120: Crear y Aprovisionar Coordinador de Programa en Facultad

> **Historia de Usuario:** HU120 (Crear y asignar Coordinador a Programa / Aprovisionamiento en Facultad)  
> **Work Item:** `HU120-crear-coordinador`  
> **Estado:** `FORMALIZED` (CONTRACT_FROZEN)  
> **Ámbito:** Base de Datos (`GestioAsistenciaDB`), Backend API (`GestioAsistenciaBackend`) y Frontend Web (`GestioAsistenciaFrontend`).  
> **Fecha de Congelamiento:** 2026-10-19  
> **Conformidad:** `CONSTITUTION.md`, `AGENTS.md` y `GUIA_METODICA_IMPLEMENTACION_HU.md`.

---

## 🏛️ 1. Principios de Congelamiento y Reglas Inviolables

1. **Tupla Transaccional Canónica (Persistencia):** Toda mutación en SQL Server retorna la tupla estándar de 4 columnas (`idResultado`, `exitoso`, `mensajeUsuario`, `mensajeTecnico`) / compatible con `ProcedureResultMapper`.
2. **Control Transaccional y Reglas T-SQL:**
   - Obligatorio `SET XACT_ABORT ON;` y `SET NOCOUNT ON;`.
   - Cero `ISNULL` / `COALESCE` en asignación de parámetros de entrada de SPs públicos; evaluación explícita con `IS NULL`.
   - Cero `SELECT` directo a tablas físicas en SPs públicos; las lecturas se realizan exclusivamente sobre vistas (`dbo.uv_*`).
3. **Resolución de Candado Circular (`dbo.Programa.[coordinador] NOT NULL`):**
   - `dbo.usp_crear_coordinador` permite `@idPrograma = NULL` para aprovisionar coordinadores en una facultad recién creada que aún no posee programas académicos.
   - `dbo.uv_coordinador` emplea `LEFT JOIN uv_programa` para exponer coordinadores disponibles (`idPrograma IS NULL`).
4. **Aprovisionamiento IAM Institucional:**
   - Todo coordinador registrado se aprovisiona en Keycloak vía `IdentityProviderPort.crearCuenta(...)` con rol `InstitutionalRole.COORDINADOR`.

---

## 🗄️ 2. Especificación del Contrato de Base de Datos (SQL Server 2022+)

### 2.1 Vista `dbo.uv_coordinador`
- Enlaza `dbo.uv_coordinador_identidad ci` con `LEFT JOIN dbo.uv_programa pr ON ci.id = pr.idCoordinador`.
- Columnas expuestas:
  - `id`, `idUsuario`, `idTipoIdentificacion`, `codigoTipoIdentificacion`, `nombreTipoIdentificacion`, `numeroIdentificacion`, `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `nombreCompleto`, `correo`, `idPrograma` (nullable), `nombrePrograma` (nullable), `idFacultad` (nullable), `nombreFacultad` (nullable), `estaActivoCoordinador`, `estaActivoTextoCoordinador`.

### 2.2 Procedimiento Almacenado `dbo.usp_crear_coordinador`

| Parámetro | Tipo SQL | Requerido | Descripción |
| :--- | :--- | :---: | :--- |
| `@idCoordinador` | `UNIQUEIDENTIFIER = NULL` | **NO** | ID del coordinador. Si es `NULL`, se genera con `NEWID()` cuando no existe registro previo. |
| `@numeroIdentificacion` | `NVARCHAR(20)` | **SÍ** | Documento de identidad numérico del coordinador. |
| `@primerNombre` | `NVARCHAR(100)` | **SÍ** | Primer nombre. |
| `@segundoNombre` | `NVARCHAR(100) = NULL` | **NO** | Segundo nombre. |
| `@primerApellido` | `NVARCHAR(100)` | **SÍ** | Primer apellido. |
| `@segundoApellido` | `NVARCHAR(100) = NULL` | **NO** | Segundo apellido. |
| `@correo` | `NVARCHAR(200)` | **SÍ** | Correo institucional único. |
| `@idPrograma` | `UNIQUEIDENTIFIER = NULL` | **NO** | ID del programa académico a coordinar. Si es `NULL`, el coordinador queda aprovisionado y disponible para ser asignado al crear un programa (`HU076`). |
| `@idFacultad` | `UNIQUEIDENTIFIER = NULL` | **SÍ (vía Scope)** | ID de la facultad del Decano ejecutor. |
| `@passwordHash` | `NVARCHAR(255) = NULL` | **NO** | Hash de contraseña para nuevo usuario. |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | **SÍ** | Identificador de correlación para trazabilidad distribuida. |
| `@idUsuarioEjecutor` | `UNIQUEIDENTIFIER = NULL` | **SÍ** | ID del usuario Decano/Admin que ejecuta la operación. |

---

## 🌐 3. Especificación del Contrato HTTP REST (Backend API)

### 3.1 `POST /api/v1/decano/coordinadores`
- **Autorización:** `@PreAuthorize("hasAnyRole('DECANO','ADMINISTRADOR')")`
- **Request (`CrearCoordinadorDTO`):**
  ```json
  {
    "numeroIdentificacion": "1017555444",
    "primerNombre": "Jorge",
    "segundoNombre": "Luis",
    "primerApellido": "Henao",
    "segundoApellido": "Pérez",
    "correo": "jorge.henao@uco.edu.co",
    "idPrograma": null
  }
  ```
- **Response (`201 Created`):** `ApiMessageResponse` con mensaje catalogado de confirmación y aprovisionamiento IAM en Keycloak bajo rol `COORDINADOR`.

### 3.2 `GET /api/v1/decano/coordinadores`
- **Autorización:** `@PreAuthorize("hasAnyRole('DECANO','ADMINISTRADOR')")`
- **Response (`200 OK`):** `ApiDataResponse<List<CoordinadorFacultadDTO>>` incluyendo los coordinadores asignados a programas de la facultad del Decano y los coordinadores aprovisionados sin programa asignado (`idPrograma = null`), deduplicados por `id`.

---

## 💻 4. Especificación del Contrato en Frontend (Angular 19)

- **`CreateCoordinadorPayload` (`role-management.model.ts`):**
  - `idPrograma?: string | null;`
- **`DeanManagementService.crearCoordinador(...)`:**
  - Envía `idPrograma: payload.idPrograma || null` a `POST /api/v1/decano/coordinadores`.
  - Normaliza coordinadores con `idPrograma == null` asignando `nombrePrograma: 'Sin programa asignado (Disponible)'`.
- **`DeanCoordinadoresComponent`:**
  - Permite abrir el modal y registrar coordinadores aun cuando `programas().length === 0`.

---

## 🔒 5. Declaración de Congelamiento Contractual

- [x] Contrato de Base de Datos (`dbo.uv_coordinador` y `dbo.usp_crear_coordinador`) congelado.
- [x] Contrato REST API (`POST` y `GET /api/v1/decano/coordinadores`) congelado.
- [x] Contrato Frontend (`DeanManagementService` y `DeanCoordinadoresComponent`) congelado.
- **Estado:** `CONTRACT_FROZEN`
