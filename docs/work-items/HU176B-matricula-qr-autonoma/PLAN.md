---
status: draft
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# PLAN — HU176B: Matrícula autónoma por QR/PIN del curso, con creación de cuenta y validación de red del campus

Fase 01: Planificación y Definition of Ready (DoR), según `GUIA_METODICA_IMPLEMENTACION_HU.md`, `docs/governance/DEFINITION_OF_READY.md` y `AGENTS.md`.
Programa padre: [`REG-000`](../REG-000-programa-registro-vinculacion-roles/PLAN.md), grupo **C**. Depende de HU050 ✔ (matrícula manual). Recomienda tener IAM-001 (grupo E) cerrado, por la restricción de `POST /usuarios`.

> **Cambio de alcance respecto de REG-000:** el índice agrupaba HU175 con HU176B. Al revisar el código se ve que **HU175 es el QR dinámico de *asistencia* por sesión** (`GET /api/v1/sesiones/{sesionId}/qr-token`, flag `sessionQrEnabled`), no de matrícula. Se propone sacarlo de este programa (D1) y que lo retome el módulo de asistencia, reutilizando el mecanismo de token de este WI.

---

## 1. Identidad y objetivo

- **Historia de usuario:** **HU176B**, matrícula autónoma por QR del curso proyectado por el docente.
- **Regla institucional que implementa:**
  - Catálogo, regla 4: el QR de matrícula "**crea la cuenta de usuario y matricula al alumno en el grupo**".
  - Catálogo, regla 5 y AGENTS.md §3.3: la marcación autónoma por QR o PIN **valida la red Wi-Fi del campus**.
- **Work Item ID:** `HU176B-matricula-qr-autonoma`. **Fecha:** 2026-10-21. **Base Git:** `develop` @ `4ff37ff`.
- **Actores:**
  - **Docente titular** del grupo: abre y cierra la ventana de matrícula y proyecta el QR/PIN.
  - **Estudiante:** escanea o digita el PIN desde la red del campus y se registra (cuenta nueva) o se matricula (cuenta existente).

### Criterios de aceptación

| ID | Criterio |
|---|---|
| CA-01 | El docente titular **abre una ventana de matrícula** de su grupo: recibe un QR (imagen generada por el **backend**), un PIN numérico corto, la URL de matrícula y la hora de rotación. Solo hay una ventana activa por grupo; abrir otra revoca la anterior. |
| CA-01b | **(D2 = QR rotativo, 2026-11-04)** Mientras la ventana está abierta, el backend **rota** el token y el PIN cada `ROTACION_SEGUNDOS` (60 s). El token vigente y el **inmediatamente anterior** (durante `TOLERANCIA_SEGUNDOS`, 60 s) son válidos; cualquier otro se rechaza. El QR nuevo llega a la pantalla del docente **por SSE dirigido solo a él**, sin polling ni `setInterval`. La ventana termina al cerrarla el docente o al llegar a `DURACION_MAXIMA_MINUTOS`. |
| CA-02 | El docente puede **cerrar** la ventana antes de que expire, y **regenerar** el código de inmediato. Un token expirado, rotado fuera de tolerancia o revocado se rechaza con un mensaje de catálogo. |
| CA-03 | Un estudiante **sin cuenta** abre la URL del QR (o la pantalla de PIN) **sin iniciar sesión**, ve los datos mínimos del curso y completa tipo y número de documento, nombres, correo y contraseña inicial (DA-5). Se crean el usuario, el estudiante y la matrícula **activa** en el grupo, y la cuenta en Keycloak con rol `ESTUDIANTE`. |
| CA-04 | Un estudiante **con cuenta** inicia sesión y se matricula con el mismo token o PIN (`POST /estudiante/matricular-grupo`). No se duplica ninguna cuenta. |
| CA-05 | Toda validación y registro por token exige que la IP del cliente pertenezca a los **rangos de red del campus**, configurados como parámetro (App Configuration). Fuera del campus: 403 con un mensaje de catálogo. |
| CA-06 | Se aplican las reglas de la matrícula manual: cupo, cruce de horario, duplicado en el grupo, programa del estudiante y consistencia institucional. La matrícula es **atómica**. |
| CA-07 | Los endpoints públicos tienen **límite de intentos** por IP y por token, y el token se guarda **hasheado** en la DB. El PIN solo funciona mientras la ventana está activa. |
| CA-08 | El roster del docente se actualiza **por SSE** (`ESTUDIANTE_MATRICULADO`) sin polling. El evento no lleva datos personales y se entrega **solo al docente emisor** (SSE dirigido, ver §8.1). |
| CA-09 | Desaparece la generación del QR en el cliente con `api.qrserver.com` y el "PIN = id del grupo". |

---

## 2. AS-IS (evidencia) vs. TARGET

| Dimensión | AS-IS (evidencia) | TARGET |
|---|---|---|
| **Token de matrícula** | No existe. En el front, `teacher-matricula-modal.component.ts` (l.127-142) arma `${origin}/app/estudiante/materias?matricularGrupo=<idGrupo>` y la imagen con **`https://api.qrserver.com`**, que envía la URL a un tercero. El "PIN" **es el id del grupo** (l.55-62): no expira y es adivinable si se filtra. | Tabla `TokenMatriculaGrupo` con hash SHA-256 del token, PIN de 6 dígitos único entre las ventanas activas, emisión y expiración en UTC, estado (catálogo) y docente emisor. La ventana la abre y cierra el docente; el TTL es un parámetro (D2). |
| **QR del curso** | Lo genera un tercero (arriba). En el backend no hay librería de QR (Grep en `pom.xml`: sin `zxing`). | El backend genera un **PNG en base64** con ZXing (D4) y lo devuelve en la respuesta de apertura. El front lo muestra con `<img [src]="'data:image/png;base64,' + qr">`, sin `bypassSecurityTrust*`. |
| **SP de matrícula autónoma** | `usp_registrar_estudiante_en_grupo_autonomo.sql`: **sin RBAC ni validación de token**, **sin transacción**; crea el usuario con `@estaActivo = 0` (l.92), lo que impide el login, y la matrícula en estado `'I'` (l.116). Ningún backend lo invoca. | Se retira (D6), junto con su test. Lo reemplazan `usp_matricular_estudiante_por_token` (transaccional; la credencial es el token) y los SP de ventana. Se reutiliza `usp_registrar_estudiante_en_grupo_interno` (`@codigoEstado`, cupo, cruce, programa). |
| **SP interno de matrícula** | `usp_registrar_estudiante_en_grupo_interno` recibe `@codigoEstado NVARCHAR(10) = 'A'` y hace `IF @codigoEstadoDefecto = '' SET @codigoEstadoDefecto = 'A'` (l.46): un **literal quemado** por defecto. | Sin cambio de firma. Las fachadas nuevas pasan el código **resuelto desde el parámetro** `MATRICULA_QR / ESTADO_MATRICULA` (D3), sin literal. El valor por defecto del interno queda como deuda (ver §13) para no tocar HU050. |
| **Asistencia por QR (referencia, no se toca)** | `usp_registrar_asistencia_estudiante_autonomo` compara `@codigoVerificacion` con un `Sesion.codigo` **estático** (l.93-97). Sin TTL ni validación de red. `GET /sesiones/{id}/qr-token` → 501. | Fuera de alcance (D1). El mecanismo de token y de red de este WI queda disponible para que el módulo de asistencia lo reutilice en HU175/HU176. |
| **Backend: endpoints** | `EstudiantePortalController.java:80-81`: `POST /api/v1/estudiante/matricular-grupo` → 501. No hay endpoints públicos de negocio: `SecurityConfig` solo deja `permitAll` en actuator, el webhook de Event Grid y Swagger (l.121-132). No existe la apertura de ventana. | Docente: `POST/DELETE /api/v1/grupos/{grupoId}/matricula-qr`. Estudiante autenticado: `POST /api/v1/estudiante/matricular-grupo`, que deja de responder 501. Públicos **nuevos**: `POST /api/v1/public/matricula-qr/validar` y `POST /api/v1/public/matricula-qr/registro`, con `permitAll` explícito, rate limit y red del campus. |
| **Validación de red** | No existe en ninguna capa. `ClientIpResolver` (`controller/filter/`) solo sanea `request.getRemoteAddr()` para los logs de `AuditInterceptor`. | Puerto `RedInstitucionalPort` (aplicación) + adapter `CidrRedInstitucionalAdapter`. Lee los rangos CIDR de `ParameterCatalogPort` (`asistencias:red:cidrs-campus`) y resuelve la IP con `ClientIpResolver` extendido: solo confía en `X-Forwarded-For` si la petición llega desde un proxy de `asistencias:red:proxies-confiables` (D5). |
| **Rate limiting** | No existe (Grep: sin `bucket4j`, `resilience4j` ni rate limit). | Filtro `PublicEndpointRateLimitFilter` solo para `/api/v1/public/**`: ventana deslizante en memoria por IP y por hash de token, con límites parametrizados. Responde 429 con un mensaje de catálogo (D7). |
| **Identidad** | Patrón de `RegistrarEstudianteUseCaseImpl` (HU050): hash a la DB y `crearCuenta(..., ESTUDIANTE)` después del commit. | El mismo patrón. Cuando IAM-001 esté cerrado se usa `SincronizarCuentaInstitucionalService`; si no, el de HU050 con TD-015. **Sin contraseña por defecto** (DA-5). |
| **Realtime** | `RealtimePublisherPort.publish(RealtimeEvent)` (tipos `UPPER_SNAKE`, p. ej. `ASISTENCIA_REGISTRADA` en `RegistrarAsistenciaUseCaseImpl`). `ReactorRealtimeAdapter` **no filtra por destinatario**: todo evento llega a todos los clientes SSE conectados. | Evento `ESTUDIANTE_MATRICULADO` con payload `{ idGrupo, origen: 'QR' }`, **sin PII**. El roster del docente reacciona solo si `idGrupo` es uno de sus grupos y vuelve a pedir la lista paginada con ámbito. |
| **Frontend: estudiante** | `student-courses.component.ts` lee `?matricularGrupo=` (l.195) y llama a `StudentManagementService.matricularGrupo(pin)` → `POST /estudiante/matricular-grupo` (501). Solo sirve con sesión iniciada. `features/auth/register/register.component.ts` no se puede abrir (`app.routes.ts:26`: `register` → `login`). No hay ruta pública de matrícula. | Rutas **públicas** nuevas `/matricula/qr?t=<token>` y `/matricula/pin`, con el componente `MatriculaQrComponent`: valida, muestra el curso, y ofrece "Crear cuenta y matricularme" o "Ya tengo cuenta" (login con `returnUrl`, y luego `matricular-grupo`). El parámetro `matricularGrupo` de `student-courses` pasa a aceptar token o PIN (ya no el id del grupo). `register.component` se retira o se reutiliza dentro del flujo (D8). |
| **Frontend: docente** | `teacher-matricula-modal.component.ts` (con spec) es solo cliente. `teacher-proyeccion-qr-modal.component.ts` (sin spec) es de asistencia y no se toca. | El modal llama a `abrirVentanaMatricula(grupoId)` y muestra el QR del backend, el PIN, la expiración como **hora absoluta** (sin cuenta regresiva con timers) y los botones Cerrar y Regenerar. El roster escucha `ESTUDIANTE_MATRICULADO` a través de `RealtimeService`. |

---

## 3. Clase de cambio y perímetro de rutas

- **Change class:** `BEHAVIOR_CHANGE` + `CONTRACT_CHANGE` aditivo + **DDL** (tabla de tokens) + **SECURITY** (primeros endpoints públicos de negocio, validación de red, rate limit) + dependencia nueva (ZXing, D4).
- **PRIMARY_VARIABLE:** matrícula autónoma por token de ventana emitido por el docente, con la red del campus como segundo factor.
- **ALLOWED_PATHS:**
  - `GestioAsistenciaBackend/docs/work-items/HU176B-matricula-qr-autonoma/**`
  - DB:
    - `GestioAsistenciaDB/migrations/<nn>_hu176b_token_matricula_grupo.sql` y `schema/tables/TokenMatriculaGrupo.sql` (nuevos); `schema/tables/zz_foreign_keys.sql` (agregar FKs)
    - `GestioAsistenciaDB/schema/stored-procedures/{usp_abrir_ventana_matricula_grupo, usp_cerrar_ventana_matricula_grupo, usp_validar_token_matricula_publico, usp_matricular_estudiante_por_token, usp_internal_resolver_token_matricula}.sql` (nuevos)
    - `GestioAsistenciaDB/schema/views/uv_token_matricula_grupo.sql` (nueva)
    - `GestioAsistenciaDB/schema/seed/{03_parametro.sql, 04_mensaje.sql}` (parámetros `MATRICULA_QR/*` y códigos `MQR_*`)
    - `GestioAsistenciaDB/schema/stored-procedures/usp_registrar_estudiante_en_grupo_autonomo.sql` + `test/test_usp_registrar_estudiante_en_grupo_autonomo.sql` (retiro, D6)
    - `GestioAsistenciaDB/test/{test_usp_abrir_ventana_matricula_grupo, test_usp_cerrar_ventana_matricula_grupo, test_usp_validar_token_matricula_publico, test_usp_matricular_estudiante_por_token}.sql` (nuevos)
  - Backend:
    - `GestioAsistenciaBackend/pom.xml` (solo `com.google.zxing:core` y `javase`, D4)
    - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/features/matriculaqr/{abrirventana, cerrarventana, validartoken, registrarpormatriculaqr, matricularestudianteautenticado}/**` (nuevos)
    - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/{academic/TokenMatriculaCommandPort.java, academic/TokenMatriculaQueryPort.java, network/RedInstitucionalPort.java, qr/CodigoQrGeneratorPort.java, security/TokenGeneratorPort.java}` (nuevos)
    - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/{persistence/sqlserver/jpa/repository/TokenMatriculaJpaRepository.java, persistence/sqlserver/jpa/entity/Sp*TokenMatricula*Entity.java, network/CidrRedInstitucionalAdapter.java, qr/ZxingCodigoQrGeneratorAdapter.java, security/SecureRandomTokenGeneratorAdapter.java}` (nuevos)
    - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/{publico/MatriculaQrPublicaController.java, grupo/GrupoMatriculaQrController.java, estudiante/EstudiantePortalController.java, filter/ClientIpResolver.java, filter/PublicEndpointRateLimitFilter.java}` (+ request, response, mapper y validator)
    - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/wiring/MatriculaQrWiringConfiguration.java` (nuevo)
    - **(D2)** Rotación: `application/features/matriculaqr/rotartoken/**` y `infrastructure/scheduling/RotacionTokenMatriculaScheduler.java` (nuevos); SP `usp_rotar_token_matricula_grupo` y `usp_consultar_ventanas_matricula_por_rotar` (DB).
    - **(D2)** Realtime dirigido (ampliación aditiva): `application/secondaryports/realtime/RealtimePublisherPort.java` (método `publishToUser`), `RealtimeEvent` (destinatario opcional) y `ReactorRealtimeAdapter` con sus tests. Frontend: lectura de `core/realtime/**`; solo se cambia si hace falta exponer el filtro por tipo de evento.
    - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/security/SecurityConfig.java` ⚠️ (solo `permitAll` de `/api/v1/public/matricula-qr/**`, la regla `DOCENTE` de `/api/v1/grupos/*/matricula-qr` y el registro del filtro)
    - `GestioAsistenciaBackend/src/main/java/.../support/error/DbFailureClassifier.java` (`MQR_*`)
    - `GestioAsistenciaBackend/src/test/java/co/edu/uco/asistenciasuco/**` (tests del perímetro + `RbacSecurityFilterChainTest` ⚠️)
    - `GestioAsistenciaBackend/docs/contracts/HTTP_AS_IS_MATRIX.md` ⚠️ (fase 06)
  - Frontend:
    - `GestioAsistenciaFrontend/src/app/app.routes.ts` (rutas públicas `matricula/qr` y `matricula/pin`; la ruta `register` según D8)
    - `GestioAsistenciaFrontend/src/app/features/public/matricula-qr/**` (nuevo)
    - `GestioAsistenciaFrontend/src/app/core/services/matricula-qr.service{,.spec}.ts` (nuevo), `core/models/matricula-qr.model.ts` (nuevo)
    - `GestioAsistenciaFrontend/src/app/core/services/student-management.service{,.spec}.ts` (solo `matricularGrupo`)
    - `GestioAsistenciaFrontend/src/app/features/teacher/teacher-grupos/components/modals/teacher-matricula-modal.component{,.spec}.ts`
    - `GestioAsistenciaFrontend/src/app/features/teacher/teacher-grupos/components/teacher-grupo-estudiantes.component{,.spec}.ts` (suscripción SSE)
    - `GestioAsistenciaFrontend/src/app/features/student/student-courses/student-courses.component{,.spec}.ts` y `components/modals/student-matricula-codigo-modal.component{,.spec}.ts` (el spec es nuevo; hoy no existe)
    - `GestioAsistenciaFrontend/src/app/features/auth/register/**` (solo con D8)
- **FORBIDDEN_PATHS:**
  - Contratos WORM.
  - `usp_registrar_estudiante_en_grupo.sql` y su interno (HU050 certificada; solo se **invoca** el interno).
  - Toda la asistencia por QR (`usp_registrar_asistencia_estudiante_autonomo`, `AsistenciaQrController`, `teacher-proyeccion-qr-modal`).
  - `Usuario.sql` / `Estudiante.sql` / `EstudianteGrupo.sql`.
  - Archivos de HU035/036.
  - `AsistenciasUCO-Infra/**` hasta la fase 06 (salvo la documentación de los parámetros de App Configuration).
- **CONTRACTS:** HTTP (3 rutas nuevas + 1 que sale de 501); DB (tabla, vista, SP nuevos y retiro de un SP).
- **PROVIDERS:** SQL Server, Keycloak, App Configuration (rangos de red, proxies, límites), SSE local.
- **EXTERNAL_ENVIRONMENT:** `sql_server_asistencias` y Keycloak `:8081`. Para el E2E de red: rangos de desarrollo `127.0.0.1/32` y `::1/128` en el perfil local, y un caso negativo que simula una IP fuera del campus con un proxy confiable de prueba.
- **SECURITY_IMPACT:** **alto**.
  - Primeros endpoints de negocio sin JWT.
  - Mitigaciones:
    - token aleatorio de 256 bits, guardado como hash;
    - ventana corta abierta por el docente;
    - red del campus;
    - rate limit;
    - el cupo del grupo como tope natural;
    - respuestas mínimas: la validación no expone datos de estudiantes y muestra solo el nombre del docente;
    - mensajes genéricos ante un token inválido, sin distinguir "no existe" de "expiró", para no permitir enumerar.
  - **No se agrega captcha**: queda como decisión de producción.
- **OBSERVABILITY_IMPACT:**
  - Logs de `MATRICULA_QR_VENTANA_ABIERTA`, `MATRICULA_QR_VALIDACION`, `MATRICULA_QR_REGISTRO`, `MATRICULA_QR_RED_RECHAZADA` y `MATRICULA_QR_RATE_LIMIT`, con `correlationId`, `idGrupo` y el hash parcial de la IP. **Nunca** el token ni el PIN.
  - Contadores `matricula_qr_total{resultado}`.
- **TEST_LEVEL_REQUIRED:** T-SQL + JUnit (unit, slice, security, filtro) + IT SQL Server + `FakeKeycloakAdminServer` + Jasmine + E2E.
- **ROLLBACK:** revertir los PRs (Front → Back → DB). La migración tiene `down`. Los endpoints públicos se pueden apagar por configuración (`asistencias:matricula-qr:habilitada=false` → 404) sin redeploy (D9).
- **CONSUMERS:** docente (modal de matrícula), estudiante (rutas públicas y portal), grupo D (las solicitudes reutilizan el token), módulo de asistencia (HU175/HU176 a futuro).
- **STOP_CONDITIONS:**
  - D1–D9 sin resolver.
  - El owner de seguridad no aprueba endpoints públicos sin captcha → `BLOCKED_BY_MISSING_EVIDENCE`.
  - No se conocen los rangos reales de la red del campus para producción (en desarrollo basta con localhost).
  - HU035/036 sin merge al llegar a los pasos ⚠️.

Lo que no está en ALLOWED_PATHS no se modifica. Ampliar el alcance exige actualizar este PLAN con aprobación.

---

## 4. Alcance y no alcance

### Alcance
1. **DB:** tabla y vista de tokens; SP de abrir y cerrar ventana, validar (público) y matricular por token (público o autenticado), todos transaccionales y con delegación; parámetros y mensajes; retiro del SP autónomo; pruebas T-SQL.
2. **Backend:**
   - generación de token y PIN y del PNG del QR;
   - endpoints del docente, públicos y del estudiante autenticado;
   - validación de red;
   - rate limit;
   - aprovisionamiento en Keycloak;
   - evento SSE;
   - pruebas.
3. **Frontend:** rutas públicas de matrícula, modal del docente con el QR del backend, actualización del roster por SSE, `matricularGrupo` con token o PIN, specs.

### No alcance
1. HU175 / HU176: QR dinámico de asistencia y validación de red en la asistencia (D1).
2. Solicitudes de matrícula fuera de una ventana, y aprobación por docente o coordinador: **grupo D** (HU026 / HU053–HU055).
3. Retirar al estudiante del grupo (grupo J).
4. Captcha y verificación del correo (TD-014).
5. Cambiar `usp_registrar_estudiante_en_grupo_interno` (deuda §13).

---

## 5. Archivos afectados

### Existentes (ruta comprobada)
- DB:
  - `schema/stored-procedures/usp_registrar_estudiante_en_grupo_autonomo.sql`, `usp_registrar_estudiante_en_grupo_interno.sql`, `usp_registrar_estudiante_en_grupo.sql` (patrón), `usp_sincronizar_usuario_interno.sql`, `usp_validar_cupo_disponible_grupo_interno.sql`
  - `schema/views/uv_grupo.sql`, `uv_estado_estudiante_grupo.sql`
  - `schema/seed/03_parametro.sql`, `04_mensaje.sql`, `07_estado_estudiante_grupo.sql`
  - `test/test_usp_registrar_estudiante_en_grupo_autonomo.sql`, `test_suite.sql` ⚠️
- Backend:
  - `controller/estudiante/EstudiantePortalController.java`
  - `controller/filter/ClientIpResolver.java`, `AuditInterceptor.java`
  - `config/security/SecurityConfig.java` ⚠️
  - `application/secondaryports/realtime/{RealtimePublisherPort, RealtimeEvent}.java`
  - `application/features/grupo/registrarestudianteengrupo/**` (patrón HU050)
  - `ParameterCatalogPort` / `MessageCatalogPort` (skill `uco-catalogos`)
  - `pom.xml`
- Frontend:
  - `app.routes.ts`
  - `features/teacher/teacher-grupos/components/modals/teacher-matricula-modal.component{,.spec}.ts`
  - `features/teacher/teacher-grupos/components/teacher-grupo-estudiantes.component{,.spec}.ts`
  - `features/student/student-courses/student-courses.component{,.spec}.ts`, `components/modals/student-matricula-codigo-modal.component.ts`
  - `core/services/student-management.service{,.spec}.ts`
  - `features/auth/register/register.component{,.spec}.ts`
  - `core/realtime/**` (lectura, para suscribirse)

### Nuevos
Los de §3 (ALLOWED_PATHS) y los tests de [TEST_PLAN.md](TEST_PLAN.md).

### Retirar
- `usp_registrar_estudiante_en_grupo_autonomo` y su test (D6). Grep del 2026-10-21: no lo invoca el backend ni el front.
- La URL a `api.qrserver.com`.
- El "PIN = id de grupo".

---

## 6. Contratos y consumidores afectados

| Tipo | Contrato | Compatibilidad | Consumidor |
|---|---|---|---|
| PERSISTENCE | `TokenMatriculaGrupo`, la vista y los SP nuevos | Aditivo | Backend nuevo |
| PERSISTENCE | Retiro de `usp_registrar_estudiante_en_grupo_autonomo` | Sin consumidores | — |
| HTTP | `POST/DELETE /grupos/{grupoId}/matricula-qr` (DOCENTE) | Aditivo | Modal del docente |
| HTTP | `POST /public/matricula-qr/validar`, `POST /public/matricula-qr/registro` (**público**) | Aditivo; el primer patrón público de negocio | Ruta pública del front |
| HTTP | `POST /estudiante/matricular-grupo` | Sale de 501. El body pasa de `{codigoOPin}` a `{ token?, pin? }` | `student-courses` |
| SECURITY | `permitAll` acotado + filtro de rate limit + validación de red | Nuevo | — |
| REALTIME | `ESTUDIANTE_MATRICULADO` | Aditivo | Roster del docente |
| BUILD | ZXing | Dependencia nueva (Apache-2.0) | — |

---

## 7. Archivos calientes

`SecurityConfig.java`, `RbacSecurityFilterChainTest.java`, `HTTP_AS_IS_MATRIX.md` y `test_suite.sql` → después del merge de HU035/036. El resto puede avanzar antes en `feature/HU176B-matricula-qr`.

---

## 8. Decisiones requeridas (bloquean READY)

| ID | Pregunta | Opciones | Recomendación |
|---|---|---|---|
| **D1** | ¿HU175 sigue en este grupo? | (a) Sacarla del programa REG: es QR de **asistencia** y la retoma el módulo de asistencia reutilizando el token y la red de este WI. (b) Incluirla aquí. | **(a)**: no es registro de roles y duplicaría el alcance (sesiones, TTL de 60 s, rotación). |
| **D2** | ¿Cómo vive el token de matrícula? | (a) Ventana que el docente abre y cierra, con TTL por parámetro (`MATRICULA_QR / TTL_MINUTOS`, propuesta 15) y regeneración manual. (b) QR que rota cada 60 s, como HU175. | **(a)**: la rotación automática obligaría a refrescar la pantalla del docente con timers o con SSE, y el SSE actual **no filtra destinatario** (el token llegaría a todos los clientes). La ventana corta junto con la red del campus da una protección equivalente para matrícula. |
| **D3** | ¿Con qué estado queda la matrícula por QR? | (a) Activa (`A`): la regla 4 dice que el QR "matricula al alumno". (b) Pre-matrícula (`I`) pendiente de aprobación (como el SP autónomo actual). | **(a)**: la ventana la abre el docente en clase. La aprobación de solicitudes fuera de ventana es del grupo D. El código se resuelve desde el parámetro `MATRICULA_QR / ESTADO_MATRICULA`. |
| **D4** | ¿Quién genera la imagen QR? | (a) El backend, con ZXing (PNG base64). (b) El front, con una librería npm (`qrcode`). (c) Un servicio externo (hoy). | **(a)**: deja al front como presentación pura, no expone la URL a terceros y no agrega dependencias al front. (c) queda prohibida. |
| **D5** | Resolución de la IP real detrás de proxies | (a) `X-Forwarded-For` solo si `remoteAddr` está en `asistencias:red:proxies-confiables`; en otro caso `remoteAddr`. (b) Confiar siempre en `X-Forwarded-For`. | **(a)**: (b) permite falsear la red del campus con un header. |
| **D6** | Retirar `usp_registrar_estudiante_en_grupo_autonomo` | (a) Sí, con su test (sin consumidores). (b) Mantenerlo. | **(a)**: no tiene RBAC ni transacción y deja usuarios inactivos que no pueden entrar. |
| **D7** | Rate limit | (a) En memoria por instancia (ventana deslizante; límites por parámetro: propuesta de 20 intentos/min por IP y 5 registros/min por token). (b) Distribuido (Redis), que no existe hoy. | **(a)** para el despliegue actual de una instancia; (b) se anota para escalar. |
| **D8** | `register.component` (inaccesible) y `POST /usuarios` | (a) Retirar el componente: su función la cubre la ruta pública de matrícula. `POST /usuarios` queda solo para el admin (IAM-001 D4) o se retira. (b) Reutilizar el formulario dentro de `MatriculaQrComponent`. | **(b)**: se reutiliza su UI (campos y validaciones de forma) y se retira la ruta `register`. `POST /usuarios` se retira cuando este WI cierre, si IAM-001 lo dejó solo para el admin y nadie lo usa. |
| **D9** | ¿Interruptor de la funcionalidad? | (a) Parámetro `asistencias:matricula-qr:habilitada` (App Configuration): si está en `false`, los endpoints públicos responden 404 y el botón del docente se oculta. (b) Sin interruptor. | **(a)**: permite apagar la superficie pública sin redeploy ante un abuso. |

### 8.1 Resoluciones del owner (2026-11-04)

| ID | Resolución | Efecto en el diseño |
|---|---|---|
| **D1** | **(a)** Solo matrícula. HU175/HU176 quedan para un WI posterior que reutiliza token, red y rate limit. | Sin cambio. |
| **D2** | **Cambia a rotativo:** QR/PIN rotan cada 60 s (se rechazó la recomendación (a) del borrador). | Ver abajo: rotación en servidor, SSE dirigido, tolerancia del token anterior. |
| **D3** | **(a)** Matrícula activa. | Sin cambio (`MATRICULA_QR / ESTADO_MATRICULA`). |
| **IAM-001** | **No se espera.** Se usa el patrón de HU050 con la deuda **TD-015** aceptada; se ajusta lo necesario para que el registro y la matrícula por QR queden completos. | Documentar TD-015 en el cierre; migrar a `SincronizarCuentaInstitucionalService` cuando IAM-001 cierre. |
| **D4** | **(a)** Aceptada: QR generado en el backend con ZXing. | Sin cambio. |
| **D5** | **(a)** Aceptada: `X-Forwarded-For` solo desde proxies confiables. | Sin cambio. |
| **D6** | **(a)** Aceptada: se retira `usp_registrar_estudiante_en_grupo_autonomo` y su test. | Sin cambio. |
| **D7** | **(a)** Aceptada: rate limit en memoria (20 intentos/min por IP, 5 registros/min por token). | Sin cambio. |
| **D8** | **(b)** Aceptada: se reutiliza el formulario de `register` dentro de `MatriculaQrComponent` y se retira la ruta `register`. | `POST /usuarios` se retira al cerrar el WI si nadie lo usa. |
| **D9** | **(a)** Aceptada: interruptor `asistencias:matricula-qr:habilitada`. | Sin cambio. |
| **Duración máxima** | **Variable global administrable**, no un valor fijo: `DURACION_MAXIMA_MINUTOS` se lee del catálogo de parámetros (`ParameterCatalogPort`) y el administrador puede cambiarla sin redeploy. Valor inicial de siembra: 60. | Ver nota de límites abajo. |

> **Límite conocido de la variable administrable:** `AdminPortalController` ya lista los parámetros (`GET /admin/parametros`, pestaña "Parámetros Globales" de `admin-system`), pero `PUT /admin/parametros/{id}` responde 501. Hasta que exista esa edición, el valor se cambia con un `UPDATE` controlado sobre el parámetro de la DB (`MATRICULA_QR` / `DURACION_MAXIMA_MINUTOS`), que los SP leen en cada operación y que ya aparece en la pestaña de parámetros. Implementar el `PUT` queda **fuera de este WI** y se registra como deuda. El mismo criterio aplica a `ROTACION_SEGUNDOS` y `TOLERANCIA_SEGUNDOS`. *(Corregido por el addendum A1 del CONTRACT: la versión anterior de esta nota mencionaba App Configuration.)*
| **Tolerancia** | Se acepta el token **vigente y el anterior**. | `TOLERANCIA_SEGUNDOS` (60) parametrizable. |

**Consecuencias de D2 = rotativo:**

1. **Parámetros nuevos** (reemplazan a `TTL_MINUTOS`), todos administrables: `ROTACION_SEGUNDOS` = 60, `TOLERANCIA_SEGUNDOS` = 60, `DURACION_MAXIMA_MINUTOS` = 60 (siembra inicial).
2. **DB:** la fila de la ventana guarda token y PIN vigentes y anteriores (`tokenHash`, `pin`, `tokenHashAnterior`, `pinAnterior`, `fechaRotacionUtc`). Nuevo SP `usp_rotar_token_matricula_grupo` y consulta `usp_consultar_ventanas_matricula_por_rotar`.
3. **Quién rota:** un job del **servidor** (`RotacionTokenMatriculaScheduler`, `@Scheduled`) rota las ventanas vencidas. No es polling del cliente. Es idempotente (el SP revalida la condición), por lo que dos instancias no rotan doble.
4. **Realtime:** el `ReactorRealtimeAdapter` hoy es broadcast. Se amplía con **entrega dirigida a un usuario** (`publishToUser`), para que el token solo llegue a la pantalla del docente emisor. Esto resuelve también el riesgo R6 (el roster deja de recibir eventos ajenos).
5. **Frontend:** el modal del docente escucha `MATRICULA_QR_ROTADO` por `RealtimeService` y reemplaza el QR. Sin `setInterval`. Si el SSE se reconecta, el modal llama a `POST /grupos/{id}/matricula-qr/rotacion` para obtener un QR fresco (el token en claro no se guarda, solo su hash).
6. **Efecto en el estudiante:** un token escaneado vale entre 60 s y 120 s según cuándo se escanee. El formulario de registro debe completarse en ese lapso. Mitigación: ajustar `TOLERANCIA_SEGUNDOS`, y mensaje `MQR_001` que pide escanear de nuevo.

---

## 9. Riesgos y dependencias

| ID | Riesgo | Mitigación |
|---|---|---|
| R1 | Superficie pública nueva: abuso, enumeración o creación masiva de cuentas | Token de 256 bits guardado como hash; PIN válido solo con una ventana activa y desde el campus; rate limit; cupo; mensajes genéricos; interruptor (D9); log y métrica de rechazos |
| R2 | Rangos de red mal configurados que bloquean a todos los estudiantes, o los dejan pasar a todos | Sin rangos configurados se **cierra por defecto** (fail-closed: 403 y log `ERROR`). Test de configuración. Los rangos de desarrollo están separados de los de producción |
| R3 | El PIN de 6 dígitos colisiona entre ventanas activas | Unicidad del PIN entre las ventanas activas, garantizada en el SP (`UPDLOCK, HOLDLOCK`), con reintento de generación en el backend (máximo 5) |
| R4 | Keycloak cae después del commit | Igual que en HU050: error controlado y desfase (IAM-001). Reintentar con el mismo token: el modo existente detecta al estudiante y la reconciliación crea la cuenta |
| R5 | El token viaja en la URL (`?t=`) y puede quedar en el historial del navegador | TTL corto; el token no permite leer datos sensibles por sí solo y exige la red del campus. Se descarta tras el registro (el front lo borra de la URL con `history.replaceState`). Alternativa: solo PIN (D2) |
| R6 | Broadcast SSE sin destinatario | **Resuelto con D2:** entrega dirigida al docente emisor (`publishToUser`). El payload de matrícula sigue sin PII; el roster vuelve a pedir los datos con ámbito |
| R8 | **(D2)** El token en claro viaja por SSE al docente y por la pantalla proyectada | Entrega dirigida al emisor; el token solo vive 60 s + tolerancia; nunca se loguea; las pruebas verifican que otro usuario conectado no recibe `MATRICULA_QR_ROTADO` |
| R9 | **(D2)** El estudiante no alcanza a completar el registro antes de que expire el token | Tolerancia parametrizable (`TOLERANCIA_SEGUNDOS`), mensaje claro y reescaneo. Se mide `matricula_qr_total{resultado=token_invalido}` en la validación manual |
| R10 | **(D2)** El job de rotación falla o se detiene: el QR proyectado deja de ser válido | El token anterior sigue válido solo durante la tolerancia. Log `ERROR` y métrica `matricula_qr_rotacion_total{resultado}`; el docente puede regenerar manualmente |
| R11 | **(D2)** Cambio en el módulo realtime (sensible, compartido con el resto) | Cambio aditivo (`publishToUser`), sin alterar `publish`; pruebas de no regresión del broadcast; revisión con la skill `uco-realtime` |
| R7 | Estudiante que ya existe con otro rol (p. ej. un docente) | El SP en modo público detecta el documento o correo existente → `MQR_005`, y pide iniciar sesión con el flujo autenticado, que exige el rol ESTUDIANTE. La regla queda acordada en el CONTRACT §2.4 |

**Dependencias:**
- HU050 ✔ (reglas de matrícula).
- IAM-001 (recomendado: servicio de sincronización y `POST /usuarios` restringido).
- Rangos de red del campus (dato de Infra).

---

## 10. Test plan

Ver [TEST_PLAN.md](TEST_PLAN.md).

---

## 11. Secuencia de ejecución (fases 02–06)

| Fase | Repo | Rama | Commits atómicos (`Refs: HU176B`) |
|---|---|---|---|
| 02 | Backend (docs) | `docs/plan-registro-roles` | `docs(hus): congelar contrato HU176B matricula qr autonoma` |
| 03/04 DB | GestioAsistenciaDB | `feature/HU176B-matricula-qr` | `test(db/sp): …`; `feat(db/tables): agregar TokenMatriculaGrupo`; `feat(db/sp): agregar apertura y cierre de ventana de matricula`; `feat(db/sp): agregar matricula transaccional por token`; `refactor(db/sp): retirar usp_registrar_estudiante_en_grupo_autonomo` |
| 03/04 Back | GestioAsistenciaBackend | idem | `test(backend/…): …`; `chore(deps): agregar zxing para generar qr en backend`; `feat(backend/ports): …`; `feat(backend/adapter): validar red institucional por cidr`; `feat(backend/adapter): generar token y qr de matricula`; `feat(backend/usecase): …`; `feat(backend/controller): exponer matricula qr publica y de docente`; ⚠️ `feat(backend/security): habilitar endpoints publicos de matricula con limite de intentos` |
| 03/04 Front | GestioAsistenciaFrontend | idem | `test(frontend/…): …`; `feat(frontend/estudiante): agregar ruta publica de matricula por qr`; `feat(frontend/docente): proyectar qr de matricula generado por backend`; `feat(frontend/realtime): refrescar roster con ESTUDIANTE_MATRICULADO`; `refactor(frontend/auth): retirar ruta register` (D8) |
| 05 | todos | — | `VALIDATION.md`, incluida la prueba desde un dispositivo en la red local |
| 06 | docs + Infra | — | `HTTP_AS_IS_MATRIX.md`, `HU-CATALOG.md` (HU176B; HU175 movida de programa), parámetros documentados en Infra |

---

## 12. Rollback y stop conditions

- **Rollback:** interruptor D9 (inmediato), luego reversión de los PRs y la migración `down`.
- **CONTRACT_CONFLICT:** aparece un consumidor del SP autónomo o de `matricular-grupo` con el body antiguo.
- **TEST_CONTRACT_CONFLICT:** documentar cualquier ajuste del spec de `teacher-matricula-modal` que asierte el "PIN = id".
- **BLOCKED_BY_MISSING_EVIDENCE:** rangos de red de producción; aprobación de seguridad para los endpoints públicos.

---

## 13. Deuda conocida y validación manual

- **Cierra:** QR mediante un tercero, PIN = id del grupo, SP autónomo inseguro, `matricular-grupo` en 501.
- **Registra:**
  - el literal `'A'` por defecto en `usp_registrar_estudiante_en_grupo_interno` (l.46), pendiente de resolver desde el catálogo cuando un grupo toque HU050 (J);
  - el rate limit no distribuido (D7);
  - la falta de captcha.
- **Validación manual:**
  1. El docente `maria.rostagno` abre una ventana y proyecta el QR.
  2. Desde un celular en la misma LAN (rango de desarrollo), escanear y crear la cuenta.
  3. Login del estudiante nuevo.
  4. El roster del docente se actualiza solo (SSE).
  5. Cerrar la ventana y volver a escanear: rechazo.
  6. Desde una IP fuera de rango (simulada): 403.
  7. 21 intentos en un minuto: 429.

---

## 14. Definition of Ready (DoR)

| Criterio DoR | Estado | Evidencia / pendiente |
|---|---|---|
| 1. Objetivo y alcance delimitados | ✅ | §1, §4 (con el cambio de alcance de HU175) |
| 2. AS-IS con evidencia | ✅ | §2 (verificado el 2026-10-21) |
| 3. Contratos identificados | ✅ (borrador) | [CONTRACT.md](CONTRACT.md) |
| 4. Matriz de pruebas | ✅ | [TEST_PLAN.md](TEST_PLAN.md) |
| 5. Sin conflictos WORM | ✅ | — |
| 6. Decisiones del owner | ✅ | D1–D9, IAM-001 y duración máxima resueltas el 2026-11-04 (§8.1) |
| 7. Aprobación de seguridad (endpoints públicos) | ❌ | Owner de seguridad |
| 8. Rangos de red del campus | ❌ (solo producción) | Infra |
| 9. Archivos calientes liberados | ✅ | HU035/HU036 figuran `closed` en `docs/work-items/INDEX.md` (2026-11-04) |

**Dictamen DoR:** 👉 **`NOT_READY`**.

> **Nota IDENT-001 (2026-10-31): antes de congelar el CONTRACT de esta HU, el alta autónoma (CA-03) debe reutilizar `CorreoInstitucionalPolicy` (dominio institucional) y el aviso `VERIFICACION_CORREO` de [IDENT-001](../IDENT-001-correo-institucional-verificacion/CONTRACT.md); el punto «Captcha y verificación del correo (TD-014)» de «No alcance» queda parcialmente cubierto (TD-082).
