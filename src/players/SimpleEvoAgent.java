package players;

import core.GameState;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.Types;

import java.util.Arrays;
import java.util.Random;

public class SimpleEvoAgent extends Player {

    /**
     * Random number generator
     */
    private Random random;


    /**
     * Heuristic to evaluate states at the end of the individual.
     */
    private StateHeuristic rootStateHeuristic;


    /* All the parameters that control the agent */
    public boolean flipAtLeastOneValue = true;          //If true, makes sure mutation changes at least one action.
    public double mutProb = 0.4;                        //Probability of mutating one gene in the individual
    public int sequenceLength = 20;                     //Length of the individual.
    public int nEvals = 120;                            //Number of evaluations to run this for.
    public boolean useShiftBuffer = true;               //true if Shiftbuffer should be used.
    public Double discountFactor = 0.99;                //discount factor to boost actions that choose closer rewards.

    //A small number
    public double epsilon = 1e-6;

    //Keeps the best solution, as an array of int (action indices), found so far.
    int[] solution;

    //Opponent model for deciding actions of the other players every time the state needs to be rolled.
    Player opponent = new DoNothingPlayer(0);


    /**
     * Constructor that receives a random seed and the player ID
     * @param seed - random seed
     * @param id - player ID
     */
    public SimpleEvoAgent(long seed, int id) {
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

        //Set the heuristic with information about the starting state.
        rootStateHeuristic = new CustomHeuristic(gs);

        //Determine what's the action (index) to play
        int action = getAction(gs, playerID);

        //Return the action determined.
        return Types.ACTIONS.all().get(action);
    }

    /**
     * Returns an action to play in the game
     * @param gameState the current state of the game.
     * @param playerId the id of this player
     * @return the action to apply
     */
    public int getAction(GameState gameState, int playerId) {
        //Returns the first action from the best sequence found by SimpleEvoAgent
        return getActions(gameState, playerId)[0];
    }

    /**
     * Given a game state and my player ID, returns a potential solution (best individual)
     * @param gameState current game state
     * @param playerId my player ID
     * @return the best solution (sequence of actions) found
     */
    public int[] getActions(GameState gameState, int playerId) {
        //If using a shift buffer and I've got a solution from the previous game tick...
        if (useShiftBuffer && solution != null) {
            //re-use it, shift it and append
            solution = shiftLeftAndRandomAppend(solution, gameState.nActions());
        } else {
            // let's start form a random point in the search space: a random individual / sequence of actions
            solution = randomPoint(gameState.nActions());
        }

        //Let's create and evaluate new individuals until the number of evaluations (budget) runs out.
        for (int i = 0; i < nEvals; i++) {
            // mutate the current individual and evaluate the best one so far and the new mutatted one
            int[] mut = mutate(solution, mutProb, gameState.nActions());
            double curScore = evalSeq(gameState.copy(), solution, playerId);
            double mutScore = evalSeq(gameState.copy(), mut, playerId);
            if (mutScore >= curScore) {
                //If the mutated individual is better or equal, we keep it as our new best.
                solution = mut;
            }
        }

        //Return the best solution (sequence of actions) found for this cycle.
        int[] tmp = solution;
        if (!useShiftBuffer) solution = null;       // nullify if not using a shift buffer
        return tmp;
    }

    /**
     * Mutates an individual
     * @param v individual (sequence of actions) to mutate.
     * @param mutProb Mutation probability per gene (action)
     * @param nActions Number of available actions to choose from
     * @return a new individual, mutated version of 'v'
     */
    private int[] mutate(int[] v, double mutProb, int nActions) {

        int n = v.length;
        int[] x = new int[n];
        // pointwise probability of additional mutations
        // choose element of vector to mutate
        int ix = random.nextInt(n);
        if (!flipAtLeastOneValue) {
            // setting this to -1 means it will never match the first clause in the if statement in the loop
            // leaving it at the randomly chosen value ensures that at least one bit (or more generally value) is always flipped
            ix = -1;
        }
        // copy all the values faithfully apart from the chosen one
        for (int i = 0; i < n; i++) {
            if (i == ix || random.nextDouble() < mutProb) {
                x[i] = mutateValue(v[i], nActions);
            } else {
                x[i] = v[i];
            }
        }
        return x;
    }

    /**
     * Mutates one value, if possible.
     * @param cur Current value to move away from.
     * @param nActions number of possible values this could take
     * @return a different value than 'cur', chosen at random, among the possible ones.
     */
    private int mutateValue(int cur, int nActions) {
        // the range is nActions-1, since we
        // selecting the current value is not allowed
        // therefore we add 1 if the randomly chosen
        // value is greater than or equal to the current value
        if (nActions <= 1) return cur;
        int rx = random.nextInt(nActions - 1);
        return rx >= cur ? rx + 1 : rx;
    }

    /**
     * Gets a random individual
     * @param nActions number of possible actions
     * @return a random sequence of actions, were each one could be an action index between 0 and nActions-1
     */
    private int[] randomPoint(int nActions) {
        int[] p = new int[sequenceLength];
        for (int i=0; i<p.length; i++) {
            p[i] = random.nextInt(nActions);
        }
        return p;
    }

    /**
     * Takes and individual and applies the shift buffer, appending a random action at the end.
     * @param v individual to shift and append
     * @param nActions actions avilable.
     * @return the new individual.
     */
    private int[] shiftLeftAndRandomAppend(int[] v, int nActions) {
        int[] p = new int[v.length];
        for (int i = 0; i < p.length - 1; i++) {
            p[i] = v[i + 1];
        }
        p[p.length - 1] = random.nextInt(nActions);
        return p;
    }

    /**
     * Evaluates an individual or sequence of actions
     * @param gameState Current game state to evaluate this sequence from
     * @param seq sequence to evaluate.
     * @param playerId this player's ID
     * @return The value of the state reached at the end of the sequence
     */
    private double evalSeq(GameState gameState, int[] seq, int playerId) {
        if (discountFactor == null) {
            return evalSeqNoDiscount(gameState, seq, playerId);
        } else {
            return evalSeqDiscounted(gameState, seq, playerId, discountFactor);
        }
    }

    /**
     * Evaluates an individual or sequence of actions applying NO discount.
     * @param gameState Current game state to evaluate this sequence from
     * @param seq sequence to evaluate.
     * @param playerId this player's ID
     * @return The value of the state reached at the end of the sequence
     */
    private double evalSeqNoDiscount(GameState gameState, int[] seq, int playerId) {
        double current = rootStateHeuristic.evaluateState(gameState);
        for (int action : seq) {
            //For each action in the sequence, apply the move in 'seq' and a move for all opponents.
            Types.ACTIONS[] allActions = actAllPlayers(gameState, action, playerId);
            gameState.next(allActions);
        }
        double nextScore = rootStateHeuristic.evaluateState(gameState);

        //The state is the difference be value of the initial state and the one found at the end.
        double delta = nextScore - current;
        return delta;
    }

    /**
     * Evaluates an individual or sequence of actions applying discount.
     * @param gameState Current game state to evaluate this sequence from
     * @param seq sequence to evaluate.
     * @param playerId this player's ID
     * @return The value of the state reached at the end of the sequence
     */
    private double evalSeqDiscounted(GameState gameState, int[] seq, int playerId, double discountFactor) {
        double currentScore = rootStateHeuristic.evaluateState(gameState);
        double delta = 0;
        double discount = 1;

        for (int action : seq) {
            Types.ACTIONS[] allActions = actAllPlayers(gameState, action, playerId);
            gameState.next(allActions);
            double nextScore = rootStateHeuristic.evaluateState(gameState);
            double tickDelta = nextScore - currentScore;
            currentScore = nextScore;
            delta += tickDelta * discount;  //Every action of the sequence adds a discount.
            discount *= discountFactor;
        }
        return delta;

    }

    /**
     * Transforms an action to be taken in an array of actions that also includes actions for the opponents.
     * @param gs state to apply the action from.
     * @param myAction action index I want to play.
     * @param playerId my ID
     * @return array of actions including myAction and those the opponents could take.
     */
    private Types.ACTIONS[] actAllPlayers(GameState gs, int myAction, int playerId)
    {
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];
        for(int i = 0; i < nPlayers; ++i)
        {
            if(i == playerId - Types.TILETYPE.AGENT0.getKey())
            {
                //This is me, use my action
                actionsAll[i] = Types.ACTIONS.all().get(myAction);
            }else {
                //This is an action for an opponent.
                actionsAll[i] = opponent.act(gs);
            }
        }
        return actionsAll;
    }

    /**
     * Makes a copy of this object
     * @return an exact copy of this object.
     */
    @Override
    public Player copy() {
        return new SimpleEvoAgent(seed, playerID);
    }


    public String toString() {
        return "SEA: " + nEvals + " : " + sequenceLength + " : " + opponent;
    }

    /** Some setter methods to configure this agent externally **/
    public SimpleEvoAgent setUseShiftBuffer(boolean useShiftBuffer) {
        this.useShiftBuffer = useShiftBuffer;
        return this;
    }

    public SimpleEvoAgent setSequenceLength(int sequenceLength) {
        this.sequenceLength = sequenceLength;
        return this;
    }

    public SimpleEvoAgent setOpponent(Player opponent) {
        this.opponent = opponent;
        return this;
    }

}
