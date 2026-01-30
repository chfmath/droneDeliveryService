package dds.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistanceRequest {
    @Valid
    @NotNull(message = "Position1 must not be null")
    private Position position1;

    @Valid
    @NotNull(message = "Position2 must not be null")
    private Position position2;
}
