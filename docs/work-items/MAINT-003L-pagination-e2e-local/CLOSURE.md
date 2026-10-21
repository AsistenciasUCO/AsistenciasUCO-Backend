# MAINT-003L — Cierre

## Resultado

`PENDIENTE` para cierre funcional completo: backend, SQL Server, Keycloak y HTTP real están preparados y verificados; la visualización Angular no pudo ejecutarse porque el checkout disponible no es el frontend de AsistenciasUCO.

## Alcance entregado y decisiones

- Tres cuentas E2E independientes: `docente.paginacion300`, `docente.paginacion1000`, `docente.paginacion5000`.
- Cada cuenta conserva `sub` propio, claim `idUsuario` del docente sintético y únicamente client role `DOCENTE`.
- Se añadieron dos horarios reservados; el horario preexistente de 300 se reutilizó sin modificación.
- Se añadió backup/rollback, preparación idempotente, cleanup limitado y validación HTTP sanitizada.
- Se alinearon los scripts DEV DB para crear horarios deterministas y limpiar horarios de grupos sintéticos antes de borrar grupos.

## Deuda

Ninguna deuda técnica nueva. La validación visual pendiente es una limitación de ambiente, no un cambio autorizado en frontend.

## Validación manual

Ver `VALIDATION.md`: HTTP real PASS; Angular `NOT_RUN`; Maven `verify` global quedó `INTERRUPTED` en una prueba preexistente sin salida observable.

## ADR relacionados

No aplica: no cambió una decisión duradera de arquitectura, contrato o proveedor.

## Elementos pendientes y bloqueos

Repetir login y navegación en el frontend real de AsistenciasUCO, verificando que cada docente visualiza su grupo y la tabla de 300/1000/5000.

## Cambios fuera de alcance

No se tocaron `docente.prueba`, el realm bootstrap, el backend productivo, OpenAPI, estudiantes, matrículas, grupos, asistencias, sesiones, grupos reales, despliegue ni merge.

## Condición de parada

No iniciar otra fase ni declarar DONE hasta adjuntar evidencia visual del frontend correcto.
