package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;

/**
 * Abstraccion INTERNA de Infrastructure (no es un Application Port) para el unico command de asistencia
 * que puede resolverse con una tecnologia distinta a JDBC (LB-002.2): el registro en lote por sesion.
 * Permite que el adapter hibrido delegue el command sin conocer JPA. Simetrica a
 * {@link AsistenciaQueryPersistence}.
 */
@FunctionalInterface
public interface AsistenciaCommandPersistence {

    void registrarAsistenciasSesion(RegistrarAsistenciasSesionRepositoryDTO dto);
}
