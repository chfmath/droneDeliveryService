package dds.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionRequest {
    @Valid
    @NotNull(message = "Position must not be null")
    private Position position;

    @Valid
    @NotNull(message = "Region must not be null")
    private Region region;
}
