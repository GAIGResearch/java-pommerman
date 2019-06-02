package players;

import core.GameState;
import utils.Types;

public class TestingPlayer extends SimplePlayer {

    private GameState gs;

    public TestingPlayer(long seed, int id) {
        super(seed, id);
    }

    public GameState getGameState(){
        return gs;
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        this.gs = gs;
        return super.act(gs);
    }
}
