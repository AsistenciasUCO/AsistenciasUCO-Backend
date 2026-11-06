---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# CONTRACT — HU159

## Cuerpo común (documento opcional)

`idTipoIdentificacion` (UUID) y `numeroIdentificacion` (entero positivo) van juntos; si no vienen, el documento no cambia. `motivo` tiene hasta 500 caracteres.

| Método y endpoint | Roles | Cuerpo | Respuesta |
|---|---|---|---|
| `PUT /api/v1/coordinador/estudiantes/{idEstudiante}/identidad` (nuevo) | COORDINADOR (el SP rechaza otro perfil; `/coordinador/**` admite ADMINISTRADOR en Spring) | `primerNombre`*, `segundoNombre`, `primerApellido`*, `segundoApellido`, documento opcional, `motivo`* | `ApiDataResponse` de `{ idRegistro, sincronizacionIdentidad: OK o PENDIENTE, mensajeUsuario }` |
| `PUT /api/v1/admin/decanos/{id}/identidad` (nuevo) | ADMINISTRADOR | Igual | Igual |
| `GET /api/v1/configuracion/identidad` (nuevo) | Autenticado | — | `{ permitirCorregirDocumento: boolean }` |
| `PUT /api/v1/decano/coordinadores/{id}` (HU121, aditivo) | DECANO | + `idTipoIdentificacion`, `numeroIdentificacion`, `motivo` (obligatorio si viene el documento) | Sin cambios |
| `PUT /api/v1/coordinador/docentes/{idDocente}` (HU092, aditivo) | COORDINADOR | + los mismos tres campos. El documento cuenta como "dato a actualizar" | Sin cambios |

## Errores

| Código | Cuándo |
|---|---|
| 400 | Forma inválida; `IDE_001` (motivo); `VAL_001`/`VAL_003`/`VAL_004` (formatos) |
| 403 | Perfil sin la jerarquía (`SEC_001`) |
| 404 | Estudiante o decano inexistente o fuera del ámbito (`GEN_001`) |
| 409 | `ERR_UNICIDAD_DOCUMENTO` (documento de otro usuario); `IDE_003` (corrección del documento apagada) |

La base de datos es la fuente de verdad. Si el proveedor de identidad falla, la respuesta es 200 con `sincronizacionIdentidad = PENDIENTE` y el desfase queda registrado.
