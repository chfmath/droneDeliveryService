package dds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryPathResponse {
    private double totalCost;
    private double totalMoves;
    private List<DronePath> dronePaths;
}

