package co.edu.uco.asistenciasuco.infrastructure.audit.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableOperation {

    String action();

    String resourceType();

    String resourceIdPathVariable() default "";

    String resourceIdRequestField() default "";

    String resourceIdResponseField() default "";

    /**
     * CULM-001 D.6: campos del cuerpo de la peticion que se copian a {@code metadata} (valor saneado, 240 caracteres como
     * maximo). Los nombres sensibles ({@code password}, {@code clave_inicial}, {@code token}...) nunca se copian.
     */
    String[] metadataRequestFields() default {};

    /** CULM-001 D.6: igual que {@link #metadataRequestFields()} para el cuerpo de la respuesta (sobre {@code datos} si existe). */
    String[] metadataResponseFields() default {};
}
