# PR — SES-01 (Backend): 409 fuera de la ventana de la sesión activa

**Base:** `develop` · **Rama:** `fix/ses01-cierre-estricto-asistencia` · **Orden de integración:** 2/3 (después del PR de DB)

## Alcance funcional
`POST /api/v1/asistencias/lote` solo registra con la sesión activa, `inicio <= ahora < fin`, con el reloj UTC del servidor y procedencia temporal confirmada. Sin tolerancia posterior al cierre. API directa, F5 y reloj del navegador no lo evaden.

## Causa corregida
El caso de uso resolvía la sesión por v1 (sin procedencia) y persistía sin comparar el instante actual.

## Contrato (aditivo)
- Nuevo `409` (`SesionFueraDeVentana`) con `ERR_SESION_NO_INICIADA`, `ERR_SESION_CERRADA` o `ERR_SESION_HORARIO_INDETERMINADO`. Se regeneró el sidecar `openapi-golden-path.sha256`.
- Orden: 404 (sesión inexistente) → 403 (docente no titular) → 409 (fuera de ventana) → persistencia → evento realtime. Un rechazo no persiste ni publica evento.
- DB `SES_007/008/009` (lote y autónomo) se clasifican como el mismo 409.

## Pruebas
- RED `be5c9fdd` (compilación) → GREEN: 174 focalizadas; contrato HTTP y OpenAPI 17/17.
- `./mvnw.cmd verify`: **3082/3082**, ArchUnit, JaCoCo OK.
- Matriz con reloj fijo: 12:59:59Z, 13:00Z, 14:59:59.999999999Z, 15:00Z, +1 ns, 17:00Z, día siguiente; zonas Londres/Berlín/Bogotá/Tokio; procedencias NULL, desconocidas y confirmadas.
- **NOT_RUN:** `-Pintegration` (escribe en la base local; pendiente de la DB aprobada), HTTP real y navegador (después del merge).

## Dependencias
Requiere el PR de DB para la garantía transaccional en el instante de cierre; el backend rechaza por sí mismo aunque la DB aún no esté desplegada. Frontend: `shesho22/AsistenciasUCO-Frontend#?`.

## Reversión
Revertir el PR. El 409 es aditivo.
