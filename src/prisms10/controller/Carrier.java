package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.MemoryAddress;
import prisms10.memory.MemorySection;
import prisms10.memory.SharedMemory;
import prisms10.util.Location;
import prisms10.util.Random;

import java.util.ArrayList;
import java.util.List;

public class Carrier extends Robot {

    public Carrier(RobotController rc) {
        super(rc);
        robotType = RobotType.CARRIER;
    }

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    @Override
    public void run() throws GameActionException {

        super.run();

        Anchor anchor = rc.getAnchor();
        // update current state
        rc.setIndicatorString("current state: " + state + ", state counter = " + stateCounter);
        if (stateCounter > STATE_COUNTER_MAX && anchor == null) {
            rc.disintegrate();
        }
        // perform an operation according to its state
        switch (state) {
            case 0:
                // if the robot is holding an anchor, go to state 2
                if (anchor != null && bindTo != null) {
                    changeState(2);
                    break;
                } else if (anchor != null) {
                    // in wandering state, if can place the anchor, place it immediately
                    if (rc.canPlaceAnchor()) {
                        rc.placeAnchor();
                    }
                }
                // try to get an anchor
                if (bindTo != null) {
                    if (rc.canTakeAnchor(bindTo, Anchor.ACCELERATING)) {
                        rc.takeAnchor(bindTo, Anchor.ACCELERATING);
                        bindTo = null;
                        changeState(2);
                        break;
                    }
                    if (rc.canTakeAnchor(bindTo, Anchor.STANDARD)) {
                        rc.takeAnchor(bindTo, Anchor.STANDARD);
                        bindTo = null;
                        changeState(2);
                        break;
                    }
                }
                // if the bot can still carry stuff, try to find a well
                if (rc.getWeight() < 40) {
                    List<MapLocation> locations = new ArrayList<>();
                    for (int i = MemorySection.IDX_WELL; i < MemorySection.IDX_HQ; i++) {
                        // find a valid well and set it for target
                        int pos = rc.readSharedArray(i);
                        if (pos != MemoryAddress.LOCATION_DEFAULT) {
                            locations.add(MemoryAddress.toLocation(pos));
                        }
                    }
                    if (locations.size() != 0) {
                        // if the robot can find a well, target toward the well
                        bindTo = locations.get(Math.abs(Random.nextInt()) % locations.size());
                        changeState(1);
                        break;
                    }
                }
                // if the bot does not find any job, wander randomly
                Direction dir = Direction.values()[(Random.nextInt() % 8 + 8) % 8];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
                break;

            case 1:
                stateCounter++;
                rc.setIndicatorString("Targeting to " + bindTo.x + ", " + bindTo.y);
                MapLocation current = rc.getLocation();
                if (rc.canCollectResource(bindTo, -1)) {
                    rc.setIndicatorString("Collecting resource");
                    // if can collect resource, collect
                    rc.collectResource(bindTo, -1);
                    if (rc.getWeight() >= 40) {
                        bindTo = null;
                        changeState(3);
                    }
                } else {
                    // otherwise, move toward the destination
                    moveToward(bindTo);
                }
                break;

            case 2:
                stateCounter++;
                if (bindTo == null) {
                    int minDist = Integer.MAX_VALUE;
                    for (int i = 0; i < 36; i++) {
                        int read = rc.readSharedArray(i + MemorySection.IDX_SKY_ISLAND);
                        if (read != MemoryAddress.LOCATION_DEFAULT && ((read & MemoryAddress.MEMORY_MARK) == 0)) {
                            // if the island has not been marked, navigate the bot to it
                            MapLocation skyIsland = MemoryAddress.toLocation(read);
                            int distance = Location.diagonalDist(skyIsland, rc.getLocation());
                            if (distance < minDist) {
                                minDist = distance;
                                bindTo = skyIsland;
                            }
                        }
                    }
                }
                // if can place anchor, place it
                if (rc.canPlaceAnchor()) {
                    rc.placeAnchor();
                    bindTo = null;
                    changeState(3);
                }
                // otherwise, walk toward the sky island
                if (bindTo != null) {
                    moveToward(bindTo);
                } else {
                    changeState(0);
                }
                break;

            case 3:
                stateCounter++;
                if (bindTo == null) {
                    // find the headquarter with the smallest distance
                    int minDist = Integer.MAX_VALUE;
                    for (int i = MemorySection.IDX_HQ; i < MemorySection.IDX_SKY_ISLAND; i++) {
                        int read = rc.readSharedArray(i);
                        if (read != MemoryAddress.LOCATION_DEFAULT) {
                            MapLocation headquarter = MemoryAddress.toLocation(read);
                            int distance = Location.diagonalDist(headquarter, rc.getLocation());
                            if (distance < minDist) {
                                minDist = distance;
                                bindTo = headquarter;
                            }
                        }
                    }
                }
                // try to transfer every resource back to headquarter
                boolean transferred = false;          // whether the bot successfully transferred any resource to headquarter
                for (ResourceType resourceType : ResourceType.values()) {
                    if (rc.canTransferResource(bindTo, resourceType, rc.getResourceAmount(resourceType))) {
                        rc.transferResource(bindTo, resourceType, rc.getResourceAmount(resourceType));
                        transferred = true;
                    }
                }
                if (!transferred) {
                    moveToward(bindTo);
                } else if (rc.getWeight() <= 0) {
                    changeState(0);
                }
                break;
        }
        // clear up repeated information in locationsToWrite array
        SharedMemory.writeBackLocs(rc);

    }

}
