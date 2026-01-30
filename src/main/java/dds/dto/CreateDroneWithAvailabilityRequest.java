package dds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Combined request to create a drone and set its availability at a service point in one step.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Create a drone with availability in one request")
public class CreateDroneWithAvailabilityRequest {
    
    @NotNull
    @Schema(description = "Unique drone identifier", example = "11")
    private String id;
    
    @Schema(description = "Human-readable drone name", example = "New Cooling Drone")
    private String name;
    
    @Valid
    @NotNull
    @Schema(description = "Drone capabilities and costs")
    private DroneCapability capability;
    
    @NotNull
    @Schema(description = "Service point ID to assign this drone to", example = "1")
    private Integer servicePointId;
    
    @Valid
    @NotEmpty
    @Schema(description = "Availability windows at the service point")
    private List<DroneAvailabilityWindow> availability;
}

