package dds.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single delivery option with a specific drone.
 * Used for comparing different drone choices for the same delivery request.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't include null fields in JSON
@Schema(description = "A delivery option using a specific drone")
public class DeliveryOption {
    @Schema(description = "Drone ID", example = "5")
    private String droneId;
    
    @Schema(description = "Drone name", example = "Drone 5")
    private String droneName;
    
    @Schema(description = "Total moves in the flight path", example = "83")
    private int moves;
    
    @Schema(description = "Total cost in GBP", example = "8.62")
    private double cost;
    
    @Schema(description = "Estimated flight time in minutes", example = "1.38")
    private double estimatedMinutes;
    
    @Schema(description = "Flight path coordinates (excluded when includePaths=false)")
    private List<Position> flightPath;
}

