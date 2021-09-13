package players.heuristics;

import core.GameState;
import utils.Types;

public class WinScoreHeuristic extends StateHeuristic {
    @Override
    public double evaluateState(GameState gs) {
        if (gs.winner() == Types.RESULT.LOSS) {
            return -1;
        } else if (gs.winner() == Types.RESULT.WIN)
            return 1;
        else if (gs.winner() == Types.RESULT.TIE)
            return 0.5;
        return 0;
    }
}
