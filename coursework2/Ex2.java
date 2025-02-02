import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;
import java.util.ArrayDeque;


class RobotData {
    private static int maxJunctions = 10000;
    private int junctionCounter = 0;
    private int[] arrivalHeadings = new int[maxJunctions];

    /**
     * Records junction using arrival heading.
     * @param heading
     */
    public void recordJunction(int heading) {
        arrivalHeadings[junctionCounter] = heading;
        junctionCounter++;
    }

    /**
     * Set last recorded junction to -1.
     */
    public void removeJunction() {
        junctionCounter--;

        // Erases arrival heading by setting it to -1
        arrivalHeadings[junctionCounter] = -1;
    }

    /**
     * Find arrival heading of last recorded junction.
     * @return int arrivalHeading
     */
    public int getArrivalHeading() {
        return arrivalHeadings[junctionCounter - 1];
    }
}


public class Ex2 {
    private final static int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
    private RobotData robotData;
    private int stepCounter = 0;
    private int explorerMode = 1;  // 1: explore, 0: backtrack
    
    /**
     * Called when run is reset.
     */
    public void reset() {
        robotData = new RobotData();
        explorerMode = 1;
        stepCounter = 0;
    }
    
    /**
     * Called each step.
     * @param robot
     */
    public void controlRobot(IRobot robot) {

        // If it is a new maze
        if (stepCounter == 0 && robot.getRuns() == 0) {
            reset();
        }

        // Either explore of backtrack depending on the mode
        if (explorerMode == 1) {
            explorerControl(robot);
        } else {  // explorerMode == 0
            backtrackControl(robot);
        }

        // Increment stepCounter
        stepCounter++;
    }

    /**
     * Controller for when the robot is exploring.
     * @param robot
     */
    private void explorerControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits == 0 || exits == 1) {  // Deadend

            // Set mode to backtrack if it is not the first step
            if (stepCounter != 0) {
                explorerMode = 0;
            }

            deadEndControl(robot);

        } else if (exits > 2) {  // Junction or crossroads
            robotData.recordJunction(robot.getHeading());
            junctionControl(robot);

        } else if (exits == 2) {  // Corridor
            corridorControl(robot);
        }
    }

    /**
     * Controller for when the robot is backtracking.
     * @param robot
     */
    private void backtrackControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits == 0 || exits == 1) {  // Deadend
            deadEndControl(robot);
        } else if (exits > 2) {  // Junction or crossroads
            backtrackJunctionControl(robot);
        } else if (exits == 2) {  // Corridor
            corridorControl(robot);
        }
    }

    /**
     * Controls the robot for when it meets a junction while backtracking.
     * If there are no passage exits, backtrack by moving in the opposite direction to the arrival heading.
     * Else, go into explorer mode and pick a passage exit randomly.
     * @param robot
     */
    private void backtrackJunctionControl(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits == 0) {

            // Go opposite direction of initial arrival heading.
            int arrivalHeading = robotData.getArrivalHeading();
            int oppositeHeading = ((arrivalHeading + 2) % 4) + IRobot.NORTH;
            robot.setHeading(oppositeHeading);

            // Remove junction
            robotData.removeJunction();

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
    private void deadEndControl(IRobot robot) {
        if (stepCounter != 0) {

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
     * @param robot
     */
    private void corridorControl(IRobot robot) {

        // Iterates through all directions that are not behind the robot until it finds the only one it can move to.
        for (int d : directions) {
            if (d != IRobot.BEHIND && robot.look(d) != IRobot.WALL) {

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
    private void junctionControl(IRobot robot) {
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
     * @return int number of non-WALL exits
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
     * @return int number of PASSAGE exits
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
