package core.gameConfig;

import objects.GameObject;
import utils.Types;

import java.util.ArrayList;

/**
 * This interface provides the possibiilty of define different team configurations for Pommerman
 */
public interface IGameConfig
{

    /**
     * Returns the name for this config.
     * @return the name for this config.
     */
    String getEnvironmentName();

    /**
     * Returns the team id of the player represented by playerID.
     * @param gameMode Game mode used
     * @param playerID playerID of the player team is requested
     * @return the team ID
     */
    int getTeam(Types.GAME_MODE gameMode, int playerID);

    /**
     * Returns an array with all the teammates of player with playerID
     * @param gameMode Game mode used
     * @param playerID playerID whose teammates we want to obtain
     * @return array with all the teammates
     */
    Types.TILETYPE[] getTeammates(Types.GAME_MODE gameMode, int playerID);

    /**
     * Returns an array with all the enemies of player with playerID
     * @param gameMode Game mode used
     * @param playerID playerID whose enemies we want to obtain
     * @return array with all the enemies
     */
    Types.TILETYPE[] getEnemies(Types.GAME_MODE gameMode, int playerID);

    /**
     * Returns a multi-dimiensional array with the IDs of all players. First dimension is
     * the number of teams. Second dimension is the members of each team.
     * Use Types.TILETYPE.AGENTDUMMY if teams with different sizes are built.
     * @param gameMode Game mode being used
     * @return the teams array.
     */
    Types.TILETYPE[][] getTeams(Types.GAME_MODE gameMode);

    /**
     * Processes winners from the list of agends dead this tick. It updates the game objects in the allAgents and
     * aliveAgents based on the agents indicated in the deadAgentsThisTick array.
     * @param deadAgentsThisTick agents dead this tick
     * @param allAgents All agents in the game, dead or alive.
     * @param aliveAgents Alive agents in the game.
     * @param game_mode Game mode this is being played with
     */
    void processDeadAgents(GameObject[] allAgents, ArrayList<GameObject> aliveAgents,
                           ArrayList<GameObject> deadAgentsThisTick, Types.GAME_MODE game_mode);

    /**
     * Processes the final winning statuses for all agents when the time runs out.
     * @param gameMode Game mode being played
     * @param allAgents All agents in the game.
     * @param aliveAgents All alive agents in the game.
     */
    void processTimeout(Types.GAME_MODE gameMode, GameObject[] allAgents, ArrayList<GameObject> aliveAgents);

    /**
     * Indicates if the game is ended.
     * @param gameTick current game tick
     * @param gameMode game mode being played
     * @param aliveAgents All alive agents in the game.
     * @return true is the game is over.
     */
    boolean isEnded(int gameTick, Types.GAME_MODE gameMode, ArrayList<GameObject> aliveAgents);


    /**
     * Returns the rewards for all agents playing the game.
     * @param gameTick current game tick
     * @param results winning conditions for all agents.
     * @return the rewards for all agents playing the game
     */
    double[] getRewards(int gameTick, Types.RESULT[] results);

}
