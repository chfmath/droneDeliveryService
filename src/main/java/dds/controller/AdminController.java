package dds.controller;

import dds.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dds.service.DataSeederService;
import dds.service.PostgresDataService;

import java.util.List;

/**
 * Admin controller for CRUD operations on DDS data.
 * Allows creating, reading, updating, and deleting drones, service points,
 * restricted areas, and drone availability.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final PostgresDataService postgresDataService;
    private final DataSeederService dataSeederService;

    public AdminController(PostgresDataService postgresDataService, DataSeederService dataSeederService) {
        this.postgresDataService = postgresDataService;
        this.dataSeederService = dataSeederService;
    }

    // ==================== DRONE ENDPOINTS ====================

    @Operation(summary = "List drones", description = "Get all drones stored in PostgreSQL", tags = {"Admin - Drones"})
    @GetMapping("/drones")
    public ResponseEntity<List<Drone>> getAllDrones() {
        return ResponseEntity.ok(postgresDataService.getAllDrones());
    }

    @Operation(summary = "Get drone", description = "Get a specific drone by ID", tags = {"Admin - Drones"})
    @GetMapping("/drones/{id}")
    public ResponseEntity<Drone> getDrone(@PathVariable String id) {
        Drone drone = postgresDataService.getDroneById(id);
        if (drone == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(drone);
    }

    @Operation(summary = "Create drone (basic)", description = "Create a drone without availability. Use /drones/full for the one-step workflow.", tags = {"Admin - Drones"})
    @PostMapping("/drones")
    public ResponseEntity<Drone> createDrone(@RequestBody @Valid Drone drone) {
        Drone saved = postgresDataService.saveDrone(drone);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Update drone", description = "Update an existing drone by ID", tags = {"Admin - Drones"})
    @PutMapping("/drones/{id}")
    public ResponseEntity<Drone> updateDrone(@PathVariable String id, @RequestBody Drone drone) {
        if (postgresDataService.getDroneById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        drone.setId(id);  // Ensure ID matches path
        Drone saved = postgresDataService.saveDrone(drone);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Delete drone", description = "Delete a drone and its availability", tags = {"Admin - Drones"})
    @DeleteMapping("/drones/{id}")
    public ResponseEntity<Void> deleteDrone(@PathVariable String id) {
        if (postgresDataService.getDroneById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        // Also delete availability for this drone
        postgresDataService.deleteAvailabilityByDroneId(id);
        postgresDataService.deleteDrone(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Create a drone with availability in one step.
     * This is a convenience endpoint that creates the drone AND sets its availability
     * at a service point in a single request.
     */
    @Operation(
        summary = "Create drone associated to service point with availability",
        description = "Creates a new drone and assigns it to a service point with availability windows in one step. " +
                      "This is the recommended way to add a new drone to the fleet.",
        tags = {"Admin - Drones"}
    )
    @PostMapping("/drones/full")
    public ResponseEntity<Drone> createDroneWithAvailability(
            @Valid @RequestBody CreateDroneWithAvailabilityRequest request) {
        
        // Check if service point exists
        if (postgresDataService.getServicePointById(request.getServicePointId()) == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Create the drone
        Drone drone = new Drone(
            request.getId(),
            request.getName(),
            request.getCapability()
        );
        Drone saved = postgresDataService.saveDrone(drone);
        
        // Set the availability
        postgresDataService.saveAvailability(
            request.getId(),
            request.getServicePointId(),
            request.getAvailability()
        );
        
        return ResponseEntity.ok(saved);
    }

    // ==================== SERVICE POINT ENDPOINTS ====================

    @Operation(summary = "List service points", description = "Get all service points stored in PostgreSQL", tags = {"Admin - Service Points"})
    @GetMapping("/servicePoints")
    public ResponseEntity<List<ServicePoint>> getAllServicePoints() {
        return ResponseEntity.ok(postgresDataService.getServicePoints());
    }

    @Operation(summary = "Get service point", description = "Get a specific service point by ID", tags = {"Admin - Service Points"})
    @GetMapping("/servicePoints/{id}")
    public ResponseEntity<ServicePoint> getServicePoint(@PathVariable Integer id) {
        ServicePoint sp = postgresDataService.getServicePointById(id);
        if (sp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sp);
    }

    @Operation(summary = "Create service point", description = "Create a new service point", tags = {"Admin - Service Points"})
    @PostMapping("/servicePoints")
    public ResponseEntity<ServicePoint> createServicePoint(@RequestBody @Valid ServicePoint servicePoint) {
        ServicePoint saved = postgresDataService.saveServicePoint(servicePoint);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Update service point", description = "Update an existing service point", tags = {"Admin - Service Points"})
    @PutMapping("/servicePoints/{id}")
    public ResponseEntity<ServicePoint> updateServicePoint(@PathVariable Integer id, @RequestBody @Valid ServicePoint servicePoint) {
        if (postgresDataService.getServicePointById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        servicePoint.setId(id);  // Ensure ID matches path
        ServicePoint saved = postgresDataService.saveServicePoint(servicePoint);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Delete service point", description = "Delete a service point", tags = {"Admin - Service Points"})
    @DeleteMapping("/servicePoints/{id}")
    public ResponseEntity<Void> deleteServicePoint(@PathVariable Integer id) {
        if (postgresDataService.getServicePointById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        postgresDataService.deleteServicePoint(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== RESTRICTED AREA ENDPOINTS ====================

    @Operation(summary = "List restricted areas", description = "Get all restricted areas", tags = {"Admin - Restricted Areas"})
    @GetMapping("/restrictedAreas")
    public ResponseEntity<List<RestrictedArea>> getAllRestrictedAreas() {
        return ResponseEntity.ok(postgresDataService.getRestrictedAreas());
    }

    @Operation(summary = "Get restricted area", description = "Get a specific restricted area by ID", tags = {"Admin - Restricted Areas"})
    @GetMapping("/restrictedAreas/{id}")
    public ResponseEntity<RestrictedArea> getRestrictedArea(@PathVariable Integer id) {
        RestrictedArea area = postgresDataService.getRestrictedAreaById(id);
        if (area == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(area);
    }

    @Operation(summary = "Create restricted area", description = "Create a new restricted area", tags = {"Admin - Restricted Areas"})
    @PostMapping("/restrictedAreas")
    public ResponseEntity<RestrictedArea> createRestrictedArea(@RequestBody @Valid RestrictedArea area) {
        RestrictedArea saved = postgresDataService.saveRestrictedArea(area);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Update restricted area", description = "Update an existing restricted area", tags = {"Admin - Restricted Areas"})
    @PutMapping("/restrictedAreas/{id}")
    public ResponseEntity<RestrictedArea> updateRestrictedArea(@PathVariable Integer id, @RequestBody @Valid RestrictedArea area) {
        if (postgresDataService.getRestrictedAreaById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        area.setId(id);  // Ensure ID matches path
        RestrictedArea saved = postgresDataService.saveRestrictedArea(area);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Delete restricted area", description = "Delete a restricted area", tags = {"Admin - Restricted Areas"})
    @DeleteMapping("/restrictedAreas/{id}")
    public ResponseEntity<Void> deleteRestrictedArea(@PathVariable Integer id) {
        if (postgresDataService.getRestrictedAreaById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        postgresDataService.deleteRestrictedArea(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== DRONE AVAILABILITY ENDPOINTS ====================

    @Operation(summary = "List availability", description = "Get all drone availability grouped by service point", tags = {"Admin - Availability"})
    @GetMapping("/availability")
    public ResponseEntity<List<DroneForServicePoint>> getAllAvailability() {
        return ResponseEntity.ok(postgresDataService.getDronesForServicePoints());
    }

    @Operation(summary = "Set availability", description = "Set availability for a drone at a service point (replaces existing)", tags = {"Admin - Availability"})
    @PostMapping("/availability/{droneId}/{servicePointId}")
    public ResponseEntity<Void> setAvailability(
            @PathVariable String droneId,
            @PathVariable Integer servicePointId,
            @RequestBody List<@Valid DroneAvailabilityWindow> windows) {
        postgresDataService.saveAvailability(droneId, servicePointId, windows);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete availability", description = "Delete all availability entries for a drone", tags = {"Admin - Availability"})
    @DeleteMapping("/availability/{droneId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable String droneId) {
        postgresDataService.deleteAvailabilityByDroneId(droneId);
        return ResponseEntity.noContent().build();
    }

    // ==================== UTILITY ENDPOINTS ====================

    @Operation(summary = "Re-seed database", description = "Clear all fleet data (drones, service points, restricted areas, availability) and re-populate from Azure API. Note: delivery_attempts analytics are preserved.", tags = {"Admin - Utilities"})
    @PostMapping("/reseed")
    public ResponseEntity<String> reseedDatabase() {
        dataSeederService.reseedAllData();
        return ResponseEntity.ok("Database re-seeded from Azure API");
    }
}

