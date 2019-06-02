package players;

import core.GameState;
import utils.Types;

import java.util.Random;

public class RandomPlayer extends Player {
    private Random random;

    public RandomPlayer(long seed, int id) {
        super(seed, id);
        random = new Random(seed);
    }


    @Override
    public Types.ACTIONS act(GameState gs) {
        int actionIdx = random.nextInt(gs.nActions());
        return Types.ACTIONS.all().get(actionIdx);
    }

    @Override
    public Player copy() {
        return new RandomPlayer(seed, playerID);
    }
}
