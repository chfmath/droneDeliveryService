package dds.service;

import org.springframework.stereotype.Service;
import dds.dto.Position;
import dds.dto.RestrictedArea;

import java.util.*;


@Service
public class PathfindingService {

    private final LocationService locationService;
    private final UnifiedDataService dataService;


    private static final double[] ANGLES = {
        0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5,
        180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5
    };

    public PathfindingService(LocationService locationService, UnifiedDataService dataService) {
        this.locationService = locationService;
        this.dataService = dataService;
    }

    /**
     * A* pathfinding algorithm to find a path while avoiding restricted areas.
     * @param start start Position
     * @param end end Position
     * @return List of Positions representing the path from start to end.
     */
    public List<Position> findPath(Position start, Position end) {
        List<RestrictedArea> restrictedAreas = dataService.getRestrictedAreas();

        // If start and end are 'the same' as per isCloseTo, path is just start
        if (locationService.isCloseTo(start, end)) {
            List<Position> path = new ArrayList<>();
            path.add(start);
            return path;
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, 0, heuristic(start, end), null);
        openSet.add(startNode);
        allNodes.put(positionKey(start), startNode);

        int iterations = 0;
        final int MAX_ITERATIONS = 200000;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            Node current = openSet.poll();
            String currentKey = positionKey(current.position);

            // Check if current position is 'the same' as end position
            if (locationService.isCloseTo(current.position, end)) {
                return reconstructPath(current);
            }

            closedSet.add(currentKey);

            // Explore neighbors in 16 directions
            for (double angle : ANGLES) {
                Position neighbor = locationService.nextPosition(current.position, angle);
                String neighborKey = positionKey(neighbor);

                // Skip if already explored
                if (closedSet.contains(neighborKey)) {
                    continue;
                }

                // Skip if in restricted area
                if (isInRestrictedArea(neighbor, restrictedAreas)) {
                    continue;
                }

                // Skip if path segment crosses restricted area
                if (isPathSegmentInRestrictedArea(current.position, neighbor, restrictedAreas)) {
                    continue;
                }

                double tentativeGScore = current.gCost + 1;

                Node neighborNode = allNodes.get(neighborKey);
                if (neighborNode == null) {
                    neighborNode = new Node(
                        neighbor,
                        tentativeGScore,
                        heuristic(neighbor, end),
                        current
                    );
                    allNodes.put(neighborKey, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeGScore < neighborNode.gCost) {
                    openSet.remove(neighborNode);
                    neighborNode.gCost = tentativeGScore;
                    neighborNode.parent = current;
                    openSet.add(neighborNode);
                }
            }
        }

        // no path found, so return list with only start position
        return Collections.singletonList(start);
    }

    private boolean isInRestrictedArea(Position position, List<RestrictedArea> restrictedAreas) {
        if (restrictedAreas == null || restrictedAreas.isEmpty()) {
            return false;
        }

        for (RestrictedArea area : restrictedAreas) {
            if (area.getVertices() != null && !area.getVertices().isEmpty()) {
                Position[] vertices = area.getVertices().toArray(new Position[0]);
                if (locationService.isInRegion(position, vertices)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPathSegmentInRestrictedArea(Position from, Position to, List<RestrictedArea> restrictedAreas) {
        if (restrictedAreas == null || restrictedAreas.isEmpty()) {
            return false;
        }

        for (RestrictedArea area : restrictedAreas) {
            if (area.getVertices() != null && !area.getVertices().isEmpty()) {
                Position[] vertices = area.getVertices().toArray(new Position[0]);
                if (locationService.doesLineIntersectRegion(from, to, vertices)) {
                    return true;
                }
            }
        }
        return false;
    }


    // backtracks through parent nodes, reconstructing the path from start to goal
    private List<Position> reconstructPath(Node goalNode) {
        List<Position> path = new ArrayList<>();
        Node current = goalNode;

        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    private double heuristic(Position a, Position b) {
        return locationService.calculateDistance(a, b);
    }

    // makes a key for each position, this efficiently checks if a position has been visited
    private String positionKey(Position p) {
        // rounding to 4 decimal places (~10m precision) to reduce number of unique positions (it takes forever otherwise)
        // 10m should be fine as closeTo uses ~15m threshold to consider positions the same
        return String.format("%.4f,%.4f", p.getLng(), p.getLat());
    }

    private static class Node implements Comparable<Node> {
        Position position;
        double gCost;
        double hCost;
        Node parent;

        Node(Position position, double gCost, double hCost, Node parent) {
            this.position = position;
            this.gCost = gCost;
            this.hCost = hCost;
            this.parent = parent;
        }

        double getFCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.getFCost(), other.getFCost());
        }
    }
}

