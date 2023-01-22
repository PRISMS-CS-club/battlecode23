package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.*;
import prisms10.util.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class Robot {

    RobotController rc;
    RobotType robotType;

    Robot(RobotController rc) {
        this.rc = rc;
        this.gridWeight = new float[rc.getMapWidth()][rc.getMapHeight()];
    }


    MapLocation bindTo = null;                       // An important location (such as a well) that the robot is bound to
    int state;                                       // Current state of robot. Its meaning depends on the type of robot
    int stateCounter = 0;                            // Number of rounds the robot has been staying in current state
    final int STATE_COUNTER_MAX = 450;
    MapInfo[][] mapInfos = new MapInfo[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];    // What the robot knows about the map
    int[][] passable = new int[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];    // What the robot knows about the map (passable)
    // 0 -> cannot pass, 1 can pass, -1 unknown
    float[][] gridWeight = null;                     // assign a weight to each grid

    void changeState(int newState) {
        state = newState;
        stateCounter = 0;
    }


    public void run() throws GameActionException {
        // scan nearby environment and record information to shared memory
        scanForWells();
        scanForSkyIslands();
        scanForEnemyHQs();
    }


    /**
     * Move this robot toward a given position one step.
     *
     * @param destination destination
     * @param toward      true for moving toward the position, false for moving away from the position
     * @param performMove if the robot need to actually perform the movement, or only return the destination but not move to it
     * @return MapLocation the final position of the robot
     */
    MapLocation moveToward(MapLocation destination, boolean toward, boolean performMove) throws GameActionException {
//        rc.setIndicatorString("moving toward " + destination);
        // TODO (avoid obstacles)
        MapLocation myLocation = rc.getLocation();
        boolean rotateDir = Randomness.nextBoolean();  // when one cannot move toward one direction, whether to rotate left or right
        while (rc.isMovementReady() && (myLocation.x != destination.x || myLocation.y != destination.y)) {
            Direction direction = Location.toDirection(destination.x - myLocation.x, destination.y - myLocation.y);
            if (!toward) {
                direction = direction.opposite();
            }
            boolean canMove = false;
            for (int i = 0; i < 6; i++) {
                // search either clockwise or counterclockwise for the first direction the bot can move to
                // search for at most 8 rounds
                if (rc.canMove(direction)) {
                    if (performMove) {
                        rc.move(direction);
                    }
                    myLocation = myLocation.add(direction);
                    canMove = true;
                    break;
                }
                direction = rotateDir ? direction.rotateLeft() : direction.rotateRight();
            }
            if (!canMove) {
                break;
            }
        }
        return myLocation;
    }

    void moveToward(MapLocation dest) throws GameActionException {
        moveToward(dest, true, true);
    }

    void randomMove() throws GameActionException {
        ArrayList<Integer> myHeadquarters = SharedMemory.readBySection(rc, MemorySection.HQ);
        ArrayList<Integer> enemyHeadquarters = SharedMemory.readBySection(rc, MemorySection.ENEMY_HQ);
        ArrayList<Integer> wells = SharedMemory.readBySection(rc, MemorySection.WELL);
        MapLocation curLoc = rc.getLocation();
        // calculate each point's grid weight
        int[] nearbyGrid = new int[Direction.values().length];
        boolean[] canMoveToDir = new boolean[Direction.values().length];
        for (int i = 0; i < Direction.values().length; i++) {
            canMoveToDir[i] = rc.canMove(Direction.values()[i]);

            // calculate grid weight of this point
            MapLocation afterMove = curLoc.add(Direction.values()[i]);
            nearbyGrid[i] = GridWeight.INITIAL;
            for (Integer myHQ : myHeadquarters) {
                MapLocation myHQLoc = MemoryAddress.toLocation(myHQ);
                nearbyGrid[i] -= Math.max(0, GridWeight.HQ - Location.sqEuclidDistance(afterMove, myHQLoc) * GridWeight.HQ_DECAY);
            }
            for (Integer enemyHQ : enemyHeadquarters) {
                MapLocation enemyHQLoc = MemoryAddress.toLocation(enemyHQ);
                nearbyGrid[i] += Math.max(0, GridWeight.HQ - Location.sqEuclidDistance(afterMove, enemyHQLoc) * GridWeight.HQ_DECAY);
            }
            for (Integer well : wells) {
                MapLocation wellLoc = MemoryAddress.toLocation(well);
                nearbyGrid[i] += Math.max(0, GridWeight.WELL - Location.sqEuclidDistance(afterMove, wellLoc) * GridWeight.WELL_DECAY);
            }

        }

        Direction randSel = null;
        while(!rc.canMove(randSel = Randomness.randomSelect(Direction.values(), nearbyGrid))){
            // rand select until can move
        }
        rc.move(randSel);

    }


    /**
     * Scan for nearby wells and write their locations to shared memory
     */
    void scanForWells() throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            int s = MemoryAddress.fromLocation(well.getMapLocation(), well.getResourceType());
            boolean toWrite = true;
            for (int i = MemorySection.IDX_WELL; i < MemorySection.IDX_HQ; i++) {
                if (s == rc.readSharedArray(i)) {
                    toWrite = false;
                    break;
                }
            }
            if (toWrite) {
                Set<Integer> wellLocs = SharedMemory.locsToWrite.get(MemorySection.WELL);
                assert wellLocs != null : "locationsToWrite should be initialized in static block";
                wellLocs.add(s);
            }
        }
    }

    void scanForEnemyHQs() throws GameActionException {
        // first check if all enemy headquarters are found
        boolean allFound = true;
        for (int i = MemorySection.ENEMY_HQ.getStartIdx(); i < MemorySection.ENEMY_HQ.getEndIdx(); i++) {
            if (rc.readSharedArray(i) == MemoryAddress.DEFAULT_COORDINATES) {
                allFound = false;
                break;
            }
        }
        if (allFound) {
            return;
        }

        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() != rc.getTeam()) {
                // make sure this is an enemy headquarters
                int s = MemoryAddress.fromLocation(robot.getLocation());
                boolean toWrite = true;
                if (SharedMemory.exist(rc, s, MemorySection.ENEMY_HQ) == -1) {
                    Set<Integer> enemyHQlocs = SharedMemory.locsToWrite.get(MemorySection.ENEMY_HQ);
                    assert enemyHQlocs != null : "locationsToWrite should be initialized in static block";
                    enemyHQlocs.add(s);
                }
            }
        }
    }

    void scanForSkyIslands() throws GameActionException {

        Team myTeam = rc.getTeam();

        for (int islandID : rc.senseNearbyIslands()) {

            int islandSharedInfo = rc.readSharedArray(islandID + MemorySection.IDX_SKY_ISLAND);
            Team occupiedTeam = rc.senseTeamOccupyingIsland(islandID);
            int stat = MemoryAddress.fromTeam(occupiedTeam, myTeam);
            if (islandSharedInfo != MemoryAddress.DEFAULT_COORDINATES) {
                int islandInfoNew = (islandSharedInfo & (MemoryAddress.MASK_X_COORDINATE | MemoryAddress.MASK_Y_COORDINATE)) | (stat << 12);
                if (islandInfoNew != islandSharedInfo && rc.canWriteSharedArray(islandID + MemorySection.IDX_SKY_ISLAND, islandInfoNew)) {
                    rc.writeSharedArray(islandID + MemorySection.IDX_SKY_ISLAND, islandInfoNew);
                }
                return;
            }
            MapLocation location = new MapLocation(Integer.MAX_VALUE, Integer.MAX_VALUE);
            for (MapLocation islandLocation : rc.senseNearbyIslandLocations(islandID)) {
                if (islandLocation.x <= location.x && islandLocation.y <= location.y) {
                    location = islandLocation;
                }
            }
            int locationInt = MemoryAddress.fromLocation(location) | (stat << 14);
            if (rc.canWriteSharedArray(islandID + MemorySection.IDX_SKY_ISLAND, locationInt)) {
                rc.writeSharedArray(islandID + MemorySection.IDX_SKY_ISLAND, locationInt);
            }

        }

    }


    boolean isInVisRange(MapLocation len) {
        return (len.x * len.x) + (len.y * len.y) <= getVisDis();
    }

    boolean isInVisRange(MapLocation loc1, MapLocation loc2) {
        int dx = loc1.x - loc2.x;
        int dy = loc1.y - loc2.y;
        return (dx * dx) + (dy * dy) <= getVisDis();
    }


    int getVisDis() {
        switch (robotType) {
            case HEADQUARTERS:
            case AMPLIFIER:
                return 34;
            case CARRIER:
            case LAUNCHER:
            case DESTABILIZER:
            case BOOSTER:
                return 20;
            default:
                return -1;
        }
    }

    int getActDis() {
        switch (robotType) {
            case HEADQUARTERS:
            case CARRIER:
                return 9;
            case LAUNCHER:
                return 16;
            case DESTABILIZER:
                return 13;
            default:
                return -1;
        }
    }

}
