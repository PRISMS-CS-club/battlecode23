package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.MemoryCache;

public class Amplifier extends Robot {

    public Amplifier(RobotController rc) {
        super(rc);
        robotType = RobotType.AMPLIFIER;
    }


    @Override
    public void run() throws GameActionException {
        super.run();
        randomMove();
        MemoryCache.writeBackLocs(rc);
    }

}
