package dds.service;

import dds.dto.DeliveryOption;
import dds.dto.Drone;
import dds.dto.MedDispatchRec;
import dds.dto.RouteComparisonResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for comparing all available route options for a delivery request.
 * Calculates paths for each available drone and ranks them by cost and speed.
 */
@Service
public class RouteComparisonService {

    private final DroneService droneService;
    private final RouteCalculationHelper routeHelper;

    public RouteComparisonService(DroneService droneService,
                                   RouteCalculationHelper routeHelper) {
        this.droneService = droneService;
        this.routeHelper = routeHelper;
    }

    /**
     * Compare all available route options for the given dispatches.
     * Returns ranked options with fastest, cheapest, and recommended choices.
     */
    public RouteComparisonResponse compareRoutes(List<MedDispatchRec> dispatches) {
        if (dispatches == null || dispatches.isEmpty()) {
            return RouteComparisonResponse.noOptions("No dispatches provided");
        }

        // Get all available drones for this request
        List<String> availableDroneIds = droneService.queryAvailableDrones(dispatches);
        
        if (availableDroneIds.isEmpty()) {
            return RouteComparisonResponse.noOptions("No drones available for the requested time and requirements");
        }

        // Calculate delivery option for each available drone
        List<DeliveryOption> options = new ArrayList<>();
        
        for (String droneId : availableDroneIds) {
            Drone drone = droneService.getDroneById(droneId);
            DeliveryOption option = routeHelper.calculateOptionForDrone(drone, dispatches);
            if (option != null) {
                options.add(option);
            }
        }

        return routeHelper.buildResponse(options);
    }
}
