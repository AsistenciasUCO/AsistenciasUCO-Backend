---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# PLAN — HU058 / HU155 / HU167: consumo y consulta de catálogos base

## Identidad y objetivo

- **HU058** (módulo docente): consumo transversal de tipos de programa, con filtros por nivel formativo en los cursos.
- **HU155 y HU167** (mismo texto en el plan de la BD: "Ver los diferentes tipos de estados que se usan"): consultar los estados del sistema.
- **Work Item ID:** `HU058-HU155-catalogos-consulta`. Rama `feature/HU058-HU155-catalogos-consulta` en BD, backend y frontend.
- **Regla vigente (John, 2026-11-04):** los catálogos base son de solo consulta (CAT-01).

## Decisiones de John (2026-11-04)

- **HU058, opción A:** los cursos del docente traen el tipo de programa (nivel formativo) de cada grupo, y la lista de grupos tiene un filtro "Nivel formativo" con el catálogo real `GET /api/v1/tipos-programa` (pregrado y postgrado en el seed).
- **HU155 P1:** se muestran los estados de negocio: `Estado` (solicitudes: pendiente, aceptada, rechazada, en espera, retirada) y `EstadoEstudianteGrupo` (estudiante en el grupo). No se muestra `EstadoNotificacion`, que es técnico y parte del trabajo de notificaciones.
- **HU155 P2:** pantalla de admin "Estados del Sistema" (`/app/admin/estados`), solo de consulta. Cierra también HU167.

## Alcance

| Capa | Cambio |
|---|---|
| DB | `uv_estado` expone `codigo` (aditivo; la vista no está congelada). Prueba T-155-01 en `test_consolidado_auditoria_catalogos.sql`. Sin SP nuevos: la consulta es de catálogos sin ámbito ni escritura |
| Backend | **HU058:** `AsignaturaDocenteProjection`, el dominio y `AsignaturaDocenteDTO` suman `idTipoPrograma` y `nombreTipoPrograma`. `AsignaturaDocenteJpaRepository` los resuelve desde `uv_programa` en una segunda consulta por los programas de los grupos, sin cambiar `uv_docente`. **HU155:** `GET /api/v1/admin/estados` en `AdminConsultasTransversalesController` (rol ADMINISTRADOR por `/admin/**`), con lectura nativa de `uv_estado` y `uv_estado_estudiante_grupo`; ruta nueva en `access-catalog.json` |
| Frontend | **HU058:** `Course` y `AsignaturaDocenteApiDto` con el tipo de programa; `CatalogService.getTiposPrograma()`; filtro "Nivel formativo" y etiqueta del tipo en la tarjeta de `TeacherGruposListComponent`. El filtro no se muestra si el catálogo no carga. **HU155:** `AdminEstadosComponent` (`/app/admin/estados`, menú "Estados del Sistema"), con dos tablas sobre `CatalogoSoloConsultaComponent`, que gana las opciones `seccion` y `conEstado` |

## Rollback

Revertir los commits de las 3 capas. El cambio de `uv_estado` es aditivo.
