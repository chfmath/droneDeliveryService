package dds.service;

import dds.dto.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Shared helper for route calculation logic used by RouteComparisonService.
 */
@Component
public class RouteCalculationHelper {

    public static final double MINUTES_PER_MOVE = 1.0 / 60.0;  // ~1 second per move

    private final DroneService droneService;
    private final PathfindingService pathfindingService;
    private final UnifiedDataService dataService;
    private final LocationService locationService;

    public RouteCalculationHelper(DroneService droneService,
                                   PathfindingService pathfindingService,
                                   UnifiedDataService dataService,
                                   LocationService locationService) {
        this.droneService = droneService;
        this.pathfindingService = pathfindingService;
        this.dataService = dataService;
        this.locationService = locationService;
    }

    /**
     * Calculate the total flight cost for a given number of moves.
     */
    public double calculateFlightCost(DroneCapability capability, int moves) {
        double initial = capability.getCostInitial() != null ? capability.getCostInitial() : 0;
        double perMove = capability.getCostPerMove() != null ? capability.getCostPerMove() : 0;
        double finalCost = capability.getCostFinal() != null ? capability.getCostFinal() : 0;

        return initial + (perMove * moves) + finalCost;
    }

    /**
     * Find a valid service point for the given drone and dispatches.
     */
    public Position findValidServicePoint(String droneId, List<MedDispatchRec> dispatches) {
        List<DroneForServicePoint> servicePoints = dataService.getDronesForServicePoints();
        List<ServicePoint> allServicePoints = dataService.getServicePoints();

        for (DroneForServicePoint sp : servicePoints) {
            if (sp.getDrones() == null) continue;

            ServicePointDroneAvailability availability = sp.getDrones().stream()
                    .filter(d -> droneId.equals(d.getId()))
                    .findFirst()
                    .orElse(null);

            if (availability == null) continue;

            boolean allValid = dispatches.stream()
                    .allMatch(d -> droneService.isDispatchWithinAvailability(d, availability));

            if (allValid) {
                for (ServicePoint servicePoint : allServicePoints) {
                    if (Objects.equals(servicePoint.getId(), sp.getServicePointId())) {
                        return servicePoint.getLocation();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get the default service point (first one available).
     */
    public Position getDefaultServicePoint() {
        List<ServicePoint> servicePoints = dataService.getServicePoints();
        if (!servicePoints.isEmpty()) {
            return servicePoints.getFirst().getLocation();
        }
        return null;
    }

    /**
     * Calculate the complete path for all dispatches starting and ending at service point.
     */
    public List<Position> calculateCompletePath(Position servicePoint, List<MedDispatchRec> dispatches) {
        List<Position> completePath = new ArrayList<>();
        Position currentLocation = servicePoint;

        for (MedDispatchRec dispatch : dispatches) {
            Position deliveryLocation = dispatch.getDelivery();

            List<Position> pathToDelivery = pathfindingService.findPath(currentLocation, deliveryLocation);
            if (pathToDelivery.isEmpty() || !locationService.isCloseTo(pathToDelivery.getLast(), deliveryLocation)) {
                return null;
            }

            if (completePath.isEmpty()) {
                completePath.addAll(pathToDelivery);
            } else {
                completePath.addAll(pathToDelivery.subList(1, pathToDelivery.size()));
            }

            // Note: pathToDelivery.getLast() is already included in the path above
            // No need to add it again - this would create a duplicate waypoint
            currentLocation = pathToDelivery.getLast();
        }

        List<Position> returnPath = pathfindingService.findPath(currentLocation, servicePoint);
        if (returnPath.isEmpty() || !locationService.isCloseTo(returnPath.getLast(), servicePoint)) {
            return null;
        }

        if (!returnPath.isEmpty()) {
            completePath.addAll(returnPath.subList(1, returnPath.size()));
        }

        return completePath;
    }

    /**
     * Determine the recommended option based on cost-time tradeoff.
     */
    public DeliveryOption determineRecommendation(DeliveryOption fastest,
                                                  DeliveryOption cheapest,
                                                  List<DeliveryOption> options) {
        if (fastest == null || cheapest == null) {
            return options.isEmpty() ? null : options.getFirst();
        }

        if (fastest.getDroneId().equals(cheapest.getDroneId())) {
            return fastest;
        }

        double timeDifference = cheapest.getEstimatedMinutes() - fastest.getEstimatedMinutes();
        double costSavings = fastest.getCost() - cheapest.getCost();

        if (costSavings > 5.0 && timeDifference < 2.0) {
            return cheapest;
        }

        if (timeDifference < 0.5) {
            return cheapest;
        }

        return fastest;
    }

    /**
     * Generate a human-readable explanation for the recommendation.
     */
    public String generateRecommendationReason(DeliveryOption fastest,
                                                DeliveryOption cheapest,
                                                DeliveryOption recommended) {
        if (fastest == null || cheapest == null || recommended == null) {
            return "No valid options available";
        }

        if (fastest.getDroneId().equals(cheapest.getDroneId())) {
            return String.format("%s is both fastest and cheapest", recommended.getDroneName());
        }

        if (recommended.getDroneId().equals(cheapest.getDroneId())) {
            double savings = fastest.getCost() - cheapest.getCost();
            double extraTime = cheapest.getEstimatedMinutes() - fastest.getEstimatedMinutes();
            return String.format("CHEAPEST saves £%.2f for only %.1f minute(s) extra delay",
                    savings, extraTime);
        }

        if (recommended.getDroneId().equals(fastest.getDroneId())) {
            double extraCost = fastest.getCost() - cheapest.getCost();
            double timeSaved = cheapest.getEstimatedMinutes() - fastest.getEstimatedMinutes();
            return String.format("FASTEST saves %.1f minute(s) for £%.2f extra cost - recommended for urgent deliveries",
                    timeSaved, extraCost);
        }

        return String.format("Balanced option: %s offers good cost-time tradeoff", recommended.getDroneName());
    }

    /**
     * Calculate delivery option for a specific drone.
     */
    public DeliveryOption calculateOptionForDrone(Drone drone, List<MedDispatchRec> dispatches) {
        if (drone == null || drone.getCapability() == null) {
            return null;
        }

        Position servicePoint = findValidServicePoint(drone.getId(), dispatches);
        if (servicePoint == null) {
            servicePoint = getDefaultServicePoint();
            if (servicePoint == null) {
                return null;
            }
        }

        List<Position> completePath = calculateCompletePath(servicePoint, dispatches);
        if (completePath == null || completePath.isEmpty()) {
            return null;
        }

        int moves = Math.max(0, completePath.size() - 1);

        DroneCapability capability = drone.getCapability();
        if (capability.getMaxMoves() != null && moves > capability.getMaxMoves()) {
            return null;
        }

        double cost = calculateFlightCost(capability, moves);
        double estimatedMinutes = moves * MINUTES_PER_MOVE;

        return new DeliveryOption(
                drone.getId(),
                drone.getName(),
                moves,
                cost,
                estimatedMinutes,
                completePath
        );
    }

    /**
     * Build a RouteComparisonResponse from a list of options.
     */
    public RouteComparisonResponse buildResponse(List<DeliveryOption> options) {
        if (options.isEmpty()) {
            return RouteComparisonResponse.noOptions("No valid routes found for available drones");
        }

        options.sort(Comparator.comparingDouble(DeliveryOption::getCost));

        DeliveryOption fastest = options.stream()
                .min(Comparator.comparingInt(DeliveryOption::getMoves))
                .orElse(null);

        DeliveryOption cheapest = options.stream()
                .min(Comparator.comparingDouble(DeliveryOption::getCost))
                .orElse(null);

        DeliveryOption recommended = determineRecommendation(fastest, cheapest, options);
        String reason = generateRecommendationReason(fastest, cheapest, recommended);

        RouteComparisonResponse response = new RouteComparisonResponse();
        response.setOptions(options);
        response.setFastest(fastest);
        response.setCheapest(cheapest);
        response.setRecommended(recommended);
        response.setRecommendationReason(reason);

        return response;
    }
}
