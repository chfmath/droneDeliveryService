package dds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import dds.entity.ServicePointEntity;

/**
 * Repository for service point CRUD operations.
 */
@Repository
public interface ServicePointRepository extends JpaRepository<ServicePointEntity, Integer> {
}

