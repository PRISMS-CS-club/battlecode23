package prisms10.memory;

import battlecode.common.MapLocation;
import battlecode.common.ResourceType;
import battlecode.common.Team;

public class MemoryAddress {

    // Memory address masks
    public static final int MASK_COUNTER = 0xC000;
    public static final int MASK_SUBTYPE = 0x3000;
    public static final int MASK_X_COORDINATE = 0x0FC0;
    public static final int MASK_Y_COORDINATE = 0x003F;
    public static final int DEFAULT_COORDINATES = 0x0FFF; // 0x0FFF = 12 bits of 1s


    // Helper methods
    public static MapLocation toLocation(int address) {
        // The x and y coordinates are stored with 6 bits each in the lower 12 bits of the address
        return new MapLocation((address & MASK_X_COORDINATE) >> 6, address & MASK_Y_COORDINATE);
    }

    public static int fromLocation(MapLocation location, ResourceType type) {
        return (type.resourceID << 12) + fromLocation(location);
    }

    public static int fromLocation(MapLocation location) {
        return (location.x << 6) + location.y;
    }

    public static int fromTeam(Team team, Team myTeam) {
        return (team == Team.NEUTRAL) ? 0 : ((team == myTeam) ? 1 : 2);
    }

}
