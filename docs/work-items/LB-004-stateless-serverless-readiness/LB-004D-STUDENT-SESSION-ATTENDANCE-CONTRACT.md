---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-20
---

# LB-004D — Asistencia real en `GET /api/v1/estudiante/materias/{materiaId}/sesiones`

> **Cambio de contrato (aditivo) pendiente de revisión de los owners DB/Backend/Frontend antes de integrarse a `develop`.**

## Motivo

Angular no podía habilitar «Radicar Reclamo» porque el endpoint solo devolvía la sesión (`uv_sesion`) y no el estado de
asistencia del estudiante. El frontend, correctamente, trataba la ausencia del dato como «Estado no disponible».

## Contrato

`SesionMateriaEstudianteDTO` conserva todos sus campos y agrega tres, todos calculados en el servidor para el estudiante
autenticado (el `idEstudiante` se resuelve con `InstitutionalScopePort.findEstudianteIdByUsuario`; el cliente no lo envía):

| Campo | Tipo | Significado |
|---|---|---|
| `estadoAsistencia` | `PRESENTE` \| `AUSENTE` \| `JUSTIFICADA` \| `NO_DISPONIBLE` | Nunca nulo. |
| `estadoSolicitudRevision` | `string` \| `null` | `uv_estado.nombre` de la solicitud más reciente (`pendiente`, `aceptada`, `rechazada`) o `DESCONOCIDO` si la fila no trae nombre. `null` = sin solicitud. |
| `reclamable` | `boolean` | Pista de presentación: `estadoAsistencia = AUSENTE` **y** sin ninguna solicitud previa. |

Las fechas siguen siendo `LocalDateTime` de `uv_sesion` (semántica UTC v2 sin cambios).

## Significado comprobado de los datos (DB local `gestionasistenciadb`)

Verificado en `usp_registrar_asistencias_sesion` / `usp_sincronizar_asistencia_estudiante_interno` y sobre las 6 350 filas vivas de `uv_detalle_asistencia`:

| `asistio` | `codigoRazonCausa` | Filas vivas | Estado expuesto |
|---|---|---|---|
| 1 | `AN` (asistencia normal) | 4 944 | `PRESENTE` |
| 0 | `SJC` (sin justa causa) | 832 | `AUSENTE` |
| 0 | `EX` (excusa) | 574 | `JUSTIFICADA` |
| cualquier otra combinación | | 0 | `NO_DISPONIBLE` (no se interpreta) |
| sin cabecera `uv_asistencia` | | | `NO_DISPONIBLE` |
| cabecera sin `uv_detalle_asistencia` | | | `NO_DISPONIBLE` |

* `CPVP` / `CPI` existen en `uv_razon_causa` pero ningún detalle de asistencia los usa (son razones de cancelación de matrícula).
* **Retardo no existe en el contrato de la DB**: el catálogo de entrada es `AN`/`SJC`/`EX`. No se infiere ni se expone; si la DB lo incorpora, el enum se extiende de forma aditiva.
* **Ausencia de fila ≠ ausencia**: nunca se usa `COALESCE(asistio, 0)`.
* Si hubiera varios detalles por asistencia prevalece el de mayor `codigo` (última escritura).

## Diseño

* Puerto nuevo `AsistenciaSesionEstudianteQueryPort` (solo lectura) con adaptador `AsistenciaSesionEstudianteJpaRepository` (JPA/HQL, sin JDBC).
  El puerto/adaptador de sesiones existente (`SesionMateriaEstudianteQueryPort`) **no cambia**: su paridad JDBC↔JPA sigue intacta.
* HQL: `uv_asistencia ⋈ uv_estudiante_grupo ⋈ uv_grupo` con `left join` a `uv_detalle_asistencia` y a `uv_solicitud_revision_asistencia`,
  filtrado por `eg.idEstudiante` (servidor) y `g.idAsignatura`. Solo se leen filas de la matrícula del estudiante autenticado.
* La derivación del estado vive en el caso de uso (`ConsultarSesionesMateriaEstudianteUseCaseImpl`), no en el adaptador.
* Entidades de solo lectura: se agregó `UvSolicitudRevisionAsistenciaEntity` y la columna `codigo` a `UvDetalleAsistenciaEntity`.
* Sin JDBC productivo, sin SP nuevos, sin cambios de seguridad ni de permisos. Endpoints existentes sin cambios.

## Elegibilidad del reclamo — lo que el backend/DB hacen y NO hacen hoy

`reclamable` es solo una pista. Estado real de las garantías al radicar (`usp_radicar_solicitud_revision_asistencia_v2`):

| Regla de negocio | ¿La aplica la DB al radicar? |
|---|---|
| El estudiante pertenece al ejecutor autenticado (H-01) | Sí |
| El estudiante está matriculado en el grupo de la sesión | Sí |
| Soporte propio, `DRAFT` y no reutilizado | Sí |
| La asistencia es una ausencia (AUSENTE) | **No** — el SP no consulta `DetalleAsistencia` |
| No existe una solicitud previa para la misma asistencia | **No** — `SolicitudRevisionAsistencia` solo tiene la PK por `id` |
| La sesión ya ocurrió / ventana de reclamo | **No** |
| Estado de la matrícula (activa/cancelada) | **No** (el SP solo exige pertenencia al grupo) |

Además, el SP **crea la cabecera `Asistencia` si no existe** (paso 10), y el cleanup de `support_binding_e2e.py` borra la solicitud y el soporte
pero **no** esa cabecera.

### Solicitudes para los owners (no implementadas aquí)

1. **DB**: que `usp_radicar_solicitud_revision_asistencia_v2` rechace (código de negocio) si la asistencia no es `SJC` y si ya existe una solicitud
   pendiente/aceptada para la misma `Asistencia`; idealmente con un índice único filtrado.
2. **DB**: decidir si una solicitud *rechazada* permite una nueva. Hoy la UI conservadoramente **no** ofrece un segundo reclamo si existe cualquier solicitud.
3. **DB**: que el SP no cree cabeceras de asistencia huérfanas para sesiones sin registro (o que el E2E las limpie).
4. **Producto**: definir ventana temporal de reclamo y si las ausencias `EX` (excusa) pueden disputarse.
5. **DB**: si se desea `RETARDO`, agregarlo al catálogo `RazonCausa` y a `usp_registrar_asistencias_sesion` (hoy solo `AN`/`SJC`/`EX`).

## Pruebas

* `ConsultarSesionesMateriaEstudianteUseCaseImplTest` (15): estudiante no resoluble, sin registro, cabecera sin detalle, presente, ausente,
  justificada, combinaciones fuera de contrato (incl. «RETARDO» no inventado), varios detalles, solicitud vigente/múltiples/sin nombre,
  asignatura con varios grupos, hechos de sesiones ajenas, instantes preservados.
* `EstudiantePortalControllerTest`: JSON del DTO con los nuevos campos.
* `JpaQueryAdapterContractTest`: parámetros enlazados (`idEstudiante`, `idAsignatura`), nulos conservados, fallo de provider → `DatabaseOperationException`.
* `JpaRepositoryArchitectureRulesTest`: el nuevo adaptador implementa su puerto directamente y no usa JDBC.
* `AsistenciaSesionEstudianteJpaParityIT` (SQL Server real, solo lectura, `-Pintegration`): la consulta JPA coincide con un oráculo JDBC escrito en el test,
  una cabecera sin detalle llega con nulos y un estudiante inexistente no recibe filas.
