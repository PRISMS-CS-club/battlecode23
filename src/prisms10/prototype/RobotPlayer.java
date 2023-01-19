package prisms10.prototype;

import battlecode.common.*;
import java.util.*;
import java.util.List;

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
     * @param rc The RobotController object. You use it to perform actions from this robot, and to get
     *           information on its current status. Essentially your portal to interacting with the world.
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
                switch (rType = rc.getType()) {
                    case HEADQUARTERS:
                        runHeadquarters(rc);
                        break;
                    case CARRIER:
                        runCarrier(rc);
                        break;
                    case LAUNCHER:
                        runLauncher(rc);
                        break;
                    case BOOSTER: // Examplefuncsplayer doesn't use any of these robot types below.
                    case DESTABILIZER: // You might want to give them a try!
                    case AMPLIFIER:
                        break;
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
    static final Direction[][] DIRECTION_MAP = new Direction[][]{
            {Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST},
            {Direction.WEST, Direction.CENTER, Direction.EAST},
            {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST}
    };
    static final int SHARED_MEMORY_WELLS = 0;         // starting position of well section in shared memory
    static final int SHARED_MEMORY_HEADQUARTERS = 8;
    static final int SHARED_MEMORY_SKY_ISLAND = 12;

    // helper functions
    static MapLocation intToLoc(int number) {
        return new MapLocation((number & 0x3F80) >> 7, number & 0x007F);
    }

    static int locToInt(MapLocation location, ResourceType type) {
        return ((type.resourceID << 14) + (location.x << 7) + location.y);
    }

    static int locToInt(MapLocation location) {
        return ((location.x << 7) + location.y);
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
     *
     * @param rc robot controller
     */
    static void scanForWells(RobotController rc) throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            int s = locToInt(well.getMapLocation(), well.getResourceType());
            boolean toWrite = true;
            for (int i = SHARED_MEMORY_WELLS; i < SHARED_MEMORY_HEADQUARTERS; i++) {
                if (s == rc.readSharedArray(i)) {
                    toWrite = false;
                    break;
                }
            }
            if (toWrite) {
                locationsToWrite.add(s);
            }
        }
    }
    static void scanForSkyIsland(RobotController rc) throws GameActionException {
        for(int islandID : rc.senseNearbyIslands()) {
            if(rc.readSharedArray(islandID) != LOCATION_DEFAULT) {
                // repeated island
                continue;
            }
            MapLocation location = new MapLocation(Integer.MAX_VALUE, Integer.MAX_VALUE);
            for(MapLocation islandLocation : rc.senseNearbyIslandLocations(islandID)) {
                if(islandLocation.x <= location.x && islandLocation.y <= location.y) {
                    location = islandLocation;
                }
            }
            int locationInt = locToInt(location);
            if(rc.canWriteSharedArray(islandID + SHARED_MEMORY_SKY_ISLAND, locationInt)) {
                rc.writeSharedArray(islandID + SHARED_MEMORY_SKY_ISLAND, locationInt);
            }
        }
    }

    /**
     * A helper function used for a* search, returns the estimated cost (g-cost)
     * It is diagonal distance, because we are allowed to move in 8 directions
     *
     * @param loc1 start location
     * @param loc2 end location
     */
    static int diagnoDist(MapLocation loc1, MapLocation loc2) {
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);
        return Math.max(dx, dy);
    }

    /**
     * helper function for a* search, returns the estimated cost plus distance already travelled
     */
    static int aStarHeuristic(int stepUsed, MapLocation loc1, MapLocation loc2) {
        return stepUsed + diagnoDist(loc1, loc2);
        // f(x) = g(x) + h(x)
    }

    static int getVisDis(){
        switch (rType){
            case HEADQUARTERS:
                return 34;
            case CARRIER:
                return 20;
            case LAUNCHER:
                return 20;
            case DESTABILIZER:
                return 20;
            case BOOSTER:
                return 20;
            case AMPLIFIER:
                return 34;
            default:
                return -1;
        }
    }

    static boolean isInVisRange(MapLocation len){
        return (len.x * len.x) + (len.y * len.y) <= getVisDis();
    }

    static boolean isInVisRange(MapLocation loc1, MapLocation loc2){
        int dx = loc1.x - loc2.x;
        int dy = loc1.y - loc2.y;
        return (dx * dx) + (dy * dy) <= getVisDis();
    }

    /**
     * move the robot to destination, returns the step taken
     * */
    static int moveTowardInVisRange(RobotController rc, MapLocation destination, boolean doMove) throws GameActionException {
        assert isInVisRange(rc.getLocation(), destination) : "only support moving in vis range for this function";
        assert destination != null : "destination cannot be null";

        MapLocation start = rc.getLocation();
        int[][] dist = new int[rc.getMapWidth()][rc.getMapHeight()];
        HashMap<MapLocation, MapLocation> prePos = new HashMap<>();
        boolean reached = false;

        PriorityQueue<MapLocation> nextPq = new PriorityQueue<>(new Comparator<MapLocation>() {
            public int compare(MapLocation o1, MapLocation o2) {
                int o1cost = aStarHeuristic(dist[o1.x][o1.y], o1, destination);
                int o2cost = aStarHeuristic(dist[o2.x][o2.y], o2, destination);
                // sort the cost from smallest to largest
                return o2cost - o1cost;
            }
        }); // available adjacent locations, sorted by cost from low to high

        dist[start.x][start.y] = 0;
        nextPq.add(start);
        System.out.println("start: " + start + " dest: " + destination);
        while(nextPq.size() > 0){
            MapLocation cur = nextPq.poll();
            assert cur != null : "nextPq should not be empty";
            if (cur.equals(destination)){
                reached = true;
                System.out.println("found");
                break;
            }
            // test if able to put it in the queue
            for (Direction dir : Direction.allDirections()){
                if (dir == Direction.CENTER) continue;
                MapLocation nexPos = new MapLocation(dir.dx + cur.x, dir.dy + cur.y);
//                rc.setIndicatorString("checking nextpos " + nexPos);

                // test if in the range from 0 to rc.getMapWidth() - 1 and 0 to rc.getMapHeight() - 1
                if (!isInVisRange(rc.getLocation(), nexPos) || !rc.onTheMap(nexPos) ){
//                    System.out.println("out of range: " + nexPos);
                    continue;
                }
                if (!rc.sensePassability(nexPos)){
                    continue;
                }
                dist[nexPos.x][nexPos.y] = dist[cur.x][cur.y] + 1;
//                rc.setIndicatorString("pushing " + nexPos + " into pq");
                nextPq.add(nexPos);
                prePos.put(nexPos, cur);
            }
        }
        if (!reached) return -1;
        if (!doMove) return dist[destination.x][destination.y];
        ArrayList<Direction> movPath = new ArrayList<>();
        MapLocation cur = destination;
        while(!cur.equals(start)){
            MapLocation pre = prePos.get(cur);
            movPath.add(pre.directionTo(cur));
            cur = pre;
        }
        Collections.reverse(movPath);
        for (Direction dir : movPath){
            if (rc.canMove(dir)){
                rc.move(dir);
            }
        }
        return dist[destination.x][destination.y];
    }


    static MapLocation[] getCircleRimLocs(MapLocation cent, int radSqr){
        int rad = (int)Math.sqrt(radSqr);
        MapLocation[] vecs = new MapLocation[rad + 1]; // each x value of radius corresbond to a y value
        for (int i = 0; i <= rad; i++){
            // x^2 + y^2 = r^2
            // so that y = sqrt(r^2 - x^2)
            vecs[i] = new MapLocation(i, (int)Math.sqrt(radSqr - i * i));
        }
        MapLocation[] ret = new MapLocation[vecs.length * 4];

        // center +x, +y | +x, -y | -x, +y | -x, -y from vec
        for (int i = 0; i < vecs.length; i++){
            ret[i] = new MapLocation(cent.x + vecs[i].x, cent.y + vecs[i].y);
            ret[i + vecs.length] = new MapLocation(cent.x + vecs[i].x, cent.y - vecs[i].y);
            ret[i + vecs.length * 2] = new MapLocation(cent.x - vecs[i].x, cent.y + vecs[i].y);
            ret[i + vecs.length * 3] = new MapLocation(cent.x - vecs[i].x, cent.y - vecs[i].y);
        }

        for (int i = 0; i < vecs.length; i++){
//            System.out.println("vec: " + vecs[i]);
        }

        return ret;
    }

    static MapLocation getClosestLocOnCircToTar(MapLocation cent, int radSqr, MapLocation tar){
        MapLocation[] rimLocs = getCircleRimLocs(cent, radSqr);
        int minDist = Integer.MAX_VALUE;
        MapLocation ret = null;
        for (MapLocation loc : rimLocs){
            int dist = diagnoDist(loc, tar);
            if (dist < minDist){
                minDist = dist;
                ret = loc;
            }
        }
        return ret;
    }


    /**
     * Move this robot toward a given position one step.
     *
     * @param rc          robot controller
     * @param destination destination
     */
    static void moveToward(RobotController rc, MapLocation destination) throws GameActionException {
        rc.setIndicatorString("moving toward " + destination);
        // TODO (avoid obstacles)
//        MapLocation current = rc.getLocation();
//        Direction direction = toDirection(destination.x - current.x, destination.y - current.y);
//        Direction dirL = direction.rotateLeft(), dirR = direction.rotateRight();
//        if(rc.canMove(direction)) {
//            rc.move(direction);
//        } else if(rc.canMove(dirL)) {
//            // if the bot cannot move directly toward the destination, try sideways
//            rc.move(dirL);
//        } else if(rc.canMove(dirR)) {
//            rc.move(dirR);
//        } else if(rc.canMove(dirL.rotateLeft())) {
//            rc.move(dirL.rotateLeft());
//        } else if(rc.canMove(dirR.rotateRight())) {
//            rc.move(dirR.rotateRight());
//        }
        Direction curDir = null; // if faced walls, move to this direction, null means not faced walls
        while(rc.getLocation() != destination){
            MapLocation closestOnRim = getClosestLocOnCircToTar(rc.getLocation(), getVisDis(), destination);
            int dist = moveTowardInVisRange(rc, closestOnRim, true);

            if (dist == -1){
                curDir = rc.getLocation().directionTo(closestOnRim);
                while(!rc.canMove(curDir)){
                    curDir = turningLeft ? curDir.rotateLeft() : curDir.rotateRight();
                }
                rc.setIndicatorString("cannot move to closest on rim, now moving to " + curDir);
                rc.move(curDir);
            } else {
                rc.setIndicatorString("moving to " + closestOnRim + " dist: " + dist);
            }
        }
    }

    static final int LOCATION_DEFAULT = 0x3FFF;

    // the first few robots the headquarters will build
    static final RobotType[] initialRobots = new RobotType[]{
            RobotType.AMPLIFIER, RobotType.CARRIER, RobotType.LAUNCHER
    };

    /*** local variables kept by each robot ***/
    static Set<Integer> locationsToWrite = new HashSet<>(); // Every important location that is scheduled to record into shared memory
    static MapLocation bindTo = null;                       // An important location (such as a well) that the robot is bound to
    static int state;                                       // Current state of robot. Its meaning depends on the type of robot
    static MapInfo[][] mapInfos = new MapInfo[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];    // What the robot knows about the map
    static int[][] passable = new int[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];    // What the robot knows about the map (passable)
    // 0 -> cannot pass, 1 can pass, -1 unknown
    static RobotType rType;
    static boolean turningLeft = Math.random() < 0.5;              // when facing a wall, should the robot turn left or right?
    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
        // initialize
        if ((rc.readSharedArray(63) & 0x8000) == 0) {
            rc.writeSharedArray(63, 0x8000);
            // initialize shared memory
            for (int i = 0; i < 48; i++) {
                rc.writeSharedArray(i, LOCATION_DEFAULT);
            }
            // initialize nearby well info
            scanForWells(rc);
        }
        // record the current headquarter's position into shared memory
        int currentLocation = locToInt(rc.getLocation());
        for (int i = 8; i < 11; i++) {
            int data = rc.readSharedArray(i);
            if (data == currentLocation) {
                // repeated information found in shared memory
                break;
            }
            if (rc.readSharedArray(i) == LOCATION_DEFAULT) {
                rc.writeSharedArray(i, currentLocation);
                break;
            }
        }
        // produce first few items as scheduled in array `initialRobots`
        if(state < initialRobots.length) {
            boolean robotBuilt = false;
            for(Direction dir : Direction.values()) {
                if(rc.canBuildRobot(initialRobots[state], rc.getLocation().add(dir))) {
                    rc.buildRobot(initialRobots[state], rc.getLocation().add(dir));
                    robotBuilt = true;
                    break;
                }
            }
            if(robotBuilt) {
                state++;
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
        // record information of observed wells and sky islands
        scanForWells(rc);
        scanForSkyIsland(rc);
        // update current state
        rc.setIndicatorString("current state: " + state);
        // perform an operation according to its state
        switch (state) {
            case 0:
                // if the robot is holding an anchor, go to state 2
                if(rc.getAnchor() != null) {
                    state = 2;
                    break;
                }
                // try to get an anchor
                if(bindTo != null) {
                    if(rc.canTakeAnchor(bindTo, Anchor.ACCELERATING)) {
                        rc.takeAnchor(bindTo, Anchor.ACCELERATING);
                        bindTo = null;
                        state = 2;
                        break;
                    }
                    if(rc.canTakeAnchor(bindTo, Anchor.STANDARD)) {
                        rc.takeAnchor(bindTo, Anchor.STANDARD);
                        bindTo = null;
                        state = 2;
                        break;
                    }
                }
                // if the bot can still carry stuff, try to find a well
                if(rc.getWeight() < 40) {
                    List<MapLocation> locations = new ArrayList<>();
                    for(int i = SHARED_MEMORY_WELLS; i < SHARED_MEMORY_HEADQUARTERS; i++) {
                        // find a valid well and set it for target
                        int pos = rc.readSharedArray(i);
                        if(pos != LOCATION_DEFAULT) {
                            locations.add(intToLoc(pos));
                        }
                    }
                    if(locations.size() != 0) {
                        // if the robot can find a well, target toward the well
                        bindTo = locations.get(Math.abs(rng.nextInt()) % locations.size());
                        state = 1;
                        break;
                    }
                }
                // if the bot does not find any job, wander randomly
                Direction dir = Direction.values()[(rng.nextInt() % 8 + 8) % 8];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
                break;

            case 1:
                rc.setIndicatorString("Targeting to " + bindTo.x + ", " + bindTo.y);
                MapLocation current = rc.getLocation();
                if (rc.canCollectResource(bindTo, -1)) {
                    rc.setIndicatorString("Collecting resource");
                    // if can collect resource, collect
                    rc.collectResource(bindTo, -1);
                    if (rc.getWeight() >= 40) {
                        bindTo = null;
                        state = 3;
                    }
                } else {
                    // otherwise, move toward the destination
                    moveToward(rc, bindTo);
                }
                break;

            case 2:
                if(bindTo == null) {
                    int minDist = Integer.MAX_VALUE;
                    for(int i = 0; i < 36; i++) {
                        int read = rc.readSharedArray(i + SHARED_MEMORY_SKY_ISLAND);
                        if(read != LOCATION_DEFAULT) {
                            MapLocation skyIsland = intToLoc(read);
                            int distance = diagnoDist(skyIsland, rc.getLocation());
                            if(distance < minDist) {
                                minDist = distance;
                                bindTo = skyIsland;
                            }
                        }
                    }
                }
                // if can place anchor, place it
                if(rc.canPlaceAnchor()) {
                    rc.placeAnchor();
                    bindTo = null;
                    state = 3;
                }
                // otherwise, walk toward the sky island
                if(bindTo != null) {
                    moveToward(rc, bindTo);
                } else {
                    state = 0;
                }
                break;

            case 3:
                if (bindTo == null) {
                    // find the headquarter with the smallest distance
                    int minDist = Integer.MAX_VALUE;
                    for (int i = SHARED_MEMORY_HEADQUARTERS; i < SHARED_MEMORY_SKY_ISLAND; i++) {
                        int read = rc.readSharedArray(i);
                        if (read != LOCATION_DEFAULT) {
                            MapLocation headquarter = intToLoc(read);
                            int distance = diagnoDist(headquarter, rc.getLocation());
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
                    moveToward(rc, bindTo);
                } else if (rc.getWeight() <= 0) {
                    state = 0;
                }
                break;
        }
        // clear up repeated information in locationsToWrite array
        for(int location : locationsToWrite) {
            for(int i = SHARED_MEMORY_WELLS; i < SHARED_MEMORY_HEADQUARTERS; i++) {
                int before = rc.readSharedArray(i);
                if(before == location) {
                    break;
                }
                if(before == LOCATION_DEFAULT && rc.canWriteSharedArray(i, location)) {
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
        scanForSkyIsland(rc);
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
        // clear up repeated information in locationsToWrite array
        for(int location : locationsToWrite) {
            for(int i = SHARED_MEMORY_WELLS; i < SHARED_MEMORY_HEADQUARTERS; i++) {
                int before = rc.readSharedArray(i);
                if(before == location) {
                    break;
                }
                if(before == LOCATION_DEFAULT && rc.canWriteSharedArray(i, location)) {
                    rc.writeSharedArray(i, location);
                    break;
                }
            }
        }
    }
}
