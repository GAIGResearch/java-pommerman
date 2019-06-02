package players.optimisers.evodef;

/**
 * Created by simonmarklucas on 06/08/2016.
 *
 * Evaluates solutions and logs fitness improvement
 *
 *
 */
public interface SolutionEvaluator {
    // call reset before running
    public void reset();
    double evaluate(int[] solution);

    // has the algorithm found the optimal solution?
    boolean optimalFound();
    SearchSpace searchSpace();
    int nEvals();

    EvolutionLogger logger();

    // boolean epsilonGood(double epsilon);

    // return null if unknown
    public Double optimalIfKnown();

    double test(int[] solution);
}
