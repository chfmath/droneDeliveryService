package dds.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class DroneAngleValidator implements ConstraintValidator<ValidDroneAngle, Double> {

    private static final Set<Double> VALID_ANGLES = Set.of(
            0.0, 22.5, 45.0, 67.5, 90.0, 112.5, 135.0, 157.5,
            180.0, 202.5, 225.0, 247.5, 270.0, 292.5, 315.0, 337.5
    );

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return VALID_ANGLES.contains(value);
    }
}
