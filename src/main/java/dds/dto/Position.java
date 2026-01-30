package dds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Geographic coordinates")
public class Position {
    @NotNull(message = "Longitude must not be null")
    @Min(value = -180, message = "Longitude must be greater than or equal to -180")
    @Max(value = 180, message = "Longitude must be less than or equal to 180")
    @Schema(description = "Longitude coordinate", example = "-3.19")
    private Double lng;

    @NotNull(message = "Latitude must not be null")
    @Min(value = -90, message = "Latitude must be greater than or equal to -90")
    @Max(value = 90, message = "Latitude must be less than or equal to 90")
    @Schema(description = "Latitude coordinate", example = "55.94")
    private Double lat;
}
