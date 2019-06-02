package players.optimisers.evodef;

import core.Game;
import players.DoNothingPlayer;
import players.SimplePlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.optimisers.ParameterizedPlayer;
import players.Player;
import utils.Types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static utils.Types.NUM_PLAYERS;

public class EvaluatePommerman implements NoisySolutionEvaluator, SearchSpace, FitnessSpace {

    private int nDims;
    private ArrayList<Integer> m;

    private double noise;
    private static Random random = new Random();

    private EvolutionLogger logger;
    private ParameterizedPlayer player;

    public EvaluatePommerman(ArrayList<Integer> possibleValues, ParameterizedPlayer player) {
        this(possibleValues, player, 0);
    }

    private EvaluatePommerman(ArrayList<Integer> possibleValues, ParameterizedPlayer player, double noise) {
        this.nDims = possibleValues.size();
        this.m = possibleValues;
        this.noise = noise;
        this.player = player;
        logger = new EvolutionLogger();
        player.getParameters().printParameterSearchSpace();
    }

    @Override
    public void reset() {
        logger.reset();
    }

    @Override
    public Double optimalIfKnown() {
        return 0.0;
    }

    @Override
    public double test(int[] solution) {
        double tot = 0;
        int nReps = 5;

        for (int i = 0; i < nReps; i++) {
            tot += trueFitness(solution);
        }

        return tot/nReps;
    }


    @Override
    public double evaluate(int[] a) {
        // keep track of whether it is truly optimal
        double tot = trueFitness(a);
        boolean isOptimal = isOptimal(a);
        tot += noise * random.nextGaussian();
        logger.log(tot, a, isOptimal);
        return tot;
    }

    @Override
    public Double trueFitness(int[] a) {
        double fit = 0;
        long seed = System.currentTimeMillis();

        // Translate the given parameters, assign them to the player and call the reset() method to make sure all
        // is initialized properly.
        player.translateParameters(a);

        // Create the game
        int boardSize = Types.BOARD_SIZE;
        Game game = new Game(seed, boardSize, Types.GAME_MODE.FFA, "");

        // Run 1 game with our tuned player in each of the 4 starting positions
        for (int i = 0; i < NUM_PLAYERS; i++) {
            // Reset game and player
            game.reset(true);
            player.reset(seed, Types.TILETYPE.AGENT0.getKey() + i);

            // Create player array and put our tuned player in the right position
            Player[] players = new Player[NUM_PLAYERS];
            players[i] = player;

            // Create opponents
            for (int j = 0; j < NUM_PLAYERS; j++) {
                if (j != i) {
                    players[j] = new SimplePlayer(seed, Types.TILETYPE.AGENT0.getKey() + j);
                }
            }

            // Start the game.
            game.setPlayers(new ArrayList<>(Arrays.asList(players)));
            Types.RESULT[] results = game.run(false);
            fit += (results[i] == Types.RESULT.WIN ? 1.0 : (results[0] == Types.RESULT.LOSS ? 0.0 : 0.5));
        }

        // Return result of this player. Use win or loss as fitness
        return fit / NUM_PLAYERS;
    }

    @Override
    public boolean optimalFound() {
        // return false for the noisy optimisation experiments in order
        // to prevent the optimisers from cheating
        return false;
    }

    @Override
    public SearchSpace searchSpace() {
        return this;
    }

    @Override
    public int nEvals() {
        return logger.nEvals();
    }

    @Override
    public EvolutionLogger logger() {
        return logger;
    }


    @Override
    public int nDims() {
        return nDims;
    }

    @Override
    public int nValues(int i) {
        return m.get(i);
    }

    @Override
    public Boolean isOptimal(int[] solution) {
        return false;
    }
}

