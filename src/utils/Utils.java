package utils;

import objects.Avatar;
import objects.Bomb;
import objects.GameObject;

import java.util.*;

import static utils.Types.FLAME_LIFE;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Utils
{
    // Takes an object from an array at random
    public static Object choice(Object[] elements, Random rnd)
    {
        return elements[rnd.nextInt(elements.length)];
    }

    // Takes an integer from an array at random
    public static int choice(int[] elements, Random rnd)
    {
        return elements[rnd.nextInt(elements.length)];
    }

    // Clamps 'val' between 'mim' and 'max'
    public static int clamp(int min, int val, int max)
    {
        if(val < min) return min;
        if(val > max) return max;
        return val;
    }

    //Choices a direction at random
    public static Types.DIRECTIONS choiceDir(ArrayList<Types.DIRECTIONS> elements, Random rnd)
    {
        return elements.get(rnd.nextInt(elements.size()));
    }

    //Choices a Vector2d from a list at random
    public static Vector2d choice(ArrayList<Vector2d> elements, Random rnd)
    {
        return elements.get(rnd.nextInt(elements.size()));
    }

    // Ugh, a regex!
    public static String formatString(String str)
    {
        // 1st replaceAll: compresses all non-newline whitespaces to single space
        // 2nd replaceAll: removes spaces from beginning or end of lines
        return str.replaceAll("[\\s&&[^\\n]]+", " ").replaceAll("(?m)^\\s|\\s$", "");
    }

    //Normalizes a value between its MIN and MAX.
    public static double normalise(double a_value, double a_min, double a_max)
    {
        if(a_min < a_max)
            return (a_value - a_min)/(a_max - a_min);
        else    // if bounds are invalid, then return same value
            return a_value;
    }

    /**
     * Adds a small noise to the input value.
     * @param input value to be altered
     * @param epsilon relative amount the input will be altered
     * @param random random variable in range [0,1]
     * @return epsilon-random-altered input value
     */
    public static double noise(double input, double epsilon, double random)
    {
        if(input != -epsilon) {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }else {
            //System.out.format("utils.tiebreaker(): WARNING: value equal to epsilon: %f\n",input);
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }
    }

    // Returns the index of the element in the array with the highest value.
    public static int argmax (double[] values)
    {
        int maxIndex = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            double elem = values[i];
            if (elem > max) {
                max = elem;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    //Concatenates the elements of a String array into a String object, separated by ','
    public static String toStringArray(String[] array)
    {
        if (array != null && array.length > 0) {
            StringBuilder nameBuilder = new StringBuilder();

            for (String elem : array)
                nameBuilder.append(elem).append(",");

            nameBuilder.deleteCharAt(nameBuilder.length() - 1);

            return nameBuilder.toString();
        } else {
            return "";
        }
    }

    //Finds the maximum divisor of a value
    public static int findMaxDivisor(int value) {
        int divisor = 1;
        for (int i=1; i<=Math.sqrt(value)+1; i++) {
            if (value % i == 0) {
                divisor = i;
            }
        }
        return divisor;
    }

    //Sums all ints in an array and returns the sum
    public static int sumArray(int[] ar)
    {
        int sum = 0;
        for(int val : ar)
        {
            sum+=val;
        }
        return sum;
    }


    /**
     * Checks if a given game object can take a new position on the given board. If it's a legal position,
     * sets the desired coordinate of the given object to the new position, otherwise to its old position.
     * @param gameObject - game object to check
     * @param pos - new position
     * @param board - board state to check legal positions on
     * @return true if new position set successfully, false otherwise
     */
    public static boolean setDesiredCoordinate(GameObject gameObject, Vector2d pos, Types.TILETYPE[][] board) {
        ArrayList<Types.TILETYPE> defaultCollisions = new ArrayList<>();
        defaultCollisions.add(Types.TILETYPE.RIGID);
        defaultCollisions.add(Types.TILETYPE.WOOD);
        return setDesiredCoordinate(gameObject, pos, board, defaultCollisions);
    }

    /**
     * Optionally specify collisions.
     * @param gameObject - game object to check
     * @param pos - new position
     * @param board - board state to check legal positions on
     * @param collisions - list of types which would make the position of a sprite illegal
     * @return true if new position set successfully, false otherwise
     */
    public static boolean setDesiredCoordinate(GameObject gameObject, Vector2d pos, Types.TILETYPE[][] board,
                                               ArrayList<Types.TILETYPE> collisions) {
        if (_checkLegalPosition(board, pos, collisions)) {
            gameObject.setDesiredCoordinate(pos);
            return true;
        } else {
            // Revert to current position if new position is illegal
            if (gameObject.getPosition() != null) {
                gameObject.setDesiredCoordinate(gameObject.getPosition());
            }
            return false;
        }
    }

    /**
     * Checks if a given position is legal on the current board. This is defined as both x and y are within the limits
     * of the given board, and the position indicated is not a wall type.
     * @param board - given board to check position on
     * @param pos - given position
     * @return true if position is legal, false otherwise
     */
    private static boolean _checkLegalPosition(Types.TILETYPE[][] board, Vector2d pos,
                                               ArrayList<Types.TILETYPE> collisions) {
        return pos != null && pos.x >= 0 && pos.y >= 0 && pos.y < board.length && pos.x < board[0].length &&
                (board[pos.y][pos.x] == null || !collisions.contains(board[pos.y][pos.x]));
    }

    /**
     * Checks if the object of given type, at given position in given board can be overwritten.
     * Uses default list of types that should not be removed from the board, powerups.
     * @param pos - position to check in the board
     * @param board - board to check position in
     * @return true if object can overwrite, false otherwise
     */
    public static boolean canOverwrite(Vector2d pos, Types.TILETYPE[][] board,
                                       HashSet<Types.TILETYPE> illegalOverwriteTypes) {
        return !illegalOverwriteTypes.contains(board[pos.y][pos.x]);
    }

    /**
     * Iterates through 2 lists of game objects and checks their desired position and current position.
     * If the 2 objects swap positions, revert both desired position to their original position.
     * @param golist1 - first list of game objects
     * @param golist2 - second list of game objects
     * @param revertOnlySecond - if true, revert only the positions of the elements in the second list.
     * @param board - board to update positions on
     */
    public static void checkPositionSwap(ArrayList<GameObject> golist1, ArrayList<GameObject> golist2,
                                         Types.TILETYPE[][] board, boolean revertOnlySecond, boolean verbose) {
        for (GameObject g1: golist1) {
            for (GameObject g2: golist2) {
                if (!g1.equals(g2)) {
                    if (g1.getDesiredCoordinate() != null && g1.getPosition() != null &&
                            g2.getDesiredCoordinate() != null && g2.getPosition() != null &&
                            !g1.getDesiredCoordinate().equals(g1.getPosition()) &&
                            !g2.getDesiredCoordinate().equals(g2.getPosition())) {
                        // The objects need to both have moved to count for a swap check.
                        if (g1.getDesiredCoordinate().equals(g2.getPosition()) &&
                                g2.getDesiredCoordinate().equals(g1.getPosition())) {
                            if (!revertOnlySecond) {
                                if (verbose) {
                                    System.out.println("Reverting " + g1.getType() + " swap with " + g2.getType());
                                }
                                setDesiredCoordinate(g1, g1.getPosition(), board);
                            }
                            if (verbose) {
                                System.out.println("Reverting " + g2.getType() + " swap with " + g1.getType());
                            }
                            setDesiredCoordinate(g2, g2.getPosition(), board);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if more than 1 object wants to move to the same position. Bounce all back.
     * @param golist - list of game objects to check.
     */
    public static void checkPositionOverlap(ArrayList<GameObject> golist, Types.TILETYPE[][] board, boolean verbose) {
        // Count how many objects are in the same position.
        HashMap<Vector2d, Integer> countList = checkOccupancy(golist);

        // If more than 1 object are at a position, revert all to previous position.
        for (GameObject g: golist) {
            if (countList.get(g.getDesiredCoordinate()) > 1) {
                if (verbose) {
                    System.out.println("Reverting " + g.getType() + " overlap");
                }
                setDesiredCoordinate(g, g.getPosition(), board);
            }
        }
    }

    /**
     * Checks how many of the objects in the given list occupy the same position.
     * @param golist - given list of game objects
     * @return mapping position -> number of game objects at that position
     */
    public static HashMap<Vector2d, Integer> checkOccupancy(ArrayList<GameObject> golist) {
        HashMap<Vector2d, Integer> countList = new HashMap<>();
        for (GameObject g: golist) {
            countList.merge(g.getDesiredCoordinate(), 1, Integer::sum);
        }
        return countList;
    }

    /**
     * Creates a deep copy of an ArrayList of game objects
     * @param arr - given list
     * @return a new list containing copies of objects in the original list
     */
    public static GameObject[] deepCopy(GameObject[] arr) {
        GameObject[] newArr = new GameObject[arr.length];
        for (int i = 0; i < arr.length; i++) {
            newArr[i] = arr[i].copy();
        }
        return newArr;
    }

    /**
     * Adds copies of game objects to a new list, given original list, where items are within a certain range from the
     * given position.
     * @param originalList - original list of game objects to check.
     * @param copyList - list which will contain the new objects.
     * @param refPosition - position reference for range check.
     * @param range - range within which the objects should be included. May be -1, which means all objects
     *              should be included
     */
    public static void _reduceHiddenList(ArrayList<GameObject> originalList, ArrayList<GameObject> copyList,
                                         Vector2d refPosition, int range) {
        for (GameObject g: originalList) {

            Vector2d posG = null;
            if (range >= 0) {
                // Get object's position.
                posG = g.getPosition();
            }

            // Check if the object is in range. If it is, add a copy of the object to the copy list.
            if (range == -1 || posG != null && refPosition != null && refPosition.custom_dist(posG) <= range) {
                GameObject ob = g.copy();
                if (ob.getType() == Types.TILETYPE.FLAMES) ob.setLife(FLAME_LIFE);
                else if (ob.getType() == Types.TILETYPE.BOMB) {
                    Bomb b = (Bomb)ob;
                    b.setVelocity(new Vector2d());
                    b.setPlayerIdx(-1);
                }
                copyList.add(ob);
            }
        }
    }

    /**
     * Finds all agents which are still alive (win status INCOMPLETE), given list of all agents.
     * @param allAgents - list of all agents, including those that died already.
     * @return - a list of agents left alive in the game.
     */
    public static ArrayList<GameObject> findAliveAgents(GameObject[] allAgents) {
        ArrayList<GameObject> alive = new ArrayList<>();
        for (GameObject go: allAgents) {
            if (((Avatar)go).getWinner() == Types.RESULT.INCOMPLETE) {
                alive.add(go);
            }
        }
        return alive;
    }

    /**
     * Determines the direction of an adjacent position (nextPosition) in reference to a position (position).
     * @param position - given list of game objects
     * @param nextPosition - given list of game objects
     * @return determined direction.
     */
    public static Types.DIRECTIONS getDirection(Vector2d position, Vector2d nextPosition) {

        if(position.x == nextPosition.x){
            if(position.y < nextPosition.y)
                return Types.DIRECTIONS.DOWN;
            else if(position.y > nextPosition.y)
                return Types.DIRECTIONS.UP;
            else
                return Types.DIRECTIONS.NONE;
        }
        else if(position.y == nextPosition.y){
            if(position.x < nextPosition.x)
                return Types.DIRECTIONS.RIGHT;
            else if(position.x > nextPosition.x)
                return Types.DIRECTIONS.LEFT;
            else
                return Types.DIRECTIONS.NONE;
        }

        throw new IllegalArgumentException("Invalid position transition received: " + position + " to " + nextPosition);
    }

    /**
     * Determines the position resulted by a movement action in a particular direction.
     * @param myPosition - initial position
     * @param direction - movement direction
     * @return final position.
     */
    public static Vector2d getNextPosition(Vector2d myPosition, Types.DIRECTIONS direction){
        return myPosition.add(direction.toVec());
    }

    /**
     * Converts direction into movement action.
     * @param direction - the direction
     * @return movement action
     */
    public static Types.ACTIONS directionToAction(Types.DIRECTIONS direction)
    {
        if(direction == Types.DIRECTIONS.DOWN)
            return Types.ACTIONS.ACTION_DOWN;
        else if(direction == Types.DIRECTIONS.LEFT)
            return Types.ACTIONS.ACTION_LEFT;
        else if(direction == Types.DIRECTIONS.RIGHT)
            return Types.ACTIONS.ACTION_RIGHT;
        else if(direction == Types.DIRECTIONS.UP)
            return Types.ACTIONS.ACTION_UP;
        else if(direction == Types.DIRECTIONS.NONE)
            return Types.ACTIONS.ACTION_STOP;

        System.out.println("WARNING: " + direction + " is an invalid direction, using (0,0).");
        return Types.ACTIONS.ACTION_STOP;
    }

    /**
     * Checks if the given position is passable.
     * A passable tile must be an agent, a power-up or a passage; and it should not be an enemy.
     * @param board - game board
     * @param position - the position to be checked
     * @param enemies - array of enemy agents
     * @return the result as boolean
     */
    public static boolean positionIsPassable(Types.TILETYPE[][] board, Vector2d position, ArrayList<GameObject> enemies) {
        Types.TILETYPE tileType = board[position.y][position.x];

        boolean positionIsPassable = false;
        if(Types.TILETYPE.getAgentTypes().contains(tileType) || Types.TILETYPE.getPowerUpTypes().contains(tileType) ||
                tileType == Types.TILETYPE.PASSAGE){

            // Also check if position is an enemy
            boolean positionIsEnemy = false;
            for(GameObject enemy : enemies){
                if(tileType == enemy.getType()){
                    positionIsEnemy = true;
                    break;
                }
            }
            positionIsPassable = !positionIsEnemy;
        }
        return positionIsPassable;
    }

    /**
     * Checks if the given position matches the given tile type.
     * @param board - game board
     * @param position - the position to be checked
     * @param item - tile type to be checked
     * @return the result as boolean
     */
    public static boolean positionIsItem(Types.TILETYPE[][] board, Vector2d position, Types.TILETYPE item) {
        return board[position.y][position.x] == item;
    }

    /**
     * Checks if the given position contains fog.
     * @param board - game board
     * @param position - the position to be checked
     * @return the result as boolean
     */
    public static boolean positionIsFog(Types.TILETYPE[][] board, Vector2d position) {
        return positionIsItem(board, position, Types.TILETYPE.FOG);
    }

    /**
     * Checks if the given position is within the limits of the given board
     * @param board - game board
     * @param position - the position to be checked
     * @return the result as boolean
     */
    public static boolean positionOnBoard(Types.TILETYPE[][] board, Vector2d position) {
        return position.x < board[0].length && position.x >= 0 &&
                position.y < board.length && position.y >= 0;
    }


    /**
     * Helper method to set a winning status to all avatars in the passed array
     * @param ags array of game objects (avatars) that we wish to set the new winning status
     * @param status status to set.
     */
    public static void setWinningStatus(GameObject[] ags, Types.RESULT status) {
        for (GameObject gobj : ags) {
            ((Avatar)gobj).setWinner(status);
        }
    }

    /**
     * Helper method to set a winning status to all avatars in the passed arraylist
     * @param ags arraylist of game objects (avatars) that we wish to set the new winning status
     * @param status status to set.
     */
    public static void setWinningStatus(ArrayList<GameObject> ags, Types.RESULT status) {
        for (GameObject gobj : ags) {
            ((Avatar)gobj).setWinner(status);
        }
    }


    /**
     * Helper method to set a winning status to all avatars from a given team and in the passed array
     * @param ags array of game objects (avatars) that we wish to set the new winning status
     * @param status status to set.
     * @param team avatars which status will change belong to this team
     */
    public static void setWinningStatus(GameObject[]ags, Types.RESULT status, int team)
    {
        for (GameObject gobj : ags)
        {
            Avatar av = (Avatar)gobj;
            if (av.getTeam() == team)
                av.setWinner(status);
        }
    }

    /**
     * Finds all objects in an arraylist of GameObject given position.
     * @param pos - given position to search
     * @param objList - list to search for objects in.
     * @return - list of objects from list at given position.
     */
    public static ArrayList<GameObject> findObjectInList(Vector2d pos, ArrayList<GameObject> objList) {
        ArrayList<GameObject> gos = new ArrayList<>();
        for (GameObject go : objList) {
            if (go.getPosition() != null && go.getPosition().equals(pos)) {
                gos.add(go);
            }
        }
        return gos;
    }

    /**
     * Checks if a given avatar is stuck in a single cell.
     * @param board - given board to check position on
     * @param avatar - avatar
     * @return true if avatar is stuck, false otherwise
     */
    public static boolean isStuck(Types.TILETYPE[][] board, Avatar avatar) {
        for (Types.DIRECTIONS d: Types.DIRECTIONS.values()) {
            if (isPassable(board, avatar.getPosition().add(d.toVec()), avatar)) return false;
        }
        return true;
    }

    /**
     * Checks if the given position is passable by the avatar (lightweight version).
     * A passable tile must be a power-up, a passage or a bomb (if agent can kick).
     * @param board - game board
     * @param pos - the position to be checked
     * @param avatar - avatar
     * @return the result as boolean
     */
    public static boolean isPassable(Types.TILETYPE[][] board, Vector2d pos, Avatar avatar){
        if (pos != null && pos.x >= 0 && pos.y >= 0 && pos.x < board[0].length && pos.y < board.length &&
                (board[pos.y][pos.x] != null)){ // Tiletype is valid and the position is on board.
            Types.TILETYPE tiletype = board[pos.y][pos.x];
            return tiletype == Types.TILETYPE.PASSAGE ||
                    tiletype == Types.TILETYPE.BOMB ||
                    Types.TILETYPE.getPowerUpTypes().contains(tiletype);
        }
        return false;
    }

    /**
     * Checks if a given avatar is stuck in a single cell.
     * @param board - given board to check position on
     * @param avatar - avatar
     * @return true if avatar is stuck, false otherwise
     */
    private static ArrayList<Vector2d> passableDirections(Types.TILETYPE[][] board, Avatar avatar) {
        ArrayList<Vector2d> passableDirections = new ArrayList<>();
        for(Types.DIRECTIONS d : Types.DIRECTIONS.values())
        {
            Vector2d newPos = new Vector2d(avatar.getPosition().x + d.x(), avatar.getPosition().y + d.y());
            if(isPassable(board, newPos, avatar))
                passableDirections.add(newPos);
        }
        return passableDirections;
    }

    /**
     * Checks if a given avatar is stuck in a single cell.
     * @param board - given board to check position on
     * @param avatar - avatar
     * @return true if avatar is stuck, false otherwise
     */
    public static boolean isStuckAdvanced(Types.TILETYPE[][] board, ArrayList<GameObject> bombs, Avatar avatar) {

        boolean isStuck = true;

        // Determine which tiles may have flames next turn (based on bomb lives but also on early triggers)
        HashSet<Vector2d> upcomingFlames = new HashSet<>();
        for (GameObject b : bombs){
            ArrayList<GameObject> flames = new ArrayList<>();

            if (b.getLife() == 1) {

                Vector2d position = b.getPosition();
                int blastStrength = ((Bomb) b).getBlastStrength();

                // First add the flame at the current position
                expandFlames(position.x, position.y, board, upcomingFlames);
                boolean advanceP = true;
                boolean advanceM = true;
                for (int i = 1; i < blastStrength; i++) {
                    if (advanceP) {
                        int x1 = position.x + i;
                        advanceP = expandFlames(x1, position.y, board, upcomingFlames);
                    }
                    if (advanceM) {
                        int x2 = position.x - i;
                        advanceM = expandFlames(x2, position.y, board, upcomingFlames);
                    }
                }
                advanceM = true;
                advanceP = true;
                for (int i = 1; i < blastStrength; i++) {
                    if (advanceP) {
                        int y1 = position.y + i;
                        advanceP = expandFlames(position.x, y1, board, upcomingFlames);
                    }
                    if (advanceM) {
                        int y2 = position.y - i;
                        advanceM = expandFlames(position.x, y2, board, upcomingFlames);
                    }
                }
            }
        }

        Vector2d avatarPosition = avatar.getPosition();

        // Is avatar in danger?
        if (upcomingFlames.contains(avatarPosition)){
            // Check passable movement directions
            ArrayList<Vector2d> passableDirections = passableDirections(board, avatar);
            for (Vector2d pd : passableDirections){
                if(!upcomingFlames.contains(pd)){
                    isStuck = false;
                    break;
                }
            }
        }
        else{
            isStuck = false;
        }

        return isStuck;
    }

    /**
     * Accumulates in an array of flames the position (x,y) unless there's a rigid block in there.
     * @param x x coordinate of the position
     * @param y y coordinate of the position
     * @param board current board state.
     * @param upcomingFlames Array of flame positions to maybe add this new position to.
     * @return false if the flame stops at (x,y)
     */
    private static boolean expandFlames(int x, int y, Types.TILETYPE[][] board, HashSet<Vector2d> upcomingFlames) {
        if (x < 0 || y < 0 || x >= board[0].length || y >= board.length || board[y][x] == Types.TILETYPE.RIGID)
            return false;
        upcomingFlames.add(new Vector2d(x, y));
        return (board[y][x] != Types.TILETYPE.WOOD); // Flames should stop at first wooden block
    }
}
