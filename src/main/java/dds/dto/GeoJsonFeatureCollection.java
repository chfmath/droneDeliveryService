package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON FeatureCollection containing multiple geographic features.
 * Used for visualizing restricted areas, service points, and delivery locations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeatureCollection {
    private String type = "FeatureCollection";
    private List<GeoJsonFeature> features;
}

