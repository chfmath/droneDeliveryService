package dds.service;

import dds.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DroneService {

    public static final double MOVE_DISTANCE = 0.00015;
    private final UnifiedDataService dataService;
    private final LocationService locationService;

    public DroneService(UnifiedDataService dataService, LocationService locationService) {
        this.dataService = dataService;
        this.locationService = locationService;
    }

    @Transactional(readOnly = true)
    public List<String> getDronesWithCooling(boolean state) {
        return dataService.getAllDrones().stream()
                .filter(Objects::nonNull)
                .filter(drone -> {
                    DroneCapability capability = drone.getCapability();
                    if (capability == null) {
                        return false;
                    }
                    // Treats null as no cooling capability
                    boolean hasCooling = Boolean.TRUE.equals(capability.getCooling());
                    return hasCooling == state;
                })
                .map(Drone::getId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Drone getDroneById(String id) {
        return dataService.getAllDrones().stream()
                .filter(Objects::nonNull)
                .filter(drone -> drone.getId().equals(id))
                // just return first match (probably unnecessary since IDs should be unique)
                .findFirst()
                .orElse(null);
    }

    // used for queryAsPath
    @Transactional(readOnly = true)
    public List<String> queryBySingleCapability(String attribute, String value) {
        return dataService.getAllDrones().stream()
                .filter(Objects::nonNull)
                // as only used for queryAsPath, which only allows equality, so use generic method with "=" operator
                .filter(drone -> matchesCapability(drone, attribute, value, "="))
                .map(Drone::getId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> queryByCapabilities(List<QueryAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }

        return dataService.getAllDrones().stream()
                .filter(Objects::nonNull)
                .filter(drone -> attributes.stream()
                        .allMatch(attr -> matchesCapability(
                                drone,
                                attr.getAttribute(),
                                attr.getValue(),
                                attr.getOperator())))
                .map(Drone::getId)
                .collect(Collectors.toList());
    }

    private boolean matchesCapability(Drone drone, String attribute, String rawValue, String operator) {
        if (drone == null || attribute == null || rawValue == null) {
            return false;
        }

        DroneCapability capability = drone.getCapability();
        if (capability == null) {
            return false;
        }

        // If operator is null or blank assume '=', else trim (to avoid issues with extra spaces)
        String op = (operator == null || operator.isBlank()) ? "=" : operator.trim();
        // normalise to be as accepting as possible
        String attr = attribute.trim().toLowerCase();

        // important to distinguish between number and boolean comparisons
        return switch (attr) {
            case "cooling" -> compareBoolean(capability.getCooling(), rawValue, op);
            case "heating" -> compareBoolean(capability.getHeating(), rawValue, op);
            case "capacity" -> compareNumber(capability.getCapacity(), rawValue, op);
            case "maxmoves" -> compareNumber(capability.getMaxMoves(), rawValue, op);
            case "costpermove" -> compareNumber(capability.getCostPerMove(), rawValue, op);
            case "costinitial" -> compareNumber(capability.getCostInitial(), rawValue, op);
            case "costfinal" -> compareNumber(capability.getCostFinal(), rawValue, op);
            default -> false;
        };
    }

    private boolean compareNumber(Number actual, String rawValue, String operator) {
        if (actual == null) {
            return false;
        }
        double actualValue = actual.doubleValue();
        double targetValue = Double.parseDouble(rawValue);

        return switch (operator) {
            case "=" -> Double.compare(actualValue, targetValue) == 0;
            case "!=" -> Double.compare(actualValue, targetValue) != 0;
            case "<" -> actualValue < targetValue;
            case ">" -> actualValue > targetValue;
            default -> false;
        };
    }

    private boolean compareBoolean(Boolean actual, String rawValue, String operator) {
        // as used above, this treats null as false too
        boolean actualValue = Boolean.TRUE.equals(actual);
        boolean targetValue = Boolean.parseBoolean(rawValue);

        return switch (operator) {
            case "=" -> actualValue == targetValue;
            case "!=" -> actualValue != targetValue;
            default -> false;
        };
    }

    @Transactional(readOnly = true)
    public List<String> queryAvailableDrones(List<MedDispatchRec> dispatches) {
        if (dispatches == null || dispatches.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Drone> dronesById = dataService.getAllDrones().stream()
                .collect(Collectors.toMap(Drone::getId, Function.identity()));

        Map<Integer, Position> servicePointLocations = dataService.getServicePoints().stream()
                .collect(Collectors.toMap(ServicePoint::getId, ServicePoint::getLocation));

        List<DroneForServicePoint> servicePoints = dataService.getDronesForServicePoints();

        Map<Optional<LocalDate>, List<MedDispatchRec>> dispatchesByDate = dispatches.stream()
                .collect(Collectors.groupingBy(dispatch ->
                        Optional.ofNullable(dispatch.getDate())));

        return servicePoints.stream()
                .filter(point -> servicePointLocations.containsKey(point.getServicePointId()))
                .flatMap(point -> point.getDrones().stream()
                        .filter(availability -> dronesById.containsKey(availability.getId()))
                        .filter(availability -> canDroneSupportAllDates(
                                dispatchesByDate,
                                dronesById.get(availability.getId()),
                                availability,
                                servicePointLocations.get(point.getServicePointId())))
                        .map(ServicePointDroneAvailability::getId))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private boolean canDroneSupportAllDates(Map<Optional<LocalDate>, List<MedDispatchRec>> dispatchesByDate,
                                           Drone drone,
                                           ServicePointDroneAvailability availability,
                                           Position servicePointLocation) {
        if (servicePointLocation == null) {
            return false;
        }

        for (Map.Entry<Optional<LocalDate>, List<MedDispatchRec>> entry : dispatchesByDate.entrySet()) {
            if (!isDroneSuitableForGroup(entry.getValue(), drone, availability, servicePointLocation)) {
                return false;
            }
        }
        return true;
    }

    private boolean isDroneSuitableForGroup(List<MedDispatchRec> dispatches,
                                            Drone drone,
                                            ServicePointDroneAvailability availability,
                                            Position servicePointLocation) {
        if (drone == null || drone.getCapability() == null) {
            return false;
        }

        DroneCapability capability = drone.getCapability();
        double totalCapacityRequired = dispatches.stream()
                .filter(d -> d.getRequirements() != null)
                .mapToDouble(d -> d.getRequirements().getCapacity())
                .sum();

        if (!hasRequiredCapacity(capability, totalCapacityRequired)) {
            return false;
        }

        boolean needsCooling = dispatches.stream()
                .anyMatch(d -> d.getRequirements() != null && Boolean.TRUE.equals(d.getRequirements().getCooling()));
        boolean needsHeating = dispatches.stream()
                .anyMatch(d -> d.getRequirements() != null && Boolean.TRUE.equals(d.getRequirements().getHeating()));

        if (!meetsTemperatureRequirements(capability, needsCooling, needsHeating)) {
            return false;
        }

        if (!dispatches.stream().allMatch(dispatch -> isDispatchWithinAvailability(dispatch, availability))) {
            return false;
        }

        return matchesCostRequirements(dispatches, drone, servicePointLocation);
    }

    private boolean matchesCostRequirements(List<MedDispatchRec> dispatches,
                                           Drone drone,
                                           Position servicePointLocation) {
        if (dispatches == null || dispatches.isEmpty() || servicePointLocation == null) {
            return false;
        }

        DroneCapability capability = drone.getCapability();

        boolean hasCostConstraint = dispatches.stream()
                .anyMatch(d -> d.getRequirements() != null && d.getRequirements().getMaxCost() > 0);

        if (!hasCostConstraint) {
            return true;
        }

        boolean allIndividuallyFeasible = true;
        for (MedDispatchRec dispatch : dispatches) {
            MedDispatchRequirements requirements = dispatch.getRequirements();
            if (requirements != null && requirements.getMaxCost() > 0) {
                double roundTripMoves = calculateMinimumMoves(servicePointLocation, dispatch.getDelivery()) * 2;
                double deliveryCost = capability.getCostInitial()
                        + capability.getCostFinal()
                        + (capability.getCostPerMove() * roundTripMoves);

                if (Double.compare(deliveryCost, requirements.getMaxCost()) > 0) {
                    allIndividuallyFeasible = false;
                    break;
                }
            }
        }

        if (allIndividuallyFeasible) {
            return true;
        }

        return couldWorkWithSharedCosts(dispatches, drone, servicePointLocation);
    }

    // Special case: check if shared costs would make all deliveries feasible, because initial and final costs are only paid once
    private boolean couldWorkWithSharedCosts(List<MedDispatchRec> dispatches,
                                            Drone drone,
                                            Position servicePointLocation) {
        DroneCapability capability = drone.getCapability();

        double totalMoves = 0;
        Position currentPos = servicePointLocation;

        for (MedDispatchRec dispatch : dispatches) {
            double moves = calculateMinimumMoves(currentPos, dispatch.getDelivery());
            totalMoves += moves;
            currentPos = dispatch.getDelivery();
        }
        totalMoves += calculateMinimumMoves(currentPos, servicePointLocation);

        double totalCost = capability.getCostInitial()
                + capability.getCostFinal()
                + (capability.getCostPerMove() * totalMoves);

        double costPerDelivery = totalCost / dispatches.size();

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
    public boolean isDispatchWithinAvailability(MedDispatchRec dispatch,
                                                ServicePointDroneAvailability availability) {
        if (availability == null || availability.getAvailability() == null) {
            return false;
        }

        DayOfWeek day = dispatch.getDate() != null ? dispatch.getDate().getDayOfWeek() : null;
        LocalTime time = dispatch.getTime();

        return availability.getAvailability().stream()
                .filter(slot -> day == null || slot.getDayOfWeek() == day)
                .anyMatch(slot -> time == null
                        || (!time.isBefore(slot.getFrom()) && !time.isAfter(slot.getUntil())));
    }

    private boolean hasRequiredCapacity(DroneCapability capability, double totalCapacityRequired) {
        return capability.getCapacity() != null
                && Double.compare(capability.getCapacity(), totalCapacityRequired) >= 0;
    }

    private boolean meetsTemperatureRequirements(DroneCapability capability,
                                                 boolean needsCooling,
                                                 boolean needsHeating) {
        if (needsCooling && !Boolean.TRUE.equals(capability.getCooling())) {
            return false;
        }
        return !needsHeating || Boolean.TRUE.equals(capability.getHeating());
    }

    private double calculateMinimumMoves(Position servicePointLocation, Position deliveryLocation) {
        double distance = locationService.calculateDistance(servicePointLocation, deliveryLocation);
        return Math.ceil(distance / MOVE_DISTANCE);
    }
}