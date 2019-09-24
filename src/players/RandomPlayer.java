package players;

import core.GameState;
import utils.Types;

import java.util.Random;

public class RandomPlayer extends Player {
    private Random random;

    public RandomPlayer(long seed, int id) {
        super(seed, id);
        reset(seed, id);
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);
        random = new Random(seed);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        int actionIdx = random.nextInt(gs.nActions());
        return Types.ACTIONS.all().get(actionIdx);
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    @Override
    public Player copy() {
        return new RandomPlayer(seed, playerID);
    }
}
