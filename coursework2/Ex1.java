import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;
import java.util.function.ObjDoubleConsumer;


class JunctionRecorder {
    public int x;
    public int y;
    public int arrivalHeading;

    public JunctionRecorder(int x, int y, int arrivalHeading) {
        this.x = x;
        this.y = y;
        this.arrivalHeading = arrivalHeading;
    }
}


class RobotData {
    private static int maxJunctions = 20000;
    private int junctionCounter = 0;
    private JunctionRecorder[] junctions = new JunctionRecorder[maxJunctions];

    /**
     * Prints out the junction information.
     * @param x
     * @param y
     * @param heading
     */
    private void printJunction(int x, int y, int heading) {
        String headingStr = "";
        switch (heading) {
            case IRobot.NORTH: headingStr = "NORTH"; break;
            case IRobot.EAST: headingStr = "EAST"; break;
            case IRobot.SOUTH: headingStr = "SOUTH"; break;
            case IRobot.WEST: headingStr = "WEST"; break;
        }

        System.out.println("Junction " + junctionCounter + " (x=" + x + ",y=" + y + ") heading " + headingStr);
    }

    /**
     * Store junction data if it has not been recorded already.
     * @param x
     * @param y
     * @param heading
     */
    public void recordJunction(int x, int y, int heading) {
        // Iterate through all recorded junctions to check if it has been recorded already
        for (int i = 0; i < junctionCounter; i++) {
            JunctionRecorder junction = junctions[i];

            if (junction.x == x && junction.y == y) {

                // Junction already recorded, return out
                return;
            }
        }

        // Record junction
        JunctionRecorder junction = new JunctionRecorder(x, y, heading);

        junctions[junctionCounter] = junction;

        // Print junction information
        printJunction(x, y, heading);

        // Increment junction counter
        junctionCounter++;
    }

    /**
     * Find the arrival heading at a junction.
     * @param x
     * @param y
     * @return arrivalHeading if junction is stored. -1 if junction is not stored
     */
    public int searchJunction(int x, int y) {

        // Iterate through all recorded junctions
        for (int i = 0; i < junctionCounter; i++) {
            JunctionRecorder junction = junctions[i];

            if (junction.x == x && junction.y == y) {
                return junction.arrivalHeading;
            }
        }
        return -1;
    }
}


public class Ex1 {
    private final static int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
    private RobotData robotData = new RobotData();
    private int stepNumber = 0;
    private int explorerMode = 1;  // 1 = explore, 0 = backtrack
    
    /**
     * Called when run is reset.
     */
    public void reset() {
        robotData = new RobotData();
        explorerMode = 1;
        stepNumber = 0;
    }
    
    /**
     * Called each step of the robot.
     * @param robot
     */
    public void controlRobot(IRobot robot) {

        // Either explore of backtrack depending on the mode
        if (explorerMode == 1) {
            explorerControl(robot);
        } else {  // explorerMode == 0
            backtrackControl(robot);
        }

        // Increment stepNumber
        stepNumber++;
    }

    /**
     * Controller for when the robot is exploring.
     * @param robot
     */
    private void explorerControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits == 0 || exits == 1) {  // Deadend

            // Set mode to backtrack if it is not the first step
            if (stepNumber != 0) {
                explorerMode = 0;
            }

            deadEnd(robot);
        } else if (exits == 2) {  // Corridor
            corridor(robot);
        } else {  // exits > 2, junction or crossroads
            robotData.recordJunction(robot.getLocation().x, robot.getLocation().y, robot.getHeading());
            junction(robot);
        }
    }

    /**
     * Controller for when the robot is backtracking.
     * @param robot
     */
    private void backtrackControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits == 0 || exits == 1) {  // Deadend
            deadEnd(robot);
        } else if (exits == 2) {  // Corridor
            corridor(robot);
        } else {  // exits > 2, junction or crossroads
            backtrackJunction(robot);
        }
    }

    /**
     * Controls the robot for when it meets a junction while backtracking.
     * If there are passage exits, go into explorer mode and pick a passage exit randomly.
     * Else, find the original heading when first arriving at the junction and move in the opposite direction.
     * @param robot
     */
    private void backtrackJunction(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits == 0) {
            int arrivalHeading = robotData.searchJunction(robot.getLocation().x, robot.getLocation().y);
            
            // Face opposite to arrival heading
            int oppositeHeading = ((arrivalHeading + 2) % 4) + IRobot.NORTH;
            robot.setHeading(oppositeHeading);

        } else {  // passageExits > 0

            // Switch to explorer mode
            explorerMode = 1;

            // Get arraylist of passage exits
            ArrayList<Integer> passageExitsArr = new ArrayList<Integer>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageExitsArr.add(d);
                }
            }

            // Face a random direction
            int randIndex = (int) (Math.random() * passageExitsArr.size());
            robot.face(passageExitsArr.get(randIndex));
        }
    }

    /**
     * Controls the robot for when it meets a dead end.
     * If first step, find the direction it can move to.
     * Else, go back from the direction it came from
     * @param robot
     */
    private void deadEnd(IRobot robot) {
        if (stepNumber != 0) {
            // Go back
            robot.face(IRobot.BEHIND);

        } else {
            // Iterate through all directions until it finds the only one it can move to.
            for (int d : directions) {
                if (robot.look(d) != IRobot.WALL) {
                    // Face direction
                    robot.face(d);
                    return;
                }
            }
        }
    }

    /**
     * Controls the robot for when it meets a corridor.
     * Iterates through all directions that are not behind the robot until it finds the only one it can move to.
     * @param robot
     */
    private void corridor(IRobot robot) {
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL && d != IRobot.BEHIND) {
                // Face direction
                robot.face(d);
                return;
            }
        }
    }

    /**
     * Controls the robot for when it meets a junction or crossroad.
     * If there are PASSAGE exits, randomly choose between them. Else, randomly choose between all non-WALL exits.
     * @param robot
     */
    private void junction(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits == 0) {
            // Get arraylist of all directions of non-wall exits
            ArrayList<Integer> allExitsArr = new ArrayList<Integer>();
            for (int d : directions) {
                if (robot.look(d) != IRobot.WALL) {
                    allExitsArr.add(d);
                }
            }
            
            // Face random direction
            int randIndex = (int) (Math.random() * allExitsArr.size());
            robot.face(allExitsArr.get(randIndex));

        } else {  // passageExits > 0
            // Get arraylist of all directions of passage exits
            ArrayList<Integer> passageExitsArr = new ArrayList<Integer>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageExitsArr.add(d);
                }
            }

            // Face random direction
            int randIndex = (int) (Math.random() * passageExitsArr.size());
            robot.face(passageExitsArr.get(randIndex));
        }
    }

    /**
     * Finds the number of non-WALL exits in the 4 cardinal directions around the robot.
     * @param robot
     * @return integer of the number of non-WALL exits
     */
    private int nonwallExits(IRobot robot) {
        int exits = 0;
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL) {
                exits++;
            }
        }
        return exits;
    }

    /**
     * Returns the number of PASSAGE exits in the 4 cardinal directions around the robot.
     * @param robot
     * @return integer of the number of PASSAGE exits
     */
    private int passageExits(IRobot robot) {
        int exits = 0;
        for (int d : directions) {
            if (robot.look(d) == IRobot.PASSAGE) {
                exits++;
            }
        }
        return exits;
    }
}
