---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-03
---

# VALIDATION — SES-01 (Backend)

Rama `fix/ses01-cierre-estricto-asistencia`. Commits: `be5c9fdd` (RED), `cf146bd6` (fix).

## RED → GREEN

| Paso | Comando | Resultado |
|---|---|---|
| RED | `./mvnw.cmd -o -Dtest=RegistrarAsistenciasSesionUseCaseImplTest,DbFailureClassifierVentanaAsistenciaTest,AsistenciaWiringConfigurationTest test` | Falla de compilación esperada: faltan `ERR_SESION_*` y el constructor con `SesionProcedenciaQueryPort`/`Clock` |
| GREEN focalizado | mismas clases + `DbExceptionTranslatorTest`, `DbFailureClassifier*Test`, `AsistenciaControllerContractTest` | 174/174 PASS |
| Contrato HTTP | `AsistenciaControllerContractTest`, `OpenApiGoldenPathConformanceTest` | 17/17 PASS (409 con código público y sin detalle técnico) |
| Primer `verify` completo | `./mvnw.cmd -o verify` | 3081/3082: `OpenApiGoldenPathValidationTest.canonicalSha256MatchesExactYamlBytes` exigía regenerar `openapi-golden-path.sha256` tras el cambio de contrato; regenerado sobre los bytes exactos (eol=lf) |
| `verify` final | `./mvnw.cmd -o verify` | **3082/3082 PASS**, ArchUnit incluido, JaCoCo «All coverage checks have been met», BUILD SUCCESS |

## Matriz SES-01 (unitaria, reloj fijo; sesión 08:00–10:00 Bogotá = 13:00–15:00 UTC)

| Caso | Esperado | Estado |
|---|---|---|
| 12:59:59Z (antes del inicio) | 409 `ERR_SESION_NO_INICIADA`, sin persistir ni publicar | PASS |
| 13:00:00Z (inicio exacto), 14:00Z, 14:59:59.999999999Z | Persiste | PASS |
| 15:00:00Z (fin exacto), +1 ns, 17:00Z (dos horas después), día siguiente | 409 `ERR_SESION_CERRADA`, sin persistir | PASS |
| Reloj del servidor en Londres, Berlín, Bogotá, Tokio con el mismo instante | Misma decisión | PASS |
| Procedencia NULL, desconocida o en minúsculas | 409 `ERR_SESION_HORARIO_INDETERMINADO` | PASS |
| `UTC_V2`, `UTC_GENERADOR`, `UTC_OWNER` dentro de la ventana | Persiste | PASS |
| Docente ajeno con sesión cerrada | 403 (no revela el estado temporal) | PASS |
| Sesión inexistente | 404 | PASS |
| DB `SES_007/008/009` (lote y autónomo) | 409 con el código público equivalente | PASS |

## No ejecutado

| Prueba | Estado | Motivo |
|---|---|---|
| `-Pintegration` (`AsistenciaCommand*IT`, fixture con sesiones v2 activas) | **NOT_RUN** | Escribe en la base local compartida y la versión DB de SES-01 está pendiente de aprobación del owner |
| HTTP real contra 8080 y navegador (DOCENTE) | **NOT_RUN** | Requiere backend en 8080 con esta rama y login manual del usuario; sin sesión activa segura solo se valida lectura |
| Carrera en el instante de cierre extremo a extremo | **NOT_RUN** | Depende de la versión DB aprobada (validación bajo bloqueo + revalidación antes del `COMMIT`) |

## CI

Pendiente: el CI del backend corre al abrir el PR.
