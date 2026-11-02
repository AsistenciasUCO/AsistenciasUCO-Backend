---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# VALIDATION â€” F3: el docente crea sus propios grupos (backend)

## Evidencia ejecutada (2026-11-02)

| Comando | Resultado |
|---|---|
| `mvnw -B test` con `RbacSecurityFilterChainTest`, `DbExceptionTranslatorTest`, `AccessCatalogConsistencyTest`, `SecurityPepEnforcementFilterTest`, `OpenApiGoldenPathConformanceTest`, las reglas de arquitectura (`CleanArchitectureRulesTest`, `ControllersMustDependOnlyOnInputPortsTest`, `InfrastructureStructureRulesTest`, `Jpa*RulesTest`), `JpaRepositoryBeanUniquenessTest` y las tres clases de prueba del endpoint nuevo | Exit 0, sin fallos |

Casos nuevos:
- `RbacSecurityFilterChainTest.crear_grupo_docente_es_permitido_por_security_filter_chain`: `POST /api/v1/grupos` con DOCENTE pasa el filtro.
- `RbacSecurityFilterChainTest.actualizar_y_eliminar_grupo_siguen_cerrados_al_docente`: `PUT` y `DELETE /api/v1/grupos/{id}` con DOCENTE responden 403.
- `DbExceptionTranslatorTest`: `HOR_005` se traduce a `CONFLICT` (409).
- Endpoint `GET /api/v1/docente/oferta-creacion-grupos`: controlador (delegaciÃ³n y JSON), caso de uso (idDocente del token, lista vacÃ­a, `ForbiddenException` si no es docente) y adaptador JPA (parÃ¡metros nombrados y mapeo de columnas).

## Pendiente

- `mvnw -B verify` completo y la verificaciÃ³n contra la BD con F3 (los SP nuevos de la rama `feature/F3-docente-crea-grupo` del repo de BD deben estar desplegados): lo corre el CI del PR.
- Prueba manual integrada con la BD y el frontend de F3 (docente con y sin vínculo, creando con horario y sin período).

## Estado

`PARCIAL`: cambio de acceso y pruebas unitarias en verde; falta el `verify` completo y la integraciÃ³n con la BD de F3.
