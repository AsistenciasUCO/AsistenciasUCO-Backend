---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# VALIDATION — F2: precarga de las sesiones del grupo (backend)

## Evidencia ejecutada (2026-11-02)

| Comando | Resultado |
|---|---|
| `mvnw -B test` con `GrupoJpaCommandPatternTest`, `JpaQueryAdapterContractTest`, `GrupoControllerWriteEndpointsTest`, `GrupoHttpMapperContractTest`, `CleanArchitectureRulesTest` y `JpaRepositoryArchitectureRulesTest` | Exit 0, sin fallos |

## Pendiente

- `mvnw -B verify` completo y la verificación contra la BD con F2 (el SP nuevo de la rama `feature/F2-precarga-sesiones` del repo de BD debe estar desplegado): lo corre el CI del PR.
- Prueba manual: crear un grupo con horario y ver las sesiones; cambiar el horario y ver que se ajustan.

## Estado

`PARCIAL`: la llamada a la precarga y sus pruebas unitarias están en verde; falta el `verify` completo y la integración con la BD de F2.
