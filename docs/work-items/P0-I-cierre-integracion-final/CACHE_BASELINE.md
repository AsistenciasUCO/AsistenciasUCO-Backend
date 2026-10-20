# Línea base previa a caché (sin implementar caché)

Estado: **plan de medición**. Este documento no contiene mediciones: ninguna se ejecutó en P0-I. No hay Redis, `@Cacheable` ni cachés distribuidas.

## Candidatos a medir (lectura, alto uso)
Grupos y horarios del docente; sesiones por grupo (paginadas); directorios y catálogos (parámetros/mensajes); materias y sesiones del estudiante; reclamos del docente (`/docente/reclamos`); estados de reclamo; asistencias por grupo.

## Qué medir antes de decidir
p50/p95/p99 por endpoint (Prometheus/Tempo ya desplegados), número de sentencias SQL por request (detectar N+1 en `Uv*` entities y repositorios JPA), tamaño de respuesta, tasa de lectura/escritura por recurso.

## No cachear
Tokens/claims, resultados de autorización/ownership por usuario, descargas de soportes, estados de reclamo y asistencia del estudiante autenticado sin clave por identidad, cualquier respuesta con datos personales entre roles.

## Riesgos
Claves sin `idUsuario`/rol/organización mezclarían estudiante↔docente↔coordinación; el catálogo de parámetros ya tiene clave propia (MAINT-002) — reutilizar su patrón.

## Invalidación mínima a diseñar
Registrar asistencias (lote/autónoma) → asistencias del grupo y estado del estudiante; radicar/resolver reclamo → reclamos del docente y sesiones del estudiante; crear/actualizar sesión → sesiones del grupo; cambios de matrícula → grupos/materias del estudiante.
