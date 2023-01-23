package prisms10.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Location {

    static final Direction[][] DIRECTION_MAP = new Direction[][]{
            {Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST},
            {Direction.WEST, Direction.CENTER, Direction.EAST},
            {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST}
    };


    static int sign(int x) {
        return Integer.compare(x, 0);
    }

    public static Direction toDirection(int dx, int dy) {
        return DIRECTION_MAP[sign(dy) + 1][sign(dx) + 1];
    }


    /**
     * Square of Euclidean distance between two points
     *
     * @param loc1 first location
     * @param loc2 second location
     * @return Euclidean distance squared
     */
    public static int sqEuclidDistance(MapLocation loc1, MapLocation loc2) {
        int dx = loc1.x - loc2.x;
        int dy = loc1.y - loc2.y;
        return dx * dx + dy * dy;
    }


    /**
     * A helper function used for a* search, returns the estimated cost (g-cost)
     * It is diagonal distance, because we are allowed to move` in 8 directions
     *
     * @param loc1 start location
     * @param loc2 end location
     */
    public static int diagonalDist(MapLocation loc1, MapLocation loc2) {
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);
        return Math.max(dx, dy);
    }

    /**
     * A helper function for A* search. Returns the estimated cost plus distance already travelled
     */
    public static int aStarHeuristic(int stepUsed, MapLocation loc1, MapLocation loc2) {
        return stepUsed + diagonalDist(loc1, loc2);
        // f(x) = g(x) + h(x)
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
            int dist = Location.diagonalDist(loc, tar);
            if (dist < minDist) {
                minDist = dist;
                ret = loc;
            }
        }
        return ret;
    }


}
