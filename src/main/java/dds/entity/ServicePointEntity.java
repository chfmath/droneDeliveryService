package dds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a service point (drone depot).
 * Maps to the 'service_points' table in PostgreSQL.
 */
@Entity
@Table(name = "service_points")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicePointEntity {
    
    @Id
    private Integer id;
    
    private String name;
    
    // Location fields
    private Double lng;
    private Double lat;
    private Double alt;
}

