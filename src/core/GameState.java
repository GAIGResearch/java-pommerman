package core;

import com.google.gson.*;
import objects.Avatar;
import objects.GameObject;
import utils.Types;
import utils.Vector2d;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static utils.Types.*;

@SuppressWarnings("unused")
public class GameState {

    // Number of actions available to agents - invariable
    private int nActions = NUM_ACTIONS;

    // Forward model for the game.
    ForwardModel model;

    // Message
    private int[][] message;

    // Seed for the game state.
    private long seed;

    // Size of the board.
    private int size;

    // Avatar object, of the player that is in control of this GameState
    private Avatar avatar;

    // Index that is in control of the player in this GameState.
    private int playerIdx = -1;

    // Current tick of the game.
    private int tick = 0;

    // Game mode being played
    Types.GAME_MODE gameMode;

    /**
     * Constructor, first thing to call. Creates a GameState object with some information.
     * @param seed - random seed to be used in generating the board.
     * @param size - size of the board.
     * @param gameMode - game mode being played.
     * @param newFM - indicates if a new ForwardModel should be created at this point or not
     */
    protected GameState(long seed, int size, Types.GAME_MODE gameMode, boolean newFM) {
        this.seed = seed;
        this.size = size;
        this.gameMode = gameMode;

        if (newFM) {
            model = new ForwardModel(size, gameMode);
        }
        if (gameMode.equals(Types.GAME_MODE.TEAM_RADIO)){
            this.message = new int[NUM_PLAYERS][MESSAGE_LENGTH];
        }
    }

    /**
     * Optional game state constructor.
     * @param seed - random seed for board generation
     * @param size - ize of the board
     * @param gameMode - game mode being played
     */
    public GameState(long seed, int size, Types.GAME_MODE gameMode) {
        this.seed = seed;
        this.size = size;
        this.gameMode = gameMode;
        model = new ForwardModel(seed,size,gameMode);
        if (gameMode.equals(Types.GAME_MODE.TEAM_RADIO)){
            this.message = new int[NUM_PLAYERS][MESSAGE_LENGTH];
        }
    }

    /**
     * Constructor which creates a new GameState object.
     * Provides a forward model directly.
     * @param seed - random seed to be used in generating the board.
     * @param model - a ForwardModel for this GameState
     * @param gameMode - game mode being played.
     */
    protected GameState(long seed, ForwardModel model, Types.GAME_MODE gameMode) {
        this(seed, model.getBoard().length, gameMode, false);
        this.model = model;
        if (gameMode.equals(Types.GAME_MODE.TEAM_RADIO)){
            this.message = new int[NUM_PLAYERS][MESSAGE_LENGTH];
        }
    }

    /**
     * Initializes the ForwardModel in the GameState. If the model is null, it creates a new one.
     * Board is only generated when this initialization method is called.
     */
    void init() {
        if (model == null) {
            model = new ForwardModel(size, gameMode);
        }
        this.model.init(seed, size, gameMode, null, null);
    }

    /**
     * Gets the agents of the game
     * @return the agents of the game
     */
    GameObject[] getAgents() {
        return model.getAgents();
    }

    /**
     * Gets the alive agents of the game
     * @return the alive agents of the game
     */
    ArrayList<GameObject> getAliveAgents() {
        return model.getAliveAgents();
    }

    /**
     * For debug purposes only: shows the current state of winners for all players.
     */
    private void computeResults()
    {
        Types.RESULT[] results = new Types.RESULT[Types.NUM_PLAYERS];
        GameObject[] agents = getAgents();

        for (int i = 0; i < Types.NUM_PLAYERS; i++) {
            Avatar av = (Avatar) agents[i];
            results[i] = av.getWinner();
        }

        System.out.println("Results at time : " + tick + ": " + Arrays.toString(results));
    }

    /**
     * Creates a deep copy of this game state, given player index. Sets up the game state so that it contains
     * only information available to the given player. If -1, state contains all information.
     * When making a copy of a copy of an already assigned state (which receives a -1 playerIdx), the first copy's
     * player Idx is retained, while the model is not further reduced.
     * @return a copy of this state
     */
    GameState copy(int playerIdx) {
        // Determine this copy's player idx. If either received playerIdx or this.playerIdx is >= 0, keep that one.
        // Otherwise, keep original playerIdx
        int copyIdx = this.playerIdx;
        if (playerIdx != -1) {
            copyIdx = playerIdx;
        }

        GameState copy = new GameState(seed, size, gameMode, false);
        copy.model = model.copy(playerIdx);  // Use given playerIdx to reduce state (-1 in copies of copies)
        copy.tick = tick;

        // Use this copy's player idx as determined earlier to update copy playerIdx and its assigned avatar.
        copy.playerIdx = copyIdx;
        if (copyIdx >= 0) {
            copy.avatar = (Avatar) copy.model.getAgents()[copyIdx];
            if (gameMode.equals(GAME_MODE.FFA) && message != null)
                copy.message = message.clone();
        } else {
            copy.avatar = null;
        }
        return copy;
    }

    /**
     * @return the random seed of this state
     */
    long getSeed() {
        return seed;
    }

    /* ----- Agents have access to all following publicly available methods ----- */

    /**
     * Advances the game state applying all actions received and increments the tick counter.
     * @param actions actions to be executed in the current game state.
     * @return true if the game could be advanced. False if it couldn't because ticks reached the game ticks limit.
     */
    public boolean next(Types.ACTIONS[] actions) {

        if (tick < Types.MAX_GAME_TICKS)
        {
            model.next(actions, tick);
            tick++;
            if (tick == Types.MAX_GAME_TICKS)
                Types.getGameConfig().processTimeout(gameMode, getAgents(), getAliveAgents());

            return true;
        }

        return false;
    }
    /**
     * @return a copy of the current game state.
     */
    public GameState copy() {
        return copy(-1);  // No reduction happening if no index specified
    }

    /** GETTERS AND SETTERES **/


    public Types.TILETYPE[][] getBoard() {
        return model.getBoard();
    }

    public int[][] getBombBlastStrength() {
        return model.getBombBlastStrength();
    }

    public int[][] getBombLife() {
        return model.getBombLife();
    }

    public int getTeam(){ return avatar.getTeam(); }

    public Types.TILETYPE[] getTeammates(){ return avatar.getTeammates(); }

    public Types.TILETYPE[] getEnemies(){ return avatar.getEnemies(); }

    public int nActions() {
        return nActions;
    }

    public Types.RESULT winner() {
        return avatar != null? avatar.getWinner() : Types.RESULT.INCOMPLETE;
    }

    public int getBlastStrength() {
        return avatar != null? avatar.getBlastStrength() : -1;
    }

    public int getPlayerId() {
        return avatar.getPlayerID();
    }

    public int getAmmo() {
        return avatar != null? avatar.getAmmo() : -1;
    }

    public boolean canKick() {
        return avatar != null? avatar.canKick() : false;
    }

    public Vector2d getPosition() {
        return avatar.getPosition();
    }

    public Types.GAME_MODE getGameMode() {
        return gameMode;
    }

    /**
     * @return an array of IDs for all agents left alive in the game.
     */
    public Types.TILETYPE[] getAliveAgentIDs() {
        ArrayList<GameObject> aliveAgents = getAliveAgents();
        Types.TILETYPE[] alive = new Types.TILETYPE[aliveAgents.size()];
        for (int i = 0; i < alive.length; i++) {
            alive[i] = aliveAgents.get(i).getType();
        }
        return alive;
    }

    /**
     * @return arraylist with the IDs of teammates which are alive
     */
    public ArrayList<Types.TILETYPE> getAliveTeammateIDs(){
        List<Types.TILETYPE> aliveAgents = Arrays.asList(getAliveAgentIDs()); // Doesn't include AGENTDUMMY
        Types.TILETYPE[] teammateIDs = avatar.getTeammates(); // May include AGENTDUMMY (if FFA)
        return trimAliveList(aliveAgents, teammateIDs);
    }

    /**
     * @return arraylist with the IDs of enemies which are alive
     */
    public ArrayList<Types.TILETYPE> getAliveEnemyIDs(){
        List<Types.TILETYPE> aliveAgents = Arrays.asList(getAliveAgentIDs()); // Doesn't include AGENTDUMMY
        Types.TILETYPE[] enemyIDs = avatar.getEnemies(); // May include AGENTDUMMY (if Team Mode)
        return trimAliveList(aliveAgents, enemyIDs);
    }

    /**
     * Trims the list of alive agents based on an array of agent ID types which should be the only ones included, if
     * alive.
     * @param aliveAgents - list of alive agents
     * @param trimIDs - list of IDs that should be included in the return, if alive
     * @return list of trim ID agents which are alive
     */
    private ArrayList<Types.TILETYPE> trimAliveList(List<Types.TILETYPE> aliveAgents, Types.TILETYPE[] trimIDs) {
        ArrayList<Types.TILETYPE> aliveTeammateIDs = new ArrayList<>();
        for (Types.TILETYPE teammateID : trimIDs) {
            if (teammateID == Types.TILETYPE.AGENTDUMMY || aliveAgents.contains(teammateID))
                aliveTeammateIDs.add(teammateID);
        }
        return aliveTeammateIDs;
    }

    /**
     * Checks if this game state is terminal, based on the status of the agent focused in this game state.
     * @return true if terminal, false otherwise.
     */
    public boolean isTerminal()
    {
        if (tick >= Types.MAX_GAME_TICKS)
            return true;
        if (avatar != null)
            return this.winner() != Types.RESULT.INCOMPLETE;
        for (GameObject aliveAgent : getAliveAgents()) {
            Avatar agent = (Avatar) aliveAgent;
            if (agent.getWinner() != Types.RESULT.INCOMPLETE && agent.getWinner() != Types.RESULT.LOSS)
                return true;
        }
        return false;
    }

    /**
     * @return the current game tick
     */
    public int getTick() { return tick; }


    /* ----- Methods to insert or remove observations into the game model ----- */

    public void addBomb(int x, int y, int blastStrength, int bombLife, int playerIdx, boolean addToBoard) {
        model.addBomb(x, y, blastStrength, bombLife, playerIdx, addToBoard);
    }

    public void addFlame(int x, int y, int life) {
        model.addFlame(x, y, life);
    }

    public void addPowerUp(int x, int y, Types.TILETYPE type, boolean visible) {
        model.addPowerUp(x, y, type, visible);
    }

    public void addObject(int x, int y, Types.TILETYPE type) {
        model.addObject(x, y, type);
    }

    public void removeObject(int x, int y, Types.TILETYPE type, boolean onlyBoard) {
        model.removeObject(x, y, type, onlyBoard);
    }

    public void removePowerUp(int x, int y, Types.TILETYPE type) {
        model.removePowerUp(x, y, type);
    }

    public void addAgent(int x, int y, int idx) {
        model.addAgent(x, y, idx);
    }

    public void setAgent(int playerIdx, int x, int y, boolean canKick, int ammo, int blastStrength) {
        model.setAgent(playerIdx, x, y, canKick, ammo, blastStrength);
    }

    public void setBomb(int x, int y, int playerIdx, Vector2d velocity) {
        model.setBomb(x, y, playerIdx, velocity);
    }

    public void setFlame(int x, int y, int life) {
        model.setFlame(x, y, life);
    }

    public int[] getMessage(){
        return getMessage(playerIdx);
    }

    public int[] getMessage(int playerIdx){
        return message[playerIdx];
    }

    void setMessage(int playerIdx, int[] msg){
        message[playerIdx] = msg.clone();
    }

    /* ----- Other methods ----- */

    @Override
    public String toString() {
        return model.toString();
    }

    @Override
    public boolean equals(Object o){
        if (o.getClass() != getClass())
            return false;
        GameState gs = (GameState)o;

        // Compare the state of everything. If anything is different, return false
        if (tick != gs.tick)
            return false;
        if (seed != gs.seed)
            return false;
        if (size != gs.size)
            return false;
        if (!model.equals(gs.model))
            return false;
        if (!gameMode.equals(gs.gameMode))
            return false;
        if (playerIdx != gs.playerIdx)
            return false;
        if ((avatar == null) || (gs.avatar == null)) {
            if ((avatar == null) != (gs.avatar == null))
                return false;
        }
        else if (!avatar.equals(gs.avatar))
            return false;
        return true;
    }


    /**
     * Optional game state constructor, used to parse JSON observations
     * @param state JSON game state
     */
    public GameState(String state){
        // TODO might be too much construction, maybe creating only forward models would be more efficient?
//        System.out.println("state = " + state);

        // State is [obs, action_space]
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(state);
        JsonObject object = element.getAsJsonObject();

        // Take obs and further extract the information
        String obs1 = object.get("obs").toString();

        // TODO this regex works, problem: if obs.get called the JSON breaks so have to fix it with replacements
        String obs = obs1.replaceAll("\\\\", "");
        obs = obs.replaceAll("\"\\{", "\\{");
        obs = obs.replaceAll("\\}\"", "\\}");
        JsonElement obsE = parser.parse(obs);
        JsonObject obsObj = obsE.getAsJsonObject();

        // Casting all the information to their java equivalent before passing it to the gamestate constructor
        int[] alive = gson.fromJson(obsObj.get("alive"), int[].class);
        int[][] board = gson.fromJson(obsObj.get("board"), int[][].class);
        int[][] bomb_blast_strength = gson.fromJson(obsObj.get("bomb_blast_strength"), int[][].class);
        int[][] bomb_life = gson.fromJson(obsObj.get("bomb_life"), int[][].class);

        int game_type = gson.fromJson(obsObj.get("game_type"), int.class); // game_type: 1 FFA
        String game_env = gson.fromJson(obsObj.get("game_env"), String.class); // pommerman.envs.v0:Pomme
        int[] position = gson.fromJson(obsObj.get("position"), int[].class); // [9, 1] - current agent's position x,y
        int blast_strength = gson.fromJson(obsObj.get("blast_strength"), int.class); // int -current agent's blast strength
        boolean can_kick = gson.fromJson(obsObj.get("can_kick"), boolean.class); // boolean
        //Types.TILETYPE[] teammate; //teammate - dummy 9 // todo it's hardcoded in our levegenerator
        int ammo = gson.fromJson(obsObj.get("ammo"), int.class); // int 1 -default
        //Types.TILETYPE[] enemies; // enemies list // todo
        int step_count = gson.fromJson(obsObj.get("step_count"), int.class); // step_count
        int action_space = object.get("action_space").getAsInt();

        Types.GAME_MODE gameMode = Types.GAME_MODE.FFA; // Default
        if (game_type == 1) {
            gameMode = Types.GAME_MODE.FFA;
            DEFAULT_VISION_RANGE = 4; // TODO THIS IS HARDCODED BY US
        }
        else if (game_type == 2){
            gameMode = Types.GAME_MODE.TEAM;
            DEFAULT_VISION_RANGE = 4; // TODO THIS IS HARDCODED BY US
        }
        else if (game_type == 3){
            gameMode = Types.GAME_MODE.TEAM_RADIO;
        }

        this.gameMode = gameMode;

        this.tick = step_count;
        this.seed = -1; // todo setting seed to -1 when communicating with python
        this.playerIdx = board[position[0]][position[1]]-10; // Coordinates are swapped
        this.nActions = action_space;
        this.size = board.length;

        try {
            this.model = new ForwardModel(board, bomb_blast_strength, bomb_life, alive, gameMode, this.playerIdx);
            this.avatar = (Avatar) model.getAgents()[playerIdx];
            this.avatar.setAmmo(ammo);
            this.avatar.setBlastStrength(blast_strength);
            this.avatar.setVisionRange(DEFAULT_VISION_RANGE);
            if (can_kick) this.avatar.canKick();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // Prints a game board in int representation
    private void printBoard(int[][] board) {
        for (int[] a: board){
            System.out.println(Arrays.toString(a));
        }
        System.out.println();
    }

    /**
     * @return a Json string representing the current game state
     */
    public String toJson(){
        SerializableGameState serialisableGameState = new SerializableGameState(
                getAliveAgentIDs(),
                model.getBoard(),
                model.getBombBlastStrength(),
                model.getBombLife(),
                gameMode,
                Types.getGameConfig().getEnvironmentName(),
                avatar.getPosition(),
                avatar.getBlastStrength(),
                avatar.canKick(),
                avatar.getTeammates(),
                avatar.getAmmo(),
                avatar.getEnemies(),
                getTick());

        // TODO: These may be initialised only once to improve performance
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        return gson.toJson(serialisableGameState);
    }

    /**
     * A serializable form of GameState containing information compatible with Python framework
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    class SerializableGameState {

        // Variable namings are set to match Python framework observations
        private int[] alive;
        private int[][] board;
        private int[][] bomb_blast_strength;
        private int[][] bomb_life;
        private int game_mode;
        private String game_env;
        private int[] position;
        private int blast_strength;
        private boolean can_kick;
        private Types.TILETYPE[] teammate;
        private int ammo;
        private Types.TILETYPE[] enemies;
        private int step_count;

        private SerializableGameState(
                Types.TILETYPE[] alive,
                Types.TILETYPE[][] boardTypes,
                int[][] bombBlastStrength,
                int[][] bombLife,
                Types.GAME_MODE gameMode,
                String gameEnv,
                Vector2d position,
                int blastStrength,
                boolean canKick,
                Types.TILETYPE[] teammate,
                int ammo,
                Types.TILETYPE[] enemies,
                int ticks)
        {
            this.alive = new int[alive.length];
            for (int i = 0; i < alive.length; i++) {
                this.alive[i] = alive[i].getKey();
            }

            this.board = new int[boardTypes.length][boardTypes[0].length];
            for (int i = 0; i < boardTypes.length; i++) {
                for (int j = 0; j < boardTypes[0].length; j++) {
                    board[i][j] = boardTypes[i][j].getKey();
                }
            }

            this.bomb_blast_strength = bombBlastStrength;
            this.bomb_life = bombLife;
            this.game_mode = gameMode.getKey();
            this.game_env = gameEnv;
            this.position = new int[]{position.x, position.y};
            this.blast_strength = blastStrength;
            this.can_kick = canKick;
            this.teammate = teammate;
            this.ammo = ammo;
            this.enemies = enemies;
            this.step_count = ticks;
        }
    }
}
