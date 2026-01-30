package dds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dds.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dds.entity.DeliveryAttemptEntity;
import dds.repository.DeliveryAttemptRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeoJsonService {

    private final DeliveryPlanningService deliveryPlanningService;
    private final UnifiedDataService unifiedDataService;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ObjectMapper objectMapper;

    public GeoJsonService(DeliveryPlanningService deliveryPlanningService,
            UnifiedDataService unifiedDataService,
            DeliveryAttemptRepository deliveryAttemptRepository,
            ObjectMapper objectMapper) {
        this.deliveryPlanningService = deliveryPlanningService;
        this.unifiedDataService = unifiedDataService;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.objectMapper = objectMapper;
    }

    public GeoJsonLineString calcDeliveryPathAsGeoJson(List<MedDispatchRec> dispatches) {
        DeliveryPathResponse pathResponse = deliveryPlanningService.calcDeliveryPath(dispatches, null);
        return buildLineString(pathResponse);
    }

    private GeoJsonLineString buildLineString(DeliveryPathResponse pathResponse) {
        if (pathResponse == null
                || pathResponse.getDronePaths() == null
                || pathResponse.getDronePaths().size() != 1) {
            return new GeoJsonLineString(new ArrayList<>());
        }

        DronePath dronePath = pathResponse.getDronePaths().getFirst();
        List<double[]> coordinates = new ArrayList<>();

        for (Delivery delivery : dronePath.getDeliveries()) {
            if (delivery.getFlightPath() == null || delivery.getFlightPath().isEmpty()) {
                continue;
            }

            for (Position pos : delivery.getFlightPath()) {
                coordinates.add(new double[] { pos.getLng(), pos.getLat() });
            }
        }

        return new GeoJsonLineString(coordinates);
    }

    /**
     * Generate a GeoJSON FeatureCollection containing:
     * - Restricted areas (as Polygons)
     * - Service point locations (as Points) with aggregated drone statistics
     * - Recent delivery locations (as Points)
     * Service point features include: total drone count, cooling drone count,
     * heating drone count, and max maxMoves.
     * 
     * @param dayOfWeek Optional filter: only count drones available on this day
     * @param time      Optional filter: only count drones available at this time
     *                  (requires dayOfWeek)
     */
    public GeoJsonFeatureCollection getDataAsGeoJson(DayOfWeek dayOfWeek, LocalTime time) {
        List<GeoJsonFeature> features = new ArrayList<>();

        // Add restricted areas as Polygons
        List<RestrictedArea> restrictedAreas = unifiedDataService.getRestrictedAreas();
        for (RestrictedArea area : restrictedAreas) {
            if (area.getVertices() != null && area.getVertices().size() >= 3) {
                GeoJsonFeature feature = createPolygonFeature(area);
                features.add(feature);
            }
        }

        // Add service points as Points with aggregated drone statistics
        List<ServicePoint> servicePoints = unifiedDataService.getServicePoints();
        List<DroneForServicePoint> dronesForServicePoints = unifiedDataService.getDronesForServicePoints();
        List<Drone> allDrones = unifiedDataService.getAllDrones();
        Map<String, Drone> dronesById = allDrones.stream()
                .collect(Collectors.toMap(Drone::getId, d -> d));

        for (ServicePoint sp : servicePoints) {
            DroneForServicePoint dfsp = dronesForServicePoints.stream()
                    .filter(d -> Objects.equals(d.getServicePointId(), sp.getId()))
                    .findFirst()
                    .orElse(null);

            DroneStatistics stats = calculateDroneStatistics(dfsp, dronesById, dayOfWeek, time);
            GeoJsonFeature feature = createServicePointFeature(sp, stats);
            features.add(feature);
        }

        // Add recent delivery locations as Points
        List<DeliveryAttemptEntity> recentAttempts = deliveryAttemptRepository.findTop20ByOrderByAttemptedAtDesc();
        for (DeliveryAttemptEntity attempt : recentAttempts) {
            if (attempt.getRequestPayload() != null) {
                List<Position> deliveryLocations = extractDeliveryLocations(attempt.getRequestPayload());
                for (Position location : deliveryLocations) {
                    GeoJsonFeature feature = createDeliveryLocationFeature(location, attempt);
                    features.add(feature);
                }
            }
        }

        GeoJsonFeatureCollection collection = new GeoJsonFeatureCollection();
        collection.setFeatures(features);
        return collection;
    }

    private GeoJsonFeature createPolygonFeature(RestrictedArea area) {
        // Convert vertices to GeoJSON Polygon format: [[[lng, lat], [lng, lat], ...]]
        List<List<double[]>> polygonCoordinates = new ArrayList<>();
        List<double[]> ring = area.getVertices().stream()
                .map(pos -> new double[] { pos.getLng(), pos.getLat() })
                .collect(Collectors.toList());
        // Close the polygon by adding first point at the end
        if (!ring.isEmpty()) {
            ring.add(ring.getFirst());
        }
        polygonCoordinates.add(ring);

        GeoJsonGeometry geometry = new GeoJsonGeometry("Polygon", polygonCoordinates);
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", area.getName());
        properties.put("id", area.getId());
        properties.put("type", "restricted_area");

        GeoJsonFeature feature = new GeoJsonFeature();
        feature.setGeometry(geometry);
        feature.setProperties(properties);
        return feature;
    }

    private GeoJsonFeature createServicePointFeature(ServicePoint sp, DroneStatistics stats) {
        double[] coordinates = new double[] { sp.getLocation().getLng(), sp.getLocation().getLat() };
        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", coordinates);

        // Use LinkedHashMap to preserve property order
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", sp.getName());
        properties.put("id", sp.getId());
        properties.put("type", "service_point");
        properties.put("marker-color", "grey");
        properties.put("droneCount", stats.totalCount);
        properties.put("droneWithCooling", stats.coolingCount > 0);
        properties.put("droneWithHeating", stats.heatingCount > 0);
        if (stats.maxMaxMoves != null) {
            properties.put("maxMoves", stats.maxMaxMoves);
        }

        GeoJsonFeature feature = new GeoJsonFeature();
        feature.setGeometry(geometry);
        feature.setProperties(properties);
        return feature;
    }

    /**
     * Calculate aggregated drone statistics for a service point.
     * Reuses availability checking logic.
     */
    private DroneStatistics calculateDroneStatistics(DroneForServicePoint dfsp,
            Map<String, Drone> dronesById,
            DayOfWeek dayOfWeek,
            LocalTime time) {
        DroneStatistics stats = new DroneStatistics();

        if (dfsp == null || dfsp.getDrones() == null) {
            return stats;
        }

        for (ServicePointDroneAvailability spda : dfsp.getDrones()) {
            String droneId = spda.getId();
            Drone drone = dronesById.get(droneId);
            if (drone == null)
                continue;

            // Filter by availability if dayOfWeek/time provided
            if (dayOfWeek != null) {
                boolean isAvailable = isDroneAvailableAt(spda.getAvailability(), dayOfWeek, time);
                if (!isAvailable)
                    continue;
            }

            stats.totalCount++;

            if (drone.getCapability() != null) {
                DroneCapability cap = drone.getCapability();

                // Count cooling drones
                if (Boolean.TRUE.equals(cap.getCooling())) {
                    stats.coolingCount++;
                }

                // Count heating drones
                if (Boolean.TRUE.equals(cap.getHeating())) {
                    stats.heatingCount++;
                }

                // Track max maxMoves
                if (cap.getMaxMoves() != null) {
                    if (stats.maxMaxMoves == null || cap.getMaxMoves() > stats.maxMaxMoves) {
                        stats.maxMaxMoves = cap.getMaxMoves();
                    }
                }
            }
        }

        return stats;
    }

    /**
     * Helper class to hold aggregated drone statistics for a service point.
     */
    private static class DroneStatistics {
        int totalCount = 0;
        int coolingCount = 0;
        int heatingCount = 0;
        Integer maxMaxMoves = null;
    }

    private GeoJsonFeature createDeliveryLocationFeature(Position location, DeliveryAttemptEntity attempt) {
        double[] coordinates = new double[] { location.getLng(), location.getLat() };
        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", coordinates);

        Map<String, Object> properties = new HashMap<>();
        properties.put("type", "delivery_location");
        properties.put("marker-color", attempt.isSuccess() ? "green" : "red");
        properties.put("attemptId", attempt.getId());
        properties.put("attemptedAt", attempt.getAttemptedAt().toString());
        properties.put("success", attempt.isSuccess());
        if (attempt.getDroneIdUsed() != null) {
            properties.put("droneId", attempt.getDroneIdUsed());
        }
        if (attempt.getTotalCost() != null) {
            properties.put("cost", attempt.getTotalCost());
        }

        GeoJsonFeature feature = new GeoJsonFeature();
        feature.setGeometry(geometry);
        feature.setProperties(properties);
        return feature;
    }

    private List<Position> extractDeliveryLocations(String requestPayloadJson) {
        try {
            List<MedDispatchRec> dispatches = objectMapper.readValue(
                    requestPayloadJson,
                    new TypeReference<>() {
                    });
            return dispatches.stream()
                    .map(MedDispatchRec::getDelivery)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse delivery request payload JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isDroneAvailableAt(List<DroneAvailabilityWindow> availability, DayOfWeek dayOfWeek,
            LocalTime time) {
        if (availability == null || availability.isEmpty()) {
            return false;
        }

        for (DroneAvailabilityWindow window : availability) {
            if (window.getDayOfWeek() == dayOfWeek) {
                if (time == null) {
                    // If no time specified, just check if drone is available on this day
                    return true;
                }
                // Check if time falls within the window
                LocalTime from = window.getFrom();
                LocalTime until = window.getUntil();
                if (from != null && until != null) {
                    // Handle case where until might be before from (e.g., overnight)
                    if (from.isBefore(until) || from.equals(until)) {
                        // Normal window: time must be >= from and <= until
                        return (time.isAfter(from) || time.equals(from)) &&
                                (time.isBefore(until) || time.equals(until));
                    } else {
                        // Overnight window (e.g., 22:00 to 06:00)
                        return time.isAfter(from) || time.equals(from) ||
                                time.isBefore(until) || time.equals(until);
                    }
                }
            }
        }
        return false;
    }
}
