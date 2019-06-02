package players.heuristics;

import core.GameState;
import utils.Types;

public class PlayerCountHeuristic extends StateHeuristic {

    @Override
    public double evaluateState(GameState gs) {
        double value;

        if(gs.winner() == Types.RESULT.WIN)
            value = 1;
        else if (gs.winner() == Types.RESULT.LOSS)
            value = -1;
        else if (gs.winner() == Types.RESULT.TIE)
            value = 0.5;
        else {
            int pCount = 0;
            for (Types.TILETYPE id : gs.getAliveAgentIDs()) {
                if (id.getKey() != gs.getPlayerId()) {
                    pCount++;
                }
            }
            value = 1.0 / pCount;
        }

        return value;
    }
}
