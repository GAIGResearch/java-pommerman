package players.optimisers.evodef;

/**
 * Created by sml on 16/08/2016.
 *
 *  Some of the methods in this interface
 *
 */

public interface EvoAlg {

    // seed the algorithm with a specified point in the search space
    void setInitialSeed(int[] seed);
    int[] runTrial(SolutionEvaluator evaluator, int nEvals);
    void setModel(BanditLandscapeModel nTupleSystem);
    BanditLandscapeModel getModel();

    EvolutionLogger getLogger();
    void setSamplingRate(int samplingRate);

    // EvoAlg setTimeLimit(int ms);

}

