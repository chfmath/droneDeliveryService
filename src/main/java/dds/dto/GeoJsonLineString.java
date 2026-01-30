package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoJsonLineString {
    private String type = "LineString";

    private List<double[]> coordinates;

    public GeoJsonLineString(List<double[]> coordinates) {
        this.type = "LineString";
        this.coordinates = coordinates;
    }
}

