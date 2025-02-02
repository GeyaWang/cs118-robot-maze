import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;


class JunctionRecorder {
    public int x;
    public int y;
    public int arrivalHeading;
    public int exitHeading;
    public boolean isVisited = false;

    public JunctionRecorder(int x, int y, int arrivalHeading) {
        this.x = x;
        this.y = y;
        this.arrivalHeading = arrivalHeading;
    }
}


class RobotData {
    public ArrayList<JunctionRecorder> passedJunctions = new ArrayList<JunctionRecorder>();

    /**
     * Find junction using coordinates.
     * @param x
     * @param y
     * @return JunctionRecorder if found, else null
     */
    public JunctionRecorder searchJunction(int x, int y) {
        for (JunctionRecorder j : passedJunctions) {
            if (j.x == x && j.y == y) {
                return j;
            }
        }
        return null;
    }

    /**
     * Records junction using coordinates and arrival heading if not already recorded.
     * @param x
     * @param y
     * @param heading
     * @return recorded JunctionRecorder obj
     */
    public JunctionRecorder recordJunction(int x, int y, int heading) {
        // Test if junction has already been recorded
        JunctionRecorder junction = searchJunction(x, y);
        if (junction != null) {
            return junction;
        }

        // Create and record new junction
        JunctionRecorder newJunction = new JunctionRecorder(x, y, heading);
        passedJunctions.add(newJunction);
        return newJunction;
    }

    /**
     * Sets the exit heading of a junction using coordinates.
     * @param x
     * @param y
     * @param heading
     */
    public void setExitHeading(int x, int y, int heading) {
        JunctionRecorder junction = searchJunction(x, y);
        junction.exitHeading = heading;
    }

    /**
     * Sets the exit heading of a junction.
     * @param junction
     * @param heading
     */
    public void setExitHeading(JunctionRecorder junction, int heading) {
        junction.exitHeading = heading;
    }

    /**
     * Find arrival heading of a junction using coordinates.
     * @param x
     * @param y
     * @return int arrivalHeading
     */
    public int getArrivalHeading(int x, int y) {
        JunctionRecorder junction = searchJunction(x, y);

        if (junction == null) {
            return -1;
        }

        return junction.arrivalHeading;
    }

    /**
     * Find exit heading of a junction using coordinates.
     * @param x
     * @param y
     * @return
     */
    public int getExitHeading(int x, int y) {
        JunctionRecorder junction = searchJunction(x, y);

        if (junction == null) {
            return -1;
        }

        return junction.exitHeading;
    }
}


enum RobotMode {
    EXPLORE,
    BACKTRACK,
    ROUTE
}


public class GrandFinale {
    private final static int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
    private RobotData robotData;
    private int stepCounter = 0;
    private RobotMode mode = RobotMode.EXPLORE;
    private int startX;
    private int startY;
    
    /**
     * Called when run is reset.
     */
    public void reset() {
        stepCounter = 0;
    }

    /**
     * Handles event where a new maze is generated.
     */
    private void newMaze() {
        robotData = new RobotData();
        mode = RobotMode.EXPLORE;
        reset();
    }
    
    /**
     * Called each step.
     * @param robot
     */
    public void controlRobot(IRobot robot) {

        // If it is a new maze
        if (stepCounter == 0) {
            // Set start coordinates
            startX = robot.getLocation().x;
            startY = robot.getLocation().y;

            if (robot.getRuns() == 0) {
                newMaze();
            } else {  // robot.getRuns() > 0
                // Switch to route mode if not first run
                mode = RobotMode.ROUTE;
            }
        }

        // Explore, backtrack or follow route depending on the mode
        if (mode == RobotMode.EXPLORE) {
            explorerControl(robot);
        } else if (mode == RobotMode.BACKTRACK) {
            backtrackControl(robot);
        } else {  // mode == RobotMode.ROUTE
            routeControl(robot);
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
                mode = RobotMode.BACKTRACK;
            }

            deadEndControl(robot);

        } else if (exits > 2) {  // Junction or crossroads
            // Record junction data
            JunctionRecorder junction = robotData.recordJunction(robot.getLocation().x, robot.getLocation().y, robot.getHeading());

            exploreJunctionControl(robot, junction);

            robotData.setExitHeading(junction, robot.getHeading());

        } else if (exits == 2) {  // Corridor

            // If first step, create junction at start point
            if (stepCounter == 0) {
                JunctionRecorder junction = robotData.recordJunction(robot.getLocation().x, robot.getLocation().y, -1);
                corridorControl(robot);
                robotData.setExitHeading(junction, robot.getHeading());
                return;
            }

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
            robotData.setExitHeading(robot.getLocation().x, robot.getLocation().y, robot.getHeading());

        } else if (exits == 2) {  // Corridor
            corridorControl(robot);

            // If backtracked past start, set exit heading for start position
            if (startX == robot.getLocation().x && startY == robot.getLocation().y) {
                robotData.setExitHeading(startX, startY, robot.getHeading());
            }
        }

        // If there is passage ahead, go into explore mode
        if (robot.look(IRobot.AHEAD) == IRobot.PASSAGE) {
            mode = RobotMode.EXPLORE;
        }
    }

    private void routeControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits == 0 || exits == 1) {  // Deadend
            deadEndControl(robot);
        } else if (exits > 2) {  // Junction or crossroads
            routeJunctionControl(robot);
        } else if (exits == 2) {  // Corridor

            // If at start pos, follow exit heading
            if (startX == robot.getLocation().x && startY == robot.getLocation().y) {
                int heading = robotData.getExitHeading(robot.getLocation().x, robot.getLocation().y);
                robot.setHeading(heading);
            }

            corridorControl(robot);
        }
    }

    /**
     * Controls the robot for when it meets a junction or crossroad.
     * If it has been visited before, turn around and go into backtrack mode. Else, pick between passage exits.
     * @param robot
     */
    private void exploreJunctionControl(IRobot robot, JunctionRecorder junction) {
        int passageExits = passageExits(robot);
        int nonwallExits = nonwallExits(robot);

        // Junction visited before
        if (junction.isVisited) {

            // Turn around
            robot.face(IRobot.BEHIND);

            // Go into backtrack mode
            mode = RobotMode.BACKTRACK;

        } else {  // Not visited before
            junction.isVisited = true;

            // Get arraylist of all directions of passage exits
            ArrayList<Integer> passageExitsArr = new ArrayList<Integer>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageExitsArr.add(d);
                }
            }

            // If no PASSAGE exits, pick randomly between non-WALL exits
            if (passageExitsArr.size() == 0) {
                // Pick random non wall direction
                ArrayList<Integer> exitsArr = new ArrayList<Integer>();
                for (int d : directions) {
                    if (robot.look(d) != IRobot.WALL) {
                        exitsArr.add(d);
                    }
                }
                robot.face(exitsArr.get((int) (Math.random() * exitsArr.size())));
                return;
            }

            // Choose random direction
            int randIndex = (int) (Math.random() * passageExitsArr.size());

            // Face chosen direction
            robot.face(passageExitsArr.get(randIndex));
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
            int arrivalHeading = robotData.getArrivalHeading(robot.getLocation().x, robot.getLocation().y);
            int oppositeHeading = ((arrivalHeading + 2) % 4) + IRobot.NORTH;
            robot.setHeading(oppositeHeading);

        } else {  // passageExits > 0

            // Switch to explorer mode
            mode = RobotMode.EXPLORE;

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

    private void routeJunctionControl(IRobot robot) {
        int heading = robotData.getExitHeading(robot.getLocation().x, robot.getLocation().y);

        // Junction not found or exit heading leads to opposite direction. Run was reset early
        if (heading == -1 || (robot.getHeading() - heading + 4) % 4 == 2) {

            // Go into explore mode
            mode = RobotMode.EXPLORE;

            // In case junction not found, record new junction
            JunctionRecorder junction = robotData.recordJunction(robot.getLocation().x, robot.getLocation().y, robot.getHeading());

            junctionControl(robot);
            robotData.setExitHeading(junction, robot.getHeading());
            return;
        }

        robot.setHeading(heading);
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
