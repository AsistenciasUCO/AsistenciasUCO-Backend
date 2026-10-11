---
status: TECHNICAL_DESIGN_DECIDED_EXTERNAL_AUTHORIZATION_PENDING
date: 2026-10-09
source-branch: jose-valencia/maint-003d-utc-contract-tests-handoff
local-only-reported-commit: afbde3a
db-baseline: johnjduque/gestion-asistencia-db@f2871a9564d6c4cc5abc3745854414243bfda238
---
# MAINT-003E: causas, decisiones de cierre y puertas de activación

## Lo que se inspeccionó
En GitHub la rama remota MAINT-003D termina en a495ba7afec729bb8f401847f3018db07e126e5e: no contiene merge local de 003C ni afbde3a, y no permite corroborar el reporte de 1486 tests. Esas cifras son **evidencia reportada por Codex en el equipo**, no ejecución nueva de esta revisión. El repo DB develop f2871a9 no tiene procedenciaTemporal; DB baseline congela shape exacto de uv_sesion con 10 columnas. No modificar la DB ni inferir estado de filas de producción.

## Diagnóstico de los 17 fallos de clean verify reportados
- 13 assertions de SesionV2HttpContractRedTest: no hay controladores v2 => RED intencional.
- 3 assertions de OpenApiSesionesV2ContractRedTest: aún no está contrato v2 => RED intencional.
- 1 assertion de HttpUtcInstantCodecStrictProfileRedTest (local 003C): RECHAZA ".5Z" en contradicción con D02; ese test está **mal**.
- JDK25 reportado: original codec 7/7, D02/003D 5/5, strict 003C 7/8, guard v1 3/3. JaCoCo 90.26% lines y 80.27% branches, ArchUnit 82/82. Integración SQL 191 IT 0 failures 1 skip (no relacionado con UTC, pero registrar motivo y nombre).
- clean verify EXIT 1 no es GREEN de release. La rama de especificación RED no debe confundirse con rama de código estable.

## Decisiones técnicas ya cerradas sin necesidad de convertirlas en firmas ficticias
**D02**: formato exacto `yyyy-MM-dd'T'HH:mm:ss[.1..7](Z|±HH:mm)`, letras T/Z mayúsculas, sin whitespace exterior. ".5Z" es VÁLIDO, representa 500000000 nanosegundos; ".0000001Z" es válido (100 ns). ".12345678Z" y ".123456789Z" son inválidos; nunca redondear a 7. Una regex es filtro lexical; validar fecha/offset semánticos; normalizar al mismo `Instant` y garantizar rango SQL `datetime2` después de conversión UTC.

**D02 corrección concreta**: en test local 003C cambiar aserción `assertThrows(... "2026-...00.5Z")` por `assertEquals(LocalDateTime.of(..., 500_000_000), ...)`, o quitar solo ese valor de los inválidos y crear test positivo para 1 decimal. No eliminar los otros siete tests ni reducir reglas del codec. Si test usa parametrización, reubicar la entrada al conjunto positivo; conservar mismo conteo o ampliarlo.

**D06**: el pasado sin metadatos jamás se convierte por defecto. Histórico queda NULL hasta resolución por ID+entorno. El diseño propuesto de 003D `NULL/UTC_V2/UTC_GENERADOR/UTC_OWNER` sigue vigente. La marca debe quedar en el mismo INSERT/UPDATE de las horas; v1 las invalida con NULL, v2 confirma UTC_V2, generador confirma UTC_GENERADOR. Para un histórico UTC_OWNER, exigir manifiesto de verificación o conversión individual con hashes y aprobadores; ninguna semilla reclasifica filas.

**Corrección crítica del diseño de vistas**: el contrato DB existente asegura el shape EXACTO de `uv_sesion` (10 columnas y orden). NO añadir `procedenciaTemporal` a `uv_sesion` ni `uv_auth_sesion` existentes sin romper freeze. Proponer una vista nueva `uv_sesion_v2` como proyección de `dbo.uv_sesion` JOIN `dbo.Sesion` por ID con las 10 columnas + marca; una nueva `@Entity` JPA y proyección DTO v2. Conservar campos, orden y consulta legacy. Si la política de DB obliga a modificar vistas anteriores, el owner debe documentar explícitamente la revisión de su contrato exacto y sus pruebas; la opción preferida es vista separada.

**D07**: GET v2 de fila confirmada serializa Z desde UTC ya almacenado; fila indeterminada devuelve `fechaHoraInicio=null`, `fechaHoraFin=null`, `estadoTemporal=INDETERMINADA`, `procedenciaTemporal=null`. Mantiene ID/nombre/numero/grupo. Prohibido interpretar histórico como hora UTC. Si el producto necesita visualizar su literal histórico, hacerlo en campo opcional separado etiquetado `horaSinZonaVerificada` aprobado por contratos, nunca fingir Z.

**D08**: respuestas de formato inválido usan `400 VALIDATION_ERROR` y detalles por campo con `FIELD_INVALID_FORMAT`; campo ausente `FIELD_REQUIRED`; no devolver 500 de `IllegalArgumentException`. Si `fin <= inicio` después de normalizar a UTC, conservar SES_004 (dominio/SP). Actor viene del JWT, roles/ownership reales permanecen.

**Compatibilidad**: POST/PATCH/GET v1, consultas del estudiante `/api/v1/estudiante/materias/{id}/sesiones`, Excel de asistencia, LocalTime recurrentes y SSE sin modificaciones de wire. v1 no transmite timezone y no debe etiquetarse Z. El frontend no hará conversiones hasta release contract v2 real.

## Dos tipos distintos de bloqueo (no mezclarlos)
1. **Contrato técnico** (decidible por equipo de implementación): formato decimal, rutas, salida indeterminada, separación vistas, matriz de tests. Este documento toma las decisiones, evita volver a preguntárselas a Codex.
2. **Autoridad y realidad externa** (no se puede simular): owner DB autoriza nuevo work item pos-DB-GP-001C; owner funcional aporta prueba para clasificar filas; contratos valida wire E1, despliegue y permisos SQL. Sin ello, no ejecutar DDL ni llamar READY a v2.

## Punto adicional observado
El backend `SesionJpaRepository` lee `UvSesionEntity` y construye `SesionRepositoryProjection` SIN marca; por eso añadir columna únicamente en SQL no basta. V2 requiere camino de lectura separado o proyección extendida controlada, sin cambiar serialización v1. El frontend `SessionService` separa strings LocalDateTime en date/time sin convertir: cambiar TZ del navegador no surtirá efecto hasta consumir GET v2 Z y renderizar con IANA.

## Gate de salida y topología de ramas
- Commit local afbde3a y merges 003C: no están en remoto. No intentar hacer fast-forward forzado sobre esos cambios locales. Llevar MAINT-003E por cherry-pick o merge selectivo a worktree local después de comparar.
- Fase codec: validar test 1 decimal corregido, original 7/7, strict 8/8, D02 5/5, v1 guard 3/3. Una microfase codec independiente puede pasar CI si no incluye asserts de endpoints inexistentes.
- Fase RED contract: conservar expectativas v2 en rama aparte, publicar fallo esperado con números; no bajar umbrales, borrar tests ni declarar GREEN. Incorporar a PR v2 cuando DB y owner están listos.
- Fase DB aprobada: esquema + SP + vista v2 y pruebas de integridad en SQL 2022, sin cambiar shape legacy.
- Fase backend v2: JUnit/MockMvc/OpenAPI, JPA + SQL 2022 + Keycloak, no skips focales, 401/403, rollback y correlación.
- Fase frontend: solo después de release real; detectar IANA al cargar, respetar selección explícita, re-render sobre Z canónico, validar DST gaps/overlaps; probar refresh y cambio de día.
