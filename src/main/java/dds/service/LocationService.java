package dds.service;

import org.springframework.stereotype.Service;
import dds.dto.Position;

@Service
public class LocationService {

    private static final double CLOSE_DISTANCE_THRESHOLD = 0.00015;

    /**
     * Calculate the straight-line (Euclidean) distance between two positions.
     *
     * <p>The method treats the {@code lat} and {@code lng} values of {@code Position}
     * as coordinates and returns the Euclidean distance between them. The curvature of the earth
     * is not considered.
     *
     * @param p1 the first position (must not be {@code null})
     * @param p2 the second position (must not be {@code null})
     * @return the Euclidean distance between {@code p1} and {@code p2}
     */
    public double calculateDistance(Position p1, Position p2) {
        double lngDiff = p2.getLng() - p1.getLng();
        double latDiff = p2.getLat() - p1.getLat();
        return Math.sqrt(lngDiff * lngDiff + latDiff * latDiff);
    }

    /**
     * Determine whether two positions are considered close to each other.
     *
     * <p>Two positions are "close" if their Euclidean distance is less than
     * {@link #CLOSE_DISTANCE_THRESHOLD}.
     * Being "close" effectively means they can be treated as the same location
     *
     * @param p1 the first position (must not be {@code null})
     * @param p2 the second position (must not be {@code null})
     * @return {@code true} if the positions are within the close-distance threshold,
     *         {@code false} otherwise
     */
    public boolean isCloseTo(Position p1, Position p2) {
        return calculateDistance(p1, p2) < CLOSE_DISTANCE_THRESHOLD;
    }

    /**
     * Compute the next position when moving from a starting position by a fixed
     * step equal to {@link #CLOSE_DISTANCE_THRESHOLD} in the direction specified
     * by {@code angleDegrees}.
     *
     * <p>Angle convention: 0 degrees corresponds to movement along the positive
     * longitude axis (east), 90 degrees corresponds to movement along the positive
     * latitude axis (north) etc.
     *
     * @param start the starting position (must not be {@code null})
     * @param angleDegrees movement angle in degrees where 0 = east, 90 = north etc.
     *                     angle must be one of the 16 directions (0, 22.5, 45, ..., 337.5)
     *                     this is validated elsewhere
     * @return a new {@code Position} representing the computed next location
     */
    public Position nextPosition(Position start, double angleDegrees) {
        double distance = CLOSE_DISTANCE_THRESHOLD;
        double angleRadians = Math.toRadians(angleDegrees);

        double newLng = start.getLng() + distance * Math.cos(angleRadians);
        double newLat = start.getLat() + distance * Math.sin(angleRadians);

        return new Position(newLng, newLat);
    }

    /**
     * Determine whether a given point lies inside a polygon defined by an array
     * of vertices. Points on the polygon boundary are considered inside.
     *
     * <p>Uses the even-odd algorithm to determine whether the point is inside the polygon.
     * Points on or within {@link #CLOSE_DISTANCE_THRESHOLD} of the polygon boundary
     * are considered inside. This is due to possible uncertainty through
     * usage of Double rather than BigDecimal.
     *
     * @param point the point to test (must not be {@code null})
     * @param polygonVertices array of polygon vertices in order (must not be {@code null} or empty)
     * @return {@code true} if {@code point} is inside the polygon or on its boundary,
     *         {@code false} otherwise
     */
    public boolean isInRegion(Position point, Position[] polygonVertices) {
        if (isPointOnBoundary(point, polygonVertices)) {
            return true; // Boundary is inside (as on spec)
        }

        int crossingCount = 0;
        int vertexCount = polygonVertices.length;
        double pointX = point.getLng();
        double pointY = point.getLat();

        for (int current = 0, previous = vertexCount - 1; current < vertexCount; previous = current++) {
            double currentX = polygonVertices[current].getLng();
            double currentY = polygonVertices[current].getLat();
            double previousX = polygonVertices[previous].getLng();
            double previousY = polygonVertices[previous].getLat();

            boolean edgeCrossed = ((currentY > pointY) != (previousY > pointY)) &&
                    (pointX < (previousX - currentX) * (pointY - currentY) /
                            (previousY - currentY) + currentX);

            if (edgeCrossed) {
                crossingCount++;
            }
        }

        return (crossingCount % 2) == 1;
    }

    private boolean isPointOnBoundary(Position point, Position[] polygonVertices) {
        for (Position vertex : polygonVertices) {
            if (isCloseTo(point, vertex)) {
                return true;
            }
        }

        int vertexCount = polygonVertices.length;
        for (int startIdx = 0, endIdx = vertexCount - 1; startIdx < vertexCount; endIdx = startIdx++) {
            Position start = polygonVertices[startIdx];
            Position end = polygonVertices[endIdx];

            double pointX = point.getLng();
            double pointY = point.getLat();
            double startX = start.getLng();
            double startY = start.getLat();
            double endX = end.getLng();
            double endY = end.getLat();

            if (!(pointY >= Math.min(startY, endY) &&
                    pointY <= Math.max(startY, endY) &&
                    pointX >= Math.min(startX, endX) &&
                    pointX <= Math.max(startX, endX))) {
                continue;
            }

            double lineLength = calculateDistance(start, end);
            if (lineLength < CLOSE_DISTANCE_THRESHOLD) {
                continue;
            }

            double distance = Math.abs((endY - startY) * pointX -
                    (endX - startX) * pointY +
                    endX * startY - endY * startX) / lineLength;

            if (distance < CLOSE_DISTANCE_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a line segment between two points intersects with or passes through a polygon region.
     *
     * <p>This method checks:
     * <ul>
     *   <li>If either endpoint is inside the polygon</li>
     *   <li>If the line segment crosses any edge of the polygon</li>
     * </ul>
     *
     * @param start the start position of the line segment (must not be {@code null})
     * @param end the end position of the line segment (must not be {@code null})
     * @param polygonVertices array of polygon vertices in order (must not be {@code null} or empty)
     * @return {@code true} if the line segment intersects or passes through the polygon,
     *         {@code false} otherwise
     */
    public boolean doesLineIntersectRegion(Position start, Position end, Position[] polygonVertices) {
        if (isInRegion(start, polygonVertices) || isInRegion(end, polygonVertices)) {
            return true;
        }

        int n = polygonVertices.length;
        for (int i = 0; i < n; i++) {
            Position edgeStart = polygonVertices[i];
            Position edgeEnd = polygonVertices[(i + 1) % n]; // next vertex, wraps around

            if (lineSegmentsIntersect(start, end, edgeStart, edgeEnd)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if two line segments intersect.
     *
     * <p>Uses the orientation method to determine if segments (lineStart, lineEnd) and (edgeStart, edgeEnd) intersect.
     *
     * @param lineStart first point of first segment
     * @param lineEnd second point of first segment
     * @param edgeStart first point of second segment
     * @param edgeEnd second point of second segment
     * @return {@code true} if the segments intersect, {@code false} otherwise
     */
    private boolean lineSegmentsIntersect(Position lineStart, Position lineEnd, Position edgeStart, Position edgeEnd) {
        int o1 = orientation(lineStart, lineEnd, edgeStart);
        int o2 = orientation(lineStart, lineEnd, edgeEnd);
        int o3 = orientation(edgeStart, edgeEnd, lineStart);
        int o4 = orientation(edgeStart, edgeEnd, lineEnd);

        // if orientations are different, meaning segments intersect
        if (o1 != o2 && o3 != o4) {
            return true;
        }

        // check for collinearity, meaning the points are on the same line (and therefore overlap)
        if (o1 == 0 && isOnSegment(lineStart, edgeStart, lineEnd)) return true;
        if (o2 == 0 && isOnSegment(lineStart, edgeEnd, lineEnd)) return true;
        if (o3 == 0 && isOnSegment(edgeStart, lineStart, edgeEnd)) return true;
        return o4 == 0 && isOnSegment(edgeStart, lineEnd, edgeEnd);
    }

    /**
     * Find orientation of (p, q, r).
     *
     * @param p first position
     * @param q second position
     * @param r third position
     * @return 0 if collinear, 1 if clockwise, 2 if counterclockwise
     */
    private int orientation(Position p, Position q, Position r) {
        double val = (q.getLat() - p.getLat()) * (r.getLng() - q.getLng()) -
                     (q.getLng() - p.getLng()) * (r.getLat() - q.getLat());

        // if the area is close to zero, its collinear
        if (Math.abs(val) < CLOSE_DISTANCE_THRESHOLD * CLOSE_DISTANCE_THRESHOLD) {
            return 0;
        }
        return (val > 0) ? 1 : 2; // Clockwise (pos area) : Counterclockwise, (neg area)
    }

    /**
     * Check if point q lies on segment pr (given they are collinear).
     *
     * @param p first endpoint of segment
     * @param q point to check
     * @param r second endpoint of segment
     * @return {@code true} if q is on segment pr, {@code false} otherwise
     */
    private boolean isOnSegment(Position p, Position q, Position r) {
        return q.getLng() <= Math.max(p.getLng(), r.getLng()) &&
               q.getLng() >= Math.min(p.getLng(), r.getLng()) &&
               q.getLat() <= Math.max(p.getLat(), r.getLat()) &&
               q.getLat() >= Math.min(p.getLat(), r.getLat());
    }

}
