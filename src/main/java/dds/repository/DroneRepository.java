package dds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import dds.entity.DroneEntity;

/**
 * Repository for drone CRUD operations.
 */
@Repository
public interface DroneRepository extends JpaRepository<DroneEntity, String> {
}
