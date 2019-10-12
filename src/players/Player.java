package players;

import core.GameState;
import utils.Types;

/**
 * All Java-Pommerman players should extend this class and implement at least:
 * - a constructor invoking the super's constructor.
 * - the act(GameState) method.
 * They may also implement the result(GameState) method for any end of game post-processing.
 */
public abstract class Player {
    protected int playerID;
    protected long seed;

    /**
     * Default constructor, to be called in subclasses (initializes player ID and random seed for this agent.
     * @param seed - random seed for this player.
     * @param pId - this player's ID.
     */
    protected Player(long seed, int pId) {
        reset(seed, pId);
    }

    /**
     * Function requests an action from the agent, given current game state observation.
     * @param gs - current game state.
     * @return - action to play in this game state.
     */
    public abstract Types.ACTIONS act(GameState gs);

    /**
     * Function that is called for requesting a message from the player
     * @return int array, representing the message to be passed for the teammate
     */
    public abstract int[] getMessage();

    /**
     * Function called at the end of the game. May be used by agents for final analysis.
     * @param reward - final reward for this agent.
     */
    public void result(double reward) {}

    /**
     * Getter for player ID field.
     * @return - this player's ID.
     */
    public final int getPlayerID() {
        return playerID;
    }

    /**
     * Getter for seed field.
     * @return - this player's random seed.
     */
    public final long getSeed() {
        return seed;
    }

    public abstract Player copy();

    public void reset(long seed, int playerID) {
        this.playerID = playerID;
        this.seed = seed;
    }
}
