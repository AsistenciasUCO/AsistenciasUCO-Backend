package co.edu.uco.asistenciasuco.karate;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/**
 * Runner principal de Karate para validación de criterios de aceptación de Historias de Usuario (HUs).
 * Inicia el contexto Spring Boot con puerto dinámico para no colisionar con puertos fijos ni depender de servicios externos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "asistencias.swagger-ui.enabled=true",
        "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=gestionasistenciadb;encrypt=true;trustServerCertificate=true"
})
class KarateRunnerTest {

    @LocalServerPort
    private int port;

    @Karate.Test
    Karate testAll() {
        return Karate.run("classpath:karate")
                .systemProperty("server.port", String.valueOf(port))
                .relativeTo(getClass());
    }
}
