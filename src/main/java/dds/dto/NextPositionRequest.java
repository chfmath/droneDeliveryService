package dds.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import dds.validation.ValidDroneAngle;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextPositionRequest {
    @Valid
    @NotNull(message = "Start position must not be null")
    private Position start;

    @NotNull(message = "Angle must not be null")
    @ValidDroneAngle
    private Double angle;
}
