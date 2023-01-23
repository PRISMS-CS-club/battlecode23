package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.*;
import prisms10.util.*;

import java.util.ArrayList;
import java.util.Set;

public class Robot {

    RobotController rc;
    RobotType robotType;
    Randomness random;

    Robot(RobotController rc) {
        this.rc = rc;
        random = new Randomness(rc.getID());
        gridWeight = new float[rc.getMapWidth()][rc.getMapHeight()];
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


    /**
     * Run a single turn for a robot.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public void run() throws GameActionException {
        // scan nearby environment and record information to shared memory
        // TODO: reduce redundant scans to save bytecode
        scanForWells();
        scanForSkyIslands();
        scanForEnemyHeadquarters();
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
        // rc.setIndicatorString("moving toward " + destination);
        // TODO: avoid obstacles
        MapLocation myLocation = rc.getLocation();
        boolean rotateDir = random.nextBoolean();  // when one cannot move toward one direction, whether to rotate left or
        // right
        while (rc.isMovementReady() && (myLocation.x != destination.x || myLocation.y != destination.y)) {
            Direction direction = Map.directionTo(myLocation, destination);
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

        ArrayList<Integer> myHeadquarters = MemorySection.HQ.readSection(rc);
        ArrayList<Integer> enemyHeadquarters = MemorySection.ENEMY_HQ.readSection(rc);
        ArrayList<Integer> wells = MemorySection.WELL.readSection(rc);
        MapLocation curLoc = rc.getLocation();
        // calculate each point's grid weight
        int[] nearbyGrid = new int[Direction.values().length];
        for (int i = 0; i < Direction.values().length - 1; i++) {
            // calculate grid weight of this point
            MapLocation afterMove = curLoc.add(Direction.values()[i]);
            nearbyGrid[i] = GridWeight.INITIAL;
            for (Integer myHQ : myHeadquarters) {
                MapLocation myHQLoc = MemoryAddress.toLocation(myHQ);
                nearbyGrid[i] -= Math.max(0, GridWeight.HQ - Map.sqEuclideanDist(afterMove, myHQLoc) * GridWeight.HQ_DECAY);
            }
            for (Integer enemyHQ : enemyHeadquarters) {
                MapLocation enemyHQLoc = MemoryAddress.toLocation(enemyHQ);
                nearbyGrid[i] += Math.max(0, GridWeight.HQ - Map.sqEuclideanDist(afterMove, enemyHQLoc) * GridWeight.HQ_DECAY);
            }
            for (Integer well : wells) {
                MapLocation wellLoc = MemoryAddress.toLocation(well);
                nearbyGrid[i] += Math.max(0, GridWeight.WELL - Map.sqEuclideanDist(afterMove, wellLoc) * GridWeight.WELL_DECAY);
            }

        }

        Direction randSel = random.randomSelect(Direction.values(), nearbyGrid);
        if (!rc.canMove(randSel)) {
            for (Direction cur : Direction.values()) {
                if (rc.canMove(cur)) {
                    rc.move(cur);
                    return;
                }
            }
        } else {
            rc.move(randSel);
        }

    }


    /**
     * Scan for nearby wells and write their locations to shared memory
     */
    void scanForWells() throws GameActionException {

        for (WellInfo well : rc.senseNearbyWells()) {

            int address = MemoryAddress.fromResourceLocation(well.getResourceType(), well.getMapLocation());

            boolean toWrite = true;
            for (int i = MemorySection.IDX_WELL; i < MemorySection.IDX_HQ; i++) {
                if (address == rc.readSharedArray(i)) {
                    toWrite = false;
                    break;
                }
            }

            if (toWrite) {
                Set<Integer> wells = MemoryCache.locsToWrite.get(MemorySection.WELL);
                assert wells != null : "locationsToWrite should be initialized in static block";
                wells.add(address);
            }

        }

    }

    private void scanForEnemyHeadquarters() throws GameActionException {

        // first check if all enemy headquarters are found
        boolean allFound = true;
        for (int i = MemorySection.ENEMY_HQ.getStartIdx(); i < MemorySection.ENEMY_HQ.getEndIdx(); i++) {
            if (MemoryAddress.isInitial(rc.readSharedArray(i))) {
                allFound = false;
                break;
            }
        }
        if (allFound) return;

        for (RobotInfo robot : rc.senseNearbyRobots()) {
            if (robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() != rc.getTeam()) {
                // make sure this is an enemy headquarters
                int address = MemoryAddress.fromLocation(robot.getLocation());

                if (MemorySection.ENEMY_HQ.contains(rc, address) == -1) {
                    Set<Integer> enemyHeadquarters = MemoryCache.locsToWrite.get(MemorySection.ENEMY_HQ);
                    assert enemyHeadquarters != null : "locationsToWrite should be initialized in static block";
                    enemyHeadquarters.add(address);
                }
            }
        }

    }

    private void scanForSkyIslands() throws GameActionException {

        for (int islandID : rc.senseNearbyIslands()) {

            final int index = islandID + MemorySection.SKY_ISLAND.getStartIdx();
            final int currentMemoryAddress = rc.readSharedArray(index);
            final int occupationStatus = MemoryAddress.fromOccupationStatus(rc.senseTeamOccupyingIsland(islandID), rc.getTeam());

            if (MemoryAddress.isInitial(currentMemoryAddress)) {
                // memory is empty, write the location of the island
                MapLocation locationToWrite = new MapLocation(Integer.MAX_VALUE, Integer.MAX_VALUE);

                // find the bottom left corner of the island
                for (MapLocation islandLocation : rc.senseNearbyIslandLocations(islandID)) {
                    if (islandLocation.compareTo(locationToWrite) < 0) {
                        locationToWrite = islandLocation;
                    }
                }

                // TODO: 存入 cache
                final int newMemoryAddress = occupationStatus | MemoryAddress.fromLocation(locationToWrite);
                if (rc.canWriteSharedArray(index, newMemoryAddress)) {
                    rc.writeSharedArray(index, newMemoryAddress);
                }

            } else {
                // already recorded the location, update the occupation status
                int newMemoryAddress = occupationStatus | MemoryAddress.extractCoords(currentMemoryAddress);

                // TODO: 分类讨论直接写入还是存到 cache 里
                if (newMemoryAddress != currentMemoryAddress && rc.canWriteSharedArray(index, newMemoryAddress)) {
                    rc.writeSharedArray(index, newMemoryAddress);
                }

            }

        }

    }


}
