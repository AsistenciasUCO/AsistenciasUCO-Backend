---
status: PREPARED_BACKEND_NOT_RELEASED
target: AsistenciasUCO/AsistenciasUCO-Frontend
work-item: MAINT-003D
---
# Handoff frontend: visualización por zona sin alterar instantes

Se inspeccionaron `src/app/core/services/session.service.ts`, `src/app/core/api/models/sesion-consultada-api-dto.model.ts`, `teacher-sesion-form.component.ts`, `teacher-grupo-sesiones.component.ts`, `group-sessions-overview.component.ts` en `develop`.

**AS-IS:** `getSessionsByGroup` llama `/sesiones/grupo/{grupoId}` v1 y `splitLocalDateTime` devuelve fecha/hora sin conversión; `createSession/updateSession` envían `date + 'T' + time + ':00'` sin offset. `ClassSession` usa `date/startTime/endTime` como strings. Cambiar la zona del dispositivo **no** puede recomputar estas strings sin un instante canónico. No cambiar v1 anticipadamente.

**Objetivo tras backend READY:**
1. Modelo v2 debe conservar `fechaHoraInicioUtc/FinUtc` como strings `Z` sin parsearlas para luego reserializarlas a servidor. `estadoTemporal=INDETERMINADA` con horas NULL deshabilita presentación temporal y edición hasta clasificación.
2. Zona efectiva `zoneId` IANA: selección explícita de usuario (si aplica) o `Intl.DateTimeFormat().resolvedOptions().timeZone` al cargar. Persistir elección explícita de forma consistente con el diseño de preferencias del producto; en recarga recalcular TODOS los horarios desde instantes UTC, nunca sobre strings de UI transformadas. Fallback `America/Bogota` **visible** si timezone no disponible.
3. Presentar `Intl.DateTimeFormat('es-CO',{timeZone: zoneId,...})` o biblioteca temporal validada. La fecha puede cambiar de día. Mostrar identificador de zona cuando relevante.
4. Formulario: fecha + inicio/fin local en la zona seleccionada → resolver reglas IANA para esa fecha → rechazar gap, pedir elección de offset en overlap → generar RFC3339 con offset real del día. **No** agregar `Z` a wall-clock ni aplicar `new Date('2026-...')` a naive v1. Comparar instantes, no etiquetas.
5. Consultas v2: `GET /api/v2/sesiones/grupo/{grupoId}`, `GET /api/v2/sesiones/{id}`; POST/PATCH v2; un adaptador v2 separado del actual. Evitar mezcla de formatos v1/v2. Mantener id, nombre, numero y envelopes.
6. Superficies: tarjetas docentes, lista grupo/asistencia, detalles/edición, modal nueva sesión, selector sesión activa (basado en instante), pantalla estudiante y reporte/Excel **solo cuando se versionen**; las dos últimas permanecen v1 sin cambios en esta entrega.
7. Casos DST y zonas: Bogotá 2026-07-15 20:00 equivale Berlín 2026-07-16 03:00 y Londres 02:00. Enero Berlín +01. UI gap `2026-03-29T02:30 Europe/Berlin` inválido. UI overlap `2026-10-25T02:30` exige elegir +02/+01.
8. No configurar horario recurrente `HorarioDocente` como instante: `dia`+`LocalTime` es regla de calendario institucional, no `Instant`.
9. Preservar pruebas de no-v2 aún: no activar frontend hasta publication gate BACKEND_READY_FOR_FRONTEND. Confirmar endpoints, roles, schema, marca DB y SQL IT antes de modificar el servicio de producción.

**Tests front exigidos**: `session.service.utc-v2.spec.ts` (request offset real, GET UTC y zonas), `session-timezone.util.spec.ts` (gap, overlap, precisión, recarga), render docente y vista attendance, zona preferida vs zona detectada, sesión cruzando medianoche, estado INDETERMINADA. No marcar verdes tests que todavía no existen.

**Límite**: esta rama escribe documentación en backend; no modifica ni sincroniza frontend automáticamente.
