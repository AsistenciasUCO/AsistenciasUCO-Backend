---
status: blocked-with-findings
type: validation
scope: database-backend
owner: fullstack-team
last-reviewed: 2026-11-01
---

# VALIDATION — IDENT-001: Correo institucional y verificación del correo (registro manual del estudiante)

## Dictamen

**VALIDADO EN LOCAL para base de datos y backend** (sin push, PR ni merge con `develop`). El frontend queda **pendiente** (TD-081): el repositorio no estaba disponible en este workspace. Todas las cifras se midieron en la corrida indicada; ninguna se tomó de otra.

Alcance validado (HU050, registro manual por el docente): el correo del estudiante nuevo debe terminar en el dominio configurado (`REGISTRO/DOMINIO_CORREO_ESTUDIANTE`, hoy `@uco.edu.co`), validado en el procedimiento almacenado, en el backend y expuesto al frontend por un endpoint; el estudiante recibe un enlace de un solo uso, su cuenta nace deshabilitada y se habilita al verificar. La contraseña se asigna como hasta ahora (TD-079).

## Comandos ejecutados

| Fecha / entorno | Comando | Resultado | Cifras |
|---|---|---|---|
| 2026-10-31, BD Docker local recreada desde el esquema del repo | `deploy_schema.ps1` + `test_summary.ps1` | **DB GATE PASS** | `TOTAL_EXECUTED=377`, `PASSED=376`, `FAILED=0`, `SKIPPED=1` (permitida), `SQL_ERROR_COUNT=0`, `@@TRANCOUNT=0` |
| 2026-10-31, Java 25 | `mvnw.cmd test jacoco:report` | **PASS** | `Tests run: 2944, Failures: 0, Errors: 0, Skipped: 0`; global LINE 93,68 % / BRANCH 84,14 % |
| 2026-10-31, BD local, entorno cargado sin BOM | `mvnw.cmd -Pintegration failsafe:integration-test failsafe:verify` | **PASS** | `Tests run: 265, Failures: 0, Errors: 0, Skipped: 5`; `VerificacionCorreoGreenMailIT` 7/7, `NotificacionSmtpGreenMailIT` 11/11, `NotificacionOutboxIT` 9/9 |
| 2026-10-31, backend en `:8090` + Keycloak real + sumidero SMTP | `scratch/test_ident001.ps1` | **PASS** | 23/23 (ver E2E) |

### Línea base anterior y diferencias
| Capa | Antes (NOTIF-002A) | Ahora | Diferencia |
|---|---|---|---|
| Gate de BD | 376 ejecutadas, 375 pasadas, 1 omitida | 377 / 376 / 1 | +1 archivo de prueba nuevo (`test_ident001_correo_verificacion.sql`) |
| Unitarias | 2.864 | 2.944 | +80 (política, verificación, reenvío, configuración, Identity, canal SMTP, renderer, controladores, filtro, helpers) |
| Integración | 258 ejecutadas, 7 omitidas | 265 ejecutadas, 5 omitidas | +7 (`VerificacionCorreoGreenMailIT`); 2 omitidas menos porque la BD local ahora tiene los datos que esas pruebas necesitan |

Cobertura de las clases nuevas del backend: 100 % de líneas y ramas (`CorreoInstitucionalPolicy`, `VerificarCorreoUseCaseImpl`, `ReenviarVerificacionCorreoUseCaseImpl`, `ConsultarConfiguracionRegistroEstudiante*`, `VerificacionCorreoMarcadorProvider`, `TokenVerificacionCorreoJpaRepository`, interactors).

## E2E (puerto 8090)

**Declaración:** ejecutado con `APP_ADAPTERS_SECURITY_PROVIDER=legacy` y `APP_ADAPTERS_IDENTITY_PROVIDER=legacy`, catálogos de parámetros y mensajes en `sqlserver`, JWT reales del Keycloak local (realm `asistencias-uco`, docente `maria.rostagno@uco.edu.co`) y provisionamiento real de cuentas en Keycloak con el service account `asistencias-backend-admin`. **No se probó contra el PDP de seguridad baseline ni contra Azure App Configuration.** El SMTP fue un sumidero local basado en GreenMail (`scratch/GreenMailSink.java`) en `127.0.0.1:3025`; no se envió ningún correo real (el usuario y la contraseña de Gmail se anularon para ese proceso). El usuario de prueba se eliminó de la BD y de Keycloak al terminar.

| ID | Resultado | Evidencia |
|---|---|---|
| E2E-01 | PASS | `GET /configuracion/registro-estudiante` → 200 con `@uco.edu.co`; sin token → 401 |
| E2E-02 | PASS | Correo `@gmail.com` → 400 `VCE_001` con «…terminar en @uco.edu.co.»; 0 filas creadas |
| E2E-03 | PASS | Registro institucional → 201; `correoConfirmado=0`; la cuenta de Keycloak responde `Account disabled` |
| E2E-04 | PASS | Llega 1 correo con asunto «Verifique su correo institucional», el nombre y el enlace `…/verificar-correo#token=<43 caracteres>`; el outbox no contiene el token ni el correo; 1 token activo en BD |
| E2E-05 | PASS | Reenvío inmediato → 409 `VCE_003` |
| E2E-06 | PASS | Reenvío tras la espera → 200; llega un segundo correo con un token distinto |
| E2E-07 | PASS | Token anterior (revocado) → 400 `VCE_002`; token inexistente y token de formato inválido responden **cuerpos idénticos** |
| E2E-08 | PASS | Verificar con el token vigente → 200 «Su correo fue verificado. Ya puede iniciar sesión.»; `correoConfirmado=1`; Keycloak pasa a `Account is not fully set up` (cuenta habilitada, contraseña temporal) |
| E2E-09 | PASS | Reutilizar el token → 400; reenviar a un correo verificado → 409 `VCE_005` |
| E2E-10 | PASS | 25 intentos seguidos al endpoint público → aparecen 429 (límite por IP) |
| E2E-11 | PASS | Limpieza de Keycloak y BD |

## Tests existentes modificados (para el PR)

Cambios por contrato formal (PLAN §4 / CONTRACT §6 y §9). Ninguno relaja una aserción ni elimina un caso.

| Archivo | Cambio | Justificación |
|---|---|---|
| `test_usp_registrar_estudiante_en_grupo_usuario_no_existente.sql` (DB) | El correo de prueba usa el dominio del parámetro en lugar de `@test.local` | El alta nueva exige correo institucional |
| `test_transaction_ownership.sql` (DB) | Ídem para `ownership.estudiante` | Ídem |
| `test_notif002a_plantillas_contacto.sql` (DB) | Los conteos «exactamente 4 plantillas» y «9 marcadores» se acotan a los 4 tipos originales | Entra el tipo `VERIFICACION_CORREO`; la aserción sigue exigiendo exactamente 4 y 9 de los tipos que verifica |
| `test_contract_closure.sql` (DB) | Dos SP de consulta (`usp_consultar_token_verificacion_correo`, `usp_consultar_reenvio_verificacion_correo`) entran a la lista de resultsets de lectura no canónicos | Devuelven `idUsuario`; el resultset canónico de 4 columnas es solo para comandos |
| `test_suite.sql` (DB) | Registro de `test_ident001_correo_verificacion.sql` | Prueba nueva |
| `RegistrarEstudianteUseCaseImplTest` | Las 11 construcciones pasan por un helper que arma el caso de uso con la política y el puerto de aviso | El constructor gana dos colaboradores; las aserciones no cambian |
| `GrupoWiringConfigurationTest` | La llamada al bean pasa los colaboradores nuevos | Cambio de firma del bean |
| `JpaRepositoryBeanUniquenessTest` | `EXPECTED_REPOSITORIES` 50 → 51 | Repositorio nuevo (`VerificacionCorreoJpaRepository`), como en cada work item anterior |
| `SesionGrupoCommandsSpParityIT` (GRP_04) | El correo usa el dominio configurado (leído de BD) en lugar de `@example.test` | El alta nueva exige correo institucional |

## Hallazgos durante la implementación

| Hallazgo | Resolución |
|---|---|
| Los nombres de savepoint de SQL Server admiten 32 caracteres | Nombres cortos `sp_ident_*` |
| `INSERT … EXEC` no admite `ROLLBACK` dentro del SP | `usp_confirmar_verificacion_correo` no escribe al rechazar, así que no necesita deshacer nada |
| AR-07 define el módulo de correo como `template`, `contact` y `channel.smtp` | Las clases del canal se ubicaron en `template`; la regla no se relajó |
| `StorageProviderIsolationRulesTest` rechaza nombres con «minio» y «Dominio» lo contiene | Renombrado a `CorreoInstitucionalPolicy` |
| El catálogo Java usa `MessageFormat` (`{0}`) y el de la BD `{}`: `VCE_001` salía con el marcador sin reemplazar | La política sustituye `{}`; el manejador global nunca muestra un marcador sin resolver y prefiere el mensaje de la excepción |
| `DbExceptionTranslator` usaba el mensaje genérico de validación para `VCE_002` | Códigos de error propios (`VerificacionCorreoErrorCode`) |
| GreenMail guarda una copia por buzón (To y BCC) y reordena su arreglo | La IT afirma «≥ 1»; el sumidero del E2E deduplica por `Message-ID` |
| El realm de Keycloak exige contraseñas de **12 o más caracteres** y el backend acepta 8: un alta con una clave corta termina en 500 tras confirmar la BD | Preexistente (TD-013/TD-015); el E2E usa una clave de 16. Conviene alinear la política del backend con la del realm |
| El proveedor de catálogos por defecto es Azure App Configuration | Registrado como TD-077 |

## Entorno de ejecución (documentado)
`JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp -Djdk.net.hosts.file=C:\jtmp\hosts` (la ruta temporal con espacio rompe los sockets del JDK 25 y el DNS inverso del equipo tarda ~5 s); `.env` cargado solo en el proceso; claves de MinIO de Infra; la BD debe estar recreada desde el esquema del repositorio (sin SP obsoletos) y con los datos de prueba que piden las pruebas de paridad.

## Pendientes y límites
- **Frontend (TD-081):** implementado y probado con pruebas unitarias (1359/1359) en el repositorio del frontend (commits `bb3651c`, `f954eb9`, sin push). Pendiente: prueba en navegador real contra backend y Keycloak, y su E2E.
- **Contraseña** definida por el docente (TD-079); **purga** de cuentas nunca verificadas (TD-078); **doble fuente** del dominio con Azure (TD-077); límite de intentos por instancia (TD-080); **HU176B** debe reutilizar el mecanismo (TD-082).
- **SMTP institucional y SPF/DKIM/DMARC** (TD-073/TD-074): ahora condicionan el registro, porque sin entrega del correo el estudiante no puede activar su cuenta.
- El correo de verificación sale por el módulo de NOTIF (no por Keycloak), así que el modo de pruebas con redirección sí lo cubre.

## Addendum — Security Baseline / primera autenticación (2026-11-01)

### Dictamen

**`BLOCKED_WITH_FINDINGS`**. El E2E de Security Baseline no se cierra y no se declara `READY_FOR_PR` ni `READY_FOR_MANUAL_TESTING_FULL`: la última evidencia autenticada disponible deja al estudiante sin un inicio de sesión utilizable, y esta sesión no modificó la cuenta conocida ni cambió su contraseña. La conectividad de servicio se confirmó desde PowerShell del host; eso no sustituye el login ni una decisión PEP/PDP con identidad válida.

### Precheck Git y separación

- Backend: rama `fix/ident001-confirmar-correo-baseline-20261101`, HEAD `b288ca51`; el único diff de código de este fix son los dos archivos Java señalados abajo. También permanecen modificaciones previas de launcher/README/GreenMail y artefactos locales sin seguimiento; no se mezclaron ni se alteraron. `.codex/` y la evidencia FIX-003 se preservaron.
- Frontend: `develop` (`350bf12`), limpio antes/después de `npm run verify`.
- DB: `develop` (`d1e0af8`), limpio; no se ejecutó SQL.
- No hay archivos staged ni commit, push o merge.

### Conectividad local (PowerShell del host)

El proceso ordinario de Codex no ve listeners en `localhost`; PowerShell del host sí alcanzó los servicios. Sondas HTTP de solo lectura: Backend `8080/actuator/health` **200**; Frontend `4200/` **200**; metadatos OIDC de `security-baseline` en Keycloak `19090` **200**; `18080/health` PDP **401** y `18081/health` PEP **401** sin credenciales. `docker inspect` mostró Keycloak y PDP `running/healthy`, PEP `running`. El 401 anónimo demuestra que PDP/PEP respondieron, no que se haya probado una decisión de acceso. La primera ruta OIDC consultada fue un realm incorrecto (404); se corrigió a `security-baseline`. No se consultó SQL Server ni se enviaron credenciales de cuentas.

### Causa raíz y diff del 500

`ProfileSyncingIdentityProvider` decoraba `IdentityProviderPort` pero no implementaba `confirmarCorreoCuenta`. La llamada heredaba el `default` del puerto, que lanza `IdentityProviderException`; por eso `POST /api/v1/public/verificar-correo` terminaba en 500 en el wiring de Security Baseline. El diff exacto es `ProfileSyncingIdentityProvider.java` (+5 líneas) y `ProfileSyncingIdentityProviderTest.java` (+8 líneas); no cambia el puerto, `KeycloakIdentityProviderAdapter`, contrato HTTP ni roles.

```java
@Override
public void confirmarCorreoCuenta(final String idExterno) {
    delegate.confirmarCorreoCuenta(idExterno);
}
```

La prueba llama `provider.confirmarCorreoCuenta("kc-1")`, verifica `delegate.confirmarCorreoCuenta("kc-1")` y comprueba `never()` sobre `profiles.sincronizar(...)`.

Se auditaron los demás métodos del puerto: el decorador delega creación/eliminación, asignación/retiro de rol, búsqueda, habilitar/deshabilitar, actualización de datos, consulta de correo y roles, además de confirmar correo. La sincronización de perfiles sigue limitada a creación/asignación/retiro según su selector existente.

- **RED:** con la implementación de producción temporalmente restituida desde `HEAD`, `mvnw.cmd -Dtest=ProfileSyncingIdentityProviderTest test` falló en la nueva expectativa, como se esperaba. El archivo de producción se restauró byte por byte antes de continuar.
- **GREEN:** el mismo test focalizado pasó **11/11**, 0 fallos/errores/skips.
- `git diff --check`: PASS.

### Primer acceso — transiciones A–I

La siguiente tabla distingue el último reporte previo de una ejecución actual. La evidencia de instalación original está en `docs/work-items/INTEGRACION-ORIGINAL-20261101/VALIDATION.md`; el E2E IDENT-001 de 2026-10-31 usó `legacy` y no certifica Security Baseline. La sesión actual confirmó conectividad pública pero no autenticó ni cambió la cuenta de prueba ya existente; las transiciones que dependen de esa cuenta permanecen `NOT_RUN` y el E2E total no es PASS.

| Transición | Evidencia previa disponible | Estado en esta ejecución |
|---|---|---|
| Registro inicial: cuenta deshabilitada, correo GreenMail y token de un solo uso | El E2E IDENT-001 en modo legacy documenta esos pasos; no certifica Security Baseline. | `NOT_RUN` |
| A. Cuenta Keycloak `enabled` | El reporte IDENT-001 previo documenta habilitación al confirmar el enlace. | `NOT_RUN` |
| B. `emailVerified` | El mismo reporte documenta confirmación del correo; no se volvió a consultar Keycloak. | `NOT_RUN` |
| C. Required Actions (`UPDATE_PASSWORD`) | El reporte E2E previo informa que Keycloak exige el cambio inicial y que el flujo no termina el setup. La cuenta no se consultó en esta sesión. | `FAIL` previo / `NOT_RUN` actual |
| D. Retorno OIDC/PKCE, Bearer y `GET /api/v1/auth/me` | El reporte previo registra `auth/me → 401` después del intento fallido de login. Esta llamada requiere Bearer, no la cookie BFF del PDP; no se repitió con un token válido. | `FAIL` previo / `NOT_RUN` actual |
| E. Reconocimiento de persona en PDP | El reporte previo informa que la persona nueva no aparece en PDP. El cliente backend busca por correo y si falta devuelve `PENDIENTE_PERSONA_NO_REGISTRADA`; no crea la persona. | `FAIL` previo / `NOT_RUN` actual |
| F. Perfil institucional `ESTUDIANTE` en PDP | No se obtuvo una sesión reconocida ni se consultó el perfil del usuario de prueba. | `NOT_RUN` |
| G. Client role institucional en Keycloak | El código de provisioning asigna el rol institucional, pero no se leyó la asignación real de la cuenta. | `NOT_RUN` |
| H. Decisiones reales PEP/PDP, endpoint permitido a `ESTUDIANTE`, denegación a otros roles y 401 legítimos/ilegítimos | No se enviaron solicitudes al PEP/PDP con una sesión autenticada. | `NOT_RUN` |
| I. Angular después de login, logout y acceso posterior | No hubo login exitoso con el cual validar navegación, acceso o cierre de sesión. | `NOT_RUN` |

La primera transición fallida según el E2E previo es la acción Keycloak `UPDATE_PASSWORD`: el navegador no obtiene una sesión/token utilizable para continuar; el `401` previo de `auth/me` corresponde a autenticación no establecida y no demuestra una denegación `403` del PDP. A continuación existe un bloqueo independiente de perfil: el backend deja la cuenta pendiente cuando no encuentra a la persona en PDP. No hay evidencia actual de que el PDP haya evaluado una solicitud autenticada ni de que la ausencia de perfil produjera un `403`; autorización y Angular permanecen `NOT_RUN`.

### Conflicto contractual y permisos

`temporary=true` es deliberado, no un valor accidental: el commit `9b7d6d62` cambió `false` a `true` y renombró el test a “password temporal con cambio obligatorio”; `MANUAL_TEST_GUIDE.md` documenta que eso genera `UPDATE_PASSWORD` y bloquea el login directo. Sin embargo, el commit no registra una decisión contractual que justifique el cambio y `docs/security/keycloak-identity-provider.md` §3.3, fuente normativa vigente, exige `temporary:false` al crear cuenta nueva. Se mantiene `CONTRACT_CONFLICT`; no se cambió el flag.

El contrato backend documenta API `stateless`, PKCE y Bearer: la cookie `SECURITY_BASELINE_SESSION` del BFF del PDP no autentica al backend. Angular vuelve por OIDC/PKCE y manda Bearer a `/api/v1/auth/me`; sin Bearer válido el contrato espera 401. Un 403 sería una decisión PEP/PDP posterior a autenticar. No hay contrato aprobado en estos repositorios que describa la acción requerida `UPDATE_PASSWORD`, el retorno OIDC tras cambiar la clave y la emisión del token que usa la SPA. Los owners de identidad y Security Baseline deben congelar ese flujo antes de cambiar el flag o sus pruebas.

También queda `BLOCKED_BY_MISSING_EVIDENCE`: no se encontró contrato aprobado del PDP/BFF que describa cómo completa `UPDATE_PASSWORD` durante el login, cuándo emite la sesión BFF y cómo reconoce el perfil institucional nuevo. Se buscó en `docs/security/runtime-security-provider-architecture.md`, `infra/security-baseline/README.md` y el login baseline de `src/app/core/services/auth.service.ts` del Frontend. El owner de Security Baseline/PDP debe aportar el contrato y la prueba operacional correspondiente.

El perfil PDP sigue sin sincronizarse automáticamente. `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED` permanece deshabilitado. `SecurityBaselinePdpProfileClient` consulta usuarios/asignaciones; ante persona ausente devuelve `PENDIENTE_PERSONA_NO_REGISTRADA` sin crearla. El código espera que el componente PDP registre la identidad en su primer acceso, pero ese comportamiento externo no se comprobó en vivo. La reconciliación backend es una operación explícita y auditada con alcance institucional; no se ejecutó y también requiere que la persona exista en PDP. Sigue activo `PDP_ADMIN_API_AUTHORIZATION_FINDING.md`: habilitar escrituras requiere decisión de Seguridad UCO sobre identidad técnica y permisos efectivos. No se elevó el service account, no se activó la bandera ni se hicieron asignaciones manuales.

### Calidad por repositorio

| Alcance | Resultado |
|---|---|
| Backend `mvnw.cmd test jacoco:report jacoco:check@check` desde PowerShell del host | `PASS`, exit 0: **2.988**, 0 fallos/errores/skips; 26 suites ArchUnit incluidas. JaCoCo LINE **92,31 %**, BRANCH **83,79 %**; gates 80/70 cumplidos. El JAR original conservó el mismo SHA-256; no hubo package ni parada del Backend. |
| Backend `mvnw.cmd verify` en ruta normal, evidencia previa | `FAIL` de build al ejecutar `spring-boot:repackage` renombrando el JAR generado; las pruebas y ArchUnit anteriores terminaron 2.988/0/0/0. La causa exacta del bloqueo del archivo no está demostrada. |
| Maven `verify` de diagnóstico sin elevación | `FAIL` no representativo: 130 errores en tests de servidores HTTP simulados; 114 incluyen `Permission denied` al abrir sockets desde el sandbox. En PowerShell del host, la suite completa anterior terminó sin esos errores. |
| Maven `package -DskipTests=true` en `target/ident001-package-probe` | `PASS` de diagnóstico: `spring-boot:repackage` produjo un JAR con `BOOT-INF`; el SHA-256 del artefacto original no cambió. Confirma que el goal funciona en una ruta libre; el fallo en `target/` es compatible con un bloqueo de archivo por el Backend activo, pero el handle/causa exacta no se verificó. No equivale a un `verify` empaquetado en la ruta normal. |
| Intento `verify` con ruta de salida temporal no estándar | Cancelado al detectar falsos positivos de ArchUnit sobre `@RestController` anidados en clases de test, porque el importador no excluyó el test-output no estándar. No se considera resultado de código ni se modificó la regla. POM temporal retirado; JAR original intacto. |
| Frontend, pruebas focalizadas de autenticación/login/verificación | `PASS`: **121/121**. |
| Frontend `npm run verify` | `PASS`: Karma **1356/1356**, check de cobertura realtime y build de producción completados (exit 0). Repositorio sin diff tras la validación. |
| DB / fixture HU051 | `NOT_RUN` en esta continuación, separado de IDENT-001; no se abrió conexión ni se ejecutó SQL contra la base original. |
| E2E Security Baseline, respuestas 401/403 y token válido | `NOT_RUN`: las sondas confirmaron disponibilidad, pero no se usó una cuenta autenticada; 401 de `/health` anónimo no es una decisión de endpoint estudiante ni una prueba de token legítimo/ilegítimo. |
| Acceso Angular después de autenticación, logout y acceso posterior | `NOT_RUN`: la raíz del frontend devuelve 200, pero no hubo sesión de estudiante para certificar guards, navegación ni logout. |

La evidencia de integración anterior atribuye el fallo del fixture HU051 a dependencia de datos sembrados/orden de seeds en un despliegue limpio de `develop`, mientras el test pasa en una copia migrada. El repositorio DB permanece limpio en `develop`; no se alteró. Propuesta separada para el owner DB: fixture determinista y autocontenido (o seed explícito independiente del orden) dentro de la transacción con rollback, y reproducción únicamente en una base desechable identificada. No se cambiaron fixtures ni datos.

### Riesgos residuales y preservación

- El estudiante sigue sin autenticación certificada; la cuenta no se alteró y no se repitió el cambio de contraseña.
- El conflicto `temporary` y la transición BFF `UPDATE_PASSWORD` requieren decisión contractual.
- El provisioning PDP requiere aprobación explícita de Seguridad UCO; no activar la bandera ni ampliar permisos antes de ella.
- El bloqueo del repackage en `target/` es compatible con el JAR activo en uso, pero no se identificó el proceso que deniega el rename.
- El fixture HU051 sigue pendiente del owner DB y separado del fix IDENT-001.
- No se modificaron usuarios, contraseñas, matrículas, volúmenes Docker ni datos SQL. No se hizo commit, push ni merge.

**Dictamen final:** `BLOCKED_WITH_FINDINGS`.

## Incidente: pantalla blanca Angular después del login institucional — 2026-11-02

### Observaciones host y Backend

- PowerShell del host alcanzó localhost. En la comprobación final aceptaron TCP los puertos 8080, 4200, 19090, 18080 y 18081. El PID 5876 siguió siendo java; inicio local 2026-11-01 22:38:14. No se detuvo ni reinició ningún servicio.
- Con curl sin proxy, GET /actuator/health devolvió 200 en 17 ms. GET /actuator/info terminó sin cabeceras ni bytes: curl status 000, exit 28, timeout 12 s. GET /api/v1/auth/me sin Authorization terminó sin cabeceras ni bytes: status 000, exit 28, timeout 10 s. 000 significa que no se observó respuesta HTTP; no equivale a 401 ni 403.
- La configuración expone health,prometheus, no info. La ruta /actuator/** es pública y PEP-exempt. /api/v1/** requiere autenticación. La cadena debe responder 401 para /auth/me anónimo mediante ApiAuthenticationEntryPoint.
- SecurityPepEnforcementFilter se ejecuta después de BearerTokenAuthenticationFilter y, cuando no hay identidad autenticada, continúa la cadena sin llamar al cliente PEP. Por tanto, la sonda anónima no demuestra ni implica una decisión PDP/PEP.
- CorrelationIdFilter añade el identificador y continúa la cadena. HttpRequestTraceFilter escribe HTTP_REQUEST_COMPLETED en finally; no tener ese evento tras el timeout solo muestra que no hubo finalización observada, no si la petición entró al filtro.
- La muestra de jcmd durante la sonda mostró Acceptor, Poller y VirtualThread-unblocker, sin un hilo servlet de petición identificable. No demuestra saturación ni localiza el bloqueo.
- El JSONL histórico contiene 14 finalizaciones previas de /api/v1/auth/me: 7 con 200 y 7 con 500. No corresponden a la sonda actual y no localizan el componente. No se encontró el archivo convencional de log activo.
- Causa Backend del timeout: no determinada con la evidencia disponible. La conexión TCP se completa y health responde, pero info y /auth/me anónimo no entregan cabeceras. No atribuir a PDP, SQL, Keycloak, hilos virtuales, controller o un filtro concreto. La ausencia de logs al inicio impide separar recepción, inicio de filtros, autenticación, autorización y controller.

### Causa raíz de la pantalla blanca y corrección Frontend

app.config.ts registra AuthService.initKeycloak como APP_INITIALIZER; Angular espera su promesa antes de montar router-outlet. En AuthService.initBaselineSession, sin callback OIDC, se iniciaba prompt=none y se retornaba una promesa que nunca resolvía. En completeBaselineCallback, exchangeCode y fetchInstitutionalIdentity esperaban red y cuerpo JSON sin timeout. Una llamada retenida impedía el bootstrap y dejaba app-root vacío.

El Frontend personal queda con cambios locales en fix/auth-baseline-bootstrap-timeout-20261102:

- baseline-oidc.ts define 8 s por solicitud, aborta con AbortController y aplica Promise.race para que el límite también cubra proveedores que no honren AbortSignal. Cubre respuesta/cuerpo del intercambio authorization_code y conserva PKCE S256.
- AuthService limita la resolución de /auth/me, mantiene credentials omit y Bearer-only, no activa la sesión hasta validar respuesta, UUID y rol institucional. Timeout/red/5xx limpia token, usuario y estado PKCE, quita code/state de la URL y devuelve false para que Angular renderice el login con error.
- La inicialización sin callback ahora se resuelve después de iniciar la redirección silenciosa. Un fallo inmediato se muestra en login. El login BFF elimina estado PKCE silencioso abandonado.
- 401 y 403 de /auth/me se representan por separado. La UI mantiene el botón institucional y cambia su texto a reintento cuando hay error. No se usa password grant, bypass, rol por defecto ni perfil sincronizado automáticamente.
- Se conservan authGuard, roleGuard, los roles reales y la ruta de administrador; un rol ausente o no permitido mantiene la sesión rechazada.

### Validación y estado

| Validación | Resultado |
|---|---|
| Pruebas focalizadas OIDC/AuthService/login previas al último resguardo PKCE | PASS, 126/126 |
| npm run verify posterior al último cambio | PASS, exit 0: Karma 1359/1359; líneas 92,21 %, ramas 75,87 %; coverage:realtime PASS (líneas 95,06 %, ramas 84,52 %); build producción PASS |
| git diff --check Frontend | PASS |
| Endpoints anónimos en host | FAIL observado: /auth/me status 000 tras 10 s sin respuesta. El 401 esperado no se observó. |
| Estado de cuenta Keycloak, emailVerified y UPDATE_PASSWORD | NOT_RUN; no se inició una sesión real ni se alteraron cuentas. |
| Intercambio OIDC real, sesión BFF y auth/me con Bearer | NOT_RUN |
| Persona y perfil institucional en PDP, client role y decisiones autenticadas PEP/PDP | NOT_RUN |
| Navegación autenticada Angular, panel de uco-administrador, logout y acceso posterior | NOT_RUN |
| Datos SQL, usuarios, matrículas, Docker y configuraciones de seguridad | Sin cambios. |

Karma reportó los 1359 tests como SUCCESS. La salida también contiene mensajes de consola NG0205 durante pruebas y una advertencia de un spec legacy sin expectativas; el runner terminó exit 0 y no reportó fallos.

### Riesgos residuales y dictamen

El fix evita la pantalla blanca indefinida y presenta un error con reintento seguro, pero el endpoint Backend /auth/me continúa sin responder desde el host. Sin una respuesta HTTP no se puede certificar autenticación, reconocimiento en PDP, autorización ni el panel. Capturar una traza de entrada o stack/JFR del Backend mientras /auth/me está pendiente es la siguiente acción técnica necesaria; no se reinició el proceso ni se habilitó logging con datos sensibles.

El cambio Frontend y este informe no tienen commit, push ni merge. La rama y cambios locales previos de IDENT-001 en Backend, las 5 cuentas de prueba y las evidencias FIX-003 se preservan.

**Dictamen del incidente: BLOCKED_WITH_FINDINGS.**