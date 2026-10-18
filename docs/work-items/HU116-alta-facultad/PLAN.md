---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-17
---

# PLAN — HU116: Alta Institucional de Nueva Facultad

Fase 01: Planificación y Definition of Ready (DoR) conforme a `GUIA_METODICA_IMPLEMENTACION_HU.md`, `DEFINITION_OF_READY.md` y `AGENTS.md`.

---

## 1. Identidad y Objetivo

- **Historia de Usuario:** HU116 (Alta institucional de nueva facultad).
- **Work Item ID:** `HU116-alta-facultad`
- **Fecha:** 2026-10-17
- **Base Git:** develop
- **Rol / Actor Gestor:** Administrador Institucional (`ADMINISTRADOR`)
- **Jerarquía:** Nivel 1 — Custodia de Decanatura y Estructura Organizacional UCO.
- **Objetivos Principales:**
  1. Habilitar el procedimiento almacenado transaccional y atómico `dbo.usp_crear_facultad` en SQL Server, asegurando control transaccional explícito (`SET XACT_ABORT ON;`, `BEGIN TRY/TRANSACTION ... COMMIT/ROLLBACK`), validación de unicidad de nombre de facultad y retorno de la tupla estándar de 4 columnas (`idResultado`, `exitoso`, `mensajeUsuario`, `mensajeTecnico`).
  2. Implementar bajo Clean Architecture el endpoint `POST /api/v1/admin/facultades` en Backend (removiendo el stub `FeatureUnavailableException` HTTP 501 en `AdminPortalController`), orquestando la entrada mediante `CrearFacultadInputPort`, `CrearFacultadInteractor`, `CrearFacultadUseCase` y persistencia con `FacultadRepositorySqlServerAdapter`.
  3. Conectar el formulario reactivo existente en el Frontend Angular (`catalog-facultades.component.ts` vía `AdminManagementService.crearFacultad()`), garantizando actualización reactiva de las señales (`signals`), feedback instantáneo y cero regresiones.

---

## 2. Matriz AS-IS vs. TARGET

| Dimensión | Estado Anterior (AS-IS) | Estado Ajustado (TARGET) |
| :--- | :--- | :--- |
| **Base de Datos (SP)** | `schema/stored-procedures/usp_crear_facultad.sql` es un borrador no transaccional (falta `SET XACT_ABORT ON;`, `BEGIN TRANSACTION`, `COMMIT/ROLLBACK`), no asigna `institucion` ni `estado` (obligatorios en `Facultad.sql`), y retorna la tupla no estándar `(idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado)`. | `dbo.usp_crear_facultad` normalizado, transaccional y atómico, con validación de obligatoriedad, verificación de unicidad de nombre, resolución de `institucion` institucional UCO, y retorno canónico de 4 columnas: `(idResultado, exitoso, mensajeUsuario, mensajeTecnico)`. |
| **Backend API** | `AdminPortalController.crearFacultad()` lanza `FeatureUnavailableException("crear facultad")` mapeado a HTTP 501 Not Implemented. No existen puertos ni interactores de creación de facultades. | Endpoint `POST /api/v1/admin/facultades` habilitado, securizado con `@PreAuthorize("hasRole('ADMINISTRADOR')")`, retornando HTTP 201 Created con `ApiDataResponse<FacultadDTO>` y validación de entrada `CrearFacultadRequest`. |
| **Arquitectura Backend** | Módulo de administración solo cuenta con `consultarfacultades` (de solo lectura). | Se añade el módulo temático `application/features/admin/crearfacultad/` respetando Puertos y Adaptadores (Clean Architecture) sin acoplamiento a JDBC ni JPA en el dominio. |
| **Frontend Web** | `catalog-facultades.component.ts` posee la UI lista y emite hacia `AdminManagementService.crearFacultad()`, pero la llamada a la API falla con HTTP 501. | Flujo de extremo a extremo operativo: el usuario Administrador completa el formulario, se invoca `POST /api/v1/admin/facultades`, se persiste la facultad y se actualiza reactivamente la lista en pantalla (`_facultades.update(...)`) con notificación Toast. |

---

## 3. Clase de Cambio y Perímetro de Rutas

- **Clase de Cambio:** `BEHAVIOR_CHANGE`
- **PRIMARY_VARIABLE:** Alta y persistencia de Facultad institucional en las 3 capas.
- **Rutas Permitidas (`ALLOWED_PATHS`):**
  - `GestioAsistenciaBackend/docs/work-items/HU116-alta-facultad/*`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/features/admin/crearfacultad/**`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/academic/**`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/**`
  - `GestioAsistenciaBackend/src/test/java/co/edu/uco/asistenciasuco/**`
  - `GestioAsistenciaDB/schema/stored-procedures/usp_crear_facultad.sql`
  - `GestioAsistenciaDB/test/**`
  - `GestioAsistenciaFrontend/src/app/core/services/admin-management.service.ts`
  - `GestioAsistenciaFrontend/src/app/features/admin/admin-catalogs/**`
- **Rutas Prohibidas (`FORBIDDEN_PATHS`):**
  - `AsistenciasUCO-Infra/docs/contracts/**` (Contratos WORM congelados, sin alterar hashes existentes)
  - `GestioAsistenciaDB/schema/tables/**` (Estructura física de tablas base congelada; no alterar DDL)
  - Controladores y servicios de otros roles (`docente`, `estudiante`, `coordinador`, `decano`)
  - Clases transversales fuera del alcance de administración de facultades

---

## 4. Alcance y No Alcance

### Alcance (In Scope)
1. **Base de Datos:**
   - Normalizar `dbo.usp_crear_facultad` conforme al estándar institucional (`SET XACT_ABORT ON;`, control transaccional atómico, validaciones de obligatoriedad y unicidad).
   - Crear suite de pruebas SQL transaccionales en `GestioAsistenciaDB/test/test_usp_crear_facultad.sql` cubriendo Happy Path, duplicados y validación de parámetros nulos.
2. **Backend:**
   - Crear DTOs de entrada: `CrearFacultadRequest` y `CrearFacultadDTO`.
   - Definir interfaces de puertos: `CrearFacultadInputPort` y puerto secundario de persistencia `FacultadCommandPort`.
   - Implementar `CrearFacultadInteractor` y `CrearFacultadUseCaseImpl` con validaciones de negocio.
   - Implementar adaptador de persistencia en `FacultadRepositorySqlServerAdapter` invocando `dbo.usp_crear_facultad` y mapeando la tupla estándar.
   - Actualizar `AdminPortalController.java` para exponer `POST /api/v1/admin/facultades` con OpenAPI 3.0.
   - Tests unitarios JUnit 5 + Mockito para Interactor y Controller.
3. **Frontend:**
   - Verificar la llamada HTTP en `AdminManagementService.crearFacultad()`.
   - Integración con el formulario reactivo de `catalog-facultades.component.ts`.
   - Comprobar que la lista de facultades se refresca automáticamente vía Signals al crear una nueva.

### No Alcance (Out of Scope)
1. **Alta y provisión del Usuario Decano:** La creación y aprovisionamiento IAM del Decano titular se gestiona mediante **`HU117-B`** (`POST /api/v1/admin/decanos`). HU116 asume como precondición que el Decano titular ya existe en el sistema para asociar su ID obligatorio (`NOT NULL` en `dbo.Facultad`).
2. **Reasignación de Decano o cambio de Sede:** Corresponde a **`HU117`**.
3. **Creación de Programas Académicos:** Corresponde a **`HU076`** y es potestad del Decano, no del Administrador.
4. **Edición o Desactivación de Facultades:** Corresponde a **`HU110`** y **`HU111`**.

---

## 5. Inventario de Archivos Afectados

### Archivos Existentes (A Modificar)
- `GestioAsistenciaDB/schema/stored-procedures/usp_crear_facultad.sql`: Normalización transaccional y retorno canónico.
- `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java`: Sustitución de stub 501 por invocación al `CrearFacultadInputPort`.
- `GestioAsistenciaFrontend/src/app/core/services/admin-management.service.ts`: Confirmar retorno tipado de `POST /admin/facultades`.

### Archivos Nuevos (A Crear)
- `GestioAsistenciaBackend/docs/work-items/HU116-alta-facultad/PLAN.md` (este documento).
- `GestioAsistenciaBackend/docs/work-items/HU116-alta-facultad/CONTRACT.md` (contratos formales de DB y API).
- `GestioAsistenciaBackend/docs/work-items/HU116-alta-facultad/TEST_PLAN.md` (matriz de pruebas RED).
- `GestioAsistenciaBackend/docs/work-items/HU116-alta-facultad/VALIDATION.md` (evidencia de ejecución de Quality Gates).
- `GestioAsistenciaBackend/docs/work-items/HU116-alta-facultad/CLOSURE.md` (cierre formal y actualización de línea base).
- Clases Backend bajo `co.edu.uco.asistenciasuco.application.features.admin.crearfacultad`:
  - `primaryports/CrearFacultadInputPort.java`
  - `primaryports/dto/CrearFacultadDTO.java`
  - `primaryports/interactor/CrearFacultadInteractor.java`
  - `usecase/CrearFacultadUseCase.java`
  - `usecase/impl/CrearFacultadUseCaseImpl.java`
  - `secondaryports/FacultadCommandPort.java`
- Adaptador Backend:
  - `infrastructure/adapter/secondary/persistence/sqlserver/FacultadRepositorySqlServerAdapter.java` (o extensión del repositorio existente).
- Pruebas:
  - `GestioAsistenciaDB/test/test_usp_crear_facultad.sql`
  - `GestioAsistenciaBackend/src/test/java/co/edu/uco/asistenciasuco/application/features/admin/crearfacultad/CrearFacultadUseCaseTest.java`

---

## 6. Contratos y Consumidores Afectados

- **Contrato DB:** `dbo.usp_crear_facultad` con parámetros:
  - `@nombreFacultad NVARCHAR(150)` (Obligatorio)
  - `@idInstitucion UNIQUEIDENTIFIER = NULL` (Opcional, resuelve UCO por defecto)
  - `@idDecano UNIQUEIDENTIFIER = NULL` (Opcional)
  - `@idCorrelacion UNIQUEIDENTIFIER = NULL`
  - `@idUsuarioEjecutor UNIQUEIDENTIFIER = NULL`
- **Contrato HTTP REST:** `POST /api/v1/admin/facultades`
  - Request: `{"nombre": "Facultad de Ciencias de la Salud", "idInstitucion": "uuid", "idDecano": "uuid"}`
  - Response: `201 Created` con `ApiDataResponse<FacultadDTO>`
- **Consumidores:** Frontend Web Angular (Módulo Administrador Institucional).

---

## 7. Riesgos, Rollback y Stop Conditions

- **Riesgos Identificados:**
  1. *Falta de Institución base:* La tabla `Facultad` tiene clave foránea `FK_Facultad_Institucion` hacia `Institucion`. El SP debe resolver la institución UCO existente si el parámetro no se suministra.
  2. *Violación de unicidad de nombre:* Si se intenta crear una facultad con el mismo nombre (case-insensitive), el sistema debe capturarlo y retornar un error de negocio (`ERR_FACULTAD_YA_EXISTE` / HTTP 409 Conflict) en lugar de un error 500 no controlado.
- **Condiciones de Parada (`STOP_CONDITIONS`):**
  - `CONTRACT_CONFLICT`: Si la firma del SP colisiona con llamadas previas no documentadas.
  - `TEST_CONTRACT_CONFLICT`: Si las aserciones de prueba entran en conflicto con la especificación.
  - `BLOCKED_BY_MISSING_EVIDENCE`: Si no existe una institución de prueba o entorno DB activo.
- **Plan de Rollback:**
  - Revertir `AdminPortalController.java` a su versión anterior (`git checkout`).
  - Restaurar el script previo de `usp_crear_facultad.sql`.

---

## 8. Definition of Ready (DoR) — Evaluación Formal

| Criterio DoR | Estado | Evidencia |
| :--- | :---: | :--- |
| **1. Objetivo y Alcance Delimitados** | ✅ CUMPLIDO | HU116 y sus dependencias de jerarquía (Nivel 1) claramente delimitadas. |
| **2. Contratos Identificados** | ✅ CUMPLIDO | Endpoint `POST /api/v1/admin/facultades` y SP `usp_crear_facultad` especificados. |
| **3. Matriz de Pruebas TDD Inicial** | ✅ CUMPLIDO | Happy Path, Unicidad de Nombre y RBAC contemplados en el alcance. |
| **4. Repositorios y Entorno Listos** | ✅ CUMPLIDO | Base de datos SQL Server Docker y backend Spring Boot operativos. |
| **5. Cero Bloqueos o Conflictos Abiertos** | ✅ CUMPLIDO | No hay conflictos de contrato en el componente de facultades. |
| **6. Plan de Rollback Viable** | ✅ CUMPLIDO | Procedimiento de reversión documentado y sin impacto destructivo. |

**Dictamen DoR:** 👉 **`READY`**  
Se autoriza formalmente la apertura del Work Item y el paso a la **Fase 02: Congelamiento de Contratos (`CONTRACT.md`)**.
