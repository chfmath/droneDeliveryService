package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for a single delivery attempt in the history.
 * Used for displaying recent delivery attempts.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryHistoryItem {
    private Long id;
    private LocalDateTime attemptedAt;
    private boolean success;
    private String droneIdUsed;
    private Double totalCost;
    private Integer totalMoves;
}

