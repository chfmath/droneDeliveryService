package dds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import dds.service.DeliveryPlanningService;
import dds.service.DeliveryHistoryService;
import dds.dto.MedDispatchRec;
import dds.dto.MedDispatchRequirements;
import dds.dto.Position;
import dds.dto.DeliveryPathResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The Application class serves as the entry point for the Spring Boot application.
 * It is annotated with @SpringBootApplication, which is a convenience annotation
 * that adds @Configuration, @EnableAutoConfiguration, and @ComponentScan.
 * It triggers the auto-configuration of Spring Boot and scans for Spring components
 * in the package and sub-packages of this class.
 * <p>
 * The main method executes the SpringApplication.run method to launch the application.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner demoDataRunner(DeliveryPlanningService deliveryPlanningService, DeliveryHistoryService deliveryHistoryService) {
        return args -> {
            LocalDate demoDate = LocalDate.of(2025, 12, 22);
            LocalTime demoTime = LocalTime.of(14, 30);

            // Create sample dispatches
            MedDispatchRec rec1 = new MedDispatchRec(1, demoDate, demoTime,
                new MedDispatchRequirements(5.0, false, false, 20.0),
                new Position(-3.19, 55.94));
            MedDispatchRec rec2 = new MedDispatchRec(2, demoDate, demoTime,
                new MedDispatchRequirements(3.0, true, false, 15.0),
                new Position(-3.20, 55.95));
            MedDispatchRec rec3 = new MedDispatchRec(3, demoDate, demoTime,
                new MedDispatchRequirements(7.0, false, true, 25.0),
                new Position(-3.18, 55.93));
            MedDispatchRec rec4 = new MedDispatchRec(4, demoDate, demoTime,
                new MedDispatchRequirements(200.0, false, false, 10.0),
                new Position(-3.21, 55.96));

            // List of dispatches for each call
            List<List<MedDispatchRec>> demoDispatches = List.of(
                List.of(rec1),
                List.of(rec2),
                List.of(rec3),
                List.of(rec4)
            );

            for (List<MedDispatchRec> dispatches : demoDispatches) {
                DeliveryPathResponse response = deliveryPlanningService.calcDeliveryPath(dispatches, null);
                if (response != null && response.getDronePaths() != null && !response.getDronePaths().isEmpty()) {
                    deliveryHistoryService.logSuccess(dispatches, response);
                } else {
                    deliveryHistoryService.logFailure(dispatches);
                }
            }
        };
    }
}
