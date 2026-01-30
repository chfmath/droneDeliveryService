package dds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a restricted (no-fly) area.
 * Maps to the 'restricted_areas' table in PostgreSQL.
 * Vertices are stored as JSON string.
 */
@Entity
@Table(name = "restricted_areas")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestrictedAreaEntity {
    
    @Id
    private Integer id;
    
    private String name;
    
    // Limits
    private Integer lowerLimit;
    private Integer upperLimit;
    
    // Vertices stored as JSON array string
    // Format: [{"lng": -3.19, "lat": 55.94, "alt": null}, ...]
    @Column(columnDefinition = "TEXT")
    private String verticesJson;
}

