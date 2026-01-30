package dds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import dds.entity.DeliveryAttemptEntity;

import java.util.List;

/**
 * Repository for accessing delivery attempt data in PostgreSQL.
 * Provides CRUD operations plus custom queries for analytics.
 */
@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttemptEntity, Long> {
    
    /**
     * Count attempts by success status.
     */
    long countBySuccess(boolean success);
    
    /**
     * Get the most used drone ID.
     */
    @Query("SELECT d.droneIdUsed, COUNT(d) as cnt FROM DeliveryAttemptEntity d WHERE d.success = true AND d.droneIdUsed IS NOT NULL GROUP BY d.droneIdUsed ORDER BY cnt DESC")
    List<Object[]> countByDroneId();
    
    /**
     * Get total cost of all successful deliveries.
     */
    @Query("SELECT SUM(d.totalCost) FROM DeliveryAttemptEntity d WHERE d.success = true")
    Double sumTotalCost();
    
    /**
     * Get total moves of all successful deliveries.
     */
    @Query("SELECT SUM(d.totalMoves) FROM DeliveryAttemptEntity d WHERE d.success = true")
    Long sumTotalMoves();
    
    /**
     * Get recent attempts ordered by time.
     */
    List<DeliveryAttemptEntity> findTop20ByOrderByAttemptedAtDesc();
}
