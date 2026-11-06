---
status: draft
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# TEST_PLAN — HU176B: Matrícula autónoma por QR/PIN

**Estado:** `RED_PLANNED` (borrador). Las ramas son `feature/HU176B-matricula-qr`, con el orden DB → Backend → Frontend. Los pasos ⚠️ van después del merge de HU035/036.

## Fuentes
- [PLAN](PLAN.md) (CA-01…CA-09, D1–D9) y [CONTRACT](CONTRACT.md).
- Skills: `uco-testing`, `uco-seguridad`, `uco-realtime`, `uco-catalogos`, `uco-observabilidad`.
- Patrones:
  - HU050: `RegistrarEstudianteUseCaseImplTest`, `GrupoControllerWriteEndpointsTest`, `test/test_usp_registrar_estudiante_en_grupo_usuario_no_existente.sql`
  - `RegistrarAsistenciaUseCaseImplTest` (publicación SSE)
  - `RbacSecurityFilterChainTest`, `FakeKeycloakAdminServer`
  - `teacher-matricula-modal.component.spec.ts`

## 1. Matriz de comportamiento

| ID | Requisito | Acción | Resultado esperado | Nivel |
|---|---|---|---|---|
| T-01 | CA-01: abrir ventana | Docente titular | 201 con PIN de 6 dígitos, URL con token, PNG decodificable (el QR leído = URL) y fin de ventana = ahora + `DURACION_MAXIMA_MINUTOS` | DB, UseCase, Controller, IT |
| T-02 | Una ventana activa por grupo | Abrir dos veces | La primera queda `CERRADA`; su token y su PIN se rechazan | DB |
| T-03 | Titularidad | Docente no titular / coordinador | 403 (scope) / 403 (`SecurityConfig`) | DB, Security |
| T-04 | Sin cupo | Grupo lleno | 409 | DB |
| T-05 | CA-02: cerrar | Ventana activa / sin ventana | `CERRADA` / éxito sin cambios | DB, Controller |
| T-06 | Expiración | `fechaExpiracionUtc` vencida (fixture) | `validar` → 404 `MQR_001`; la fila pasa a `EXPIRADA` | DB |
| T-07 | Anti-enumeración | Token inexistente vs. expirado vs. cerrado | Mismo HTTP, mismo mensaje y tiempos comparables (sin rama observable) | DB, Controller |
| T-08 | El token se guarda hasheado | Después de abrir | La DB contiene `SHA-256(token)`; el token en claro no aparece en tablas ni en logs | DB, UseCase, test de logs |
| T-09 | Colisión de PIN | El fake del puerto devuelve `MQR_006` dos veces | El backend reintenta y tiene éxito; al sexto intento, error controlado | UseCase |
| T-10 | CA-03: registro público | Datos válidos desde una IP del campus | Usuario **activo**, estudiante, `EstudianteGrupo` en estado `MATRICULA_QR/ESTADO_MATRICULA`, `EstudiantePrograma`; `crearCuenta(..., ESTUDIANTE)`; evento publicado; 201 sin token de sesión | DB, UseCase, IT, E2E |
| T-11 | Persona ya registrada | Documento o correo existentes | 409 `MQR_005`, sin cambios | DB |
| T-12 | Ya matriculado | Repetir | 409 `MQR_004` | DB |
| T-13 | Reglas de HU050 | Cupo agotado / cruce de horario | Mismos códigos que HU050; 0 filas nuevas | DB |
| T-14 | CA-06: atomicidad | Falla forzada después de crear el usuario | Rollback total, `@@TRANCOUNT=0`, `totalMatriculas` intacto | DB |
| T-15 | Concurrencia por el último cupo | Dos registros simultáneos con el mismo token y cupo 1 | Uno OK y el otro con conflicto de cupo | DB (script de 2 conexiones) |
| T-16 | CA-04: modo autenticado | JWT ESTUDIANTE + PIN | Matrícula sin crear usuario; no se llama a `crearCuenta` | DB, UseCase, Controller |
| T-17 | Modo autenticado con otro rol | JWT DOCENTE | 403 | Security |
| T-18 | CA-05: fuera del campus | IP fuera de los rangos | 403 `MQR_002` en validar, registro y matricular-grupo; el SP **no** se invoca | Controller + adapter |
| T-19 | D5: `X-Forwarded-For` falso | Header con IP del campus desde un `remoteAddr` no confiable | 403 (se usa `remoteAddr`) | Adapter / `ClientIpResolver` |
| T-20 | D5: proxy confiable | `remoteAddr` = proxy y XFF = IP del campus | Se acepta | Adapter |
| T-21 | R2: fail-closed | Sin rangos configurados | 403 + log ERROR | Adapter |
| T-22 | CIDR IPv4/IPv6 | `10.0.0.0/8`, `2001:db8::/32`, bordes del rango | Pertenencia correcta en los bordes | Adapter (parametrizado) |
| T-23 | CA-07: rate limit por IP | 21 solicitudes en 60 s | La 21.ª → 429 `MQR_003`; después de la ventana, vuelve a pasar | Filtro |
| T-24 | Rate limit por token | 6 registros en 60 s con el mismo token | El 6.º → 429 | Filtro |
| T-25 | Memoria acotada | Muchas IPs distintas | El mapa no supera el tamaño máximo (las entradas viejas se desalojan) | Filtro |
| T-26 | D9: interruptor apagado | `habilitada=false` | Los públicos responden 404 y la apertura de ventana 404 | Controller |
| T-27 | CA-08: SSE sin PII | Después del registro | `publish` con `type=ESTUDIANTE_MATRICULADO` y payload exactamente `{idGrupo, origen}` | UseCase |
| T-28 | Una falla de SSE no rompe la matrícula | El publisher no-op o con error interno | Matrícula 201 (el contrato del puerto no lanza) | UseCase |
| T-29 | Keycloak caído | `FakeKeycloakAdminServer` 500 | Error controlado `IAM_001` o desfase (según IAM-001); la matrícula en DB persiste | UseCase, IT |
| T-30 | Validator | Ningún modo o ambos / PIN no numérico / contraseña débil / documento fuera de rango | 400 sin invocar el use case | Validator |
| T-31 | Rutas públicas acotadas | `POST /api/v1/public/otra` sin token | 401 (no hay comodín público) | `RbacSecurityFilterChainTest` ⚠️ |
| T-32 | Reglas DOCENTE antes del comodín de grupos | `POST /grupos/{id}/matricula-qr` con JWT DOCENTE / COORDINADOR | 201-403 según el scope / 403 | `RbacSecurityFilterChainTest` ⚠️ |
| T-33 | Retiro del SP autónomo (D6) | Después de migrar | `OBJECT_ID` NULL; su test y su `:r` se retiran | DB |
| T-34 | Logs sin secretos | Todos los flujos | Ni token, ni PIN, ni contraseña, ni documento, ni correo | UseCase / filtro (captura de salida) |
| T-35 | **D2** rotación en SP | `debeRotar=1` (fixture con `fechaRotacionUtc` antigua) | Token y PIN vigentes pasan a anteriores; los nuevos rigen; `fechaRotacionUtc` se actualiza | DB |
| T-36 | **D2** idempotencia del job | Rotar dos veces seguidas (modo job) | La segunda no cambia nada (`debeRotar=0`) | DB |
| T-37 | **D2** tolerancia | Token anterior dentro / fuera de `TOLERANCIA_SEGUNDOS` | Dentro: válido; fuera: 404 `MQR_001` | DB |
| T-38 | **D2** token de dos rotaciones atrás | Rotar dos veces y usar el primero | 404 `MQR_001` | DB |
| T-39 | **D2** PIN único con anteriores | PIN nuevo igual al `pinAnterior` de otra ventana activa | `MQR_006` | DB |
| T-40 | **D2** rotación manual | Docente titular / no titular / sin ventana | 200 con QR nuevo / 403 / 404 | DB, Controller, Security |
| T-41 | **D2** ventana vencida | `fechaExpiracionUtc` pasada | La rotación marca `EXPIRADA` y no genera token nuevo | DB |
| T-42 | **D2** scheduler | Dos ventanas por rotar, una falla | La otra se rota igual; métrica `matricula_qr_rotacion_total{error}` incrementa; sin excepción propagada | UseCase / Scheduler |
| T-43 | **D2** SSE dirigido | Dos usuarios conectados; rota la ventana del docente A | Solo A recibe `MATRICULA_QR_ROTADO` y `ESTUDIANTE_MATRICULADO`; B no | Adapter realtime, IT |
| T-44 | **D2** no regresión de `publish` | Evento broadcast existente | Sigue llegando a todos los clientes | Adapter realtime |
| T-45 | **D2** usuario sin conexión | `publishToUser` sin sesiones SSE | Descarta sin lanzar excepción | Adapter realtime |
| T-46 | **D2** sin secretos en logs | Rotación | Ni token, ni PIN, ni PNG en logs | Scheduler (captura de salida) |
| T-FE-09 | **D2** modal rota por SSE | Llega `MATRICULA_QR_ROTADO` de su grupo / de otro grupo | Reemplaza QR y PIN / ignora; sin `setInterval` ni `setTimeout` | Jasmine |
| T-FE-10 | **D2** reconexión SSE | El stream se reconecta | Llama a `/rotacion` una vez y muestra el QR nuevo | Jasmine |
| T-ARCH | Arquitectura | ArchUnit | Los puertos nuevos no dependen de ZXing ni de Servlet; los controllers solo dependen de input ports | ArchUnit existentes |
| T-WIR | Wiring | Contexto | `MatriculaQrWiringConfiguration` completo | `MatriculaQrWiringConfigurationTest` |
| T-FE-01 | Ruta pública | Navegar a `/matricula/qr?t=x` sin sesión | No redirige a login; llama a `validar`; el token se borra de la URL | Jasmine |
| T-FE-02 | Estados | Validando / curso / error (404, 403, 429) | Muestra el `message` del backend tal cual | Jasmine |
| T-FE-03 | Crear cuenta | Formulario válido | `registrar` y luego navegar a `/login` con el usuario precargado | Jasmine |
| T-FE-04 | Ya tengo cuenta | Click | Navega a login con `returnUrl` y `matricula=<pin>`; **no** pasa el token | Jasmine |
| T-FE-05 | Modal del docente | Abrir / cerrar / regenerar | `img` con data URL; no queda ninguna referencia a `api.qrserver.com`; muestra la hora absoluta; sin timers | Jasmine (y Grep en el gate) |
| T-FE-06 | Roster por SSE | Evento de su grupo / de otro grupo | Vuelve a pedir el roster solo para su grupo | Jasmine (`teacher-grupo-estudiantes.component.spec.ts`) |
| T-FE-07 | `student-courses` | `?matricula=482913` con sesión | Llama a `matricularAutenticado({pin})` | Jasmine |
| T-FE-08 | Modal de código | PIN de 5 dígitos / letras | Botón deshabilitado | Jasmine (spec nuevo de `student-matricula-codigo-modal`) |
| T-E2E | Flujo completo | Ver §2.4 | — | E2E |

## 2. Pruebas RED por capa

### 2.1 DB
- `test_usp_abrir_ventana_matricula_grupo.sql`: T-01…04, T-08.
- `test_usp_cerrar_ventana_matricula_grupo.sql`: T-05.
- `test_usp_rotar_token_matricula_grupo.sql`: T-35…41.
- `test_usp_validar_token_matricula_publico.sql`: T-06, 07.
- `test_usp_matricular_estudiante_por_token.sql`: T-10…14, 16.
- `scripts/database/tests/test_matricula_qr_concurrencia.ps1`: T-15.
- Migración: T-33.
- Fixtures propios. Los `:r` van en `test_suite.sql` ⚠️ y se retira el `:r` del test autónomo.

### 2.2 Backend
- Use cases de `application/features/matriculaqr/**`: T-01, 09, 10, 16, 27–29, 34. Cobertura 100 % de ramas.
- `CidrRedInstitucionalAdapterTest` y `ClientIpResolverTest`: T-18…22.
- `PublicEndpointRateLimitFilterTest`: T-23…25.
- `ZxingCodigoQrGeneratorAdapterTest`: genera y **decodifica** el PNG (T-01).
- `SecureRandomTokenGeneratorAdapterTest`: longitud, alfabeto Base64URL, PIN de 6 dígitos, hash SHA-256 conocido.
- `MatriculaQrPublicaControllerTest`, `GrupoMatriculaQrControllerTest` y `EstudiantePortalControllerTest` (se amplía): T-07, 18, 26, 30.
- `TokenMatriculaJpaCommandPatternTest`.
- `MatriculaQrSqlServerIT`: T-10, 15 y 29, con `FakeKeycloakAdminServer`.
- `RbacSecurityFilterChainTest` ⚠️: T-17, 31, 32.
- `DbFailureClassifierTest`: `MQR_*`.
- `MatriculaQrWiringConfigurationTest`.

### 2.3 Frontend
- `features/public/matricula-qr/matricula-qr.component.spec.ts` (nuevo): T-FE-01…04.
- `core/services/matricula-qr.service.spec.ts` (nuevo).
- `teacher-matricula-modal.component.spec.ts` (se amplía): T-FE-05.
- `teacher-grupo-estudiantes.component.spec.ts`: T-FE-06.
- `student-courses.component.spec.ts`: T-FE-07.
- `student-matricula-codigo-modal.component.spec.ts` (nuevo): T-FE-08.

### 2.4 E2E (`scratch/test_hu176b.ps1`)
1. Token de `maria.rostagno` y `POST /grupos/{id}/matricula-qr`: espera 201. Se guardan el PIN y el token.
2. `POST /public/matricula-qr/validar` con el token desde localhost (rango de desarrollo): espera 200.
3. `POST /public/matricula-qr/registro`: espera 201.
4. Token Keycloak del nuevo estudiante: OK y rol `ESTUDIANTE`.
5. Repetir el registro: espera 409 `MQR_005`.
6. Con el token del estudiante, `POST /estudiante/matricular-grupo` con el PIN: espera 409 `MQR_004`.
7. `DELETE` de la ventana y luego `validar`: espera 404.
8. Con el perfil `e2e-fuera-campus` (rangos que excluyen localhost): `validar` → 403.
9. 21 llamadas: la última responde 429.
10. `sqlcmd` sobre `uv_estudiante_grupo` (estado) y `uv_token_matricula_grupo` (`totalMatriculas`).

**Comandos:**
```powershell
cd GestioAsistenciaBackend; .\mvnw.cmd test
```
```powershell
cd GestioAsistenciaBackend; .\mvnw.cmd verify -Pintegration
```
```powershell
cd GestioAsistenciaFrontend; npm run test:ci
```
```powershell
cd GestioAsistenciaFrontend; npm run verify
```
Y `test_suite.sql` (AGENTS.md §4.1).

## 3. Efectos secundarios y rollback

- El E2E crea un usuario real (DB + Keycloak) con un documento reservado para pruebas, y lo documenta en `VALIDATION.md`.
- Las pruebas DB hacen rollback.
- El interruptor D9 se prueba en los dos estados.

## 4. RED_SNAPSHOT (fase 03)

| Repo | Commit base | Archivos | SHA-256 / commit RED | Comando | Exit | Falla esperada |
|---|---|---|---|---|---|---|
| GestioAsistenciaDB | — | §2.1 | — | `test_suite.sql` | — | No existen la tabla ni los SP |
| GestioAsistenciaBackend | — | §2.2 | — | `mvnw test` | — | `matricular-grupo` responde 501; no existen los públicos, el filtro ni los adapters |
| GestioAsistenciaFrontend | — | §2.3 | — | `npm run test:ci` | — | No existen la ruta pública ni el servicio; el modal usa `api.qrserver.com` |

## 5. Congelamiento y falsos positivos

- Después del RED no se modifican las pruebas sin un registro `TEST_CONTRACT_CONFLICT`.
- Hay que evitar:
  - probar la red solo con `127.0.0.1`: se necesitan casos fuera de rango, el XFF falso y los bordes de los CIDR;
  - un rate limit probado con el reloj real: se inyecta un `Clock` y se usa un tiempo controlado;
  - verificar el QR solo por la longitud del base64: hay que decodificarlo;
  - specs públicos que importen `authGuard` "por accidente": hay que verificar que la ruta **no** lo tenga;
  - dar por bueno el SSE solo porque se publicó: hay que asertar que el payload **no** tiene PII.
