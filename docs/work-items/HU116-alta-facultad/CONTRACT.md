# 📑 CONTRACT SPECIFICATION — HU116: Alta Institucional de Nueva Facultad

> **Historia de Usuario:** HU116 (Alta institucional de nueva facultad)  
> **Work Item:** `HU116-alta-facultad`  
> **Estado:** `FORMALIZED` (CONTRACT_FROZEN)  
> **Ámbito:** Base de Datos (`GestioAsistenciaDB`), Backend API (`GestioAsistenciaBackend`) y Frontend Web (`GestioAsistenciaFrontend`).  
> **Fecha de Congelamiento:** 2026-10-17  
> **Conformidad:** `CONSTITUTION.md`, `AGENTS.md` y `GUIA_METODICA_IMPLEMENTACION_HU.md`.

---

## 🏛️ 1. Principios de Congelamiento y Reglas Inviolables

1. **Tupla Transaccional Canónica (Persistencia):** Toda interacción de mutación entre Backend y SQL Server retorna exactamente 4 columnas:
   $$\text{ResultSet} \longrightarrow (\mathtt{idResultado}, \mathtt{exitoso}, \mathtt{mensajeUsuario}, \mathtt{mensajeTecnico})$$
2. **Control Transaccional Seguro:**
   - Obligatorio `SET XACT_ABORT ON;` y `SET NOCOUNT ON;`.
   - Manejo explícito de transacciones con `BEGIN TRY ... BEGIN TRANSACTION ... COMMIT TRANSACTION ... END TRY`.
   - Rollback seguro en bloque `CATCH` comprobando `IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;`.
3. **Cero Datos Quemados (No Hardcoding):**
   - No comparar con `'00000000-0000-0000-0000-000000000000'`, evaluar siempre `@parametro IS NULL`.
   - Catálogos y mensajes resueltos mediante catálogo oficial o constantes de dominio.
4. **Clean Architecture (Backend):**
   - `@RestController` delega exclusivamente a `PrimaryPorts` / `UseCases`.
   - Cero `JdbcTemplate` o SQL directo en controladores.
   - Seguridad declarativa: `@PreAuthorize("hasRole('ADMINISTRADOR')")`.
5. **Frontend Reactivo (Angular 19):**
   - Consumo tipado mediante `ApiResponse<T>`.
   - Reactividad gobernada por Signals; prohibido el uso de timers o polling.

---

## 🗄️ 2. Especificación del Contrato de Base de Datos (SQL Server 2022+)

### 2.1 Nombre Canónico del Procedimiento
```sql
dbo.usp_crear_facultad
```

### 2.2 Parámetros de Entrada (`IN`)

| Parámetro | Tipo SQL | Modo | Requerido | Descripción |
| :--- | :--- | :---: | :---: | :--- |
| `@id` | `UNIQUEIDENTIFIER` | `IN` | **NO** | Identificador de la nueva facultad. Si es `NULL`, se genera automáticamente con `NEWID()`. |
| `@nombre` | `NVARCHAR(50)` | `IN` | **SÍ** | Nombre único de la facultad (longitud 3 a 50 caracteres). Se aplica `TRIM()`. |
| `@idInstitucion` | `UNIQUEIDENTIFIER` | `IN` | **NO** | ID de la institución educativa. Si es `NULL`, se resuelve la institución activa UCO por defecto. |
| `@idDecano` | `UNIQUEIDENTIFIER` | `IN` | **SÍ** | ID del decano titular (`dbo.Decano`). Obligatorio por restricción relacional `NOT NULL` en `dbo.Facultad` y `FK_Facultad_Decano`. No se admiten nulos ni identificadores en ceros. |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | `IN` | **NO** | Identificador de correlación W3C para observabilidad distribuida. Si es `NULL`, se genera `NEWID()`. |
| `@idUsuarioEjecutor` | `UNIQUEIDENTIFIER` | `IN` | **NO** | ID del usuario autenticado que ejecuta la operación (para auditoría). |

### 2.3 Validaciones de Negocio en T-SQL

1. **Obligatoriedad y Formato de Nombre:**
   - Si `@nombre IS NULL` o `LEN(TRIM(@nombre)) < 3` $\longrightarrow$ Retorna `exitoso = 0`, código de error `ERR_NOMBRE_FACULTAD_INVALIDO` ("El nombre de la facultad es obligatorio y debe tener entre 3 y 50 caracteres.").
2. **Unicidad de Nombre (Case-Insensitive):**
   - Si `EXISTS (SELECT 1 FROM dbo.Facultad WHERE LOWER(nombre) = LOWER(TRIM(@nombre)))` $\longrightarrow$ Retorna `exitoso = 0`, código de error `ERR_FACULTAD_YA_EXISTE` ("Ya existe una facultad registrada con el nombre ingresado.").
3. **Existencia de Institución:**
   - Si `@idInstitucion` especificado no existe en `dbo.Institucion` $\longrightarrow$ Retorna `exitoso = 0`, error `ERR_INSTITUCION_NO_EXISTE` ("La institución especificada no existe en el sistema.").
4. **Obligatoriedad y Existencia de Decano Titular:**
   - Si `@idDecano IS NULL` o no existe en `dbo.Decano` (vía `dbo.uv_decano_identidad`) $\longrightarrow$ Retorna `exitoso = 0`, error `GEN_002` ("Decano obligatorio") / `GEN_001` ("Decano inexistente"). No se permiten magic values ni nulos.

### 2.4 Mutación de Persistencia
```sql
INSERT INTO dbo.Facultad (id, nombre, institucion, decano, estado)
VALUES (@idFinal, TRIM(@nombre), @idInstitucionFinal, @idDecanoFinal, 1);
```

### 2.5 Conjunto de Resultados (Salida Estándar de 4 Columnas)
```sql
SELECT
    @idFinal                 AS idResultado,
    @estadoResultado         AS exitoso,
    @mensajeUsuarioResultado AS mensajeUsuario,
    @mensajeTecnicoResultado AS mensajeTecnico;
```

---

## 🌐 3. Especificación del Contrato HTTP REST (Backend API)

### 3.1 Definición del Endpoint
- **Método HTTP:** `POST`
- **Ruta:** `/api/v1/admin/facultades`
- **Autenticación:** Requerida (`Bearer JWT`)
- **Autorización RBAC:** `@PreAuthorize("hasRole('ADMINISTRADOR')")`
- **Cabeceras Aceptadas:**
  - `Content-Type: application/json`
  - `X-Correlation-Id: <UUID>` (Opcional, autogenerado si no se envía)

### 3.2 Esquema del Request (`CrearFacultadRequest`)
```json
{
  "nombre": "Facultad de Ciencias de la Salud",
  "idInstitucion": "a1b2c3d4-0000-0000-0000-000000000001",
  "idDecano": "d1e2f3a4-0000-0000-0000-000000000001"
}
```

#### Reglas de Validación en Backend (`@Valid` / Jakarta Validation):
| Campo | Tipo | Requerido | Validaciones |
| :--- | :--- | :---: | :--- |
| `nombre` | `String` | **SÍ** | `@NotBlank`, `@Size(min = 3, max = 50)` |
| `idInstitucion` | `UUID` | **NO** | Opcional |
| `idDecano` | `UUID` | **SÍ** | `@NotNull`, ID del decano titular existente en el directorio |

### 3.3 Respuestas HTTP

#### `201 Created` — Creación Exitosa
```json
{
  "exitoso": true,
  "mensaje": "Facultad creada exitosamente.",
  "datos": {
    "id": "c3d4e5f6-7890-1234-5678-90abcdef1234",
    "nombreFacultad": "Facultad de Ciencias de la Salud",
    "idInstitucion": "a1b2c3d4-0000-0000-0000-000000000001",
    "nombreInstitucion": "Universidad Católica de Oriente",
    "idDecano": "d1e2f3a4-0000-0000-0000-000000000001",
    "nombreCompletoDecano": "Decano Designado",
    "estaActivaFacultad": true,
    "estaActivaTextoFacultad": "SI"
  }
}
```

#### `400 Bad Request` — Datos de Entrada Inválidos
```json
{
  "exitoso": false,
  "codigoError": "VALIDATION_FAILED",
  "mensaje": "El nombre de la facultad es obligatorio y debe tener entre 3 y 50 caracteres.",
  "detalles": [
    {
      "campo": "nombre",
      "error": "El nombre no puede estar vacío"
    }
  ]
}
```

#### `409 Conflict` — Nombre de Facultad Duplicado
```json
{
  "exitoso": false,
  "codigoError": "CONFLICT_RESOURCE",
  "mensaje": "Ya existe una facultad registrada con el nombre ingresado.",
  "detalles": []
}
```

#### `403 Forbidden` — Usuario no autorizado
```json
{
  "exitoso": false,
  "codigoError": "ACCESS_DENIED",
  "mensaje": "Acceso restringido: se requiere rol ADMINISTRADOR institucional.",
  "detalles": []
}
```

---

## 💻 4. Especificación del Contrato en Frontend (Angular 19)

### 4.1 Método de Integración (`AdminManagementService`)
```typescript
crearFacultad(item: { nombre: string; idInstitucion?: string; idDecano?: string }): Observable<ApiResponse<FacultadDTO>>
```
- Invocación: `POST ${environment.apiUrl}/admin/facultades` con payload JSON.
- Actualización reactiva del Signal `_facultades`:
  ```typescript
  this._facultades.update(prev => [facultadCreada, ...prev]);
  ```
- Notificación de feedback:
  - Éxito: `this.toast.success('Facultad registrada exitosamente')`.
  - Error: `this.toast.error(error.message || 'Error al registrar facultad')`.

### 4.2 Modelo de Datos Integrado
```typescript
export interface FacultadDTO {
  id: string;
  nombreFacultad: string;
  idInstitucion: string;
  nombreInstitucion: string;
  idDecano?: string;
  nombreCompletoDecano?: string;
  estaActivaFacultad: boolean;
  estaActivaTextoFacultad: string;
}
```

---

## 🔒 5. Declaración de Congelamiento Contractual

- [x] Contrato de Base de Datos (`dbo.usp_crear_facultad`) congelado con tupla canónica de 4 columnas.
- [x] Contrato REST API (`POST /api/v1/admin/facultades`) congelado con DTOs tipados y códigos HTTP formales.
- [x] Integración Frontend con Angular Signals y `AdminManagementService` alineada al contrato.
- **Estado:** `CONTRACT_FROZEN` — Autorizado para pasar a la **Fase 03: Pruebas en Rojo (TEST RED)**.
