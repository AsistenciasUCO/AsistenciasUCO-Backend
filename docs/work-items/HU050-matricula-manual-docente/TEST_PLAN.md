# TEST PLAN — HU050: registrarEstudianteEnGrupo (JPA Migration + Enrolamiento)

Fase 03-tester-red.

## 1. Alcance de Pruebas
1. **Persistencia JPA (GrupoJpaCommandPersistence):**
   - Ejecución de dbo.usp_registrar_estudiante_en_grupo mediante EntityManager.createStoredProcedureQuery.
   - Registro y mapeo de los 11 parámetros en orden exacto (idGrupo, numeroIdentificacion, primerNombre, segundoNombre, primerApellido, segundoApellido, correo, password, idCorrelacion, idUsuarioEjecutor, idTipoIdIdentificacion).
   - Mapeo canónico del ResultSet (4 columnas: idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado).
   - Validación de correlación con CorrelationIdContext.
   - Manejo de excepciones y traducción semántica con DbExceptionTranslator.
   - Control de parámetros nulos opcionales (segundoNombre, segundoApellido, password, idTipoIdIdentificacion).

2. **Controlador HTTP (GrupoController):**
   - Eliminar throw new FeatureUnavailableException ante la presencia de estudianteId (enrolamiento de estudiante existente).
   - Verificar que POST /api/v1/grupos/{grupoId}/estudiantes con estudiante existente responde 201 Created y ejecuta el caso de uso pasando los datos.
   - Preservar validación de errores HTTP 400 y formato.

3. **Arquitectura y Reglas Clean (CleanArchitectureRulesTest):**
   - Verificar que no haya @Entity, EntityManager, ni imports de JPA en Application/Domain.

## 2. Casos de Prueba Unitarios y de Integración

### Caso T-HU050-01: Ejecución exitosa con los 11 parámetros en JPA
- Componente: GrupoJpaCommandPersistenceTest
- Entrada: RegistrarEstudianteRepositoryDTO completo con correlación activa.
- Expectativa: Llama a createStoredProcedureQuery('dbo.usp_registrar_estudiante_en_grupo'), registra los 11 parámetros tipados (ParameterMode.IN), obtiene el ResultSet canónico con estadoResultado = true, valida correlación y retorna RegistrarEstudianteRepositoryProjection.

### Caso T-HU050-02: Manejo de error de negocio devuelto por el SP
- Componente: GrupoJpaCommandPersistenceTest
- Entrada: RegistrarEstudianteRepositoryDTO que genera conflicto (e.g. cruce de horario o matrícula duplicada en BD).
- Expectativa: DbExceptionTranslator traduce el fallo a la excepción semántica de negocio correspondiente (e.g. ConflictException).

### Caso T-HU050-03: Manejo de fallo de infraestructura JPA
- Componente: GrupoJpaCommandPersistenceTest
- Entrada: Lanzamiento de PersistenceException durante la ejecución.
- Expectativa: Captura y traduce a DatabaseOperationException preservando el correlationId en logs y sin fugas de excepciones de Hibernate.

### Caso T-HU050-04: Enrolamiento de estudiante existente en GrupoController
- Componente: GrupoControllerWriteEndpointsTest
- Entrada: Request con estudianteId != null y datos del alumno.
- Expectativa: No lanza FeatureUnavailableException. Delega al puerto y responde 201 Created.
