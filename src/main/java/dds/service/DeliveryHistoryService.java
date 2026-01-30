package dds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dds.dto.DeliveryHistoryItem;
import dds.dto.DeliveryPathResponse;
import dds.dto.DeliveryStats;
import dds.dto.MedDispatchRec;
import dds.entity.DeliveryAttemptEntity;
import dds.repository.DeliveryAttemptRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for logging delivery attempts and querying analytics.
 * All delivery calculations (success and failure) are logged to PostgreSQL.
 */
@Service
@Slf4j
public class DeliveryHistoryService {

    private final DeliveryAttemptRepository repository;
    private final ObjectMapper objectMapper;

    public DeliveryHistoryService(DeliveryAttemptRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Log a successful delivery attempt.
     */
    public void logSuccess(List<MedDispatchRec> dispatches, DeliveryPathResponse response) {
        log.debug("Logging successful delivery attempt with {} dispatches", dispatches.size());
        String requestJson = toJson(dispatches);
        
        String droneId = null;
        if (response.getDronePaths() != null && !response.getDronePaths().isEmpty()) {
            droneId = response.getDronePaths().getFirst().getDroneId();
        }
        
        DeliveryAttemptEntity entity = DeliveryAttemptEntity.success(
                requestJson,
                droneId,
                response.getTotalCost(),
                (int) response.getTotalMoves()
        );
        
        repository.save(entity);
    }

    /**
     * Log a failed delivery attempt.
     */
    public void logFailure(List<MedDispatchRec> dispatches) {
        log.debug("Logging failed delivery attempt with {} dispatches", dispatches.size());
        String requestJson = toJson(dispatches);
        DeliveryAttemptEntity entity = DeliveryAttemptEntity.failure(requestJson);
        repository.save(entity);
    }

    /**
     * Get aggregated statistics about all delivery attempts.
     */
    @Transactional(readOnly = true)
    public DeliveryStats getStats() {
        DeliveryStats stats = new DeliveryStats();
        
        long total = repository.count();
        long successCount = repository.countBySuccess(true);
        long failureCount = repository.countBySuccess(false);
        
        stats.setTotalAttempts(total);
        stats.setSuccessCount(successCount);
        stats.setFailureCount(failureCount);
        stats.setSuccessRate(total > 0 ? (double) successCount / total : 0);
        
        // Cost and moves stats
        Double totalCost = repository.sumTotalCost();
        Long totalMoves = repository.sumTotalMoves();
        
        stats.setTotalCost(totalCost);
        stats.setTotalMoves(totalMoves);
        
        if (successCount > 0) {
            stats.setAverageCost(totalCost != null ? totalCost / successCount : null);
            stats.setAverageMoves(totalMoves != null ? (double) totalMoves / successCount : null);
        }
        
        // Most used drone
        List<Object[]> droneCounts = repository.countByDroneId();
        if (!droneCounts.isEmpty()) {
            stats.setMostUsedDrone((String) droneCounts.getFirst()[0]);
        }
        
        return stats;
    }

    /**
     * Get recent delivery attempts as DTOs.
     */
    @Transactional(readOnly = true)
    public List<DeliveryHistoryItem> getRecentAttempts() {
        return repository.findTop20ByOrderByAttemptedAtDesc().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO.
     */
    private DeliveryHistoryItem entityToDto(DeliveryAttemptEntity entity) {
        return new DeliveryHistoryItem(
                entity.getId(),
                entity.getAttemptedAt(),
                entity.isSuccess(),
                entity.getDroneIdUsed(),
                entity.getTotalCost(),
                entity.getTotalMoves()
        );
    }

    /**
     * Convert object to JSON string for storage.
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize delivery request to JSON", e);
            return "{}";
        }
    }
}
