package players.optimisers.ntbea;

import players.optimisers.evodef.*;
import utils.ElapsedCpuTimer;
import utils.StatSummary;

/**
 * Created by sml on 09/01/2017.
 */

public class NTupleBanditEA implements EvoAlg {

    // public NTupleSystem banditLandscapeModel;
    public BanditLandscapeModel banditLandscapeModel;

    // the exploration rate normally called K or C - called kExplore here for clarity
    public double kExplore = 100.0; // 1.0; // Math.sqrt(0.4);
    // the number of neighbours to explore around the current point each time
    // they are only explored IN THE FITNESS LANDSCAPE MODEL, not by sampling the fitness function
    int nNeighbours = 50;


    // when searching for the best solution overall, at the end of the run
    // we ask the NTupleMemory to explore a neighbourhood around each
    // of the points added during the search
    // this param controls the size of the neighbourhood
    int neighboursWhenFindingBest = 10;
    static double defaultEpsilon = 0.5;
    double epsilon = defaultEpsilon;

    public int nSamples = 1;

    public void setSamplingRate(int n) {
        nSamples = n;
    }


    public NTupleBanditEA(double kExplore, int nNeighbours) {
        this.kExplore = kExplore;
        this.nNeighbours = nNeighbours;
    }


    public NTupleBanditEA() {
    }

    public NTupleBanditEA setKExplore(double kExplore) {
        this.kExplore = kExplore;
        return this;
    }

    public NTupleBanditEA setReportFrequency(int reportFrequency) {
        this.reportFrequency = reportFrequency;
        return this;
    }

    public NTupleBanditEA setNeighbours(int nNeighbours) {
        this.nNeighbours = nNeighbours;
        return this;
    }

    StatSummary fitness(SolutionEvaluator evaluator, int[] sol) {
        StatSummary ss = new StatSummary();
        for (int i = 0; i < nSamples; i++) {
            double fitness = evaluator.evaluate(sol);
            ss.add(fitness);

        }
        return ss;
    }


    int[] seed;

    public void setInitialSeed(int[] seed) {
        this.seed = SearchSpaceUtil.copyPoint(seed);
    }

    SolutionEvaluator evaluator;

    int reportFrequency = 10000;

    // want to reset for comparability tests involving multiple runs
    // but turn this off to accumulate knowledge from run to run
    public boolean resetModelEachRun = true;

    public NTupleBanditEA setResetModelEachRun(boolean resetModelEachRun) {
        this.resetModelEachRun = resetModelEachRun;
        return this;
    }

    public boolean logBestYet = false;

    @Override
    public int[] runTrial(SolutionEvaluator evaluator, int nEvals) {

        this.evaluator = evaluator;
        // set  up some convenient references
        SearchSpace searchSpace = evaluator.searchSpace();
        EvolutionLogger logger = evaluator.logger();
        DefaultMutator mutator = new DefaultMutator(searchSpace);
        banditLandscapeModel.setSearchSpace(searchSpace);

        nNeighbours = (int) Math.min(nNeighbours, SearchSpaceUtil.size(searchSpace) / 4);
        System.out.println("Set neighbours to: " + nNeighbours);

        if (banditLandscapeModel == null) {
            // System.out.println("NTupleBanditEA.runTrial: Creating new landscape model");
            banditLandscapeModel = new NTupleSystem().setSearchSpace(searchSpace);
        }
        banditLandscapeModel.setEpsilon(epsilon);

        // create an NTuple fitness landscape model if needed
        if (resetModelEachRun) {
            // System.out.println("NTupleBanditEA.runTrial: resetting landscape model");
            banditLandscapeModel.reset();
        }

        // then each time around the loop try the following
        // create a neighbourhood set of points and pick the best one that combines it's exploitation and evaluation scores

        StatSummary ss = new StatSummary();

        int[] p;
        if (seed == null) {
            p = SearchSpaceUtil.randomPoint(searchSpace);
        } else {
            p = seed;
        }

        // banditLandscapeModel.printDetailedReport();

        while (evaluator.nEvals() < nEvals) {

            // each time around the loop we make one fitness evaluation of p
            // and add this NEW information to the memory
            int prevEvals = evaluator.nEvals();

            // the new version enables resampling
            double fitness;

            if (nSamples == 1) {
                fitness = evaluator.evaluate(p);
            } else {
                fitness = fitness(evaluator, p).mean();
            }
            // System.out.println();

            if (reportFrequency > 0 && evaluator.nEvals() % reportFrequency == 0) {
                System.out.format("Iteration: %d\t %.1f\n", evaluator.nEvals(), fitness);
                System.out.println(evaluator.logger().ss);
                System.out.println();
                // System.out.println(p.length);
                // System.out.println(p);
            }

            ElapsedCpuTimer t = new ElapsedCpuTimer();

            banditLandscapeModel.addPoint(p, fitness);

            // ss.add(t.elapsed());
//            System.out.println(ss);
//            System.out.println("N Neighbours: " + nNeighbours);
            EvaluateChoices evc = new EvaluateChoices(banditLandscapeModel, kExplore);
            // evc.add(p);

            // and then explore the neighbourhood around p, balancing exploration and exploitation
            // depending on the mutation function, some of the neighbours could be far away
            // or some of them could be duplicates - duplicates a bit wasteful so filter these
            // out - repeat until we have the required number of unique neighbours

            while (evc.n() < nNeighbours) {
                int[] pp = mutator.randMut(p);
                evc.add(pp);
            }

            // evc.report();

            // now set the next point to explore
            p = evc.picker.getBest();
//            logger.keepBest(picker.getBest(), picker.getBestScore());

            int diffEvals = evaluator.nEvals() - prevEvals;

            if (logBestYet) {
                int[] bestYet = banditLandscapeModel.getBestOfSampled();
                for (int i = 0; i < diffEvals; i++) {
                    evaluator.logger().logBestYest(bestYet);
                }
            }

            // System.out.println("Best solution: " + Arrays.toString(evc.picker.getBest()) + "\t: " + evc.picker.getBestScore());
        }

//        System.out.println("Time for calling addPoint: ");
//        System.out.println(ss);

        // int[] solution = banditLandscapeModel.getBestSolution();
        int[] solution = banditLandscapeModel.getBestOfSampled();
        // int[] solution = banditLandscapeModel.getBestOfSampledPlusNeighbours(neighboursWhenFindingBest);
        logger.keepBest(solution, evaluator.evaluate(solution));
        return solution;
    }

    @Override
    public void setModel(BanditLandscapeModel banditLandscapeModel) {
        this.banditLandscapeModel = banditLandscapeModel;
    }

    @Override
    public BanditLandscapeModel getModel() {
        return banditLandscapeModel;
    }

    @Override
    public EvolutionLogger getLogger() {
        return evaluator.logger();
    }

    public NTupleBanditEA setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }
}
