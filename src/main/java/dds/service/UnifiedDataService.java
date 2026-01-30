package dds.service;

import dds.dto.Drone;
import dds.dto.DroneForServicePoint;
import dds.dto.RestrictedArea;
import dds.dto.ServicePoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Unified data service that can read from either PostgreSQL (local) or Azure API.
 * By default, uses PostgreSQL if data is available, otherwise falls back to Azure.
 * This allows the system to work with local CRUD operations while maintaining
 * compatibility with the original Azure-based system.
 */
@Service
@Slf4j
public class UnifiedDataService {

    private final PostgresDataService postgresDataService;
    private final IlpDataService ilpDataService;
    
    @Value("${ilp.data.source:postgres}")
    private String dataSource;  // "postgres" or "azure"

    public UnifiedDataService(PostgresDataService postgresDataService, IlpDataService ilpDataService) {
        this.postgresDataService = postgresDataService;
        this.ilpDataService = ilpDataService;
    }

    /**
     * Check if we should use PostgreSQL.
     */
    private boolean usePostgres() {
        return "postgres".equalsIgnoreCase(dataSource) && postgresDataService.hasData();
    }

    @Transactional(readOnly = true)
    public List<Drone> getAllDrones() {
        if (usePostgres()) {
            log.debug("Using PostgreSQL data source for drones");
            return postgresDataService.getAllDrones();
        }
        log.debug("Using Azure API data source for drones");
        return ilpDataService.getAllDrones();
    }

    @Transactional(readOnly = true)
    public List<ServicePoint> getServicePoints() {
        if (usePostgres()) {
            log.debug("Using PostgreSQL data source for service points");
            return postgresDataService.getServicePoints();
        }
        log.debug("Using Azure API data source for service points");
        return ilpDataService.getServicePoints();
    }

    @Transactional(readOnly = true)
    public List<RestrictedArea> getRestrictedAreas() {
        if (usePostgres()) {
            log.debug("Using PostgreSQL data source for restricted areas");
            return postgresDataService.getRestrictedAreas();
        }
        log.debug("Using Azure API data source for restricted areas");
        return ilpDataService.getRestrictedAreas();
    }

    @Transactional(readOnly = true)
    public List<DroneForServicePoint> getDronesForServicePoints() {
        if (usePostgres()) {
            log.debug("Using PostgreSQL data source for drone availability");
            return postgresDataService.getDronesForServicePoints();
        }
        log.debug("Using Azure API data source for drone availability");
        return ilpDataService.getDronesForServicePoints();
    }
}
