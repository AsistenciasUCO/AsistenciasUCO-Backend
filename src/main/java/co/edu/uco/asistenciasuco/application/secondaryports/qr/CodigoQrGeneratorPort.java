package co.edu.uco.asistenciasuco.application.secondaryports.qr;

/**
 * Puerto secundario que genera la imagen de un codigo QR. El cliente nunca arma el QR ni lo delega a terceros.
 */
public interface CodigoQrGeneratorPort {

    /**
     * @param contenido texto a codificar (URL de matricula).
     * @param lado      lado del cuadrado en pixeles.
     * @return bytes de una imagen PNG.
     */
    byte[] generarPng(String contenido, int lado);
}
