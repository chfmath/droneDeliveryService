package dds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a drone with its capabilities.
 * Maps to the 'drones' table in PostgreSQL.
 */
@Entity
@Table(name = "drones")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DroneEntity {
    
    @Id
    private String id;
    
    private String name;
    
    // Capability fields
    private Boolean cooling;
    private Boolean heating;
    private Double capacity;
    private Integer maxMoves;
    private Double costPerMove;
    private Double costInitial;
    private Double costFinal;
}

