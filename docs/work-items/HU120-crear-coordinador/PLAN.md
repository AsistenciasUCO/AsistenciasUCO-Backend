---
status: closed
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-19
---

# PLAN — HU120: Crear y Aprovisionar Coordinador de Programa en Facultad (Rol Decano)

Fase 01: Planificación y Definition of Ready (DoR) conforme a `GUIA_METODICA_IMPLEMENTACION_HU.md`, `HOJA_DE_RUTA_JERARQUIA_ROLES.md`, `DEFINITION_OF_READY.md` y `AGENTS.md`.

---

## 1. Identidad y Objetivo

- **Historia de Usuario:** HU120 (Crear y asignar Coordinador a Programa / Aprovisionamiento de Coordinador en Facultad).
- **Work Item ID:** `HU120-crear-coordinador`
- **Fecha:** 2026-10-19
- **Base Git:** develop
- **Rol / Actor Gestor:** Decano de Facultad (`DECANO`)
- **Jerarquía:** Nivel 2 — Custodia de Coordinación (Decano ➔ Coordinador ➔ Programa Académico).
- **Objetivos Principales:**
  1. **Romper el Candado Circular Relacional (Regla 5.1 de `GUIA_METODICA_IMPLEMENTACION_HU.md`):** En el esquema físico `dbo.Programa`, la columna `[coordinador] uniqueidentifier NOT NULL` exige que exista un Coordinador antes de registrar un Programa Académico (`HU076`). Por tanto, `dbo.usp_crear_coordinador` debe permitir registrar un Coordinador con `@idPrograma = NULL` (disponible en la Facultad) o vinculado a un `@idPrograma` existente, y la vista `dbo.uv_coordinador` debe usar `LEFT JOIN uv_programa` para exponer coordinadores disponibles sin programa asignado (simétrico a `dbo.uv_decano`).
  2. **Aprovisionamiento IAM en Keycloak y Consulta Completa en Backend:** Extender `CrearCoordinadorUseCaseImpl` y `DecanoWiringConfiguration` para codificar la contraseña inicial (`PasswordEncoderPort`), persistir el coordinador y aprovisionar su identidad institucional en Keycloak (`IdentityProviderPort.crearCuenta` con `InstitutionalRole.COORDINADOR`), además de incluir los coordinadores disponibles sin programa en `ConsultarCoordinadoresUseCaseImpl`.
  3. **Desbloquear UI Reactiva del Decano en Angular 19:** Actualizar `DeanCoordinadoresComponent` y `DeanManagementService` para permitir dar de alta coordinadores aun cuando la facultad recién creada tenga `0` programas registrados, mostrando el selector de programa como opcional y listando los coordinadores disponibles como `'Sin programa asignado (Disponible)'`.

---

## 2. Matriz AS-IS vs. TARGET

| Dimensión | Estado Anterior (AS-IS) | Estado Ajustado (TARGET) |
| :--- | :--- | :--- |
| **Base de Datos (`uv_coordinador` & `usp_crear_coordinador`)** | `uv_coordinador` usa `INNER JOIN uv_programa`, ocultando cualquier coordinador sin programa asignado. `usp_crear_coordinador` exige `@idPrograma` existente en `uv_programa`, creando un candado circular con `dbo.Programa.[coordinador] NOT NULL` en facultades nuevas. | `uv_coordinador` usa `LEFT JOIN uv_programa` y calcula `estaActivoCoordinador = IIF(ci.estaActivoUsuario = 1 AND (pr.id IS NULL OR pr.estaActivoPrograma = 1), 1, 0)`. `usp_crear_coordinador` acepta `@idPrograma = NULL` (validando `@idFacultad` y custodia jerárquica del Decano) o `@idPrograma IS NOT NULL` (asignándolo al programa). |
| **Backend API (`CrearCoordinadorUseCaseImpl`)** | Invoca `commandPort.crearCoordinador(...)` con el password sin codificar y no aprovisiona la cuenta institucional en Keycloak (`IdentityProviderPort`). `ConsultarCoordinadoresUseCaseImpl` solo consulta filas con `idFacultad = :idFacultad`, omitiendo coordinadores recién creados con `idPrograma = NULL`. | Codifica la credencial inicial con `PasswordEncoderPort`, sincroniza el usuario en `IdentityProviderPort` con `InstitutionalRole.COORDINADOR` y retorna tanto los coordinadores con programa de la facultad como los coordinadores disponibles sin programa asignado (`consultarCoordinadoresSinPrograma()`), deduplicados por `id`. |
| **Frontend Web (`DeanCoordinadoresComponent`)** | Bloquea el botón `"Nuevo Coordinador"` si `programas().length === 0` y exige seleccionar un programa en el formulario. | Permite abrir el formulario y registrar un coordinador con `idPrograma: null` cuando la facultad aún no tiene programas o cuando se aprovisiona como disponible para `HU076`, mostrando `'Sin programa asignado (Disponible)'`. |

---

## 3. Clase de Cambio y Perímetro de Rutas

- **Clase de Cambio:** `BEHAVIOR_CHANGE`
- **PRIMARY_VARIABLE:** Aprovisionamiento de Coordinador en Facultad (con o sin programa inicial) y sincronización IAM en las 3 capas.
- **Rutas Permitidas (`ALLOWED_PATHS`):**
  - `GestioAsistenciaBackend/docs/work-items/HU120-crear-coordinador/*`
  - `GestioAsistenciaDB/schema/views/uv_coordinador.sql`
  - `GestioAsistenciaDB/schema/stored-procedures/usp_crear_coordinador.sql`
  - `GestioAsistenciaDB/test/test_usp_crear_coordinador.sql`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/features/decano/crearcoordinador/**`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/features/decano/consultarcoordinadores/**`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/academic/CoordinadorQueryPort.java`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/CoordinadorJpaRepository.java`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/wiring/DecanoWiringConfiguration.java`
  - `GestioAsistenciaBackend/src/test/java/co/edu/uco/asistenciasuco/application/features/decano/**`
  - `GestioAsistenciaFrontend/src/app/core/models/role-management.model.ts`
  - `GestioAsistenciaFrontend/src/app/core/services/dean-management.service*.ts`
  - `GestioAsistenciaFrontend/src/app/features/dean/dean-coordinadores/**`
- **Rutas Prohibidas (`FORBIDDEN_PATHS`):**
  - `AsistenciasUCO-Infra/docs/contracts/**` (Contratos WORM congelados)
  - `GestioAsistenciaDB/schema/tables/**` (DDL físico de tablas base)

---

## 4. Alcance y No Alcance

### Alcance (In Scope)
1. **Base de Datos:**
   - Actualizar `dbo.uv_coordinador` con `LEFT JOIN uv_programa`.
   - Actualizar `dbo.usp_crear_coordinador` para soportar `@idPrograma = NULL` y `@idCoordinador = NULL` con validación jerárquica de `@idFacultad`.
   - Añadir escenario de prueba transaccional 5 (`@corrSinPrograma`) en `GestioAsistenciaDB/test/test_usp_crear_coordinador.sql`.
2. **Backend:**
   - Integrar `UsuarioRepositoryPort`, `PasswordEncoderPort` e `IdentityProviderPort` en `CrearCoordinadorUseCaseImpl` y `DecanoWiringConfiguration`.
   - Añadir `consultarCoordinadoresSinPrograma()` en `CoordinadorQueryPort` y `CoordinadorJpaRepository`, e integrarlo con deduplicación por `id` en `ConsultarCoordinadoresUseCaseImpl`.
   - Añadir tests unitarios en `CrearCoordinadorUseCaseImplTest` y `ConsultarCoordinadoresUseCaseImplTest`.
3. **Frontend:**
   - Soportar `idPrograma?: string | null` en `CreateCoordinadorPayload` (`role-management.model.ts` y `dean-management.service.ts`).
   - Habilitar creación de coordinador sin programa obligatorio en `dean-coordinadores.component.ts` y actualizar suites `*.spec.ts`.

### No Alcance (Out of Scope)
1. **Alta de Programa Académico:** Corresponde al siguiente paso inmediato (`HU076` + `HU023`), que consumirá los coordinadores aprovisionados en `HU120`.
2. **Edición y cambio de estado de Coordinador:** Corresponde a `HU121` y `HU122`.

---

## 5. Contratos y Consumidores Afectados

- **Contrato DB:**
  - `dbo.uv_coordinador` (mismas columnas; filas adicionales cuando `idPrograma IS NULL`).
  - `dbo.usp_crear_coordinador` (`@idCoordinador UNIQUEIDENTIFIER = NULL`, `@idPrograma UNIQUEIDENTIFIER = NULL`).
- **Contrato HTTP REST:**
  - `POST /api/v1/decano/coordinadores` (`idPrograma` opcional/nullable en `CrearCoordinadorDTO`).
  - `GET /api/v1/decano/coordinadores` (incluye coordinadores de la facultad y coordinadores disponibles sin programa).
- **Consumidores:** Portal Decano en Frontend Angular (`DeanCoordinadoresComponent` y selector de Coordinador en `DeanFacultyComponent` para `HU076`).

---

## 6. Riesgos, Rollback y Stop Conditions

- **Riesgos Identificados:**
  1. *Paridad JDBC vs JPA en `CoordinadorJpaRepository`:* Modificar la query de `consultarCoordinadoresPorFacultad(idFacultad)` rompería la paridad con `CoordinadorJdbcBaseline`. Se mitiga manteniendo intacta `consultarCoordinadoresPorFacultad` y agregando el método dedicado `consultarCoordinadoresSinPrograma()` con método default en el puerto.
- **Plan de Rollback:**
  - Revertir los cambios mediante `git checkout` en los archivos del perímetro permitido.

---

## 7. Definition of Ready (DoR) — Evaluación Formal

| Criterio DoR | Estado | Evidencia |
| :--- | :---: | :--- |
| **1. Objetivo y Alcance Delimitados** | ✅ CUMPLIDO | Precedencia relacional `HU120` ➔ `HU076` verificada contra `dbo.Programa`. |
| **2. Contratos Identificados** | ✅ CUMPLIDO | `uv_coordinador`, `usp_crear_coordinador` y `/api/v1/decano/coordinadores` especificados. |
| **3. Matriz de Pruebas TDD Inicial** | ✅ CUMPLIDO | Escenarios SQL, JUnit 5 y Jasmine definidos. |
| **4. Cero Conflictos con Contratos WORM** | ✅ CUMPLIDO | Ninguno de los 10 objetos DB congelados en WORM es alterado. |
| **5. Plan de Rollback Viable** | ✅ CUMPLIDO | Cambios retrocompatibles y reversibles por capa. |

**Dictamen DoR:** 👉 **`READY`**
