package players;

import core.GameState;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.Random;

public class OSLAPlayer extends Player {
    private Random random;
    private boolean rndOpponentModel;
    public double epsilon = 1e-6;
    private StateHeuristic rootStateHeuristic;

    public OSLAPlayer(long seed, int id) {
        super(seed, id);
        reset(seed, id);
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);
        random = new Random(seed);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {

        rootStateHeuristic = new CustomHeuristic(gs);
        rndOpponentModel = true;

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        double maxQ = Double.NEGATIVE_INFINITY;
        Types.ACTIONS bestAction = null;

        for (Types.ACTIONS act : actionsList) {
            GameState gsCopy = gs.copy();
            rollRnd(gsCopy, act);
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

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    @Override
    public Player copy() {
        return new OSLAPlayer(seed, playerID);
    }

    private void rollRnd(GameState gs, Types.ACTIONS act)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];

        for(int i = 0; i < nPlayers; ++i)
        {
            if(i == getPlayerID() - Types.TILETYPE.AGENT0.getKey())
            {
                actionsAll[i] = act;
            }else{
                if(rndOpponentModel){
                    int actionIdx = random.nextInt(gs.nActions());
                    actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
                }else
                {
                    actionsAll[i] = Types.ACTIONS.ACTION_STOP;
                }
            }
        }

        gs.next(actionsAll);
    }
}
