package dds.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DroneAngleValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDroneAngle {
    String message() default "Angle must be one of the 16 valid drone directions (multiples of 22.5 degrees)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
