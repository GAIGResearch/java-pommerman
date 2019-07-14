package players.optimisers.ntbea;

import players.optimisers.evodef.SearchSpace;
import players.optimisers.evodef.SearchSpaceUtil;
import players.optimisers.evodef.BanditLandscapeModel;
// import ntuple.params.Param;
import utils.Picker;
import utils.StatSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Modified from original NTupleSystem created by simonmarklucas on 13/11/2016.
 *
 * This version created by simonmarklucas on 10/03/2018
 *
 * This one uses an array of int directly as the key and thereby avoids possible address collisions
 *
 *
 */

// todo need to make this more efficient
    // todo there is something strange that happens when NTuples are created
    // something like making the StatSummary objects seems really slow

@SuppressWarnings({"FieldCanBeLocal", "UnusedReturnValue", "unused"})
public class NTupleSystem implements BanditLandscapeModel {

    private static int minTupleSize = 1;
    private static double defaultEpsilon = 0.5;

    private double epsilon = defaultEpsilon;

    private List<int[]> sampledPoints;

    public SearchSpace searchSpace;
    ArrayList<NTuple> tuples;

    boolean use1Tuple = true;
    boolean use2Tuple = true;
    boolean use3Tuple = false;
    boolean useNTuple = true;

    NTupleSystem() {
        // this.searchSpace = searchSpace;
        tuples = new ArrayList<>();
        sampledPoints = new ArrayList<>();
    }

    public NTupleSystem useTuples(boolean[] useTuples) {
        use1Tuple = useTuples[0];
        use2Tuple = useTuples[1];
        use3Tuple = useTuples[2];
        useNTuple = useTuples[3];
        return this;
    }

    private NTupleSystem addTuples() {
        // this should only be called AFTER setting up the search space
        tuples = new ArrayList<>();
        if (use1Tuple) add1Tuples();
        if (use2Tuple) add2Tuples();
        if (use3Tuple) add3Tuples();
        if (useNTuple) addNTuple();
        return this;
    }

    public NTupleSystem setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }

    @Override
    public BanditLandscapeModel reset() {
//        System.out.println("Resetting model");
        sampledPoints = new ArrayList<>();
        for (NTuple nTuple : tuples) {
            nTuple.reset();
        }
        return this;
    }

    @Override
    public BanditLandscapeModel setSearchSpace(SearchSpace searchSpace) {
        this.searchSpace = searchSpace;
        addTuples();
        return this;
    }

    @Override
    public SearchSpace getSearchSpace() {
        return searchSpace;
    }

    @Override
    public void addPoint(int[] p, double value) {
        for (NTuple tuple : tuples) {
            tuple.add(p, value);
        }
        sampledPoints.add(p);
    }

    public void addSummary(int[] p, StatSummary ss) {
        for (NTuple tuple : tuples) {
            tuple.add(p, ss);
        }
    }

    // careful - this can be slow - it iterates over all points in the search space!
    @Override
    public int[] getBestSolution() {
        Picker<int[]> picker = new Picker<>(Picker.MAX_FIRST);
        for (int i = 0; i < SearchSpaceUtil.size(searchSpace); i++) {
            int[] p = SearchSpaceUtil.nthPoint(searchSpace, i);
            picker.add(getMeanEstimate(p), p);
        }
        // System.out.println("Best solution: " + Arrays.toString(picker.getBest()) + "\t: " + picker.getBestScore());
        return picker.getBest();
    }

    @Override
    public int[] getBestOfSampled() {
        Picker<int[]> picker = new Picker<>(Picker.MAX_FIRST);
        for (int[] p : sampledPoints) {
            picker.add(getMeanEstimate(p), p);
        }
        // System.out.println("Best solution: " + Arrays.toString(picker.getBest()) + "\t: " + picker.getBestScore());
        return picker.getBest();
    }

    @Override
    public int[] getBestOfSampledPlusNeighbours(int nNeighbours) {
        // evaluate choices with zero exploration factor - want to exploit best
        EvaluateChoices evc = new EvaluateChoices(this, 0);
        for (int[] p : sampledPoints) {
            evc.add(p);
        }
        // System.out.println("Best solution: " + Arrays.toString(picker.getBest()) + "\t: " + picker.getBestScore());
        return evc.picker.getBest();
    }

    @Override
    public Double getMeanEstimate(int[] x) {
        // we could get an average ...

        StatSummary ssTot = new StatSummary();
        for (NTuple tuple : tuples) {
            StatSummary ss = tuple.getStats(x);
            if (ss != null) {
                if (tuple.tuple.length >= minTupleSize) {
                    double mean = ss.mean();
                    if (!Double.isNaN(mean))
                        ssTot.add(mean);
                }
            }
        }
        // BarChart.display(probVec, "Prob Vec: " + Arrays.toString(x) + " : " + pWIn(probVec));

        // return rand.nextDouble();
        // System.out.println("Returning: " + ssTot.mean() + " : " + ssTot.n());

        double ret = ssTot.mean();
        if (Double.isNaN(ret)) {
            return 0.0;
        } else {
            return ret;
        }
    }

    @Override
    public double getExplorationEstimate(int[] x) {
        // just takes the average of the exploration vector
        double[] vec = getExplorationVector(x);
        double tot = 0;
        for (double e : vec) tot += e;
        return tot / vec.length;
    }

    private double[] getExplorationVector(int[] x) {
        // idea is simple: we just provide a summary over all
        // the samples, comparing each to the maximum in that N-Tuple

        // todo check whether we need the 1+

        double[] vec = new double[tuples.size()];
        for (int i = 0; i < tuples.size(); i++) {
            NTuple tuple = tuples.get(i);
            StatSummary ss = tuple.getStats(x);
            if (ss != null) {
                vec[i] = Math.sqrt(Math.log(1 + tuple.nSamples()) / (epsilon + ss.n()));
            } else {
                vec[i] = Math.sqrt(Math.log(1 + tuple.nSamples) / epsilon);
            }
        }
        return vec;
    }


    // note that there is a smarter way to add different n-tuples, but this way is easiest

    private NTupleSystem add1Tuples() {
        for (int i = 0; i < searchSpace.nDims(); i++) {
            int[] a = new int[]{i};
            tuples.add(new NTuple(searchSpace, a));
        }
        return this;
    }

    private NTupleSystem add2Tuples() {
        for (int i = 0; i < searchSpace.nDims() - 1; i++) {
            for (int j = i + 1; j < searchSpace.nDims(); j++) {
                int[] a = new int[]{i, j};
                tuples.add(new NTuple(searchSpace, a));
            }
        }
        return this;
    }

    private NTupleSystem add3Tuples() {
        for (int i = 0; i < searchSpace.nDims() - 2; i++) {
            for (int j = i + 1; j < searchSpace.nDims() - 1; j++) {
                for (int k = j + 1; k < searchSpace.nDims(); k++) {
                    int[] a = new int[]{i, j, k};
                    tuples.add(new NTuple(searchSpace, a));
                }
            }
        }
        return this;
    }

    private NTupleSystem addNTuple() {
        // adds the entire one
        int[] a = new int[searchSpace.nDims()];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        tuples.add(new NTuple(searchSpace, a));
        return this;
    }
}
