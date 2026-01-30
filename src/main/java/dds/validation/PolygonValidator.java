package dds.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import dds.dto.Position;

public class PolygonValidator implements ConstraintValidator<ValidPolygon, Position[]> {

    @Override
    public boolean isValid(Position[] vertices, ConstraintValidatorContext context) {
        if (vertices == null) {
            return true;
        }

        if (vertices.length < 4) {
            return false;
        }

        Position first = vertices[0];
        Position last = vertices[vertices.length - 1];

        return first.getLat().equals(last.getLat()) && first.getLng().equals(last.getLng());
    }
}
