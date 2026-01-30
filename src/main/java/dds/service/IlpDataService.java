package dds.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import dds.dto.Drone;
import dds.dto.DroneForServicePoint;
import dds.dto.RestrictedArea;
import dds.dto.ServicePoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class IlpDataService {

    private final RestTemplate restTemplate;
    private final String ilpServiceUrl;

    // @Qualifier is safer to use here in case there are multiple 'String' beans (chooses the one called 'ilpServiceUrl')
    @Autowired
    public IlpDataService(RestTemplate restTemplate, @Qualifier("ilpServiceUrl") String ilpServiceUrl) {
        this.restTemplate = restTemplate;
        this.ilpServiceUrl = ilpServiceUrl;
    }

    public List<Drone> getAllDrones() {
        String endpoint = "/drones";
        try {
            ResponseEntity<Drone[]> response =
                    restTemplate.getForEntity(ilpServiceUrl + endpoint, Drone[].class);
            Drone[] body = response.getBody();
            if (body == null) {
                log.warn("Azure API returned null body for endpoint: {}", endpoint);
                return Collections.emptyList();
            }
            log.debug("Successfully fetched {} drones from {}", body.length, endpoint);
            return Arrays.asList(body);
        } catch (Exception e) {
            log.error("Failed to fetch {} from Azure API", endpoint, e);
            return Collections.emptyList();
        }
    }

    public List<DroneForServicePoint> getDronesForServicePoints() {
        String endpoint = "/drones-for-service-points";
        try {
            ResponseEntity<DroneForServicePoint[]> response =
                    restTemplate.getForEntity(ilpServiceUrl + endpoint,
                            DroneForServicePoint[].class);
            DroneForServicePoint[] body = response.getBody();
            if (body == null) {
                log.warn("Azure API returned null body for endpoint: {}", endpoint);
                return Collections.emptyList();
            }
            log.debug("Successfully fetched {} drone-service-point mappings from {}", body.length, endpoint);
            return Arrays.asList(body);
        } catch (Exception e) {
            log.error("Failed to fetch {} from Azure API", endpoint, e);
            return Collections.emptyList();
        }
    }

    public List<ServicePoint> getServicePoints() {
        String endpoint = "/service-points";
        try {
            ResponseEntity<ServicePoint[]> response =
                    restTemplate.getForEntity(ilpServiceUrl + endpoint,
                            ServicePoint[].class);
            ServicePoint[] body = response.getBody();
            if (body == null) {
                log.warn("Azure API returned null body for endpoint: {}", endpoint);
                return Collections.emptyList();
            }
            log.debug("Successfully fetched {} service points from {}", body.length, endpoint);
            return Arrays.asList(body);
        } catch (Exception e) {
            log.error("Failed to fetch {} from Azure API", endpoint, e);
            return Collections.emptyList();
        }
    }

    public List<RestrictedArea> getRestrictedAreas() {
        String endpoint = "/restricted-areas";
        try {
            ResponseEntity<RestrictedArea[]> response =
                    restTemplate.getForEntity(ilpServiceUrl + endpoint,
                            RestrictedArea[].class);
            RestrictedArea[] body = response.getBody();
            if (body == null) {
                log.warn("Azure API returned null body for endpoint: {}", endpoint);
                return Collections.emptyList();
            }
            log.debug("Successfully fetched {} restricted areas from {}", body.length, endpoint);
            return Arrays.asList(body);
        } catch (Exception e) {
            log.error("Failed to fetch {} from Azure API", endpoint, e);
            return Collections.emptyList();
        }
    }
}
