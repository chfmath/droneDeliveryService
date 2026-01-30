package dds.dto;

import lombok.Data;
import java.util.List;

@Data
public class ServicePointDroneAvailability {
    private String id;
    private List<DroneAvailabilityWindow> availability;
}