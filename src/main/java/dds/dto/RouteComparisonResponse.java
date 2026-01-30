package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing all available delivery options ranked and compared.
 * Helps operators choose between fastest, cheapest, or recommended options.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteComparisonResponse {
    private List<DeliveryOption> options;      // All valid options
    private DeliveryOption fastest;             // Option with the fewest moves
    private DeliveryOption cheapest;            // Option with the lowest cost
    private DeliveryOption recommended;         // The suggestion
    private String recommendationReason;        // Why it is recommended
    
    /**
     * Method for when no options are available
     */
    public static RouteComparisonResponse noOptions(String reason) {
        RouteComparisonResponse response = new RouteComparisonResponse();
        response.setOptions(List.of());
        response.setRecommendationReason(reason);
        return response;
    }
}

