package co.edu.uco.asistenciasuco.application.secondaryports.network;

/**
 * Puerto secundario que responde si una direccion IP pertenece a la red institucional (campus UCO).
 *
 * <p>Regla institucional: la matricula y la marcacion autonomas por QR/PIN solo son validas desde la red del campus.
 * Si la red no esta configurada, el puerto debe negar (fail-closed).</p>
 */
public interface RedInstitucionalPort {

    /**
     * @param direccionIp direccion IP literal del cliente, ya resuelta por la infraestructura (nunca un nombre de host).
     * @return {@code true} solo si la direccion es valida y pertenece a los rangos del campus.
     */
    boolean perteneceACampus(String direccionIp);
}
