import core.Game;
import players.*;
import utils.Types;
import players.rhea.utils.Constants;
import objects.Avatar;
import players.mcts.MCTSPlayer;
import players.mcts.MCTSParams;
import players.rhea.RHEAPlayer;
import players.rhea.utils.RHEAParams;
import utils.*;


import java.util.ArrayList;

public class Test {

    public static void main(String[] args) {

        // Game parameters
        long seed = System.currentTimeMillis();
        int boardSize = Types.BOARD_SIZE;
        Types.GAME_MODE gameMode = Types.GAME_MODE.FFA;
        boolean useSeparateThreads = false;

        Game game = new Game(seed, boardSize, Types.GAME_MODE.FFA, "");

        // Key controllers for human player s (up to 2 so far).
        KeyController ki1 = new KeyController(true);
        KeyController ki2 = new KeyController(false);

        // Create parameters for players
//        MCTSParams mctsParams = new MCTSParams();
//        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
//        RHEAParams rheaParams = new RHEAParams();
//        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;

        // Create players
        ArrayList<Player> players = new ArrayList<>();
        int playerID = Types.TILETYPE.AGENT0.getKey();

        MCTSParams mctsParams = new MCTSParams();
        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;

        RHEAParams rheaParams = new RHEAParams();
        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;

        Types.DEFAULT_VISION_RANGE = 1;

//        players.add(new HumanPlayer(ki1, playerID++));
//        players.add(new DoNothingPlayer(playerID++));
//        players.add(new OSLAPlayer(seed, playerID++));
//        players.add(new DoNothingPlayer(playerID++));
        players.add(new MCTSPlayer(seed, playerID++, mctsParams));

//        players.add(new HumanPlayer(ki1, playerID++));

//        players.add(new DoNothingPlayer(playerID++));

//        players.add(new HumanPlayer(ki2, playerID++));


//        players.add(new OSLAPlayer(seed, playerID++));
//        players.add(new OSLAPlayer(seed, playerID++));
//        players.add(new OSLAPlayer(seed, playerID++));

//        players.add(new RandomPlayer(seed, playerID++));
//        players.add(new RandomPlayer(seed, playerID++));
//        players.add(new RandomPlayer(seed, playerID++));
//        players.add(new RandomPlayer(seed, playerID++));
//        players.add(new SimplePlayer(seed, playerID++));
//        players.add(new RHEAPlayer(seed, playerID++, rheaParams));

        players.add(new SimplePlayer(seed, playerID++));
        players.add(new RHEAPlayer(seed, playerID++, rheaParams));
//        players.add(new SimplePlayer(seed, playerID++));
        players.add(new SimplePlayer(seed, playerID++));
//
//        players.add(new RHEAPlayer(seed, playerID++, rheaParams));
//        players.add(new MCTSPlayer(seed, playerID++, mctsParams));
//        players.add(new MCTSPlayer(seed, playerID++, mctsParams));
//        players.add(new RHEAPlayer(seed, playerID++, rheaParams));
//        players.add(new RHEAPlayer(seed, playerID++, rheaParams));

//        players.add(new DoNothingPlayer(playerID++));
//        players.add(new DoNothingPlayer(playerID++));

        // Make sure we have exactly NUM_PLAYERS players
        assert players.size() == Types.NUM_PLAYERS;
        game.setPlayers(players);

        Run.runGame(game, ki1, ki2, useSeparateThreads);
        
        /* Run the replay: */
//        if (game.isLogged()){
//            game = Game.getReplayGame();
//            Run.runGame(game, ki1, ki2, useSeparateThreads);
//        }

        /* Run with no visuals, N Times: */
//        int N = 20;
//        Run.runGames(game, N, useSeparateThreads, false);

    }

}
