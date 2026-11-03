---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-02
---

# TEST_PLAN — HU112 / HU128 / HU143 / HU157

## DB (`test/test_consultas_transversales_hu112_hu157.sql`)

| ID | Caso |
|---|---|
| T-112-01 | El plan trae la suma real de créditos y el número de asignaturas; solo programas de la facultad; el decano ajeno no ve nada. |
| T-128-01 | El filtro por programa trae al docente vinculado y solo docentes con vínculo activo, sin repetidos; el decano ajeno no ve nada. |
| T-143-01 | El decano titular encuentra la asignatura por código y solo ve su facultad; el decano de otra facultad no la ve; el administrador ve la institución con filtro por facultad. |
| T-157-01 | El administrador encuentra al estudiante por documento. |
| T-157-02 | Historial con la matrícula (administrador); el del decano se limita a su facultad; el decano ajeno no ve nada; un usuario sin perfil recibe `SEC_001`. |
| T-SEC-03 | `SEC_001` para el usuario sin el perfil exigido en cada SP paginado. |
| T-PAG-02 | `PAG_001` (página 0, tamaño 0, página nula) y `VAL_001` (ejecutor o estudiante nulos). |
| T-INT-01 | Interno compartido: éxito con desplazamiento y contexto, `VAL_001`, `SEC_001`, `PAG_001`; el contexto se restaura y la transacción del llamador sigue confirmable. |

## Backend

| Clase | Cobertura |
|---|---|
| `ConsultasTransversalesUseCaseImplTest` | Valores por defecto, búsqueda recortada o vacía, filtro nulo, paginación y búsqueda inválidas (400 sin consultar), usuario nulo (403), historial con estudiante nulo (400), vacío (404) y con filas. |
| `ConsultasTransversalesInteractorTest` | Mapeo de las 4 listas; ficha armada con la primera fila y una matrícula por fila; matrícula sin grupo con `resumen` nulo. |
| `ConsultaPaginadaRulesTest` | Página, tamaño y búsqueda: defectos, límites y rechazos. |
| `ConsultasTransversalesJpaRepositoryTest` | `EXEC` nombrado de los 5 SP con sus parámetros; UUID en texto; banderas `BIT`/`INT`; total 0 sin filas; historial con y sin grupo; porcentaje no decimal; `PAG_001` ⇒ 400, `SEC_001` ⇒ 403, fallo técnico ⇒ `DatabaseOperationException`. |
| `DecanoConsultasTransversalesControllerTest` | Serialización de las 4 rutas del decano, 403, 400 y 404. |
| `AdminConsultasTransversalesControllerTest` | Serialización de las 3 rutas del administrador, historial sin grupos y 403. |
| `JpaRepositoryBeanUniquenessTest` | Conteo de repositories +1. |
| `AccessCatalogConsistencyTest` | Las 7 rutas nuevas tienen regla en el catálogo y coinciden con `SecurityConfig`. |
| ArchUnit (`CleanArchitectureRulesTest` y demás) | Capas, ubicación de DTO e interactor, sin referencias a procedimientos internos. |

## Frontend

| Spec | Cobertura |
|---|---|
| `dean-docentes.component.spec.ts` | Listado, filtro por programa, búsqueda, estado, paginación y error. |
| `dean-asignaturas.component.spec.ts` | Búsqueda por código o nombre, estado, paginación, sin resultados y error. |
| `admin-estudiantes.component.spec.ts` | Buscador, filtro por facultad, apertura de la ficha con historial, matrícula sin grupo y errores. |
| `dean-programas-creditos.component.spec.ts` | Tabla unificada, búsqueda, paginación, programa sin plan y error. |
| `dean-programas.component.spec.ts` | Las tarjetas toman los créditos de la tabla, sin una llamada a la malla por plan. |
| `dean-estudiantes.component.spec.ts` | La ficha muestra el historial del estudiante. |
| Servicios y mock | Rutas, parámetros y simulación de las consultas nuevas. |
