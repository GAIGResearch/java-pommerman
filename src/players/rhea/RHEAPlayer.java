package players.rhea;

import core.GameState;
import players.optimisers.ParameterizedPlayer;
import players.Player;
import players.rhea.utils.RHEAParams;
import utils.ElapsedCpuTimer;
import utils.Types;

import java.util.Random;
import static players.rhea.utils.Constants.TIME_BUDGET;

public class RHEAPlayer extends ParameterizedPlayer {
    private RollingHorizonPlayer player;
    private GameInterface gInterface;
    private RHEAParams params;

    public RHEAPlayer(long seed, int playerID) {
        this(seed, playerID, new RHEAParams());
    }

    public RHEAPlayer(long seed, int playerID, RHEAParams params) {
        super(seed, playerID, params);
        reset(seed, playerID);
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);

        // Make sure we have parameters
        this.params = (RHEAParams) getParameters();
        if (this.params == null) {
            this.params = new RHEAParams();
            super.setParameters(this.params);
        }

        // Set up random generator
        Random randomGenerator = new Random(seed);

        // Create interface with game
        gInterface = new GameInterface(this.params, randomGenerator, playerID - Types.TILETYPE.AGENT0.getKey());

        // Set up player
        player = new RollingHorizonPlayer(randomGenerator, this.params, gInterface);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        ElapsedCpuTimer elapsedTimer = null;
        if (params.budget_type == TIME_BUDGET) {
            elapsedTimer = new ElapsedCpuTimer();
            elapsedTimer.setMaxTimeMillis(params.time_budget);
        }
        setup(gs, elapsedTimer);
        return gInterface.translate(player.getAction(elapsedTimer, gs.nActions()));
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    private void setup(GameState rootState, ElapsedCpuTimer elapsedTimer) {
        gInterface.initTick(rootState, elapsedTimer);
    }

    @Override
    public Player copy() {
        return new RHEAPlayer(seed, playerID, params);
    }
}
