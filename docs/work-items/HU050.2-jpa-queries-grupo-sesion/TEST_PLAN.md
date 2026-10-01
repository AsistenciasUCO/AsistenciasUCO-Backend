---
status: active
type: test-plan
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# TEST_PLAN — HU050.2: Pruebas Unitarias y Delegación a JPA

Fase 03-tester-red.

## 1. Casos de Prueba Requeridos

### 1.1 GrupoRepositorySqlServerAdapterTest
1. queries_delega_a_jpa_cuando_esta_configurado:
   - Mockear GrupoJpaQueryPersistence.
   - Invocar dapter.consultarGrupos().
   - Verificar delegación a jpaQueryPersistence.consultarGrupos().
   - Invocar dapter.consultarEstudiantesGrupo(grupoId).
   - Verificar delegación a jpaQueryPersistence.consultarEstudiantesGrupo(grupoId).
2. consultarGrupos_con_fallback_jdbc:
   - Sin GrupoJpaQueryPersistence (null).
   - Verificar ejecución mediante NamedParameterJdbcOperations.
3. consultarEstudiantesGrupo_con_fallback_jdbc:
   - Sin GrupoJpaQueryPersistence (null).
   - Verificar ejecución mediante NamedParameterJdbcOperations.

### 1.2 SesionRepositorySqlServerAdapterTest
1. consultarSesionesPorGrupoDelegaAJpaCuandoEstaConfigurado:
   - Mockear SesionJpaQueryPersistence.
   - Invocar dapter.consultarSesionesPorGrupo(grupoId).
   - Verificar delegación a jpaQueryPersistence.consultarSesionesPorGrupo(grupoId).
   - Verificar erifyNoMoreInteractions(jdbcOperations).
2. consultarSesionesPorGrupo_con_fallback_jdbc:
   - Sin SesionJpaQueryPersistence (null).
   - Verificar ejecución mediante NamedParameterJdbcOperations.
