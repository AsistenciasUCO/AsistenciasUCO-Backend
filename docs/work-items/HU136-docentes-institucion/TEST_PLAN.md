---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# TEST_PLAN — HU136

| Capa | Prueba | Caso |
|---|---|---|
| DB | T-136-01 y T-136-02 (`test_consultas_transversales_hu112_hu157.sql`) | El administrador ve el docente de su institución y solo de ella; el filtro por facultad; un decano ajeno no ve nada. Por defecto no aparecen docentes sin vínculo; `SIN_VINCULO` trae al retirado con programa vacío; `TODOS` trae ambos; el decano nunca ve docentes sin vínculo |
| Backend | `ConsultasTransversalesJpaRepositoryTest` | `@idFacultad` enlazado (administrador) y nulo (decano) |
| Backend | `ConsultasTransversalesUseCaseImplTest`, `…InteractorTest` | Normalización del filtro, facultad, `estadoVinculo` (mayúsculas, vacío = nulo, valor inválido = 400) y mapeo de `tieneVinculoActivo` |
| Backend | `AdminConsultasTransversalesControllerTest` | `GET /admin/docentes` con filtros |
| Backend | `AdminPortalControllerTest` | Ya no lista las mutaciones de instituciones como 501 |
| Backend | `AccessCatalogConsistencyTest` | Ruta nueva y 3 retiradas |
| Frontend | `admin-docentes.component.spec.ts`, `consultas-transversales.service.spec.ts` | Lista, filtros, paginación y errores |
| Frontend | `admin-catalogs.component.spec.ts` | Sin acciones de crear, editar ni activar instituciones |

Validación manual: MV-017 del ledger.
