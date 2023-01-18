package prisms10.prototype;

import battlecode.common.*;

import java.util.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case HEADQUARTERS:     runHeadquarters(rc);  break;
                    case CARRIER:      runCarrier(rc);   break;
                    case LAUNCHER: runLauncher(rc); break;
                    case BOOSTER: // Examplefuncsplayer doesn't use any of these robot types below.
                    case DESTABILIZER: // You might want to give them a try!
                    case AMPLIFIER:       break;
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*** static functions and constants ***/
    // constants
    static final Direction[][] DIRECTION_MAP = new Direction[][] {
            {Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST},
            {Direction.WEST, Direction.CENTER, Direction.EAST},
            {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST}
    };
    // helper functions
    static MapLocation intToLoc(short number) {
        return new MapLocation((number & 0x3F80) >> 7, number & 0x007F);
    }
    static short locToInt(MapLocation location, ResourceType type) {
        return (short) ((type.resourceID << 14) + (location.x << 7) + location.y);
    }
    static short locToInt(MapLocation location) {
        return (short) ((location.x << 7) + location.y);
    }
    static Direction toDirection(int dx, int dy) {
        return DIRECTION_MAP[sign(dy) + 1][sign(dx) + 1];
    }
    static int sign(int x) {
        return Integer.compare(x, 0);
    }
    static int taxicabDistance(MapLocation loc1, MapLocation loc2) {
        return Math.abs(loc2.x - loc1.x) + Math.abs(loc2.y - loc1.y);
    }

    /**
     * Scanning all wells around a robot. If found any new well, record it into shared memory
     * @param rc robot controller
     */
    static void scanForWells(RobotController rc) throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        for(WellInfo well : wells) {
            short s = locToInt(well.getMapLocation(), well.getResourceType());
            boolean toWrite = true;
            for(int i = 0; i < 16; i++) {
                if(s == rc.readSharedArray(i)) {
                    toWrite = false;
                    break;
                }
            }
            if(toWrite) {
                locationsToWrite.add(s);
            }
        }
    }

    /**
     * Move this robot toward a given position one step.
     * @param rc robot controller
     * @param destination destination
     */
    static void moveToward(RobotController rc, MapLocation destination) throws GameActionException {
        // TODO (avoid obstacles)
        MapLocation current = rc.getLocation();
        Direction direction = toDirection(destination.x - current.x, destination.y - current.y);
        Direction dirL = direction.rotateLeft(), dirR = direction.rotateRight();
        if(rc.canMove(direction)) {
            rc.move(direction);
        } else if(rc.canMove(dirL)) {
            // if the bot cannot move directly toward the destination, try sideways
            rc.move(dirL);
        } else if(rc.canMove(dirR)) {
            rc.move(dirR);
        } else if(rc.canMove(dirL.rotateLeft())) {
            rc.move(dirL.rotateLeft());
        } else if(rc.canMove(dirR.rotateRight())) {
            rc.move(dirR.rotateRight());
        }
    }

    static final short LOCATION_DEFAULT = 0x3FFF;

    // the first few robots the headquarters will build
    static final RobotType[] initialRobots = new RobotType[] {
            RobotType.CARRIER, RobotType.LAUNCHER
    };

    /*** local variables kept by each robot ***/
    static Set<Short> locationsToWrite = new HashSet<>(); // Every important location that is scheduled to record into shared memory
    static MapLocation bindTo = null;                     // An important location (such as a well) that the robot is bound to
    static int state;                                     // Current state of robot. Its meaning depends on the type of robot

    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
        // initialize
        if((rc.readSharedArray(63) & 0x8000) == 0) {
            rc.writeSharedArray(63, 0x8000);
            // initialize shared memory
            for(int i = 0; i < 20; i++) {
                rc.writeSharedArray(i, LOCATION_DEFAULT);
            }
            // initialize nearby well info
            scanForWells(rc);
        }
        // record the current headquarter's position into shared memory
        short currentLocation = locToInt(rc.getLocation());
        for(int i = 16; i < 20; i++) {
            int data = rc.readSharedArray(i);
            if(data == currentLocation) {
                // repeated information found in shared memory
                break;
            }
            if(rc.readSharedArray(i) == LOCATION_DEFAULT) {
                rc.writeSharedArray(i, currentLocation);
                break;
            }
        }
        // Pick a direction to build in.
        Direction dir = Direction.values()[rng.nextInt(Direction.values().length)];
        MapLocation newLoc = rc.getLocation().add(dir);
        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
        }
        if (rng.nextBoolean()) {
            // Let's try to build a carrier.
            rc.setIndicatorString("Trying to build a carrier");
            if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                rc.buildRobot(RobotType.CARRIER, newLoc);
            }
        } else {
            // Let's try to build a launcher.
            rc.setIndicatorString("Trying to build a launcher");
            if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                rc.buildRobot(RobotType.LAUNCHER, newLoc);
            }
        }
    }

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runCarrier(RobotController rc) throws GameActionException {
        // record information of observed wells
        scanForWells(rc);
        // update current state
        if(rc.getNumAnchors(Anchor.STANDARD) + rc.getNumAnchors(Anchor.ACCELERATING) > 0) {
            state = 1;
        } else if(rc.getWeight() >= 40 && state != 2) {
            bindTo = null;
            state = 2;
        } else if(rc.getWeight() <= 0 && bindTo != null) {
            state = 0;
        } else if(rc.getWeight() <= 0) {
            state = 3;
        }
        rc.setIndicatorString("current state: " + state);
        // perform an operation according to its state
        switch(state) {
            case 0:
                rc.setIndicatorString("Targeting to " + bindTo.x + ", " + bindTo.y);
                MapLocation current = rc.getLocation();
                if(rc.canCollectResource(bindTo, -1)) {
                    // if can collect resource, collect
                    rc.collectResource(bindTo, -1);
                    if(rc.getWeight() >= 40) {
                        bindTo = null;
                    }
                } else {
                    // otherwise, move toward the destination
                    moveToward(rc, bindTo);
                }
                break;
            case 1:
                // TODO
                break;
            case 2:
                if(bindTo == null) {
                    // find the headquarter with the smallest distance
                    int minDist = Integer.MAX_VALUE;
                    MapLocation targetLocation = null;
                    for(int i = 16; i < 20; i++) {
                        short read = (short) rc.readSharedArray(i);
                        if(read != LOCATION_DEFAULT) {
                            MapLocation headquarter = intToLoc(read);
                            int distance = taxicabDistance(headquarter, rc.getLocation());
                            if(distance < minDist) {
                                minDist = distance;
                                targetLocation = headquarter;
                            }
                        }
                    }
                    bindTo = targetLocation;
                }
                boolean transferred = false;          // whether the bot successfully transferred any resource to headquarter
                for(ResourceType resourceType : ResourceType.values()) {
                    if(rc.canTransferResource(bindTo, resourceType, rc.getResourceAmount(resourceType))) {
                        rc.transferResource(bindTo, resourceType, rc.getResourceAmount(resourceType));
                        transferred = true;
                    }
                }
                if(!transferred) {
                    moveToward(rc, bindTo);
                } else if(rc.getWeight() <= 0) {
                    bindTo = null;
                }
                break;
            case 3:
                if(bindTo == null) {
                    for(int i = 0; i < 8; i++) {
                        // find a valid well and set it for target
                        short pos = (short) rc.readSharedArray(i);
                        if(pos != LOCATION_DEFAULT) {
                            bindTo = intToLoc(pos);
                            break;
                        }
                    }
                }
                if(bindTo != null) {
                    state = 0;
                    break;
                }
                Direction dir = Direction.values()[(rng.nextInt() % 8 + 8) % 8];
                if(rc.canMove(dir)) {
                    rc.move(dir);
                }
                break;
        }
        // clear up repeated information in locationsToWrite array
        for(Short location : locationsToWrite) {
            for(int i = 0; i < 8; i++) {
                if(rc.readSharedArray(i) == LOCATION_DEFAULT && rc.canWriteSharedArray(i, location)) {
                    rc.writeSharedArray(i, location);
                    break;
                }
            }
        }
    }

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        // scan for wells in its observable range
        scanForWells(rc);
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 0) {
            // MapLocation toAttack = enemies[0].location;
            MapLocation toAttack = rc.getLocation().add(Direction.EAST);

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = Direction.values()[rng.nextInt(Direction.values().length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
