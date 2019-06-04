package players.mcts;

import core.GameState;
import players.heuristics.AdvancedHeuristic;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.Random;

/**
 * This class models a node in the tree
 */
public class SingleTreeNode
{
    /** Main members of the tree node */
    private GameState rootState;                //Root state of the tree.
    private SingleTreeNode parent;              //Parent of this node
    private SingleTreeNode[] children;          //Children of this node.
    private double totValue;                    //Accummulated reward obtained from this node. Divide by nVisits to obtain Q(s,a)
    private int nVisits;                        //Number of times this node has been visited.
    private int m_depth;                        //Maximum depth (number of states ahead) reached on each iteration.
    private int num_actions;                    //Number of available actions
    private Types.ACTIONS[] actions;            //Array with all actions available

    private StateHeuristic rootStateHeuristic;  //Heuristic to evaluate game states at the end of rollouts.

    /** Other auxiliary members */

    //Running bounds: keep track of the highest and lowest rewards ever seen in the game. Used for normalizing Q(s,a)
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    public MCTSParams params;                   //Parameters set for MCTS

    private int childIdx;                       //Index of this child in the parent's array of children
    private int fmCallsCount;                   //Keeps a count on the number of usages of the forward model (used for termination condition)
    private Random m_rnd;                       //Random object for random choosing. Same object to depend only on one seed




    //Constructor of the MCTS node class.
    SingleTreeNode(MCTSParams p, Random rnd, int num_actions, Types.ACTIONS[] actions) {
        this(p, null, -1, rnd, num_actions, actions, 0, null);
    }

    //Constructor of the MCTS node class.
    private SingleTreeNode(MCTSParams p, SingleTreeNode parent, int childIdx, Random rnd, int num_actions,
                           Types.ACTIONS[] actions, int fmCallsCount, StateHeuristic sh) {
        this.params = p;
        this.fmCallsCount = fmCallsCount;
        this.parent = parent;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
        children = new SingleTreeNode[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null) {
            m_depth = parent.m_depth + 1;
            this.rootStateHeuristic = sh;
        }
        else
            m_depth = 0;
    }

    /**
     * Used before running the algorithm to set the state of the root note. It also assigns the heuristic that will
     * evaluates game state, which may also depend on the root.
     * @param gs Current game state, root of MCTS.
     */
    void setRootGameState(GameState gs)
    {
        this.rootState = gs;
        if (params.heuristic_method == params.CUSTOM_HEURISTIC)
            this.rootStateHeuristic = new CustomHeuristic(gs);
        else if (params.heuristic_method == params.ADVANCED_HEURISTIC) // New method: combined heuristics
            this.rootStateHeuristic = new AdvancedHeuristic(gs, m_rnd);
    }


    /**
     * Main entry point of the algorithm. Runs MCTS until budget is exhausted. Budget is determined by the MCTSParams
     * variable of this class.
     * @param elapsedTimer In case budget is expressed in time, elapsedTimer includes the time remaining until this
     *                     time is exhausted.
     */
    void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        //Some auxiliary variables to manage different budget expirations
        long remaining;
        int numIters = 0;
        int remainingLimit = 2; //Safe time threshold for time budget.

        //Run MCTS in a loop until termination condition.
        boolean stop = false;
        while(!stop){

            //Start from a copy of the game state
            GameState state = rootState.copy();

            //1. Selection and 2. Expansion are executed in treePolicy(state)
            SingleTreeNode selected = treePolicy(state);
            //3. Simulation - rollout
            double delta = selected.rollOut(state);
            //4. Back-propagation
            backUp(selected, delta);

            //Stopping condition: it can be time, number of iterations or uses of the forward model.
            //For each case, update counts to determine if we must stop.
            if(params.stop_type == params.STOP_TIME) {
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                numIters++;
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS)
            {
                fmCallsCount+=params.rollout_depth;
                stop = (fmCallsCount + params.rollout_depth) > params.num_fmcalls;
            }
        }

    }

    /**
     * Performs the tree policy. Navigates down the tree selecting nodes using UCT, until a not-fully expanded
     * node is reach. Then, it starts the call to expand it.
     * @param state Current state to do the policy from.
     * @return the expanded node.
     */
    private SingleTreeNode treePolicy(GameState state) {

        //'cur': our current node in the tree.
        SingleTreeNode cur = this;

        //We keep going down the tree as long as the game is not over and we haven't reached the maximum depth
        while (!state.isTerminal() && cur.m_depth < params.rollout_depth)
        {
            //If not fully expanded, expand this one.
            if (cur.notFullyExpanded()) {
                //This one is the node to start the rollout from.
                return cur.expand(state);

            } else {
                //If fully expanded, apply UCT to pick one of the children of 'cur'
                cur = cur.uct(state);
            }
        }

        //This one is the node to start the rollout from.
        return cur;
    }

    /**
     * Checks if this node is not fully expanded. If any of my children is null, then it's not fully expanded.
     * @return true if the node is not fully expanded.
     */
    private boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Performs the expansion phase of the MCTS iteration.
     * @param state Game state *before* the expansion happens (i.e. parent node that is not fully expanded).
     * @return The newly expande tree node.
     */
    private SingleTreeNode expand(GameState state) {

        //Go through all the not-expanded children of this node and pick one at random.
        int bestAction = 0;
        double bestValue = -1;
        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        //Roll the state forward, using the Forward Model, applying the action chosen at random
        roll(state, actions[bestAction]);

        //state is now the next state, of the expanded node. Create a node with such state
        // and add it to the tree, as child of 'this'
        SingleTreeNode tn = new SingleTreeNode(params,this,bestAction,this.m_rnd,num_actions,
                actions, fmCallsCount, rootStateHeuristic);
        children[bestAction] = tn;

        //Get the expanded node back.
        return tn;
    }

    /**
     * Uses the Forward Model to advance the state 'gs' with the action 'act'.
     * @param gs Game state to advance.
     * @param act Action to use to advance the game state.
     */
    private void roll(GameState gs, Types.ACTIONS act)
    {
        //To roll the state forward, we need to pass an action for *all* players.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];

        //This is the location in the array of actions according to my player ID
        int playerId = gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

        for(int i = 0; i < nPlayers; ++i)
        {
            if(playerId == i)
            {
                //This is me, just put the action in the array.
                actionsAll[i] = act;
            }else {
                // This is another player. We can have different models:

                // Random model
                int actionIdx = m_rnd.nextInt(gs.nActions());           // Action index at random
                actionsAll[i] = Types.ACTIONS.all().get(actionIdx);     // Pick the action from the array of actions

//                actionsAll[i] = Types.ACTIONS.ACTION_STOP;            //This is to assume the other players would do nothing.
            }
        }

        //Once the array is ready, advance the state. This changes the internal 'gs' object.
        gs.next(actionsAll);

    }

    /**
     * Performs UCT in a node. Selects the action to follow during the tree policy.
     * @param state
     * @return
     */
    private SingleTreeNode uct(GameState state) {

        //We'll pick the action with the highest UCB1 value.
        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode child : this.children)
        {
            //For each chindren, calculate the different parts. First, exploitation:
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + params.epsilon);  //Use epsilon to avoid /0.

            //Normalize rewards between 0 and 1 for the exploitation term (allows use of sqrt(2) as balance constant K
            double exploit = childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
            double explore = Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon)); //Note we can use child.nVisits for N(s,a)

            //UCB1!
            double uctValue = exploit + params.K * explore;

            //Little trick: in case there are ties of values, add some little random noise to it to break ties
            uctValue = Utils.noise(uctValue, params.epsilon, this.m_rnd.nextDouble());

            // keep the best one.
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }

        if (selected == null)
        {
            //This would be odd, but can happen if we reach a tree with no children. That probable means ERROR.
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //We need to roll the state, using the Forward Model, to keep going down the tree.
        roll(state, actions[selected.childIdx]);

        //Return the selected node to continue the Selection phase.
        return selected;
    }

    /**
     * Performs the default policy (random rollout).
     * @param state State where the rollout starts.
     * @return Returns the value of the state found at the end of the rollout.
     */
    private double rollOut(GameState state)
    {
        //Keep track of the current depth - we won't be creating nodes here.
        int thisDepth = this.m_depth;

        //While the rollout shouldn't finish...
        while (!finishRollout(state,thisDepth)) {
            //Take a random (but safe) action from this state.
            int action = safeRandomAction(state);
            //Advance the state with it and add 1 to the depth count.
            roll(state, actions[action]);
            thisDepth++;
        }

        return rootStateHeuristic.evaluateState(state);
    }

    /**
     * Takes a random action among the possible safe one.
     * @param state State to take the action from
     * @return index of the action to execute.
     */
    private int safeRandomAction(GameState state)
    {
        Types.TILETYPE[][] board = state.getBoard();
        ArrayList<Types.ACTIONS> actionsToTry = Types.ACTIONS.all();
        int width = board.length;
        int height = board[0].length;

        //For all actions
        while(actionsToTry.size() > 0) {

            //See where would this take me.
            int nAction = m_rnd.nextInt(actionsToTry.size());
            Types.ACTIONS act = actionsToTry.get(nAction);
            Vector2d dir = act.getDirection().toVec();

            Vector2d pos = state.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            //Make sure there are no flames that would kill me there.
            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES)
                    return nAction;

            actionsToTry.remove(nAction);
        }

        //If we got here, we couldn't find an action that wouldn't kill me. We can take any, really.
        return m_rnd.nextInt(num_actions);
    }

    /**
     * Checks if a rollout should finish.
     * @param rollerState State being rolled
     * @param depth How far should we go rolling it forward.
     * @return False when we shoud continue, true if we should finish.
     */
    private boolean finishRollout(GameState rollerState, int depth)
    {
        if (depth >= params.rollout_depth)      //rollout end condition.
            return true;

        if (rollerState.isTerminal())           //end of game
            return true;

        return false;
    }

    /**
     * Back propagation step of MCTS. Takes the value of a state and updates the accummulated reward on each
     * traversed node, using the parent link. Updates count visits as well. Also updates bounds of the rewards
     * seen so far.
     * @param node Node to start backup from. This node should be the one expanded in this iteration.
     * @param result Reward to back-propagate
     */
    private void backUp(SingleTreeNode node, double result)
    {
        SingleTreeNode n = node;

        //Go up until n == null, which happens after updating the root.
        while(n != null)
        {
            n.nVisits++;                    //Another visit to this node (N(s)++)
            n.totValue += result;           //Accummulate result (computationally cheaper than having a running average).

            //Update the bounds.
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }

            //Next one, the parent.
            n = n.parent;
        }
    }

    /**
     * Checks which one is the index of the most used action of this node. Used for the recommendation policy.
     * @return Returns the index of the most visited action action
     */
    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                //As with UCT, we add small random noise to break potential ties.
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        //This should happen, just pick the first action available
        if (selected == -1)
        {
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal (rare), we opt to choose for the one with the highest UCB1 value
            selected = bestAction();
        }

        return selected;
    }

    /**
     * Returns the index of the action with the highest UCB1 value to take from this node.
     * @return the index of the best action
     */
    private int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + params.epsilon);
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }

}
