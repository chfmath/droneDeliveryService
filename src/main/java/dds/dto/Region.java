package dds.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import dds.validation.ValidPolygon;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {

    @NotNull(message = "Region name must not be null")
    private String name;

    @Valid
    @NotNull(message = "Vertices array must not be null")
    @ValidPolygon
    private Position[] vertices;
}
