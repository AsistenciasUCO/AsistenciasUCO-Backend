# Plan de Pruebas (TDD): HU076 - Agregar nuevo programa académico a la facultad

## 1. Pruebas de Base de Datos (`Repositorio_GestionAsistenciadb`)

Archivo: `test/test_usp_crear_programa_academico.sql` (registrado en `test_suite.sql`).

| ID Caso | Escenario | Resultado Esperado |
| :--- | :--- | :--- |
| `DB-HU076-01` | Ejecución sin `@idUsuarioEjecutor` (`NULL`) | `estadoResultado = 0`, código técnico `GEN_002` (Fail-Closed). |
| `DB-HU076-02` | Ejecución con actor sin rol `DECANO` (ej. Docente/Coordinador) | `estadoResultado = 0`, rechazo RBAC (`RBAC_001`). |
| `DB-HU076-03` | Ejecución con nombre menor a 3 caracteres (`'AB'`) | `estadoResultado = 0`, código técnico `GEN_002`. |
| `DB-HU076-04` | Ejecución con `@idTipoPrograma` inexistente | `estadoResultado = 0`, código técnico `GEN_001`. |
| `DB-HU076-05` | Ejecución con `@idCoordinador` inexistente | `estadoResultado = 0`, código técnico `GEN_001`. |
| `DB-HU076-06` | Ejecución por Decano sobre una facultad de la cual no es titular | `estadoResultado = 0`, rechazo de titularidad jerárquica (`SCOPE_001` / `RBAC`). |
| `DB-HU076-07` | Creación exitosa de programa académico por el Decano titular | `estadoResultado = 1`, `PROG_001`, fila persistida y visible en `dbo.uv_programa` con `idTipoPrograma` y `nombreTipoPrograma`. |
| `DB-HU076-08` | Intento de crear un nuevo programa con nombre duplicado (case-insensitive) | `estadoResultado = 0`, código técnico `GEN_003` (unicidad de nombre). |

## 2. Pruebas Unitarias y de Arquitectura en Backend (`Repositorio_GestionAsistenciaBackend`)

| Clase de Prueba | Cobertura |
| :--- | :--- |
| `CrearProgramaUseCaseImplTest` | Creación exitosa, validación de nombre nulo/corto/largo, validación de `idFacultad`, `idTipoPrograma` e `idCoordinador` obligatorios. |
| `ConsultarProgramasUseCaseImplTest` | Consulta filtrada por `idFacultad` del Decano y validación cuando `idFacultad` es nulo. |
| `ConsultarTiposProgramaUseCaseImplTest` | Retorno de catálogo de tipos de programa (`pregrado`, `postgrado`). |
| `ProgramaJpaCommandPatternTest` | Verificación de invocación SP `dbo.usp_crear_programa_academico` y mapeo de errores SQL (`GEN_003` -> `CONFLICT`). |
| `DecanoPortalControllerTest` | preservación de tests existentes de coordinadores + nuevos tests para `GET /api/v1/decano/programas`, `GET /api/v1/decano/tipos-programa` y `POST /api/v1/decano/programas`. |
| `CleanArchitectureTest` | Reglas ArchUnit garantizando 0 anotaciones Spring en `application.**`. |

## 3. Pruebas Unitarias en Frontend (`Repositorio_GestionAsistenciaFrontend`)

| Archivo Spec | Cobertura |
| :--- | :--- |
| `dean-management.service.spec.ts` | Pruebas de `getProgramas()`, `getTiposPrograma()` y `createPrograma()` contra `/api/v1/decano/*`. |
| `dean-programas.component.spec.ts` | Preservación intacta del test existente de planes de estudio + nuevas pruebas para apertura del formulario de registro de programa, carga de catálogos (`programas`, `tipos-programa`, `coordinadores`), validación de campos obligatorios y envío exitoso de `POST /api/v1/decano/programas`. |
