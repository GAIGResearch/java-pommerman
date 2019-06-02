package players.rhea.utils;

public class Constants {
    public static final double epsilon = 1e-6;
    public static final int break_ms = 5;

    // Budget type
    public final static int TIME_BUDGET = 0;
    public final static int ITERATION_BUDGET = 1;
    public final static int FM_BUDGET = 2;

    // Genetic operators
    public final static int MUTATION_AND_CROSSOVER = 0;
    public final static int MUTATION_ONLY = 1;
    public final static int CROSSOVER_ONLY = 2;

    public final static int MUTATION_UNIFORM = 0;
    public final static int MUTATION_BIT = 1;
    public final static int MUTATION_BIAS = 2;

    public final static int SELECT_RANK = 0;
    public final static int SELECT_TOURNAMENT = 1;
    public final static int SELECT_ROULETTE = 2;

    public final static int CROSS_UNIFORM = 0;
    public final static int CROSS_ONE_POINT = 1;
    public final static int CROSS_TWO_POINT = 2;

    // Init type
    public final static int INIT_RANDOM = 0;
    public final static int INIT_1SLA = 1;
    public final static int INIT_MCTS = 2;

    // Skip frame types
    public final static int SKIP_REPEAT = 0;
    public final static int SKIP_NULL = 1;
    public final static int SKIP_RANDOM = 2;
    public final static int SKIP_SEQUENCE = 3;

    // Evaluation
    public final static int EVALUATE_ACT_LAST = 0;
    public final static int EVALUATE_ACT_DELTA = 1;
    public final static int EVALUATE_ACT_AVG = 2;
    public final static int EVALUATE_ACT_MIN = 3;
    public final static int EVALUATE_ACT_MAX = 4;
    public final static int EVALUATE_ACT_DISCOUNT = 5;

    public final static int EVALUATE_UPDATE_RAW = 0;
    public final static int EVALUATE_UPDATE_DELTA = 1;
    public final static int EVALUATE_UPDATE_AVERAGE = 2;
    public final static int EVALUATE_UPDATE_MIN = 3;  // Pessimist
    public final static int EVALUATE_UPDATE_MAX = 4;  // Optimist

    // Heuristics
    public final static int WIN_SCORE_HEURISTIC = 0;
    public final static int PLAYER_COUNT_HEURISTIC = 1;
    public final static int CUSTOM_HEURISTIC = 2;
    public final static int ADVANCED_HEURISTIC = 3;
}
