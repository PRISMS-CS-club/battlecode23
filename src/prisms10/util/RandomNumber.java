package prisms10.util;

import java.util.Arrays;

public class RandomNumber {

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final java.util.Random rng = new java.util.Random(6147);

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
     * Randomly select one object from an array giving the weight (or probability) each one being selected
     * @param objects array of objects
     * @param probability array of weights. Each element corresponds to the object with same index in object array.
     *                    The data should guarantee that elements in probability array are all non-negative and at least one positive.
     * @param <T> type of object
     * @return a random object being selected.
     */
    public static <T> T randomSelect(T[] objects, float[] probability) {
        float[] prefixSum = probability.clone();
        for(int i = 1; i < prefixSum.length; i++) {
            prefixSum[i] += prefixSum[i - 1];
        }
        float rand = nextFloat() * prefixSum[prefixSum.length - 1];
        int index = upperBound(prefixSum, rand, -1, prefixSum.length);
        return objects[index];
    }

    // search for the smallest number greater than given number in the given range of array
    private static int upperBound(float[] array, float number, int left, int right) {
        if(right - left == 1) {
            return right;
        }
        int mid = (left + right) / 2;
        if(number >= array[mid]) {
            return upperBound(array, number, mid, right);
        } else {
            return upperBound(array, number, left, mid);
        }
    }

}
