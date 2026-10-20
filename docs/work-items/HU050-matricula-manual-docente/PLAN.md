---
status: closed
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-17
---

# PLAN — HU050: Matrícula Manual de Estudiante por Docente (JPA Moderno + Enrolamiento Simplificado)

Fase 01-planificador / 02-contratos.

## 1. Identidad y Objetivo

- **Historia de Usuario:** HU050 (Matrícula o registro manual del estudiante por docente).
- **Work Item:** HU050-matricula-manual-docente
- **Fecha:** 2026-10-17.
- **Base Git:** develop.
- **Objetivo:** 
  1. Habilitar la matrícula de estudiantes por parte del docente titular mediante el endpoint `POST /api/v1/grupos/{grupoId}/estudiantes`, permitiendo tanto el registro de un nuevo estudiante como el enrolamiento de estudiantes ya existentes (por `estudianteId` o solo con `tipoIdentificacionId` y `numeroIdentificacion`).
  2. Autorizar al rol `DOCENTE` para consultar el directorio institucional de estudiantes (`GET /api/v1/estudiantes/**`) con el fin de validar y autocompletar la identidad del alumno en el modal de matrícula del frontend.
  3. Integrar y alinear la persistencia sobre la arquitectura JPA moderna consolidada (`GrupoJpaRepository` vía `JpaProcedureExecutor` invocando `dbo.usp_registrar_estudiante_en_grupo`), eliminando adaptadores híbridos legacy y excepciones artificiales `FeatureUnavailableException`.

## 2. AS-IS vs TARGET

| Dimensión | Estado Anterior (AS-IS) | Estado Ajustado (TARGET) |
|---|---|---|
| **Tecnología Persistencia** | Clases sueltas `*CommandPersistence` en rama desfasada / fallback JDBC legacy | `GrupoJpaRepository` consolidado bajo Clean Architecture invocando `dbo.usp_registrar_estudiante_en_grupo` (11 parámetros) con `JpaProcedureExecutor`. |
| **Comando SQL** | `dbo.usp_registrar_estudiante_en_grupo` (11 parámetros) | `dbo.usp_registrar_estudiante_en_grupo` ejecutado vía JPA con mapeo canónico de 4 columnas. |
| **Estudiante Existente** | `GrupoController` lanzaba `FeatureUnavailableException` si `estudianteId != null`. | `GrupoController` delega a `RegistrarEstudianteInputPort`, permitiendo matrícula transparente. |
| **Matrícula por Documento** | Requería obligatoriamente nombres, apellidos, correo y password en el request HTTP. | `RegistrarEstudianteRequestValidator` y `RegistrarEstudianteInteractor` permiten matricular alumnos existentes proveyendo únicamente `tipoIdentificacionId` y `numeroIdentificacion`. |
| **Autorización Docente** | `/api/v1/estudiantes/**` restringido únicamente a `COORDINADOR` y `ADMINISTRADOR` (HTTP 403 para docente). | `/api/v1/estudiantes/**` abierto para `DOCENTE`, `COORDINADOR` y `ADMINISTRADOR`. |
| **Identidad en Keycloak** | Error 500 al intentar crear cuenta duplicada en Keycloak para usuarios institucionales existentes. | `KeycloakIdentityProviderAdapter` resuelve la identidad existente de forma idempotente y `RegistrarEstudianteUseCaseImpl` provisiona cuenta sin duplicidad. |

## 3. Definition of Ready (DoR)

- [x] Contrato del Stored Procedure `dbo.usp_registrar_estudiante_en_grupo` congelado en base de datos.
- [x] Reglas de Clean Architecture y aislamiento JPA verificadas (sin entidades ni JPA en Application).
- [x] Metodología TDD estricta (Plan -> Contrato -> Test RED -> Implementación GREEN -> Validación -> Cierre).
- **Estado DoR:** READY y COMPLETADO.
