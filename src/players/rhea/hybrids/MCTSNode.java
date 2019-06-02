package players.rhea.hybrids;

import core.GameState;
import players.rhea.GameInterface;
import players.rhea.utils.FMBudget;
import players.heuristics.StateHeuristic;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Utils;

import java.util.ArrayList;
import java.util.Random;

public class MCTSNode
{
    private MCTSNode parent;
    private MCTSNode[] children;
    private double totValue;
    private int nVisits;
    private Random m_rnd;
    private int m_depth;
    private double[] bounds = new double[]{-1, 1};
    private int childIdx;

    private int num_actions;
    private Types.ACTIONS[] actions;
    private int ROLLOUT_DEPTH = 10;
    private double K = Math.sqrt(2);

    public static GameState rootState;
    private StateHeuristic stateHeuristic;
    private static GameInterface gameInterface;
    private static FMBudget fmBudget;

    private static ArrayList<MCTSNode> allNodes;

    public MCTSNode(Random rnd, int num_actions, Types.ACTIONS[] actions, StateHeuristic sh, GameInterface gInterface) {
        this(null, -1, rnd, num_actions, actions, sh, gInterface);
    }

    private MCTSNode(MCTSNode parent, int childIdx, Random rnd, int num_actions, Types.ACTIONS[] actions,
                     StateHeuristic sh, GameInterface gInterface) {
        this.stateHeuristic = sh;
        MCTSNode.gameInterface = gInterface;
        this.parent = parent;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
        children = new MCTSNode[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null) {
            m_depth = parent.m_depth + 1;
            allNodes.add(this);
        }
        else
            m_depth = 0;
    }

    public void mctsSearch(ElapsedCpuTimer elapsedTimer, int fm_budget, int iteration_budget, int depth) {
        allNodes = new ArrayList<>();

        fmBudget = new FMBudget(fm_budget);
        int numIters = iteration_budget;

        ROLLOUT_DEPTH = depth;

        while(gameInterface.budget(elapsedTimer, numIters, fmBudget)){
            GameState state = rootState.copy();

            MCTSNode selected = treePolicy(state);
            double delta = selected.rollOut(state, fm_budget);
            backUp(selected, delta);

            numIters--;
            gameInterface.endIteration(elapsedTimer, fmBudget);
        }
    }

    private MCTSNode treePolicy(GameState state) {

        MCTSNode cur = this;

        while (!state.isTerminal() && cur.m_depth < ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand(state);

            } else {
                cur = cur.uct(state);
            }
        }

        return cur;
    }

    private MCTSNode expand(GameState state) {

        int bestAction = 0;
        double bestValue = -1;

        int nChildren = children.length;
        for (int i = 0; i < nChildren; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        //Roll the state
        gameInterface.advanceState(state, actions[bestAction]);
        fmBudget.use();

        MCTSNode tn = new MCTSNode(this, bestAction, this.m_rnd, num_actions, actions, stateHeuristic,
                gameInterface);
        children[bestAction] = tn;
        return tn;
    }

    private MCTSNode uct(GameState state) {

        MCTSNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (MCTSNode child : this.children)
        {
            double hvVal = child.totValue;
            double epsilon = 1e-6;
            double childValue =  hvVal / (child.nVisits + epsilon);

            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

            double uctValue = childValue +
                    K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + epsilon));

            uctValue = Utils.noise(uctValue, epsilon, this.m_rnd.nextDouble());     //break ties randomly

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        gameInterface.advanceState(state, actions[selected.childIdx]);
        fmBudget.use();

        return selected;
    }


    private double rollOut(GameState state, int numCalls)
    {
        int thisDepth = this.m_depth;

        while (!finishRollout(state, thisDepth, numCalls)) {

            int action = m_rnd.nextInt(num_actions);
            gameInterface.advanceState(state, actions[action]);
            fmBudget.use();
            thisDepth++;
        }

        double delta = value(state);

        if(delta < bounds[0])
            bounds[0] = delta;
        if(delta > bounds[1])
            bounds[1] = delta;

        //double normDelta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);

        return delta;
    }

    public double value(GameState a_gameState) {
        return gameInterface.evaluateState(a_gameState);
    }

    private boolean finishRollout(GameState rollerState, int depth, int numCalls)
    {
        if (fmBudget.getUsed() >= numCalls)
            return true;

        if (depth >= ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        //end of game
        return rollerState.isTerminal();

    }

    private void backUp(MCTSNode node, double result)
    {
        MCTSNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            n = n.parent;
        }
    }

    public double[][] compressTree(int max_depth, int max_actions) {
        double[][] actions = new double[max_depth][max_actions];
        double[] sum = new double[max_depth];

        for (MCTSNode n: allNodes) {

            if (actions[n.m_depth - 1] == null) {
                actions[n.m_depth - 1] = new double[max_actions];
            }

            actions[n.m_depth - 1][n.childIdx] += n.nVisits;
            sum[n.m_depth - 1] += n.nVisits;
        }

        // Break down to percentages
        for (int i = 0; i < max_depth; i++) {
            for (int j = 0; j < max_actions; j++) {
                actions[i][j] /= sum[i];
            }
        }

        return actions;
    }

    private boolean notFullyExpanded() {
        for (MCTSNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
}
