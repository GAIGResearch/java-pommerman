package players;

import core.GameState;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.Types;

import java.util.Arrays;
import java.util.Random;

public class SimpleEvoAgent extends Player {

    private Random random;
    public double epsilon = 1e-6;
    private StateHeuristic rootStateHeuristic;


    // these are all the parameters that control the agend
    public boolean flipAtLeastOneValue = true;
    public double mutProb = 0.4;
    public int sequenceLength = 20;
    public int nEvals = 120;
    public boolean useShiftBuffer = true;
    public Double discountFactor = 0.99;


    public SimpleEvoAgent(long seed, int id) {
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
        int action = getAction(gs, playerID);

        return Types.ACTIONS.all().get(action);
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    @Override
    public Player copy() {
        return new SimpleEvoAgent(seed, playerID);
    }



    int[] solution;

    Player opponent = new DoNothingPlayer(0);

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

    public int[] getActions(GameState gameState, int playerId) {
        if (useShiftBuffer && solution != null) {
            solution = shiftLeftAndRandomAppend(solution, gameState.nActions());
        } else {
           // System.out.println("New random solution with nActions = " + gameState.nActions());
            solution = randomPoint(gameState.nActions());
        }

        for (int i = 0; i < nEvals; i++) {
            // evaluate the current one
            int[] mut = mutate(solution, mutProb, gameState.nActions());
            double curScore = evalSeq(gameState.copy(), solution, playerId);
            double mutScore = evalSeq(gameState.copy(), mut, playerId);
            if (mutScore >= curScore) {
                solution = mut;
                // System.out.println(mutScore + " : " + Arrays.toString(solution));
            }
        }

        int[] tmp = solution;
        // nullify if not using a shift buffer
        if (!useShiftBuffer) solution = null;
        return tmp;
    }


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

    private int mutateValue(int cur, int nPossible) {
        // the range is nPossible-1, since we
        // selecting the current value is not allowed
        // therefore we add 1 if the randomly chosen
        // value is greater than or equal to the current value
        if (nPossible <= 1) return cur;
        int rx = random.nextInt(nPossible - 1);
        return rx >= cur ? rx + 1 : rx;
    }

    private int[] randomPoint(int nValues) {
        int[] p = new int[sequenceLength];
        for (int i=0; i<p.length; i++) {
            p[i] = random.nextInt(nValues);
        }
        return p;
    }

    private int[] shiftLeftAndRandomAppend(int[] v, int nActions) {
        int[] p = new int[v.length];
        for (int i = 0; i < p.length - 1; i++) {
            p[i] = v[i + 1];
        }
        p[p.length - 1] = random.nextInt(nActions);
        return p;
    }

    private double evalSeq(GameState gameState, int[] seq, int playerId) {
        if (discountFactor == null) {
            return evalSeqNoDiscount(gameState, seq, playerId);
        } else {
            return evalSeqDiscounted(gameState, seq, playerId, discountFactor);
        }
    }

    private double evalSeqNoDiscount(GameState gameState, int[] seq, int playerId) {
        double current = rootStateHeuristic.evaluateState(gameState);
        for (int action : seq) {

            Types.ACTIONS[] allActions = actAllPlayers(gameState, action, playerId);
            gameState.next(allActions);
        }
        double nextScore = rootStateHeuristic.evaluateState(gameState);
        double delta = nextScore - current;
        return delta;
    }

    private Types.ACTIONS[] actAllPlayers(GameState gs, int myAction, int playerId)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];
        for(int i = 0; i < nPlayers; ++i)
        {
            if(i == playerId - Types.TILETYPE.AGENT0.getKey())
            {
                actionsAll[i] = Types.ACTIONS.all().get(myAction);
            }else {
                actionsAll[i] = opponent.act(gs);
            }
        }
        return actionsAll;
    }

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
            delta += tickDelta * discount;
            discount *= discountFactor;
        }
        return delta;

    }

    public String toString() {
        return "SEA: " + nEvals + " : " + sequenceLength + " : " + opponent;
    }

    public int getAction(GameState gameState, int playerId) {
        return getActions(gameState, playerId)[0];
    }

}
