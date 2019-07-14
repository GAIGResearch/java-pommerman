package players.optimisers.ntbea;

import core.Game;
import players.Player;
import players.mcts.MCTSPlayer;
import players.optimisers.ParameterizedPlayer;
import players.optimisers.evodef.EvaluatePommerman;
import players.rhea.RHEAPlayer;
import players.rhea.utils.RHEAParams;
import utils.Types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static utils.Types.NUM_PLAYERS;

/**
 * Run a simple test
 */

public class RunPlayerSolution {
    public static void main(String[] args) {
        long seed = System.currentTimeMillis();

        RHEAParams parameterSet = new RHEAParams();
        ParameterizedPlayer player = new RHEAPlayer(0, 0, parameterSet);

//        MCTSParams parameterSet = new MCTSParams();
//        ParameterizedPlayer player = new MCTSPlayer(0, 0, parameterSet);

        // Set parameter genome
        int[] solution = new int[]{1, 1, 1, 1, 0, 5, 1, 1, 0, 0, 0, 0};
        boolean topLevel = true;
        player.getParameters().translate(solution, topLevel);
//        player.getParameters().printParameters();

        // Create the game
        int boardSize = Types.BOARD_SIZE;
        Game game = new Game(seed, boardSize, Types.GAME_MODE.FFA, "");

        // Run 1 game with our tuned player in each of the 4 starting positions
        for (int i = 0; i < NUM_PLAYERS; i++) {
            // Reset game and player. Needed to make sure player uses the right parameters and game is in starting state
            game.reset(true);
            player.reset(seed, Types.TILETYPE.AGENT0.getKey() + i);

            // Create player array and put our tuned player in the right position
            Player[] players = new Player[NUM_PLAYERS];
            players[i] = player;

            // Create opponents
            for (int j = 0; j < NUM_PLAYERS; j++) {
                if (j != i) {
                    players[j] = new MCTSPlayer(seed, Types.TILETYPE.AGENT0.getKey() + j);
                }
            }

            // Start the game.
            game.setPlayers(new ArrayList<>(Arrays.asList(players)));
            game.run(false);
        }
    }
}

