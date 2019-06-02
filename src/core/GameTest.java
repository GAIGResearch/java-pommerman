package core;

import objects.Avatar;
import objects.GameObject;
import org.junit.jupiter.api.Test;
import players.*;
import utils.Types;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static utils.Types.VERBOSE;

class GameTest {

    @Test
    void getReplayGame(){
        Game replay = Game.getReplayGame();
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
    void playerSpecificGameStateObservations(){
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
            game.tick(); //This has to come after the gs is copied, because the old gs is stored in the test player
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
                                System.out.println("player pos "+pos);
                            }
                            assertNotEquals(5, value);
                        } else { // The part of the board which IS supposed to be fogged out
                            if (5 != value) {
                                System.out.println("no fog at (" + i + " " + i1 + ") where it's supposed to be");
                                System.out.println("player pos "+pos);
                            }
                            assertEquals(5, value);

                            int[][] bombLife = pgs.model.getBombLife();
                            if (bombLife[i][i1] != 0){
                                System.out.println("Bomb life info should not be made available to the player");
                            }
                            assertEquals(0, bombLife[i][i1]);

                            int[][] blastStrength = pgs.model.getBombBlastStrength();
                            if (blastStrength[i][i1] != 0){
                                System.out.println("Blast strength info should not be made available to the player");
                            }
                            assertEquals(0, blastStrength[i][i1]);
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
                game.tick();
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
     * This tests whether the agents doing the exact same sequence of actions on the exact same map
     * always leads to the exact same intermediate steps and outcomes
     */
    @Test
    void tick() {
        int seed = 123456;
        int boardSize = 11;
        Game game = new Game(seed, boardSize, Types.GAME_MODE.FFA, "");
        int numberOfCopies = 10000;

        ArrayList<Player> players = new ArrayList<>();
        int playerID = Types.TILETYPE.AGENT0.getKey();
        players.add(new SimonSaysPlayer(playerID++));
        players.add(new SimonSaysPlayer(playerID++));
        players.add(new SimonSaysPlayer(playerID++));
        players.add(new SimonSaysPlayer(playerID++));

        game.setPlayers(players);

        Avatar avatar1 = (Avatar)game.getAliveAvatars(-1).get(0);
        Avatar avatar2 = (Avatar)game.getAliveAvatars(-1).get(1);
        avatar1.setCanKick();
        avatar2.setCanKick();

        List<Game> gameList = new ArrayList<>(numberOfCopies);
        for (int i = 0; i < numberOfCopies; i++) {
            gameList.add(game.copy());
        }

        GameState[][] stateArray = new GameState[SimonSaysPlayer.defaultSequenceLength()][gameList.size()];
        for (int i = 0; i < SimonSaysPlayer.defaultSequenceLength(); i++) {
            for (int i1 = 0; i1 < gameList.size(); i1++) {
                game = gameList.get(i);
                if (!game.isEnded()) {
                    game.tick();
                    if (VERBOSE) {
                        game.printBoard();
                    }
                }
                // Add resulting state hash to array
                stateArray[i][i1] = game.getGameState().copy();
            }
        }

        //Check that all outcomes are equal
        for (GameState[] gameStates : stateArray) {
            assertArrayEquals(stateArray[0], gameStates);
        }
    }
}