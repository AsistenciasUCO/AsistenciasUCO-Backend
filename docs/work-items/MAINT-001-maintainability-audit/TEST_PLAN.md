---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---
# MAINT-001 — pruebas de comportamiento candidatas (aún NO implementadas)

| ID | Given | When | Then (contrato propuesto) | Wrong behavior caught | Nivel |
|---|---|---|---|---|---|
| M01a | EntityManager lanza PersistenceException | repositorio consulta | DatabaseOperationException con causa | ocultar causa | Unit |
| M01b | función de mapping falla por error aritmético ajeno a JPA | query retorna y mapper evalúa | error de programación identificado correctamente, NO etiquetado como error SQL | catch demasiado amplio | Unit |
| M01c | tipo/parámetro de JPA inválido | query se construye/ejecuta | error técnico controlado conforme contrato de repo | fuga de datos internos | Unit |
| M02a | cache hit, otro request igual | getParameter | resultado idempotente con acceso DB único | consulta redundante | Unit |
| M02b | repo provider falló | getParameter | error con causa original | Optional.empty y fallback silencioso | Unit |
| M02c | datos de catálogo se invalidan | lee después invalidación | valor fresco conforme estrategia aprobada | valor obsoleto perpetuo | IT/contrato pendiente |
| M03a | page/size extremos y negativos | buscar estudiantes | rechazo antes de hacer cálculo u overflow, de acuerdo con endpoint validado | offset negativo / overflow silencioso | Unit + HTTP |
| M04a | respuesta subida | serializar | JSON igual a contrato actual con DTO tipado si se aprueba | consumer roto | HTTP contract |
| M05a | filename con CRLF/MIME hostil | descargar | cabeceras sanitizadas y política nosniff/attachment aprobada | response splitting/sniff | HTTP |
| M06a | cambiar byte[] fuera de record tras construcción | leer record | contenido estable si se aprueba inmutabilidad | aliasing accidental | Unit |
| M09a | docente de fixture con múltiples grupos | query JPA real | sin skip, paridad SQL | green falso por @Disabled/assumption | SQL IT |

RED ni GREEN certificados: NOT_RUN. Algunos comportamientos necesitan decisión; nunca forzar fail-only en branch principal. ArchUnit no reemplaza unit/e2e funcional.


## MAINT-01A — pruebas implementadas candidatas de paginación

| Caso | Entrada | Salida esperada | Antirregresión |
|---|---|---|---|
| JPA-PAG-01 | page=-1, size=10 | CrosscuttingException sin interacción JPA | `setFirstResult(-10)` silencioso |
| JPA-PAG-02 | page=0, size=0 o -10 | CrosscuttingException sin interacción JPA | division por cero / tamaño negativo |
| JPA-PAG-03 | page=2^31-1, size=2 | CrosscuttingException sin interacción JPA | overflow de `int` / excepción SQL mal clasificada |
| JPA-PAG-04 | page=2,size=10 y 21 items | mismos `setFirstResult(20)`, `totalPages=3` y DTO | cambio accidental semántica de página |

La test suite previa ya incluye JPA-PAG-04 y cobertura de JPQL constante/parametrizado. Implementación/test nuevos sin Maven local; CI/GREEN y test del driver real todavía pendientes.


## MAINT-01B — criterios del profesor y pruebas de vistas

| Caso | Entrada/fixture | Observable | Nivel |
|---|---|---|---|
| VISTA-01 | uv_estudiante_identidad con 0/N estudiantes | total y page correctos, no carga masiva | SQL Server IT |
| VISTA-02 | páginas 0/1/2, size=5 | mismos IDs/orden que SQL `ORDER BY ... OFFSET ... FETCH` | SQL Server IT |
| VISTA-03 | mismo apellido/nombre/documento | desempate por UUID `e.id`, sin duplicados | SQL Server IT con fixture >10 |
| VISTA-04 | estudiante en múltiples grupos | filtro `EXISTS` sobre uv_estudiante no duplica identidad | SQL Server IT con fixture multigrupo |
| HTTP-01 | page=0,size=20 | válido | Unit validator |
| HTTP-02 | page=MAX_INT,size=1 | offset permitido por JPA | Unit validator |
| HTTP-03 | page=MAX_INT,size=100 | error de validación en page, sin truncamiento | Unit validator |
| HTTP-04 | size=0 o 101 | regla HTTP 1..100 preservada | Unit validator |

`VISTA-03/04` requieren fixture SQL aprobada y datos suficientes; no declararlos PASS con test mock o IT vacía. Tests nuevos preparados en `ConsultarEstudiantesPaginationValidatorTest` y `CoreViewQueriesJpaParityIT`. No SQL DDL, no cambio wire ni orden de aplicación.
