package dds.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PolygonValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidPolygon {
    String message() default "Polygon must have at least 4 vertices, be closed, and contain valid coordinates";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
