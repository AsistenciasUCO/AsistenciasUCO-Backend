package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Implementacion de {@link CorreoTransport} sobre Jakarta Mail / Spring Mail (CONTRACT §4.3).
 */
public class JakartaMailCorreoTransport implements CorreoTransport {

    private static final Pattern CODIGO_SMTP_5XX = Pattern.compile("(?<!\\d)5\\d{2}(?!\\d)");

    private final JavaMailSender mailSender;

    public JakartaMailCorreoTransport(final SmtpNotificationProperties properties) {
        this( construirMailSender(Objects.requireNonNull(properties, "SmtpNotificationProperties es obligatorio.")));
    }

    public JakartaMailCorreoTransport(final JavaMailSender mailSender) {
        this.mailSender = Objects.requireNonNull(mailSender, "JavaMailSender es obligatorio.");
    }

    private static JavaMailSender construirMailSender(final SmtpNotificationProperties props) {
        final JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.host());
        sender.setPort(props.port());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        final boolean tieneAuth = !props.username().isBlank();
        if (tieneAuth) {
            sender.setUsername(props.username());
            sender.setPassword(props.password());
        }

        final Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", String.valueOf(tieneAuth));
        javaMailProps.put("mail.smtp.starttls.enable", String.valueOf(props.starttls()));
        javaMailProps.put("mail.smtp.ssl.enable", String.valueOf(props.ssl()));
        final String connectTimeoutMs = String.valueOf(props.connectTimeout().toMillis());
        final String readTimeoutMs = String.valueOf(props.readTimeout().toMillis());
        javaMailProps.put("mail.smtp.connectiontimeout", connectTimeoutMs);
        javaMailProps.put("mail.smtp.timeout", readTimeoutMs);
        javaMailProps.put("mail.smtp.writetimeout", readTimeoutMs);
        javaMailProps.put("mail.smtp.localhost", "localhost");
        return sender;
    }

    @Override
    public void enviar(final CorreoSaliente correo) throws NotificacionNoEntregableException {
        Objects.requireNonNull(correo, "CorreoSaliente es obligatorio.");
        try {
            final MimeMessage base = mailSender.createMimeMessage();
            final MimeMessage message = new FixedMessageIdMimeMessage(base.getSession(), correo.messageId());
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            if (correo.fromName().isBlank()) {
                helper.setFrom(correo.fromAddress());
            } else {
                helper.setFrom(correo.fromAddress(), correo.fromName());
            }
            helper.setTo(correo.toAddress());
            if (!correo.bccAddresses().isEmpty()) {
                helper.setBcc(correo.bccAddresses().toArray(String[]::new));
            }
            helper.setSubject(correo.asunto());
            helper.setText(correo.textoPlano(), correo.html());

            mailSender.send(message);
        } catch (final MailException | MessagingException | java.io.UnsupportedEncodingException ex) {
            if (esFalloPermanente(ex)) {
                throw new NotificacionNoEntregableException("Rechazo SMTP permanente al enviar notificacion.");
            }
            throw new IllegalStateException("Fallo SMTP transitorio al enviar notificacion.", ex);
        }
    }

    static boolean esFalloPermanente(final Throwable error) {
        // Un fallo de red (host que no resuelve, conexion rechazada o expirada) es transitorio. Su mensaje incluye el puerto
        // (por ejemplo "..., 587") y no debe confundirse con una respuesta SMTP 5xx del servidor.
        for (Throwable red = error; red != null; red = red.getCause()) {
            if (red instanceof java.net.UnknownHostException
                    || red instanceof java.net.ConnectException
                    || red instanceof java.net.SocketException
                    || red instanceof java.net.SocketTimeoutException
                    || red instanceof java.io.InterruptedIOException
                    || red instanceof org.eclipse.angus.mail.util.MailConnectException) {
                return false;
            }
        }
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof AddressException) {
                return true;
            }
            if (cursor instanceof SendFailedException sfe
                    && sfe.getInvalidAddresses() != null
                    && sfe.getInvalidAddresses().length > 0
                    && (sfe.getValidUnsentAddresses() == null || sfe.getValidUnsentAddresses().length == 0)) {
                return true;
            }
            final String msg = cursor.getMessage();
            if (msg != null && CODIGO_SMTP_5XX.matcher(msg).find()) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static final class FixedMessageIdMimeMessage extends MimeMessage {
        private final String customMessageId;

        private FixedMessageIdMimeMessage(final Session session, final String customMessageId) {
            super(session);
            this.customMessageId = customMessageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", customMessageId);
        }
    }
}
