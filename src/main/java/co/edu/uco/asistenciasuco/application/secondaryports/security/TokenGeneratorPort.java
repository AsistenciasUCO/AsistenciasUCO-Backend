package co.edu.uco.asistenciasuco.application.secondaryports.security;

/**
 * Puerto secundario para generar credenciales efimeras de matricula: token de alta entropia y PIN corto. El hash
 * SHA-256 con el que se persiste lo calcula el adaptador de persistencia ({@code TokenHashHelper}).
 */
public interface TokenGeneratorPort {

    /** Token aleatorio de 256 bits en Base64URL sin relleno (43 caracteres). */
    String generarToken();

    /** PIN numerico de 6 digitos, con ceros a la izquierda si hace falta. */
    String generarPin();
}
