package dds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Entity representing a drone's availability at a service point.
 * Maps to the 'drone_availability' table in PostgreSQL.
 * Each row represents one availability window for one drone at one service point.
 */
@Entity
@Table(name = "drone_availability")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DroneAvailabilityEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "drone_id")
    private String droneId;
    
    @Column(name = "service_point_id")
    private Integer servicePointId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;
    
    @Column(name = "from_time")
    private LocalTime fromTime;
    
    @Column(name = "until_time")
    private LocalTime untilTime;
}

