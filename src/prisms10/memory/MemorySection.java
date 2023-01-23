package prisms10.memory;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.ArrayList;

public enum MemorySection {

    WELL {
        @Override
        public int getStartIdx() {
            return IDX_WELL;
        }

        @Override
        public int getEndIdx() {
            return IDX_HQ;
        }
    },
    HQ {
        @Override
        public int getStartIdx() {
            return IDX_HQ;
        }

        @Override
        public int getEndIdx() {
            return IDX_SKY_ISLAND;
        }
    },
    SKY_ISLAND {
        @Override
        public int getStartIdx() {
            return IDX_SKY_ISLAND;
        }

        @Override
        public int getEndIdx() {
            return IDX_ENEMY_HQ;
        }
    },
    ENEMY_HQ {
        @Override
        public int getStartIdx() {
            return IDX_ENEMY_HQ;
        }

        @Override
        public int getEndIdx() {
            return IDX_ENEMY_HQ_END;
        }
    };

    // starting index of each section in shared memory
    public static final int IDX_WELL = 0;
    public static final int IDX_HQ = 8;
    public static final int IDX_SKY_ISLAND = 12;
    public static final int IDX_ENEMY_HQ = 48;
    public static final int IDX_ENEMY_HQ_END = 52;

    /**
     * Reads all values in a specific section of the shared memory
     */
    public static ArrayList<Integer> read(RobotController rc, MemorySection section) throws GameActionException {

        ArrayList<Integer> addresses = new ArrayList<>();

        for (int i = section.getStartIdx(); i < section.getEndIdx(); i++) {
            int address = rc.readSharedArray(i);
            if (address != MemoryAddress.MASK_COORDS) {
                addresses.add(address);
            }
        }

        return addresses;

    }

    public abstract int getStartIdx();

    public abstract int getEndIdx();

}
