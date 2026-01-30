package dds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Requirements for a medical dispatch")
public class MedDispatchRequirements {
    @Schema(description = "Required payload capacity in kg", example = "5.0")
    private double capacity;
    
    @Schema(description = "Whether cooling is required", example = "false")
    private Boolean cooling;
    
    @Schema(description = "Whether heating is required", example = "false")
    private Boolean heating;
    
    @Schema(description = "Maximum acceptable cost in GBP", example = "20.0")
    private double maxCost;
}
