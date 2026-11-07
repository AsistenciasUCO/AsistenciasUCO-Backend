package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CULM-001 A.2-9 (A2-B-03): DbCodeTranslator es la unica via para los DBCODE de los SP de consulta. Los codigos que no
 * lista de forma explicita se traducen con la tabla de DbFailureClassifier (SEC_002 -> 403, NTF_002 -> 404...). La
 * sobrecarga de JpaQueryExecutor con traductor lanza la excepcion de negocio y deja el resto como fallo tecnico.
 */
class DbCodeTranslatorTest {

    private static PersistenceException con(final String mensaje) {
        return new PersistenceException("wrapper", new RuntimeException(mensaje));
    }

    @Test
    void sec_002_de_un_sp_de_consulta_es_403() {
        assertInstanceOf(ForbiddenException.class, DbCodeTranslator.traducir(con("DBCODE=SEC_002|fuera de ambito")));
    }

    @Test
    void ntf_002_rev_006_son_404_y_rev_007_dec_002_son_409() {
        assertInstanceOf(ResourceNotFoundException.class, DbCodeTranslator.traducir(con("DBCODE=NTF_002|no existe")));
        assertInstanceOf(ResourceNotFoundException.class, DbCodeTranslator.traducir(con("DBCODE=REV_006|ajeno")));
        assertInstanceOf(ConflictException.class, DbCodeTranslator.traducir(con("DBCODE=REV_007|no pendiente")));
        assertInstanceOf(ConflictException.class, DbCodeTranslator.traducir(con("DBCODE=DEC_002|programas activos")));
    }

    @Test
    void pag_001_y_val_001_son_400_y_ses_017_tambien() {
        assertInstanceOf(ValidationException.class, DbCodeTranslator.traducir(con("DBCODE=PAG_001|pagina")));
        assertInstanceOf(ValidationException.class, DbCodeTranslator.traducir(con("DBCODE=VAL_001|ejecutor")));
        assertInstanceOf(ValidationException.class, DbCodeTranslator.traducir(con("DBCODE=SES_017|solape")));
    }

    @Test
    void sin_marcador_o_codigo_no_clasificado_devuelve_la_misma_excepcion() {
        final PersistenceException sinMarcador = new PersistenceException("timeout");
        assertSame(sinMarcador, DbCodeTranslator.traducir(sinMarcador));
        final PersistenceException noClasificado = con("DBCODE=SYS_001|catch");
        assertSame(noClasificado, DbCodeTranslator.traducir(noClasificado));
    }

    @Test
    void la_traduccion_especifica_previa_no_cambia() {
        final RuntimeException gen = DbCodeTranslator.traducir(con("DBCODE=GEN_001|no existe"), "programa");
        assertInstanceOf(ResourceNotFoundException.class, gen);
        assertEquals("El programa especificado no existe en el sistema.", gen.getMessage());
    }

    @Test
    void la_sobrecarga_del_executor_lanza_la_excepcion_traducida() {
        final Supplier<String> falla = () -> {
            throw con("DBCODE=SEC_002|fuera de ambito");
        };
        assertThrows(ForbiddenException.class, () -> JpaQueryExecutor.execute("op", "msg", falla, DbCodeTranslator::traducir));
    }

    @Test
    void la_sobrecarga_del_executor_deja_como_fallo_tecnico_lo_no_traducido() {
        final PersistenceException causa = new PersistenceException("timeout");
        final Supplier<String> falla = () -> {
            throw causa;
        };
        final DatabaseOperationException error = assertThrows(DatabaseOperationException.class,
                () -> JpaQueryExecutor.execute("op", "msg", falla, DbCodeTranslator::traducir));
        assertSame(causa, error.getCause());
        assertEquals("msg", error.getMessage());

        final IllegalStateException estado = new IllegalStateException("em cerrado");
        final Supplier<String> otra = () -> {
            throw estado;
        };
        assertSame(estado, assertThrows(DatabaseOperationException.class,
                () -> JpaQueryExecutor.execute("op", "msg", otra, DbCodeTranslator::traducir)).getCause());
        assertEquals("ok", JpaQueryExecutor.execute("op", "msg", () -> "ok", DbCodeTranslator::traducir));
    }

    @Test
    void la_sobrecarga_con_traductor_que_devuelve_nulo_conserva_el_fallo_tecnico() {
        final Supplier<String> falla = () -> {
            throw new PersistenceException("x");
        };
        assertThrows(DatabaseOperationException.class, () -> JpaQueryExecutor.execute("op", "msg", falla, e -> null));
    }
}
