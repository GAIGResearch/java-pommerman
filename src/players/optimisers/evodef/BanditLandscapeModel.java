package players.optimisers.evodef;

/**
 * Created by sml on 19/01/2017.
 */


public interface BanditLandscapeModel {

    // reset removes all data from the model
    BanditLandscapeModel reset();

    BanditLandscapeModel setSearchSpace(SearchSpace searchSpace);

    BanditLandscapeModel setEpsilon(double epsilon);

    SearchSpace getSearchSpace();

    void addPoint(int[] p, double value);

    // careful - this can be slow - it iterates over all points in the search space!
    int[] getBestSolution();

    // get best of sampled is the default choice
    int[] getBestOfSampled();

    // including the neighbours is more expensive but less
    // likely to miss a hidden gem
    int[] getBestOfSampledPlusNeighbours(int nNeighbours);

    // return a Double object - a null return indicates that
    // we know nothing yet;
    Double getMeanEstimate(int[] x);

    // if we've seen nothing of this point then the value
    // for the exploration term will be high, but small epsilon
    // prevents overflow
    double getExplorationEstimate(int[] x);

}


