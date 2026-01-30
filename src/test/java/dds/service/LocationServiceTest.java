package dds.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import dds.dto.Position;

import static org.junit.jupiter.api.Assertions.*;

class LocationServiceTest {

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService();
    }

    @Nested
    @DisplayName("Tests for calculateDistance method")
    class CalculateDistanceTests {
        @Test
        @DisplayName("Distance between two positions should be calculated correctly")
        void shouldCalculateDistanceBetweenPositions() {
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position(3.0, 4.0);
            double expectedDistance = 5.0;

            double actualDistance = locationService.calculateDistance(p1, p2);

            assertEquals(expectedDistance, actualDistance, 0.0001,
                    "Distance calculation should match the expected value");
        }

        @Test
        @DisplayName("Distance between identical positions should be zero")
        void shouldReturnZeroForIdenticalPositions() {
            Position p = new Position(1.5, 2.5);

            double distance = locationService.calculateDistance(p, p);

            assertEquals(0.0, distance, "Distance between identical positions should be zero");
        }
    }

    @Nested
    @DisplayName("Tests for isCloseTo method")
    class IsCloseToTests {
        @Test
        @DisplayName("Positions within threshold should return true")
        void shouldReturnTrueForPositionsWithinThreshold() {
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position(0.0001, 0.0001);

            boolean result = locationService.isCloseTo(p1, p2);

            assertTrue(result, "Positions within threshold should be considered close");
        }

        @Test
        @DisplayName("Positions outside threshold should return false")
        void shouldReturnFalseForPositionsOutsideThreshold() {
            Position p1 = new Position(0.0, 0.0);
            Position p2 = new Position(0.1, 0.1);

            boolean result = locationService.isCloseTo(p1, p2);

            assertFalse(result, "Positions outside threshold should not be considered close");
        }
    }

    @Nested
    @DisplayName("Tests for nextPosition method")
    class NextPositionTests {
        @Test
        @DisplayName("Next position at 0 degrees should be calculated correctly")
        void shouldCalculateNextPositionAt0Degrees() {
            Position start = new Position(0.0, 0.0);
            double angle = 0.0;

            Position result = locationService.nextPosition(start, angle);

            assertTrue(result.getLng() > start.getLng(), "Longitude should increase when moving east");
            assertEquals(start.getLat(), result.getLat(), 0.0001, "Latitude should remain unchanged");
        }

        @Test
        @DisplayName("Next position at 90 degrees should be calculated correctly")
        void shouldCalculateNextPositionAt90Degrees() {
            Position start = new Position(0.0, 0.0);
            double angle = 90.0;

            Position result = locationService.nextPosition(start, angle);

            assertEquals(start.getLng(), result.getLng(), 0.0001, "Longitude should remain unchanged");
            assertTrue(result.getLat() > start.getLat(), "Latitude should increase when moving north");
        }

        @Test
        @DisplayName("Next position at 180 degrees should be calculated correctly")
        void shouldCalculateNextPositionAt180Degrees() {
            Position start = new Position(0.0, 0.0);
            double angle = 180.0;

            Position result = locationService.nextPosition(start, angle);

            assertTrue(result.getLng() < start.getLng(), "Longitude should decrease when moving west");
            assertEquals(start.getLat(), result.getLat(), 0.0001, "Latitude should remain unchanged");
        }

        @Test
        @DisplayName("Next position at 270 degrees should be calculated correctly")
        void shouldCalculateNextPositionAt270Degrees() {
            Position start = new Position(0.0, 0.0);
            double angle = 270.0;

            Position result = locationService.nextPosition(start, angle);

            assertEquals(start.getLng(), result.getLng(), 0.0001, "Longitude should remain unchanged");
            assertTrue(result.getLat() < start.getLat(), "Latitude should decrease when moving south");
        }

        @Test
        @DisplayName("Next position at 45 degrees should be calculated correctly")
        void shouldCalculateNextPositionAt45Degrees() {
            Position start = new Position(0.0, 0.0);
            double angle = 45.0;

            Position result = locationService.nextPosition(start, angle);

            assertTrue(result.getLng() > start.getLng(), "Longitude should increase when moving northeast");
            assertTrue(result.getLat() > start.getLat(), "Latitude should increase when moving northeast");
        }
    }

    @Nested
    @DisplayName("Tests for isInRegion method")
    class IsInRegionTests {
        @Test
        @DisplayName("Should return true for point inside polygon")
        void shouldReturnTrueForPointInsidePolygon() {
            Position point = new Position(1.0, 1.0);
            Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0)
            };

            boolean result = locationService.isInRegion(point, vertices);

            assertTrue(result, "Point inside polygon should return true");
        }

        @Test
        @DisplayName("Should return false for point outside polygon")
        void shouldReturnFalseForPointOutsidePolygon() {
            Position point = new Position(3.0, 3.0);
            Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0)
            };

            boolean result = locationService.isInRegion(point, vertices);

            assertFalse(result, "Point outside polygon should return false");
        }

        @Test
        @DisplayName("Should handle polygon vertex on point correctly")
        void shouldHandlePolygonVertexOnPointCorrectly() {
            Position point = new Position(0.0, 0.0);
            Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0)
            };

            boolean result = locationService.isInRegion(point, vertices);

            assertTrue(result, "Point on vertex should be considered inside the polygon");
        }

        @Test
        @DisplayName("Should consider point on horizontal edge as inside")
        void shouldConsiderPointOnHorizontalEdgeAsInside() {
            Position point = new Position(1.0, 0.0); // Point on bottom edge
            Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0),
                new Position(0.0, 0.0)
            };

            boolean result = locationService.isInRegion(point, vertices);

            assertTrue(result, "Point on horizontal edge should be considered inside the polygon");
        }

        @Test
        @DisplayName("Should consider point on vertical edge as inside")
        void shouldConsiderPointOnVerticalEdgeAsInside() {
            Position point = new Position(2.0, 1.0); // Point on right edge
            Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0),
                new Position(0.0, 0.0)
            };

            boolean result = locationService.isInRegion(point, vertices);

            assertTrue(result, "Point on vertical edge should be considered inside the polygon");
        }

        @Test
        @DisplayName("Should consider point on sloped edge as inside")
        void shouldConsiderPointOnSlopedEdgeAsInside() {
            Position point = new Position(1.0, 1.0); // Point on diagonal
            Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 0.0)
            };

            boolean result = locationService.isInRegion(point, vertices);

            assertTrue(result, "Point on sloped edge should be considered inside the polygon");
        }

        @Test
        @DisplayName("Should handle nearly-on-boundary cases correctly")
        void shouldHandleNearlyOnBoundaryCorrectly() {
            double epsilon = 1e-10; // Very small value
            Position pointSlightlyInside = new Position(1.0, epsilon);
            Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0),
                new Position(0.0, 0.0)
            };

            assertTrue(locationService.isInRegion(pointSlightlyInside, vertices),
                    "Point slightly inside should be considered inside");
        }

        @Test
        @DisplayName("Should handle complex polygons correctly")
        void shouldHandleComplexPolygonsCorrectly() {
            Position insidePoint = new Position(2.5, 1.5);
            Position outsidePoint = new Position(0.5, 1.5);
            Position[] vertices = {
                new Position(1.0, 0.0),
                new Position(3.0, 0.0),
                new Position(3.0, 3.0),
                new Position(1.0, 3.0),
                new Position(1.0, 2.0),
                new Position(2.0, 2.0),
                new Position(2.0, 1.0),
                new Position(1.0, 1.0),
                new Position(1.0, 0.0)
            };

            assertTrue(locationService.isInRegion(insidePoint, vertices),
                    "Point inside concave region should be considered inside");
            assertFalse(locationService.isInRegion(outsidePoint, vertices),
                    "Point in concave 'opening' should be considered outside");
        }
    }
}
