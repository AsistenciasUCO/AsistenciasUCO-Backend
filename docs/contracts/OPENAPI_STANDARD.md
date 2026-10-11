---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-24
---

# Estándar Contract First / OpenAPI

Reglas de gobierno acordadas en el paquete y ratificadas en la consolidación de gobernanza. La
ubicación canónica es [docs/contracts/openapi](openapi/); el Golden Path se especifica en
[openapi-golden-path.yaml](openapi/openapi-golden-path.yaml) y se verifica durante `mvn verify`.
La carpeta histórica `contracts/openapi/` solo redirige a esta fuente.

## Proceso

Toda API nueva o modificada debe definirse en contrato aprobado antes de implementar. Inspeccionar controller, request/response, errores, seguridad, tests, consumidor y contrato DB real. Separar AS-IS/TARGET. Un archivo generado desde annotations puede ayudar, pero no sustituye aprobación/versionado. `CONTRACT_CONFLICT` o evidencia relevante ausente impiden formalizar por suposición.

## Convenciones acordadas

| Tema | Regla y límite |
|---|---|
| Versionamiento | Preservar `/api/v1` observado. LB-001C.1 seleccionó OpenAPI 3.1.2; breaking changes requieren decisión/migración y no crean v2 automáticamente |
| Naming | JSON camelCase y recursos con sustantivos/kebab-case cuando proceda; preservar nombres públicos existentes. No trasladar sufijos técnicos DTO/Entity a schemas nuevos |
| operationId | Verbo + recurso, estable y único; no hay operationIds AS-IS aprobados todavía |
| Schemas | Reutilizables, tipos/requeridos/nullability/constraints/ejemplos fieles al comportamiento acordado; sin secretos ni modelos JPA expuestos |
| Métodos | Aplicar [API_DESIGN_RULES](../governance/API_DESIGN_RULES.md): recursos pragmáticos + business commands; PUT/DELETE nuevos requieren `METHOD_EXCEPTION`; inventariar legado antes de migrar |
| Errores | Estados/códigos solo donde una condición real los respalde; no añadir 409/422 por convención genérica. [ApiErrorResponse](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/error/ApiErrorResponse.java) contiene timestamp/status/error/code/message/path/correlationId/details. Revisar también writer de seguridad |
| Correlation ID | `X-Correlation-Id` según filtro real, presente en respuestas/errores cuando corresponda; no inventar otro identificador ni trasladarlo como API técnica de Application |
| Bearer auth | Documentar JWT Bearer, roles y alcance institucional reales; distinguir 401 de 403. Tokens no van en URLs |
| Tiempo | Para instantes globales nuevos/objetivo, ISO-8601 con UTC u offset explícito. El Golden Path congela explícitamente el wire legacy de sesión ISO local sin offset y su semántica UTC persistida; migrarlo exige fase compatible; [TD-005](../baseline/TECHNICAL_DEBT.md#td-005) |
| Filtros | Query params tipados y whitelist; no nombres de columnas SQL del cliente |
| Paginación | Decisión explícita para listados que crecen. El Golden Path actual devuelve lista sin page/size; no afirmar ni implementar límites nuevos sin aprobar contrato. Excepción publicada por operación: `GET /api/v1/estudiantes` (MAINT-001, AS-IS `page>=0` default 0, `size` 1..100 default 20) |
| Sort | Campos/direcciones permitidos y comportamiento por defecto explícitos cuando se apruebe; no prometer orden estable que el SQL actual no garantiza |

La propuesta del paquete `page >= 0`, `size 1..100` era una recomendación pendiente: **no es una convención aprobada ni el AS-IS universal**. LB-001B/C debe decidir por operación y comprobar consumidores; no aplicar esos números automáticamente.

## Documentación runtime

La documentación interactiva, cuando está habilitada, sirve el mismo archivo canónico sin
generarlo desde el código:

- UI: `/swagger-ui/` o `/swagger-ui/index.html`.
- contrato crudo: `/openapi/openapi-golden-path.yaml`.
- fuente única: `docs/contracts/openapi/openapi-golden-path.yaml`, copiada sin filtrado durante
  `process-resources`.
- activación: `asistencias.swagger-ui.enabled`; el valor por defecto es `false` y los perfiles
  `local` y `dev` lo habilitan. Producción debe conservar el valor deshabilitado salvo decisión
  operativa explícita.
- seguridad: únicamente los GET de esos recursos son públicos. `/api/v1/**` conserva JWT/RBAC.
- assets: empaquetados localmente; el navegador no depende de CDN ni de `/v3/api-docs`.

La configuración del UI fija `persistAuthorization: false`, `deepLinking: true`,
`displayOperationId: true`, `tryItOutEnabled: true` y `docExpansion: 'list'`. Swagger UI es una
vista del contrato; no adquiere autoridad sobre él.

## Compatibilidad

Eliminar/renombrar campos, cambiar tipos, requeridos, valores válidos, status/semántica o formato temporal puede ser breaking. Exige decisión explícita de versión/migración, consumidores y pruebas. Un cambio de schema DB no autoriza un cambio HTTP. Mantener queries legacy hasta decisión de retiro con consumidores comprobados.

Los [envoltorios de éxito](../architecture/http-success-responses.md), [validación de entrada](../architecture/input-validation.md) y [seguridad](../security/runtime-security-provider-architecture.md) siguen siendo las referencias detalladas. Primera vertical: [Golden Path](../baseline/GOLDEN_PATH_ASISTENCIA.md) y su [evento](REALTIME_EVENT_STANDARD.md).
