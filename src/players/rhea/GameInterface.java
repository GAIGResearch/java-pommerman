package players.rhea;

import core.GameState;
import gnu.trove.set.hash.TIntHashSet;
import players.heuristics.*;
import players.rhea.evo.Individual;
import players.rhea.evo.Mutation;
import players.rhea.hybrids.MCTSNode;
import players.rhea.utils.FMBudget;
import players.rhea.utils.RHEAParams;
import players.rhea.utils.Utilities;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static players.rhea.utils.Constants.*;
import static players.rhea.utils.Utilities.*;

public class GameInterface {

    private StateHeuristic stateHeuristic;
    private FMBudget fmBudget;
    private GameState rootState;
    private RHEAParams params;
    private int playerID;
    private Random random;

    private ElapsedCpuTimer elapsedTimer;
    private HashMap<Integer, Types.ACTIONS> action_mapping;

    private static double[] bounds = new double[]{-1, 1};

    GameInterface(RHEAParams params, Random random, int playerID) {
        this.params = params;
        this.random = random;
        fmBudget = new FMBudget(params.fm_budget);
        this.playerID = playerID;
    }

    void initTick(GameState stateObs, ElapsedCpuTimer elapsedTimer) {
        rootState = stateObs;
        this.elapsedTimer = elapsedTimer;
        fmBudget.reset();
        initStateInfo();
        switch (params.heurisic_type) {
            case PLAYER_COUNT_HEURISTIC: stateHeuristic = new PlayerCountHeuristic(); break;
            case CUSTOM_HEURISTIC: stateHeuristic = new CustomHeuristic(stateObs); break;
            case ADVANCED_HEURISTIC: stateHeuristic = new AdvancedHeuristic(stateObs, random); break;
            default:
            case WIN_SCORE_HEURISTIC: stateHeuristic = new WinScoreHeuristic(); break;
        }

    }

    /**
     * Seed individual using given seeding type.
     * @param ind - individual to change actions of.
     * @param type - type of seeding.
     */
    public void seed(Individual ind, int type) {
        double[][] seed_distribution;
        if (type == INIT_1SLA) {
            seed_distribution = seed_1sla();
        } else if (type == INIT_MCTS) {
            seed_distribution = seed_mcts();
        } else {
            // Random is default
            seed_distribution = Utilities.getRandomDistribution(params.individual_length, rootState.nActions());
        }
        ind.set_actions(Individual.sample_individual(seed_distribution, random).get_actions());
    }

    /**
     * Creates an action distribution determined by a One Step Look Ahead agent.
     * @return action distribution
     */
    private double[][] seed_1sla() {
        int max_actions = rootState.nActions();
        double[][] distribution = new double[params.individual_length][max_actions];

        GameState so = rootState.copy();
        Types.ACTIONS bestAction;
        int bestActionIdx;
        double maxQ;

        for (int k = 0; k < params.individual_length; k++) {
            bestAction = null;
            bestActionIdx = -1;
            maxQ = Double.NEGATIVE_INFINITY;

            if (!so.isTerminal()) {
                ArrayList<Types.ACTIONS> actions = Types.ACTIONS.all();
                int nActions = actions.size();
                for (int j = 0; j < nActions; j++) {
                    Types.ACTIONS action = actions.get(j);
                    GameState stCopy = so.copy();
                    advanceState(stCopy, action);
                    double Q = evaluateState(stCopy);
                    Q = Utils.noise(Q, epsilon, random.nextDouble());

                    //System.out.println("Action:" + action + " score:" + Q);
                    if (Q > maxQ) {
                        maxQ = Q;
                        bestAction = action;
                        bestActionIdx = j;
                    }
                }

                distribution[k][bestActionIdx] = 1;
                advanceState(so, bestAction);
            }
        }

        // Inform budget of usage
        fmBudget.use(params.individual_length * max_actions);
        return distribution;
    }

    /**
     * Creates action distribution given by a Monte Carlo Tree Search player.
     * @return action distribution.
     */
    private double[][] seed_mcts() {
        int nActions = rootState.nActions();

        Types.ACTIONS[] actions = Types.ACTIONS.all().toArray(new Types.ACTIONS[0]);
        MCTSNode m_root = new MCTSNode(random, nActions, actions, stateHeuristic, this);
        MCTSNode.rootState = rootState;

        // Do the search within the available budget.
        m_root.mctsSearch(elapsedTimer, params.mcts_fm_budget, params.mcts_iteration_budget, params.mcts_depth);

        // Compress the tree into action probabilities at each level
        double[][] distribution = m_root.compressTree(params.individual_length, nActions);

        // Inform budget of usage
        fmBudget.use(params.mcts_fm_budget);
        return distribution;
    }

    /**
     * Evaluate given individual with given mutation class.
     * @param individual - individual to evaluate.
     * @param mutation - mutation class to perform gene mutation.
     * @return value of individual.
     */
    @SuppressWarnings("UnusedReturnValue")
    public double evaluate(Individual individual, Mutation mutation, int evaluation_update) {
        double[] values = new double[individual.get_length() + 1];
        GameState stateObsCopy = rootState.copy();
        if (params.evaluate_act == EVALUATE_ACT_LAST) {  // This doesn't need first state value
            values[0] = 0;
        } else {
            values[0] = evaluateState(stateObsCopy);  // Evaluate current state
        }

        // Evaluate subsequent states obtained by rolling through the actions
        int lastIdx = evaluateRollout(values, stateObsCopy, individual.get_length(), individual, mutation);

        if (lastIdx < values.length - 1) {
            // We stopped early, trim the values array to remove trailing 0s
            values = Arrays.copyOfRange(values, 0, lastIdx + 1);
        }

        // Get the value of the rollout according to the evaluation method.
        double state_value = getRolloutValue(values);

        // We may need to do extra rollouts from the end of the state reached previously, if not terminal.
        if (params.mc_rollouts && !stateObsCopy.isTerminal()) {
            state_value = MCrollouts(stateObsCopy, values);
        }

        // Update value according to update rule
        double update_value;

        switch(evaluation_update) {
            case EVALUATE_UPDATE_DELTA: update_value = individual.get_value() - state_value; break;
            case EVALUATE_UPDATE_AVERAGE: update_value = (individual.get_value() + state_value) / 2; break;
            case EVALUATE_UPDATE_MIN: update_value = Math.min(individual.get_value(), state_value); break;
            case EVALUATE_UPDATE_MAX: update_value = Math.max(individual.get_value(), state_value); break;
            default:
            case EVALUATE_UPDATE_RAW: update_value = state_value;
        }

        // Set the individual's value and return it
        individual.set_value(update_value);
        return update_value;
    }

    /**
     * Evaluates an individual by rolling the state forward through the actions
     * @param values - array in which we'll save state values for every action we pass through
     * @param copy - copy of root game state
     * @param length - length of this rollout
     * @param individual - individual that should be used for the rollout. If null, we're doing random rollout.
     * @param mutation - mutation class containing information about genes which should be mutated for this individual,
     *                 used during rollout to modify genes if needed.
     * @return index of last action reached. may terminate early if a terminal state is reached before the end
     * of the rollout length.
     */
    private int evaluateRollout(double[] values, GameState copy, int length, Individual individual,
                                Mutation mutation) {
        // Keep track of where the rollout stopped (in case of early terminal state).
        int lastIdx = 0;

        // Retrieve the list of genes to mutate.
        TIntHashSet genesToMutate = null;
        if (mutation != null) {
            genesToMutate = mutation.getGenesToMutate();
        }

        // Roll through the actions
        for (int i = 0; i < length; i++) {
            // Stop if the state reached is terminal
            if (!copy.isTerminal()) {
                if (individual != null) {
                    // Mutate gene if needed to a new random value.
                    if (genesToMutate != null && genesToMutate.contains(i)) {
                        mutation.mutateGeneToNewValue(individual, i);
                    }
                    // Advance the state with the action in the individual
                    advanceState(copy, action_mapping.get(individual.get_action(i)));

                } else {  // No individual passed, doing random rollout
                    ArrayList<Types.ACTIONS> acts = Types.ACTIONS.all();
                    int bound = rootState.nActions();
                    Types.ACTIONS action = Types.ACTIONS.ACTION_STOP;
                    if (bound > 0) {
                        action = acts.get(random.nextInt(bound));
                    }
                    advanceState(copy, action);
                }

                // Signal we used 1 FM call
                fmBudget.use();

                // Save the value of this state in the values array and update lastIdx reached.
                if ((params.evaluate_act == EVALUATE_ACT_DELTA || params.evaluate_act == EVALUATE_ACT_LAST)
                        && (i != length - 1)) {  // This only needs last state evaluated, speed up execution
                    values[i + 1] = 0;
                } else {  // In all other cases we need all intermediate state values.
                    values[i + 1] = evaluateState(copy);
                }
                lastIdx = i;
            } else {
                break;
            }
        }
        if (lastIdx < length - 1) {
            // Broke out of the loop early, end of game
            values[lastIdx + 1] = evaluateState(copy);
        }
        lastIdx++;

        return lastIdx;
    }

    /**
     * Returns the value of a rollout given an array with values in each state rolled through, according to the
     * evaluation model in the parameters.
     * @param values - list of all values of states passed through while doing the rollout.
     * @return value of this rollout
     */
    private double getRolloutValue(double[] values) {
        double state_value;
        int length = values.length;
        switch(params.evaluate_act) {
            case EVALUATE_ACT_DELTA:
                state_value = values[length - 1] - values[0];
                break;
            case EVALUATE_ACT_AVG:
                state_value = get_avg(values);
                break;
            case EVALUATE_ACT_MIN:
                state_value = get_min(values);
                break;
            case EVALUATE_ACT_MAX:
                state_value = get_max(values);
                break;
            case EVALUATE_ACT_DISCOUNT:
                state_value = 0;
                for (int i = 0; i < length; i++) {
                    state_value += Math.pow(params.evaluate_discount, i) * values[i];
                }
                break;
            default:
            case EVALUATE_ACT_LAST:
                state_value = values[length - 1];
                break;
        }

        return state_value;
    }

    /**
     * Performs monte carlo rollouts from the given state.
     * @param start - root state for MC rollouts
     * @param ind_values - array of values from individual we just evaluated.
     * @return value of individual at the end of the MC rollouts.
     */
    private double MCrollouts(GameState start, double[] ind_values) {
        double reward = 0;

        // We may average over multiple repetitions of rollouts
        for (int k = 0; k < params.mc_rollouts_repeat; k++) {
            GameState first = start.copy();

            // Save values of states we pass through in values array
            double[] values = new double[params.mc_rollouts_length + 1];
            if (params.evaluate_act != EVALUATE_ACT_LAST) {  // This doesn't need first state value
                values[0] = evaluateState(first);  // Evaluate current state
            }

            // Passing null as individual and mutation to perform random rollout
            int lastIdx = evaluateRollout(values, first, params.mc_rollouts_length, null, null);

            if (lastIdx < values.length - 1) {
                // We may have terminated the rollout earlier due to reaching terminal state
                values = Arrays.copyOfRange(values, 0, lastIdx + 1);
            }

            // Use both individual values and rollout values to determine reward
            double thisReward = combineAndNormalize(ind_values, values);

            reward += thisReward;
        }

        // Returned value is average over all rollout repetitions
        reward /= params.mc_rollouts_repeat;

        return reward;
    }

    private double combineAndNormalize(double[] values, double[] values_extension) {
        double state_value = 0;

        if (values_extension != null) {
            if (params.evaluate_act != EVALUATE_ACT_LAST) {
                // We combine the 2 arrays if we need to and the 2nd isn't null.
                values = Utilities.add_array_to_array(values, values_extension, 0);
            } else {
                // Apply evaluation rule with only rollout values
                state_value = getRolloutValue(values_extension);
                state_value = Utils.normalise(state_value, bounds[0], bounds[1]);
            }
        }

        if (values_extension == null || params.evaluate_act != EVALUATE_ACT_LAST) {
            // Apply evaluation rule
            state_value = getRolloutValue(values);
            state_value = Utils.normalise(state_value, bounds[0], bounds[1]);
        }

        return state_value;
    }

    /**
     * Checks if enough budget is remaining for another iteration.
     * @param elapsedCpuTimer - timer, if time budget
     * @param iterationsRemaining - number of iterations remaining, if iteration budget
     * @param fmBudget - FM budget tracker, if FM budget
     * @return true if enough budget, false otherwise
     */
    public boolean budget(ElapsedCpuTimer elapsedCpuTimer, int iterationsRemaining, FMBudget fmBudget) {
        boolean gotBudget = true;
        if (params.budget_type == TIME_BUDGET) {
            gotBudget = elapsedCpuTimer.enoughBudgetIteration(break_ms);
        } else if (params.budget_type == ITERATION_BUDGET) {
            gotBudget = iterationsRemaining > 0;
        } else if (params.budget_type == FM_BUDGET) {
            if (fmBudget != null) {
                gotBudget = fmBudget.enoughBudgetIteration();
            } else {
                gotBudget = this.fmBudget.enoughBudgetIteration();
            }
        }
        return gotBudget;
    }

    public void endIteration(ElapsedCpuTimer elapsedCpuTimer, FMBudget fmBudget) {
        if (params.budget_type == TIME_BUDGET) {
            elapsedCpuTimer.endIteration();
        } else if (params.budget_type == FM_BUDGET) {
            if (fmBudget != null) {
                fmBudget.endIteration();
            } else {
                this.fmBudget.endIteration();
            }
        }
    }

    /**
     * Initializes action mappings.
     */
    private void initStateInfo() {
        ArrayList<Types.ACTIONS> availableActions = Types.ACTIONS.all();
        int max_actions = availableActions.size();
        action_mapping = new HashMap<>();
        for (int i = 0; i < max_actions; i++) {
            action_mapping.put(i, availableActions.get(i));
        }
        action_mapping.put(max_actions, Types.ACTIONS.ACTION_STOP);
    }


    /**
     * Advances the state with given action and chosen opponent model
     * @param gs - current game state
     * @param action - action for this player
     */
    public void advanceState(GameState gs, Types.ACTIONS action) {
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[nPlayers];

        for (int i = 0; i < nPlayers; ++i) {
            if (playerID == i) {
                actionsAll[i] = action;
            } else {
                actionsAll[i] = opponentModel(gs);
            }
        }

        gs.next(actionsAll);
    }

    /**
     * Retrieves the action of an opponent.
     * @param gs - current game state.
     * @return action for opponent.
     */
    private Types.ACTIONS opponentModel(GameState gs) {
        return _random_model(gs.nActions());
//        return _stop_model();
    }

    /**
     * Random opponent model, returns random action within limits.
     * @param nActions - number of available actions.
     * @return - random action.
     */
    @SuppressWarnings("unused")
    private Types.ACTIONS _random_model(int nActions) {
        int actionIdx = random.nextInt(nActions);
        return Types.ACTIONS.all().get(actionIdx);
    }

    /**
     * Do nothing opponent model.
     * @return - ACTION_STOP always.
     */
    @SuppressWarnings("unused")
    private Types.ACTIONS _stop_model() {
        return Types.ACTIONS.ACTION_STOP;
    }

    /**
     * Function to evaluate a state. Calls heuristic with the player ID.
     * @param a_gameState - state to evaluate.
     * @return value of given state.
     */
    public double evaluateState(GameState a_gameState) {
        return stateHeuristic.evaluateState(a_gameState);
    }

    /**
     * Translates action evolved by EA (int) to game action (Types.ACTIONS) according to inner mapping
     * @param action - int action evolved by EA
     * @return game action corresponding to int action
     */
    Types.ACTIONS translate(int action) {
        return action_mapping.get(action);
    }
}
