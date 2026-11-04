---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# PLAN — HU061 / HU147 / HU153 / HU172 y P5: consolidado, auditoría y catálogos

## 1. Identidad y objetivo

- **Historias de usuario** (títulos del HU-CATALOG de Infra):
  - **HU061**: Consolidado de asistencia por cohorte, materia y docente.
  - **HU172**: Consulta de la auditoría de cambios de asistencia.
  - **HU147**: Consultar componentes de formación.
  - **HU153**: Administrar el catálogo de tipos de documento.
  - **P5 (defecto encontrado en el análisis de negocio)**: crear o editar una asignatura asignaba un área, un componente o un semestre "cualquiera".
- **Work Item ID:** `HU061-HU147-HU153-HU172-consolidado-auditoria-catalogos`
- **Fecha:** 2026-11-04
- **Base Git:** rama `feature/HU061-HU172-consolidado-asistencia-auditoria` sobre `develop` en BD, backend y frontend.
- **AS-IS comprobado:** las 4 HU estaban `[PENDIENTE]`. `GET /api/v1/admin/auditoria` era un stub que respondía `FEATURE_UNAVAILABLE`. El formulario de asignaturas enviaba `area`, `componente` y `semestre`, pero el backend espera `nombreArea`, `nombreComponente` y `semestreNumero`: llegaban nulos y `usp_crear_asignatura` tomaba un `TOP 1` arbitrario de área, componente y semestre, y ponía 3 créditos si no llegaban.

### Criterios de aceptación

1. **HU061:** el coordinador ve una fila por grupo de su programa (el decano, de su facultad; el administrador, de su institución) con estudiantes activos, sesiones ejecutadas, asistencias, faltas, excusas, % de asistencia e inasistencia, estudiantes en riesgo y en pérdida. Filtros: cohorte (período académico), programa, materia y docente. Paginado.
2. **HU172:** solo el administrador consulta el rastro de `AuditoriaEvento` con el nombre del usuario. Por defecto: últimos 7 días y solo acciones de asistencia; rango máximo de 90 días; filtros por acción, recurso, usuario y resultado. Paginado. Sin valores antes y después.
3. **HU147:** se consultan los componentes de formación reales (BAS, INS, ESP) y el formulario de asignaturas solo ofrece esos.
4. **HU153:** el administrador crea tipos de documento (código de 1 a 5 letras, único e inmutable; nombre de 3 a 50 caracteres, único) y edita su nombre. No se inactivan.
5. **P5 y reglas de asignatura:** área y componente deben existir; créditos obligatorios y mayores que cero; el semestre debe existir en el plan; el coordinador solo crea y edita asignaturas de planes de su programa.

## 2. Alcance

| Capa | Cambio |
|---|---|
| DB | SP `usp_consultar_consolidado_asistencia_grupos_paginado`, `usp_consultar_auditoria_eventos_paginado`, `usp_consultar_componentes_formacion`, `usp_guardar_tipo_identificacion` (+ `_interno`), `usp_resolver_area_componente_asignatura_interno`; vista `uv_auditoria_evento`; `uv_componente` con `codigo`; reglas nuevas en `usp_crear_asignatura` y `usp_actualizar_asignatura`; mensaje `ASI_001`; permisos en `rol_asistencias_runtime_hu061_hu172.sql`. Detalle en el work item homónimo de la BD. |
| Backend | `ConsultasTransversalesQueryPort`/`JpaRepository`/caso de uso/interactor extendidos con consolidado, auditoría y componentes; controlador nuevo `ConsolidadoAsistenciaController`; `GET /api/v1/admin/auditoria` en `AdminConsultasTransversalesController` (se quita el stub de `AdminPortalController`); feature nueva `tipoidentificacion/guardartipoidentificacion` con `TipoIdentificacionCommandPort` + `TipoIdentificacionCommandJpaRepository` y `AdminTipoIdentificacionController`; `DbFailureClassifier` clasifica `ASI_001` (400) y `GEN_001` al guardar asignaturas o tipos de documento (404). Rutas nuevas en `access-catalog.json`. `SecurityConfig` sin cambios. |
| Frontend | Pantalla `consolidado-asistencia` para coordinador y decano; pestaña "Trazabilidad y Auditoría" de `admin-system` conectada al endpoint real (`admin-auditoria`); sección "Tipos de Documento" en `admin-catalogs`; formulario de asignaturas con componente y área reales, créditos obligatorios y semestre del plan, y mapeo del cuerpo al contrato del backend. Mocks y menú. |

**Fuera de alcance:** HU148 queda **DESCARTADA** (los componentes son fijos BAS/INS/ESP por regla de negocio; decisión de John, 2026-11-04) y HU006 sigue solo como consulta.

## 3. Decisiones (John, 2026-11-04)

- **D1:** la cohorte de HU061 es el período académico del grupo (el modelo no tiene cohorte de ingreso).
- **D2:** HU172 solo para ADMINISTRADOR, solo lectura, paginada, rango obligatorio de máximo 90 días (por defecto 7), por defecto solo acciones de asistencia, con el nombre del usuario, en la pestaña existente.
- **D3:** HU153 solo crea y edita el nombre; sin columna de estado ni DDL.
- **D4:** no se asigna un área o componente "cualquiera": se asigna uno de la tabla o se rechaza.
- **D5:** créditos obligatorios y mayores que cero, sin valor por defecto; el semestre inexistente en el plan se rechaza; el coordinador solo gestiona asignaturas de planes de su programa.
- **D6:** las asignaturas son BAS, INS o ESP y un estudiante entra a un programa solo por una ESP; la oferta y la matrícula no cambian.
- **D7 (técnica):** las fechas de auditoría viajan como `OffsetDateTime` ISO-8601; el caso de uso las normaliza a UTC y aplica el rango por defecto y el máximo con un `Clock` inyectable.
- **D8 (técnica):** el consolidado del coordinador y del decano se sirven desde un único método con dos rutas, para no duplicar código.

## 4. Rollback

Revertir los commits del work item en las tres capas. Sin cambios de tablas ni datos; los permisos nuevos se revocan con `REVOKE`.
