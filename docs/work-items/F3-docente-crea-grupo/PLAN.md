---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# PLAN — F3: el docente crea sus propios grupos (backend)

## Decisión

John (2026-11-01): el docente crea sus grupos para no cargar todo al coordinador. Alcance: el docente **crea** el grupo a su nombre y define su **horario inicial**; los cambios posteriores del docente irán por la aprobación del coordinador (F6) y el coordinador edita directo. La BD lo hace cumplir (rama `feature/F3-docente-crea-grupo` del repo de BD: `usp_crear_grupo` y `usp_guardar_horarios_grupo` aceptan DOCENTE; `HOR_005`).

## Cambios

1. **`SecurityConfig`:** `POST /api/v1/grupos` (solo la ruta de creación) admite `DOCENTE`, `COORDINADOR` y `ADMINISTRADOR`. `PUT` y `DELETE /api/v1/grupos/**` y el resto de `POST /api/v1/grupos/**` siguen solo para `COORDINADOR` y `ADMINISTRADOR`.
2. **`infra/security-baseline/access-catalog.json`:** la entrada `POST /api/v1/grupos` agrega `DOCENTE`; el PEP lee este catálogo.
3. **Errores:** `SEC_002` (grupo a nombre de otro docente) ya respondía 403; `DbFailureClassifier` traduce `HOR_005` (el docente quiere reemplazar un horario ya definido) a `CONFLICT` (409).
4. **Endpoint nuevo de lectura `GET /api/v1/docente/oferta-creacion-grupos`** (`DocenteOfertaCreacionGruposController`, solo DOCENTE): devuelve `{idDocente, asignaturas[{idAsignatura, codigoAsignatura, nombreAsignatura, idPrograma, nombrePrograma}]}` leído de `usp_consultar_oferta_creacion_grupo_docente`. El `idDocente` sale del token (`InstitutionalScopePort`) y la lista, de los programas con vínculo activo. Arquitectura: caso de uso `consultarofertacreaciongrupos`, puerto `OfertaCreacionGruposDocenteQueryPort` y adaptador `OfertaCreacionGruposDocenteJpaRepository`.
5. **Sin cambios del contrato de crear grupo:** mismo cuerpo de F1 (`franjas`). El docente envía su propio `idDocente` y no manda período (la DB toma el más reciente).

## Pruebas

- `RbacSecurityFilterChainTest`: `POST /api/v1/grupos` con DOCENTE pasa el filtro; `PUT` y `DELETE` con DOCENTE siguen en 403.
- `AccessCatalogConsistencyTest` verifica que `SecurityConfig` y el catálogo coincidan.
- `DocenteOfertaCreacionGruposControllerTest`, `ConsultarOfertaCreacionGruposDocenteUseCaseImplTest` y `OfertaCreacionGruposDocenteJpaRepositoryTest` cubren el endpoint nuevo; `JpaRepositoryBeanUniquenessTest` pasa a 52 repositorios.

## Validación

Ver `VALIDATION.md`.
