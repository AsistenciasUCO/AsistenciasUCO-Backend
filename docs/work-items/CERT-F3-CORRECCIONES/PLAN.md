---
status: active
type: work-item
scope: database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-06
---

# PLAN — Correcciones de la certificación de Fase 3 (2026-11-06)

## Origen

Recorrido en pantalla de las 43 HU en Fase 3, con los usuarios de prueba del lab Security Baseline. El reporte completo está fuera de los repos, en `informes/CERTIFICACION_FASE3_2026-11-06/REPORTE.md`. John pidió reparar todo lo que fallara sin romper reglas.

## Cambios

| HU | Capa | Causa | Corrección |
|---|---|---|---|
| HU136 | Backend | `ConsultarProgramasUseCaseImpl` exigía facultad de decano y respondía 403 al ADMINISTRADOR, aunque `access-catalog` le concede `GET /api/v1/decano/programas` y `uv_auth_programa` ya lo acota a su institución | El administrador activo consulta sin la regla del decano; la BD fija el ámbito. El decano no cambia |
| HU051, HU053-HU055 (cupos) | Backend | `/docente/asignaturas` no traía la capacidad del grupo y el hub inventaba "35" | `capacidadMaximaPermitida` aditivo (de `uv_grupo`) en `AsignaturaDocenteDTO`, con constructores compatibles |
| HU051, HU053-HU055 | Frontend | Ver el work item homónimo del frontend | — |
| HU094 | Backend | `DocenteProgramaReporteJpaRepository` ejecutaba el SP con `JpaQueryExecutor` y convertía el `GEN_001` de "docente fuera del programa" en 500 | Traduce el DBCODE con `DbCodeTranslator`: GEN_001 responde 404 "El docente especificado no existe", como las demás consultas por SP |
| HU094, HU095, HU159 | Backend | `findProgramaIdByCoordinadorUsuario` tomaba "el primero" sin orden; con datos anteriores a la regla 1 a 1 (`PROG_005`), cada pantalla podía resolver un programa distinto | `HQL_PROGRAMA_POR_COORDINADOR` prefiere el programa activo y ordena por nombre e id. La regla 1 a 1 ya existe en la BD |
| HU006, HU008 | BD (seed) | El seed 14 inscribía estudiantes sin `EstudiantePrograma` | Ver el work item homónimo de la BD |
| HU145 | Backend | El cronograma del decano mostraba el `fechaHoraInicio` v1 (guardado en UTC) como hora local: 13:00 en vez de las 08:00 que ve el coordinador. Era el bloque pendiente de APP-04 | `ConsultarFacultadDecanoUseCaseImpl` completa cada sesión con `SesionProcedenciaQueryPort` (una consulta por grupo de la página, solo la fila del mismo grupo e id), con el mismo criterio que el coordinador. `SesionFacultadDecanoResponse` agrega `fechaHoraInicioUtc`/`fechaHoraFinUtc` con `Z` (solo si es CONFIRMADA) y `estadoTemporal`. Sin cambios en la BD |
| HU147 / HU081-HU083 (plan del coordinador) | Backend | `/coordinador/planes-estudio` no traía los totales de la malla y la pantalla mostraba "Créditos: 0" y "Asignaturas 0" con ARQ-402 (3 créditos) en la malla | `PlanEstudioJpaRepository.HQL_TOTALES_MALLA` (por lote, junto a la de semestres) con la misma regla que `usp_consultar_programas_creditos_facultad_paginado`; `totalAsignaturas` y `totalCreditos` aditivos en proyección, dominio y DTO, con constructores compatibles |

## Contrato

- `GET /api/v1/decano/programas`: ahora responde 200 al ADMINISTRADOR con los programas de su institución, en vez de 403. Para el DECANO no cambia nada.
- `GET /api/v1/docente/asignaturas`: agrega `capacidadMaximaPermitida` (entero o nulo). El cambio es aditivo.
- `GET /api/v1/decano/sesiones`: agrega `fechaHoraInicioUtc`, `fechaHoraFinUtc` (texto UTC con `Z`, `null` si no es CONFIRMADA) y `estadoTemporal` (`CONFIRMADA` o `INDETERMINADA`). Los campos v1 no cambian.
- `GET /api/v1/coordinador/planes-estudio`: agrega `totalAsignaturas` y `totalCreditos` (enteros; 0 si el plan no tiene asignaturas).
- `GET /api/v1/decano/coordinadores`: agrega `nombreFacultad`, `totalDocentes` (docentes con vinculación activa en el programa) y `totalGrupos` (grupos habilitados del programa). Un coordinador sin programa trae 0 y 0.
- `access-catalog` y `SecurityConfig` no cambian. El PDP no necesita reaprovisionarse.

## Decisiones de John aplicadas (2026-11-06, "sí a las 5 en este paquete")

| Decisión | Capa | Cambio |
|---|---|---|
| Semestres del plan: una sola regla | BD | `usp_consultar_estructura_facultad` y `usp_consultar_programas_creditos_facultad_paginado` pasan de contar semestres a `MAX(numeroSemestre)`, la regla HU082/HU083 del coordinador. Ver el work item homónimo de la BD |
| Conteos reales en la tarjeta del coordinador | Backend | `CoordinadorQueryPort.consultarResumenProgramasFacultad` (método default) y `CoordinadorJpaRepository.SQL_RESUMEN_PROGRAMAS_FACULTAD`: consulta nativa de solo lectura sobre `uv_programa`, `uv_docente_programa` (vinculación activa) y `uv_grupo_programa` (grupos habilitados). `CoordinadorDomain`/`CoordinadorDTO` agregan `nombreFacultad`, `totalDocentes` y `totalGrupos`, con constructores compatibles. Las vistas ya tienen el GRANT del rol runtime (`uv_*`); la BD no cambia |
| Retiro de los campos "Aula" | Frontend | Ver el work item homónimo del frontend |
| HU038: catálogo al texto original | Infra (docs) | Va en la rama de documentación de Infra con los estados de certificación |
| HU159 con un decano desechable | Prueba manual | Se crea por el flujo normal (el administrador registra el decano) cuando haya login en el lab |

## Rollback

Revertir el commit.
