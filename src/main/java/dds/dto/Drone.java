package dds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Drone with its capabilities")
public class Drone {
    @NotNull(message = "Drone ID must not be null")
    @Schema(description = "Unique drone identifier", example = "virtual-1")
    private String id;

    @Schema(description = "Human-readable drone name", example = "Virtual Cooling Drone")
    private String name;

    @Valid
    @NotNull(message = "Capability must not be null")
    @Schema(description = "Drone capabilities and costs")
    private DroneCapability capability;
}
