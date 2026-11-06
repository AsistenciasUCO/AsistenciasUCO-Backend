---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# TEST_PLAN — HU159

## DB

`test/test_usp_corregir_identidad_usuario.sql`. Son 9 marcadores críticos:
- Éxito con auditoría antes/después.
- Motivo vacío, nulo o largo.
- Ámbito: otro programa, inexistente, tipo no soportado.
- Unicidad del documento y transacción externa viva.
- RBAC.
- Decano corregido por el administrador.
- HU121 sin documento, sin motivo y con duplicado.
- HU092 con documento.
- Parámetro en 0: se rechaza el documento y los nombres siguen funcionando.

## Backend

| Test | Caso |
|---|---|
| `CorreccionIdentidadServiceTest` | Username solo si cambia el documento; `PENDIENTE` con `IAM_002`; tipo no soportado; configuración 1/0/ausente; `CorreccionDocumentoDTO` |
| `CorreccionIdentidadControllerTest` | PUT de estudiante y decano (mapeo, actor, recorte); 400 por motivo, tipo sin número, número no positivo y motivo largo; GET de configuración |
| `CorreccionIdentidadJpaRepositoryTest` | Parámetros del SP y lectura de las 3 columnas extra; sin usuario = violación del contrato |
| `KeycloakIdentityProviderAdapterGestionCuentaTest` | Envía y verifica el username; si Keycloak no lo aplica, falla (desfase); username en blanco = sin cambio |
| `ActualizarCoordinadorUseCaseImplTest`, `DecanoCoordinadorGestionControllerTest` | HU121 con documento: motivo obligatorio y username sincronizado |
| `ActualizarDocenteProgramaUseCaseImplTest` | HU092: sin documento no sincroniza; con documento sincroniza el username |
| `DbFailureClassifierCoordinadorTest` | `IDE_001`, `IDE_003` y `GEN_001` de la corrección |
| `AccessCatalogConsistencyTest` | 3 rutas nuevas |

## Frontend

| Spec | Caso |
|---|---|
| `correccion-documento.component.spec.ts` | Parámetro apagado = solo lectura; catálogo caído = solo lectura; tipo preseleccionado; cambio exige motivo y avisa del usuario de acceso; número inválido; `motivoSiempre` |
| `correccion-identidad-modal.component.spec.ts` | Precarga, error del backend, payload con y sin documento |
| `correccion-identidad.service.spec.ts` | Configuración cacheada, error = no corregible, rutas PUT |
| `coordinator-ficha-modal.component.spec.ts` | Corregir y recargar la ficha; error en el modal |
| `admin-decanos.component.spec.ts` | Corregir decano con aviso `PENDIENTE`; error en el modal |
| `dean-coordinador-edit-modal.component.spec.ts`, `coordinator-docente-edit-modal.component.spec.ts` | HU121/HU092 envían el documento y su motivo; bloque inválido bloquea el envío |

Validación manual: MV-016 del ledger.
