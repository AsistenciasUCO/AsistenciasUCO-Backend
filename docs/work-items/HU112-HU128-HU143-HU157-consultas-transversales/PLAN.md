---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-02
---

# PLAN — HU112 / HU128 / HU143 / HU157: Consultas del decano y del administrador

## 1. Identidad y objetivo

- **Historias de usuario** (títulos del HU-CATALOG de Infra, que manda sobre el Plan_de_Trabajo de la BD por decisión de John):
  - **HU112**: Consultar tabla unificada de programas y créditos.
  - **HU128**: Filtro de profesores por programa asignado.
  - **HU143**: Búsqueda transversal de asignaturas.
  - **HU157**: Ficha unificada del estudiante con historial completo.
- **Work Item ID:** `HU112-HU128-HU143-HU157-consultas-transversales`
- **Fecha:** 2026-11-02
- **Base Git:** rama `feature/HU112-HU128-HU143-HU157-consultas-transversales` sobre `develop` en BD, backend y frontend.
- **Actores:** `DECANO` (HU112, HU128, HU143 en su facultad, HU157 historial en su facultad) y `ADMINISTRADOR` (HU143 en su institución, HU157 buscador e historial en su institución). Solo lectura.
- **AS-IS comprobado:** las 4 HU estaban `[PENDIENTE]` en el HU-CATALOG, sin SP, endpoint ni pantalla. `dean-programas` calcula los créditos con una llamada a la malla por plan (`GET /decano/planes-estudio/{id}/asignaturas`). El decano no tiene pantalla de docentes y el administrador no tiene buscador de estudiantes. `usp_consultar_ficha_estudiante_programa` y `usp_consultar_resumen_asistencia_estudiante_programa` (HU069) solo resuelven el ámbito del coordinador. `/api/v1/decano/**` ya admite `DECANO` y `ADMINISTRADOR`, y `/api/v1/admin/**` solo `ADMINISTRADOR`.

### Criterios de aceptación

1. **HU112:** el decano ve, en una sola llamada y paginada, cada programa de su facultad con sus planes: tipo, modalidad, jornada, estado, INP, semestres, número de asignaturas y suma de créditos. Un programa sin plan aparece con totales en 0.
2. **HU128:** el decano lista los docentes de su facultad con vínculo activo y los filtra por programa, nombre o documento y estado.
3. **HU143:** busca asignaturas por código o nombre con su programa, plan, semestre, área, componente y créditos. El decano ve solo las de su facultad; el administrador, las de toda su institución, con filtro opcional por facultad.
4. **HU157:** el administrador busca estudiantes de su institución y abre una ficha con datos personales y una fila por matrícula (facultad, programa, plan, período, grupo, asignatura y estado), cada una con el resumen de asistencia (sesiones, asistencias, faltas, excusas, % de inasistencia y permanencia). El decano ve ese historial, limitado a su facultad, en la ficha que ya tiene (HU105).
5. Un usuario sin el perfil exigido recibe 403 (`SEC_001`). Si el estudiante está fuera del ámbito, el historial responde 404.

## 2. Alcance

| Capa | Cambio |
|---|---|
| DB | SP `usp_consultar_programas_creditos_facultad_paginado`, `usp_consultar_docentes_facultad_paginado`, `usp_buscar_asignaturas_paginado`, `usp_buscar_estudiantes_institucion_paginado`, `usp_consultar_historial_estudiante` y el interno compartido `usp_validar_consulta_paginada_interno`. Permisos aditivos en `rol_asistencias_runtime_hu112_hu157.sql`, pruebas en `test_consultas_transversales_hu112_hu157.sql` y exclusiones READ_QUERY_RESULTSET (ver el work item homónimo de la BD). |
| Backend | Feature `consultastransversales` (input port, interactor, caso de uso), `ConsultasTransversalesQueryPort` + `ConsultasTransversalesJpaRepository` (filas `Object[]` con `NativeRowValues.uuid`), `DecanoConsultasTransversalesController` (`/api/v1/decano/programas-creditos`, `/docentes`, `/asignaturas`, `/estudiantes/{id}/historial`) y `AdminConsultasTransversalesController` (`/api/v1/admin/asignaturas`, `/estudiantes`, `/estudiantes/{id}/historial`), wiring propio. Regla compartida `ConsultaPaginadaRules`, ahora también usada por `ConsultarFacultadDecanoUseCaseImpl`. `DbCodeTranslator` traduce `SEC_001` a 403. Rutas nuevas en `infra/security-baseline/access-catalog.json`. `SecurityConfig` sin cambios. |
| Frontend | Pantallas nuevas `decano/docentes`, `decano/asignaturas` y `admin/estudiantes` (buscador y ficha unificada). Tabla unificada de programas y créditos dentro de `dean-programas`: las tarjetas toman los créditos de la tabla en lugar de pedir la malla de cada plan. Historial en la ficha de `decano/estudiantes`. Entradas nuevas en el menú y simulación en el interceptor mock. |

**Fuera de alcance:** exportes, edición de datos del estudiante y docentes sin vínculo activo.

## 3. Decisiones

- **D1 (John, 2026-11-02):** HU143: el DECANO ve solo las asignaturas de su facultad; el ADMINISTRADOR, todas las de su institución.
- **D2 (John):** HU128 tiene pantalla propia `decano/docentes`, con selector de programa y buscador.
- **D3 (John):** HU112 agrega la tabla en `dean-programas` y conserva las tarjetas, que toman los créditos de la tabla (menos llamadas).
- **D4 (John):** HU157 tiene pantalla nueva `admin/estudiantes`, y además el historial se agrega a la ficha del decano de HU105.
- **D5:** El SP valida el perfil (`usp_validar_permiso_rbac_usuario_interno`) y el ámbito. El caso de uso solo valida la entrada y exige un usuario autenticado. Las rutas `/decano/**` admiten también al administrador: en asignaturas e historial recibe el ámbito de su institución, y en programas y docentes el SP lo rechaza con `SEC_001` (403).
- **D6:** Las filas se leen como `Object[]`, sin entidades JPA, porque una fila por programa y plan o por matrícula no tiene un identificador único que sirva de `@Id`.
- **D7:** Controladores, wiring y feature propios: las pantallas y el portal del decano y del administrador son de otro integrante (Sergio), así que los cambios en sus archivos son mínimos.

## 4. Riesgos y rollback

- `access-catalog.json` cambia: el PDP de Security Baseline debe reaprovisionarse con las rutas nuevas (aviso a José Alejandro).
- `DbCodeTranslator` ahora mapea `SEC_001` a 403 en todos los repositorios que lo usan. Antes ese código caía en un 500, así que el cambio solo corrige.
- Rollback: revertir los commits de la rama; los SP son nuevos y no hay cambios de esquema.
