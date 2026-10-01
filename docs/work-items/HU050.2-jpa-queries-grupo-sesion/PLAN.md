---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# PLAN — HU050.2: Migración de Consultas Paginadas de Grupo y Sesión a JPA

Fase 01-planificador / 02-contratos.

## 1. Identidad y Objetivo

- **Work Item:** HU050.2-jpa-queries-grupo-sesion
- **Fecha:** 2026-09-29
- **Base Git:** eature/hu050-matricula-docente-jpa
- **Objetivo:** Migrar las operaciones de consulta paginada consultarGrupos(), consultarEstudiantesGrupo(UUID) y consultarSesionesPorGrupo(UUID) desde Spring JDBC (NamedParameterJdbcOperations) hacia Spring Data JPA / Hibernate (StoredProcedureQuery) bajo el estándar institucional JDBC_TO_JPA.md, preservando la compatibilidad de fallback JDBC y respetando el contrato de la base de datos (READ_QUERY_RESULTSET).

## 2. AS-IS vs TARGET

| Dimensión | Estado AS-IS | Estado TARGET |
|---|---|---|
| **consultarGrupos** | Invoca usp_consultar_grupos_paginado vía NamedParameterJdbcOperations en GrupoRepositorySqlServerAdapter | Delega a GrupoJpaQueryPersistence vía EntityManager.createStoredProcedureQuery, mapeando a GrupoRepositoryProjection |
| **consultarEstudiantesGrupo** | Invoca usp_consultar_estudiantes_grupo_paginado vía NamedParameterJdbcOperations en GrupoRepositorySqlServerAdapter | Delega a GrupoJpaQueryPersistence vía EntityManager.createStoredProcedureQuery, mapeando a EstudianteGrupoRepositoryProjection |
| **consultarSesionesPorGrupo** | Invoca usp_consultar_sesiones_grupo_paginado vía NamedParameterJdbcOperations en SesionRepositorySqlServerAdapter | Delega a SesionJpaQueryPersistence vía EntityManager.createStoredProcedureQuery, mapeando a SesionRepositoryProjection |
| **Fallback JDBC** | N/A | Preservado intacto si EntityManagerFactory o el proveedor JPA no está disponible |
| **Composition Root** | Solo inyectaba GrupoJpaCommandPersistence | Inyecta GrupoJpaQueryPersistence y SesionJpaQueryPersistence compartiendo el EntityManagerFactory institucional |

## 3. Rutas Permitidas y Prohibidas

- **Permitidas:**
  - src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/
  - src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/core/GrupoRepositorySqlServerAdapter.java
  - src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/core/SesionRepositorySqlServerAdapter.java
  - src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/adapters/persistence/sqlserver/
  - src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/core/
  - docs/work-items/HU050.2-jpa-queries-grupo-sesion/
- **Prohibidas:**
  - Modificar contratos de dominio o puertos primarios/secundarios en co.edu.uco.asistenciasuco.application.
  - Modificar esquemas DDL o procedimientos almacenados de base de datos sin addendum contractual.
  - Eliminar o romper implementaciones JDBC existentes.

## 4. Definition of Ready (DoR)

- [x] Contratos de SPs (usp_consultar_grupos_paginado, usp_consultar_estudiantes_grupo_paginado, usp_consultar_sesiones_grupo_paginado) verificados en el repositorio de BD.
- [x] Columnas y tipos de parámetros mapeados según sys.parameters.parameter_id.
- [x] Reglas de Clean Architecture y aislamiento JPA verificadas (persistencia aislada en adaptadores secundarios de infraestructura).
- **Estado DoR:** READY.
