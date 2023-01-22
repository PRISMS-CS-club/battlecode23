package prisms10.memory;

import battlecode.common.MapLocation;
import battlecode.common.ResourceType;
import battlecode.common.Team;

public class MemoryAddress {

    // memory address masks
    public static final int MASK_COUNTER = 0xC000;
    public static final int MASK_SUBTYPE = 0x3000;
    public static final int MASK_X_COORDINATE = 0x0FC0;
    public static final int MASK_Y_COORDINATE = 0x003F;
    public static final int MASK_COORDS = 0x0FFF; // 0x0FFF = 12 bits of 1s


    public static boolean isInitial(int address) {
        return address == MASK_COORDS;
    }

    /**
     * Extracts the coordinates from the memory address.
     * The x and y coordinates are stored with 6 bits each in the lower 12 bits of the address.
     *
     * @param address the memory address to extract the location from
     * @return the location stored in the memory address
     */
    public static MapLocation toLocation(int address) {
        return new MapLocation((address & MASK_X_COORDINATE) >> 6, address & MASK_Y_COORDINATE);
    }

    public static int fromLocation(MapLocation location, ResourceType type) {
        return (type.resourceID << 12) + fromLocation(location);
    }

    public static int fromLocation(MapLocation location) {
        return (location.x << 6) + location.y;
    }

    public static int fromTeam(Team occupying, Team self) {
        return occupying == Team.NEUTRAL ? 0x0000 : occupying == self ? 0x1000 : 0x2000;
    }

    public static int extractCoords(int address) {
        return address & MASK_COORDS;
    }

}
