package dds.controller;

import dds.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import dds.service.LocationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ServiceControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private ServiceController serviceController;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    @DisplayName("Distance endpoint should calculate distance correctly")
    void shouldReturnCorrectDistance() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(5.0, 12.0);
        DistanceRequest request = new DistanceRequest();
        request.setPosition1(p1);
        request.setPosition2(p2);

        double expectedDistance = 13.0;
        when(locationService.calculateDistance(p1, p2)).thenReturn(expectedDistance);

        ResponseEntity<Double> response = serviceController.distanceTo(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedDistance, response.getBody());
        verify(locationService, times(1)).calculateDistance(p1, p2);
    }

    @Test
    @DisplayName("IsCloseTo endpoint should return correct result")
    void shouldReturnCorrectIsCloseToResult() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(0.0001, 0.0001);
        DistanceRequest request = new DistanceRequest();
        request.setPosition1(p1);
        request.setPosition2(p2);

        when(locationService.isCloseTo(p1, p2)).thenReturn(true);

        ResponseEntity<Boolean> response = serviceController.isCloseTo(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, response.getBody());
        verify(locationService, times(1)).isCloseTo(p1, p2);
    }

    @Test
    @DisplayName("NextPosition endpoint should calculate next position correctly")
    void shouldReturnCorrectNextPosition() {
        Position start = new Position(0.0, 0.0);
        double angle = 90.0;
        NextPositionRequest request = new NextPositionRequest();
        request.setStart(start);
        request.setAngle(angle);

        Position expectedPosition = new Position(0.0, 0.00015);
        when(locationService.nextPosition(start, angle)).thenReturn(expectedPosition);

        ResponseEntity<Position> response = serviceController.nextPosition(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedPosition, response.getBody());
        verify(locationService, times(1)).nextPosition(start, angle);
    }

    @Test
    @DisplayName("IsInRegion endpoint should return correct result")
    void shouldReturnCorrectIsInRegionResult() {
        Position position = new Position(1.0, 1.0);
        Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0)
        };
        RegionRequest request = new RegionRequest();
        request.setPosition(position);
        Region region = new Region("Test Region", vertices);
        request.setRegion(region);

        when(locationService.isInRegion(position, vertices)).thenReturn(true);

        ResponseEntity<Boolean> response = serviceController.isInRegion(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, response.getBody());
        verify(locationService, times(1)).isInRegion(position, vertices);
    }
}
