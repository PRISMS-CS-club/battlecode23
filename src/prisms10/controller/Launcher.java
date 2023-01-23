package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.*;
import prisms10.util.Map;

import java.util.ArrayList;
import java.util.List;

public class Launcher extends Robot {

    public Launcher(RobotController rc) {
        super(rc);
        robotType = RobotType.LAUNCHER;
    }

    boolean inCombatPos = false; // if it reached combat position
    boolean followCombatArea = false; // if this robot follows the combat area

    public void tryMoveToCombatArea() throws GameActionException {
        if (!followCombatArea) return;
        if (MemoryCache.sizeBySec(rc, MemorySection.COMBAT) >= 1) {
            ArrayList<Integer> combatLocs = MemoryCache.readBySection(rc, MemorySection.COMBAT);
            int combatLoc = combatLocs.get(random.nextInt(combatLocs.size()));
            bindTo = MemoryAddress.toLocation(combatLoc);
            assert !bindTo.equals(new MapLocation(0, 0)) : "bug locs size = " + combatLocs.size();
            state = 1;
        } else {
            // no combat area, just go to rand pos
            state = 5;
        }
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        // try to attack someone until can't
        while (true) {
            RobotInfo[] enemyLocation = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            boolean attacked = false;
            for (RobotInfo enemy : enemyLocation) {
                if (rc.canAttack(enemy.location)) {
                    rc.attack(enemy.location);
                    attacked = true;
                    break;
                }
            }
            if (!attacked) {
                break;
            }
        }

        MemoryCache.writeBackLocs(rc);
        switch (state) {
            case 0:
                rc.setIndicatorString("initial state");
                // 40 percent to move to a combat area
                if (random.nextFloat() < 0.4) {
                    followCombatArea = true;
                }

                tryMoveToCombatArea();

                float randNum = random.nextFloat();
                boolean occupied = false; // see if the launcher have something to do
                if (randNum < 0.2) {
                    List<Integer> headquarters = MemorySection.HQ.readSection(rc);
                    if (headquarters.size() > 0) {
                        bindTo =
                                MemoryAddress.toLocation(headquarters.get(Math.abs(random.nextInt()) % headquarters.size()));
                        state = 1;
                        occupied = true;
                    }
                } else if (randNum < 0.55) {
                    List<Integer> headquarters = MemorySection.ENEMY_HQ.readSection(rc);
                    if (headquarters.size() > 0) {
                        bindTo =
                                MemoryAddress.toLocation(headquarters.get(Math.abs(random.nextInt()) % headquarters.size()));
                        state = 1;
                        occupied = true;
                    }
                } else if (randNum < 0.9) {
                    List<Integer> skyIsland = MemorySection.SKY_ISLAND.readSection(rc);
                    if (skyIsland.size() > 0) {
                        bindTo = MemoryAddress.toLocation(skyIsland.get(Math.abs(random.nextInt()) % skyIsland.size()));
                        state = 1;
                        occupied = true;
                    }
                } else {
                    state = 5;
                }
                if (!occupied) {
                    state = 5;
                }
            case 4:
                // explore randomly
                tryMoveToCombatArea();
                rc.setIndicatorString("exploring randomly");
                randomMove();
                break;
            case 5:
                bindTo = random.getRandLoc(rc);
                rc.setIndicatorString("moving to randomly assigned location " + bindTo);
                moveToward(bindTo);
                tryMoveToCombatArea();
                if (Map.diagonalDist(rc.getLocation(), bindTo) < 3) {
                    //// bindTo = null;
                    state = 3;
                }
                break;
            case 1:
                rc.setIndicatorString("Targeting to " + bindTo.x + ", " + bindTo.y);
                if (rc.canSenseLocation(bindTo)) {
                    RobotInfo robot = rc.senseRobotAtLocation(bindTo);
                    if (robot != null && robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() == rc.getTeam().opponent()) {
                        state = 2;
                    } else {
                        state = 3;
                        inCombatPos = true;
                    }
                    break;
                }
                moveToward(bindTo);
                break;
            case 3:
                // if cannot see the target position, move toward it
                rc.setIndicatorString("moving toward " + bindTo + " with kept in sight");

                // if reached combat area, but this position is updated to be not combat area, move to random position
                ArrayList<Integer> combatLocs = MemoryCache.readBySection(rc, MemorySection.COMBAT);
                boolean nearCombat = false;
                for (int encoded : combatLocs) {
                    MapLocation combatLoc = MemoryAddress.toLocation(encoded);
                    if (Map.diagonalDist(rc.getLocation(), combatLoc) < 3) {
                        nearCombat = true;
                        break;
                    }
                }

                if (!nearCombat) {
                    state = 5;
                    break;
                }

                if (!rc.canSenseLocation(bindTo)) {
                    moveToward(bindTo);
                }
                // if moving too close to the target, move away from it
                if (Map.diagonalDist(bindTo, rc.getLocation()) <= 1) {
                    moveToward(bindTo, false, true);
                }
                // otherwise, random move
                Direction dir2 = Direction.values()[random.nextInt(Direction.values().length)];
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
                if (Map.sqEuclideanDist(bindTo, moveToward(bindTo, true, false)) > 9) {
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
