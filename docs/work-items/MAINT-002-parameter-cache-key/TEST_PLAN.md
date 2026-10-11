---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-002 — matriz de comportamiento

| ID | Given | When | Then | Wrong implementation caught | Nivel |
|---|---|---|---|---|---|
| PCK-01 | grupo=`a::b`, clave=`c` → valor1; grupo=`a`, clave=`b::c` → valor2 | se consultan ambos en un adapter | valor1 y valor2 separados; dos queries | clave plana concat colisiona | Unit RED candidate |
| PCK-02 | llamadas repetidas al mismo par | acceso al catálogo | caché hit y sin queries adicionales | caché deshabilitada | Unit |
| PCK-03 | claves con espacios perimetrales | consultar | mismo valor tras trim, params normalizados | caché fragmentada | Unit |
| PCK-04 | primer miss, segundo success | consultar | no guardar miss | un miss perpetuo | Unit |
| PCK-05 | proveedor lanza PersistenceException | consultar | ParameterCatalogException con causa | silencio/fallback inseguro | Unit |
| PCK-06 | grupos/key blank | consultar | Optional.empty, cero interacciones DB | consulta inútil | Unit |
| PCK-07 | provider real y par válido institucional | consultar | mismos valores que SQL Server | JPA/HQL incompatible | IT external |

Unit tests existentes cubren PCK-02..06 y se añade PCK-01. PCK-07 requiere entorno SQL y datos aprobados; no confundir "unit pass" con provider. `Query getResultList` del mock está configurado para devolver dos respuestas distintas: el test viejo falla al leer el segundo par del caché incorrecto.
