package core.gameConfig;

import objects.Avatar;
import objects.GameObject;
import utils.Types;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import static utils.Types.MAX_GAME_TICKS;

public class OriginalGameConfig implements IGameConfig {

    @Override
    public String getEnvironmentName() {
        return "java-pommermam-original";
    }

    @Override
    public int getTeam(Types.GAME_MODE gameMode, int playerID) {
        if (gameMode == Types.GAME_MODE.FFA)
            return playerID - 10;
        return playerID % 2;
    }

    @Override
    public Types.TILETYPE[] getTeammates(Types.GAME_MODE gameMode, int playerID) {


        if (gameMode == Types.GAME_MODE.FFA) { // FFA Mode
            return new Types.TILETYPE[] {Types.TILETYPE.AGENTDUMMY};

        } else { // Team Mode

            if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT0){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT2};
            }
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT1){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT3};
            }
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT2){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT0};
            }
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT3){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT1};
            }
        }

        System.out.println("WARNING: Unknown teammates for " + playerID + " in game mode " + gameMode);
        return new Types.TILETYPE[0];
    }

    @Override
    public Types.TILETYPE[] getEnemies(Types.GAME_MODE gameMode, int playerID) {


        if (gameMode == Types.GAME_MODE.FFA) { // FFA Mode

            if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT0)
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT1, Types.TILETYPE.AGENT2, Types.TILETYPE.AGENT3};
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT1)
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT0, Types.TILETYPE.AGENT2, Types.TILETYPE.AGENT3};
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT2)
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT0, Types.TILETYPE.AGENT1, Types.TILETYPE.AGENT3};
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT3)
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT0, Types.TILETYPE.AGENT1, Types.TILETYPE.AGENT2};

        } else { // Team Mode

            // Hardcoded for array initialisation
            if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT0){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT1, Types.TILETYPE.AGENT3, Types.TILETYPE.AGENTDUMMY};
            }
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT1){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT0, Types.TILETYPE.AGENT2, Types.TILETYPE.AGENTDUMMY};
            }
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT2){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT1, Types.TILETYPE.AGENT3, Types.TILETYPE.AGENTDUMMY};
            }
            else if(Types.TILETYPE.values()[playerID] == Types.TILETYPE.AGENT3){
                return new Types.TILETYPE[] {Types.TILETYPE.AGENT0, Types.TILETYPE.AGENT2, Types.TILETYPE.AGENTDUMMY};
            }
        }

        System.out.println("WARNING: Unknown enemies for " + playerID + " in game mode " + gameMode);
        return new Types.TILETYPE[0];
    }


    @Override
    public Types.TILETYPE[][] getTeams(Types.GAME_MODE gameMode) {

        Types.TILETYPE[][] teams;
        if (gameMode == Types.GAME_MODE.FFA) { // FFA Mode
            //4 teams with one agent on each.
            teams = new Types.TILETYPE[4][1];

            teams[0][0] = Types.TILETYPE.AGENT0;
            teams[1][0] = Types.TILETYPE.AGENT2;
            teams[2][0] = Types.TILETYPE.AGENT1;
            teams[3][0] = Types.TILETYPE.AGENT3;
        }else{
            //Original Pommerman has 2 teams with pairs {{AGENT0, AGENT2}{AGENT1, AGENT3}}
            teams = new Types.TILETYPE[2][2];

            //Team 0
            teams[0][0] = Types.TILETYPE.AGENT0;
            teams[0][1] = Types.TILETYPE.AGENT2;

            //Team 1
            teams[1][0] = Types.TILETYPE.AGENT1;
            teams[1][1] = Types.TILETYPE.AGENT3;
        }

        return teams;
    }

    /**
     * Processes winners from the list of agends dead this tick.
     * Rules are (from original Pommerman):
     *  "The game ends when only players from one team remains. Ties can happen when
     *   the game does not end before the max steps or if both teams' last agents are
     *   destroyed on the same turn."
     *   All final winner statuses are shared by all members of the same team.
     * @param deadAgentsThisTick agents dead this tick
     * @param allAgents All agents in the game, dead or alive.
     * @param aliveAgents Alive agents in the game.
     * @param game_mode Game mode this is being played with
     */
    public void processDeadAgents(GameObject[] allAgents, ArrayList<GameObject> aliveAgents,
                                   ArrayList<GameObject> deadAgentsThisTick, Types.GAME_MODE game_mode)
    {
        int numDeadThisTick = deadAgentsThisTick.size();
        int numAliveAgents = aliveAgents.size();

        if(game_mode == Types.GAME_MODE.FFA)
        {
            //Those who died in this tick will have lost, unless everybdy is dead, in which case is a tie
            Types.RESULT status = (numDeadThisTick == numAliveAgents) ?  Types.RESULT.TIE : Types.RESULT.LOSS;
            Utils.setWinningStatus(deadAgentsThisTick, status);

        }else  //Types.GAME_MODE.TEAM and Types.GAME_MODE.TEAM_RADIO modes
        {
            //1. First, let's look at dead agents.
            if (numDeadThisTick == numAliveAgents) {
                // ALL the agents (died before and now) end the game with a TIE.
                Utils.setWinningStatus(allAgents, Types.RESULT.TIE);
            } else {
                // Some agents still left alive, the dead ones have lost.
                Utils.setWinningStatus(deadAgentsThisTick, Types.RESULT.LOSS);
            }
        }

        //remove all dead agents from the alive array.
        aliveAgents.removeAll(deadAgentsThisTick);

        //If there's still people alive, we may have a winner.
        if(aliveAgents.size() > 0) {
            //Get the number of alive avatars per team (FFA: each player is on its own team).
            int[] aliveTeamCount = getAliveCountPerTeam(game_mode, aliveAgents);
            int nTeams = aliveTeamCount.length;

            //Set LOSS as winning status for all players in teams with no representatives.
            int teamsWithAlivePlayers = nTeams;
            int lastTeamWithAlivePlayers = -1;
            for (int i = 0; i < nTeams; i++) {
                if (aliveTeamCount[i] == 0) {
                    Utils.setWinningStatus(allAgents, Types.RESULT.LOSS, i);
                    teamsWithAlivePlayers--;
                } else lastTeamWithAlivePlayers = i;
            }

            // If there's one team only (only 1 player in FFA), we have a winner.
            if (teamsWithAlivePlayers == 1) {
                Utils.setWinningStatus(allAgents, Types.RESULT.WIN, lastTeamWithAlivePlayers);
            }
        }

    }


    /**
     * Processes the final winning statuses for all agents when the time runs out.
     * In this config, for FFA all alive agents are set a TIE. For TEAM/TEAM_RADIO, all agents in game are TIE (dead or alive).
     * @param gameMode Game mode being played
     * @param allAgents All agents in the game.
     * @param aliveAgents All alive agents in the game.
     */
    public void processTimeout(Types.GAME_MODE gameMode, GameObject[] allAgents, ArrayList<GameObject> aliveAgents) {
        if(gameMode == Types.GAME_MODE.FFA) {
            Utils.setWinningStatus(aliveAgents, Types.RESULT.TIE);
        } else { //Types.GAME_MODE.TEAM and Types.GAME_MODE.TEAM_RADIO
            Utils.setWinningStatus(allAgents, Types.RESULT.TIE);
        }
    }


    /**
     * Indicates if the game is ended. In this config, it is if there's 1 or none players alive (for FFA)
     * or if all remaining players are from the same team (TEAM, TEAM_RADIO).
     * @param gameTick current game tick
     * @param gameMode game mode being played
     * @param aliveAgents number of agents alive per team.
     * @return true is the game is over.
     */
    public boolean isEnded(int gameTick, Types.GAME_MODE gameMode, ArrayList<GameObject> aliveAgents) {

        //All game modes trigger and end when the the number of game ticks reaches the the max.
        if (gameTick == MAX_GAME_TICKS)
        {
            return true;
        }

        //If there are no alive agents, the game is over
        if(aliveAgents.size() == 0)
        {
            return true;
        }

        //If still playing, the game is over as soon as at least one player has a WIN status.
        for(GameObject gobj : aliveAgents)
        {
            Avatar av = (Avatar)gobj;
            if(av.getWinner() == Types.RESULT.WIN)
                return true;
        }

        return false;

    }

    /**
     * Returns the rewards for all agents playing the game
     *  If game is timed out, eveyone gets -1. Otherwise, +1 is given for victory and -1 for anything else (tie or loss).
     * @param gameTick current game tick
     * @param results winning conditions for all agents.
     * @return the rewards for all agents playing the game
     */
    public double[] getRewards(int gameTick, Types.RESULT[] results) {

        double[] rewards = new double[results.length];

        //Fast init.
        Arrays.fill(rewards, -1);

        if (gameTick < MAX_GAME_TICKS) {
            // The game is not over due to time , then agents get
            // 1 for winning, -1 in all the other cases.
            for(int i = 0; i < results.length; ++i)
            {
                rewards[i] = (results[i] == Types.RESULT.WIN) ? 1 : -1;
            }

        }
        return rewards;
    }


    /**
     * Retrieves the count of agents left alive per team. In FFA mode returns 4 teams of 1 player.
     * @return array of ints with the count for each team. In team modes the order is team [0,2], team [1,3].
     */
    private int[] getAliveCountPerTeam(Types.GAME_MODE gameMode, ArrayList<GameObject> aliveAgents) {

        int[] alive;
        if (gameMode == Types.GAME_MODE.FFA) {
            alive = new int[4];
            for (GameObject a : aliveAgents) {
                alive[a.getType().getKey()-10]++;
            }

        } else { // Types.GAME_MODE.TEAM & Types.GAME_MODE.TEAM_RADIO
            alive = new int[2];
            for (GameObject a : aliveAgents) {
                if (a.getType() == Types.TILETYPE.AGENT0 || a.getType() == Types.TILETYPE.AGENT2)
                    alive[0]++;
                else
                    alive[1]++;
            }
        }

        return alive;
    }

}
