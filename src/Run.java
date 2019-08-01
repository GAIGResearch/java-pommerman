import core.Game;
import players.*;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rhea.RHEAPlayer;
import players.rhea.utils.Constants;
import players.rhea.utils.RHEAParams;
import utils.*;

import java.util.*;

import static utils.Types.VISUALS;

public class Run {

    private static void printHelp()
    {
        System.out.println("Usage: java Run [args]");
        System.out.println("\t [arg index = 0] Game Mode. 0: FFA; 1: TEAM");
        System.out.println("\t [arg index = 1] Repetitions per seed [N]. \"1\" for one game only with visuals.");
        System.out.println("\t [arg index = 2] Vision Range [R]");
        System.out.println("\t [arg index = 3-6] Agents:");
        System.out.println("\t\t 0 DoNothing");
        System.out.println("\t\t 1 Random");
        System.out.println("\t\t 2 OSLA");
        System.out.println("\t\t 3 SimplePlayer");
        System.out.println("\t\t 4 RHEA 200 itereations, shift buffer, pop size 1, random init, length: 12");
        System.out.println("\t\t 5 MCTS 200 iterations, length: 12");
        System.out.println("\t\t 6 Human Player (controls: cursor keys + space bar).");
    }

    public static void main(String[] args) {

        if(args.length != 7) {
            printHelp();
            return;
        }

        try {

            Random rnd = new Random();

            // Create players
            ArrayList<Player> players = new ArrayList<>();
            int playerID = Types.TILETYPE.AGENT0.getKey();
            boolean oneGame = false;
            KeyController kc = new KeyController(true);

            // Init game, size and seed.
            long seed = System.currentTimeMillis();
            int boardSize = Types.BOARD_SIZE;

            Types.GAME_MODE gMode = Types.GAME_MODE.FFA;
            if(Integer.parseInt(args[0]) == 1)
                gMode = Types.GAME_MODE.TEAM;

            Types.DEFAULT_VISION_RANGE = Integer.parseInt(args[2]);

            int N = Integer.parseInt(args[1]);
            long seeds[];

            oneGame = (N == 1);
            if (N == 20)
            {
                //Special case, these seeds are fixed for the experiments in the paper:
                seeds = new long[] {93988, 19067, 64416, 83884, 55636, 27599, 44350, 87872, 40815,
                        11772, 58367, 17546, 75375, 75772, 58237, 30464, 27180, 23643, 67054, 19508};
            }else
            {
                //Otherwise, all seeds are random
                seeds = new long[N];
                for(int i = 0; i < N; i++)
                    seeds[i] = rnd.nextInt(100000);
            }


            String[] playerStr = new String[4];

            for(int i = 3; i <= 6; ++i) {
                int agentType = Integer.parseInt(args[i]);
                Player p = null;


                switch(agentType) {
                    case 0:
                        p = new DoNothingPlayer(playerID++);
                        playerStr[i-3] = "DoNothing";
                        break;
                    case 1:
                        p = new RandomPlayer(seed, playerID++);
                        playerStr[i-3] = "Random";
                        break;
                    case 2:
                        p = new OSLAPlayer(seed, playerID++);
                        playerStr[i-3] = "OSLA";
                        break;
                    case 3:
                        p = new SimplePlayer(seed, playerID++);
                        playerStr[i-3] = "RuleBased";
                        break;
                    case 4:
                        RHEAParams rheaParams = new RHEAParams();
                        rheaParams.budget_type = Constants.ITERATION_BUDGET;
                        rheaParams.iteration_budget = 200;
                        rheaParams.individual_length = 12;
                        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;

                        p = new RHEAPlayer(seed, playerID++, rheaParams);
                        playerStr[i-3] = "RHEA";
                        break;
                    case 5:
                        MCTSParams mctsParams = new MCTSParams();
                        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
                        mctsParams.num_iterations = 200;
                        mctsParams.rollout_depth = 12;

                        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;
                        p = new MCTSPlayer(seed, playerID++, mctsParams);
                        playerStr[i-3] = "MCTS";
                        break;
                    case 6:

                        if(!oneGame)
                        {
                            System.out.println("ERROR: Human players only available for N=1.");
                        }else {

                            p = new HumanPlayer(kc, playerID++);
                            playerStr[i - 3] = "HumanPlayer";
                        }
                        break;
                    default:
                        System.out.println("WARNING: Invalid agent ID: " + agentType );
                }

                players.add(p);
            }

            String gameIdStr = "";
            for(int i = 0; i <= 6; ++i) {
                gameIdStr += args[i];
                if(i != 6)
                    gameIdStr+="-";
            }

            Game game = new Game(seeds[0], boardSize, gMode, gameIdStr);

            // Make sure we have exactly NUM_PLAYERS players
            assert players.size() == Types.NUM_PLAYERS;
            game.setPlayers(players);

            System.out.print(gameIdStr + " [");
            for(int i = 0; i < playerStr.length; ++i) {
                System.out.print(playerStr[i]);
                if(i != playerStr.length-1)
                    System.out.print(',');

            }
            System.out.println("]");

            if(oneGame)
                runGame(game, kc, new KeyController(false), false);
            else
                runGames(game, seeds, N, false);

        } catch(Exception e) {
            e.printStackTrace();
            printHelp();
        }
    }

    /**
     * Runs 1 game.
     * @param g - game to run
     * @param ki1 - primary key controller
     * @param ki2 - secondary key controller
     * @param separateThreads - if separate threads should be used for the agents or not.
     */
    public static void runGame(Game g, KeyController ki1, KeyController ki2, boolean separateThreads) {
        WindowInput wi = null;
        GUI frame = null;
        if (VISUALS) {
            frame = new GUI(g, "Java-Pommerman", ki1, false, true);
            wi = new WindowInput();
            wi.windowClosed = false;
            frame.addWindowListener(wi);
            frame.addKeyListener(ki1);
            frame.addKeyListener(ki2);
        }

        g.run(frame, wi, separateThreads);
    }

    public static void runGames(Game g, long seeds[], int repetitions, boolean useSeparateThreads){
        int numPlayers = g.getPlayers().size();
        int[] winCount = new int[numPlayers];
        int[] tieCount = new int[numPlayers];
        int[] lossCount = new int[numPlayers];
        int numSeeds = seeds.length;
        int totalNgames = numSeeds * repetitions;

        for(int s = 0; s<numSeeds; s++) {

            for (int i = 0; i < repetitions; i++) {
                long seed = seeds[s];

                System.out.print( seed + ", " + (s*repetitions + i) + "/" + totalNgames + ", ");

                g.reset(seed);
                EventsStatistics.REP = i;
                GameLog.REP = i;

                Types.RESULT[] results = g.run(useSeparateThreads);

                for (int pIdx = 0; pIdx < numPlayers; pIdx++) {
                    switch (results[pIdx]) {
                        case WIN:
                            winCount[pIdx]++;
                            break;
                        case TIE:
                            tieCount[pIdx]++;
                            break;
                        case LOSS:
                            lossCount[pIdx]++;
                            break;
                    }
                }
            }
        }

        //Done, show stats
        System.out.println("N \tWin \tTie \tLoss \tPlayer");
        for (int pIdx = 0; pIdx < numPlayers; pIdx++) {
            String player = g.getPlayers().get(pIdx).getClass().toString().replaceFirst("class ", "");

            double winPerc = winCount[pIdx] * 100.0 / (double)totalNgames;
            double tiePerc = tieCount[pIdx] * 100.0 / (double)totalNgames;
            double lossPerc = lossCount[pIdx] * 100.0 / (double)totalNgames;

            System.out.println(totalNgames + "\t" + winPerc + "%\t" + tiePerc + "%\t" + lossPerc + "%\t" + player );
        }
    }
}
