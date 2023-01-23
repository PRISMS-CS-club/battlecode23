package prisms10.memory;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.ArrayList;
import java.util.function.Predicate;

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
            return IDX_COMBAT;
        }
    },
    COMBAT {
        @Override
        public int getStartIdx() {
            return IDX_COMBAT;
        }

        @Override
        public int getEndIdx() {
            return IDX_COMBAT_END;
        }
    };


    // starting index of each section in shared memory
    public static final int IDX_WELL = 0;
    public static final int IDX_HQ = 8;
    public static final int IDX_SKY_ISLAND = 12;
    public static final int IDX_ENEMY_HQ = 48;
    public static final int IDX_COMBAT = 52;
    public static final int IDX_COMBAT_END = 54;


    /**
     * Reads all addresses in a specific section of the shared memory.
     */
    public ArrayList<Integer> readSection(RobotController rc) throws GameActionException {

        ArrayList<Integer> addresses = new ArrayList<>();

        for (int i = this.getStartIdx(); i < this.getEndIdx(); i++) {
            int address = rc.readSharedArray(i);
            if (address != MemoryAddress.MASK_COORDS) {
                addresses.add(address);
            }
        }

        return addresses;
    }

    public ArrayList<Integer> readSection(RobotController rc, Predicate<Integer> pred) throws GameActionException {

        ArrayList<Integer> addresses = new ArrayList<>();

        for (int i = this.getStartIdx(); i < this.getEndIdx(); i++) {
            int address = rc.readSharedArray(i);
            if (address != MemoryAddress.MASK_COORDS && pred.test(address)) {
                addresses.add(address);
            }
        }

        return addresses;

    }

    /**
     * Checks if an address exists in a specific section of the shared memory.
     *
     * @return -1 if not found, otherwise return the index of the address
     */
    public int contains(RobotController rc, int address) throws GameActionException {

        for (int i = this.getStartIdx(); i < this.getEndIdx(); i++) {
            if (rc.readSharedArray(i) == address) {
                return i;
            }
        }

        return -1;
    }


    public abstract int getStartIdx();

    public abstract int getEndIdx();

    public int getRandIdx() {

        // rand in range [getStartIdx(), getEndIdx())
        return (int) (Math.random() * (getEndIdx() - getStartIdx())) + getStartIdx();
    }
// returns a random index in range [startIdx, endIdx)

}
