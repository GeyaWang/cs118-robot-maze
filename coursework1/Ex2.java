import uk.ac.warwick.dcs.maze.logic.IRobot;


public class Ex2
{
    /**
     * Method called each step to control the movement of the robot.
     * @param robot - robot object
     */
    public void controlRobot(IRobot robot) {
        // Use algorithm to randomly generate a direction to move.
        int direction = directionController(robot);

        // Face the direction chosen.
        robot.face(direction);

        // Log the movements.
        printMovement(robot, direction);
	}

    /**
     * Method to log the movements of the robot.
     * Finds the type of environment and direction moved as strings and prints out to terminal.
     * @param robot - robot object
     * @param direction - integer representing the relative direction chosen
     */
    private void printMovement(IRobot robot, int direction) {
        // Find the number of walls touching the robot by checking each direction.
        int n_walls = 0;
        if (robot.look(IRobot.LEFT) == IRobot.WALL) {n_walls++;}
        if (robot.look(IRobot.RIGHT) == IRobot.WALL) {n_walls++;}
        if (robot.look(IRobot.BEHIND) == IRobot.WALL) {n_walls++;}
        if (robot.look(IRobot.AHEAD) == IRobot.WALL) {n_walls++;}
        
        // Get the type of environment.
        String str_env_type = "";  // Stores the type of environment as a string.
        switch (n_walls) {
            case 0:
                str_env_type = "crossroads";
                break;
            case 1:
                str_env_type = "junction";
                break;
            case 2:
                str_env_type = "corridor";
                break;
            case 3:
                str_env_type = "deadend";
                break;
        }

        // Get the direction moved as a string.
        String str_direction = "";  // Stores the direction as a string.
        switch (direction) {
            case IRobot.LEFT:
                str_direction = "left";
                break;
            case IRobot.RIGHT:
                str_direction = "right";
                break;
            case IRobot.BEHIND:
                str_direction = "backwards";
                break;
            case IRobot.AHEAD:
                str_direction = "forwards";
                break;
        }
        
        // Print out movements to console.
        System.out.println("I'm going " + str_direction + " at a " + str_env_type);
    }

    /**
     * Method to generate the direction for the robot to move
     * by randomly picking a direction until it does not lead into a wall.
     * @param robot - robot object
     * @return int representing the relative direction chosen to travel
     */
    private int directionController(IRobot robot) {
        if (
                (robot.look(IRobot.AHEAD) != IRobot.WALL) &&  // If no wall ahead, choose direction as "AHEAD"
                Math.random() > 0.125  // 1/8 = 0.125 chance of bypassing this
            ) {
                return IRobot.AHEAD;
        }

        // Else, pick a random direction that does not lead into a wall

        int direction;
        int rand_no;

        do {
            // Select a random number.
            // Cast the double into an integer, rounding down.
            rand_no = (int) (Math.random() * 4);
            
            // Convert it to a direction.
            if (rand_no == 0) {
                direction = IRobot.LEFT;
            } else if (rand_no == 1) {
                direction = IRobot.RIGHT;
            } else if (rand_no == 2) {
                direction = IRobot.BEHIND;
            } else {
                direction = IRobot.AHEAD;
            }

        // If the direction leads into a wall, repeat and select another random direction.
        } while (robot.look(direction) == IRobot.WALL);

        return direction;
    }
}
