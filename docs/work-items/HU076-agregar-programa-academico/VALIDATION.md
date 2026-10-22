# Validación y Auditoría: HU076 - Agregar nuevo programa académico a la facultad

- **ID Work Item**: `HU076-agregar-programa-academico`
- **Estado**: `VALIDADO`

## 1. Evidencias de Base de Datos (`Repositorio_GestionAsistenciadb`)

- **Objetos desplegados y verificados en `sql_server_asistencias` (`gestionasistenciadb`)**:
  - `schema/views/uv_programa.sql` y `schema/views/uv_auth_programa.sql` (exponen `idTipoPrograma` y `nombreTipoPrograma` preservando orden de columnas previas).
  - `schema/stored-procedures/usp_crear_programa_academico_interno.sql` (persistencia atómica con control de transacción propia / savepoint).
  - `schema/stored-procedures/usp_crear_programa_academico.sql` (validación de `@idCorrelacion`, fail-closed de `@idUsuarioEjecutor IS NULL` -> `GEN_002`, RBAC `'DECANO'` -> `SEC_001`, titularidad jerárquica sobre `'FACULTAD'`, longitud de nombre `3..50` -> `GEN_002`, integridad referencial en `uv_facultad`, `uv_tipo_programa` y `uv_coordinador_identidad` -> `GEN_001`, y unicidad de nombre -> `GEN_003`).
- **Resultados de Pruebas SQL**:
  - `test_public_objects.sql`: `TEST_PASS:PUBLIC_VIEWS_REFRESH`, `TEST_PASS:BROKEN_DEPENDENCIES_ZERO`.
  - `test_contract_closure.sql`: `TEST_PASS:PUBLIC_SECURITY_EXECUTOR_REQUIRED`, `TEST_PASS:PUBLIC_CANONICAL_RESULTSET`.
  - `test_reactive_academic.sql`: `TEST_PASS:PROGRAM_UPSERT_CREATE`, `TEST_PASS:PROGRAM_UPSERT_UPDATE`.
  - `test_titularidad_jerarquica.sql`: `TEST_PASS:TITULARIDAD_FACULTAD_USUARIO_ID`.
  - `test_authorized_views_rbac.sql`: `TEST_PASS:RBAC_DECANO_FACULTY_ISOLATION`.
  - `test_usp_crear_programa_academico.sql`:
    - `TEST_PASS:HU076_PROGRAM_CREATE_EXECUTOR_REQUIRED`
    - `TEST_PASS:HU076_PROGRAM_CREATE_EXECUTOR_RBAC`
    - `TEST_PASS:HU076_PROGRAM_CREATE_NAME_VALIDATION`
    - `TEST_PASS:HU076_PROGRAM_CREATE_TYPE_FK_VALIDATION`
    - `TEST_PASS:HU076_PROGRAM_CREATE_SUCCESS`
    - `TEST_PASS:HU076_PROGRAM_CREATE_DUPLICATE_REJECTED`

## 2. Evidencias de Backend (`Repositorio_GestionAsistenciaBackend`)

- **Puertos y Adaptadores Creados**:
  - `ProgramaProjection`, `TipoProgramaProjection`, `ProgramaQueryPort`, `ProgramaCommandPort`, `TipoProgramaQueryPort`.
  - `UvProgramaEntity`, `UvTipoProgramaEntity`, `ProgramaJpaRepository`, `TipoProgramaJpaRepository`, `AcademicViewJpaProjectionMapper`.
- **Casos de Uso (Clean Architecture - 0 anotaciones Spring en `application`)**:
  - `co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.*`
  - `co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.*`
  - `co.edu.uco.asistenciasuco.application.features.decano.crearprograma.*`
- **Exposición REST (`DecanoPortalController`)**:
  - `GET /api/v1/decano/programas`
  - `GET /api/v1/decano/tipos-programa`
  - `POST /api/v1/decano/programas`
- **Pruebas Unitarias y de Arquitectura**:
  - `ConsultarProgramasUseCaseImplTest`: `2/2 PASS`
  - `ConsultarTiposProgramaUseCaseImplTest`: `1/1 PASS`
  - `CrearProgramaUseCaseImplTest`: `5/5 PASS`
  - `DecanoPortalControllerTest`: `5/5 PASS`
  - `CleanArchitectureRulesTest`: `PASS`

## 3. Evidencias de Frontend (`Repositorio_GestionAsistenciaFrontend`)

- **Modelos y Servicios**:
  - `ProgramaAcademicoItem`, `TipoProgramaItem`, `CreateProgramaPayload` en `role-management.model.ts`.
  - `getProgramas()`, `getTiposPrograma()`, `createPrograma()` en `DeanManagementService` (`dean-management.service.ts`).
- **Interfaz de Usuario (`DeanProgramasComponent`)**:
  - Botón `+ Nuevo Programa Académico`, formulario reactivo con validaciones de longitud (`3..50`), selección de tipo de programa y coordinador responsable, listado de programas de la facultad y notificaciones de éxito/error.
- **Pruebas Unitarias**:
  - `dean-management.service.spec.ts` y `dean-programas.component.spec.ts` actualizados sin alterar aserciones preexistentes (*Zero Test Tampering*).
