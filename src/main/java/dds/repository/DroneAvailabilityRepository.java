package dds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import dds.entity.DroneAvailabilityEntity;

import java.util.List;

/**
 * Repository for drone availability CRUD operations.
 */
@Repository
public interface DroneAvailabilityRepository extends JpaRepository<DroneAvailabilityEntity, Long> {

    List<DroneAvailabilityEntity> findByDroneId(String droneId);

    List<DroneAvailabilityEntity> findByDroneIdAndServicePointId(String droneId, Integer servicePointId);
}
