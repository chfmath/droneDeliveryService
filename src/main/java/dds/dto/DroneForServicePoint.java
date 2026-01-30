package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DroneForServicePoint {
    private Integer servicePointId;
    private List<ServicePointDroneAvailability> drones;
}