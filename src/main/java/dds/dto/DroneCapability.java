package dds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Drone capabilities and cost structure")
public class DroneCapability {
    @Schema(description = "Whether drone has cooling capability", example = "true")
    private Boolean cooling;
    
    @Schema(description = "Whether drone has heating capability", example = "false")
    private Boolean heating;
    
    @NotNull
    @Schema(description = "Maximum payload capacity in kg", example = "10.0")
    private Double capacity;
    
    @Schema(description = "Maximum number of moves before recharge", example = "2000")
    private Integer maxMoves;
    
    @Schema(description = "Cost per move in GBP", example = "0.02")
    private Double costPerMove;
    
    @Schema(description = "Initial/takeoff cost in GBP", example = "2.0")
    private Double costInitial;
    
    @Schema(description = "Final/landing cost in GBP", example = "3.0")
    private Double costFinal;
}
