package players.heuristics;

import core.GameState;
import objects.Bomb;
import objects.GameObject;
import utils.Types;
import utils.Vector2d;

import java.util.*;

import static java.lang.Math.*;
import static java.lang.Math.min;
import static utils.Utils.*;
import static utils.Utils.positionIsPassable;

public class AdvancedHeuristic extends StateHeuristic {

    private BoardStats rootBoardStats;
    private Random random;

    public AdvancedHeuristic(GameState root, Random random) {
        this.random = random;
        rootBoardStats = new BoardStats(root, this.random);

    }

    @Override
    public double evaluateState(GameState gs) {
        boolean gameOver = gs.isTerminal();
        Types.RESULT win = gs.winner();

        // Compute a score relative to the root's state.
        BoardStats lastBoardState = new BoardStats(gs, this.random);
        double rawScore = rootBoardStats.score(lastBoardState);

        // TODO: Should we reserve -1 and 1 to LOSS and WIN, and shrink rawScore to be in [-0.5, 0.5]?
        // rawScore is in [-1, 1], move it to [-0.5, 0.5]
        rawScore /= 2.0;

        if(gameOver && win == Types.RESULT.LOSS)
            rawScore = -1;

        if(gameOver && win == Types.RESULT.WIN)
            rawScore = 1;

        return rawScore;
    }

    public static class BoardStats
    {
        int tick, nTeammates, nEnemies, blastStrength;
        boolean canKick;
        int nWoods;

        static double maxWoods = -1;
        static double maxBlastStrength = 10;

        // 0.4
        double FACTOR_SAFE_DIRECTIONS = 0.2;
        double FACTOR_BOMB_DIRECTIONS = 0.2;

        // 0.3
        double FACTOR_ENEMY;
        double FACTOR_TEAM;

        // 0.1
        double FACTOR_ENEMY_DIST = 0.1;

        // 0.2
        double FACTOR_CANKICK = 0.05;
        double FACTOR_BLAST = 0.05;
        //double FACTOR_ADJ_ENEMY = 0.12;
        double FACTOR_NEAREST_POWERUP = 0.05;
        double FACTOR_WOODS = 0.05;

        // State information
        private Random random;

        private Vector2d myPosition;
        private Types.TILETYPE[][] board;
        private ArrayList<Bomb> bombs;
        private ArrayList<GameObject> enemies;

        private HashMap<Types.TILETYPE, ArrayList<Vector2d>> items;
        private HashMap<Vector2d, Integer> dist;
        private HashMap<Vector2d, Vector2d> prev;

        // Extra state information (to be used as heuristics):

        // Directions in range of a bomb
        private HashMap<Types.DIRECTIONS, Integer> directionsInRangeOfBomb = null;
        private Integer n_directionsInRangeOfBomb = null;

        // Safe directions
        private ArrayList<Types.DIRECTIONS> safeDirections = null;
        private Integer n_safeDirections = null;

        // Adjacency to an enemy
        private Integer isAdjacentEnemy = null;

        // Distance to nearest enemy
        private Integer distanceToNearestEnemy = null;

        // Distance to nearest power-up, up to 10 (default: 1000 as max distance)
        private Integer distanceToNearestPowerUp = null;

        BoardStats(GameState gs, Random random) {

            this.random = random;

            nEnemies = gs.getAliveEnemyIDs().size();

            // Init weights based on game mode
            if (gs.getGameMode() == Types.GAME_MODE.FFA) {
                FACTOR_TEAM = 0;
                FACTOR_ENEMY = 0.3;
            } else {
                FACTOR_TEAM = 0.1;
                FACTOR_ENEMY = 0.2;
                nTeammates = gs.getAliveTeammateIDs().size();  // We only need to know the alive teammates in team modes
                nEnemies -= 1;  // In team modes there's an extra Dummy agent added that we don't need to care about
            }

            // Save game state information
            this.tick = gs.getTick();
            this.blastStrength = gs.getBlastStrength();
            this.canKick = gs.canKick();

            // Count the number of wood walls
            this.nWoods = 1;
            for (Types.TILETYPE[] gameObjectsTypes : gs.getBoard()) {
                for (Types.TILETYPE gameObjectType : gameObjectsTypes) {
                    if (gameObjectType == Types.TILETYPE.WOOD)
                        nWoods++;
                }
            }
            if (maxWoods == -1) {
                maxWoods = nWoods;
            }

            this.myPosition = gs.getPosition();
            this.board = gs.getBoard();
            int[][] bombBlastStrength = gs.getBombBlastStrength();
            int[][] bombLife = gs.getBombLife();
            int ammo = gs.getAmmo();
            int blastStrength = gs.getBlastStrength();
            ArrayList<Types.TILETYPE> enemyIDs = gs.getAliveEnemyIDs();
            int boardSizeX = board.length;
            int boardSizeY = board[0].length;

            this.bombs = new ArrayList<>();
            this.enemies = new ArrayList<>();

            for (int x = 0; x < boardSizeX; x++) {
                for (int y = 0; y < boardSizeY; y++) {

                    if(board[y][x] == Types.TILETYPE.BOMB){
                        // Create a bomb object
                        Bomb bomb = new Bomb();
                        bomb.setPosition(new Vector2d(x, y));
                        bomb.setBlastStrength(bombBlastStrength[y][x]);
                        bomb.setLife(bombLife[y][x]);
                        bombs.add(bomb);
                    }
                    else if(Types.TILETYPE.getAgentTypes().contains(board[y][x]) &&
                            board[y][x].getKey() != gs.getPlayerId()){ // May be an enemy
                        if(enemyIDs.contains(board[y][x])) { // Is enemy
                            // Create enemy object
                            GameObject enemy = new GameObject(board[y][x]);
                            enemy.setPosition(new Vector2d(x, y));
                            enemies.add(enemy); // no copy needed
                        }
                    }
                }
            }

            Container from_dijkstra = dijkstra(board, myPosition, bombs, enemies, 10);
            this.items = from_dijkstra.items;
            this.dist = from_dijkstra.dist;
            this.prev = from_dijkstra.prev;
        }

        /**
         * Computes score for a game, in relation to the initial state at the root.
         * Minimizes number of opponents in the game and number of wood walls. Maximizes blast strength and
         * number of teammates, wants to kick.
         * @param futureState the stats of the board at the end of the rollout.
         * @return a score [0, 1]
         */
        double score(BoardStats futureState)
        {
            int diffSafeDirections = futureState.getNumberOfSafeDirections() - this.getNumberOfSafeDirections();
            int diffDirectionsInRangeOfBomb = -(futureState.getNumberOfDirectionsInRangeOfBomb() - this.getNumberOfDirectionsInRangeOfBomb());

            int diffTeammates = futureState.nTeammates - this.nTeammates;
            int diffEnemies = -(futureState.nEnemies - this.nEnemies);

            int diffDistanceToNearestEnemy = -(futureState.getDistanceToNearestEnemy() - this.getDistanceToNearestEnemy());

            int diffWoods = -(futureState.nWoods - this.nWoods);
            int diffCanKick = futureState.canKick && !this.canKick ? 1 : 0;
            int diffBlastStrength = futureState.blastStrength - this.blastStrength;
            //int diffAdjacentEnemy = futureState.getIsAdjacentEnemy() - this.getIsAdjacentEnemy();
            int diffDistanceToNearestPowerUp = -(futureState.getDistanceToNearestPowerUp() - this.getDistanceToNearestPowerUp());

            return (diffSafeDirections / 4.0) * FACTOR_SAFE_DIRECTIONS
                    + (diffDirectionsInRangeOfBomb / 4.0) * FACTOR_BOMB_DIRECTIONS
                    + (diffEnemies / 3.0) * FACTOR_ENEMY
                    + diffTeammates * FACTOR_TEAM
                    + (diffDistanceToNearestEnemy / 10.0) * FACTOR_ENEMY_DIST
                    + (diffWoods / maxWoods) * FACTOR_WOODS
                    + diffCanKick * FACTOR_CANKICK
                    + (diffBlastStrength / maxBlastStrength) * FACTOR_BLAST
                    //+ diffAdjacentEnemy * FACTOR_ADJ_ENEMY
                    + (diffDistanceToNearestPowerUp / 10.0) * FACTOR_NEAREST_POWERUP;
        }

        private HashMap<Types.DIRECTIONS, Integer> getDirectionsInRangeOfBomb(){
            if(this.directionsInRangeOfBomb == null){
                this.directionsInRangeOfBomb = computeDirectionsInRangeOfBomb(this.myPosition, this.bombs, this.dist);
            }
            return this.directionsInRangeOfBomb;
        }

        private Integer getNumberOfDirectionsInRangeOfBomb(){
            if(this.n_directionsInRangeOfBomb == null){
                this.n_directionsInRangeOfBomb = getDirectionsInRangeOfBomb().size();
            }
            return this.n_directionsInRangeOfBomb;
        }

        private HashMap<Types.DIRECTIONS, Integer> computeDirectionsInRangeOfBomb(Vector2d myPosition, ArrayList<Bomb> bombs,
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

                if(myPosition == position){ // We are on a bomb. All directions are in range of bomb.
                    Types.DIRECTIONS[] directions = Types.DIRECTIONS.values();

                    for (Types.DIRECTIONS direction : directions) {
                        ret.put(direction, max(ret.getOrDefault(direction, 0), bombBlastStrength));
                    }
                }
                else if(myPosition.x == position.x){
                    if(myPosition.y < position.y){ // Bomb is right.
                        ret.put(Types.DIRECTIONS.DOWN, max(ret.getOrDefault(Types.DIRECTIONS.DOWN, 0), bombBlastStrength));
                    }
                    else{ // Bomb is left.
                        ret.put(Types.DIRECTIONS.UP, max(ret.getOrDefault(Types.DIRECTIONS.UP, 0), bombBlastStrength));
                    }
                }
                else if(myPosition.y == position.y){
                    if(myPosition.x < position.x){ // Bomb is down.
                        ret.put(Types.DIRECTIONS.RIGHT, max(ret.getOrDefault(Types.DIRECTIONS.RIGHT, 0), bombBlastStrength));
                    }
                    else{ // Bomb is up.
                        ret.put(Types.DIRECTIONS.LEFT, max(ret.getOrDefault(Types.DIRECTIONS.LEFT, 0), bombBlastStrength));
                    }
                }
            }
            return ret;
        }

        private ArrayList<Types.DIRECTIONS> getSafeDirections(){
            if(this.safeDirections == null){
                this.safeDirections = computeSafeDirections(this.board, this.myPosition, getDirectionsInRangeOfBomb(),
                        this.bombs, this.enemies);
            }
            return this.safeDirections;
        }

        private Integer getNumberOfSafeDirections(){
            if(this.n_safeDirections == null){
                this.n_safeDirections = getSafeDirections().size();
            }
            return this.n_safeDirections;
        }

        private ArrayList<Types.DIRECTIONS> computeSafeDirections(Types.TILETYPE[][] board, Vector2d myPosition,
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

                nextBoard[myPosition.x][myPosition.y] = Types.TILETYPE.BOMB;

                for (Map.Entry<Types.DIRECTIONS, Integer> entry : unsafeDirections.entrySet()){

                    Types.DIRECTIONS direction = entry.getKey();
                    int bomb_range = entry.getValue();

                    Vector2d nextPosition = myPosition.copy();
                    nextPosition = nextPosition.add(direction.toVec());

                    if(!positionOnBoard(nextBoard, nextPosition) ||
                            !positionIsPassable(nextBoard, nextPosition, enemies))
                        continue;

                    if(!isStuckDirection(nextPosition, bomb_range, nextBoard, enemies)){
                        return new ArrayList<>(Arrays.asList(direction));
                    }
                }
                return safe;
            }

            // The directions that will go off the board.
            Set<Types.DIRECTIONS> disallowed = new HashSet<>();

            Types.DIRECTIONS[] directions = Types.DIRECTIONS.values();

            for (Types.DIRECTIONS current_direction : directions) {

                Vector2d position = myPosition.copy();
                position = position.add(current_direction.toVec());

                Types.DIRECTIONS direction = getDirection(myPosition, position);

                if(!positionOnBoard(board, position)){
                    disallowed.add(direction);
                    continue;
                }

                if(unsafeDirections.containsKey(direction)) continue;

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

        private boolean isStuckDirection(Vector2d nextPosition, int bombRange, Types.TILETYPE[][] nextBoard,
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

                Types.DIRECTIONS[] directions = Types.DIRECTIONS.values();

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

        private int getIsAdjacentEnemy(){
            if(this.isAdjacentEnemy == null){
                this.isAdjacentEnemy = computeIsAdjacentEnemy(this.items, this.dist, this.enemies) ? 1 : 0;
            }
            return this.isAdjacentEnemy;
        }

        private boolean computeIsAdjacentEnemy(
                HashMap<Types.TILETYPE, ArrayList<Vector2d> > items,
                HashMap<Vector2d, Integer> dist,
                ArrayList<GameObject> enemies)
        {
            for(GameObject enemy : enemies){
                if(items.containsKey(enemy.getType())) {
                    ArrayList<Vector2d> items_list = items.get(enemy.getType());
                    for (Vector2d position : items_list) {
                        if (dist.get(position) == 1)
                            return true;
                    }
                }
            }
            return false;
        }

        private int getDistanceToNearestEnemy(){
            if(this.distanceToNearestEnemy == null){
                this.distanceToNearestEnemy = computeDistanceToNearestEnemy(this.items, this.dist, this.enemies);
            }
            return this.distanceToNearestEnemy;
        }

        private int computeDistanceToNearestEnemy(
                HashMap<Types.TILETYPE, ArrayList<Vector2d> > items,
                HashMap<Vector2d, Integer> dist,
                ArrayList<GameObject> enemies)
        {
            int distance = 1000; // TODO: Max distance/Infinity
            for(GameObject enemy : enemies){
                if(items.containsKey(enemy.getType())) {
                    ArrayList<Vector2d> items_list = items.get(enemy.getType());
                    for (Vector2d position : items_list) {
                        if(dist.get(position) < distance)
                            distance = dist.get(position);
                    }
                }
            }
            if(distance > 10)
                distance = 10;
            return distance;
        }

        private int getDistanceToNearestPowerUp(){
            if(this.distanceToNearestPowerUp == null){
                this.distanceToNearestPowerUp = computeDistanceToNearestPowerUp(this.items);
            }
            return this.distanceToNearestPowerUp;
        }

        private int computeDistanceToNearestPowerUp(HashMap<Types.TILETYPE, ArrayList<Vector2d> > items)
        {
            Vector2d previousNode = new Vector2d(-1, -1); // placeholder, these values are not actually used
            int distance = 1000; // TODO: Max distance/Infinity
            for (Map.Entry<Types.TILETYPE, ArrayList<Vector2d>> entry : items.entrySet()) {
                // check pickup entries on the board
                if (entry.getKey().equals(Types.TILETYPE.EXTRABOMB) ||
                        entry.getKey().equals(Types.TILETYPE.KICK) ||
                        entry.getKey().equals(Types.TILETYPE.INCRRANGE)){
                    // no need to store just get closest
                    for (Vector2d coords: entry.getValue()){
                        if (dist.get(coords) < distance){
                            distance = dist.get(coords);
                            previousNode = coords;
                        }
                    }
                }
            }
            if(distance > 10)
                distance = 10;
            return distance;
        }

        /**
         * Dijkstra's pathfinding
         * @param board - game board
         * @param myPosition - the position of agent
         * @param bombs - array of bombs in the game
         * @param enemies - array of enemies in the game
         * @param depth - depth of search (default: 10)
         * @return TODO
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

                    Types.TILETYPE itemType = board[r][c];
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
                        dist.put(position, 100000); // TODO: Inf
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

                    Types.DIRECTIONS[] directionsToBeChecked = Types.DIRECTIONS.values();

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

        // Container for return values of Dijkstra's pathfinding algorithm.
        private class Container {
            HashMap<Types.TILETYPE, ArrayList<Vector2d> > items;
            HashMap<Vector2d, Integer> dist;
            HashMap<Vector2d, Vector2d> prev;
            Container() { }
        }

    }
}
