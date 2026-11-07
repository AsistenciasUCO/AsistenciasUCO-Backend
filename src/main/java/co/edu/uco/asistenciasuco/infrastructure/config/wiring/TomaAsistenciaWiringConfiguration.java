package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.primaryports.ConsultarResumenAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.usecase.impl.ConsultarResumenAsistenciaGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.usecase.impl.AbrirTomaAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.usecase.impl.CerrarTomaAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.primaryports.CerrarTomasVencidasInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.usecase.impl.CerrarTomasVencidasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.SesionTitularAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.primaryports.ConsultarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.usecase.impl.ConsultarAsistenciasTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.primaryports.ConsultarFaltantesTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.usecase.impl.ConsultarFaltantesTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports.ConsultarTomasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.usecase.impl.ConsultarTomasSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.usecase.impl.RegistrarAsistenciasTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de las tomas de asistencia y del resumen por horas (CULM-001 B.2, DEC-013). */
@Configuration(proxyBeanMethods = false)
public class TomaAsistenciaWiringConfiguration {

    @Bean
    public TomaAsistenciaAcceso tomaAsistenciaAcceso(final TomaAsistenciaRepositoryPort repositorio, final InstitutionalScopePort scope) {
        return new TomaAsistenciaAcceso(repositorio, scope);
    }

    @Bean
    public SesionTitularAcceso sesionTitularAcceso(final SesionProcedenciaQueryPort sesiones, final InstitutionalScopePort scope) {
        return new SesionTitularAcceso(sesiones, scope);
    }

    @Bean
    public TomaAsistenciaEventos tomaAsistenciaEventos(final RealtimePublisherPort realtimePublisherPort) {
        return new TomaAsistenciaEventos(realtimePublisherPort);
    }

    @Bean
    public AbrirTomaAsistenciaInputPort abrirTomaAsistenciaInputPort(final SesionTitularAcceso sesiones,
                                                                     final TomaAsistenciaRepositoryPort repositorio,
                                                                     final TomaAsistenciaAcceso acceso,
                                                                     final TomaAsistenciaEventos eventos) {
        return new AbrirTomaAsistenciaUseCaseImpl(sesiones, repositorio, acceso, eventos);
    }

    @Bean
    public ConsultarTomasSesionInputPort consultarTomasSesionInputPort(final SesionTitularAcceso sesiones,
                                                                       final TomaAsistenciaRepositoryPort repositorio) {
        return new ConsultarTomasSesionUseCaseImpl(sesiones, repositorio);
    }

    @Bean
    public CerrarTomaAsistenciaInputPort cerrarTomaAsistenciaInputPort(final TomaAsistenciaAcceso acceso,
                                                                       final TomaAsistenciaRepositoryPort repositorio,
                                                                       final TomaAsistenciaEventos eventos) {
        return new CerrarTomaAsistenciaUseCaseImpl(acceso, repositorio, eventos);
    }

    @Bean
    public RegistrarAsistenciasTomaInputPort registrarAsistenciasTomaInputPort(final TomaAsistenciaAcceso acceso,
                                                                               final TomaAsistenciaRepositoryPort repositorio,
                                                                               final TomaAsistenciaEventos eventos) {
        return new RegistrarAsistenciasTomaUseCaseImpl(acceso, repositorio, eventos);
    }

    @Bean
    public ConsultarAsistenciasTomaInputPort consultarAsistenciasTomaInputPort(final TomaAsistenciaAcceso acceso,
                                                                               final TomaAsistenciaRepositoryPort repositorio) {
        return new ConsultarAsistenciasTomaUseCaseImpl(acceso, repositorio);
    }

    @Bean
    public ConsultarFaltantesTomaInputPort consultarFaltantesTomaInputPort(final TomaAsistenciaAcceso acceso,
                                                                           final TomaAsistenciaRepositoryPort repositorio) {
        return new ConsultarFaltantesTomaUseCaseImpl(acceso, repositorio);
    }

    @Bean
    public CerrarTomasVencidasInputPort cerrarTomasVencidasInputPort(final TomaAsistenciaRepositoryPort repositorio,
                                                                     final TomaAsistenciaEventos eventos) {
        return new CerrarTomasVencidasUseCaseImpl(repositorio, eventos);
    }

    @Bean
    public ConsultarResumenAsistenciaGrupoInputPort consultarResumenAsistenciaGrupoInputPort(final InstitutionalScopePort scope,
                                                                                             final ResumenAsistenciaQueryPort resumen) {
        return new ConsultarResumenAsistenciaGrupoUseCaseImpl(scope, resumen);
    }
}
