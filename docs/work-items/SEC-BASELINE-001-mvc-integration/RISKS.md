# Riesgos y controles

| Riesgo | Control | Estado |
|---|---|---|
| Bypass si PEP no responde | Fail-closed a `503`; flag OFF por defecto | controlado en diseño |
| Cambio accidental de JWT/roles | Filtro adicional después de Bearer; sin cambios a decoder/converter | controlado |
| Confundir `sub` con actor institucional | No mapear sin evidencia; E2E bloqueado | abierto |
| CSRF por introducir cookies | No se reenvía Cookie; no se activa BFF/cookie | controlado |
| Exposición de secretos | placeholders, revisión de diff y logs | controlado; finding externo reportado |
| Incompatibilidad WebFlux/MVC | cliente servlet JDK; sin dependencia WebFlux | controlado |
| Estado PEP no contractual o transporte caído | estados no permitidos/timeout/excepción → `503` | controlado |
| Contrato documental incompleto | manuales PDF ausentes registrados | abierto |
| Laboratorio comparte estado con estable | proyecto/volúmenes/loopback separados | controlado |
