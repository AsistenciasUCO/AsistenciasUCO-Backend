---
status: AUDITED_BACKLOG
date: 2026-10-11
scope: backend
work_item: MAINT-003K
---
# MAINT-003K — diagnóstico del backlog funcional administrativo

## Alcance y fuentes

Este documento congela el diagnóstico AS-IS observado en el backend de PR #22, sin implementar módulos administrativos ni cambiar el contrato HTTP. La revisión se hizo sobre el HEAD local `6348e2f1ed6dcce1ab3748488d690fd1e6cdd460`, sobre el PR backend #22, y sobre la DB oficial del owner DB en el merge de PR #36 `e2486194d42c6c7a649882a6957273742aa26536`.

Fuentes principales: `AdminPortalController`, `CoordinadorPortalController`, `DecanoPortalController`, `UsuarioController`, `SecurityConfig`, los tests de esos controladores y wiring, los repositorios JPA de catálogos, `DB_BASELINE_CONTRACT.md` del repositorio DB y la metadata/ejecución transaccional de `sql_server_asistencias / gestionasistenciadb`. Una prueba no ejecutada o un mock no se cuenta como certificación del proveedor.

## Resumen ejecutivo

| Capacidad | AS-IS comprobado | Clasificación siguiente |
|---|---|---|
| Instituciones, facultades y áreas — lectura | `GET` implementado con Application ports/use cases y JPA sobre vistas públicas; autorización ADMINISTRADOR | Implementación disponible; falta E2E/proveedor real explícito |
| Instituciones — alta/cambio/estado | Rutas declaradas, pero lanzan `FeatureUnavailableException` → 501; no hay command público backend/DB auditado | Requiere contrato funcional y procedimiento/permiso DB del owner |
| Facultades — alta/cambio/estado | Rutas 501; `usp_crear_facultad` existe, pero el alta falla en runtime por `institucion` y `estado` `NOT NULL`; actualizar por ID/nombre sí se observa dentro de transacción | Requiere decisión/corrección de contrato DB; no corregir desde PR #22 |
| Áreas — alta/cambio/estado | Rutas 501; no se encontró procedimiento público equivalente | Requiere contrato funcional y procedimiento/permiso DB |
| Sedes | GET/POST/PUT/PATCH declarados y 501; no hay port/use case/JPA/view/SP comprobado | Requiere definición funcional y contrato DB completo |
| Espacios físicos | GET/POST/PUT/PATCH declarados y 501; no hay port/use case/JPA/view/SP comprobado | Requiere definición funcional y contrato DB completo |
| Perfil autenticado — lectura | `GET /api/v1/usuarios/perfil` 501; no existe port/use case/entity de perfil | Candidato acotado a una fase de lectura JPA, después de congelar DTO/contrato |
| Perfil autenticado — actualización | `PUT` 501; no existe command público autorizado | Requiere contrato funcional y command DB; fuera de PR #22 |
| Coordinación y decanato | Varias operaciones existentes; las rutas no implementadas devuelven 501 con rol válido | Backlog por capacidad; no convertir 403 esperado en falsa ausencia |

## Lecturas de catálogos ya disponibles

| Endpoint/método | Rol | Application | Adapter/persistencia | DB/runtime | Tests y estado |
|---|---|---|---|---|---|
| `GET /api/v1/admin/instituciones` | `ADMINISTRADOR` | `ConsultarInstitucionesInputPort` → `ConsultarInstitucionesUseCaseImpl` → `InstitucionQueryPort` | `InstitucionJpaRepository`, `UvInstitucionEntity` | `uv_institucion`; runtime role tiene `SELECT` | `AdminPortalControllerTest`, `AdminWiringConfigurationTest`; `IMPLEMENTATION_AVAILABLE_PENDING_E2E` |
| `GET /api/v1/admin/facultades` | `ADMINISTRADOR` | `ConsultarFacultadesInputPort` → `ConsultarFacultadesUseCaseImpl` → `FacultadQueryPort` | `FacultadJpaRepository`, `UvFacultadEntity` | `uv_facultad`; runtime role tiene `SELECT` | `AdminPortalControllerTest`, `AdminWiringConfigurationTest`; `IMPLEMENTATION_AVAILABLE_PENDING_E2E` |
| `GET /api/v1/admin/areas` | `ADMINISTRADOR` | `ConsultarAreasInputPort` → `ConsultarAreasUseCaseImpl` → `AreaQueryPort` | `AreaJpaRepository`, `UvAreaEntity` | `uv_area`; runtime role tiene `SELECT` | `AdminPortalControllerTest`, `AdminWiringConfigurationTest`; `IMPLEMENTATION_AVAILABLE_PENDING_E2E` |

La autorización está en `SecurityConfig`: una petición sin bearer debe ser 401; una identidad autenticada sin `ADMINISTRADOR` debe ser 403; una identidad ADMINISTRADOR que llegue a una ruta declarada pero no implementada recibe 501. No se propone ampliar roles para ocultar el backlog.

## Mutaciones administrativas

### Instituciones

Rutas AS-IS: `POST /api/v1/admin/instituciones`, `PUT /api/v1/admin/instituciones/{id}` y `PATCH /api/v1/admin/instituciones/{id}/toggle-estado`. Las tres terminan en `FeatureUnavailableException`; no hay input port, use case, secondary port, adapter JPA command ni procedimiento público equivalente comprobado en PR #36. La DB sí expone `Institucion` y `uv_institucion` para lectura. Antes de implementar faltan DTO/request, semántica de unicidad/nombre/estado, errores, auditoría, idempotencia, procedimiento y `GRANT EXEC` aprobados por el owner DB.

### Facultades

Rutas AS-IS: `POST /api/v1/admin/facultades`, `PUT /api/v1/admin/facultades/{id}` y `PATCH /api/v1/admin/facultades/{id}/estado`. Las tres devuelven 501.

La DB oficial contiene:

- `dbo.Facultad(id, nombre, institucion, decano, estado)`, con las cinco columnas `NOT NULL` y sin default observado para `institucion` o `estado`.
- `dbo.usp_crear_facultad(@idFacultad, @nombreFacultad, @idDecano, @idCorrelacion, @idUsuarioEjecutor)`, sin parámetro de institución.
- La rama existente actualiza por `id = @idFacultad` **o** por `LOWER(nombre) = LOWER(@nombreFacultad)`; el probe transaccional observó ambos caminos sin persistir cambios.
- La rama de alta inserta solo `(id, nombre, decano)`. El probe transaccional devolvió SQL 515 por `estado`, y no insertó filas después del rollback. El mismo contrato también deja `institucion` sin valor.
- La metadata de permisos revisada no mostró `GRANT EXEC` runtime para `usp_crear_facultad`; debe resolverse expresamente aunque se corrija el procedimiento.

Esto es un `DB_CONTRACT_FINDING`, no una invitación a rellenar defaults desde backend. El owner DB debe decidir la firma, valores canónicos de `institucion`/`estado`, unicidad, actualización por nombre, y permiso runtime. Después se congela el contrato y se derivan RED/integration tests.

### Áreas

Rutas AS-IS: `POST /api/v1/admin/areas`, `PUT /api/v1/admin/areas/{id}` y `PATCH /api/v1/admin/areas/{id}/estado`; todas 501. Se observó `dbo.Area`/`uv_area` para lectura, pero no un procedimiento público de alta/edición/estado ni puertos Application equivalentes. Requiere contrato DB del owner, no DML directo desde backend.

## Sedes y espacios físicos

Sedes: `GET/POST /api/v1/admin/sedes`, `PUT /api/v1/admin/sedes/{id}`, `PATCH /api/v1/admin/sedes/{id}/estado`.

Espacios físicos: `GET/POST /api/v1/admin/espacios-fisicos`, `PUT /api/v1/admin/espacios-fisicos/{id}`, `PATCH /api/v1/admin/espacios-fisicos/{id}/estado`.

Todas las rutas anteriores están declaradas en el controller, pero devuelven 501. En el backend auditado no se encontró port/use case, entidad/proyección JPA, repositorio, view pública ni SP equivalente que permita afirmar un contrato. El backlog correcto es primero funcional/contractual: jerarquía sede–espacio, campos, estado, unicidad, ownership, paginación, errores y consumers; luego modelo/vista/commands DB, permisos runtime y finalmente implementación/test del backend.

## Perfil autenticado

`GET /api/v1/usuarios/perfil` y `PUT /api/v1/usuarios/perfil` están declarados y hoy responden 501. El GET no necesita una expansión de Keycloak: la identidad debe provenir del usuario autenticado neutral (`AuthenticatedUserResolver`) y el resultado debe ser lectura de la DB.

La DB oficial ya expone `uv_usuario` y `uv_usuario_perfil`, y el rol runtime tiene `SELECT` sobre ambas vistas. El backend tiene `UvUsuarioEntity` para `uv_usuario`, pero no tiene `UvUsuarioPerfilEntity`, port, use case ni DTO de perfil. `UvUsuarioEntity` no mapea password; la futura lectura no debe devolver contraseñas, hashes ni datos de terceros.

Propuesta para una fase independiente, no autorizada por PR #22: congelar primero el DTO HTTP y los campos visibles; crear en Application un `UsuarioPerfilQueryPort` neutral y un caso de uso de lectura; implementar en Infrastructure una proyección JPA inmutable sobre `uv_usuario` + `uv_usuario_perfil`, restringida al UUID del actor autenticado; agregar wiring, 401/validación de actor, integración SQL y contrato OpenAPI. La consulta no debe aceptar el UUID de otro usuario como autoridad ni depender de clases/imports de Keycloak.

El `PUT` queda separado: requiere contrato de actualización, command público DB, autorización de campos, auditoría y pruebas de no exposición de secretos. No se implementa como `UPDATE` directo.

## Coordinación, administración y decanato — 501 real

Con el rol correcto, las rutas que lanzan `FeatureUnavailableException` representan una capacidad no implementada y deben catalogarse como 501, no como fallo de autorización:

- Admin: `PATCH /api/v1/admin/decanos/{id}/toggle`, todas las rutas de sedes y espacios físicos, `PUT /api/v1/admin/parametros/{id}`, `GET /api/v1/admin/auditoria`, todas las mutaciones de instituciones, facultades y áreas.
- Decano: `PATCH /api/v1/decano/coordinadores/{id}/estado`.
- Coordinador: operaciones de docentes (`GET/POST/PATCH`), `PATCH /planes-estudio/{id}/toggle-estado`, alta/baja de semestres, baja de asignación, mutaciones de periodos, revisión de solicitudes de matrícula y alta/baja de estudiantes en grupos, según `CoordinadorPortalController`.

Las rutas implementadas de consulta/command no se reclasifican como backlog 501: por ejemplo, los paths de planes de estudio que ya llaman ports y los providers de PR #36 conservan su estado propio. El trabajo futuro debe dividirse por capability y contrato, no por “desbloquear todos los 501” con una sola ampliación de roles.

## Orden recomendado y límites

1. El owner DB publica la decisión contractual de facultad y los contratos faltantes de instituciones/áreas/sedes/espacios; el backend no modifica `schema/`, SP ni permisos en PR #22.
2. Abrir una rama separada propuesta `codex/admin-profile-read-contract` para congelar y probar únicamente el GET de perfil, si el contrato funcional es aprobado.
3. Abrir ramas separadas para commands administrativos y para sedes/espacios, cada una con work item, contrato DB, RED, integración real y permisos runtime explícitos.
4. Mantener `PUT /usuarios/perfil`, auditoría, parámetros y toggles como capacidades distintas hasta tener sus contratos.

No se implementan aquí módulos administrativos, no se cambian roles JWT, no se incorporan SDKs de identidad y no se usa un mock como evidencia de DB.
