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

class RegionRequestValidationTest {

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
    @DisplayName("Valid region request should be accepted")
    void shouldAcceptValidRegionRequest() {
        Position position = new Position(0.5, 0.5);
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0),
            new Position(1.0, 0.0),
            new Position(0.0, 0.0)
        };
        Region region = new Region("Appleton Inc.", vertices);
        RegionRequest request = new RegionRequest(position, region);

        Set<ConstraintViolation<RegionRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Valid region request should not have violations");
    }

    @Test
    @DisplayName("Null position should be rejected")
    void shouldRejectNullPosition() {
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0),
            new Position(1.0, 0.0),
            new Position(0.0, 0.0)
        };
        Region region = new Region("Appleton Inc.", vertices);
        RegionRequest request = new RegionRequest(null, region);

        Set<ConstraintViolation<RegionRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());

        ConstraintViolation<RegionRequest> violation = violations.iterator().next();
        assertEquals("position", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Null region should be rejected")
    void shouldRejectNullRegion() {
        Position position = new Position(0.5, 0.5);
        RegionRequest request = new RegionRequest(position, null);

        Set<ConstraintViolation<RegionRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());

        ConstraintViolation<RegionRequest> violation = violations.iterator().next();
        assertEquals("region", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Request with invalid position should be rejected")
    void shouldRejectInvalidPosition() {
        Position invalidPosition = new Position(200.0, 0.5); // Invalid longitude
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0),
            new Position(1.0, 0.0),
            new Position(0.0, 0.0)
        };
        Region region = new Region("Appleton Inc.", vertices);
        RegionRequest request = new RegionRequest(invalidPosition, region);

        Set<ConstraintViolation<RegionRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        boolean hasLngViolation = false;
        for (ConstraintViolation<RegionRequest> violation : violations) {
            if (violation.getPropertyPath().toString().contains("lng")) {
                hasLngViolation = true;
                break;
            }
        }
        assertTrue(hasLngViolation, "Should have validation error for invalid longitude");
    }

    @Test
    @DisplayName("Request with invalid region should be rejected")
    void shouldRejectInvalidRegion() {
        Position position = new Position(0.5, 0.5);
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0)
            // Not enough vertices and not closed
        };
        Region invalidRegion = new Region("Appleton Inc.", vertices);
        RegionRequest request = new RegionRequest(position, invalidRegion);

        Set<ConstraintViolation<RegionRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        boolean hasPolygonViolation = false;
        for (ConstraintViolation<RegionRequest> violation : violations) {
            if (violation.getPropertyPath().toString().contains("vertices")) {
                hasPolygonViolation = true;
                break;
            }
        }
        assertTrue(hasPolygonViolation, "Should have validation error for invalid polygon");
    }
}
