package dds.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PositionValidationTest {

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

    @Nested
    @DisplayName("Longitude validation tests")
    class LongitudeRangeTests {
        @Test
        @DisplayName("Valid longitude values should be accepted")
        void shouldAcceptValidLongitudeValues() {
            Position position = new Position(0.0, 0.0);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);
            assertTrue(violations.isEmpty());

            position = new Position(-180.0, 0.0);
            violations = validator.validate(position);
            assertTrue(violations.isEmpty());

            position = new Position(180.0, 0.0);
            violations = validator.validate(position);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Longitude below minimum should be rejected")
        void shouldRejectLongitudeBelowMinimum() {
            Position position = new Position(-180.1, 0.0);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);

            assertFalse(violations.isEmpty());
            assertEquals(1, violations.size());

            ConstraintViolation<Position> violation = violations.iterator().next();
            assertEquals("lng", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("greater than or equal to -180"));
        }

        @Test
        @DisplayName("Longitude above maximum should be rejected")
        void shouldRejectLongitudeAboveMaximum() {
            Position position = new Position(180.1, 0.0);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);

            assertFalse(violations.isEmpty());
            assertEquals(1, violations.size());

            ConstraintViolation<Position> violation = violations.iterator().next();
            assertEquals("lng", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("less than or equal to 180"));
        }

        @Test
        @DisplayName("Null longitude should be rejected")
        void shouldRejectNullLongitude() {
            Position position = new Position(null, 0.0);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);

            assertFalse(violations.isEmpty());
            assertEquals(1, violations.size());

            ConstraintViolation<Position> violation = violations.iterator().next();
            assertEquals("lng", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("must not be null"));
        }
    }

    @Nested
    @DisplayName("Latitude validation tests")
    class LatitudeRangeTests {
        @Test
        @DisplayName("Valid latitude values should be accepted")
        void shouldAcceptValidLatitudeValues() {
            Position position = new Position(0.0, 0.0);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);
            assertTrue(violations.isEmpty());

            position = new Position(0.0, -90.0);
            violations = validator.validate(position);
            assertTrue(violations.isEmpty());

            position = new Position(0.0, 90.0);
            violations = validator.validate(position);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Latitude below minimum should be rejected")
        void shouldRejectLatitudeBelowMinimum() {
            Position position = new Position(0.0, -90.1);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);

            assertFalse(violations.isEmpty());
            assertEquals(1, violations.size());

            ConstraintViolation<Position> violation = violations.iterator().next();
            assertEquals("lat", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("greater than or equal to -90"));
        }

        @Test
        @DisplayName("Latitude above maximum should be rejected")
        void shouldRejectLatitudeAboveMaximum() {
            Position position = new Position(0.0, 90.1);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);

            assertFalse(violations.isEmpty());
            assertEquals(1, violations.size());

            ConstraintViolation<Position> violation = violations.iterator().next();
            assertEquals("lat", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("less than or equal to 90"));
        }

        @Test
        @DisplayName("Null latitude should be rejected")
        void shouldRejectNullLatitude() {
            Position position = new Position(0.0, null);
            Set<ConstraintViolation<Position>> violations = validator.validate(position);

            assertFalse(violations.isEmpty());
            assertEquals(1, violations.size());

            ConstraintViolation<Position> violation = violations.iterator().next();
            assertEquals("lat", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("must not be null"));
        }
    }
}
