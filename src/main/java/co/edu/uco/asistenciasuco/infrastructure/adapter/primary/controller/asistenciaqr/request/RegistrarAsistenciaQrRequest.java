package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.ArrayList;
import java.util.List;

/**
 * CULM-001 B.2 (HU176, contrato 3.2.7): cuerpo nuevo del registro por QR: exactamente uno de {@code token} o {@code pin}.
 * Cualquier otro campo (los antiguos {@code sesion}, {@code sesionId}, {@code codigo}, {@code codigoAcceso}) se recoge
 * aparte y la validacion lo rechaza con 400. Nunca se registra en logs: lleva el token.
 */
public final class RegistrarAsistenciaQrRequest {

    private String token;
    private String pin;
    private final List<String> camposDesconocidos = new ArrayList<>();

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(final String pin) {
        this.pin = pin;
    }

    @JsonAnySetter
    public void campoDesconocido(final String nombre, final Object valor) {
        camposDesconocidos.add(nombre);
    }

    public List<String> camposDesconocidos() {
        return List.copyOf(camposDesconocidos);
    }
}
