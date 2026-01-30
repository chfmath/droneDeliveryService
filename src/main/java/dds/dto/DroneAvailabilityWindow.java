package dds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Availability window for a drone")
public class DroneAvailabilityWindow {
    @Schema(description = "Day of week", example = "MONDAY")
    private DayOfWeek dayOfWeek;

    @Schema(description = "Start time (HH:MM:SS)", example = "00:00:00")
    private LocalTime from;

    @Schema(description = "End time (HH:MM:SS)", example = "23:59:59")
    private LocalTime until;
}
