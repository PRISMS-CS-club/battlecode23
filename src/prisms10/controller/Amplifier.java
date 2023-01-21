package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.SharedMemory;
import prisms10.util.RandomNumber;

public class Amplifier extends Robot {

    public Amplifier(RobotController rc) {
        super(rc);
        robotType = RobotType.AMPLIFIER;
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        Direction dir = Direction.values()[RandomNumber.nextInt(Direction.values().length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
            state++;
        }
        SharedMemory.writeBackLocs(rc);
    }

}
