# Backlog de correcciones de AsistenciasUCO por rol — 2026-11-02

Estado: propuesta funcional y técnica, **pendiente de implementación**. Este documento no da por implementadas reglas ni autoriza cambios de datos. Base: SEC-BASELINE-018/019, `PDP_ADMIN_API_AUTHORIZATION_FINDING.md` y revisión funcional de cinco roles. Mantener separadas las incidencias del proveedor Seguridad-UCO de las de AsistenciasUCO.

## Decisiones funcionales confirmadas — 2026-11-02

Estas decisiones fueron aclaradas por el responsable del proyecto y prevalecen sobre supuestos anteriores; continúan **pendientes de implementación y validación contractual**.

1. **Cierre estricto a la hora de fin:** una sesión institucional programada de 08:00 a 10:00 se considera cerrada a las 10:00 (hora local del centro académico, instant UTC persistido). A las 12:00 de ese mismo día, el docente no puede registrar, corregir ni sustituir asistencias de esa sesión desde la toma ordinaria, por UI **ni por API**. No existe una ventana adicional de dos horas por defecto. La lectura histórica sí continúa disponible según permisos.
2. **Única corrección posterior mediante reclamo/excusa validada:** una vez cerrada la sesión, el docente puede decidir sobre un reclamo o justificación recibida, conforme al flujo y roles autorizados; al **aprobar** debe actualizarse automáticamente la asistencia que corresponda, en una transacción auditable e idempotente; al **rechazar** no se altera la asistencia original. No permitir un segundo camino de edición ordinaria ni que una reapertura de UI evada la regla.
3. **Sesión extra requiere aprobación previa:** el docente **solicita** una sesión fuera de las predeterminadas; no la crea ni activa como sesión oficial por sí solo. Se registra una solicitud pendiente y se informa al coordinador **responsable del programa del grupo**, mediante bandeja/notificación y correo institucional como canal de aviso a validar. El correo no sustituye el registro transaccional ni la autorización. Solo al aprobar el coordinador autorizado puede generarse/activarse la sesión con su propia ventana y auditoría; rechazo implica que no se crea la sesión.
4. **Calendario institucional variable:** sesiones ordinarias derivadas del horario/periodo (por ejemplo 16, no un número fijo universal); las extraordinarias se diferencian explícitamente, sin cambiar retroactivamente las sesiones predeterminadas.
5. **QR/PIN reales de extremo a extremo (por diseñar):** reemplazar QR decorativo y PIN fijo por mecanismo persistido/verificable en DB y validado en Backend, consumido por Angular. Revisar si ya existen contratos/entidades (`sesiones/{id}/qr-token`, `estudiante/asistencia-qr`) para ampliarlos sin duplicar. Estudiar generación segura, caducidad, revocación, ownership, unicidad/uso único o rotación, no divulgación de secretos y registro de evidencias. **No implementar una solución de QR ni elegir almacenamiento/codificación hasta recibir las decisiones específicas del usuario**.

**Casos obligatorios de aceptación**: sesión 08:00–10:00 permite toma de asistencia solo dentro de la ventana; a las 10:00:00 se bloquea toda escritura ordinaria; intento a las 12:00 responde rechazo contractual sin mutación; F5, API directa, concurrencia y reloj local modificado no evaden cierre. Una excusa verificada aprobada tras el cierre produce una única rectificación y registro de auditoría; rechazada no modifica nada. Solicitud extraordinaria permanece PENDIENTE y genera notificación trazable; otro coordinador o un docente no pueden aprobar; aprobación repetida nunca duplica una sesión. Sin correo disponible, la solicitud debe conservarse y la notificación poder reintentarse de forma idempotente.

## Prioridades y propiedad

| ID | Prioridad | Responsable | Hallazgo / trabajo | Validación obligatoria |
| --- | --- | --- | --- | --- |
| SEC-01 | P0 | Seguridad-UCO | `GET /api/v1/users` y `GET /api/v1/applications` devuelven 200 a Bearer de ESTUDIANTE sin rol administrativo | 403 o filtrado estricto para sujetos sin autorización; pruebas cross-tenant |
| SEC-02 | P0 | Seguridad-UCO | 67/150 respuestas 503 con 50 decisiones simultáneas en 0.1.9; serialización ≈68 ms/decisión; causa no demostrada | Trazas, análisis del cuello de botella, reducción de 503 y sin fail-open |
| SEC-03 | P0 | Integración y Seguridad-UCO | Rotar dos credenciales PEP expuestas en salida de herramienta | Rotación coordinada, secreto anterior inválido, cliente actualizado, evidencias sanitizadas |
| SEC-04 | P1 | Seguridad-UCO | Definir credencial técnica y scopes mínimos para sincronizar perfiles PDP | Política por aplicación, revocación, auditoría, pruebas negativas; sincronización permanece apagada |
| SEC-05 | P1 | Owner catálogo | Concesión ESTUDIANTE sobrante y tres recursos huérfanos | Decisión explícita antes de `--prune` |
| APP-01 | P1 | Backend + Frontend | Parámetros admin: Angular PATCH no registrado, PUT 501; mensajes engañosos | Contrato y catálogo alineados, guardar y recargar, errores reales y RBAC |
| APP-02 | P1 | Backend | `POST /api/v1/archivos/subir` no multipart devuelve 500 | 400/415 con error sanitizado; multipart válido conserva ClamAV y ownership |
| APP-03 | P1 | Frontend + Backend + DB | «Proyectar QR» usa PIN fijo y QR decorativo | Diseñar e implementar QR/PIN reales sobre contratos existentes; decisiones de diseño aún pendientes del usuario |
| APP-04 | P1 | Backend + Frontend | Horario de sesiones presenta +5 horas según rol | Contrato UTC inequívoco, visualización local, pruebas v1/v2 |
| APP-05 | P1 | Frontend | Diálogo de reclamos docente marca 0 por bandera obsoleta | GET real, estados y grupo correctos |
| APP-06 | P1 | Frontend | Perfil estudiante oculta identificación por bandera obsoleta | Mostrar dato de API correctamente sin almacenamiento persistente |
| APP-07 | P1 | Backend + Frontend | 501 de sedes, auditoría, parámetros, matrícula, etc. con acciones visibles | Matriz método/endpoint/owner, implementar o deshabilitar CTA con estado correcto |
| APP-08 | P1 | Backend | Solicitudes ingreso de grupo ajeno responden 200 [] en lugar de prohibición de propiedad | 403 o política contractual explícita; no divulgar existencia |
| DATA-01 | P1 | DB/negocio | Cuentas semilla uco-decano sin facultad, uco-coordinador sin programa | Vinculación aprobada vía SP oficial, reversibilidad, E2E por rol |
| DATA-02 | P2 | DB/Frontend | 7 estudiantes inscritos pero horario docente indica 0, franjas/aula faltan | Fuente de matrícula consistente, prueba con/sin franja |
| DATA-03 | P2 | Negocio/DB | Oferta de grupos [] y asistencia consolidada no disponible | Determinar elegibilidad/periodo y contrato antes de modificar consultas |
| UX-01 | P2 | Frontend | No confundir 501 con listas vacías o 503 con pérdida de credenciales | Mensajes específicos, reintento seguro, no saltar guardas |

## NUEVO: política de sesiones y asistencia (propuesta de negocio a validar)

**SES-01 — cierre por horario y control temporal.** Una sesión programada debe tener inicio y fin inequívocos en UTC (visualizados en America/Bogota). Al llegar el instante de cierre, impedir que el docente registre, edite, elimine o aplique operaciones masivas de asistencia desde la pantalla ordinaria. No basta deshabilitar botones: el backend debe rechazar cambios fuera de ventana con código contractual (409/422 según decisión), incluso mediante API directa, reintentos o carreras. Una sesión pasada no debe volver a habilitarse por refresco, huso horario ni cambio de reloj local. El corte al fin programado es obligatorio y sin gracia posterior por defecto. Por definir con negocio: anticipación para comenzar, cierre anticipado, captura tardía por contingencia, ausencias automáticas y políticas para caídas de red; no alterar la regla sin autorización.

**SES-02 — sesiones programadas.** Generar las sesiones previstas (ej. 16 por período, o cantidad variable según calendario oficial, grupo, franja, festivos y novedades), sin duplicar al recalcular. Contar y verificar el calendario efectivo, no codificar «16» como constante universal. Diferenciar PROGRAMADA, EN_CURSO, CERRADA y CANCELADA de manera consistente con el modelo de datos y contratos existentes; evaluar migración antes de introducir estados.

**SES-03 — solicitud de sesión extraordinaria por DOCENTE.** Si el docente no pudo dictar en el horario previsto, no debe poder habilitar unilateralmente una sesión adicional que afecte la asistencia oficial. Flujo confirmado: DOCENTE crea **solicitud** justificada asociada a grupo, asignatura, motivo, fecha propuesta y franja; permanece PENDIENTE sin crear ni habilitar sesión oficial. Registrar evento y avisar al COORDINADOR responsable del programa por notificación persistente y correo institucional (canal concreto por acordar), con reintento seguro sin duplicar avisos ni solicitudes. COORDINADOR responsable del programa al que pertenece el grupo revisa y APRUEBA o RECHAZA. Solo la aprobación válida, auditada e idempotente crea/activa la nueva sesión, con su ventana temporal propia. La solicitud original y la decisión conservan actor, fecha, motivo y correlación; rechazar no crea sesión. Si hay conflicto horario, duplicados, grupo ajeno, cambio de coordinador o fecha pasada, validar política antes de aprobar. Confirmar en producto si el decano o administrador puede sustituir al coordinador; no asumir.

**SES-04 — reclamos posteriores al cierre.** Tras cerrar una sesión, la asistencia **solo** puede cambiar por un flujo formal de revisión/reclamo con autorización y evidencia; el docente no debe poder modificarla desde «Tomar asistencia». Si recibe una excusa y la aprueba o rechaza, la decisión se procesa de forma transaccional e idempotente: aprobación ajusta el estado correspondiente a la asistencia y conserva valor anterior, valor nuevo, responsable, fecha, motivo, soporte y trazabilidad; rechazo mantiene el estado original (salvo otra regla acordada). Validar las categorías reales de asistencia y reclamos existentes, y quién tiene competencia para resolver. No editar directamente tablas ni sobreescribir historias.

**SES-05 — permisos, concurrencia, reportes y auditoría.** El backend debe verificar rol y ownership (docente del grupo, coordinador del programa, estudiante de la sesión), rango de fechas UTC, estados, límite temporal, duplicados, transiciones permitidas, replay de peticiones, autorización PEP/PDP y privacidad de archivos. Dos aprobaciones concurrentes jamás deben crear dos sesiones. Los reportes consolidados deben distinguir asistencia original, rectificación por reclamo y última decisión válida, manteniendo trazabilidad y sin doble conteo. Definir observabilidad y avisos en UI (sesión cerrada, solicitud pendiente, aprobada, rechazada, reclamo resuelto).

### Criterios de aceptación mínimos de SES-01..05

1. Sesión futura: no se toma asistencia antes de apertura salvo política aprobada; sesión activa: solo docente autorizado registra; sesión cerrada a la **hora exacta de fin**: UI bloqueada **y** API rechaza toda edición ordinaria, incluso dos horas después.
2. Expiración durante edición y dos solicitudes simultáneas: ninguna escritura fuera del plazo; comparación con reloj de servidor, no navegador; no se altera la sesión histórica.
3. Nueva sesión extraordinaria: solicitud de docente no habilita asistencia; coordinador del programa correcto puede aprobar una sola vez; coordinador de otro programa y docente reciben 403; rechazo no crea sesión.
4. Una excusa válida aprobada tras cierre modifica asistencia conforme al contrato y deja auditoría; excusa rechazada no modifica; sin soporte/permiso devuelve error controlado.
5. Pruebas de UTC/Bogotá y fechas límite; idempotencia, concurrencia, ownership, seguridad fail-closed, compatibilidad V1/V2, reportes y frontend.
6. No modificar SQL Server ni crear migraciones sin revisar contratos/SP y aprobación del owner DB. Escribir casos de prueba primero.

## Orden para la siguiente jornada

1. Revisar modelado actual de sesiones/estados, SP de creación y cierre, ownership, notificaciones y flujo de reclamos antes de programar SES-01..05; mantener las decisiones ya confirmadas, resolver únicamente las reglas abiertas con negocio.
2. Abrir historias separadas de Backend, Frontend y DB para SES-01..05 con contratos/API y pruebas E2E; una regla de UI sola no satisface la seguridad.
3. Resolver APP-02, APP-08 y APP-01 por prioridad, luego APP-03/04 y banderas APP-05/06; coordinar 501/semillas.
4. Seguimiento por separado de SEC-01..04 con Seguridad-UCO; no mezclar cambios del proveedor con PR de AsistenciasUCO.

## Evidencias

- `docs/work-items/SEC-BASELINE-018-upgrade-login/VALIDATION.md`
- `docs/work-items/SEC-BASELINE-019-upgrade-performance/VALIDATION.md`
- `docs/security/PDP_ADMIN_API_AUTHORIZATION_FINDING.md`
- Informe externo `Informe_hallazgos_Security_Baseline_019_Seguridad_UCO.pdf` (preparado en conversación; no incluir secretos)
