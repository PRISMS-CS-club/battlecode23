package prisms10.memory;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SharedMemory {


    //    static Set<Integer> locationsToWrite = new HashSet<>(); // Every important location that is scheduled to record into shared memory
    public static HashMap<MemorySection, ArrayList<Integer>> locsToWrite = new HashMap<>();

    static {
        for (MemorySection type : MemorySection.values()) {
            locsToWrite.put(type, new ArrayList<>());
        }
    }

    static int firEmptyInShMem(RobotController rc, MemorySection type) throws GameActionException {
        // return -1 if not found, otherwise return the index
        return existInShMem(rc, MemoryAddress.LOCATION_DEFAULT, type);
    }

    public static int existInShMem(RobotController rc, int x, MemorySection type) throws GameActionException {
        // return -1 if not found, otherwise return the index
        for (int i = type.getStartIdx(); i < type.getEndIdx(); i++) {
            if (rc.readSharedArray(i) == x) {
                return i;
            }
        }
        return -1;
    }

    public static void writeBackLocs(RobotController rc) throws GameActionException {
//        for (int i = SHARED_MEMORY_WELLS; i < SHARED_MEMORY_HQ; i++) {
//            if (rc.readSharedArray(i) == 0) {
//                if (locationsToWrite.size() > 0) {
//                    rc.writeSharedArray(i, locationsToWrite.remove(0));
//                } else {
//                    break;
//                }
//            }
//        }
        // check different types of locs, and write them back into shared mem
        for (MemorySection type : MemorySection.values()) {
            int st = type.getStartIdx();
            int ed = type.getEndIdx();
            ArrayList<Integer> locs = locsToWrite.get(type);
            assert locs != null : "locationsToWrite should be initialized in static block";
            Iterator<Integer> it = locs.iterator();
            while (it.hasNext()) {
                int loc = it.next();
                int empPos;
                if (existInShMem(rc, loc, type) != -1) {
                    // already exist, remove it
                    it.remove();
                } else if ((empPos = firEmptyInShMem(rc, type)) != -1) {
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
    public static ArrayList<Integer> readShMemBySec(RobotController rc, MemorySection sec) throws GameActionException {
        ArrayList<Integer> locs = new ArrayList<>();
        for (int i = sec.getStartIdx(); i < sec.getEndIdx(); i++) {
            int loc = rc.readSharedArray(i);
            if (loc != MemoryAddress.LOCATION_DEFAULT) {
                locs.add(loc);
            }
        }
        return locs;
    }

}
