package prisms10.util;

public class Random {

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
}
