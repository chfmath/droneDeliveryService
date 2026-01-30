package dds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for logging delivery attempts to the database.
 * Stores both successful and failed attempts for analytics.
 */
@Entity
@Table(name = "delivery_attempts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAttemptEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
    
    @Column(nullable = false)
    private boolean success;
    
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;  // JSON of original request
    
    @Column(name = "drone_id_used")
    private String droneIdUsed;  // null if failed
    
    @Column(name = "total_cost")
    private Double totalCost;
    
    @Column(name = "total_moves")
    private Integer totalMoves;
    
    /**
     * Create a new attempt entity for a successful delivery.
     */
    public static DeliveryAttemptEntity success(String requestPayload, String droneId, double cost, int moves) {
        DeliveryAttemptEntity entity = new DeliveryAttemptEntity();
        entity.setAttemptedAt(LocalDateTime.now());
        entity.setSuccess(true);
        entity.setRequestPayload(requestPayload);
        entity.setDroneIdUsed(droneId);
        entity.setTotalCost(cost);
        entity.setTotalMoves(moves);
        return entity;
    }
    
    /**
     * Create a new attempt entity for a failed delivery.
     */
    public static DeliveryAttemptEntity failure(String requestPayload) {
        DeliveryAttemptEntity entity = new DeliveryAttemptEntity();
        entity.setAttemptedAt(LocalDateTime.now());
        entity.setSuccess(false);
        entity.setRequestPayload(requestPayload);
        return entity;
    }
}
