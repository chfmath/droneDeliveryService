package dds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import dds.entity.RestrictedAreaEntity;

/**
 * Repository for restricted area CRUD operations.
 */
@Repository
public interface RestrictedAreaRepository extends JpaRepository<RestrictedAreaEntity, Integer> {
}

