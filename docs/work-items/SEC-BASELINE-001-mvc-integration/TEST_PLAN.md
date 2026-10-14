# Plan de pruebas

## RED

- Cliente PEP: serializa exactamente el request raw, usa el endpoint y headers contractuales, no reenvía `Cookie`, y clasifica `2xx`, `401`, `403`, `5xx`, timeout y errores de transporte.
- Filtro MVC: no llama al PEP con flag apagado/ruta no protegida/sin autenticación; permite continuar solo con respuesta PEP permitida; mapea indisponibilidad a `503`; conserva `X-Decision-Id` cuando el PEP lo entrega.
- Wiring: el bean experimental solo existe con flag ON; `SecurityConfig` conserva el filtro JWT y las reglas actuales.
- Higiene: no se escriben bearer tokens, cookies, credenciales ni cuerpos sensibles en logs.

## GREEN

Implementar lo mínimo en Infrastructure y Composition Root hasta que las pruebas focales anteriores pasen. No cambiar tests RED para acomodar la implementación.

## VALIDATE

- `.\mvnw.cmd -B -ntp -Dtest=... test` para el foco.
- `.\mvnw.cmd -B -ntp verify` para la rama, registrando cualquier interrupción o skip.
- `git diff --check`, búsqueda de secretos y revisión de que no aparezca WebFlux nuevo.
- Docker solo con configuración no destructiva; sin `down -v`.
