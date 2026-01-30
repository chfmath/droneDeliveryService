package dds.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicePoint {
    private String name;
    @NotNull
    private Integer id;
    @NotNull
    private Position location;
}

