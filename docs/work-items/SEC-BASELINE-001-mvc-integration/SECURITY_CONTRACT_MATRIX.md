# Matriz de contrato de seguridad

| Caso | Precondición | Resultado esperado | Evidencia | Estado |
|---|---|---|---|---|
| Sin Bearer | Ruta protegida, filtro experimental ON | `401` del resource server | `SecurityConfig`, tests existentes | AS-IS |
| JWT inválido | issuer/audience/claim no válidos | `401` | `KeycloakSecurityAdapterConfiguration` | AS-IS |
| JWT válido + PEP `2xx` | PEP permite | Continúa a autorización/handler | contrato PEP embedded v0.1.6 | TARGET |
| JWT válido + PEP `401` | PEP rechaza token | `401` | starter oficial | TARGET |
| JWT válido + PEP `403` | PEP deniega | `403` | starter oficial | TARGET |
| JWT válido + PEP `5xx`/timeout | PEP no disponible | `503`, nunca bypass | starter oficial + D-004 | TARGET |
| PEP responde estado no contractual o falla transporte | Estado distinto de `2xx/401/403`, timeout o excepción | `503`, nunca bypass | D-004 | TARGET |
| PEP permite, rol local insuficiente | JWT válido pero falta authority | `403` de Spring | reglas actuales de `SecurityConfig` | TARGET |
| PEP permite, ownership falla | JWT/rol válidos pero recurso ajeno | decisión de Application actual | puertos de ownership existentes | TARGET |
| Flag OFF | configuración por defecto | comportamiento AS-IS sin llamada PEP | `application.yml` + test | TARGET |

No se certifica un código de estado del proveedor real sin ejecutar una integración contra el proveedor; mocks y servidor local solo prueban el adaptador y el mapeo.
