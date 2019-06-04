package players;

import core.GameState;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.Types;
import utils.Utils;

import java.util.ArrayList;
import java.util.Random;

public class OSLAPlayer extends Player {

    //Random generator for this controller
    private Random random;

    //True if the opponent model playes at random. False if it does nothing.
    private boolean rndOpponentModel;

    //A small number
    public double epsilon = 1e-6;

    // Heuristic to evaluate a given state.
    private StateHeuristic rootStateHeuristic;

    /**
     * Agent constructor
     * @param seed seed for the random generator for this controller
     * @param id id of this player
     */
    public OSLAPlayer(long seed, int id) {
        super(seed, id);
        random = new Random(seed);
    }

    /**
     * Action called every game tick. It must return an action to play in the real game.
     * @param gs - current game state.
     * @return the action to apply in the game.
     */
    @Override
    public Types.ACTIONS act(GameState gs) {

        rootStateHeuristic = new CustomHeuristic(gs);
        rndOpponentModel = true;

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        double maxQ = Double.NEGATIVE_INFINITY;
        Types.ACTIONS bestAction = null;

        for (Types.ACTIONS act : actionsList) {
            GameState gsCopy = gs.copy();
            roll(gsCopy, act);
            double valState = rootStateHeuristic.evaluateState(gsCopy);

            //System.out.println(valState);
            double Q = Utils.noise(valState, this.epsilon, this.random.nextDouble());

            //System.out.println("Action:" + action + " score:" + Q);
            if (Q > maxQ) {
                maxQ = Q;
                bestAction = act;
            }

        }

        return bestAction;

    }


    /**
     * Creates a copy of this agent
     * @return the copy of this agent.
     */
    @Override
    public Player copy() {
        return new OSLAPlayer(seed, playerID);
    }

    /**
     * Rolls the state forward applying action 'act' for this player
     * @param gs state to roll forward
     * @param act action OSLA would apply
     */
    private void roll(GameState gs, Types.ACTIONS act)
    {
        //gs.next() requires an array with the simulataneous actions of all players.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];

        for(int i = 0; i < nPlayers; ++i)
        {
            if(i == getPlayerID() - Types.TILETYPE.AGENT0.getKey())
            {
                //THIS IS ME. Insert the action I have.
                actionsAll[i] = act;
            }else{
                if(rndOpponentModel){
                    //If I assume opponents play at random, use that.
                    int actionIdx = random.nextInt(gs.nActions());
                    actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
                }else
                {
                    //Or I could assume they would do nothing.
                    actionsAll[i] = Types.ACTIONS.ACTION_STOP;
                }
            }
        }

        //Advance this state with all the actions for a turn.
        gs.next(actionsAll);
    }
}
