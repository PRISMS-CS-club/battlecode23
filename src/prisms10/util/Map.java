package prisms10.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Map {

    private static final Direction[][] DIRECTION_MAP = {
            {Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST},
            {Direction.WEST, Direction.CENTER, Direction.EAST},
            {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST}
    };


    /**
     * Returns the direction to move given a change in x and y.
     *
     * @param dx delta x
     * @param dy delta y
     */
    public static Direction directionTo(int dx, int dy) {
        return DIRECTION_MAP[sign(dy) + 1][sign(dx) + 1];
    }

    /**
     * Returns the direction to move given the start and destination.
     */
    public static Direction directionTo(MapLocation start, MapLocation dest) {
        return directionTo(dest.x - start.x, dest.y - start.y);
    }


    // distance calculation functions

    /**
     * Squared Euclidean distance between two points.
     */
    public static int sqEuclideanDist(MapLocation loc1, MapLocation loc2) {
        final int dx = loc1.x - loc2.x;
        final int dy = loc1.y - loc2.y;
        return dx * dx + dy * dy;
    }

    /**
     * A helper function for A* search. Calculates heuristic cost {@code h(n)}.
     * Since we are allowed to move in 8 directions, the diagonal distance is used.
     *
     * @return the diagonal distance between the two locations
     */
    public static int diagonalDist(MapLocation loc1, MapLocation loc2) {
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);
        return Math.max(dx, dy);
    }

    /**
     * A helper function for A* search. Calculates sum of the distance already travelled and the heuristic cost.
     * <p>
     * {@code f(n) = g(n) + h(n)}
     *
     * @param distTraveled the distance already travelled {@code g(n)}
     *                     (the number of steps taken from the start to the current node)
     * @param current      the current node (the node to expand)
     * @param destination  the destination node
     * @return the estimated cost {@code f(n)}
     */
    public static int aStarCost(int distTraveled, MapLocation current, MapLocation destination) {
        return distTraveled + diagonalDist(current, destination);
    }


    public static MapLocation[] getCircleRimLocs(MapLocation cent, int radSqr) {
        int rad = (int) Math.sqrt(radSqr);
        MapLocation[] vecs = new MapLocation[rad + 1]; // each x value of radius corresbond to a y value
        for (int i = 0; i <= rad; i++) {
            // x^2 + y^2 = r^2
            // so that y = sqrt(r^2 - x^2)
            vecs[i] = new MapLocation(i, (int) Math.sqrt(radSqr - i * i));
        }
        MapLocation[] ret = new MapLocation[vecs.length * 4];

        // center +x, +y | +x, -y | -x, +y | -x, -y from vec
        for (int i = 0; i < vecs.length; i++) {
            ret[i] = new MapLocation(cent.x + vecs[i].x, cent.y + vecs[i].y);
            ret[i + vecs.length] = new MapLocation(cent.x + vecs[i].x, cent.y - vecs[i].y);
            ret[i + vecs.length * 2] = new MapLocation(cent.x - vecs[i].x, cent.y + vecs[i].y);
            ret[i + vecs.length * 3] = new MapLocation(cent.x - vecs[i].x, cent.y - vecs[i].y);
        }

        return ret;
    }

    public static MapLocation getClosestLocOnCircToTar(MapLocation cent, int radSqr, MapLocation tar) {
        MapLocation[] rimLocs = getCircleRimLocs(cent, radSqr);
        int minDist = Integer.MAX_VALUE;
        MapLocation ret = null;
        for (MapLocation loc : rimLocs) {
            int dist = Map.diagonalDist(loc, tar);
            if (dist < minDist) {
                minDist = dist;
                ret = loc;
            }
        }
        return ret;
    }


    // symmetries

    /**
     * Calculates the horizontal reflection of a given point.
     *
     * @param loc       the point to reflect
     * @param mapHeight the height of the map
     */
    public static MapLocation reflectHorizontally(MapLocation loc, int mapHeight) {
        return new MapLocation(loc.x, mapHeight - loc.y - 1);
    }

    /**
     * Calculates the vertical reflection of a given point.
     *
     * @param loc      the point to reflect
     * @param mapWidth the width of the map
     */
    public static MapLocation reflectVertically(MapLocation loc, int mapWidth) {
        return new MapLocation(mapWidth - loc.x - 1, loc.y);
    }

    /**
     * Calculates the rotational reflection of a given point.
     *
     * @param loc       the point to reflect
     * @param mapWidth  the width of the map
     * @param mapHeight the height of the map
     */
    public static MapLocation reflectRotationally(MapLocation loc, int mapWidth, int mapHeight) {
        return new MapLocation(mapWidth - loc.x - 1, mapHeight - loc.y - 1);
    }

    /**
     * Calculates either the horizontal, vertical, or rotational reflection of a given point.
     *
     * @param loc       the point to reflect
     * @param mapWidth  the width of the map
     * @param mapHeight the height of the map
     * @param symmetry  the symmetry to use. {@code 0} for horizontal, {@code 1} for vertical, {@code 2} for rotational
     */
    public static MapLocation reflect(MapLocation loc, int mapWidth, int mapHeight, int symmetry) {
        switch (symmetry) {
            case 0:
                return reflectHorizontally(loc, mapHeight);
            case 1:
                return reflectVertically(loc, mapWidth);
            case 2:
                return reflectRotationally(loc, mapWidth, mapHeight);
            default:
                throw new IllegalArgumentException("invalid symmetry: " + symmetry);
        }
    }


    // private functions

    /**
     * Returns the sign of the given integer.
     *
     * @param x the integer
     * @return the value {@code 0} if {@code x == y};
     * the value {@code -1} if {@code x < y}; and
     * the value {@code 1} if {@code x > y}
     */
    private static int sign(int x) {
        return Integer.compare(x, 0);
    }


}
