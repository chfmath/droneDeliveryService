package dds.service;

import dds.dto.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that seeds the PostgreSQL database with data from the Azure API on first run.
 * Only seeds if the database is empty.
 */
@Slf4j
@Service
public class DataSeederService {

    private final IlpDataService ilpDataService;
    private final PostgresDataService postgresDataService;

    public DataSeederService(IlpDataService ilpDataService, PostgresDataService postgresDataService) {
        this.ilpDataService = ilpDataService;
        this.postgresDataService = postgresDataService;
    }

    /**
     * Seed the database on application startup if it's empty.
     */
    @PostConstruct
    public void seedIfEmpty() {
        if (!postgresDataService.hasData()) {
            log.info("Database is empty. Seeding from Azure API...");
            seedAllData();
            log.info("Database seeding complete!");
        } else {
            log.debug("Database already has data. Skipping seed.");
        }
    }

    /**
     * Seed all data from Azure API to PostgreSQL.
     */
    public void seedAllData() {
        seedDrones();
        seedServicePoints();
        seedRestrictedAreas();
        seedDroneAvailability();
    }

    private void seedDrones() {
        List<Drone> drones = ilpDataService.getAllDrones();
        log.info("Seeding {} drones...", drones.size());
        
        for (Drone drone : drones) {
            postgresDataService.saveDrone(drone);
        }
        log.debug("Successfully seeded {} drones", drones.size());
    }

    private void seedServicePoints() {
        List<ServicePoint> servicePoints = ilpDataService.getServicePoints();
        log.info("Seeding {} service points...", servicePoints.size());
        
        for (ServicePoint sp : servicePoints) {
            postgresDataService.saveServicePoint(sp);
        }
        log.debug("Successfully seeded {} service points", servicePoints.size());
    }

    private void seedRestrictedAreas() {
        List<RestrictedArea> areas = ilpDataService.getRestrictedAreas();
        log.info("Seeding {} restricted areas...", areas.size());
        
        for (RestrictedArea area : areas) {
            postgresDataService.saveRestrictedArea(area);
        }
        log.debug("Successfully seeded {} restricted areas", areas.size());
    }

    private void seedDroneAvailability() {
        List<DroneForServicePoint> dronesForServicePoints = ilpDataService.getDronesForServicePoints();
        log.info("Seeding drone availability for {} service points...", dronesForServicePoints.size());
        
        int totalWindows = 0;
        for (DroneForServicePoint dfsp : dronesForServicePoints) {
            if (dfsp.getDrones() != null) {
                for (ServicePointDroneAvailability spda : dfsp.getDrones()) {
                    if (spda.getAvailability() != null) {
                        postgresDataService.saveAvailability(
                                spda.getId(),
                                dfsp.getServicePointId(),
                                spda.getAvailability()
                        );
                        totalWindows += spda.getAvailability().size();
                    }
                }
            }
        }
        log.debug("Seeded {} availability windows", totalWindows);
    }

    /**
     * Force re-seed the database (clears existing data first).
     * This deletes all drones, service points, restricted areas, and availability,
     * then re-populates from Azure API. Note: delivery_attempts table is NOT cleared.
     */
    @Transactional
    public void reseedAllData() {
        log.info("Re-seeding database from Azure API...");
        
        // Clear all existing data first
        log.debug("Clearing existing data...");
        postgresDataService.deleteAllDrones();
        postgresDataService.deleteAllServicePoints();
        postgresDataService.deleteAllRestrictedAreas();
        postgresDataService.deleteAllAvailability();
        
        // Re-seed from Azure
        seedAllData();
        log.info("Re-seeding complete!");
    }
}
