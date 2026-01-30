package dds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Medical dispatch record")
public class MedDispatchRec {
    @NotNull
    @Schema(description = "Unique dispatch identifier", example = "1")
    private Integer id;

    @Schema(description = "Date of delivery (YYYY-MM-DD)", example = "2025-12-22")
    private LocalDate date;

    @Schema(description = "Time of delivery (HH:MM)", example = "14:30")
    private LocalTime time;

    @Valid
    @NotNull
    @Schema(description = "Delivery requirements")
    private MedDispatchRequirements requirements;

    @Valid
    @Schema(description = "Delivery destination coordinates")
    private Position delivery;
}
