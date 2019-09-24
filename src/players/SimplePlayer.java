package players;

import core.GameState;
import objects.Bomb;
import objects.GameObject;
import utils.Types;
import utils.Vector2d;

import java.util.*;

import static java.lang.Math.*;
import static utils.Utils.*;

public class SimplePlayer extends Player {
    private Random random;
    private ArrayList<Vector2d> recentlyVisitedPositions;
    private int recentlyVisitedLength;

    /**
     * Constructor.
     * @param seed seed for random moves.
     * @param id ID of this player.
     */
    public SimplePlayer(long seed, int id) {
        super(seed, id);
        reset(seed, id);
    }


    /**
     * Makes a copy of this player.
     * @return a deep copy of this player.
     */
    @Override
    public Player copy() {
        SimplePlayer player = new SimplePlayer(seed,playerID);
        player.recentlyVisitedPositions = new ArrayList<>();
        recentlyVisitedPositions.forEach(e -> player.recentlyVisitedPositions.add(e.copy()));
        player.recentlyVisitedLength = recentlyVisitedLength;
        //player.prevDirection = prevDirection;
        return player;
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);
        random = new Random(seed);

        this.recentlyVisitedPositions = new ArrayList<>();
        this.recentlyVisitedLength = 6;
    }

    // Container for return values of Dijkstra's pathfinding algorithm.
    public class Container
    {
        HashMap<Types.TILETYPE, ArrayList<Vector2d> > items;
        HashMap<Vector2d, Integer> dist;
        HashMap<Vector2d, Vector2d> prev;

        Container() { }
    }

    /**
     * Called every tick, returns the action to execute one each frame.
     * @param gs - current game state.
     * @return
     */
    @Override
    public Types.ACTIONS act(GameState gs) {

        // 1) Initialise the required information off GameState
        Vector2d myPosition = gs.getPosition();

        Types.TILETYPE[][] board = gs.getBoard();
        int[][] bombBlastStrength = gs.getBombBlastStrength();
        int[][] bombLife = gs.getBombLife();

        int ammo = gs.getAmmo();
        int blastStrength = gs.getBlastStrength();

        ArrayList<Types.TILETYPE> enemiesObs = gs.getAliveEnemyIDs();

        int boardSizeX = board.length;
        int boardSizeY = board[0].length;

        ArrayList<Bomb> bombs = new ArrayList<>();
        ArrayList<GameObject> enemies = new ArrayList<>();

        for (int x = 0; x < boardSizeX; x++) {
            for (int y = 0; y < boardSizeY; y++) {

                Types.TILETYPE type = board[y][x];

                if(type == Types.TILETYPE.BOMB || bombBlastStrength[y][x] > 0){
                    // Create bomb object
                    Bomb bomb = new Bomb();
                    bomb.setPosition(new Vector2d(x, y));
                    bomb.setBlastStrength(bombBlastStrength[y][x]);
                    bomb.setLife(bombLife[y][x]);
                    bombs.add(bomb);
                }
                else if(Types.TILETYPE.getAgentTypes().contains(type) &&
                        type.getKey() != gs.getPlayerId()){ // May be an enemy
                    if(enemiesObs.contains(type)) { // Is enemy
                        // Create enemy object
                        GameObject enemy = new GameObject(type);
                        enemy.setPosition(new Vector2d(x, y));
                        enemies.add(enemy); // no copy needed
                    }
                }
            }
        }

        // items: tile types with their coordinates
        // dist: coordinates with their distance
        // prev: shortest path with previous node and the distance to it

        Container from_dijkstra = dijkstra(board, myPosition, bombs, enemies, 10);
        HashMap<Types.TILETYPE, ArrayList<Vector2d>> items = from_dijkstra.items;
        Iterator it;
        HashMap<Vector2d, Integer> dist = from_dijkstra.dist;
        HashMap<Vector2d, Vector2d> prev = from_dijkstra.prev;

        // 2) Move if we are in an unsafe place.
        HashMap<Types.DIRECTIONS, Integer> unsafeDirections = directionsInRangeOfBomb(myPosition, bombs, dist);

        if(!unsafeDirections.isEmpty()){

            ArrayList<Types.DIRECTIONS> directions = findSafeDirections(board, myPosition, unsafeDirections, bombs, enemies);

            if(!directions.isEmpty()) {
                return directionToAction(directions.get(random.nextInt(directions.size())));
            }
            else {
                return Types.ACTIONS.ACTION_STOP;
            }
        }

        // 3) Lay bomb if we are adjacent to an enemy.
        if(isAdjacentEnemy(items, dist, enemies) && maybeBomb(ammo, blastStrength, items, dist, myPosition)){
            return Types.ACTIONS.ACTION_BOMB;
        }

        //  4) Move towards an enemy if there is one in exactly three reachable spaces.
        // check dist to nearest enemy
        // enemies - list of ArrayList of game objects
        for (GameObject en: enemies){
            Iterator dist_it = dist.entrySet().iterator(); // <Vector2d, Integer>
            while (dist_it.hasNext()){
                Map.Entry<Vector2d, Integer> entry = (Map.Entry)dist_it.next();

                if (entry.getKey().equals(en.getPosition()) && entry.getValue() == 3){
                    // pick this direction
                    Vector2d next_node = entry.getKey();
                    while (!myPosition.equals(prev.get(next_node))){
                        next_node = prev.get(next_node);
                    }
                    // return node, which had prev_node
                    return directionToAction(getDirection(myPosition, next_node));

                }
            }

        }

        // 5) Move towards a good item if there is one within two reachable spaces.
        // good items are the pickups
        it = items.entrySet().iterator();
        Vector2d previousNode = new Vector2d(-1, -1); // placeholder, these values are not actually used
        int distance = Integer.MAX_VALUE;
        while (it.hasNext()){
            Map.Entry<Types.TILETYPE, ArrayList<Vector2d> > entry = (Map.Entry)it.next();
            // check pickup entries on the board
            if (Types.TILETYPE.getPowerUpTypes().contains(entry.getKey())){
                // no need to store just get closest
                for (Vector2d coords: entry.getValue()){
                    if (dist.get(coords) < distance){
                        distance = dist.get(coords);
                        previousNode = coords;
                    }
                }
            }
        }
        if (distance <= 2){
            // iterate until we get to the immadiate next node
            if (myPosition.equals(previousNode)){
                return directionToAction(getDirection(myPosition, previousNode));
            }
            while (!myPosition.equals(prev.get(previousNode))){ ;
                previousNode = prev.get(previousNode);
            }
            return directionToAction(getDirection(myPosition, previousNode));
        }

        // 6) Maybe lay a bomb if we are within a space of a wooden wall.
        it = items.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Types.TILETYPE, ArrayList<Vector2d>> entry = (Map.Entry) it.next();
            // check pickup entries on the board
            if (entry.getKey().equals(Types.TILETYPE.WOOD) ) {
                // check the distance from the wooden planks
                for (Vector2d coords: entry.getValue()){
                    if (dist.get(coords) == 1){
                        if( maybeBomb(ammo, blastStrength, items, dist, myPosition)){
                            return Types.ACTIONS.ACTION_BOMB;
                        }
                    }
                }
                // 7) Move towards a wooden wall if there is one within two reachable spaces and you have a bomb.
                if (ammo < 1) continue;
                for (Vector2d coords:entry.getValue()){
                    // max 2 reachable space
                    if (dist.get(coords) <= 2){
                        previousNode = coords;
                        while (!myPosition.equals(prev.get(previousNode))){
                            previousNode = prev.get(previousNode);
                        }
                        Types.DIRECTIONS direction = getDirection(myPosition, previousNode);
                        if (direction != null){
                            ArrayList<Types.DIRECTIONS> dirArray = new ArrayList<>();
                            dirArray.add(direction);
                            dirArray = filterUnsafeDirections(myPosition, dirArray, bombs);

                            if (dirArray.size() > 0){
                                return directionToAction(dirArray.get(0));
                            }
                        }


                    }
                }
            }
        }

        // 8) Choose a random but valid direction.
        ArrayList<Types.DIRECTIONS> directions = new ArrayList<>();
        directions.add(Types.DIRECTIONS.UP);
        directions.add(Types.DIRECTIONS.DOWN);
        directions.add(Types.DIRECTIONS.LEFT);
        directions.add(Types.DIRECTIONS.RIGHT);
        ArrayList<Types.DIRECTIONS> validDirections = filterInvalidDirections(board, myPosition, directions, enemies);
        validDirections = filterUnsafeDirections(myPosition, validDirections, bombs );
        validDirections = filterRecentlyVisited(validDirections, myPosition, this.recentlyVisitedPositions);

        // 9) Add this position to the recently visited uninteresting positions so we don't return immediately.
        recentlyVisitedPositions.add(myPosition);
        if (recentlyVisitedPositions.size() > recentlyVisitedLength)
            recentlyVisitedPositions.remove(0);

        if (validDirections.size() > 0){
            int actionIdx = random.nextInt(validDirections.size());
            return directionToAction(validDirections.get(actionIdx));
        }

        return Types.ACTIONS.ACTION_STOP;
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    /**
     * Dijkstra's pathfinding
     * @param board - game board
     * @param myPosition - the position of agent
     * @param bombs - array of bombs in the game
     * @param enemies - array of enemies in the game
     * @param depth - depth of search (default: 10)
     * @return A set of paths to the different elements in the game.
     */
    private Container dijkstra(Types.TILETYPE[][] board, Vector2d myPosition, ArrayList<Bomb> bombs,
                               ArrayList<GameObject> enemies, int depth){

        HashMap<Types.TILETYPE, ArrayList<Vector2d> > items = new HashMap<>();
        HashMap<Vector2d, Integer> dist = new HashMap<>();
        HashMap<Vector2d, Vector2d> prev = new HashMap<>();

        Queue<Vector2d> Q = new LinkedList<>();

        for(int r = max(0, myPosition.x - depth); r < min(board.length, myPosition.x + depth); r++){
            for(int c = max(0, myPosition.y - depth); c < min(board.length, myPosition.y + depth); c++){

                Vector2d position = new Vector2d(r, c);

                // Determines if two points are out of range of each other.
                boolean out_of_range = (abs(c - myPosition.y) + abs(r - myPosition.x)) > depth;
                if(out_of_range)
                    continue;

                Types.TILETYPE itemType = board[c][r];
                boolean positionInItems = (itemType == Types.TILETYPE.FOG ||
                        itemType == Types.TILETYPE.RIGID || itemType == Types.TILETYPE.FLAMES);
                if(positionInItems)
                    continue;

                ArrayList<Vector2d> itemsTempList = items.get(itemType);
                if(itemsTempList == null) {
                    itemsTempList = new ArrayList<>();
                }
                itemsTempList.add(position);
                items.put(itemType, itemsTempList);

                if(position.equals(myPosition)){
                    Q.add(position);
                    dist.put(position, 0);
                }
                else{
                    dist.put(position, Integer.MAX_VALUE);
                }
            }
        }

        for(Bomb bomb : bombs){
            if(bomb.getPosition().equals(myPosition)){
                ArrayList<Vector2d> itemsTempList = items.get(Types.TILETYPE.BOMB);
                if(itemsTempList == null) {
                    itemsTempList = new ArrayList<>();
                }
                itemsTempList.add(myPosition);
                items.put(Types.TILETYPE.BOMB, itemsTempList);
            }
        }

        while(!Q.isEmpty()){
            Vector2d position = Q.remove();

            if(positionIsPassable(board, position, enemies)){
                int val = dist.get(position) + 1;

                //Types.DIRECTIONS[] directionsToBeChecked = Types.DIRECTIONS.values();
                Types.DIRECTIONS[] directionsToBeChecked = {Types.DIRECTIONS.LEFT, Types.DIRECTIONS.RIGHT,
                        Types.DIRECTIONS.UP, Types.DIRECTIONS.DOWN};

                for (Types.DIRECTIONS directionToBeChecked : directionsToBeChecked) {

                    Vector2d direction = directionToBeChecked.toVec();
                    Vector2d new_position = new Vector2d(position.x + direction.x, position.y + direction.y);

                    if(!dist.containsKey(new_position))
                        continue;

                    int dist_val = dist.get(new_position);

                    if(val < dist_val){
                        dist.put(new_position, val);
                        prev.put(new_position, position);
                        Q.add(new_position);
                    }
                    else if(val == dist_val && random.nextFloat() < 0.5){
                        dist.put(new_position, val);
                        prev.put(new_position, position);
                    }
                }
            }
        }

        Container container = new Container();
        container.dist = dist;
        container.items = items;
        container.prev = prev;

        return container;
    }

    /**
     * Calculates those directions from the agent's posititon that are in the direction of a bob explosion.
     * @param myPosition - Position of this agent.
     * @param bombs - List of bombs in the board now
     * @param dist - The list of distances to all bombs.
     * @return A set of directions that would fall in the bomb explosion range.
     */
    private HashMap<Types.DIRECTIONS, Integer> directionsInRangeOfBomb(Vector2d myPosition, ArrayList<Bomb> bombs,
            HashMap<Vector2d, Integer> dist) {
        HashMap<Types.DIRECTIONS, Integer> ret = new HashMap<>();

        for(Bomb bomb : bombs){
            Vector2d position = bomb.getPosition();

            if(!dist.containsKey(position))
                continue;

            int distance = dist.get(position);
            int bombBlastStrength = bomb.getBlastStrength();

            if(distance > bombBlastStrength)
                continue;

            // We are on a bomb. All directions are in range of bomb.
            if(myPosition.x == position.x && myPosition.y == position.y){

                Types.DIRECTIONS[] directions = {Types.DIRECTIONS.LEFT, Types.DIRECTIONS.RIGHT,
                        Types.DIRECTIONS.UP, Types.DIRECTIONS.DOWN};

                for (Types.DIRECTIONS direction : directions) {
                    ret.put(direction, max(ret.getOrDefault(direction, 0), bombBlastStrength));
                }
            }
            else if(myPosition.x == position.x){
                if(myPosition.y < position.y){ // Bomb is down.
                    ret.put(Types.DIRECTIONS.DOWN, max(ret.getOrDefault(Types.DIRECTIONS.DOWN, 0), bombBlastStrength));
                }
                else{ // Bomb is up.
                    ret.put(Types.DIRECTIONS.UP, max(ret.getOrDefault(Types.DIRECTIONS.UP, 0), bombBlastStrength));
                }
            }
            else if(myPosition.y == position.y){
                if(myPosition.x < position.x){ // Bomb is right.
                    ret.put(Types.DIRECTIONS.RIGHT, max(ret.getOrDefault(Types.DIRECTIONS.RIGHT, 0), bombBlastStrength));
                }
                else{ // Bomb is left.
                    ret.put(Types.DIRECTIONS.LEFT, max(ret.getOrDefault(Types.DIRECTIONS.LEFT, 0), bombBlastStrength));
                }
            }
        }
        return ret;
    }

    /**
     * Checks if a given position is considered to be one where I couldn't get out of and a bomb would kill me.
     * @param nextPosition - My next position to check.
     * @param bombRange - Range of the bomb that could kill me
     * @param nextBoard - Board of the game.
     * @param enemies - List of enemies
     * @return true if the position is not a good one to be in.
     */
    private boolean isStuckPosition(Vector2d nextPosition, int bombRange, Types.TILETYPE[][] nextBoard,
                                    ArrayList<GameObject> enemies) {
        // A tuple class for PriorityQueue since it does not support pair of values in default
        class Tuple implements Comparable<Tuple>{
            private int distance;
            private Vector2d position;

            private Tuple(int distance, Vector2d position){
                this.distance = distance;
                this.position = position;
            }

            @Override
            public int compareTo(Tuple tuple) {
                return this.distance - tuple.distance;
            }
        }

        PriorityQueue<Tuple> Q = new PriorityQueue<>();
        Q.add(new Tuple(0, nextPosition));

        Set<Vector2d> seen = new HashSet<>();

        boolean is_stuck = true;

        while(!Q.isEmpty()){
            Tuple tuple = Q.remove();
            int dist = tuple.distance;
            Vector2d position = tuple.position;

            seen.add(position);

            if(nextPosition.x != position.x && nextPosition.y != position.y){
                is_stuck = false;
                break;
            }

            if(dist > bombRange){
                is_stuck = false;
                break;
            }

            Types.DIRECTIONS[] directions = {Types.DIRECTIONS.LEFT, Types.DIRECTIONS.RIGHT,
                    Types.DIRECTIONS.UP, Types.DIRECTIONS.DOWN};
            //Types.DIRECTIONS.values();

            for (Types.DIRECTIONS direction : directions) {
                Vector2d newPosition = position.copy();
                newPosition = newPosition.add(direction.toVec());

                if(seen.contains(newPosition)) continue;

                if(!positionOnBoard(nextBoard, newPosition)) continue;

                if(!positionIsPassable(nextBoard, newPosition, enemies)) continue;

                dist = abs(direction.x() + position.x - nextPosition.x) +
                        abs(direction.y() + position.y - nextPosition.y);

                Q.add(new Tuple(dist, newPosition));
            }
        }
        return is_stuck;
    }

    /**
     * Finds a list of directions that is Safe to move to.
     * @param board - Current game board
     * @param myPosition - Current position of the agent.
     * @param unsafeDirections - Set of previously determined unsafe directions.
     * @param bombs - List of bombs currently in hte game.
     * @param enemies - List of enemies in hte game.
     * @return A set of directions that would be safe to move (may be empty)
     */
    private ArrayList<Types.DIRECTIONS> findSafeDirections(Types.TILETYPE[][] board, Vector2d myPosition,
                                                           HashMap<Types.DIRECTIONS, Integer> unsafeDirections,
                                                           ArrayList<Bomb> bombs, ArrayList<GameObject> enemies) {
        // All directions are unsafe. Return a position that won't leave us locked.
        ArrayList<Types.DIRECTIONS> safe = new ArrayList<>();

        if(unsafeDirections.size() == 4){

            Types.TILETYPE[][] nextBoard = new Types.TILETYPE[board.length][];
            for (int i = 0; i < board.length; i++) {
                nextBoard[i] = new Types.TILETYPE[board[i].length];
                for (int i1 = 0; i1 < board[i].length; i1++) {
                    if (board[i][i1] != null) {
                        // Power-ups array contains null elements, don't attempt to copy those.
                        nextBoard[i][i1] = board[i][i1];
                    }
                }
            }

            nextBoard[myPosition.y][myPosition.x] = Types.TILETYPE.BOMB;

            for (Map.Entry<Types.DIRECTIONS, Integer> entry : unsafeDirections.entrySet()){

                Types.DIRECTIONS direction = entry.getKey();
                int bomb_range = entry.getValue();

                Vector2d nextPosition = myPosition.copy();
                nextPosition = nextPosition.add(direction.toVec());

                if(!positionOnBoard(nextBoard, nextPosition) ||
                        !positionIsPassable(nextBoard, nextPosition, enemies))
                    continue;

                if(!isStuckPosition(nextPosition, bomb_range, nextBoard, enemies)){
                    return new ArrayList<>(Arrays.asList(direction));
                }
            }
            return safe;
        }

        // The directions that will go off the board.
        Set<Types.DIRECTIONS> disallowed = new HashSet<>();

        Types.DIRECTIONS[] directions = {Types.DIRECTIONS.LEFT, Types.DIRECTIONS.RIGHT,
                Types.DIRECTIONS.UP, Types.DIRECTIONS.DOWN};

        //Types.DIRECTIONS.values();

        for (Types.DIRECTIONS current_direction : directions) {

            Vector2d position = myPosition.copy();
            position = position.add(current_direction.toVec());

            Types.DIRECTIONS direction = getDirection(myPosition, position);

            if(!positionOnBoard(board, position)){
                disallowed.add(direction);
                continue;
            }

            if(unsafeDirections.containsKey(direction)) {
                continue;
            }

            if(positionIsPassable(board, position, enemies) || positionIsFog(board, position)){
                safe.add(direction);
            }
        }

        if(safe.isEmpty()){
            // We don't have any safe directions, so return something that is allowed.
            for(Types.DIRECTIONS k : unsafeDirections.keySet()) {
                if(!disallowed.contains(k))
                    safe.add(k);
            }
        }

        return safe;
    }

    /**
     * Checks if there's an adjecent enemy.
     * @param objects - Game objects in the board.
     * @param dist - Distance to different positions around me.
     * @param enemies - Set of enemy players.
     * @return true if an agent is next to this player.
     */
    private boolean isAdjacentEnemy(
            HashMap<Types.TILETYPE, ArrayList<Vector2d> > objects,
            HashMap<Vector2d, Integer> dist,
            ArrayList<GameObject> enemies)
    {
        for(GameObject enemy : enemies){
            if(objects.containsKey(enemy.getType())) {
                ArrayList<Vector2d> items_list = objects.get(enemy.getType());
                for (Vector2d position : items_list) {
                    if (dist.get(position) == 1)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines whether we can safely bomb right now.
     * @param ammo - our ammo count
     * @param blastStrength - our blast strength
     * @param objects - list of objects in the board.
     * @param dist - distances to positions in the board.
     * @param myPosition - our agent's position
     * @return true if if would be a good idea to drop a bomb here
     */
    private boolean maybeBomb(int ammo, int blastStrength, HashMap<Types.TILETYPE, ArrayList<Vector2d> > objects,
                              HashMap<Vector2d, Integer> dist, Vector2d myPosition) {
        // Do we have ammo?
        if(ammo < 1)
            return false;

        if(objects.containsKey(Types.TILETYPE.PASSAGE)){
            ArrayList<Vector2d> items_list = objects.get(Types.TILETYPE.PASSAGE);

            // Will we be stuck?
            for (Vector2d position : items_list) {

                if(dist.containsKey(position)){
                    if(dist.get(position) > Integer.MAX_VALUE)
                        continue;
                }

                // We can reach a passage that's outside of the bomb strength.
                if(dist.containsKey(position)){
                    if(dist.get(position) > blastStrength)
                        return true;
                }

                // We can reach a passage that's outside of the bomb scope.
                if(position.x != myPosition.x && position.y != myPosition.y)
                    return true;
            }
        }
        return false;
    }

    /**
     * Checks unsafe directions of movements, where a bomb could hit me if exploding
     * @param myPosition - The position the agent is in
     * @param directions - Directions I could move towards
     * @param bombs - current set of bombs in the level
     * @return The list of safe directions to move to.
     */
    private ArrayList<Types.DIRECTIONS> filterUnsafeDirections(Vector2d myPosition, ArrayList<Types.DIRECTIONS> directions, ArrayList<Bomb> bombs){
        ArrayList<Types.DIRECTIONS> safeDirections = new ArrayList<>();
        for (Types.DIRECTIONS dir : directions){
            Vector2d myPos = getNextPosition(myPosition, dir);
            boolean isBad = false;
            for (Bomb b: bombs){
                int bombX = b.getPosition().x;
                int bombY = b.getPosition().y;
                int blastStrenght = b.getBlastStrength();
                if ((myPos.x == bombX && Math.abs(bombY - myPos.y) <= blastStrenght) ||
                (myPos.y == bombY && Math.abs(bombX - myPos.x) <= blastStrenght)){
                    isBad = true;
                    break;
                }
            }
            if (!isBad){
                safeDirections.add(dir);
            }

        }

        return safeDirections;
    }

    /**
     * List of valid directions from current position. Avoids leaving board and moving against walls.
     * @param board - The current board.
     * @param myPosition - Position of this agent.
     * @param directions - Possible directions to move to
     * @param enemies - List of enemies in the game.
     * @return A subset of the directions received that would be valid to move to
     */
    private ArrayList<Types.DIRECTIONS> filterInvalidDirections(Types.TILETYPE[][] board,
                                                                Vector2d myPosition, ArrayList<Types.DIRECTIONS> directions,
                                                                ArrayList enemies){
        ArrayList<Types.DIRECTIONS> validDirections = new ArrayList<>();
        for (Types.DIRECTIONS d: directions){
            Vector2d position = getNextPosition(myPosition, d);
            if (positionOnBoard(board, position) && (positionIsPassable(board, position, enemies))){
                validDirections.add(d);
            }
        }
        return validDirections;
    }

    /**
     * Checks for directions that would take the agent to positions that have been recently visted.
     * @param directions - Set of initial possible directions
     * @param myPosition - Current position of the agent.
     * @param recentlyVisitedPositions - set of recently visited positions.
     * @return A subset of the directions received that would be okay to move to
     */
    private ArrayList<Types.DIRECTIONS> filterRecentlyVisited(ArrayList<Types.DIRECTIONS> directions,
                                                              Vector2d myPosition, ArrayList<Vector2d> recentlyVisitedPositions){
        ArrayList<Types.DIRECTIONS> filtered = new ArrayList<>();
        for (Types.DIRECTIONS d : directions){
            if (!recentlyVisitedPositions.contains(getNextPosition(myPosition, d)))
                filtered.add(d);
        }
        if (filtered.size() > 0)
             return directions;

        return filtered;
    }
}