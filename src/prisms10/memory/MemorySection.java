package prisms10.memory;

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

    public abstract int getStartIdx();

    public abstract int getEndIdx();

}
