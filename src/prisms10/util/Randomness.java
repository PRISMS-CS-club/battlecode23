package prisms10.util;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;
import prisms10.memory.*;

import java.util.ArrayList;

/**
 * A class that provides a random number generator and random selection related methods.
 */
public class Randomness {

    private final java.util.Random rng;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The RNG is provided by the java.util.Random class.
     * Here, we *seed* the RNG with each robot's unique ID; this makes sure our robots behave differently and
     * that we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    public Randomness(long seed) {
        rng = new java.util.Random(seed);
    }

    public int nextInt() {
        return rng.nextInt();
    }

    public int nextInt(int bound) {
        return rng.nextInt(bound);
    }

    public float nextFloat() {
        return rng.nextFloat();
    }

    public boolean nextBoolean() {
        return rng.nextBoolean();
    }

    /**
     * Randomly selects one object from an array giving the probability (weight) of each one being selected.
     *
     * @param objects     array of objects
     * @param probability array of weights. Each element corresponds to the object with same index in the
     *                    {@code objects} array. All weights should be non-negative and at least one of them should be
     *                    positive.
     * @return the random object selected
     */
    public <T> T randomSelect(T[] objects, int[] probability) {
        int[] prefixSum = probability.clone();
        for (int i = 1; i < prefixSum.length; i++) {
            prefixSum[i] += prefixSum[i - 1];
        }
        int rand = (int) (nextFloat() * prefixSum[prefixSum.length - 1]);
        int index = upperBound(prefixSum, rand, -1, prefixSum.length);
        return objects[index];
    }

    /**
     * Finds the index of the first element in array that is greater than the given number.
     * The array should be sorted in ascending order.
     */
    public int upperBound(int[] array, int number, int left, int right) {
        if (right - left == 1) {
            return right;
        }
        int mid = (left + right) / 2;
        if (number >= array[mid]) {
            return upperBound(array, number, mid, right);
        } else {
            return upperBound(array, number, left, mid);
        }
    }

    public MapLocation getRandLoc(RobotController rc) {
        return new MapLocation(nextInt(rc.getMapWidth()), nextInt(rc.getMapHeight()));
    }

    public MapLocation randSelectEnemyHeadquarters(RobotController rc) throws GameActionException {
        ArrayList<MapLocation> enemyHeadquarters = new ArrayList<>();
        for (int encoded : MemorySection.ENEMY_HQ.readSection(rc)) {
            enemyHeadquarters.add(MemoryAddress.toLocation(encoded));
        }
        if (enemyHeadquarters.size() == 0) {
            return null;
        }
        return enemyHeadquarters.get(Math.abs(nextInt()) % enemyHeadquarters.size());
    }


}
