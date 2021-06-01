package core;

import objects.Avatar;
import objects.GameObject;
import org.junit.jupiter.api.Test;
import players.*;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rhea.RHEAPlayer;
import players.rhea.utils.Constants;
import players.rhea.utils.RHEAParams;
import utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static utils.Types.VERBOSE;

class GameTest {

    @Test
    void getReplayGame(){
        Game replay = Game.getLastReplayGame();
        System.out.println(replay.getGameState());
    }

    /**
     * This method tests whether (in PO games) the players only have access to the restricted info
     * about the game they are playing.
     *
     * - There should be no info of what's on the board outside this range. This includes bomb life and strength.
     * - There should be no fog inside this range.
     * - Vision range of the player should instead be dependent on Avatar getVisionRange().
     */
    @Test
    void playerSpecificGameStateObservations() {
        int seed = 123456;
        int boardSize = 11;
        Game game;
        ArrayList<Player> players = new ArrayList<>();
        game = new Game(seed, boardSize, Types.GAME_MODE.FFA, "");
        players.clear();

        int playerID = Types.TILETYPE.AGENT0.getKey();
        players.add(new TestingPlayer(new Random().nextLong(), playerID++));
        players.add(new TestingPlayer(new Random().nextLong(), playerID++));
        players.add(new TestingPlayer(new Random().nextLong(), playerID++));
        players.add(new TestingPlayer(new Random().nextLong(), playerID++));
        game.setPlayers(players);

        while (!game.isEnded()) {
            GameState gs = game.getGameState().copy();
            game.tick(false); //This has to come after the gs is copied, because the old gs is stored in the test player
            for (Player player : game.getPlayers()) {
                TestingPlayer tp = (TestingPlayer)player;
                GameState pgs = tp.getGameState();
                Types.TILETYPE[][] board = pgs.getBoard();
                System.out.println(pgs.toString());
                System.out.println(gs.toString());

                assertNotEquals(pgs, gs); // First of all, these should not be the same

                int pid = player.getPlayerID();
                Vector2d pos = null;
                Avatar avatar = null;
                for (GameObject agent : gs.getAgents()) {
                    if (pid == agent.getType().getKey()){
                        pos = agent.getPosition();
                        avatar = (Avatar)agent;
                    }
                }
                Vector2d observedPos = null;
                for (GameObject agent : pgs.getAgents()) {
                    if (pid == agent.getType().getKey())
                        observedPos = agent.getPosition();
                }
                assertEquals(pos, observedPos);

                // standard viewRadius is +/- 4 tiles
                int radius = 4;
                if (avatar != null) // IF we managed to find the avatar, use its vision range instead of the default.
                    radius = avatar.getVisionRange();

                if (radius != -1 && pos != null) {
                    int xmin = Math.max(0, pos.x-radius);
                    int xmax = Math.min(board.length, pos.x+radius);
                    int ymin = Math.max(0, pos.y-radius);
                    int ymax = Math.min(board[0].length, pos.y+radius);

                    for (int i = xmin; i < xmax; i++) {
                        for (int i1 = ymin; i1 < ymax; i1++) {
                            assertNotEquals(5, board[i][i1].getKey());
                        }
                    }
                    for (int i = 0; i < board.length; i++) {
                        for (int i1 = 0; i1 < board[i].length; i1++) {

                            int value = board[i][i1].getKey();
                            // The part of the board which is not supposed to be fogged out
                            if (xmax >= i && i >= xmin && ymax >= i1 && i1 >= ymin) {
                                if (5 == value) {
                                    System.out.println("fog at (" + i + " " + i1 + ") where it's not supposed to be");
                                    System.out.println("player pos " + pos);
                                }
                                assertNotEquals(5, value);
                            } else { // The part of the board which IS supposed to be fogged out
                                if (5 != value) {
                                    System.out.println("no fog at (" + i + " " + i1 + ") where it's supposed to be");
                                    System.out.println("player pos " + pos);
                                }
                                assertEquals(5, value);

                                int[][] bombLife = pgs.model.getBombLife();
                                if (bombLife[i][i1] != 0) {
                                    System.out.println("Bomb life info should not be made available to the player");
                                }
                                assertEquals(0, bombLife[i][i1]);

                                int[][] blastStrength = pgs.model.getBombBlastStrength();
                                if (blastStrength[i][i1] != 0) {
                                    System.out.println("Blast strength info should not be made available to the player");
                                }
                                assertEquals(0, blastStrength[i][i1]);
                            }
                        }
                    }
                }
            }
            if (VERBOSE) {
                game.printBoard();
            }
        }
    }

    /**
     * This method tests how much time is spent per game tick() on average over many games,
     * by simulating random players (taking little computation power, but some) going against each other
     */
    @Test
    void timePerTick(){
        int numberOfSimulations = 100000; //The amount of games to simulate
        double timeSpent = 0;
        int steps = 0;

        int seed = 123456;
        int boardSize = 11;
        Game game;
        ArrayList<Player> players = new ArrayList<>();
        for (int i = 0; i < numberOfSimulations; i++) {
            game = new Game(seed, boardSize, Types.GAME_MODE.FFA, "");
            players.clear();

            int playerID = Types.TILETYPE.AGENT0.getKey();
            players.add(new RandomPlayer(new Random().nextLong(), playerID++));
            players.add(new RandomPlayer(new Random().nextLong(), playerID++));
            players.add(new RandomPlayer(new Random().nextLong(), playerID++));
            players.add(new RandomPlayer(new Random().nextLong(), playerID++));
            game.setPlayers(players);

            long start = System.currentTimeMillis();
            while (!game.isEnded()) {
                game.tick(false);
                steps++;
                if (VERBOSE) {
                    game.printBoard();
                }
            }
            long end = System.currentTimeMillis() - start;
            timeSpent += end;
        }
        System.out.println("Total ticks: "+ steps);
        System.out.println("Total time spent in game loop: "+timeSpent+" millis");
        double avg = timeSpent/steps;
        System.out.println("Avg time per tick: "+ avg +" millis");
        double stepsPerSecond = (1/avg)*1000;
        System.out.println("Avg ticks per second: "+stepsPerSecond);

    }

    /**
     * Test whether given a full length game with smart agents,
     * many copies of the resulting replay game all play out the same.
     * Does NOT test if the replays play the same as the original game.
     * If this test passes but determinismTick fails, then there is probably some error in logging,
     * but probably not in engine determinism.
     *
     */
    @Test
    void copyDeterminism() {
        // Game parameters
        long seed = System.currentTimeMillis();
        int boardSize = Types.BOARD_SIZE;
        Types.GAME_MODE gameMode = Types.GAME_MODE.FFA;
        boolean useSeparateThreads = false;

        Game game = new Game(seed, boardSize, gameMode, "");
        game.setLogGame(true);

        // Create players
        ArrayList<Player> players = new ArrayList<>();
        int playerID = Types.TILETYPE.AGENT0.getKey();

        MCTSParams mctsParams = new MCTSParams();
        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;

        RHEAParams rheaParams = new RHEAParams();
        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;

        players.add(new MCTSPlayer(seed, playerID++, mctsParams));
        players.add(new SimplePlayer(seed, playerID++));
        players.add(new RHEAPlayer(seed, playerID++, rheaParams));
        players.add(new SimplePlayer(seed, playerID++));

        // Make sure we have exactly NUM_PLAYERS players
        assert players.size() == Types.NUM_PLAYERS : "There should be " + Types.NUM_PLAYERS +
                " added to the game, but there are " + players.size();

        //Assign players and run the game.
        game.setPlayers(players);

        Game copy = game.copy();
        Types.MAX_GAME_TICKS = 800;
        Game.LOG_GAME = true;
        Types.SAVE_GAME_REPLAY = true;
        //Run a single game with the players
        game.run(null, null, useSeparateThreads);
        System.out.println("game length: "+game.getTick());
        GameLog log = game.getGameLog();
        GameLog deserializedLog = GameLog.deserializeLastJSON();
        assertEquals(log, deserializedLog); //Logs are the same before and after (de)serialization

        Game replayGame = game.getReplayGame();
        Game deserializedReplayGame = Game.getLastReplayGame();
        assertEquals(replayGame.getGameState(), deserializedReplayGame.getGameState());

        int numberOfCopies = 100;
        List<Game> gameList = new ArrayList<>(numberOfCopies);
        for (int i = 0; i < numberOfCopies; i++) {
            gameList.add(replayGame.copy());
        }

        replayGame.run(null,null, useSeparateThreads);
        deserializedReplayGame.run(null, null, useSeparateThreads);
        assertEquals(replayGame.getGameState(), deserializedReplayGame.getGameState()); //The two replay games finish in same state
        System.out.println("Replay game length: "+replayGame.getTick());

        GameState[][] stateArray = new GameState[gameList.size()][replayGame.getTick()];
        for (int i = 0; i < gameList.size(); i++) {
            replayGame = gameList.get(i);
            while(!replayGame.isEnded()) {
                replayGame.tick(false);
                if (VERBOSE) {
                    game.printBoard();
                }
                // Add resulting state hash to array
                stateArray[i][replayGame.getTick()-1] = replayGame.getGameState().copy();
            }
        }

        //Check that all outcomes are equal
        for (GameState[] gameStates : stateArray) {
            assertArrayEquals(stateArray[0], gameStates);
        }
    }

    /**
     * This test checks whether the game logs show exactly the same as the original game,
     * step by step
     */
    @Test
    void logCorrectness() {
        // Game parameters
        long seed = System.currentTimeMillis();
        int boardSize = Types.BOARD_SIZE;
        Types.GAME_MODE gameMode = Types.GAME_MODE.FFA;
        boolean useSeparateThreads = false;



        // Create players
        int playerID = Types.TILETYPE.AGENT0.getKey();

        MCTSParams mctsParams = new MCTSParams();
        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;

        RHEAParams rheaParams = new RHEAParams();
        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;



        // Make sure we have exactly NUM_PLAYERS players


        //Assign players and run the game.
        int tests = 5;

        for (int n = 0; n < tests; n++) {
            ArrayList<Player> players = new ArrayList<>();
            players.add(new MCTSPlayer(seed, playerID++, mctsParams));
            players.add(new SimplePlayer(seed, playerID++));
            players.add(new RHEAPlayer(seed, playerID++, rheaParams));
            players.add(new SimplePlayer(seed, playerID++));

            assert players.size() == Types.NUM_PLAYERS : "There should be " + Types.NUM_PLAYERS +
                    " added to the game, but there are " + players.size();

            Game game = new Game(seed, boardSize, gameMode, "");
            game.setLogGame(true);
            game.setPlayers(players);

            Types.MAX_GAME_TICKS = 800;
            //Run a single game with the players
            List<GameState> stateList = new ArrayList<>();

            stateList.add(game.getGameState().copy());
            while(!game.isEnded()) {
                game.tick(false);
                if (VERBOSE) {
                    game.printBoard();
                }
                // Add resulting state hash to array
                stateList.add(game.getGameState().copy());
            }


            List<GameState> replayStateList = new ArrayList<>();
            Game replayGame = game.getReplayGame();
            replayStateList.add(replayGame.getGameState().copy());
            while(!replayGame.isEnded()) {
                replayGame.tick(false);
                if (VERBOSE) {
                    replayGame.printBoard();
                }
                // Add resulting state hash to array
                replayStateList.add(replayGame.getGameState().copy());
            }

            //assertEquals(stateList.size(), replayStateList.size());
            for (int i = 0; i < stateList.size(); i++) {
                GameState original = stateList.get(i);
                GameState replay = replayStateList.get(i);
                if (!original.equals(replay)){
                    System.out.println("ORIGINAL: ");
                    System.out.println(stateList.get(i));
                    System.out.println("REPLAY: ");
                    System.out.println(replayStateList.get(i));
                    System.out.println();
                }
                assertEquals(stateList.get(i), replayStateList.get(i));
            }
        }

    }

    /**
     * This tests whether the replay is the same as the original game,
     * when the game is played by agents that don't use the forward model.
     * If the replays are wrong, then it's not only because FM using players screw with the game state of the real game.
     */
    @Test
    void determinismTickNoForwardModel() {
        // Game parameters
        long seed = System.currentTimeMillis();
        int boardSize = Types.BOARD_SIZE;
        Types.GAME_MODE gameMode = Types.GAME_MODE.FFA;
        boolean useSeparateThreads = false;

        Game game = new Game(seed, boardSize, gameMode, "");
        game.setLogGame(true);

        // Create players
        ArrayList<Player> players = new ArrayList<>();
        int playerID = Types.TILETYPE.AGENT0.getKey();

        MCTSParams mctsParams = new MCTSParams();
        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;

        RHEAParams rheaParams = new RHEAParams();
        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;

        //players.add(new MCTSPlayer(seed, playerID++, mctsParams));
        //players.add(new RHEAPlayer(seed, playerID++, rheaParams));
        //
        players.add(new SimplePlayer(seed, playerID++));
        players.add(new SimplePlayer(seed, playerID++));
        players.add(new SimplePlayer(seed, playerID++));
        players.add(new SimplePlayer(seed, playerID++));

        // Make sure we have exactly NUM_PLAYERS players
        assert players.size() == Types.NUM_PLAYERS : "There should be " + Types.NUM_PLAYERS +
                " added to the game, but there are " + players.size();


        //Assign players and run the game.
        game.setPlayers(players);

        Game copy = game.copy();
        Types.MAX_GAME_TICKS = 400;
        //Run a single game with the players
        game.run(null, null, useSeparateThreads);
        GameLog log = game.getGameLog();
        GameLog deserializedLog = GameLog.deserializeLastJSON();
        assertEquals(log, deserializedLog); //Logs are the same before and after (de)serialization


        /* Uncomment to run the replay of the previous game: */
        if (game.isLogged()){
            Game replayGame = game.getReplayGame();
            Game deserializedReplayGame = Game.getLastReplayGame();
            assertEquals(replayGame.getGameState(), deserializedReplayGame.getGameState()); //Replay games from fresh and deserialized log are the same

            replayGame.run(null,null, useSeparateThreads);
            deserializedReplayGame.run(null, null, useSeparateThreads);
            assertEquals(replayGame.getGameState(), deserializedReplayGame.getGameState()); //The two replay games finish in same state
            /*
            If replaygame and deserialized replay game finish the same but game does not finish the same,
            then there is probably an error in how the action data is stored to the game logs,
            not in how the logs are serialized and deserialized.
            */


            assertEquals(game.getGameState(), replayGame.getGameState()); // Fresh replay finishes in same state as real game

            assertEquals(game.getGameState(), deserializedReplayGame.getGameState()); // Deserialized replay finishes in same state as real game

        }


        /* Run with no visuals, N Times: */
//        int N = 20;
//        Run.runGames(game, new long[]{seed}, N, useSeparateThreads);
    }

    /**
     * This tests whether the replay is the same as the original game,
     * when the game is played by agents that DO use the forward model.
     * If the replay is right, then the players most likely do not screw with the real model when using the FM.
     */
    @Test
    void determinismTickForwardModelUsed() {
        // Game parameters
        long seed = System.currentTimeMillis();
        int boardSize = Types.BOARD_SIZE;
        Types.GAME_MODE gameMode = Types.GAME_MODE.FFA;
        boolean useSeparateThreads = false;

        Game game = new Game(seed, boardSize, gameMode, "");
        game.setLogGame(true);

        // Create players
        ArrayList<Player> players = new ArrayList<>();
        int playerID = Types.TILETYPE.AGENT0.getKey();

        MCTSParams mctsParams = new MCTSParams();
        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;

        RHEAParams rheaParams = new RHEAParams();
        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;

        players.add(new MCTSPlayer(seed, playerID++, mctsParams));
        players.add(new RHEAPlayer(seed, playerID++, rheaParams));

        players.add(new SimplePlayer(seed, playerID++));
        players.add(new SimplePlayer(seed, playerID++));
        //players.add(new SimplePlayer(seed, playerID++));
        //players.add(new SimplePlayer(seed, playerID++));

        // Make sure we have exactly NUM_PLAYERS players
        assert players.size() == Types.NUM_PLAYERS : "There should be " + Types.NUM_PLAYERS +
                " added to the game, but there are " + players.size();


        //Assign players and run the game.
        game.setPlayers(players);

        Game copy = game.copy();
        Types.MAX_GAME_TICKS = 400;
        //Run a single game with the players
        game.run(null, null, useSeparateThreads);
        GameLog log = game.getGameLog();
        GameLog deserializedLog = GameLog.deserializeLastJSON();
        assertEquals(log, deserializedLog); //Logs are the same before and after (de)serialization


        /* Uncomment to run the replay of the previous game: */
        if (game.isLogged()){
            Game replayGame = game.getReplayGame();
            Game deserializedReplayGame = Game.getLastReplayGame();
            assertEquals(replayGame.getGameState(), deserializedReplayGame.getGameState()); //Replay games from fresh and deserialized log are the same

            replayGame.run(null,null, useSeparateThreads);
            deserializedReplayGame.run(null, null, useSeparateThreads);
            assertEquals(replayGame.getGameState(), deserializedReplayGame.getGameState()); //The two replay games finish in same state
            /*
            If replaygame and deserialized replay game finish the same but game does not finish the same,
            then there is probably an error in how the action data is stored to the game logs,
            not in how the logs are serialized and deserialized.
            */

            assertEquals(game.getGameState(), replayGame.getGameState()); // Fresh replay finishes in same state as real game
            assertEquals(game.getGameState(), deserializedReplayGame.getGameState()); // Deserialized replay finishes in same state as real game
        }
    }
}