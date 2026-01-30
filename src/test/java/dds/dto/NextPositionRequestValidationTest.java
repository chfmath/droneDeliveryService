package dds.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NextPositionRequestValidationTest {

    private Validator validator;
    private ValidatorFactory factory;

    @BeforeEach
    void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("Valid next position request should be accepted")
    void shouldAcceptValidNextPositionRequest() {
        Position start = new Position(0.0, 0.0);
        Double angle = 45.0;
        NextPositionRequest request = new NextPositionRequest(start, angle);

        Set<ConstraintViolation<NextPositionRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Valid next position request should not have violations");
    }

    @Test
    @DisplayName("Null start position should be rejected")
    void shouldRejectNullStartPosition() {
        Double angle = 45.0;
        NextPositionRequest request = new NextPositionRequest(null, angle);

        Set<ConstraintViolation<NextPositionRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());

        ConstraintViolation<NextPositionRequest> violation = violations.iterator().next();
        assertEquals("start", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Null angle should be rejected")
    void shouldRejectNullAngle() {
        Position start = new Position(0.0, 0.0);
        NextPositionRequest request = new NextPositionRequest(start, null);

        Set<ConstraintViolation<NextPositionRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size(), "Expected exactly 2 violations for a null angle");

        boolean hasNullAngleViolation = false;
        boolean hasValidAngleViolation = false;

        for (ConstraintViolation<NextPositionRequest> violation : violations) {
            String path = violation.getPropertyPath().toString();
            String message = violation.getMessage();

            if (path.equals("angle") && message.contains("must not be null")) {
                hasNullAngleViolation = true;
            } else if (path.equals("angle") && message.contains("valid drone directions")) {
                hasValidAngleViolation = true;
            }
        }

        assertTrue(hasNullAngleViolation, "Should have validation error for null angle (NotNull constraint)");
        assertTrue(hasValidAngleViolation, "Should have validation error for null angle (ValidDroneAngle constraint)");
    }

    @Test
    @DisplayName("Invalid angle should be rejected")
    void shouldRejectInvalidAngle() {
        Position start = new Position(0.0, 0.0);
        Double invalidAngle = 30.0; // Not a multiple of 22.5
        NextPositionRequest request = new NextPositionRequest(start, invalidAngle);

        Set<ConstraintViolation<NextPositionRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
    }
}
