package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * GeoJSON Feature representing a single geographic feature with geometry and properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeature {
    private String type = "Feature";
    private GeoJsonGeometry geometry;
    private Map<String, Object> properties;
}

