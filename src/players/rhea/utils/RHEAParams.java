package players.rhea.utils;

import players.optimisers.ParameterSet;
import utils.Pair;

import java.util.*;

import static players.rhea.utils.Constants.*;

public class RHEAParams implements ParameterSet {

    // EA structure modules settings
    public int genetic_operator = MUTATION_AND_CROSSOVER;
    public int mutation_type = MUTATION_UNIFORM;
    public int selection_type = SELECT_TOURNAMENT;
    public int crossover_type = CROSS_ONE_POINT;
    public int init_type = INIT_RANDOM;
    public boolean elitism = false;
    public boolean keep_parents_next_gen = true;
    private double mcts_budget_perc = 0.5;

    // Efficiency settings
    public int frame_skip = 0;
    public int frame_skip_type = SKIP_SEQUENCE;

    // EA parameters
    public int population_size = 1;
    public int individual_length = 12;
    public int mcts_depth = 12;
    public int gene_size = 1;  // Should be a divisor of individual_length. A gene may contain more than 1 action.
    public int offspring_count = 1;
    public int no_elites = 1;
    private double tournament_size_perc = 0.4;  // Percent of population that would take part in tournament selection
    public int mutation_gene_count = 1;
    public double mutation_rate = 0.3;

    // Evaluation settings
    public int evaluate_act = EVALUATE_ACT_LAST;
    public int evaluate_update = EVALUATE_UPDATE_AVERAGE;
    public double evaluate_discount = 0.99;
    public int heurisic_type = CUSTOM_HEURISTIC;
    public boolean reevaluate_pop = true;

    // Shift settings
    public boolean shift_buffer = true;
//    public double shift_discount = 0.99;

    // MC Rollouts settings
    public boolean mc_rollouts = false;
    private double mc_rollouts_length_perc = 0.5;
    public int mc_rollouts_repeat = 1;

    // Budget restrictions
    public int budget_type = ITERATION_BUDGET;//FM_BUDGET;
    public int iteration_budget = 200;
    public int fm_budget = 2000;
    public int time_budget = 40;

    // Don't change these directly. Use updateDependentVariables method instead.
    public int mcts_fm_budget = (int) (fm_budget * mcts_budget_perc);
    public int mcts_iteration_budget = (int) (iteration_budget * mcts_budget_perc);
    public int tournament_size = (int) Math.min(2, population_size * tournament_size_perc);
    public int mc_rollouts_length = (int) (individual_length * mc_rollouts_length_perc);

    public Map<String, Object[]> getParameterValues() {
        HashMap<String, Object[]> parameterValues = new HashMap<>();
        parameterValues.put("genetic_operator", new Integer[]{MUTATION_AND_CROSSOVER, MUTATION_ONLY, CROSSOVER_ONLY});
        parameterValues.put("mutation_type", new Integer[]{MUTATION_UNIFORM, MUTATION_BIT, MUTATION_BIAS});
        parameterValues.put("selection_type", new Integer[]{SELECT_RANK, SELECT_TOURNAMENT, SELECT_ROULETTE});
        parameterValues.put("crossover_type", new Integer[]{CROSS_UNIFORM, CROSS_ONE_POINT, CROSS_TWO_POINT});
        parameterValues.put("init_type", new Integer[]{INIT_RANDOM, INIT_1SLA, INIT_MCTS});
        parameterValues.put("elitism", new Boolean[]{false, true});
        parameterValues.put("keep_parents_next_gen", new Boolean[]{false, true});

//        parameterValues.put("budget_type", new Integer[]{TIME_BUDGET, ITERATION_BUDGET, FM_BUDGET});
//        parameterValues.put("iteration_budget", new Integer[]{100, 200, 500});
//        parameterValues.put("fm_budget", new Integer[]{500, 1000, 2000, 5000});
        parameterValues.put("mcts_budget_perc", new Double[]{0.25, 0.5, 0.75});

        parameterValues.put("frame_skip", new Integer[]{0, 5, 10});
        parameterValues.put("frame_skip_type", new Integer[]{SKIP_REPEAT, SKIP_NULL, SKIP_RANDOM, SKIP_SEQUENCE});

        parameterValues.put("population_size", new Integer[]{1, 2, 5, 10, 15, 20});
        parameterValues.put("individual_length", new Integer[]{5, 10, 12, 15, 20});
        parameterValues.put("mcts_depth", new Integer[]{5, 10, 12, 15, 20});
        parameterValues.put("gene_size", new Integer[]{1, 2, 3, 4, 5});
        parameterValues.put("offspring_count", new Integer[]{1, 2, 5, 10, 15, 20});
        parameterValues.put("no_elites", new Integer[]{1, 2, 3});
        parameterValues.put("tournament_size_perc", new Double[]{0.2, 0.5, 0.7});
        parameterValues.put("mutation_gene_count", new Integer[]{1, 2, 3, 4, 5});
        parameterValues.put("mutation_rate", new Double[]{0.1, 0.3, 0.5, 0.7, 0.9});

        parameterValues.put("evaluate_act", new Integer[]{EVALUATE_ACT_LAST, EVALUATE_ACT_DELTA, EVALUATE_ACT_AVG,
                EVALUATE_ACT_MIN, EVALUATE_ACT_MAX, EVALUATE_ACT_DISCOUNT});
//        parameterValues.put("evaluate_update", new Integer[]{EVALUATE_UPDATE_RAW, EVALUATE_UPDATE_DELTA,
//                EVALUATE_UPDATE_AVERAGE, EVALUATE_UPDATE_MIN, EVALUATE_UPDATE_MAX});
        parameterValues.put("evaluate_discount", new Double[]{0.9, 0.95, 0.99, 1.0});
        parameterValues.put("heuristic_type", new Integer[]{WIN_SCORE_HEURISTIC, PLAYER_COUNT_HEURISTIC,
                CUSTOM_HEURISTIC, ADVANCED_HEURISTIC});

        parameterValues.put("shift_buffer", new Boolean[]{false, true});
//        parameterValues.put("shift_discount", new Double[]{0.9, 0.95, 0.99, 1.0});

        parameterValues.put("mc_rollouts", new Boolean[]{false, true});
        parameterValues.put("mc_rollouts_length_perc", new Double[]{0.25, 0.5, 0.75, 1.0, 2.0});
        parameterValues.put("mc_rollouts_repeat", new Integer[]{1, 5, 10});

        return parameterValues;
    }

    /**
     * Parameters are in tree structure. This method returns children of given parameter.
     * Children parameters are parameters which only matter if parent has a certain value.
     * Maps values of parent to children that become relevant when parent takes that value.
     * @param parameter - given parent
     * @return - list of children
     */
    public Map<Object,ArrayList<String>> getParameterChildren(String parameter) {
        Map<Object,ArrayList<String>> values = new HashMap<>();
        ArrayList<String> children;

        switch(parameter) {
//            case "budget_type":
//                children.clear();
//                children.add("fm_budget");
//                values.put(FM_BUDGET, children);
//                break;
//            case "shift_buffer":
//                children.clear();
//                children.add("shift_discount");
//                values.put(true, children);
//                break;
            case "mc_rollouts":
                children = new ArrayList<>();
                children.add("mc_rollouts_length_perc");
                children.add("mc_rollouts_repeat");
                values.put(true, children);
                break;
            case "evaluate_act":
                children = new ArrayList<>();
                children.add("evaluate_discount");
                values.put(EVALUATE_ACT_DISCOUNT, children);
                break;
            case "frame_skip":
                children = new ArrayList<>();
                children.add("frame_skip_type");
                values.put(5, children);
                values.put(10, children);
                break;
            case "elitism":
                children = new ArrayList<>();
                children.add("no_elites");
                values.put(true, children);
                break;
            case "init_type":
                children = new ArrayList<>();
                children.add("mcts_budget_perc");
                children.add("mcts_depth");
                values.put(INIT_MCTS, children);
                break;
            case "genetic_operator":
                children = new ArrayList<>();
                children.add("mutation_type");
                values.put(MUTATION_ONLY, children);

                children = new ArrayList<>();
                children.add("selection_type");
                children.add("crossover_type");
                children.add("mutation_type");
                values.put(MUTATION_AND_CROSSOVER, children);

                children = new ArrayList<>();
                children.add("selection_type");
                children.add("crossover_type");
                values.put(CROSSOVER_ONLY, children);
                break;
            case "selection_type":
                children = new ArrayList<>();
                children.add("tournament_size_perc");
                values.put(SELECT_TOURNAMENT, children);
                break;
            case "mutation_type":
                children = new ArrayList<>();
                children.add("mutation_gene_count");
                values.put(MUTATION_BIT, children);

                children = new ArrayList<>();
                children.add("mutation_rate");
                values.put(MUTATION_UNIFORM, children);
        }
        return values;
    }

    /**
     * Parameters are in tree structure. This method returns parent of given parameter.
     * Parent parameters are parameters which makes given child parameter matter only if parent has a certain value.
     * Maps parent to values which would make this child relevant.
     * Reverse of previous method
     * @param parameter - given child
     * @return - parent
     */
    public Pair<String, ArrayList<Object>> getParameterParent(String parameter) {
        ArrayList<Object> values = new ArrayList<>();

        switch(parameter) {
//            case "iteration_budget":
//                values.add(ITERATION_BUDGET);
//                return new Pair<>("budget_type", values);
//            case "fm_budget":
//                values.add(FM_BUDGET);
//                return new Pair<>("budget_type", values);
//            case "shift_discount":
//                values.add(true);
//                return new Pair<>("shift_buffer", values);
            case "mc_rollouts_length_perc":
                values.add(true);
                return new Pair<>("mc_rollouts", values);
            case "mc_rollouts_repeat":
                values.add(true);
                return new Pair<>("mc_rollouts", values);
            case "evaluate_discount":
                values.add(EVALUATE_ACT_DISCOUNT);
                return new Pair<>("evaluate_act", values);
            case "frame_skip_type":
                values.add(5);
                values.add(10);
                return new Pair<>("frame_skip", values);
            case "no_elites":
                values.add(true);
                return new Pair<>("elitism", values);
            case "tournament_size_perc":
                values.add(SELECT_TOURNAMENT);
                return new Pair<>("selection_type", values);
            case "mcts_budget_perc":
                values.add(INIT_MCTS);
                return new Pair<>("init_type", values);
            case "mcts_depth":
                values.add(INIT_MCTS);
                return new Pair<>("init_type", values);
            case "selection_type":
                values.add(CROSSOVER_ONLY);
                values.add(MUTATION_AND_CROSSOVER);
                return new Pair<>("genetic_operator", values);
            case "crossover_type":
                values.add(CROSSOVER_ONLY);
                values.add(MUTATION_AND_CROSSOVER);
                return new Pair<>("genetic_operator", values);
            case "mutation_type":
                values.add(MUTATION_ONLY);
                values.add(MUTATION_AND_CROSSOVER);
                return new Pair<>("genetic_operator", values);
            case "mutation_rate":
                values.add(MUTATION_UNIFORM);
                return new Pair<>("mutation_type", values);
            case "mutation_gene_count":
                values.add(MUTATION_BIT);
                return new Pair<>("mutation_type", values);
        }
        return null;
    }

    public void setParameterValues(HashMap<String, Object> parameterValues) {
        for (Map.Entry<String, Object> e : parameterValues.entrySet()) {
            setParameterValue(e.getKey(), e.getValue());
        }
    }

    @Override
    public void setParameterValue(String name, Object value) {
        switch(name) {
            case "genetic_operator": genetic_operator = (int) value; break;
            case "mutation_type": mutation_type = (int) value; break;
            case "selection_type": selection_type = (int) value; break;
            case "crossover_type": crossover_type = (int) value; break;
            case "init_type": init_type = (int) value; break;
            case "elitism": elitism = (boolean) value; break;
            case "keep_parents_next_gen": keep_parents_next_gen = (boolean) value; break;

//            case "budget_type": budget_type = (int) value; break;
//            case "iteration_budget": iteration_budget = (int) value; break;
//            case "fm_budget": fm_budget = (int) value; break;
            case "mcts_budget_perc": mcts_budget_perc = (double) value; break;

            case "frame_skip": frame_skip = (int) value; break;
            case "frame_skip_type": frame_skip_type = (int) value; break;

            case "population_size": population_size = (int) value; break;
            case "individual_length": individual_length = (int) value; break;
            case "mcts_depth": mcts_depth = (int) value; break;
            case "gene_size": gene_size = (int) value; break;
            case "offspring_count": offspring_count = (int) value; break;
            case "no_elites": no_elites = (int) value; break;
            case "tournament_size_perc": tournament_size_perc = (double) value; break;
            case "mutation_gene_count": mutation_gene_count = (int) value; break;
            case "mutation_rate": mutation_rate = (double) value; break;

            case "evaluate_act": evaluate_act = (int) value; break;
//            case "evaluate_update": evaluate_update = (int) value; break;
            case "evaluate_discount": evaluate_discount = (double) value; break;
            case "heuristic_type": heurisic_type = (int) value; break;

            case "shift_buffer": shift_buffer = (boolean) value; break;
//            case "shift_discount": shift_discount = (double) value; break;

            case "mc_rollouts": mc_rollouts = (boolean) value; break;
            case "mc_rollouts_length_perc": mc_rollouts_length_perc = (double) value; break;
            case "mc_rollouts_repeat": mc_rollouts_repeat = (int) value; break;
        }
        updateDependentVariables();
    }

    @Override
    public Object getParameterValue(String name) {
        switch(name) {
            case "genetic_operator": return genetic_operator;
            case "mutation_type": return mutation_type;
            case "selection_type": return selection_type;
            case "crossover_type": return crossover_type;
            case "init_type": return init_type;
            case "elitism": return elitism;
            case "keep_parents_next_gen": return keep_parents_next_gen;

//            case "budget_type": budget_type = (int) value; break;
//            case "iteration_budget": iteration_budget = (int) value; break;
//            case "fm_budget": fm_budget = (int) value; break;
            case "mcts_budget_perc": return mcts_budget_perc;

            case "frame_skip": return frame_skip;
            case "frame_skip_type": return frame_skip_type;

            case "population_size": return population_size;
            case "individual_length": return individual_length;
            case "mcts_depth": return mcts_depth;
            case "gene_size": return gene_size;
            case "offspring_count": return offspring_count;
            case "no_elites": return no_elites;
            case "tournament_size_perc": return tournament_size_perc;
            case "mutation_gene_count": return mutation_gene_count;
            case "mutation_rate": return mutation_rate;

            case "evaluate_act": return evaluate_act;
//            case "evaluate_update": return evaluate_update;
            case "evaluate_discount": return evaluate_discount;
            case "heuristic_type": return heurisic_type;

            case "shift_buffer": return shift_buffer;
//            case "shift_discount": return shift_discount;

            case "mc_rollouts": return mc_rollouts;
            case "mc_rollouts_length_perc": return mc_rollouts_length_perc;
            case "mc_rollouts_repeat": return mc_rollouts_repeat;
        }
        return null;
    }

    public ArrayList<String> getParameters() {
        ArrayList<String> paramList = new ArrayList<>();
        paramList.add("genetic_operator");
        paramList.add("mutation_type");
        paramList.add("selection_type");
        paramList.add("crossover_type");
        paramList.add("init_type");
        paramList.add("elitism");
        paramList.add("keep_parents_next_gen");

//        paramList.add("budget_type");
//        paramList.add("iteration_budget");
//        paramList.add("fm_budget");
        paramList.add("mcts_budget_perc");

        paramList.add("frame_skip");
        paramList.add("frame_skip_type");

        paramList.add("population_size");
        paramList.add("individual_length");
        paramList.add("mcts_depth");
        paramList.add("gene_size");
        paramList.add("offspring_count");
        paramList.add("no_elites");
        paramList.add("tournament_size_perc");
        paramList.add("mutation_gene_count");
        paramList.add("mutation_rate");

        paramList.add("shift_buffer");
//        paramList.add("shift_discount");

        paramList.add("evaluate_act");
//        paramList.add("evaluate_update");
        paramList.add("evaluate_discount");

        paramList.add("mc_rollouts");
        paramList.add("mc_rollouts_length_perc");
        paramList.add("mc_rollouts_repeat");

        return paramList;
    }

    /**
     * Updates the variables used by RHEA which are set from other parameters.
     * Should call this whenever these parameters are changed.
     */
    private void updateDependentVariables() {
        mcts_fm_budget = (int) (fm_budget * mcts_budget_perc);
        mcts_iteration_budget = (int) (iteration_budget * mcts_budget_perc);
        tournament_size = (int) Math.min(2, population_size * tournament_size_perc);
        mc_rollouts_length = (int) (individual_length * mc_rollouts_length_perc);
    }

    @Override
    public Map<String, String[]> constantNames() {
        HashMap<String, String[]> names = new HashMap<>();
        names.put("genetic_operator", new String[]{"MUTATION_AND_CROSSOVER", "MUTATION_ONLY", "CROSSOVER_ONLY"});
        names.put("evaluate_act", new String[]{"EVALUATE_ACT_LAST", "EVALUATE_ACT_DELTA", "EVALUATE_ACT_AVG",
                "EVALUATE_ACT_MIN", "EVALUATE_ACT_MAX", "EVALUATE_ACT_DISCOUNT"});
//        names.put("evaluate_update", new String[]{"EVALUATE_UPDATE_RAW", "EVALUATE_UPDATE_DELTA",
//                "EVALUATE_UPDATE_AVERAGE", "EVALUATE_UPDATE_MIN", "EVALUATE_UPDATE_MAX"});
        names.put("budget_type", new String[]{"TIME_BUDGET", "ITERATION_BUDGET", "FM_BUDGET"});
        names.put("mutation_operator", new String[]{"MUTATION_AND_CROSSOVER", "MUTATION_ONLY", "CROSSOVER_ONLY"});
        names.put("mutation_type", new String[]{"MUTATION_UNIFORM", "MUTATION_BIT", "MUTATION_BIAS"});
        names.put("selection_type", new String[]{"SELECT_RANK", "SELECT_TOURNAMENT", "SELECT_ROULETTE"});
        names.put("crossover_type", new String[]{"CROSS_UNIFORM", "CROSS_ONE_POINT", "CROSS_TWO_POINT"});
        names.put("init_type", new String[]{"INIT_RANDOM", "INIT_1SLA", "INIT_MCTS"});
        names.put("frame_skip_type", new String[]{"SKIP_REPEAT", "SKIP_NULL", "SKIP_RANDOM", "SKIP_SEQUENCE"});
        names.put("heuristic_type", new String[]{"WIN_SCORE_HEURISTIC", "PLAYER_COUNT_HEURISTIC", "CUSTOM_HEURISTIC",
                "ADVANCED_HEURISTIC"});
//        names.put("draw_code", new String[]{"DRAW_EXPLORATION", "DRAW_THINKING", "DRAW_ALL"});
        return names;
    }

    public static void main(String[] args) {
        new RHEAParams().printParameterSearchSpace();
    }
}
