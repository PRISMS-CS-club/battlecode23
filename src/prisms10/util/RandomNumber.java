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
     * @param <T> type of object
     * @return a random object being selected.
     */
    public static <T> T randomSelect(T[] objects, float[] probability) {
        float[] prefixSum = probability.clone();
        for(int i = 1; i < probability.length; i++) {
            prefixSum[i] += prefixSum[i - 1];
        }
        float rand = nextFloat() * prefixSum[prefixSum.length - 1];
        int index = Arrays.binarySearch(prefixSum, rand);
        if(index < 0) {
            index = -index - 1;
        }
        return objects[index];
    }

}
