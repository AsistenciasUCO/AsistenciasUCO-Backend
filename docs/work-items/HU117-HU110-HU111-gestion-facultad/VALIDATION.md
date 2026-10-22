---
status: validated-pending-commit
type: validation
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-22
---

# VALIDATION — HU117 + HU110 + HU111: Gestión de Facultad

Evidencia de ejecución del 2026-10-22 contra el SQL Server local (`sql_server_asistencias`), Keycloak local (`:8081`) y un backend levantado desde el worktree en el puerto `8090`. Sin commits al momento de redactar este documento.

## 1. Resumen

| Capa | Comando / prueba | Resultado |
| :--- | :--- | :--- |
| DB | `test_usp_actualizar_facultad.sql` | PASS |
| DB | `test_usp_cambiar_estado_facultad.sql` | PASS |
| DB | `test_usp_crear_facultad.sql` (regresión HU116) | PASS |
| DB | `test_public_objects`, `test_golden_path_immutability`, `test_transaction_ownership` | PASS |
| Backend | `.\mvnw.cmd -o test` (incluye ArchUnit) | `1700 tests, 0 failures, 0 errors, 0 skipped`, BUILD SUCCESS |
| Frontend | `npx ng test` (suite completa, ChromeHeadlessCI) | `834 SUCCESS` |
| Frontend | `npx ng build --configuration=development` | Sin errores |
| E2E | `scratch` `test_hu117_hu110_hu111.ps1` con JWT reales | 19/19 casos, 0 fallos |

## 2. Base de datos

- SP nuevos: `usp_actualizar_facultad`, `usp_actualizar_facultad_interno`, `usp_cambiar_estado_facultad`, `usp_cambiar_estado_facultad_interno`.
- Mensajes nuevos en `schema/seed/04_mensaje.sql`: `FAC_001`, `DEC_001`, `FAC_002` (usuario y técnico).
- Los tests se registraron en `test_suite.sql`.
- Los casos DB-01 a DB-20 del `TEST_PLAN.md` están cubiertos; DB-19 verifica que el SP no altera el nivel de transacción del llamador. Los fixtures (3 decanos, 2 facultades) se crean dentro de la transacción y se revierten.
- Hallazgo: un nombre de savepoint de más de 32 caracteres falla en SQL Server. Se usa `sp_cambiar_estado_facultad`.
- Aplicación al entorno local: se desplegaron solo los 4 SP y las 6 filas de mensajes, sin `deploy_schema.ps1`, para no sobrescribir objetos de otras ramas en la base compartida.

## 3. Backend

- Casos de uso `ActualizarFacultadUseCaseImpl` y `CambiarEstadoFacultadUseCaseImpl` (POJOs cableados en `AdminWiringConfiguration`), puerto `FacultadCommandPort` ampliado, `FacultadJpaRepository` con `JpaProcedureExecutor`.
- `DbFailureClassifier`: `FAC_001` y `DEC_001` → 404, `FAC_002` → 409.
- `AdminPortalController`: `PUT /facultades/{id}` y `PATCH /facultades/{id}/estado` ya no responden 501; releen `uv_facultad` y responden 404 si la facultad no aparece.
- Cambios formales en tests existentes (por el nuevo constructor y contrato): `AdminPortalControllerTest`, `AdminPortalCrearDecanoContractTest`.
- ArchUnit: sin violaciones; no se tocó `SecurityConfig` porque `/api/v1/admin/**` ya exige `ADMINISTRADOR`.

## 4. Frontend

- `AdminManagementService.actualizarFacultad(id, {nombre?, idDecano?})` y `cambiarEstadoFacultad(id, activa)` devuelven `Observable`; la señal `facultades` solo cambia con una respuesta exitosa.
- Eliminados `asignarDecanoFacultad` (optimista, ocultaba el 501) y `desvincularDecanoFacultad` (decano es `NOT NULL`).
- `AdminFacultadesComponent`: bloquea clics repetidos (`guardandoDecano`, `facultadCambiandoEstado`), muestra el `message` del backend y mantiene el formulario o modal abierto en caso de error.
- Specs: se retiraron los tests de la API eliminada, incluido el legacy `actualizarFacultad, cambiarEstadoFacultad, asignarDecanoFacultad y desvincularDecanoFacultad funcionan`.

## 5. E2E con JWT real (backend `:8090`, usuarios `admin.institucional` y `decano.ingenieria`)

| Caso | HTTP esperado | HTTP real |
| :--- | :-: | :-: |
| `GET /admin/facultades` | 200 | 200 |
| `POST /admin/facultades` (facultad temporal con el decano existente) | 201 | 201 |
| `PUT` renombrar (devuelve el nombre nuevo) | 200 | 200 |
| `PUT` cuerpo vacío | 400 | 400 |
| `PUT` nombre de 2 caracteres | 400 | 400 |
| `PUT` nombre duplicado (otro caso de mayúsculas) | 409 | 409 |
| `PUT` facultad inexistente | 404 | 404 |
| `PUT` decano inexistente | 404 | 404 |
| `PUT` decano titular de otra facultad activa | 409 | 409 |
| `PATCH` inactivar (refleja `estaActivaFacultad=false`) | 200 | 200 |
| `PATCH` inactivar de nuevo (idempotente) | 200 | 200 |
| `PATCH` reactivar con decano en conflicto | 409 | 409 |
| `PATCH` sin `activa` | 400 | 400 |
| `PATCH` facultad inexistente | 404 | 404 |
| `PUT` y `PATCH` con rol `DECANO` | 403 | 403 |
| `PUT` con token inválido | 401 | 401 |

- Persistencia comprobada por consulta directa: la facultad temporal quedó con el nombre nuevo y estado 0. Luego se borró con SQL; la facultad original `Facultad de Ingenieria` quedó intacta y activa.
- El backend de pruebas del puerto `8090` se detuvo al terminar.

## 6. Limitaciones y pendientes

- `infra.ps1 verify` (escaneo de secretos y paridad de contratos WORM) no se ejecutó.
- `test_contract_closure.sql` falla contra la base compartida por `usp_actualizar_coordinador` (`canonical_columns=5`), un SP de HU121/HU122 desplegado por otro agente; no se relaciona con este work item.
- `openapi-golden-path.yaml` no contiene `admin/facultades`, así que no se modificó ni su `.sha256`. Si se quiere documentar estos endpoints en OpenAPI, es un cambio de contrato aparte.
- Los mensajes `FAC_001`, `DEC_001` y `FAC_002` existen en el seed de DB; falta registrarlos en Azure App Configuration (`messages:user:<CODIGO>`) si el backend los resuelve por ese catálogo.
- Sin commits ni PR.
