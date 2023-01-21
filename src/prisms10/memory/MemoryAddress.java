package prisms10.memory;

import battlecode.common.MapLocation;
import battlecode.common.ResourceType;
import battlecode.common.Team;

public class MemoryAddress {

    // memory address masks
    public static final int MEMORY_COUNT = 0xC000;
    public static final int MEMORY_MARK = 0x3000;
    public static final int MEMORY_X = 0x0FC0;
    public static final int MEMORY_Y = 0x003F;
    public static final int LOCATION_DEFAULT = 0x03FF; // 0x03FF = 10 bits of 1s


    // helper methods
    public static MapLocation toLocation(int address) {
        return new MapLocation((address & MEMORY_X) >> 6, address & MEMORY_Y);
        // store x with 5 bits, and y with 5 bits
    }

    public static int fromLocation(MapLocation location, ResourceType type) {
        return (type.resourceID << 12) + ((location.x << 6) + location.y);
    }

    public static int fromLocation(MapLocation location) {
        return ((location.x << 6) + location.y);
    }

    public static int fromTeam(Team team, Team myTeam) {
        return (team == Team.NEUTRAL) ? 0 : ((team == myTeam) ? 1 : 2);
    }

}
