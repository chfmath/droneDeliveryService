package dds.service;

import dds.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class DeliveryPlanningService {

    private final DroneService droneService;
    private final PathfindingService pathfindingService;
    private final LocationService locationService;
    private final RouteCalculationHelper routeHelper;

    public DeliveryPlanningService(DroneService droneService,
            PathfindingService pathfindingService,
            LocationService locationService,
            RouteCalculationHelper routeHelper) {
        this.droneService = droneService;
        this.pathfindingService = pathfindingService;
        this.locationService = locationService;
        this.routeHelper = routeHelper;
    }

    public DeliveryPathResponse calcDeliveryPath(List<MedDispatchRec> dispatches, String droneIdToBeUsed) {
        if (dispatches == null || dispatches.isEmpty()) {
            return new DeliveryPathResponse(0, 0, Collections.emptyList());
        }

        List<DronePath> allDronePaths = new ArrayList<>();

        // If a specific drone is requested, try it first
        if (droneIdToBeUsed != null && !droneIdToBeUsed.isEmpty()) {
            DronePath preferredDronePath = planSingleDroneDelivery(droneIdToBeUsed, dispatches);
            if (preferredDronePath != null) {
                allDronePaths.add(preferredDronePath);
                DeliveryPathResponse response = new DeliveryPathResponse();
                response.setDronePaths(allDronePaths);
                calculateTotals(response);
                return response;
            }
            // If preferred drone fails, fall through to automatic selection
        }

        // Automatic drone selection (existing logic)
        List<String> availableDrones = droneService.queryAvailableDrones(dispatches);

        // Try to fulfill with a single drone first
        boolean singleDroneSuccess = false;
        if (!availableDrones.isEmpty()) {
            for (String droneId : availableDrones) {
                DronePath dronePath = planSingleDroneDelivery(droneId, dispatches);
                if (dronePath != null) {
                    allDronePaths.add(dronePath);
                    singleDroneSuccess = true;
                    break;
                }
            }
        }

        // Fallback to multiple drones if single drone plan failed (optimisation is key)
        if (!singleDroneSuccess) {
            allDronePaths.addAll(allocateToMultipleDrones(dispatches));
        }

        DeliveryPathResponse response = new DeliveryPathResponse();
        response.setDronePaths(allDronePaths);
        calculateTotals(response);

        return response;
    }

    private DronePath planSingleDroneDelivery(String droneId,
            List<MedDispatchRec> dispatches) {
        // Early check: all dispatches must be on the same date
        if (dispatches.size() > 1) {
            LocalDate firstDate = dispatches.getFirst().getDate();
            boolean allSameDate = dispatches.stream()
                    .allMatch(d -> Objects.equals(d.getDate(), firstDate));
            if (!allSameDate) {
                return null;
            }
        }

        Drone drone = droneService.getDroneById(droneId);
        if (drone == null) {
            return null;
        }

        Position servicePoint = routeHelper.findValidServicePoint(droneId, dispatches);
        if (servicePoint == null) {
            return null;
        }

        // try optimized multi-delivery trip first (more efficient)
        DronePath optimizedPath = planMultiDeliveryTrip(drone, servicePoint, dispatches);
        if (optimizedPath != null) {
            return optimizedPath;
        }

        // Fall back single delivery trip
        return planSingleDeliveryTrip(drone, servicePoint, dispatches);
    }

    private DronePath planMultiDeliveryTrip(Drone drone, Position servicePoint, List<MedDispatchRec> dispatches) {
        List<Delivery> deliveries = new ArrayList<>();
        Position currentLocation = servicePoint;

        for (int i = 0; i < dispatches.size(); i++) {
            MedDispatchRec dispatch = dispatches.get(i);
            Position deliveryLocation = dispatch.getDelivery();
            Integer deliveryId = dispatch.getId();

            List<Position> path = new ArrayList<>(pathfindingService.findPath(currentLocation, deliveryLocation));

            if (path.isEmpty() || !locationService.isCloseTo(path.getLast(), deliveryLocation)) {
                return null;
            }

            Position lastPos = path.getLast();
            path.add(lastPos); // Hover at delivery location

            if (i == dispatches.size() - 1) {
                List<Position> returnPath = pathfindingService.findPath(lastPos, servicePoint);

                if (returnPath.isEmpty() || !locationService.isCloseTo(returnPath.getLast(), servicePoint)) {
                    return null;
                }

                if (!returnPath.isEmpty()) {
                    path.addAll(returnPath.subList(1, returnPath.size()));
                }
            }

            currentLocation = lastPos;
            deliveries.add(new Delivery(deliveryId, path));
        }

        if (exceedsMultiTripBudget(deliveries, drone)) {
            return null;
        }

        if (!meetsSharedCostConstraints(deliveries, dispatches, drone)) {
            return null;
        }

        return new DronePath(drone.getId(), deliveries);
    }

    private DronePath planSingleDeliveryTrip(Drone drone, Position servicePoint, List<MedDispatchRec> dispatches) {
        List<Delivery> deliveries = new ArrayList<>();
        DroneCapability capability = drone.getCapability();

        for (MedDispatchRec dispatch : dispatches) {
            Position deliveryLocation = dispatch.getDelivery();
            Integer deliveryId = dispatch.getId();

            List<Position> completePath = new ArrayList<>(pathfindingService.findPath(servicePoint, deliveryLocation));

            if (completePath.isEmpty() || !locationService.isCloseTo(completePath.getLast(), deliveryLocation)) {
                return null;
            }

            Position lastPos = completePath.getLast();
            completePath.add(lastPos); // Hover at delivery location

            List<Position> returnPath = pathfindingService.findPath(lastPos, servicePoint);

            if (returnPath.isEmpty() || !locationService.isCloseTo(returnPath.getLast(), servicePoint)) {
                return null;
            }

            if (!returnPath.isEmpty()) {
                completePath.addAll(returnPath.subList(1, returnPath.size()));
            }

            deliveries.add(new Delivery(deliveryId, completePath));

            if (dispatch.getRequirements() != null && dispatch.getRequirements().getMaxCost() > 0) {
                int totalMoves = Math.max(0, completePath.size() - 1);
                double actualCost = routeHelper.calculateFlightCost(capability, totalMoves);

                if (Double.compare(actualCost, dispatch.getRequirements().getMaxCost()) > 0) {
                    return null;
                }
            }
        }

        if (exceedsMovesBudget(deliveries, drone)) {
            return null;
        }

        return new DronePath(drone.getId(), deliveries);
    }

    private List<DronePath> allocateToMultipleDrones(List<MedDispatchRec> dispatches) {
        List<DronePath> dronePaths = new ArrayList<>();
        List<MedDispatchRec> remaining = new ArrayList<>(dispatches);

        while (!remaining.isEmpty()) {
            List<String> available = droneService.queryAvailableDrones(remaining);
            if (available.isEmpty()) {
                MedDispatchRec dispatch = remaining.removeFirst();
                List<String> singleAvailable = droneService.queryAvailableDrones(Collections.singletonList(dispatch));
                if (!singleAvailable.isEmpty()) {
                    String droneId = singleAvailable.getFirst();
                    DronePath path = planSingleDroneDelivery(droneId, Collections.singletonList(dispatch));
                    if (path != null) {
                        dronePaths.add(path);
                    }
                }
                continue;
            }

            String droneId = available.getFirst();
            Drone drone = droneService.getDroneById(droneId);

            List<MedDispatchRec> batch = findMaximalBatch(remaining, drone);
            if (!batch.isEmpty()) {
                DronePath path = planSingleDroneDelivery(droneId, batch);
                if (path != null) {
                    dronePaths.add(path);
                    remaining.removeAll(batch);
                    continue;
                }
            }

            MedDispatchRec dispatch = remaining.removeFirst();
            DronePath path = planSingleDroneDelivery(droneId, Collections.singletonList(dispatch));
            if (path != null) {
                dronePaths.add(path);
            }
        }

        return dronePaths;
    }

    private void calculateTotals(DeliveryPathResponse response) {
        double totalCost = 0;
        int totalMoves = 0;

        for (DronePath dronePath : response.getDronePaths()) {
            Drone drone = droneService.getDroneById(dronePath.getDroneId());
            if (drone == null || drone.getCapability() == null) {
                continue;
            }

            DroneCapability capability = drone.getCapability();

            int droneMoves = 0;
            for (Delivery delivery : dronePath.getDeliveries()) {
                if (delivery.getFlightPath() != null) {
                    droneMoves += Math.max(0, delivery.getFlightPath().size() - 1);
                }
            }

            double droneCost = routeHelper.calculateFlightCost(capability, droneMoves);

            totalCost += droneCost;
            totalMoves += droneMoves;
        }

        response.setTotalCost(totalCost);
        response.setTotalMoves(totalMoves);
    }

    private boolean exceedsMovesBudget(List<Delivery> deliveries, Drone drone) {
        if (drone == null || drone.getCapability() == null || drone.getCapability().getMaxMoves() == null) {
            return false;
        }

        int maxMoves = drone.getCapability().getMaxMoves();

        for (Delivery delivery : deliveries) {
            int segmentMoves = delivery.getFlightPath() != null ? Math.max(0, delivery.getFlightPath().size() - 1) : 0;

            if (segmentMoves > maxMoves) {
                return true;
            }
        }

        return false;
    }

    private boolean exceedsMultiTripBudget(List<Delivery> deliveries, Drone drone) {
        if (drone == null || drone.getCapability() == null || drone.getCapability().getMaxMoves() == null) {
            return false;
        }

        int maxMoves = drone.getCapability().getMaxMoves();

        int totalMoves = deliveries.stream()
                .mapToInt(
                        delivery -> delivery.getFlightPath() != null ? Math.max(0, delivery.getFlightPath().size() - 1)
                                : 0)
                .sum();

        return totalMoves > maxMoves;
    }

    private boolean meetsSharedCostConstraints(List<Delivery> deliveries, List<MedDispatchRec> dispatches,
            Drone drone) {
        if (drone == null || drone.getCapability() == null) {
            return false;
        }

        DroneCapability capability = drone.getCapability();

        boolean hasCostConstraint = dispatches.stream()
                .anyMatch(d -> d.getRequirements() != null && d.getRequirements().getMaxCost() > 0);

        if (!hasCostConstraint) {
            return true;
        }

        int totalMoves = deliveries.stream()
                .mapToInt(
                        delivery -> delivery.getFlightPath() != null ? Math.max(0, delivery.getFlightPath().size() - 1)
                                : 0)
                .sum();

        double actualTotalCost = routeHelper.calculateFlightCost(capability, totalMoves);

        double costPerDelivery = actualTotalCost / dispatches.size();

        for (MedDispatchRec dispatch : dispatches) {
            MedDispatchRequirements requirements = dispatch.getRequirements();
            if (requirements != null && requirements.getMaxCost() > 0) {
                if (Double.compare(costPerDelivery, requirements.getMaxCost()) > 0) {
                    return false;
                }
            }
        }

        return true;
    }

    private List<MedDispatchRec> findMaximalBatch(List<MedDispatchRec> dispatches, Drone drone) {
        if (drone == null || dispatches.isEmpty()) {
            return Collections.emptyList();
        }

        // Early exit: if dispatches span multiple dates, batching is impossible
        if (dispatches.size() > 1) {
            LocalDate firstDate = dispatches.getFirst().getDate();
            boolean allSameDate = dispatches.stream()
                    .allMatch(d -> Objects.equals(d.getDate(), firstDate));
            if (!allSameDate) {
                return Collections.emptyList();
            }
        }

        List<MedDispatchRec> bestBatch = Collections.emptyList();

        for (MedDispatchRec reference : dispatches) {
            Position referenceBase = routeHelper.findValidServicePoint(drone.getId(),
                    Collections.singletonList(reference));
            if (referenceBase == null) {
                continue;
            }

            LocalDate referenceDate = reference.getDate();

            List<MedDispatchRec> sameBaseAndDate = new ArrayList<>();
            for (MedDispatchRec candidate : dispatches) {
                Position candidateBase = routeHelper.findValidServicePoint(drone.getId(),
                        Collections.singletonList(candidate));
                if (candidateBase == null) {
                    continue;
                }
                LocalDate candidateDate = candidate.getDate();

                if (locationService.isCloseTo(referenceBase, candidateBase)
                        && Objects.equals(referenceDate, candidateDate)) {
                    sameBaseAndDate.add(candidate);
                }
            }

            for (int size = sameBaseAndDate.size(); size > 0; size--) {
                for (int start = 0; start <= sameBaseAndDate.size() - size; start++) {
                    List<MedDispatchRec> candidate = new ArrayList<>(sameBaseAndDate.subList(start, start + size));

                    if (droneService.queryAvailableDrones(candidate).contains(drone.getId())) {
                        DronePath testPath = planSingleDroneDelivery(drone.getId(), candidate);
                        if (testPath != null && candidate.size() > bestBatch.size()) {
                            bestBatch = candidate;
                        }
                    }
                }
            }
        }

        return bestBatch;
    }
}
