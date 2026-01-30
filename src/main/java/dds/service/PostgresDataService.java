package dds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dds.dto.*;
import dds.entity.DroneAvailabilityEntity;
import dds.entity.DroneEntity;
import dds.entity.RestrictedAreaEntity;
import dds.entity.ServicePointEntity;
import dds.repository.DroneAvailabilityRepository;
import dds.repository.DroneRepository;
import dds.repository.RestrictedAreaRepository;
import dds.repository.ServicePointRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that reads ILP data from PostgreSQL database.
 * Provides the same interface as IlpDataService but uses local database.
 */
@Service
@Slf4j
public class PostgresDataService {

    private final DroneRepository droneRepository;
    private final ServicePointRepository servicePointRepository;
    private final RestrictedAreaRepository restrictedAreaRepository;
    private final DroneAvailabilityRepository droneAvailabilityRepository;
    private final ObjectMapper objectMapper;

    public PostgresDataService(DroneRepository droneRepository,
                                ServicePointRepository servicePointRepository,
                                RestrictedAreaRepository restrictedAreaRepository,
                                DroneAvailabilityRepository droneAvailabilityRepository,
                                ObjectMapper objectMapper) {
        this.droneRepository = droneRepository;
        this.servicePointRepository = servicePointRepository;
        this.restrictedAreaRepository = restrictedAreaRepository;
        this.droneAvailabilityRepository = droneAvailabilityRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Check if the database has been seeded with data.
     */
    @Transactional(readOnly = true)
    public boolean hasData() {
        return droneRepository.count() > 0;
    }

    // ==================== DRONE OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<Drone> getAllDrones() {
        return droneRepository.findAll().stream()
                .map(this::entityToDrone)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Drone getDroneById(String id) {
        return droneRepository.findById(id)
                .map(this::entityToDrone)
                .orElse(null);
    }

    public Drone saveDrone(Drone drone) {
        log.debug("Saving drone: {}", drone.getId());
        DroneEntity entity = droneToEntity(drone);
        DroneEntity saved = droneRepository.save(entity);
        log.debug("Successfully saved drone: {}", drone.getId());
        return entityToDrone(saved);
    }

    public void deleteDrone(String id) {
        log.debug("Deleting drone: {}", id);
        droneRepository.deleteById(id);
    }

    private Drone entityToDrone(DroneEntity entity) {
        DroneCapability capability = new DroneCapability();
        capability.setCooling(entity.getCooling());
        capability.setHeating(entity.getHeating());
        capability.setCapacity(entity.getCapacity());
        capability.setMaxMoves(entity.getMaxMoves());
        capability.setCostPerMove(entity.getCostPerMove());
        capability.setCostInitial(entity.getCostInitial());
        capability.setCostFinal(entity.getCostFinal());

        return new Drone(entity.getId(), entity.getName(), capability);
    }

    private DroneEntity droneToEntity(Drone drone) {
        DroneEntity entity = new DroneEntity();
        entity.setId(drone.getId());
        entity.setName(drone.getName());
        
        if (drone.getCapability() != null) {
            entity.setCooling(drone.getCapability().getCooling());
            entity.setHeating(drone.getCapability().getHeating());
            entity.setCapacity(drone.getCapability().getCapacity());
            entity.setMaxMoves(drone.getCapability().getMaxMoves());
            entity.setCostPerMove(drone.getCapability().getCostPerMove());
            entity.setCostInitial(drone.getCapability().getCostInitial());
            entity.setCostFinal(drone.getCapability().getCostFinal());
        }
        
        return entity;
    }

    // ==================== SERVICE POINT OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<ServicePoint> getServicePoints() {
        return servicePointRepository.findAll().stream()
                .map(this::entityToServicePoint)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServicePoint getServicePointById(Integer id) {
        return servicePointRepository.findById(id)
                .map(this::entityToServicePoint)
                .orElse(null);
    }

    public ServicePoint saveServicePoint(ServicePoint servicePoint) {
        log.debug("Saving service point: {}", servicePoint.getId());
        ServicePointEntity entity = servicePointToEntity(servicePoint);
        ServicePointEntity saved = servicePointRepository.save(entity);
        log.debug("Successfully saved service point: {}", servicePoint.getId());
        return entityToServicePoint(saved);
    }

    public void deleteServicePoint(Integer id) {
        log.debug("Deleting service point: {}", id);
        servicePointRepository.deleteById(id);
    }

    private ServicePoint entityToServicePoint(ServicePointEntity entity) {
        Position location = new Position(entity.getLng(), entity.getLat());
        // Note: Position doesn't have alt in the DTO, but we store it anyway
        return new ServicePoint(entity.getName(), entity.getId(), location);
    }

    private ServicePointEntity servicePointToEntity(ServicePoint sp) {
        ServicePointEntity entity = new ServicePointEntity();
        entity.setId(sp.getId());
        entity.setName(sp.getName());
        
        if (sp.getLocation() != null) {
            entity.setLng(sp.getLocation().getLng());
            entity.setLat(sp.getLocation().getLat());
            entity.setAlt(50.0);  // Default altitude
        }
        
        return entity;
    }

    // ==================== RESTRICTED AREA OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<RestrictedArea> getRestrictedAreas() {
        return restrictedAreaRepository.findAll().stream()
                .map(this::entityToRestrictedArea)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestrictedArea getRestrictedAreaById(Integer id) {
        return restrictedAreaRepository.findById(id)
                .map(this::entityToRestrictedArea)
                .orElse(null);
    }

    public RestrictedArea saveRestrictedArea(RestrictedArea area) {
        log.debug("Saving restricted area: {}", area.getId());
        RestrictedAreaEntity entity = restrictedAreaToEntity(area);
        RestrictedAreaEntity saved = restrictedAreaRepository.save(entity);
        log.debug("Successfully saved restricted area: {}", area.getId());
        return entityToRestrictedArea(saved);
    }

    public void deleteRestrictedArea(Integer id) {
        log.debug("Deleting restricted area: {}", id);
        restrictedAreaRepository.deleteById(id);
    }

    private RestrictedArea entityToRestrictedArea(RestrictedAreaEntity entity) {
        RestrictedArea area = new RestrictedArea();
        area.setId(entity.getId());
        area.setName(entity.getName());
        
        // Parse vertices from JSON
        if (entity.getVerticesJson() != null) {
            try {
                List<Position> vertices = objectMapper.readValue(
                        entity.getVerticesJson(),
                        new TypeReference<>() {
                        }
                );
                area.setVertices(vertices);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse vertices JSON for restricted area {}", entity.getId(), e);
                area.setVertices(new ArrayList<>());
            }
        }
        
        return area;
    }

    private RestrictedAreaEntity restrictedAreaToEntity(RestrictedArea area) {
        RestrictedAreaEntity entity = new RestrictedAreaEntity();
        entity.setId(area.getId());
        entity.setName(area.getName());
        entity.setLowerLimit(0);
        entity.setUpperLimit(-1);
        
        // Convert vertices to JSON
        if (area.getVertices() != null) {
            try {
                entity.setVerticesJson(objectMapper.writeValueAsString(area.getVertices()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize vertices for restricted area {}", area.getId(), e);
                entity.setVerticesJson("[]");
            }
        }
        
        return entity;
    }

    // ==================== DRONE AVAILABILITY OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<DroneForServicePoint> getDronesForServicePoints() {
        List<ServicePointEntity> servicePoints = servicePointRepository.findAll();
        List<DroneAvailabilityEntity> allAvailability = droneAvailabilityRepository.findAll();

        // Group availability by service point
        Map<Integer, List<DroneAvailabilityEntity>> byServicePoint = allAvailability.stream()
                .collect(Collectors.groupingBy(DroneAvailabilityEntity::getServicePointId));

        List<DroneForServicePoint> result = new ArrayList<>();

        for (ServicePointEntity sp : servicePoints) {
            DroneForServicePoint dfsp = new DroneForServicePoint();
            dfsp.setServicePointId(sp.getId());

            List<DroneAvailabilityEntity> spAvailability = byServicePoint.getOrDefault(sp.getId(), new ArrayList<>());
            
            // Group by drone
            Map<String, List<DroneAvailabilityEntity>> byDrone = spAvailability.stream()
                    .collect(Collectors.groupingBy(DroneAvailabilityEntity::getDroneId));

            List<ServicePointDroneAvailability> drones = new ArrayList<>();
            for (Map.Entry<String, List<DroneAvailabilityEntity>> entry : byDrone.entrySet()) {
                ServicePointDroneAvailability spda = new ServicePointDroneAvailability();
                spda.setId(entry.getKey());
                
                List<DroneAvailabilityWindow> windows = entry.getValue().stream()
                        .map(this::entityToAvailabilityWindow)
                        .collect(Collectors.toList());
                spda.setAvailability(windows);
                
                drones.add(spda);
            }
            
            dfsp.setDrones(drones);
            result.add(dfsp);
        }

        return result;
    }

    @Transactional
    public void saveAvailability(String droneId, Integer servicePointId, List<DroneAvailabilityWindow> windows) {
        log.debug("Saving availability for drone {} at service point {}", droneId, servicePointId);
        // Delete existing availability for this drone at this service point
        List<DroneAvailabilityEntity> existing = droneAvailabilityRepository
                .findByDroneIdAndServicePointId(droneId, servicePointId);
        droneAvailabilityRepository.deleteAll(existing);

        // Save new availability windows
        for (DroneAvailabilityWindow window : windows) {
            DroneAvailabilityEntity entity = new DroneAvailabilityEntity();
            entity.setDroneId(droneId);
            entity.setServicePointId(servicePointId);
            entity.setDayOfWeek(window.getDayOfWeek());
            entity.setFromTime(window.getFrom());
            entity.setUntilTime(window.getUntil());
            droneAvailabilityRepository.save(entity);
        }
        log.debug("Successfully saved availability for drone {}", droneId);
    }

    public void deleteAvailabilityByDroneId(String droneId) {
        log.debug("Deleting availability for drone: {}", droneId);
        List<DroneAvailabilityEntity> toDelete = droneAvailabilityRepository.findByDroneId(droneId);
        droneAvailabilityRepository.deleteAll(toDelete);
    }

    // ==================== BULK DELETE OPERATIONS (for reseeding) ====================

    /**
     * Delete all drones from the database.
     */
    public void deleteAllDrones() {
        droneRepository.deleteAll();
    }

    /**
     * Delete all service points from the database.
     */
    public void deleteAllServicePoints() {
        servicePointRepository.deleteAll();
    }

    /**
     * Delete all restricted areas from the database.
     */
    public void deleteAllRestrictedAreas() {
        restrictedAreaRepository.deleteAll();
    }

    /**
     * Delete all drone availability records from the database.
     */
    public void deleteAllAvailability() {
        droneAvailabilityRepository.deleteAll();
    }

    private DroneAvailabilityWindow entityToAvailabilityWindow(DroneAvailabilityEntity entity) {
        DroneAvailabilityWindow window = new DroneAvailabilityWindow();
        window.setDayOfWeek(entity.getDayOfWeek());
        window.setFrom(entity.getFromTime());
        window.setUntil(entity.getUntilTime());
        return window;
    }
}
