---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# PLAN — HU008: oferta de grupos y cupos para el estudiante

> **CULM-001 A.1 (2026-11-07):** la solicitud de ingreso (HU053) se retiro; la oferta queda como consulta de oferta y cupos. La matricula se hace por QR/PIN (HU176B) o manualmente por el docente titular (HU050).

## 1. Identidad y objetivo

- **Historia:** HU008. El enunciado original es "Ver la información de los grupos existentes"; el catálogo la llama "Consultar información de grupos y cupos".
- **Work Item ID:** `HU008-oferta-grupos-estudiante`
- **Fecha:** 2026-10-27
- **Base Git:** `develop` (DB `fec4503`, backend `84ac59d`, frontend `f010db3`). Rama `feature/HU008-oferta-grupos-estudiante` en los 3 repos.
- **Clase de cambio:**
  - funcional aditivo (lectura nueva y parámetro nuevo);
  - cambio de regla en la matrícula existente, de 1 línea por SP.
  - Ninguna tabla cambia.

### Decisiones de John (2026-10-27)

| # | Tema | Decisión |
|---|---|---|
| J1 | Alcance | **Opción A:** pantalla "Oferta de grupos" del estudiante (períodos vigentes y próximos, docente, horario, capacidad, cupos, relación con el grupo) con el botón "Solicitar ingreso" (flujo HU053). El endpoint queda bajo `/estudiante` (no `/grupos/oferta-academica`). |
| J2 | Clasificación | Las asignaturas **propias del programa** son las del componente **ESP** (específicas). **BAS** (básicas) e **INS** (institucionales) son **transversales**. |
| J3 | Transversales en la oferta | La oferta incluye las **ESP de sus programas** y las **BAS/INS de cualquier programa de su institución**, marcadas como transversales. |
| J4 | Inscripción en el programa | El estudiante **solo queda inscrito en un programa** (`EstudiantePrograma`) **cuando ingresa a un grupo de una asignatura ESP**. Al ingresar a una BAS o INS no se inscribe en el programa de esa asignatura. |
| J5 | Opción futura | Debe poder dejar de mostrarse la oferta de BAS/INS, de modo que solo se ingrese con el enlace del docente. Se resuelve con el parámetro `OFERTA.INCLUIR_TRANSVERSALES` (1 por defecto). |

### AS-IS comprobado

| Hecho | Evidencia |
|---|---|
| No existe consulta de oferta; el catálogo marcaba HU008 como PENDIENTE | `HU-CATALOG.md`, búsqueda en `src/main` |
| Cada asignatura pertenece a un solo plan y a un solo programa; el único dato de clasificación es `Asignatura.componente` (BAS/ESP/INS) | `Asignatura`, `Componente`, seed `11_institucion_infraestructura.sql` |
| `usp_registrar_estudiante_en_grupo_interno` (matrícula del docente y aceptación de HU053-055) insertaba `EstudiantePrograma` para el programa del grupo **siempre** | SP, paso 4 |
| `usp_registrar_estudiante_en_grupo_autonomo` (auto-matrícula, inactiva; HU176B en 501) tiene la misma lógica | SP |
| El SP de matrícula **no está congelado** (no figura en `DB_DEVELOP_FREEZE_MANIFEST`, `DB_LB004D_*` ni `DB_UTC_D06_*`) | `docs/contracts/*.sha256` |

## 2. Análisis de impacto del cambio de regla J4

| Consumidor de `EstudiantePrograma` | Efecto de no inscribir por BAS/INS | Valoración |
|---|---|---|
| `usp_registrar_estudiante_en_grupo_interno` (docente matricula directo; HU053-055 al aceptar) | Una matrícula en BAS/INS ya no crea la fila del programa | Es la regla pedida (J4) |
| `usp_registrar_estudiante_en_grupo_autonomo` (inactivo) | Mismo criterio, para que la regla sea única | Sin uso productivo hoy |
| `uv_estudiante_programa` → `EstudianteProgramaJpaRepository` (directorio de estudiantes por programa) | Quien solo cursa una transversal de otro programa ya no aparece en el directorio de ese programa | Deseado: no es estudiante de ese programa |
| `InstitutionalScopeJpaRepository#canGestionarCuenta` (coordinador o decano gestiona cuentas de estudiantes de su programa o facultad) | El coordinador del programa que ofrece la transversal no gestiona la cuenta de ese estudiante | Deseado: la gestiona su programa propio |
| `usp_consultar_kpis_institucion` (estudiantes activos de la institución) | Sin doble conteo por transversales | Neutro o mejora |
| `usp_consultar_oferta_grupos_estudiante` (HU008) | Las ESP visibles son solo las de sus programas reales | Coherente con J3 |

- **Datos existentes:** John pidió depurar las filas `EstudiantePrograma` creadas antes por transversales. Se hace con el script manual y opcional `scripts/maintenance/hu008_depurar_estudiante_programa_transversales.sql` del repo de DB (dry run + borrado transaccional con verificación; fuera del despliegue automático). En la BD local de John el dry run dio 0 candidatas. Detalle en el PLAN del work item de DB.
- **Asistencia:** el docente titular y el coordinador del programa del grupo siguen viendo al estudiante en el grupo, porque la asistencia se rige por la matrícula (`EstudianteGrupo`), no por `EstudiantePrograma`.

## 3. Alcance

| Capa | Cambio | Archivos |
|---|---|---|
| DB | SP nuevo `usp_consultar_oferta_grupos_estudiante`; parámetro `OFERTA.INCLUIR_TRANSVERSALES`; 1 `INNER JOIN Componente ... 'ESP'` en `usp_registrar_estudiante_en_grupo_interno` y `_autonomo`; permiso aditivo; 2 pruebas | `schema/stored-procedures/` (1 nuevo, 2 modificados), `schema/seed/24_parametro_oferta_grupos.sql`, `schema/security/rol_asistencias_runtime_hu008.sql`, `test/test_usp_consultar_oferta_grupos_estudiante.sql`, `test/test_hu008_regla_inscripcion_programa.sql`, `test_suite.sql`, `test/test_contract_closure.sql` |
| Backend | Feature `estudiante/consultarofertagrupos`; `OfertaGruposEstudianteQueryPort` + `OfertaGruposEstudianteJpaRepository`; `EstudianteOfertaAcademicaController` (`GET /api/v1/estudiante/oferta-academica`); fila nueva en `access-catalog.json`; repositorios 47 → 48 | NUEVOS + `EstudianteWiringConfiguration`, `access-catalog.json`, `JpaRepositoryBeanUniquenessTest` |
| Frontend | `OfertaAcademicaService`; `StudentGroupOfferComponent` (ruta `/app/estudiante/oferta`, menú "Oferta de Grupos"), con etiqueta "Transversal · Básica/Institucional" y botón "Solicitar ingreso" (`SolicitudIngresoService.solicitar`); mock | `core/services/oferta-academica.service.ts`, `features/student/student-group-offer/**`, `app.routes.ts`, `dashboard-layout.component.ts`, `mock-backend.interceptor.ts` |

**No alcance:**
- tablas;
- `SecurityConfig` (la regla `/api/v1/estudiante/**` ya cubre la ruta);
- clasificar obligatoria/electiva (requiere un campo o tabla nuevos);
- que una asignatura pertenezca a varios programas (requiere una tabla nueva);
- depurar filas `EstudiantePrograma` antiguas;
- el repo Infra.

## 4. Decisiones técnicas

- **D1:** El parámetro se lee en el SP (`dbo.ufn_obtener_parametro('OFERTA', 'INCLUIR_TRANSVERSALES')`). Cambiarlo no requiere desplegar código:

  ```sql
  UPDATE dbo.CatalogoParametro SET valor = '0' WHERE grupo = 'OFERTA' AND clave = 'INCLUIR_TRANSVERSALES';
  ```

  Con `0` la oferta muestra solo las ESP de sus programas. La solicitud por enlace del docente (HU053) sigue funcionando para cualquier grupo. Un valor ausente o inválido equivale a `1`. El MERGE del seed no sobrescribe un valor ya cambiado en el ambiente.
- **D2:** "Su institución" es la institución de las facultades de sus programas (`EstudiantePrograma → Programa → Facultad.institucion`).
- **D3:** El SP devuelve `codigoComponente`. La Application lo traduce a `tipoAsignatura` (ESPECIFICA, BASICA o INSTITUCIONAL) y a `transversal`.
- **D4:** Un ejecutor que no es estudiante obtiene 0 filas en el SP. El caso de uso responde 403 antes de llegar a la DB.
- **D5:** El cambio de J4 se hace en el único punto donde se crea `EstudiantePrograma` por matrícula: un `INNER JOIN Componente ... AND codigo = 'ESP'` en la consulta que obtiene el programa. Sin componente ESP, el programa queda nulo y no se inserta la fila. No cambia la firma ni el resultset.

## 5. Riesgos, rollback y parada

- **PDP:** la fila nueva del `access-catalog.json` debe reaprovisionarse en el PDP del Security Baseline (Alejo).
- **Regla J4:** cambia el comportamiento de HU053-055 y de la matrícula del docente. Hay pruebas de regresión RI-01..RI-03 y se actualizaron los work items de HU053-055.
- **Rollback:**
  - quitar el `JOIN` de los 2 SP de matrícula;
  - `DROP PROCEDURE` de la oferta;
  - `REVOKE`;
  - borrar el parámetro y la fila del catálogo.

## 6. DoR

READY: decisiones J1-J5 de John (2026-10-27). Sin tablas nuevas. Objetos congelados intactos.
