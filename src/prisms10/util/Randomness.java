package prisms10.util;

import battlecode.common.RobotController;

/**
 * A class that provides a random number generator and random selection related methods.
 */
public class Randomness {

    /**
     * A random number generator. We will use this RNG to make some random moves. The Random class is provided by the
     * java.util.Random import at the top of this file. Here, we *seed* the RNG with a constant number (114514); this
     * makes sure we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    private static final java.util.Random rng = new java.util.Random(114514);

    public static int nextInt() {

        return rng.nextInt();
    }

    public static int nextInt(int bound) {

        return rng.nextInt(bound);
    }

    public static float nextFloat() {

        return rng.nextFloat();
    }

    public static boolean nextBoolean() {

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
    public static <T> T randomSelect(T[] objects, int[] probability) {
        int[] prefixSum = probability.clone();
        for (int i = 1; i < prefixSum.length; i++) {
            prefixSum[i] += prefixSum[i - 1];
        }
        int rand = (int) (nextFloat() * prefixSum[prefixSum.length - 1]);
        int index = upperBound(prefixSum, rand, -1, prefixSum.length);
        return objects[index];
    }

    public static <T> T randomSelect(T[] objects, int[] probability, int st, int ed) {
        T[] tmpObjs = (T[]) new Object[ed - st];
        int[] tmpProbs = new int[ed - st];
        for (int i = st; i < ed; i++) {
            tmpObjs[i - st] = objects[i];
            tmpProbs[i - st] = probability[i];
        }
        return randomSelect(tmpObjs, tmpProbs);
    }

    /**
     * Finds the index of the first element in array that is greater than the given number.
     * The array should be sorted in ascending order.
     */
    public static int upperBound(int[] array, int number, int left, int right) {
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

    public static void setSeed(long seed){
        rng.setSeed(seed);
    }
}
