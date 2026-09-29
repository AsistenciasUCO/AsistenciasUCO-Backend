---
status: closed
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# PLAN — HU050: Matrícula Manual de Estudiante por Docente (JPA Migration + Enrolamiento)

Fase 01-planificador / 02-contratos.

## Identidad y objetivo

- **Historia de Usuario:** HU050 (Matrícula o registro manual del estudiante por docente).
- **Work Item:** HU050-matricula-manual-docente
- **Fecha:** 2026-09-29.
- **Base Git:** develop / sergio.
- **Objetivo:** Migrar la persistencia de egistrarEstudianteEnGrupo a Spring Data JPA (vía EntityManager / StoredProcedureQuery canónico) conforme a JDBC_TO_JPA.md, soportar tanto la creación de nuevos estudiantes como el enrolamiento de estudiantes ya existentes sin lanzar FeatureUnavailableException, y habilitar el flujo completo en Frontend.

## AS-IS y Evidencia

| Dimensión | Estado Actual | Estado Target |
|---|---|---|
| **Tecnología Persistencia** | Spring JDBC (NamedParameterJdbcTemplate en GrupoRepositorySqlServerAdapter) | Spring Data JPA (EntityManager.createStoredProcedureQuery en Infrastructure) |
| **Comando SQL** | dbo.usp_registrar_estudiante_en_grupo (11 parámetros) | dbo.usp_registrar_estudiante_en_grupo (11 parámetros en JPA) |
| **Estudiante Existente** | GrupoController lanza FeatureUnavailableException si estudianteId != null | Soporta enrolamiento de estudiantes existentes sin excepción |
| **Frontend Modal** | Muestra estudiante existente pero emite payload genérico que requiere soporte limpio | Flujo transparente para matricular tanto estudiante nuevo como existente |

## Definition of Ready (DoR)

- [x] Contrato del Stored Procedure dbo.usp_registrar_estudiante_en_grupo congelado en base de datos.
- [x] Reglas de Clean Architecture y aislamiento JPA verificadas (sin entidades ni JPA en Application).
- [x] Metodología TDD estricta (Plan -> Contrato -> Test RED -> Implementación GREEN -> Validación -> Cierre).
- **Estado DoR:** READY para implementación.
