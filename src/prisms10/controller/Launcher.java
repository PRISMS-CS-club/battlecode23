package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.MemoryAddress;
import prisms10.memory.MemorySection;
import prisms10.memory.SharedMemory;
import prisms10.util.Randomness;
import prisms10.util.Location;

import java.util.List;

public class Launcher extends Robot {

    public Launcher(RobotController rc) {
        super(rc);
        robotType = RobotType.LAUNCHER;
    }

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    @Override
    public void run() throws GameActionException {
        super.run();

        // try to attack someone
        RobotInfo[] enemyLocation = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo enemy : enemyLocation) {
            if (rc.canAttack(enemy.location)) {
                rc.attack(enemy.location);
                break;
            }
        }
        SharedMemory.writeBackLocs(rc);
        switch (state) {
            case 0:
                rc.setIndicatorString("initial state");
                float randNum = Randomness.nextFloat();
                boolean occupied = false; // see if the launcher have something to do
                if (randNum < 0.2) {
                    List<Integer> headquarters = SharedMemory.readBySection(rc, MemorySection.HQ);
                    if (headquarters.size() > 0) {
                        bindTo = MemoryAddress.toLocation(headquarters.get(Math.abs(Randomness.nextInt()) % headquarters.size()));
                        state = 1;
                        occupied = true;
                    }
                } else if (randNum < 0.55) {
                    List<Integer> headquarters = SharedMemory.readBySection(rc, MemorySection.ENEMY_HQ);
                    if (headquarters.size() > 0) {
                        bindTo = MemoryAddress.toLocation(headquarters.get(Math.abs(Randomness.nextInt()) % headquarters.size()));
                        state = 1;
                        occupied = true;
                    }
                } else if (randNum < 0.9) {
                    List<Integer> skyIsland = SharedMemory.readBySection(rc, MemorySection.SKY_ISLAND);
                    if (skyIsland.size() > 0) {
                        bindTo = MemoryAddress.toLocation(skyIsland.get(Math.abs(Randomness.nextInt()) % skyIsland.size()));
                        state = 1;
                        occupied = true;
                    }
                } else {
                    state = 5;
                }
                if (!occupied){
                    state = 5;
                }
            case 5:
                bindTo = Location.getRandLoc(rc);
                rc.setIndicatorString("moving to randomly assigned location " + bindTo);
                moveToward(bindTo);
                if (Location.diagonalDist(rc.getLocation(), bindTo) < 3) {
                    bindTo = null;
                    state = 3;
                    break;
                }
            case 4:
                // explore randomly
                rc.setIndicatorString("exploring randomly");
                randomMove();
                break;
            case 1:
                rc.setIndicatorString("Targeting to " + bindTo.x + ", " + bindTo.y);
                if (rc.canSenseLocation(bindTo)) {
                    RobotInfo robot = rc.senseRobotAtLocation(bindTo);
                    if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() == rc.getTeam().opponent()) {
                        state = 2;
                    } else {
                        state = 3;
                    }
                    break;
                }
                moveToward(bindTo);
                break;
            case 3:
                // if cannot see the target position, move toward it
                rc.setIndicatorString("moving toward " + bindTo + "with kept in sight");
                if (!rc.canSenseLocation(bindTo)) {
                    moveToward(bindTo);
                }
                // if moving too close to the target, move away from it
                if (Location.diagonalDist(bindTo, rc.getLocation()) <= 1) {
                    moveToward(bindTo, false, true);
                }
                // otherwise, random move
                Direction dir2 = Direction.values()[Randomness.nextInt(Direction.values().length)];
                if (rc.canMove(dir2)) {
                    rc.move(dir2);

                }
                break;
            case 2:
                // TODO (extra launcher blocking the map)
                rc.setIndicatorString("staying the fixed pos");
                MapLocation location = rc.getLocation();
                rc.setIndicatorString("Staying at position " + location);
                // because headquarter's action radius is 9, the launcher have to stay 9 distance away from headquarter
                if (Location.sqEuclidDistance(bindTo, moveToward(bindTo, true, false)) > 9) {
                    moveToward(bindTo);
                } else {
                    Direction windDirection = rc.senseMapInfo(location).getCurrentDirection();
                    if (windDirection != null && windDirection != Direction.CENTER && rc.canMove(windDirection.opposite())) {
                        rc.move(windDirection.opposite());
                    }
                }
                break;
        }
    }

}
