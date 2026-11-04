---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# TEST_PLAN — HU061 / HU147 / HU153 / HU172 y P5

## DB

`test/test_consolidado_auditoria_catalogos.sql` (T-061-01, T-172-01, T-147-01, T-153-01, T-P5-01, T-ASI-01, T-PAG-03), `test/test_rec2_rbac_asignatura.sql` (RA-05) y `test/test_subject_catalog.sql`. Detalle en el TEST_PLAN del work item homónimo de la BD.

## Backend

| Test | Caso |
|---|---|
| `ConsultasConsolidadoAuditoriaUseCaseImplTest` | Consolidado con filtro vacío y paginación por defecto; tamaño > 100 da 400. Auditoría: sin rango usa los últimos 7 días y solo asistencia; normaliza a UTC, acción a mayúsculas y filtros vacíos a nulo; solo `hasta` toma 7 días antes; rango vacío, > 90 días, resultado fuera de contrato o filtro > 120 dan 400; acepta exactamente 90 días. Componentes delega; sin usuario da 403. |
| `ConsultasConsolidadoAuditoriaJpaRepositoryTest` | EXEC y parámetros de los 3 SP; mapeo de métricas, fecha y correlación; conversión de los tipos de fecha del driver; `SEC_001` → 403. |
| `ConsultasConsolidadoAuditoriaInteractorTest` | Conversión de filtros DTO (soloAsistencia nulo = true) y mapeo a DTO. |
| `ConsolidadoAsistenciaControllerTest` | Las dos rutas del consolidado con filtros; paginación inválida 400; componentes. |
| `AdminConsultasTransversalesControllerTest` | `GET /admin/auditoria` con fechas ISO y filtros. |
| `GuardarTipoIdentificacionUseCaseImplTest` | Alta normaliza; edición ignora el código; código y nombre inválidos dan 400 sin llamar al provider. |
| `TipoIdentificacionCommandJpaRepositoryTest` | Parámetros del EXEC en alta y edición. |
| `AdminTipoIdentificacionControllerTest` | POST 201 con el tipo creado; PUT 200; duplicado 409; tipo no releído 404. |
| `DbFailureClassifierAsignaturaTest` | `ASI_001` → 400; `GEN_001` en crear/actualizar asignatura y guardar tipo de documento → 404. |
| `AdminPortalControllerTest` | El stub de auditoría ya no existe. |
| ArchUnit, `JpaRepositoryBeanUniquenessTest` (54), `AccessCatalogConsistencyTest` | Reglas de arquitectura, repositorio nuevo y rutas nuevas. |

## Frontend

| Spec | Caso |
|---|---|
| `consolidado-asistencia.component.spec.ts` | Ruta del coordinador y del decano, métricas, filtro por cohorte desde la página 1, error del backend, opciones acumuladas. |
| `admin-auditoria.component.spec.ts` | Por defecto 7 días y solo asistencia; acción en mayúsculas, resultado y "todas"; rango inválido no consulta; `rangoUtc` y máximo de 90 días. |
| `admin-system.component.spec.ts` | La pestaña de auditoría consulta el endpoint real. |
| `admin-tipos-documento.component.spec.ts` | Lista sin acciones de estado; validación de código y nombre; alta en mayúsculas; edición solo del nombre; error del backend. |
| `subject-form.component.spec.ts` | Solo componentes reales, áreas activas y semestres del plan; créditos sin valor por defecto, enteros y > 0; área, componente y semestre obligatorios; edición conserva lo que llega. |
| `subject-form-page.component.spec.ts` | Carga componentes y áreas; el cuerpo enviado usa `semestreNumero`, `nombreArea` y `nombreComponente`. |
| `consultas-transversales.service.spec.ts`, `mock-consultas-transversales.spec.ts` | Rutas, parámetros y simulación de los 3 endpoints nuevos. |

## Validación manual

MV-013 del ledger: pantalla de consolidado (coordinador y decano), auditoría y tipos de documento (administrador) y formulario de asignaturas con usuarios reales y el PDP reaprovisionado.
