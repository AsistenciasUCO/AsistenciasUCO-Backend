---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# PLAN — F2: precarga de las sesiones del grupo (backend)

## Decisión

John (2026-11-02): las sesiones se crean al definir el horario del grupo, con un SP nuevo de la BD (`usp_precargar_sesiones_grupo`, rama `feature/F2-precarga-sesiones` del repo de BD) que **no modifica** `usp_generar_sesiones_grupo` (congelado) y genera solo desde hoy.

## Cambios

1. **`GrupoJpaRepository`:** `crearGrupo` y `actualizarGrupo` ejecutan `usp_precargar_sesiones_grupo` justo después de `usp_guardar_horarios_grupo`, en la **misma transacción**: si la precarga falla, ni el grupo ni el horario quedan. Solo se invoca cuando el comando trae `franjas` (sin franjas, el horario y las sesiones no cambian).
2. **Quién:** la DB acepta al coordinador del programa y al docente titular (el docente que crea su grupo, F3); un ejecutor ajeno recibe `SEC_002` (403).
3. **Contrato HTTP:** sin rutas ni campos nuevos. Crear o editar un grupo con `franjas` ahora también deja sus sesiones futuras alineadas con el horario. El campo `generarSesionesAutomaticas` sigue respondiendo 501: la precarga ya ocurre sola cuando hay horario.
4. Sin cambios en `access-catalog.json` ni en el PDP.

## Pruebas

- `GrupoJpaCommandPatternTest`: crear con franjas ejecuta crear, guardar horario y precargar en ese orden; actualizar con franjas regenera con el ejecutor; sin franjas no toca horario ni sesiones.

## Validación

Ver `VALIDATION.md`.
