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

class DistanceRequestValidationTest {

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
    @DisplayName("Valid distance request should be accepted")
    void shouldAcceptValidDistanceRequest() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(1.0, 1.0);
        DistanceRequest request = new DistanceRequest(p1, p2);

        Set<ConstraintViolation<DistanceRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Valid distance request should not have violations");
    }

    @Test
    @DisplayName("Null position1 should be rejected")
    void shouldRejectNullPosition1() {
        Position p2 = new Position(1.0, 1.0);
        DistanceRequest request = new DistanceRequest(null, p2);

        Set<ConstraintViolation<DistanceRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());

        ConstraintViolation<DistanceRequest> violation = violations.iterator().next();
        assertEquals("position1", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Null position2 should be rejected")
    void shouldRejectNullPosition2() {
        Position p1 = new Position(0.0, 0.0);
        DistanceRequest request = new DistanceRequest(p1, null);

        Set<ConstraintViolation<DistanceRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());

        ConstraintViolation<DistanceRequest> violation = violations.iterator().next();
        assertEquals("position2", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Request with invalid positions should be rejected")
    void shouldRejectInvalidPositions() {
        Position invalidP1 = new Position(200.0, 0.0);
        Position invalidP2 = new Position(0.0, 100.0);
        DistanceRequest request = new DistanceRequest(invalidP1, invalidP2);

        Set<ConstraintViolation<DistanceRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 2);

        boolean hasLngViolation = false;
        boolean hasLatViolation = false;

        for (ConstraintViolation<DistanceRequest> violation : violations) {
            String path = violation.getPropertyPath().toString();
            if (path.contains("lng")) hasLngViolation = true;
            if (path.contains("lat")) hasLatViolation = true;
        }

        assertTrue(hasLngViolation, "Should have validation error for invalid longitude");
        assertTrue(hasLatViolation, "Should have validation error for invalid latitude");
    }
}
