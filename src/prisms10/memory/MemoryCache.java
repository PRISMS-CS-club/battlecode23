package prisms10.memory;

import battlecode.common.*;

import java.util.*;

public class MemoryCache {


    //    static Set<Integer> locationsToWrite = new HashSet<>(); // Every important location that is scheduled to record into shared memory
    public static HashMap<MemorySection, Set<Integer>> locsToWrite = new HashMap<>();
    public static int invalidSymmetry = -1;

    static {
        for (MemorySection type : MemorySection.values()) {
            locsToWrite.put(type, new HashSet<>());
        }
    }

    static public int firstEmpty(RobotController rc, MemorySection type) throws GameActionException {
        // return -1 if not found, otherwise return the index
        return type.contains(rc, MemoryAddress.MASK_COORDS);
    }

    public static int sizeBySec(RobotController rc, MemorySection type) throws GameActionException {
        int size = 0;
        for (int i = type.getStartIdx(); i < type.getEndIdx(); i++) {
            if (rc.readSharedArray(i) != MemoryAddress.MASK_COORDS) {
                size++;
            }
        }
        return size;
    }

    public static void writeBackLocs(RobotController rc) throws GameActionException {

        if (invalidSymmetry != -1) {
            int indicatorBit = 1 << (14 - invalidSymmetry);
            int prevAddress = rc.readSharedArray(MemorySection.IDX_GAME_STAT);
            int newAddress = prevAddress | indicatorBit;
            if (prevAddress != newAddress) {
                if (rc.canWriteSharedArray(MemorySection.IDX_GAME_STAT, newAddress)) {
                    rc.writeSharedArray(MemorySection.IDX_GAME_STAT, newAddress);
                    invalidSymmetry = -1;
                }
            } else {
                invalidSymmetry = -1;
            }
        }

        // check different types of locations, and write them back into shared mem

        for (MemorySection type : MemorySection.values()) {

            int st = type.getStartIdx();
            int ed = type.getEndIdx();

            Set<Integer> locs = locsToWrite.get(type);
            assert locs != null : "locationsToWrite should be initialized in static block";
            Iterator<Integer> it = locs.iterator();

            while (it.hasNext()) {
                int loc = it.next();
                int empPos;
                if (type.contains(rc, loc) != -1) {
                    // already exist, remove it
                    it.remove();
                } else if ((empPos = firstEmpty(rc, type)) != -1) {
                    // don't exist, need to test if there are still space to write
                    if (rc.canWriteSharedArray(empPos, loc)) {
                        rc.writeSharedArray(empPos, loc);
                        it.remove();
                    }
                    // if no space, don't remove
                }
            }
        }

    }

    /**
     * read all values in a specific section of shared memory
     */
    public static ArrayList<Integer> readBySection(RobotController rc, MemorySection sec) throws GameActionException {
        ArrayList<Integer> locs = new ArrayList<>();
        for (int i = sec.getStartIdx(); i < sec.getEndIdx(); i++) {
            int loc = rc.readSharedArray(i);
            if (loc != MemoryAddress.MASK_COORDS) {
                locs.add(loc);
            }
        }
        return locs;
    }

    public static void delPosInSec(RobotController rc, int pos, MemorySection sec) throws GameActionException {
        if (rc.canWriteSharedArray(pos, MemoryAddress.MASK_COORDS)) {
            rc.writeSharedArray(pos, MemoryAddress.MASK_COORDS);
        }
    }
}
