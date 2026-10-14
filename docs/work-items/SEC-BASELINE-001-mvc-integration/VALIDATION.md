# Validación

| Fecha | Comando/acción | Resultado | Alcance/limitación |
|---|---|---|---|
| 2026-10-13 | Inspección tag `v0.1.6` y assets publicados | PASS | Código/contratos leídos; PDFs solicitados ausentes |
| 2026-10-13 | `docker compose ... config --quiet` del laboratorio | PASS, exit `0` | Validación estática; engine no accesible y contenedores `NOT_RUN` |
| 2026-10-13 | Identidad `sub`/`idUsuario` | BLOCKED_BY_MISSING_EVIDENCE | No se inventa equivalencia |
| 2026-10-14 | `.\mvnw.cmd -B -ntp '-Dtest=SecurityBaselinePepFilterTest' test` | PASS, 3/3 | Ejecución focal servlet sin proveedor externo |
| 2026-10-14 | `.\mvnw.cmd -B -ntp '-Dtest=SecurityBaselinePepClientTest' test` | PASS, 2/2 | Servidor loopback local; ejecutado con permiso elevado para habilitar loopback |
| 2026-10-14 | `.\mvnw.cmd -B -ntp '-Dtest=SecurityConfigTest' test` | PASS, 12/12 | JWT/roles/wiring MVC existente; ejecutado con permiso elevado por Mockito/Java 25 |
| 2026-10-14 | `.\mvnw.cmd -B -ntp verify` | PASS, 1539/1539 | ArchUnit PASS; JaCoCo LINE 90.54% / BRANCH 79.79%; exit `0` |
| 2026-10-14 | `git diff --check` | PASS | Sin whitespace errors |
| 2026-10-14 | Búsqueda de secretos en el diff experimental | PASS | No se añadieron valores reales; finding externo reportado por archivo/tipo |
| 2026-10-14 | Publicación de rama canónica | PASS | `jose-valencia/experiment-security-baseline-mvc` → `a74b2886c7be175235943ca2ddd58660e6c2eb45` |
| 2026-10-14 | `github_create_pull_request` draft hacia `develop` | BLOCKED, HTTP `403` | `Resource not accessible by integration`; no PR ni merge |

No se ejecutó `-Pintegration verify`: este experimento no cambia persistencia ni esquema SQL y el laboratorio Security Baseline quedó `NOT_RUN` por falta de runtime Docker/identidad verificable.
