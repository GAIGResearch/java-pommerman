package players.heuristics;

import core.GameState;
import utils.Types;

public class CustomHeuristic extends StateHeuristic {
    private BoardStats rootBoardStats;

    public CustomHeuristic(GameState root) {
        rootBoardStats = new BoardStats(root);
    }

    @Override
    public double evaluateState(GameState gs) {
        boolean gameOver = gs.isTerminal();
        Types.RESULT win = gs.winner();

        // Compute a score relative to the root's state.
        BoardStats lastBoardState = new BoardStats(gs);
        double rawScore = rootBoardStats.score(lastBoardState);

        if(gameOver && win == Types.RESULT.LOSS)
            rawScore = -1;

        if(gameOver && win == Types.RESULT.WIN)
            rawScore = 1;

        return rawScore;
    }

    public static class BoardStats
    {
        int tick, nTeammates, nEnemies, blastStrength;
        boolean canKick;
        int nWoods;
        static double maxWoods = -1;
        static double maxBlastStrength = 10;

        double FACTOR_ENEMY;
        double FACTOR_TEAM;
        double FACTOR_WOODS = 0.1;
        double FACTOR_CANKCIK = 0.15;
        double FACTOR_BLAST = 0.15;

        BoardStats(GameState gs) {
            nEnemies = gs.getAliveEnemyIDs().size();

            // Init weights based on game mode
            if (gs.getGameMode() == Types.GAME_MODE.FFA) {
                FACTOR_TEAM = 0;
                FACTOR_ENEMY = 0.5;
            } else {
                FACTOR_TEAM = 0.1;
                FACTOR_ENEMY = 0.4;
                nTeammates = gs.getAliveTeammateIDs().size();  // We only need to know the alive teammates in team modes
                nEnemies -= 1;  // In team modes there's an extra Dummy agent added that we don't need to care about
            }

            // Save game state information
            this.tick = gs.getTick();
            this.blastStrength = gs.getBlastStrength();
            this.canKick = gs.canKick();

            // Count the number of wood walls
            this.nWoods = 1;
            for (Types.TILETYPE[] gameObjectsTypes : gs.getBoard()) {
                for (Types.TILETYPE gameObjectType : gameObjectsTypes) {
                    if (gameObjectType == Types.TILETYPE.WOOD)
                        nWoods++;
                }
            }
            if (maxWoods == -1) {
                maxWoods = nWoods;
            }
        }

        /**
         * Computes score for a game, in relation to the initial state at the root.
         * Minimizes number of opponents in the game and number of wood walls. Maximizes blast strength and
         * number of teammates, wants to kick.
         * @param futureState the stats of the board at the end of the rollout.
         * @return a score [0, 1]
         */
        double score(BoardStats futureState)
        {
            int diffTeammates = futureState.nTeammates - this.nTeammates;
            int diffEnemies = - (futureState.nEnemies - this.nEnemies);
            int diffWoods = - (futureState.nWoods - this.nWoods);
            int diffCanKick = futureState.canKick ? 1 : 0;
            int diffBlastStrength = futureState.blastStrength - this.blastStrength;

            return (diffEnemies / 3.0) * FACTOR_ENEMY + diffTeammates * FACTOR_TEAM + (diffWoods / maxWoods) * FACTOR_WOODS
                    + diffCanKick * FACTOR_CANKCIK + (diffBlastStrength / maxBlastStrength) * FACTOR_BLAST;
        }
    }
}
