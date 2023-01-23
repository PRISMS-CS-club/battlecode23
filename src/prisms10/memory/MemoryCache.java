package prisms10.memory;

import battlecode.common.*;

import java.util.*;
import java.util.function.Predicate;

public class MemoryCache {


    //    static Set<Integer> locationsToWrite = new HashSet<>(); // Every important location that is scheduled to record into shared memory
    public static HashMap<MemorySection, Set<Integer>> locsToWrite = new HashMap<>();

    static {
        for (MemorySection type : MemorySection.values()) {
            locsToWrite.put(type, new HashSet<>());
        }
    }

    static int firstEmpty(RobotController rc, MemorySection type) throws GameActionException {
        // return -1 if not found, otherwise return the index
        return exist(rc, MemoryAddress.MASK_COORDS, type);
    }

    public static int exist(RobotController rc, int x, MemorySection type) throws GameActionException {
        // return -1 if not found, otherwise return the index
        for (int i = type.getStartIdx(); i < type.getEndIdx(); i++) {
            if (rc.readSharedArray(i) == x) {
                return i;
            }
        }
        return -1;
    }

    public static void writeBackLocs(RobotController rc) throws GameActionException {
        // check different types of locs, and write them back into shared mem
        for (MemorySection type : MemorySection.values()) {
            int st = type.getStartIdx();
            int ed = type.getEndIdx();
            Set<Integer> locs = locsToWrite.get(type);
            assert locs != null : "locationsToWrite should be initialized in static block";
            Iterator<Integer> it = locs.iterator();
            while (it.hasNext()) {
                int loc = it.next();
                int empPos;
                if (exist(rc, loc, type) != -1) {
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

}
