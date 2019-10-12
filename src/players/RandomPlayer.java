package players;

import core.GameState;
import utils.Types;

import java.util.Random;

//This player executes a random action every time
public class RandomPlayer extends Player {
    private Random random;

    public RandomPlayer(long seed, int id) {
        super(seed, id);
        random = new Random(seed);
    }


    @Override
    public Types.ACTIONS act(GameState gs) {
        //nActions = NUM_ACTIONS = 6 - gs.nActions - gs is the class Gamestates
        //random.nextInt(6) - return a random number between 0 and 6
        int actionIdx = random.nextInt(gs.nActions());

        // below Types.ACTIONS.all() an array list and get(actionIdx) return the item at the actionIdx position
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
