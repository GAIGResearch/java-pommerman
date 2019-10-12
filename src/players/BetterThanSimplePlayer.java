package players;

import utils.Types;

import java.util.Random;

public class BetterThanSimplePlayer extends Player {
    private Random random; // intializing a random seed for this agent

    public BetterThanSimplePlayer(long seed, int id){
        super(seed, id);
        random = new Random(seed);
    }

    @Override
    public Types.ACTIONS


}
