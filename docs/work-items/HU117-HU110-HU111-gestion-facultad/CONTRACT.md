---
status: frozen
type: contract
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-21
---

# CONTRACT — HU117 + HU110 + HU111

Estado: **`FROZEN`** (2026-10-21). El owner aprobó D1–D5 tal como están en el `PLAN.md`.

## 1. Reglas comunes

- Salida de mutación en DB: tupla estándar del repo (`idCorrelacion`, `mensajeUsuarioResultado`, `mensajeTecnicoResultado`, `estadoResultado`), igual que `usp_crear_facultad`. Esa es la forma que consume `JpaProcedureExecutor`.
- Cada SP público valida correlación y RBAC (`usp_validar_permiso_rbac_usuario_interno`, perfil `ADMINISTRADOR`) y delega la mutación a un `*_interno` con `SET XACT_ABORT ON`, transacción propia o `SAVE TRANSACTION`, y rollback en `CATCH`.
- Sin GUIDs ni textos quemados: estados y mensajes salen de `ufn_obtener_parametro` y `usp_obtener_mensaje_catalogo`.
- Las lecturas de validación usan vistas (`uv_facultad`, `uv_decano_identidad`), nunca tablas físicas.
- Códigos de error de DB (sección 2.3). `GEN_001` es genérico y el backend no puede traducirlo a 404, por eso se agregan códigos específicos al seed `04_mensaje.sql` (mensaje de usuario y técnico) y a `DbFailureClassifier`.
- `@idCorrelacion` es obligatorio: un valor NULL falla cerrado con `CORR_001`, igual que `usp_crear_facultad`. (Corrige la nota inicial "NEWID() si es NULL".)

## 2. Base de datos

### 2.1 `dbo.usp_actualizar_facultad` (HU110 + HU117)

| Parámetro | Tipo | Requerido | Notas |
| :--- | :--- | :---: | :--- |
| `@id` | `UNIQUEIDENTIFIER` | Sí | Facultad a modificar. |
| `@nombre` | `NVARCHAR(50)` | No | Si viene, `TRIM`, 3–50 caracteres, único sin distinguir mayúsculas entre las demás facultades. |
| `@idDecano` | `UNIQUEIDENTIFIER` | No | Si viene, debe existir en `uv_decano_identidad`. (D2) no puede ser titular de otra facultad activa. |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | No | `NEWID()` si es NULL. |
| `@idUsuarioEjecutor` | `UNIQUEIDENTIFIER` | Sí | Debe tener perfil `ADMINISTRADOR`. |

Reglas:
1. La facultad `@id` debe existir; si no, `GEN_001`.
2. Al menos uno de `@nombre` o `@idDecano` es obligatorio (D5); si no, `GEN_002`.
3. Un campo no enviado (`IS NULL`) no se modifica. Nunca se asigna NULL a `decano`.
4. Sin cambios reales (mismo nombre y mismo decano) retorna éxito sin escribir.
5. Una facultad inactiva se puede renombrar, pero no se le puede reasignar decano (no se evalúa si el nuevo decano está activo en IAM; eso es IAM-001).

Mutación (`usp_actualizar_facultad_interno`): `UPDATE dbo.Facultad SET nombre = ..., decano = ... WHERE id = @id`.

### 2.2 `dbo.usp_cambiar_estado_facultad` (HU111)

| Parámetro | Tipo | Requerido | Notas |
| :--- | :--- | :---: | :--- |
| `@id` | `UNIQUEIDENTIFIER` | Sí | Facultad. |
| `@estaActiva` | `BIT` | Sí | Estado deseado (D4). Idempotente: si ya está en ese estado retorna éxito. |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | No | |
| `@idUsuarioEjecutor` | `UNIQUEIDENTIFIER` | Sí | `ADMINISTRADOR`. |

Reglas:
1. La facultad debe existir.
2. (D3) Solo cambia `Facultad.estado`; sin cascada.
3. Al reactivar, (D2) se valida que su decano no sea titular de otra facultad activa.

Mutación (`usp_cambiar_estado_facultad_interno`): `UPDATE dbo.Facultad SET estado = @estaActiva WHERE id = @id`.

### 2.3 Códigos de error

| Código | Cuándo | HTTP vía `DbFailureClassifier` |
| :--- | :--- | :---: |
| `FAC_001` | La facultad no existe. Texto: "La facultad especificada no existe en el sistema." | 404 |
| `DEC_001` | El decano no existe. Texto: "El decano especificado no existe en el sistema." | 404 |
| `FAC_002` | El decano ya es titular de otra facultad activa (D2). | 409 |
| `GEN_002` | Nombre fuera de 3–50, ningún campo a modificar, estado deseado nulo o ejecutor nulo. | 400 |
| `GEN_003` | Nombre duplicado. | 409 |
| `SEC_001` | Ejecutor sin perfil `ADMINISTRADOR`. | 403 |
| `CORR_001` | Correlación nula. | 400 |
| `GEN_005` | Éxito de actualización o cambio de estado. | 200 |

## 3. Backend HTTP

Ambos bajo `/api/v1/admin`, protegidos por la regla existente `/api/v1/admin/**` → `ADMINISTRADOR`.

### 3.1 `PUT /api/v1/admin/facultades/{id}`

```json
{ "nombre": "Facultad de Ingeniería", "idDecano": "uuid" }
```
- Ambos campos opcionales; al menos uno obligatorio.
- `200 OK` → `ApiDataResponse<FacultadDTO>` (relectura de `uv_facultad`).
- `400` validación (`nombre` 3–50 si viene; cuerpo vacío).
- `404` facultad o decano inexistente (`FAC_001`, `DEC_001`).
- `409` nombre duplicado o (D2) decano ya titular de otra facultad activa.
- `403` sin rol `ADMINISTRADOR`.

### 3.2 `PATCH /api/v1/admin/facultades/{id}/estado`

```json
{ "activa": false }
```
- `activa` obligatorio (D4).
- `200 OK` → `ApiDataResponse<FacultadDTO>`.
- `404` facultad inexistente (`FAC_001`). `409` (D2, `FAC_002`) al reactivar con decano en conflicto. `403` sin rol.

### 3.3 Capas

Controller → `ActualizarFacultadInputPort` / `CambiarEstadoFacultadInputPort` → `*UseCaseImpl` (POJO, wiring en `AdminWiringConfiguration`) → `FacultadCommandPort` (`actualizarFacultad`, `cambiarEstadoFacultad`). Si la relectura de `uv_facultad` no encuentra la facultad, el controller responde 404 → `FacultadJpaRepository` con `JpaProcedureExecutor`. Los errores del SP se clasifican con `DbFailureClassifier`/`GlobalExceptionHandler`; el controller no interpreta textos.

### 3.4 `FacultadDTO` (sin cambios)

`id`, `nombreFacultad`, `idInstitucion`, `nombreInstitucion`, `idDecano`, `nombreCompletoDecano`, `estaActivaFacultad`, `estaActivaTextoFacultad`.

## 4. Frontend

- `AdminManagementService`:
  - `actualizarFacultad(id, { nombre?, idDecano? }): Observable<ApiResponse<FacultadItem>>`.
  - `cambiarEstadoFacultad(id, activa): Observable<ApiResponse<FacultadItem>>`.
  - Se eliminan `asignarDecanoFacultad` con optimismo y `desvincularDecanoFacultad`. La reasignación pasa por `actualizarFacultad`.
  - La señal `facultades` se actualiza solo con la respuesta exitosa del backend; los errores llegan al componente.
- `AdminFacultadesComponent`: muestra `mensajeUsuario` del backend en error (sin parsear texto), mantiene `OnPush` y Signals, y deshabilita el botón mientras la petición está en curso.
- Sin polling ni timers.

## 5. Compatibilidad

- No cambia ningún contrato HTTP existente en uso: ambos endpoints hoy responden 501.
- Se actualizan `openapi-golden-path.yaml` y su `.sha256` solo si estos endpoints forman parte de ese contrato; se verifica en la fase CONTRACT y se coordina con los otros work items que editan el mismo archivo.
