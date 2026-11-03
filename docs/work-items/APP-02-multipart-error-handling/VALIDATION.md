---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# VALIDATION — APP-02

## Línea base y cambios

- Base sincronizada: `3216406543ebdb439d41b7fa4852dfb5ef8ace71`.
- Rama: `fix/app02-multipart-errors`.
- Fecha local: 2026-11-02 (America/Bogota).
- Evidencia original: `SEC-BASELINE-019/VALIDATION.md`, F-04; no repetida.
- `RED_SNAPSHOT`: archivo y SHA registrados en `TEST_PLAN.md`; 48 tests, 3 fallos esperados, 0 errores/skips, exit 1.

## Comandos y resultados

| Alcance | Comando / entorno | Resultado | Cobertura / skips | Limitaciones |
|---|---|---|---|---|
| RED focalizado | `.\mvnw.cmd -B -ntp "-Dtest=ArchivoControllerErrorHttpTest,ArchivoControllerTest,ContentSecurityValidatorTest,SubirArchivoUseCaseImplTest" test` | RED esperado: 48 run, 3 fallos por 500 recibido en APP02-01..03, exit 1 | 0 skips | Snapshot SHA en TEST_PLAN; producción aún sin cambios en esa corrida. |
| GREEN focalizado | `.\mvnw.cmd -B -ntp "-Dtest=ArchivoControllerErrorHttpTest,ArchivoControllerTest,ContentSecurityValidatorTest,SubirArchivoUseCaseImplTest" test` (JDK 25, Windows; 2026-11-02 21:40 America/Bogota) | PASS: BUILD SUCCESS, 48 tests, 0 fallos, 0 errores, 0 skips; exit 0 | Cobertura no medida en `test`; JaCoCo line/branch se mide con `verify` | Controller MockMvc + use case/validador; multipart malformado se representa por `MultipartException` lanzada en la ruta HTTP, no por parser de contenedor real. |
| Backend verify | `.\mvnw.cmd -B -ntp verify` (JDK 25, Windows; 2026-11-02 21:50 America/Bogota) | PASS: BUILD SUCCESS; 3003 tests, 0 fallos, 0 errores, 0 skips; ArchUnit y JaCoCo checks PASS; exit 0 | JaCoCo global: LINE 92.34% (14,881/16,116), BRANCH 83.79% (4,201/5,014) | La corrida requirió permiso para abrir loopback local en la prueba ClamAV; completó el mismo comando sin cambios. No se ejecutó perfil `integration`. |
| CI remoto PR #74 | GitHub, head `c60211f` (2026-11-02 America/Bogota) | PASS: Backend Quality Gate (Maven clean verify + build Docker), ArchUnit, SonarCloud, CodeQL Java Analysis, Trivy repo/imagen y Dependency Review | SonarCloud New Code: 100% coverage, 0 issues, 0 security hotspots y 0% duplication; CodeQL y Trivy no reportan alertas nuevas | CodeQL emitió una advertencia de configuración `build-mode` y usó análisis completo como fallback. No se ejecutó `-Pintegration`; los resultados son remotos. |
| Frontend manual | Stack oficial `READY`; sesión autenticada como ESTUDIANTE; formulario de reclamo y soporte disponibles | PASS (carga válida) | El archivo seleccionado desde el portal quedó visible como soporte adjunto tras la respuesta del flujo Angular. | No se envió la reclamación. No se conservaron nombre, contenido ni identificador del archivo en la evidencia. |

## Casos APP-02

| ID | Resultado | Evidencia |
|---|---|---|
| APP02-01 JSON → 415 | PASS | `ArchivoControllerErrorHttpTest.json_en_lugar_de_multipart_retorna_415_estandarizado`; JSON envelope, `INVALID_REQUEST`, input port sin invocación. |
| APP02-02 falta `archivo` → 400 | PASS | `ArchivoControllerErrorHttpTest.multipart_sin_la_parte_archivo_retorna_validacion_requerida`; `VALIDATION_ERROR`, `archivo/FIELD_REQUIRED`, input port sin invocación. |
| APP02-03 multipart malformado → 400 sanitizado | PASS (mapeo) | `ArchivoControllerErrorHttpTest.multipart_exception_retorna_400_sin_exponer_detalle_del_parser`; prueba MockMvc dispara `MultipartException`; no es una prueba de parser de contenedor real. |
| APP02-04 vacío/tipo inválido → 400 | PASS | `ContentSecurityValidatorTest`: vacío, extensión/MIME/magic bytes no coherentes siguen rechazándose. |
| APP02-05 tamaño excedido → 400 | PASS | Handler específico cubierto por `ArchivoControllerErrorHttpTest`; E2E real 5 MiB+1 ya registrado en `LB-004C-EXECUTION_REPORT.md`, no repetido. |
| APP02-06 error interno → 5xx sanitizado | PASS | `ArchivoControllerErrorHttpTest.scanner_no_disponible_retorna_5xx_tecnico_sin_detalles_internos`. |
| APP02-07 multipart válido → 201 | PASS (controller + provider previo) | `ArchivoControllerTest`; provider real 201/ClamAV/MinIO acreditado en LB-004C; carga positiva desde Angular registrada en APP02-08. |
| APP02-08 recorrido de adjunto desde Angular | PASS (carga válida) | Se seleccionó un PDF desde el portal; el formulario mostró el soporte adjunto después de completar la carga. No se envió la reclamación. La selección la realizó el usuario en el selector nativo. |

## Estado

IMPLEMENTATION_COMPLETE_PENDING_CLOSURE. La carga válida se verificó desde Angular; la reclamación no se envió. Completar los requisitos formales de cierre antes de cerrar el work item.
