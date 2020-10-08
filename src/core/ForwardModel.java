package core;

import objects.Avatar;
import objects.Bomb;
import objects.Flame;
import objects.GameObject;
import utils.EventsStatistics;
import utils.LevelGenerator;
import utils.Types;
import utils.Vector2d;

import java.util.*;

import static utils.Types.*;
import static utils.Utils.*;

public class ForwardModel {

    // Board of the game, with all objects distributed in a 2D array of size 'this.size x this.size'
    private Types.TILETYPE[][] board;

    // Blast strength of bombs mapped on board structure
    private int[][] bombBlastStrength;

    // Lives of bombs mapped on board structure
    private int[][] bombLife;

    // Power-ups of the game, hidden. All power-ups are distributed in a 2D array of size 'this.size x this.size'
    private Types.TILETYPE[][] powerups;

    // All agents that are playing the game, and the ones that are alive.
    private GameObject[] agents;  // This never changes dimension, keep as array for efficiency
    private ArrayList<GameObject> aliveAgents;

    // Current flames in the board. They kill!
    private ArrayList<GameObject> flames;

    // Current bombs in the game. They explode!
    private ArrayList<GameObject> bombs;

    // Size of the board.
    private int size;

    // Game mode being played
    private Types.GAME_MODE game_mode;

    // Indicates if this model is the true model of the game. False if it is in a simulation of the agents.
    private boolean trueModel = false;

    // Game tick counter as in GameState, for logging purposes (only valid for true model of the game)
    private int tick;

    // Event statistics
    private EventsStatistics es;
    private boolean[] isAgentStuck;

    /**
     * Creates a forward model object.
     * @param size Size of the board.
     * @param game_mode game mode being played.
     */
    ForwardModel(int size, Types.GAME_MODE game_mode) {
        this.size = size;
        this.game_mode = game_mode;
    }

    /**
     * Optional forward model constructor
     * @param seed Random seed
     * @param size Size of board
     * @param game_mode Mode of game
     */
    ForwardModel(long seed, int size, Types.GAME_MODE game_mode) {
        this.size = size;
        this.game_mode = game_mode;
        init(seed, size, game_mode, null, null);
    }

    /**
     * Optional forward model constructor
     * @param seed Random seed
     * @param intBoard Game board in int representation
     * @param game_mode Mode of game
     */
    ForwardModel(long seed, int[][] intBoard, Types.GAME_MODE game_mode) {
        size = intBoard.length;
        this.game_mode = game_mode;
        init(seed, intBoard.length, game_mode, intBoard, null);
    }

    /**
     * Optional forward model constructor, used to parse JSON observations.
     * @param intBoard Game board in int representation
     * @param bombBlastStrength Bomb blast strength array
     * @param bombLife Bomb life array
     * @param alive Indices of players alive
     * @param game_mode Mode of game
     */
    ForwardModel(int[][] intBoard, int[][] bombBlastStrength, int[][] bombLife, int[] alive, Types.GAME_MODE game_mode, int playerIdx){

        // this is used for communicating with the python client
        this.size = intBoard.length;
        this.game_mode = game_mode;
        this.board = new TILETYPE[size][size];
        init(10, intBoard.length, game_mode, intBoard, alive);
        this.bombBlastStrength = bombBlastStrength;
        this.bombLife = bombLife;

        Vector2d avatarPosition = null;
        int range = -1;

        if (playerIdx >= 0) {
            Avatar avatar = (Avatar) agents[playerIdx];
            avatarPosition = avatar.getPosition();
            range = avatar.getVisionRange();
        }

        if (range != -1) {
            for (int i = 0; i < this.agents.length; i++) {
                GameObject a = this.agents[i];

                if(a.getPosition() == null){
                    a.setPositionNull();
                    a.setDesiredCoordinateNull();
                }
                else if (a.getPosition().custom_dist(avatarPosition) > range) {
                    // This agent's position is not observed
                    a.setPositionNull();
                    a.setDesiredCoordinateNull();
                }

                // If not player observing, reset properties to default
                if (i != playerIdx) {
                    ((Avatar) a).reset();
                }
            }
        }

        HashSet<TILETYPE> agentTypes = Types.TILETYPE.getAgentTypes();
        // Reduce power-ups and board arrays
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Types.TILETYPE type = utils.Types.TILETYPE.values()[intBoard[y][x]];
                if (type == Types.TILETYPE.BOMB){
                    addBomb(x, y, bombBlastStrength[y][x], bombLife[y][x], -1, true);
                } else if (type == Types.TILETYPE.FLAMES){
                    addFlame(x, y, FLAME_LIFE);
                } else if (agentTypes.contains(type)){
                    addAgent(x, y, type.getKey()-10);
                }

                // Check last if there is a bomb that should be added to the bomb array, but not to the board
                if (bombBlastStrength[y][x] > 0 && type != Types.TILETYPE.BOMB) {
                    addBomb(x, y, bombBlastStrength[y][x], bombLife[y][x], -1, false);
                }
            }
        }
    }

    /**
     * Call this method to indicate that the model used is the true model of the game.
     */
    void setTrueModel() {
        trueModel = true;
    }

    /**
     * Executes "saveToTextFile" method of EventsStatistics class, only works for the true model.
     */
    void saveEventsStatistics(String gameIdStr, long seed) {
        if (trueModel && LOGGING_STATISTICS && es != null)
            es.saveToTextFile(gameIdStr, seed);
    }

    /**
     * Initializes the data structures of the game: board, bombs, flames, etc.
     * Adds avatars to the game and sets them alive.
     * Generates the initial board of the game.
     */
    void init(long seed, int size, Types.GAME_MODE gameMode, int[][] intBoard, int[] alive) {
        flames = new ArrayList<>();
        bombs = new ArrayList<>();

        boolean noBoard = false;
        if (intBoard == null) {
            noBoard = true;
            board = new Types.TILETYPE[size][size];
        }

        powerups = new Types.TILETYPE[size][size];

        //boardObs = new int[size][size];
        bombBlastStrength = new int[size][size];
        bombLife = new int[size][size];

        HashSet<Types.TILETYPE> agentTypes = Types.TILETYPE.getAgentTypes();
        agents = new GameObject[agentTypes.size()];
        for (Types.TILETYPE type : agentTypes) {
            agents[type.getKey() - Types.TILETYPE.AGENT0.getKey()] = new Avatar(type.getKey(), gameMode);
        }

        for (GameObject agent : agents){
            ((Avatar)(agent)).setWinner(Types.RESULT.LOSS);
        }

        if (alive == null) {
            // By default everyone is alive
            aliveAgents = new ArrayList<>(Arrays.asList(agents));
        } else {
            aliveAgents = new ArrayList<>();
            for (int agent : alive) {
                ((Avatar) agents[agent - Types.TILETYPE.AGENT0.getKey()]).setWinner(RESULT.INCOMPLETE);
                aliveAgents.add(agents[agent - Types.TILETYPE.AGENT0.getKey()]);
            }
        }

        if (noBoard)
            generateBoard(seed);
        else
            generateBoard(intBoard, seed);

        if(trueModel && LOGGING_STATISTICS){
            tick = 0;
            isAgentStuck = new boolean[]{false, false, false, false};
            es = new EventsStatistics();
        }
    }

    /**
     * Observation getters, package-level access only.
     */
    Types.TILETYPE[][] getBoard() {
        return board;
    }
    int[][] getBombBlastStrength() {
        return bombBlastStrength;
    }
    int[][] getBombLife() {
        return bombLife;
    }
    GameObject[] getAgents() {
        return agents;
    }
    ArrayList<GameObject> getAliveAgents() {
        return aliveAgents;
    }

    /**
     * Creates a copy of this model
     * @return a deep copy of this model
     */
    ForwardModel copy(int playerIdx) {
        ForwardModel copy = new ForwardModel(size, game_mode);
        copy.trueModel = false;  // This is a copy, not the true model
        reduce(copy, playerIdx);
        return copy;
    }

    /**
     * ROLLS the MODEL FORWARD, applying the actions received as parameters,
     * and executing all passive events
     * (i.e. bombs exploding, flames appearing/disappearing).
     * It modifies THIS object to time t+1.
     * @param playerActions player actions to execute in this game state.
     */
    void next(Types.ACTIONS[] playerActions, int gsTick) {
        if (VERBOSE_FM_DEBUG && trueModel) {
            System.out.println();
        }

        // 1. Put actions into effect
        translatePlayerActions(playerActions);

        if (VERBOSE_FM_DEBUG && trueModel) {
            for (GameObject o : aliveAgents) {
                if (!o.getPosition().equals(o.getDesiredCoordinate())) {
                    System.out.println(o.getType() + " desires: " + o.getPosition() + " -> " + o.getDesiredCoordinate());
                }
            }
        }

        // 2. Tick the flames
        ArrayList<GameObject> deadFlames = new ArrayList<>();
        for (GameObject f : flames) {
            f.tick();
            if (f.getLife() == 0) {  // Flame is dead, remove it from the list
                deadFlames.add(f);
            }
        }

        // 3. Agents already have desired positions set from GameState call according to their chosen actions
        // 4. Tick bombs, they set their desired position in the tick() method as well as their life. They also
        for (GameObject b : bombs) {
            b.tick();

            // Wrap around board size, don't let bombs outside of game area, check collisions with walls.
            if (!setDesiredCoordinate(b, b.getDesiredCoordinate(), board))
                ((Bomb)b).setVelocity(new Vector2d());
        }

        // 5. Position swap:
        //      agent <-> agent. Bounce back both.
        //      bomb <-> bomb. Bounce back both.
        //      bomb <-> agent. Bomb only bounce back.
        checkPositionSwap(aliveAgents, aliveAgents, board, false, VERBOSE_FM_DEBUG && trueModel);
        checkPositionSwap(bombs, bombs, board, false, VERBOSE_FM_DEBUG && trueModel);
        checkPositionSwap(aliveAgents, bombs, board, true, VERBOSE_FM_DEBUG && trueModel);

        // 6. If >= 2 agents or >= 2 bombs on same space, bounce both back.
        checkPositionOverlap(aliveAgents, board, VERBOSE_FM_DEBUG && trueModel);
        checkPositionOverlap(bombs, board, VERBOSE_FM_DEBUG && trueModel);

        // 7. Handle kicks & moving bombs hitting agents that can not kick
        handleMovingBombs();

        // 8. Late update bomb overlaps. In previous loop it's possible that some bombs ended up overlapping.
        checkPositionOverlap(bombs, board, VERBOSE_FM_DEBUG && trueModel);

        // If bombs were bounced back, then they may overlap players again, bounce players back too if players moved.
        for (GameObject b: bombs) {
            for (GameObject p : agents) {
                if(p.getDesiredCoordinate() != null && p.getPosition() != null) {
                    if (!p.getDesiredCoordinate().equals(p.getPosition()) &&
                            p.getDesiredCoordinate().equals(b.getDesiredCoordinate())) {
                        // Bounce agent back
                        if (VERBOSE_FM_DEBUG && trueModel) {
                            System.out.println("Reverting " + p.getType() + " overlap bomb late update.");
                        }
                        setDesiredCoordinate(p, p.getPosition(), board);
                    }
                }
            }
            // Update bomb positions to their desired positions
            move(b);
        }

        // 9. Players pick up power-ups
        for (GameObject p: aliveAgents) {
            if(p.getDesiredCoordinate() != null) {
                int x = p.getDesiredCoordinate().x;
                int y = p.getDesiredCoordinate().y;
                pickPowerUp((Avatar) p, x, y);
            }
        }

        // 10. Explode bombs
        HashMap<Vector2d, Integer> flameOccupancy = handleBombExplosions();

        // 11. Resolve flame on death effects
        for (GameObject f : deadFlames) {
            if (f.getPosition() != null) {  // Flame had a physical presence, resolve on death effects
                int x = f.getPosition().x;
                int y = f.getPosition().y;

                // If there is a power-up at that position, add it to the board
                if (powerups[y][x] != null) {
                    board[y][x] = powerups[y][x];
                    powerups[y][x] = null;
                    // If no power-up, add a passage to the board
                } else {
                    board[y][x] = Types.TILETYPE.PASSAGE;
                }
            }
        }
        flames.removeAll(deadFlames);

        // 12. Add flames left alive back into the board if missing. Multiple flames may share a position, and the board
        // Should contain a flame until all flames are dead.
        for (GameObject f : flames) {
            int x = f.getDesiredCoordinate().x;
            int y = f.getDesiredCoordinate().y;
            if (board[y][x] != Types.TILETYPE.FLAMES) {
                f.setPosition(f.getDesiredCoordinate());
                board[y][x] = f.getType();
            }
        }

        // 13. Kill agents on flames. Otherwise, update position on board.
        ArrayList<GameObject> deadAgentsThisTick = handleAgentKilling(flameOccupancy);

        // 14. Check for terminated agents
        if(deadAgentsThisTick.size() > 0) {
            Types.getGameConfig().processDeadAgents(agents, aliveAgents, deadAgentsThisTick, game_mode);
        }

        // 15. Update observable board grids of item types, bomb blast strengths, bomb lives
        bombBlastStrength = new int[size][size];
        bombLife = new int[size][size];

        for(GameObject bombObject : bombs){
            Bomb bomb = (Bomb) bombObject;
            Vector2d position = bomb.getPosition();
            bombBlastStrength[position.y][position.x] = bomb.getBlastStrength();
            bombLife[position.y][position.x] = bomb.getLife();
        }

        // 16. Collapse
        if(Types.COLLAPSE_BOARD) {
            if (gsTick >= COLLAPSE_START && (gsTick - COLLAPSE_START) % COLLAPSE_STEP == 0) {

                int collapse_stage = (gsTick - COLLAPSE_START) / COLLAPSE_STEP; // 0, 1, 2, ...

                int ring_min = collapse_stage;
                int ring_max = size - collapse_stage - 1;

                ArrayList<GameObject> collapsedAgents = new ArrayList<>();

                for (int x = ring_min; x <= ring_max; x++) {
                    if (x == ring_min || x == ring_max) {
                        for (int y = ring_min + 1; y <= ring_max - 1; y++) {
                            collapseTile(x, y, collapsedAgents);
                        }
                    }
                    collapseTile(x, ring_min, collapsedAgents);
                    collapseTile(x, ring_max, collapsedAgents);
                }

                // Kill agents.
                if (collapsedAgents.size() > 0)
                    Types.getGameConfig().processDeadAgents(agents, aliveAgents, collapsedAgents, game_mode);
            }
        }

        // 17. Logging
        if(trueModel && LOGGING_STATISTICS) {
            for (GameObject p : aliveAgents) {
                int agentID = p.getType().getKey() - 10;
                boolean isStuck = isStuckAdvanced(board, bombs, ((Avatar) p)); //isStuck(board, ((Avatar) p));
                /*
                if (!isAgentStuck[agentID] && isStuck){
                    String eventString = tick + " | [" + agentID + "] got stuck at ("
                            + p.getPosition().x + ", " + p.getPosition().y + ")\n";
                    es.events.add(eventString);
                }
                */
                isAgentStuck[agentID] = isStuck;
            }
            tick++;
        }
    }

    private void collapseTile(int x, int y, ArrayList<GameObject> collapsedAgents){
        //System.out.println("Collapsing "+x+" "+y);

        Types.TILETYPE tiletype = board[y][x];

        if(tiletype == Types.TILETYPE.BOMB){
            Vector2d pos = new Vector2d(x, y);
            ArrayList<GameObject> gos = findObjectInList(pos, bombs);
            for (GameObject go: gos) {
                bombs.remove(go);

                int pIdx = ((Bomb) go).getPlayerIdx();
                if (pIdx >= 0) {
                    ((Avatar)agents[pIdx]).addAmmo();
                }
            }
            bombLife[y][x] = 0;
            bombBlastStrength[y][x] = 0;
        }
        else if(tiletype == Types.TILETYPE.FLAMES){
            Vector2d pos = new Vector2d(x, y);
            ArrayList<GameObject> gos = findObjectInList(pos, flames);
            for (GameObject go: gos) {
                flames.remove(go);
            }
        }
        else if(tiletype == Types.TILETYPE.AGENT0 ||
                tiletype == Types.TILETYPE.AGENT1 ||
                tiletype == Types.TILETYPE.AGENT2 ||
                tiletype == Types.TILETYPE.AGENT3){

            Vector2d pos = new Vector2d(x, y);
            ArrayList<GameObject> gos = findObjectInList(pos, aliveAgents);
            for (GameObject go: gos) {
                collapsedAgents.add(go);
            }
        }
        board[y][x] = Types.TILETYPE.RIGID;
    }

    /**
     * Handles the movement of bombs, including kicking them if the agent can do so.
     */
    private void handleMovingBombs()
    {
        for (GameObject b: bombs) {
            for (GameObject p: aliveAgents) {

                if(p.getDesiredCoordinate() != null && p.getPosition() != null){


                    if (b.getDesiredCoordinate().equals(b.getPosition())) {
                        ((Bomb) b).setVelocity(new Vector2d());
                    }
                    if (p.getDesiredCoordinate().equals(b.getDesiredCoordinate())) {
                        // Agent tried to move onto bomb OR bomb tried to move onto agent, check if agent can kick
                        if (((Avatar) p).canKick()) {
                            // Player can kick, so set bomb velocity
                            Vector2d velocity = p.getDesiredCoordinate().subtract(p.getPosition());
                            ((Bomb) b).setVelocity(velocity);

                            // First bomb move on the same tick as the kick happened. Do not move into players or walls.
                            // If bomb couldn't move, reset its velocity
                            ArrayList<Types.TILETYPE> collisions = new ArrayList<>();
                            collisions.add(Types.TILETYPE.RIGID);
                            collisions.add(Types.TILETYPE.WOOD);
                            collisions.addAll(Types.TILETYPE.getAgentTypes());

                            if (velocity.mag() == 0) {
                                // They can be on same position only if agent just dropped bomb
                                // Move agent back if they moved & the bomb didn't move when the kick was attempted
                                if (!p.getDesiredCoordinate().equals(p.getPosition())) {
                                    if (VERBOSE_FM_DEBUG && trueModel) {
                                        System.out.println("Reverting " + p.getType() + " bomb overlap " + b.getDesiredCoordinate());
                                    }
                                    setDesiredCoordinate(p, p.getPosition(), board);
                                }
                            } else {
                                if (!setDesiredCoordinate(b, b.getDesiredCoordinate().add(velocity), board, collisions)) {
                                    ((Bomb) b).setVelocity(new Vector2d());
                                }
                            }
                        } else {
                            // Move both back
                            if (!p.getDesiredCoordinate().equals(p.getPosition())) {
                                if (VERBOSE_FM_DEBUG && trueModel) {
                                    System.out.println("Reverting " + p.getType() +
                                            " trying to overlap bomb, bomb revert too: " + p.getDesiredCoordinate() + " <> " +
                                            b.getDesiredCoordinate());
                                }
                                setDesiredCoordinate(p, p.getPosition(), board);
                            }
                            if (!b.getDesiredCoordinate().equals(b.getPosition())) {
                                setDesiredCoordinate(b, b.getPosition(), board);
                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * Handles bomb explosions, creating the flame objects that destroy things.
     * @return the set of positions occupied by flames.
     */
    private HashMap<Vector2d, Integer> handleBombExplosions()
    {
        boolean newExplosions = true;

        // Get positions of flames
        HashMap<Vector2d, Integer> flameOccupancy = checkOccupancy(flames);

        while (newExplosions) {
            // Use this flag to chain explosions. If new flames are added, then we need to check all bombs again
            newExplosions = false;

            ArrayList<GameObject> deadBombs = new ArrayList<>();
            for (GameObject b : bombs) {

                // Force this bomb to explode if there is a flame at this position.
                boolean forceExplosion = false;
                if (flameOccupancy.get(b.getPosition()) != null) forceExplosion = true;

                // Find the flame owners who triggered the explosion
                if(trueModel && LOGGING_STATISTICS) {
                    if (forceExplosion) {
                        StringBuilder eventSB = new StringBuilder();
                        eventSB.append(tick + " | [" + ((Bomb) b).getPlayerIdx() + "]'s bomb exploded at ("
                                + b.getPosition().x + ", " + b.getPosition().y + ") triggered by ");
                        Set<Integer> killerIDs = new HashSet<>();
                        for (GameObject flame : this.flames) {
                            if (flame.getPosition().equals(b.getPosition()))
                                killerIDs.add(((Flame) flame).playerIdx);
                        }
                        for (Integer id : killerIDs) {
                            eventSB.append("[" + id + "]");
                            es.bombsTriggered[id]++;
                        }
                        eventSB.append("\n");
                        es.events.add(eventSB.toString());
                    }
                    else if(b.getLife() == 0){
                        String eventString = tick + " | [" + ((Bomb) b).getPlayerIdx() + "]'s bomb exploded at ("
                                + b.getPosition().x + ", " + b.getPosition().y + ")\n";
                        es.events.add(eventString);
                    }
                }

                // TODO: Wood removals happen here, but within Bomb class, what's the best way of doing this? (to count them)

                // This bomb will explode and create new flames if life reached 0, or forced to explode
                ArrayList<GameObject> newFlames = ((Bomb) b).explode(forceExplosion, board, powerups);
                if (newFlames != null && newFlames.size() > 0) {

                    flames.addAll(newFlames);
                    newExplosions = true;

                    // Remove this bomb from the list of bombs
                    deadBombs.add(b);

                    // Give the player 1 ammo back for this bomb
                    int pIdx = ((Bomb) b).getPlayerIdx();
                    if (pIdx >= 0) {
                        ((Avatar)agents[pIdx]).addAmmo();
                    }

                    // Add new flame positions to the map
                    HashMap<Vector2d, Integer> newOccupancy = checkOccupancy(newFlames);
                    for (Map.Entry<Vector2d, Integer> e : newOccupancy.entrySet()) {
                        flameOccupancy.merge(e.getKey(), e.getValue(), (a, b1) -> b1 + a);
                    }
                }
            }
            bombs.removeAll(deadBombs);
        }

        return flameOccupancy;
    }

    /**
     * Handles killing agents with flames in the board.
     * @param flameOccupancy location of the flames on this tick.
     * @return list of agents killed on this tick
     */
    private ArrayList<GameObject> handleAgentKilling(HashMap<Vector2d, Integer> flameOccupancy)
    {
        ArrayList<GameObject> deadAgentsThisTick = new ArrayList<>();
        for (GameObject p : aliveAgents) {
            Vector2d nextPos = p.getDesiredCoordinate();
            Vector2d currPos = p.getPosition();

            if (nextPos != null && currPos != null && flameOccupancy.containsKey(nextPos)) {
                // This agent was killed by a flame, remove from list
                p.setLife(0);
                deadAgentsThisTick.add(p);

                if(trueModel && LOGGING_STATISTICS) {
                    StringBuilder eventSB = new StringBuilder();
                    eventSB.append(tick + " | [" + (((Avatar) p).getPlayerID() - 10) + "] died at ("
                            + nextPos.x + ", " + nextPos.y + ") by ");

                    Set<Integer> killerIDs = new HashSet<>();
                    for (GameObject flame : this.flames) {
                        if (flame.getPosition().equals(nextPos))
                            killerIDs.add(((Flame) flame).playerIdx);
                    }
                    for (Integer id : killerIDs) {
                        eventSB.append("[" + id + "]");
                    }
                    eventSB.append("'s flame(s)");

                    if (isAgentStuck[((Avatar) p).getPlayerID() - 10]) {
                        eventSB.append(" (was stuck)");
                    }
                    eventSB.append("\n");

                    es.events.add(eventSB.toString());
                }

                if (VERBOSE_FM_DEBUG) {
                    System.out.println("Agent " + ((Avatar) p).getPlayerID() + " died.");
                }

                if (board[currPos.y][currPos.x] != Types.TILETYPE.BOMB
                        && board[currPos.y][currPos.x] != Types.TILETYPE.FLAMES) {
                    board[currPos.y][currPos.x] = Types.TILETYPE.PASSAGE;
                }
            } else {
                move(p);
            }
        }
        return deadAgentsThisTick;
    }

    /**
     * Move object function. Objects don't move through walls (rigid or wood).
     * @param o - object to move from its current position to its desired position.
     */
    private void move(GameObject o) {
        Vector2d currentPos = o.getPosition();
        Vector2d nextPos = o.getDesiredCoordinate();

        if(currentPos != null && nextPos != null) {

            //System.out.println(o.getType());
            //System.out.println("cp: "+currentPos+" | np: "+nextPos);

            if (!(currentPos.equals(nextPos))) { // !(currentPos.equals(nextPos))  //(currentPos != null && !(currentPos.equals(nextPos)))
                Types.TILETYPE nextType = board[nextPos.y][nextPos.x];

                if (board[nextPos.y][nextPos.x] != Types.TILETYPE.RIGID &&
                        board[nextPos.y][nextPos.x] != Types.TILETYPE.WOOD) {
                    if (trueModel && VERBOSE_FM_DEBUG) {
                        System.out.println("Moving " + o.getType() + ": " + currentPos + " -> " + nextPos);
                    }
                    o.setPosition(nextPos.copy());

                    HashSet<Types.TILETYPE> powerUpTypes = Types.TILETYPE.getPowerUpTypes();
                    HashSet<Types.TILETYPE> agentTypes = TILETYPE.getAgentTypes();

                    // Set up sprites that cannot be replaced with a passage when current sprite moves from its square.
                    HashSet<Types.TILETYPE> illegalOverwriteTypes = new HashSet<>(powerUpTypes);
                    illegalOverwriteTypes.add(Types.TILETYPE.FLAMES);  // We don't remove flames
                    illegalOverwriteTypes.addAll(agentTypes);  // We don't remove other agents...
                    illegalOverwriteTypes.remove(o.getType()); // Unless this agent is the current object

                    // Bombs don't leave traces of bombs behind them, and other sprites do not remove bombs from the board
                    if (o.getType() != Types.TILETYPE.BOMB) {
                        illegalOverwriteTypes.add(Types.TILETYPE.BOMB);
                    } else {
                        // Check if next is a powerup, we should put it back in the powerup array before removing it from
                        // the board (unless it's an avatar collecting it).
                        if (!agentTypes.contains(o.getType()) && powerUpTypes.contains(nextType)) {
                            powerups[nextPos.y][nextPos.x] = board[nextPos.y][nextPos.x];
                        }
                    }

                    // Update current position
                    // Only update current position if the object there can be overwritten
                    // Replace with passage if there isn't a power-up there that should be added back in
                    if (canOverwrite(currentPos, board, illegalOverwriteTypes)) {
                        if (powerups[currentPos.y][currentPos.x] != null) {
                            board[currentPos.y][currentPos.x] = powerups[currentPos.y][currentPos.x];
                            powerups[currentPos.y][currentPos.x] = null;
                        } else {
                            board[currentPos.y][currentPos.x] = Types.TILETYPE.PASSAGE;
                        }
                    }
                }
            }

            // Update next position. The order is bombs, avatars, so avatars would overwrite bombs.
            board[nextPos.y][nextPos.x] = o.getType();
        }
    }

    /**
     * Function to insert player action effects into the game.
     * Index in actions array is the same as in aliveAgents array.
     * @param actions - array of actions, 1 for each player still alive
     */
    private void translatePlayerActions(Types.ACTIONS[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Avatar agent = (Avatar) agents[i];
            if (agent.getWinner() != Types.RESULT.INCOMPLETE) {
                continue;
            }
            if (agent.getPosition() == null){
                //System.out.println("Agent has no position");
                continue;
            }

            Vector2d pos = agent.getPosition();
            Types.ACTIONS action = actions[i];

            if (action == null)
            {
                System.out.println("WARNING: " + agent.getType() + " sent an action NULL.");
                action = Types.ACTIONS.ACTION_STOP;
            }

            boolean successful = setDesiredCoordinate(agent, pos.add(action.getDirection().toVec()), board);

            if (action == Types.ACTIONS.ACTION_BOMB) {
                if (agent.getAmmo() > 0 && bombBlastStrength[pos.y][pos.x] == 0) {
                    // Check if a bomb is not already there
                    agent.reduceAmmo();
                    addBomb(pos.x, pos.y, agent.getBlastStrength(), BOMB_LIFE, i, true);
                    successful = true;
                    if(trueModel && LOGGING_STATISTICS) {
                        int agentID = (agent.getPlayerID() - 10);
                        String eventString = tick + " | [" + agentID + "] placed a bomb at ("
                                +  pos.x + ", " + pos.y + ")\n";
                        es.events.add(eventString);
                        es.bombsPlaced[agentID]++;
                        es.bombPlacementsAttempted[agentID]++;
                    }
                } else {
                    successful = false;
                    if(trueModel && LOGGING_STATISTICS) {
                        int agentID = (agent.getPlayerID() - 10);
                        String eventString = tick + " | [" + agentID + "] failed to place a bomb at ("
                                +  pos.x + ", " + pos.y + ")\n";
                        es.events.add(eventString);
                        es.bombPlacementsAttempted[agentID]++;
                    }
                }
            }

            if (successful && action != Types.ACTIONS.ACTION_STOP && trueModel) {
                if (VERBOSE_FM_DEBUG) {
                    System.out.println(agent.getType() + " playing action " + action + " " + action.getDirection()
                            + ": " + agent.getPosition() + " -> " + agent.getDesiredCoordinate());
                }
            }
        }
    }

    /**
     * Method for a player to pick up a power-up.
     * @param p - player involved in the interaction.
     * @param x - x position of the power-up
     * @param y - y position of the power-up
     */
    private void pickPowerUp(Avatar p, int x, int y) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            if (board[y][x] == Types.TILETYPE.EXTRABOMB) {
                p.addAmmo();
                if(trueModel && LOGGING_STATISTICS) {
                    String eventString = tick + " | [" + (p.getPlayerID() - 10) + "] picked up AMMO at ("
                            +  x + ", " + y + ")\n";
                    es.events.add(eventString);
                    es.powerUpsTaken[p.getPlayerID() - 10]++;
                }
            } else if (board[y][x] == Types.TILETYPE.INCRRANGE) {
                p.addBlastStrength();
                if(trueModel && LOGGING_STATISTICS) {
                    String eventString = tick + " | [" + (p.getPlayerID() - 10) + "] picked up BLAST STRENGTH at ("
                            +  x + ", " + y + ")\n";
                    es.events.add(eventString);
                    es.powerUpsTaken[p.getPlayerID() - 10]++;
                }
            } else if (board[y][x] == Types.TILETYPE.KICK) {
                p.setCanKick();
                if(trueModel && LOGGING_STATISTICS) {
                    String eventString = tick + " | [" + (p.getPlayerID() - 10) + "] picked up CAN KICK at ("
                            +  x + ", " + y + ")\n";
                    es.events.add(eventString);
                    es.powerUpsTaken[p.getPlayerID() - 10]++;
                }
            }
        }
    }

    /**
     * Generates the game board, of size 'this.size' and using the seed 'this.seed'.
     * It uses Types.BOARD_* to decide number of rigid blocks, wood, items, etc.
     */
    private void generateBoard(long seed) {
        int[][] intBoard = LevelGenerator.makeBoard(seed, size, Types.BOARD_NUM_RIGID, Types.BOARD_NUM_WOOD, agents);
        generateBoard(intBoard, seed);
    }

    /**
     * Generates the game board given an intBoard that will be translated.
     */
    private void generateBoard(int[][] intBoard, long seed) {
        int[][] intPowerups = LevelGenerator.makeItems(intBoard, Types.BOARD_NUM_ITEMS, seed);
        translate(intBoard, true);
        translate(intPowerups, false);
    }


    /**
     * Take a board of integers and turn them into a board of game objects.
     * @param intBoard - board in integers
     * @param updateBoard - if board should be updated or not. If not, only powerups are updated from array passed.
     */
    private void translate(int[][] intBoard, boolean updateBoard){
        if (updateBoard) {
            board = new Types.TILETYPE[size][];

            for (int i = 0; i < intBoard.length; i++) {
                board[i] = new Types.TILETYPE[size];

                for (int j = 0; j < intBoard[i].length; j++) {
                    Types.TILETYPE type = utils.Types.TILETYPE.values()[intBoard[i][j]];
                    if (type == Types.TILETYPE.BOMB) {
                        addBomb(j, i, DEFAULT_BOMB_BLAST, BOMB_LIFE, -1, true);
                    } else if (type == Types.TILETYPE.FLAMES) {
                        addFlame(j, i, FLAME_LIFE);
                    } else if (Types.TILETYPE.getAgentTypes().contains(type)) {
                        int idx = type.getKey() - 10;
                        addAgent(j, i, idx);
                    } else if (Types.TILETYPE.getPowerUpTypes().contains(type)) {
                        addPowerUp(j, i, type, true);
                    } else {
                        // All other objects are simply added: walls, passage, fog
                        addObject(j, i, type);
                    }
                }
            }
        } else {
            powerups = new Types.TILETYPE[intBoard.length][];
            for (int i = 0; i < intBoard.length; i++) {
                powerups[i] = new Types.TILETYPE[size];
                for (int j = 0; j < intBoard[i].length; j++) {
                    Types.TILETYPE type = utils.Types.TILETYPE.values()[intBoard[i][j]];
                    addPowerUp(j, i, type, false);
                }
            }
        }
    }

    // add* methods can be used by agents to insert things into the model

    void addBomb(int x, int y, int blastStrength, int bombLife, int playerIdx, boolean addToBoard) {
        Bomb bomb = new Bomb(blastStrength, bombLife, playerIdx);
        bomb.setPosition(new Vector2d(x, y));
        setDesiredCoordinate(bomb, new Vector2d(x, y), board);
        bombs.add(bomb);
        if (addToBoard) {
            board[y][x] = Types.TILETYPE.BOMB;
        }
    }

    void addFlame(int x, int y, int life) {
        Flame flame = new Flame();
        flame.setLife(life);
        setDesiredCoordinate(flame, new Vector2d(x, y), board);
        flame.setPosition(flame.getDesiredCoordinate());
        flames.add(flame);
        board[y][x] = Types.TILETYPE.FLAMES;
    }

    void addPowerUp(int x, int y, Types.TILETYPE type, boolean visible) {
        Types.TILETYPE[][] targetArray;
        if (visible) targetArray = board;
        else targetArray = powerups;

        if (type == Types.TILETYPE.EXTRABOMB || type == Types.TILETYPE.INCRRANGE || type == Types.TILETYPE.KICK) {
            addObject(x, y, type, targetArray);
        }
    }

    void addObject(int x, int y, Types.TILETYPE type) {
        addObject(x, y, type, board);
    }

    void addAgent(int x, int y, int idx) {
        GameObject agent = agents[idx];
        ((Avatar)agent).setWinner(Types.RESULT.INCOMPLETE);
        agent.setPosition(new Vector2d(x, y));
        setDesiredCoordinate(agent, new Vector2d(x, y), board);
        board[y][x] = agent.getType();
    }

    void removePowerUp(int x, int y, Types.TILETYPE type) {
        removeObject(x, y, type, powerups, false);
    }

    void removeObject(int x, int y, Types.TILETYPE type, boolean onlyBoard) {
        removeObject(x, y, type, board, onlyBoard);
    }

    private void addObject(int x, int y, Types.TILETYPE type, Types.TILETYPE[][] targetArray) {
        GameObject object = new GameObject(type);
        object.setPosition(new Vector2d(x, y));
        setDesiredCoordinate(object, new Vector2d(x, y), targetArray);
        targetArray[y][x] = type;
    }

    private void removeObject(int x, int y, Types.TILETYPE type, Types.TILETYPE[][] targetArray, boolean onlyBoard) {
        Vector2d pos = new Vector2d(x, y);
        targetArray[y][x] = TILETYPE.PASSAGE;

        if (!onlyBoard) {
            if (type == TILETYPE.BOMB) {
                ArrayList<GameObject> gos = findObjectInList(pos, bombs);
                for (GameObject go: gos) {
                    bombs.remove(go);
                }
                bombLife[y][x] = 0;
                bombBlastStrength[y][x] = 0;
            } else if (type == TILETYPE.FLAMES) {
                ArrayList<GameObject> gos = findObjectInList(pos, flames);
                for (GameObject go: gos) {
                    flames.remove(go);
                }
            } else if (TILETYPE.getAgentTypes().contains(type)) {
                GameObject ob = agents[type.getKey() - 10];
                ((Avatar)ob).setWinner(RESULT.LOSS);
                aliveAgents.remove(ob);
            } else if (TILETYPE.getPowerUpTypes().contains(type)) {
                powerups[y][x] = null;
            }
        }
    }

    // Sets properties of agent, identified by player ID
    void setAgent(int playerIdx, int x, int y, boolean canKick, int ammo, int blastStrength) {
        Avatar a = (Avatar)agents[playerIdx];
        a.setPosition(new Vector2d(x, y));
        if (canKick) a.setCanKick();
        a.setAmmo(ammo);
        a.setBlastStrength(blastStrength);
    }

    // Sets properties of bomb, identified by position
    void setBomb(int x, int y, int playerIdx, Vector2d velocity) {
        Vector2d pos = new Vector2d(x, y);
        ArrayList<GameObject> gos = findObjectInList(pos, bombs);
        for (GameObject go: gos) {
            Bomb bomb = (Bomb)go;
            bomb.setPlayerIdx(playerIdx);
            bomb.setVelocity(velocity);
        }
    }

    // Sets properties of flame, identified by position
    void setFlame(int x, int y, int life) {
        Vector2d pos = new Vector2d(x, y);
        ArrayList<GameObject> flame = findObjectInList(pos, flames);
        for (GameObject f : flame) {
            f.setLife(life);
        }
    }

    /**
     * Construct a completely empty board
     */
    private void emptyBoard(){
        board = new Types.TILETYPE[size][];

        // Add empty passages everywhere
        for (int i = 0; i < board.length; i++) {
            board[i] = new Types.TILETYPE[size];
            for (int i1 = 0; i1 < board[i].length; i1++) {
                board[i][i1] = Types.TILETYPE.PASSAGE;
            }
        }

        // Add players in the corners
        addAgent(1, 1, 0);
        addAgent(board.length - 2, 1, 1);
        addAgent(1, board[1].length - 2, 2);
        addAgent(board.length - 2, board[1].length - 2, 3);
    }


    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < board.length+2; i++) {
            stringBuilder.append("*");
        }
        stringBuilder.append("\n");
        for (Types.TILETYPE[] gameObjects : board) {
            stringBuilder.append("*");
            for (Types.TILETYPE type : gameObjects) {
                if (type.getKey() < Types.TILETYPE.AGENT0.getKey() && type.getKey() > 0)
                    stringBuilder.append(type.getKey());
                else {
                    if (type == Types.TILETYPE.PASSAGE)
                        stringBuilder.append(" ");
                    else if (type == Types.TILETYPE.AGENT0)
                        stringBuilder.append("a");
                    else if (type == Types.TILETYPE.AGENT1)
                        stringBuilder.append("b");
                    else if (type == Types.TILETYPE.AGENT2)
                        stringBuilder.append("c");
                    else if (type == Types.TILETYPE.AGENT3)
                        stringBuilder.append("d");
                    else
                        stringBuilder.append("-");
                }
            }

            stringBuilder.append("*\n");
        }
        for (int i = 0; i < board.length + 2; i++) {
            stringBuilder.append("*");
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    /**
     * Method to reduce the current model of the game to the vision range of the player.
     * Any objects outside the vision range would show up as FOG type in the board, and removed from the lists
     * in the engine.
     * @param copy - copy of the forward model that should be reduced.
     * @param playerIdx - index of the player; the position of this player is used as reference for the range check.
     *                  May be -1, which means all object should be included in the copy (no reducing)
     */
    private void reduce(ForwardModel copy, int playerIdx) {
        Vector2d avatarPosition = null;
        int range = -1;

        if (playerIdx >= 0) {
            Avatar avatar = (Avatar) agents[playerIdx];
            avatarPosition = avatar.getPosition();
            range = avatar.getVisionRange();
        }

        // Init new power-up and board arrays
        copy.powerups = new Types.TILETYPE[size][size];
        copy.board = new Types.TILETYPE[size][size];

        // Init new flames and bomb arrays
        copy.flames = new ArrayList<>();
        copy.bombs = new ArrayList<>();

        // Agents position is removed and their properties reset if we don't know where they are when reducing state.
        copy.agents = deepCopy(agents);
        if (range != -1) {
            for (int i = 0; i < copy.agents.length; i++) {
                GameObject a = copy.agents[i];
                if (a.getPosition() != null && a.getPosition().custom_dist(avatarPosition) > range) {
                    // This agent's position is not observed
                    a.setPositionNull();
                    a.setDesiredCoordinateNull();
                }
                // If not player observing, reset properties to default
                if (i != playerIdx) {
                    ((Avatar) a).reset();
                }
            }
        }

        // Reduce power-ups and board arrays
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (range == -1 || avatarPosition != null && avatarPosition.custom_dist(x, y) <= range) {
                    copy.board[y][x] = board[y][x];
                    if (range == -1)
                        copy.powerups[y][x] = powerups[y][x];
                } else {
                    copy.board[y][x] = Types.TILETYPE.FOG;
                }
            }
        }

        // Reduce arraylists of flames and bombs
        // Reset flames life if playerIdx > -1, players don't know this information
        _reduceHiddenList(flames, copy.flames, avatarPosition, range);
        _reduceHiddenList(bombs, copy.bombs, avatarPosition, range);
        copy.aliveAgents = findAliveAgents(copy.agents);

        // Finally construct the main components of observations
        copy.bombBlastStrength = new int[size][size];
        copy.bombLife = new int[size][size];

        for(GameObject bombObject : copy.bombs){
            Bomb bomb = (Bomb) bombObject;
            Vector2d position = bomb.getPosition();
            copy.bombBlastStrength[position.y][position.x] = bomb.getBlastStrength();
            copy.bombLife[position.y][position.x] = bomb.getLife();
        }
    }

    @Override
    public boolean equals(Object o){
        if (o.getClass() != getClass()){
            return false;
        }
        ForwardModel fm = (ForwardModel)o;

        if (size != fm.size)
            return false;
        if (!Types.TILETYPE.boardEquals(powerups, fm.powerups))
            return false;
        if (!Types.TILETYPE.boardEquals(board, fm.board))
            return false;
        if (!Arrays.deepEquals(bombBlastStrength, fm.bombBlastStrength))
            return false;
        if (!Arrays.deepEquals(bombLife, fm.bombLife))
            return false;
        if (!GameObject.listEquals(flames, fm.flames))
            return false;
        if (!GameObject.listEquals(bombs, fm.bombs))
            return false;
        if (!GameObject.arrayEquals(agents, fm.agents))
            return false;
        if (!GameObject.listEquals(aliveAgents, fm.aliveAgents))
            return false;
        return true;
    }
}
