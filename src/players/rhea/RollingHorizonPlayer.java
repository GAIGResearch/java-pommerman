package players.rhea;

import players.rhea.evo.Evolution;
import players.rhea.utils.RHEAParams;
import utils.ElapsedCpuTimer;

import java.util.*;

import static players.rhea.utils.Constants.*;

class RollingHorizonPlayer {
    private Random randomGenerator;
    private RHEAParams params;
    private GameInterface gameInterface;

    private int[] actionBuffer;
    private boolean newBuffer;

    private Evolution ea;


    RollingHorizonPlayer(Random randomGenerator, RHEAParams params, GameInterface gInterface) {
        this.gameInterface = gInterface;
        this.randomGenerator = randomGenerator;
        this.params = params;
        actionBuffer = new int[params.frame_skip];  // This buffer will hold list of actions to be executed
        Arrays.fill(actionBuffer, -1);
        newBuffer = true;
        ea = new Evolution(params, randomGenerator, gameInterface);
    }

    int getAction(ElapsedCpuTimer elapsedTimer, int max_actions) {
        // Init if we ran out of actions in the buffer
        if (newBuffer) {
            ea.init(max_actions);
        }

//        System.out.println();
        // Find best next action within the allowed budget
        int action = max_actions;
        while (gameInterface.budget(elapsedTimer, params.iteration_budget - ea.getNIterations(),
                null)) {
            action = ea.iteration();
            gameInterface.endIteration(elapsedTimer, null);
        }
//        System.out.println(Arrays.toString(ea.population));

        // Play next action in the action buffer
        for (int i = 0; i < params.frame_skip; i++) {
            int act = actionBuffer[i];
            if (act != -1) {
                actionBuffer[i] = -1;
                newBuffer = false;
                return act;
            }
        }

        // Not returned yet, all elements in array are null. So use new action to fill up action buffer.
        newBuffer = true;
        for (int i = 0; i < params.frame_skip; i++) {
            // If type is SKIP_NULL, only first action will be the new action, the rest in the buffer will be null.
            if (i == 0 || params.frame_skip_type == SKIP_REPEAT) {
                actionBuffer[i] = action;
            } else if (params.frame_skip_type == SKIP_RANDOM) {
                actionBuffer[i] = randomGenerator.nextInt(max_actions);
            } else if (params.frame_skip_type == SKIP_SEQUENCE && i < params.individual_length) {  // Follow best sequence found
                actionBuffer[i] = getBestAction(i);
            } else { // Default is play null action
                actionBuffer[i] = max_actions;
            }
        }

        return action;
    }

    private int getBestAction(int idx) {
        return ea.getBestAction(idx);
    }
}
