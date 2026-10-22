---
status: frozen
type: test-plan
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-21
---

# TEST_PLAN — HU117 + HU110 + HU111

Todas las pruebas se escriben en rojo antes de implementar. No se relajan aserciones para hacerlas pasar.

## 1. Base de datos (`GestioAsistenciaDB/test/`)

| ID | SP | Escenario | Resultado esperado |
| :-: | :--- | :--- | :--- |
| DB-01 | `usp_actualizar_facultad` | Renombrar con nombre válido | `estadoResultado = 1`, `nombre` actualizado, `@@TRANCOUNT = 0` |
| DB-02 | `usp_actualizar_facultad` | Reasignar a otro decano libre | `estadoResultado = 1`, `decano` actualizado |
| DB-03 | `usp_actualizar_facultad` | Renombrar y reasignar en una llamada | ambos campos actualizados |
| DB-04 | `usp_actualizar_facultad` | Nombre duplicado (otra facultad, distinto caso) | `estadoResultado = 0`, sin cambios |
| DB-05 | `usp_actualizar_facultad` | Nombre propio sin cambios | éxito sin conflicto de unicidad contra sí misma |
| DB-06 | `usp_actualizar_facultad` | Nombre de 2 o 51 caracteres | `estadoResultado = 0` |
| DB-07 | `usp_actualizar_facultad` | Ni nombre ni decano | `estadoResultado = 0` |
| DB-08 | `usp_actualizar_facultad` | Facultad inexistente | `FAC_001` |
| DB-09 | `usp_actualizar_facultad` | Decano inexistente | `DEC_001`, decano anterior intacto |
| DB-10 | `usp_actualizar_facultad` | (D2) Decano titular de otra facultad activa | `FAC_002` |
| DB-11 | `usp_actualizar_facultad` | Ejecutor sin perfil `ADMINISTRADOR` | `estadoResultado = 0` |
| DB-12 | ambos | Correlación nula | `estadoResultado = 0`, `CORR_001` |
| DB-13 | `usp_cambiar_estado_facultad` | Inactivar facultad activa | `Facultad.estado = 0` |
| DB-14 | `usp_cambiar_estado_facultad` | Activar facultad inactiva | `Facultad.estado = 1` |
| DB-15 | `usp_cambiar_estado_facultad` | Repetir el mismo estado | éxito idempotente |
| DB-16 | `usp_cambiar_estado_facultad` | Facultad inexistente | `estadoResultado = 0` |
| DB-17 | `usp_cambiar_estado_facultad` | (D2) Reactivar con decano en conflicto | `estadoResultado = 0` |
| DB-18 | `usp_cambiar_estado_facultad` | Ejecutor sin perfil `ADMINISTRADOR` | `estadoResultado = 0` |
| DB-19 | ambos | Falla forzada dentro del `*_interno` | rollback, `@@TRANCOUNT = 0` |
| DB-20 | `uv_decano` | Tras inactivar la facultad | `estaActivoDecano = 0` para su decano |

## 2. Backend (`src/test/java`)

| ID | Clase | Escenarios |
| :-: | :--- | :--- |
| BE-01 | `ActualizarFacultadUseCaseImplTest` | Delegación al puerto con actor; validación de nombre y de cuerpo vacío; propagación de `ConflictException` y `NotFoundException`. |
| BE-02 | `CambiarEstadoFacultadUseCaseImplTest` | Delegación con `activa` true/false; `activa` nulo rechazado; errores propagados. |
| BE-03 | `FacultadJpaRepositoryTest` | `actualizarFacultad` y `cambiarEstadoFacultad` ejecutan el `EXEC` con todos los parámetros y la correlación; mapean fallo de DB. |
| BE-04 | `AdminPortalControllerTest` | `PUT` y `PATCH`: 200 con DTO releído, 400, 404, 409; ya no devuelven 501; el actor sale de `AuthenticatedUserResolver`. |
| BE-05 | Seguridad (sin editar `RbacSecurityFilterChainTest`) | Verificar con un test propio que `DECANO` y `COORDINADOR` reciben 403 en ambos endpoints. |
| BE-06 | `ArchUnit` | Los nuevos casos de uso no dependen de infraestructura; el controller no inyecta persistencia. |
| BE-07 | Wiring | El contexto arranca con los nuevos `@Bean` en `AdminWiringConfiguration`. |

Meta: 100 % de líneas y ramas en casos de uso nuevos; gate global de JaCoCo (LINE ≥ 80 %, BRANCH ≥ 70 %) sin bajar.

## 3. Frontend (`*.spec.ts`)

| ID | Archivo | Escenarios |
| :-: | :--- | :--- |
| FE-01 | `admin-management.service.spec.ts` | `actualizarFacultad` hace `PUT` con el cuerpo correcto y actualiza la señal solo si `exitoso`; un error HTTP no modifica la señal y se propaga. |
| FE-02 | `admin-management.service.spec.ts` | `cambiarEstadoFacultad(id, activa)` hace `PATCH` con `{activa}`; misma regla de éxito/error. |
| FE-03 | `admin-facultades.component.spec.ts` | Editar nombre, cambiar decano y alternar estado muestran el toast de éxito solo tras la respuesta; en 409/404 muestran `mensajeUsuario`. |
| FE-04 | `admin-facultades.component.spec.ts` | Botón deshabilitado mientras la petición está en curso; sin acción "desvincular decano". |
| FE-05 | `mock-backend.interceptor.spec.ts` | Solo si el interceptor ya simula estos endpoints, se alinea al nuevo contrato. |

## 4. Integración y E2E

- Script temporal `scratch/test_hu117_hu110_hu111.ps1`: token `ADMINISTRADOR` en Keycloak, `PUT` y `PATCH` felices e inválidos (400/404/409), token de `DECANO` (403) y consulta directa a SQL Server para comprobar persistencia.
- `.\AsistenciasUCO-Infra\infra.ps1 verify` en `[PASS]` antes del cierre.
- Gates: `.\mvnw.cmd test`, `npm run test:ci`, `npm run build`, `test_suite.sql`.

## 5. Estado RED (2026-10-22)

| Capa | Archivos | Resultado |
| :--- | :--- | :--- |
| DB | `test/test_usp_actualizar_facultad.sql`, `test/test_usp_cambiar_estado_facultad.sql` | Fallan en el primer caso: `Could not find stored procedure`. Los fixtures transaccionales (3 decanos, 2 facultades) se crean y se revierten. |
| Backend | `ActualizarFacultad*Test`, `CambiarEstadoFacultad*Test`, `FacultadJpaRepositoryTest`, `AdminPortalControllerTest`, `DbExceptionTranslatorTest` | `test-compile` falla solo por símbolos inexistentes (clases, puertos, constantes y constructor nuevos). |
| Frontend | `admin-management.service.spec.ts`, `admin-facultades.component.spec.ts` | `ng test` falla por errores TS: métodos y señales aún no existen. |

Cambios formales sobre tests existentes, por el contrato de este work item y no para forzar verde:
- `AdminPortalControllerTest`: nuevo constructor del controller y se quitan `actualizarFacultad` y `cambiarEstadoFacultad` de la lista de endpoints que lanzan 501.
- `admin-management.service.spec.ts`: se retiran los tests de `asignarDecanoFacultad` (optimista) y `desvincularDecanoFacultad`, que se eliminan del contrato.
- `admin-facultades.component.spec.ts`: el test de "cambiar decano" ahora usa `actualizarFacultad`.
