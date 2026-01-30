package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated statistics about delivery attempts.
 * Used for analytics and reporting.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryStats {
    private long totalAttempts;
    private long successCount;
    private long failureCount;
    private double successRate;
    
    private Double totalCost;
    private Double averageCost;
    private Long totalMoves;
    private Double averageMoves;
    
    private String mostUsedDrone;
}
