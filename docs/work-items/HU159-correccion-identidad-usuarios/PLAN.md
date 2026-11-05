---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# PLAN — HU159: corrección de datos de identidad por jerarquía

## Identidad y objetivo

- **HU159**, redefinida por John (2026-11-05): corregir errores de digitación en los datos de identidad (nombres, apellidos, tipo y número de documento) por jerarquía. No se tocan el correo, la contraseña ni el estado.
- **Work Item ID:** `HU159-correccion-identidad-usuarios`.
- **Ramas:** `feature/HU159-correccion-identidad-usuarios` en BD, backend y frontend; `docs/HU-CATALOG-HU159-HU160-HU177` en Infra.
- **HU160 DESCARTADA:** el administrador no cambia el estado de un estudiante (hoja de roles de Infra).
- **HU177 (nueva, PENDIENTE):** recuperar la contraseña olvidada. Solo se registra en el HU-CATALOG.

## Decisiones de John (2026-11-05)

| Quién | Corrige a | Endpoint | Motivo |
|---|---|---|---|
| ADMINISTRADOR | Decano | `PUT /api/v1/admin/decanos/{id}/identidad` (nuevo) | Siempre |
| DECANO de la facultad | Coordinador | `PUT /api/v1/decano/coordinadores/{id}` (HU121, aditivo) | Solo si cambia el documento |
| COORDINADOR del programa | Docente vinculado | `PUT /api/v1/coordinador/docentes/{idDocente}` (HU092, aditivo) | Solo si cambia el documento |
| COORDINADOR del programa | Estudiante del programa (`EstudiantePrograma`) | `PUT /api/v1/coordinador/estudiantes/{idEstudiante}/identidad` (nuevo) | Siempre |

- El administrador no se corrige por la app.
- Un estudiante de otro programa que esté en un grupo BAS/INS del coordinador no es suyo: el backend responde 404.
- **El documento es el username de Keycloak.** Si cambia, el backend actualiza el username y lo verifica leyendo la cuenta. Si el proveedor no lo aplica, queda como desfase pendiente (`IAM_002`); la persona puede entrar con su correo. La contraseña no cambia.
- **Reversible:**
  - El parámetro `IDENTIDAD.PERMITIR_CORREGIR_DOCUMENTO` en 0 apaga la corrección del documento: `IDE_003` y el frontend oculta los campos. Los nombres siguen funcionando.
  - El código del documento está aislado: un interno en BD; `CorreccionDocumentoDTO` y el `username` opcional en el backend; `app-correccion-documento` en el frontend.

## Alcance

| Capa | Cambio |
|---|---|
| DB | Ver `docs/work-items/HU159-correccion-identidad-usuarios/PLAN.md` en el repo de BD. `usp_corregir_identidad_usuario` (estudiante y decano), internos comunes para el documento y la auditoría, HU121/HU092 aditivos, mensajes `IDE_001`..`IDE_003` y el parámetro |
| Backend | **Común:** `CorreccionDocumentoDTO`, `CorreccionDocumentoRequest` y `CorreccionDocumentoValidation`, `CorreccionDocumentoParametros` (JPA), y `ActualizarCuentaIdentidadDTO.username`. Keycloak envía y verifica el username solo si cambia. **Nuevo:** `CorreccionIdentidadService` (implementa `CorreccionIdentidadInputPort`), `CorreccionIdentidadCommandPort` / `CorreccionIdentidadJpaRepository` y `CorreccionIdentidadController` (2 PUT + `GET /api/v1/configuracion/identidad`). **HU121 y HU092:** request, DTO, dominio, puerto y repositorio con el documento opcional; HU092 sincroniza el username solo si cambia el documento. `DbFailureClassifier`: `IDE_001` 400, `IDE_003` 409, `GEN_001` 404. `SecurityConfig` sin cambios: las rutas caen en `/coordinador/**`, `/admin/**` y `/api/v1/**`. 3 rutas nuevas en `access-catalog.json` |
| Frontend | `CorreccionIdentidadService`; `app-correccion-documento` (documento y motivo; solo lectura si el parámetro está apagado); `app-correccion-identidad-modal`; botón "Corregir datos" en la ficha del estudiante del coordinador y en la pantalla de decanos del administrador; el bloque de documento entra en los modales de HU121 y HU092 (pantallas de Sergio, cambio acotado) |

## Rollback

Revertir los commits. Sin desplegar código: poner el parámetro en 0.
