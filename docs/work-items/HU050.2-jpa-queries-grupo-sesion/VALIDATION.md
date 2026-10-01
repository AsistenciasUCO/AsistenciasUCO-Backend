---
status: active
type: validation
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# VALIDATION — HU050.2: Validación de Pruebas y Compilación

Fase 05-auditor.

## 1. Evidencia de Ejecución de Pruebas

### 1.1 Tests Unitarios de Adaptadores (Core + JPA)
- **Comando:**
  `.\mvnw.cmd test "-Dtest=GrupoRepositorySqlServerAdapterTest,SesionRepositorySqlServerAdapterTest"`
- **Resultado:**
  - `GrupoRepositorySqlServerAdapterTest`: 15 tests, 0 failures, 0 errors, 0 skipped.
  - `SesionRepositorySqlServerAdapterTest`: 8 tests, 0 failures, 0 errors, 0 skipped.
  - **Total:** 23 tests, 0 failures, 0 errors. `BUILD SUCCESS`.

### 1.2 Tests Unitarios Directos de Persistencia JPA
- **Comando:**
  `.\mvnw.cmd test "-Dtest=GrupoJpaQueryPersistenceTest,SesionJpaQueryPersistenceTest"`
- **Resultado:**
  - `GrupoJpaQueryPersistenceTest`: 5 tests, 0 failures, 0 errors, 0 skipped.
  - `SesionJpaQueryPersistenceTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
  - **Total:** 8 tests, 0 failures, 0 errors. `BUILD SUCCESS`.

### 1.3 Pruebas de Arquitectura (ArchUnit)
- **Comando:**
  `.\mvnw.cmd test "-Dtest=co.edu.uco.asistenciasuco.architecture.**.*Test"`
- **Resultado:**
  - 85 pruebas de reglas arquitecturales ejecutadas (Clean Architecture, Aislamiento JPA, Independencia del Dominio, Restricciones de Controladores).
  - **Total:** 85 tests, 0 failures, 0 errors, 0 skipped. `BUILD SUCCESS`.

### 1.4 Verificación de Salud en Runtime
- **Endpoint:** `http://localhost:8080/actuator/health`
- **Respuesta:** `{"status":"UP"}`

### 1.5 Verificación de Compilación Frontend
- **Comando:** `npm run build`
- **Resultado:** `Application bundle generation complete. Exit code: 0`.
