# MAINT-003L — Test plan E2E local de docentes de paginación

## Fuentes

Requisito pegado por el usuario, `PLAN.md`, contrato Golden Path/OpenAPI vigente, runtime security, fixture DEV DB `dev_pagination_fixture.sql` y scripts Keycloak existentes.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| PAG-E2E-01 | No tocar datos base | Snapshot antes/después | DB Developer y backup válido | Preparar horarios | Conteos/hash de estudiantes, matrículas, grupos, sesiones y asistencias | Sin variación | Script que recarga o duplica fixtures | SQL real |
| PAG-E2E-02 | Horarios válidos | Cada grupo sintético | Grupo y `dbo.Dia` existen | Ejecutar prepare dos veces | Filas de `dbo.Horario` | Al menos una fila por grupo; segunda ejecución no duplica | Insert no idempotente o día inválido | SQL real |
| PAG-E2E-03 | Limpieza reversible | Horarios propios | IDs reservados presentes | Ejecutar cleanup y prepare | Conteos y referencias | Solo se quitan/reponen filas reservadas; no quedan huérfanos | DELETE amplio o FK roto | SQL real |
| PAG-E2E-04 | Identidad exacta | Cada cuenta E2E | Password local presente | Provisionar dos veces | Usuario, `idUsuario`, `sub`, client mappings | Una cuenta por username; `idUsuario` exacto; `sub` propio; solo `DOCENTE` | Reutilizar `sub`, role legacy o mapping incorrecto | Keycloak real |
| PAG-E2E-05 | JWT contractual | Cada cuenta E2E | Cuenta creada | Obtener token con helper | `iss`, `aud`, `idUsuario`, roles | issuer exacto, audiencia API, claim correcto, `DOCENTE` | Mapper ausente, audience frontend o realm role | OIDC real |
| PAG-E2E-06 | Ownership | Docente dueño/ajeno | Backend activo y V2 ON | GET horarios, estudiantes y sesiones v2 | HTTP y payload | Dueño 200; docente ajeno 403; no ampliación DOCENTE | RBAC sin scope o scope omitido | HTTP real |
| PAG-E2E-07 | Cardinalidad | 300/1000/5000 | Backend activo | GET estudiantes por grupo | Número de filas/total | Exactamente 300, 1000, 5000 | Consulta global, duplicados o filtro roto | HTTP/SQL real |
| PAG-E2E-08 | Directorio paginado | 5000 con COORDINADOR | Cuenta coordinador autorizada | GET `/api/v1/estudiantes?page=0&size=20` | HTTP 200 y página | 20 filas; docente no obtiene 200 por esa ruta | Ampliar DOCENTE | HTTP real |
| PAG-E2E-09 | Angular real | Pantallas docentes | Frontend activo | Login y navegación | Render, grupos y tablas | Cada docente ve su grupo y paginación | Backend-only falso positivo | Manual real |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: YES.
- ENVIRONMENT: SQL Server Developer `sql_server_asistencias` / `gestionasistenciadb`, Keycloak local `asistencias-uco`, backend local con V2 habilitada y frontend Angular real si disponible.
- MOCK_SUFFICIENT: NO.
- WHY: mocks no certifican SQL Server, Keycloak claims, ownership ni navegador.

## Negative / Boundary Coverage

Cuenta ausente, password ausente, usuario existente con `idUsuario` conflictivo, grupo inexistente, DB equivocada/no Developer, docente ajeno a grupo, rol `DOCENTE` contra directorio coordinador y `GET` V2 con ownership ajeno.

## Side Effects / Rollback

Backup antes de DML. Horarios propios identificados por IDs reservados. Usuarios por usernames reservados. No se permite cleanup por prefijo amplio sobre cuentas o datos no E2E.

## Pruebas RED requeridas

NO APLICA como RED Java: el cambio no agrega comportamiento productivo ni modifica tests del backend. Los scripts se validan por parseo, prechecks, ejecución real e idempotencia.

## RED_SNAPSHOT

NO APLICA; no hay tests RED de código productivo.

## Congelación

El contrato se congela por `PLAN.md` y las fuentes vigentes citadas. Cambios de endpoint, role o claim exigirían un nuevo dictamen contractual.

## Integración y E2E

Fixture local versionado en el repo DB; backup y cleanup documentados. Los skips por password/backend/frontend ausente se registran como `NOT_RUN`, nunca como PASS.

## Falsos positivos que deben evitarse

No usar solo token decodificado sin request HTTP, no aceptar `sub` como `idUsuario`, no contar `Grupo.cantidadEstudiantes` como prueba de matrículas, no declarar PASS con un mock, ni probar el directorio de 5000 otorgando `COORDINADOR` a un docente.
