# Plan de Ejecución: HU076 - Agregar nuevo programa académico a la facultad

- **ID Work Item**: `HU076-agregar-programa-academico`
- **Historia de Usuario**: [HU076.md](../../../../Repositorio_GestionAsistenciaInfra/docs/user-stories/academic-structure/HU076.md)
- **Prioridad Hoja de Ruta**: Prioridad #1 — Bloque C (Decano de Facultad) en `HOJA_DE_RUTA_JERARQUIA_ROLES.md`
- **Estado**: `EN_EJECUCION`

## 1. Objetivo de Negocio y Alcance

Permitir que el usuario autenticado con rol **Decano (`DECANO`)** registre nuevos programas académicos adscritos a su facultad dentro del sistema de gestión de asistencia UCO, seleccionando el tipo de programa (`pregrado` / `postgrado`) y el coordinador responsable activo, garantizando:
1. Validación de permisos RBAC (`DECANO`) y titularidad jerárquica sobre la facultad (`FACULTAD`) bajo política *Fail-Closed*.
2. Validación de integridad referencial sobre `uv_facultad`, `uv_tipo_programa` y `uv_coordinador_identidad`.
3. Validación de longitud de nombre del programa (`3` a `50` caracteres) y unicidad de nombre (`GEN_003` -> HTTP `409 CONFLICT`).
4. Persistencia atómica en SQL Server 2022 mediante el contrato público `dbo.usp_crear_programa_academico` y su procedimiento interno `dbo.usp_crear_programa_academico_interno`.
5. Exposición de endpoints REST en `DecanoPortalController` (`/api/v1/decano/programas` y `/api/v1/decano/tipos-programa`) bajo Arquitectura Hexagonal / Clean Architecture (POJOs puros en `application`).
6. Interfaz reactiva en Angular 19 (`DeanProgramasComponent` y `DeanManagementService`) para listar programas de la facultad y registrar nuevos programas académicos con validaciones de formulario y notificaciones al usuario.

## 2. Auditoría de Contratos WORM y Restricciones Críticas

- **Manifiesto WORM (`DB_DEVELOP_FREEZE_MANIFEST.sha256`)**:
  - Los objetos `uv_programa.sql`, `uv_auth_programa.sql`, `uv_tipo_programa.sql`, `usp_crear_programa_academico.sql` y `usp_crear_programa_academico_interno.sql` **no** forman parte del manifiesto congelado de 10 archivos WORM.
- **Invariantes del Suite de Pruebas de Base de Datos (`test_suite.sql`)**:
  - `test_contract_closure.sql` prohíbe el literal `@idUsuarioEjecutor IS NOT NULL` en `sys.sql_modules` y exige que todo procedimiento público de mutación retorne exactamente 4 columnas descriptibles estáticamente (`idCorrelacion`, `mensajeUsuarioResultado`, `mensajeTecnicoResultado`, `estadoResultado`).
  - `test_reactive_academic.sql` invoca `dbo.usp_crear_programa_academico` pasando `@idPrograma` explícito dos veces consecutivas para validar upsert idempotente por `@idPrograma`, mientras que la creación de un programa con un `@idPrograma` distinto (o nulo) y nombre duplicado debe rechazarse con `GEN_003`.
- **Zero Test Tampering**:
  - Ningún test preexistente en DB, Backend o Frontend será modificado en sus aserciones existentes; únicamente se agregarán nuevos casos de prueba.

## 3. Fases de Implementación

1. **Fase 01 - Descubrimiento y Análisis**: Auditoría de esquema SQL, vistas autorizadas, puertos secundarios del backend y estado actual del portal del Decano en el frontend.
2. **Fase 02 - Definición de Contratos (SDD)**: Especificación formal en `CONTRACT.md` para SQL Server, API REST y modelos TypeScript.
3. **Fase 03 - Estrategia de Pruebas (TDD)**: Diseño de casos de prueba en `TEST_PLAN.md` e implementación de tests en las 3 capas.
4. **Fase 04 - Implementación Vertical**:
   - **Capa 1 (DB)**: `uv_programa.sql`, `uv_auth_programa.sql`, `usp_crear_programa_academico_interno.sql`, `usp_crear_programa_academico.sql`, `test_usp_crear_programa_academico.sql`.
   - **Capa 2 (Backend)**: Puertos secundarios (`ProgramaQueryPort`, `ProgramaCommandPort`, `TipoProgramaQueryPort`), adaptadores JPA (`UvProgramaEntity`, `UvTipoProgramaEntity`, `ProgramaJpaRepository`, `TipoProgramaJpaRepository`), casos de uso (`crearprograma`, `consultarprogramas`, `consultartiposprograma`), wiring (`DecanoWiringConfiguration`) y controlador (`DecanoPortalController`).
   - **Capa 3 (Frontend)**: Modelos (`role-management.model.ts`), servicio (`dean-management.service.ts`) y vista interactiva (`dean-programas.component.ts`).
5. **Fase 05 - Validación Integral**: Ejecución de `test_suite.sql`, `./mvnw test` (incluyendo ArchUnit) y build/tests de Angular.
6. **Fase 06 - Cierre y Certificación**: Documentación de evidencias en `VALIDATION.md` y `CLOSURE.md`, y actualización de `HU-CATALOG.md` y `HOJA_DE_RUTA_JERARQUIA_ROLES.md`.
