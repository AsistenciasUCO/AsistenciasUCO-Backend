---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# PLAN — CAT-01: catálogos base de solo consulta

## Regla (John, 2026-11-04)

Perfil, TipoIdentificacion, TipoPrograma, Semestre, Componente, Area, EstadoEstudianteGrupo, RazonCausa, Estado y Dia son catálogos de **solo consulta**: se cargan por seed y la app no los crea, edita, cambia de estado ni borra. En el HU-CATALOG, HU153, HU154 y HU155 se reducen a consulta, y HU151 y HU152 quedan DESCARTADAS.

## Alcance

| Capa | Cambio |
|---|---|
| DB | Se eliminan 14 SP de escritura (tipos de documento, tipos de programa y áreas) con `migrations/11_catalogos_solo_consulta_up.sql`, junto con sus permisos y pruebas. Detalle en el work item homónimo de la BD |
| Backend | **Tipos de documento:** se quitan `POST`/`PUT /api/v1/admin/tipos-identificacion` (`AdminTipoIdentificacionController`), la feature `guardartipoidentificacion`, `TipoIdentificacionCommandPort` y `TipoIdentificacionCommandJpaRepository`. **Tipos de programa:** se quitan `POST`, `PUT` y `PATCH /estado` de `/api/v1/admin/tipos-programa` (queda el `GET`), las features `creartipoprograma`, `actualizartipoprograma` y `cambiarestadotipoprograma`, y `TipoProgramaCommandPort`; `TipoProgramaJpaRepository` queda solo de lectura. **Áreas:** se quitan `POST`, `PUT` y `PATCH /estado` de `/api/v1/admin/areas` (`AdminAreaController`; el `GET` sigue en `AdminPortalController`), las features `creararea`, `actualizararea` y `cambiarestadoarea`, `AreaCommandPort` y `AreaWiringConfiguration`; `AreaJpaRepository` queda solo de lectura. **También:** 8 rutas menos en `access-catalog.json`, `JpaRepositoryBeanUniquenessTest` 54 → 53 y `DbFailureClassifier` sin la operación de tipos de documento |
| Frontend | Se quita la sección de tipos de documento (HU153) y los métodos de escritura de `CatalogService`. `AdminAreasComponent` y `AdminCatalogsComponent` quedan como tablas de consulta sobre el componente compartido `CatalogoSoloConsultaComponent`, y `AdminManagementService` queda sin `crear`, `actualizar` ni `cambiarEstado` de áreas y tipos de programa. Se quitan las simulaciones de escritura de áreas. El menú dice "Tipos de Programa" |

**Se conservan:**
- `GET /api/v1/tipos-identificacion`, `GET /api/v1/admin/tipos-programa`, `GET /api/v1/tipos-programa` y `GET /api/v1/admin/areas`. Los usan el registro de usuarios y los formularios de programa y asignatura.
- La columna `estado` de áreas y tipos de programa, fijada por el seed.

## Dueños

- **HU153:** John (paquete HU061-HU172 del 2026-11-04).
- **HU151, HU152 y HU154:** Sergio. John decidió que lo corrijamos nosotros, y el aviso a Sergio va en el mensaje consolidado del equipo.

## Rollback

Revertir los commits de las 3 capas y ejecutar `migrations/11_catalogos_solo_consulta_down.sql`.
