package players.heuristics;

import core.GameState;

public abstract class StateHeuristic {
    public abstract double evaluateState(GameState gs);
    public abstract double evaluateRaw(GameState gs);
}
