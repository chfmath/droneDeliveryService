package dds.controller;

import dds.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;

import jakarta.validation.Valid;

import dds.service.DeliveryHistoryService;
import dds.service.DeliveryPlanningService;
import dds.service.DroneService;
import dds.service.GeoJsonService;
import dds.service.LocationService;
import dds.service.RouteComparisonService;

/**
 * Controller class that handles various HTTP endpoints for the application.
 * Provides functionality for serving the index page, retrieving a static UUID,
 * and managing key-value pairs through POST requests.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Delivery API", description = "Endpoints for drone delivery planning and intelligence")
public class ServiceController {

    @Value("${ilp.service.url}")
    public URL serviceUrl;

    private final LocationService locationService;
    private final DroneService droneService;
    private final DeliveryPlanningService deliveryPlanningService;
    private final GeoJsonService geoJsonService;
    private final RouteComparisonService routeComparisonService;
    private final DeliveryHistoryService deliveryHistoryService;

    @Autowired
    public ServiceController(LocationService locationService,
            DroneService droneService,
            DeliveryPlanningService deliveryPlanningService,
            GeoJsonService geoJsonService,
            RouteComparisonService routeComparisonService,
            DeliveryHistoryService deliveryHistoryService) {
        this.locationService = locationService;
        this.droneService = droneService;
        this.deliveryPlanningService = deliveryPlanningService;
        this.geoJsonService = geoJsonService;
        this.routeComparisonService = routeComparisonService;
        this.deliveryHistoryService = deliveryHistoryService;
    }

    @GetMapping("/")
    public String index() {
        return "<html><body>" +
                "<h1>Welcome from ILP</h1>" +
                "<h4>ILP-REST-Service-URL:</h4> <a href=\"" + serviceUrl +
                "\" target=\"_blank\"> " + serviceUrl + " </a>" +
                "</body></html>";
    }

    @GetMapping("/demo")
    public String demo() {
        return "demo";
    }

    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@Valid @RequestBody DistanceRequest request) {
        double distance = locationService.calculateDistance(request.getPosition1(),
                request.getPosition2());
        return ResponseEntity.ok(distance);
    }

    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@Valid @RequestBody DistanceRequest request) {
        boolean close = locationService.isCloseTo(request.getPosition1(), request.getPosition2());
        return ResponseEntity.ok(close);
    }

    @PostMapping("/nextPosition")
    public ResponseEntity<Position> nextPosition(@Valid @RequestBody NextPositionRequest request) {
        Position nextPos = locationService.nextPosition(request.getStart(), request.getAngle());
        return ResponseEntity.ok(nextPos);
    }

    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> isInRegion(@Valid @RequestBody RegionRequest request) {
        boolean inside = locationService.isInRegion(request.getPosition(), request.getRegion().getVertices());
        return ResponseEntity.ok(inside);
    }

    @GetMapping("/dronesWithCooling/{state}")
    public ResponseEntity<List<String>> getDronesWithCooling(@PathVariable boolean state) {
        List<String> droneIds = droneService.getDronesWithCooling(state);
        return ResponseEntity.ok(droneIds);
    }

    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<Drone> getDroneDetails(@PathVariable String id) {
        Drone drone = droneService.getDroneById(id);
        if (drone == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(drone);
    }

    @GetMapping("/queryAsPath/{attribute}/{value}")
    public ResponseEntity<List<String>> queryAsPath(@PathVariable String attribute,
            @PathVariable String value) {
        return ResponseEntity.ok(droneService.queryBySingleCapability(attribute, value));
    }

    @PostMapping("/query")
    public ResponseEntity<List<String>> query(@RequestBody List<QueryAttribute> attributes) {
        return ResponseEntity.ok(droneService.queryByCapabilities(attributes));
    }

    @PostMapping("/queryAvailableDrones")
    public ResponseEntity<List<String>> queryAvailableDrones(
            @Valid @RequestBody List<MedDispatchRec> dispatches) {
        return ResponseEntity.ok(droneService.queryAvailableDrones(dispatches));
    }

    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<DeliveryPathResponse> calcDeliveryPath(
            @Valid @RequestBody List<MedDispatchRec> dispatches,
            @RequestParam(required = false) String droneIdToBeUsed) {
        DeliveryPathResponse response = deliveryPlanningService.calcDeliveryPath(dispatches, droneIdToBeUsed);

        // Log the delivery attempt
        if (response != null && response.getDronePaths() != null && !response.getDronePaths().isEmpty()) {
            deliveryHistoryService.logSuccess(dispatches, response);
        } else {
            // Log failure if calcDeliveryPath returns null or empty paths
            deliveryHistoryService.logFailure(dispatches);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/calcDeliveryPathAsGeoJson")
    public ResponseEntity<GeoJsonLineString> calcDeliveryPathAsGeoJson(
            @Valid @RequestBody List<MedDispatchRec> dispatches) {
        return ResponseEntity.ok(geoJsonService.calcDeliveryPathAsGeoJson(dispatches));
    }

    @Operation(summary = "Compare route options", description = "Compare all available drone options for a delivery. Returns ranked options showing fastest, cheapest, and recommended choices with explanations. Set includePaths=false for a cleaner response.")
    @PostMapping("/compareRoutes")
    public ResponseEntity<RouteComparisonResponse> compareRoutes(
            @Valid @RequestBody List<MedDispatchRec> dispatches,
            @RequestParam(defaultValue = "false") boolean includePaths) {
        RouteComparisonResponse response = routeComparisonService.compareRoutes(dispatches);
        if (!includePaths) {
            stripFlightPaths(response);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get delivery statistics", description = "Get aggregated statistics about all delivery attempts including success rate, costs, failure breakdown, and most used drone.")
    @GetMapping("/deliveries/stats")
    public ResponseEntity<DeliveryStats> getDeliveryStats() {
        return ResponseEntity.ok(deliveryHistoryService.getStats());
    }

    @Operation(summary = "Get recent delivery history", description = "Get the 20 most recent delivery attempts with their outcomes, costs, and drone usage.")
    @GetMapping("/deliveries/history")
    public ResponseEntity<List<DeliveryHistoryItem>> getDeliveryHistory() {
        return ResponseEntity.ok(deliveryHistoryService.getRecentAttempts());
    }

    @Operation(summary = "Get data as GeoJSON", description = "Returns a GeoJSON FeatureCollection containing restricted areas (Polygons), service point locations (Points with aggregated drone statistics), and recent delivery locations (Points). "
            +
            "Service points include: total drone count, cooling drone count, heating drone count, and max maxMoves. " +
            "Optional query parameters: dayOfWeek (e.g., MONDAY) and time (e.g., 14:30 or 14:30:00) to filter drone counts by availability. Useful for visualization on maps.")
    @GetMapping("/data/geojson")
    public ResponseEntity<GeoJsonFeatureCollection> getDataAsGeoJson(
            @Parameter(description = "Day of week", example = "MONDAY") @RequestParam(required = false) java.time.DayOfWeek dayOfWeek,
            @Parameter(description = "Time in HH:mm or HH:mm:ss format (e.g., 14:30 or 14:30:00)", schema = @Schema(type = "string", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$", example = "14:30")) @RequestParam(required = false) String time) {
        java.time.LocalTime localTime = null;
        if (time != null && !time.isEmpty()) {
            try {
                // Try parsing as HH:mm or HH:mm:ss
                if (time.length() == 5) {
                    // Format: HH:mm
                    localTime = java.time.LocalTime.parse(time, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    // Format: HH:mm:ss or HH:mm:ss.SSS
                    localTime = java.time.LocalTime.parse(time);
                }
            } catch (Exception e) {
                // If parsing fails, return bad request
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(geoJsonService.getDataAsGeoJson(dayOfWeek, localTime));
    }

    /**
     * Remove flight paths from route comparison response for cleaner output.
     */
    private void stripFlightPaths(RouteComparisonResponse response) {
        if (response == null || response.getOptions() == null)
            return;
        response.getOptions().forEach(opt -> opt.setFlightPath(null));
        if (response.getFastest() != null)
            response.getFastest().setFlightPath(null);
        if (response.getCheapest() != null)
            response.getCheapest().setFlightPath(null);
        if (response.getRecommended() != null)
            response.getRecommended().setFlightPath(null);
    }

}
