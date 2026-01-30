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

class RegionValidationTest {

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
    @DisplayName("Valid region should be accepted")
    void shouldAcceptValidRegion() {
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0),
            new Position(0.0, 0.0)
        };
        Region region = new Region("Appleton Inc.", vertices);

        Set<ConstraintViolation<Region>> violations = validator.validate(region);

        assertTrue(violations.isEmpty(), "Valid region should not have violations");
    }

    @Test
    @DisplayName("Null name should be rejected")
    void shouldRejectNullName() {
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0),
            new Position(0.0, 0.0)
        };
        Region region = new Region(null, vertices);

        Set<ConstraintViolation<Region>> violations = validator.validate(region);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());

        ConstraintViolation<Region> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Null vertices should be rejected")
    void shouldRejectNullVertices() {
        Region region = new Region("Appleton Inc.", null);

        Set<ConstraintViolation<Region>> violations = validator.validate(region);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());

        ConstraintViolation<Region> violation = violations.iterator().next();
        assertEquals("vertices", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("Polygon with less than 4 vertices should be rejected")
    void shouldRejectPolygonWithLessThanFourVertices() {
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0)
        };
        Region region = new Region("Appleton Inc.", vertices);

        Set<ConstraintViolation<Region>> violations = validator.validate(region);

        assertFalse(violations.isEmpty());

        ConstraintViolation<Region> violation = violations.iterator().next();
        assertEquals("vertices", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must have at least 4 vertices"));
    }

    @Test
    @DisplayName("Polygon that is not closed should be rejected")
    void shouldRejectPolygonThatIsNotClosed() {
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 1.0),
            new Position(1.0, 1.0),
            new Position(1.0, 0.0)
        };
        Region region = new Region("Appleton Inc.", vertices);

        Set<ConstraintViolation<Region>> violations = validator.validate(region);

        assertFalse(violations.isEmpty());

        ConstraintViolation<Region> violation = violations.iterator().next();
        assertEquals("vertices", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("must have at least 4 vertices"));
    }

    @Test
    @DisplayName("Polygon with invalid positions should be rejected")
    void shouldRejectPolygonWithInvalidPositions() {
        Position[] vertices = new Position[] {
            new Position(0.0, 0.0),
            new Position(0.0, 100.0),
            new Position(1.0, 1.0),
            new Position(0.0, 0.0)
        };
        Region region = new Region("Appleton Inc.", vertices);

        Set<ConstraintViolation<Region>> violations = validator.validate(region);

        assertFalse(violations.isEmpty());

        boolean hasLatViolation = false;
        for (ConstraintViolation<Region> violation : violations) {
            String path = violation.getPropertyPath().toString();
            if (path.contains("vertices") && path.contains("lat")) {
                hasLatViolation = true;
                break;
            }
        }

        assertTrue(hasLatViolation, "Should have validation error for invalid position latitude");
    }
}
