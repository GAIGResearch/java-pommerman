package players.optimisers;

import players.Player;
import players.optimisers.ParameterSet;

public abstract class ParameterizedPlayer extends Player {

    private ParameterSet params;

    /**
     * Default constructor, to be called in subclasses (initializes player ID and random seed for this agent.
     *
     * @param seed - random seed for this player.
     * @param pId  - this player's ID.
     */
    protected ParameterizedPlayer(long seed, int pId) {
        super(seed, pId);
    }

    protected ParameterizedPlayer(long seed, int pId, ParameterSet params) {
        super(seed, pId);
        this.params = params;
    }

    public final void setParameters(ParameterSet params) {
        this.params = params;
    }

    public final ParameterSet getParameters() {
        return params;
    }

    public void translateParameters(int[] a, boolean topLevel) {
        params.translate(a, topLevel);
    }

    public final void setSeed(long seed) {
        this.seed = seed;
    }

}
