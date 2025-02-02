import uk.ac.warwick.dcs.maze.logic.IRobot;

public class Ex3
{
    /**
     * Check the location of the target vertically relative to the robot.
     * @param IRobot - robot object
     * @return int - 1 if north, -1 if south, 0 if neither
     */
    private byte isTargetNorth(IRobot robot) {
        int robot_y = robot.getLocation().y;
        int target_y = robot.getTargetLocation().y;

        byte result;

        if (target_y < robot_y) {
            result = 1;
        } else if (target_y > robot_y) {
            result = -1;
        } else {
            result = 0;
        }

        return result;
    }

    /**
     * Check the location of the target horizontally relative to the robot.
     * @param IRobot - robot object
     * @return int - 1 if east, -1 if west, 0 if neither
     */
    private byte isTargetEast(IRobot robot) {
        int robot_x = robot.getLocation().x;
        int target_x = robot.getTargetLocation().x;

        byte result;

        if (target_x > robot_x) {
            result = 1;
        } else if (target_x < robot_x) {
            result = -1;
        } else {
            result = 0;
        }

        return result;
    }

    /**
     * Get the type of square in the direction of a heading.
     */
    private int lookHeading(IRobot robot, int heading) {
        int init_heading = robot.getHeading();
        robot.setHeading(heading);
        int output = robot.look(IRobot.AHEAD);
        robot.setHeading(init_heading);
        
        return output;
    }

    /**
     * Calculate a random heading that will not lead into a wall, prioritising those which makes
     * robot move closer to the target.
     * @param IRobot - robot object
     * @return int - heading
     */
    private int headingController(IRobot robot) {
        // Array storing the headings that are able to be moved to.
        // true = square is empty, false = square is a wall.
        // follows order NORTH, EAST, SOUTH, WEST
        boolean[] able_headings = {
            lookHeading(robot, IRobot.NORTH) != IRobot.WALL,
            lookHeading(robot, IRobot.EAST) != IRobot.WALL,
            lookHeading(robot, IRobot.SOUTH) != IRobot.WALL,
            lookHeading(robot, IRobot.WEST) != IRobot.WALL
        };

        // Array storing the headings corresponding to the heading of the target.
        // true = target is towards heading, false = target is not towards heading.
        // follows order NORTH, EAST, SOUTH, WEST
        boolean[] priority_headings = {
            isTargetNorth(robot) == 1,
            isTargetEast(robot) == 1,
            isTargetNorth(robot) == -1,
            isTargetEast(robot) == -1
        };

        // Initialise array storing the headings to choose from.
        // true = able to chosen from, false = ignored.
        // follows order NORTH, EAST, SOUTH, WEST
        boolean[] choices = {false, false, false, false};

        boolean is_choices_empty = true;  // Used to check if there at least one choice in array.

        // Find headings which doesn't lead to a wall and also points towards the target.
        // Store in choices array.
        for (int i = 0; i < 4; i++) {

            // if at index it is true for both, set the corresponding choice to be true.
            if (able_headings[i] && priority_headings[i]) {
                choices[i] = true;
                
                // There is at least one choice.
                is_choices_empty = false;
            }
        }

        // If no headings satisfy conditions, the choices are those that does not lead a wall.
        if (is_choices_empty == true) {
            choices = able_headings;
        }


        // We now pick a heading using the boolean array "choices".
        // First pick a random integer from 0 to n_choices - 1.
        // We then need to find the corresponding index of the array to pick the heading.


        // Find the number of possible choices.
        int n_choices = 0;
        for (int i = 0; i < 4; i++) {
            if (choices[i]) {
                n_choices++;
            }
        }

        // Find a random number 0 to n_choices - 1.
        int rand_n = (int) (Math.random() * n_choices);

        int current_n = 0;  // Keep track of the current nth true element as the array is iterated through.
        int choice_idx = 0;  // Stores the index for the choice chosen.

        // Find the index of the nth true element in choices array.
        for (int i = 0; i < 4; i++) {
            if (choices[i]) {

                // If on the nth element, return the corresponding heading.
                if (current_n == rand_n) {
                    choice_idx = i;
                }

                // Else, increment current_n and repeat.
                current_n++;
            }
        }
        
        // Return heading based on index of choice.
        switch (choice_idx) {
            case 0: return IRobot.NORTH;
            case 1: return IRobot.EAST;
            case 2: return IRobot.SOUTH;
            case 3: return IRobot.WEST;
            default: return -1;  // Code should not be reached.
        }
    }

    /**
     * Calls method to print the results of control test
     */
    public void reset() {
        ControlTest.printResults();
    }        

    /**
     * Execute instructions each step to control the robot.
     */
    public void controlRobot(IRobot robot) {
        // Get heading from headingController
        int heading = headingController(robot);

        // Test against control
        ControlTest.test(heading, robot);

        // Face heading
        robot.setHeading(heading);
    }
}
 