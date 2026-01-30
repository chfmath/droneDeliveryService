package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GeoJSON Geometry representing Point, LineString, or Polygon.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonGeometry {
    private String type;  // "Point", "LineString", "Polygon"
    private Object coordinates;  // Can be double[] for Point, List<double[]> for LineString, List<List<double[]>> for Polygon
}

