# VALIDATION — APP-01 parámetros institucionales

Fecha: 2026-11-02. Estado: fase A con pruebas focalizadas PASS; navegador detenido en login para acceso manual; lectura autenticada y escritura persistente NOT_RUN/BLOCKED.

## Sincronización y ramas

| Repo | origin comprobado | develop tras sync | Rama actual | Estado |
|---|---|---|---|---|
| Backend alejovh12/AsistenciasUCO | URL personal autorizada | bd1ecd396ea04e27fc386cace847720c1c4cf0c5 | fix/app01-parametros-contrato | Fast-forward desde 32164065; APP-02 merge PR #74 confirmado en develop. |
| Frontend shesho22/AsistenciasUCO-Frontend | URL personal autorizada | 00b60f0877288d950753dbedc9e843e0a3400145 | fix/app01-parametros-contrato | Ya estaba al día y limpio; origin local corregido desde URL de organización a la URL personal autorizada. No se contactó organización. |
| DB johnjduque/gestion-asistencia-db | URL personal autorizada | fe1ccd710aed4f84416bdfe03da36976797ce7df | develop | Ya estaba al día y limpio. No se creó rama porque esta fase no cambia SQL. |

En Backend se conservaron dos rutas preexistentes sin seguimiento: .codex/ y docs/work-items/FIX-003-role-registration-idempotency/evidence/. No fueron modificadas ni eliminadas.

## Base y ejecución focalizada

| Comando/actividad | Resultado | Alcance y límites |
|---|---|---|
| CI Frontend en commit base 00b60f0, check build-test-verify | PASS, exit success; GitHub Actions run 41509638138, 2026-11-02 | Evidencia remota de develop; no sustituye el test local de este cambio. |
| npm test -- --watch=false --browsers=ChromeHeadlessCI --include=src/app/features/admin/admin-system/admin-system.component.spec.ts --include=src/app/core/services/admin-management.service.spec.ts, base sin cambios de tests | PASS, exit 0, 30/30 | Baseline focalizada APP-01; no se ejecutó suite completa. |
| Mismo comando, RED en la rama APP-01 | FAIL esperado, exit 1, 28 PASS / 2 FAIL de 30 | Fallos: la UI no indicaba modo consulta, conservaba la edición/Guardar y el servicio exponía actualizarParametro. No hubo fallo de compilación en esta corrida. Hashes RED están en TEST_PLAN.md. |
| Primera tentativa de RED | FAIL de compilación por sintaxis en el fixture del test | Se corrigió la sintaxis del fixture solamente y se repitió; no se tomó como evidencia RED. |
| Primer GREEN tras adaptar producción | FAIL de compilación porque el tipo frontend compartido también tipaba seis fixtures mock con campos antiguos | Se separó ParametroAdminDto del modelo usado por mocks. No se cambió contrato ni se alteraron expectativas de los tests. |
| Mismo comando tras corrección de tipo/producción | PASS, exit 0, 30/30 | Fase A: GET tipado con el DTO backend, service sin escritor, pantalla de consulta sin acción de guardado. El fixture pasó a ParametroAdminDto sin cambiar sus aserciones; detalle/hash actualizado en TEST_PLAN.md. |
| git diff --check en Frontend | PASS, exit 0 | Sin errores de whitespace en los cambios actuales. |

No se ejecutaron suites completas. No se ejecutaron tests Backend ni DB porque no hubo código Backend/SQL modificado y no existe contrato de escritura. No se repitieron validaciones APP-02.

## Validación manual en navegador

- Navegador integrado: `http://localhost:4200/login` abrió correctamente en ChatGPT in-app browser; título “Gestió Asistencia | Sistema de Control Educativo”. La pantalla visible ofrece “Ingresar con cuenta institucional”. Se dejó allí, sin iniciar el flujo SSO ni ingresar credenciales.
- Rol/pantalla autenticada: PASS para observación manual; el usuario completó el acceso y la app mostró el módulo “MÓDULOS PARA ADMINISTRADOR” con ruta `/app/admin/decanos`. No se registró identidad personal.
- Vista APP-01: PASS para consulta visual en `/app/admin/sistema` → “Parámetros Globales”; la pestaña mostró 34 parámetros. Cada tarjeta mostró clave, grupo, valor actual, tipo, estado y el aviso de modo consulta. Los campos se verificaron como `readonly` en el template actual y no había acciones de Guardar/Editar.
- Resultado de lectura: la lista de 34 elementos apareció después de la carga de la vista. No se capturó el status HTTP ni un trace de red; por eso se registra como evidencia funcional de UI, no como evidencia de integración HTTP completa.
- La pantalla también mostró el aviso genérico “Esta funcionalidad no está disponible por el momento.” durante la apertura; la lista de parámetros se mantuvo visible. No se atribuye ese aviso a la petición GET sin evidencia de red.
- Salud backend vía `curl.exe http://127.0.0.1:8080/actuator/health`: NOT_RUN/INACCESIBLE desde la sesión de terminal (`curl` no pudo conectar; HTTP 000). La pantalla frontend sí fue accesible en el navegador integrado; no se infiere de ello el estado del backend.
- Integración HTTP del GET: NOT_RUN como comprobación separada de método/status; no se inspeccionaron logs, tokens ni credenciales.
- PATCH/PUT, respuesta, guardar, recarga y persistencia: NOT_RUN; no hay operación autorizada en OpenAPI/DB.
- No se han cambiado datos institucionales ni se han capturado credenciales.

## Límites y resultado

Fase A: tests focalizados PASS. Cierre funcional de APP-01: NOT DONE. La operación de escritura permanece NOT_READY por ausencia de contrato OpenAPI aprobado y contrato de persistencia DB; véanse CONTRACT_MATRIX.md y DB_WRITE_CONTRACT_PROPOSAL.md. La prueba manual de lectura está pendiente de acceso autenticado.
