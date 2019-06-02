package players.optimisers.evodef;

import utils.StatSummary;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by sml on 16/08/2016.
 */

public class EvolutionLogger {

    // this keeps track of how fitness evolves

    // bestYet are the values recorded by the logger
    // but the keepBest() method is used to track what
    // the EA under test thinks is the best solution

    public ArrayList<Double> fa;
    // these are all the solutions provided for evaluation

    public ArrayList<int[]> solutions;

    // these are the one proposed as best yet

    public ArrayList<int[]> bestYetSolutions;

    public StatSummary ss;
    int bestGen = 0;
    int[] bestYet;
    int[] finalSolution;
    double finalFitness;
    int nOptimal = 0;
    Integer firstHit;

    public EvolutionLogger() {
        reset();
    }

    public void log(double fitness, int[] solution, boolean isOptimal) {
        finalSolution = solution;
        finalFitness = fitness;
        if (fitness > ss.max()) {
            bestGen = fa.size() + 1;
            bestYet = solution;
        }
        if (isOptimal) {
            nOptimal++;
            if (firstHit == null)
                firstHit = fa.size();
        }
        fa.add(fitness);
        solutions.add(copy(solution));
        ss.add(fitness);
//        if (listener != null)
//            listener.update(this, solution, fitness);
        // System.out.println(solutions.size());
    }

    public int nEvals() {
        return fa.size();
    }

    public void report() {
        // System.out.println(ss);
        System.out.println("Best solution first found at eval: " + bestGen);
        System.out.println("Best solution: " + Arrays.toString(bestYet));
        System.out.println("Best fitness: " + ss.max());
        System.out.println("Final solution: " + Arrays.toString(finalSolution));
        System.out.println("Final fitness: " + finalFitness());
        System.out.println("Number of visits to optimal: " + nOptimal());
        System.out.println("Total number of evaluations: " + ss.n());
    }

    /*
      Important to call keepBest id we want the performance
      of the algorithm to be properly measured by the logger
    */
    public void keepBest(int[] sol, double fitness) {
        this.finalSolution = sol;
        this.finalFitness = fitness;
        // System.out.println("Called keep best");
    }

    public void logBestYest(int[] solution) {
        // bestYetSolutions.add(copy(solution));
        bestYetSolutions.add(solution);
    }

//    EvolutionListener listener;
//    public EvolutionLogger setListener(EvolutionListener listener) {
//        this.listener = listener;
//        return this;
//    }


    public double finalFitness() {
        return finalFitness;
    }

    public int[] finalSolution() {
        return finalSolution;
    }

    public int nOptimal() {
        return nOptimal;
    }

    public void reset() {
        // System.out.println("RESETTING");
        fa = new ArrayList<>();
        solutions = new ArrayList<>();
        bestYetSolutions = new ArrayList<>();
        ss = new StatSummary();
        bestYet = null;
        bestGen = 0;
        nOptimal = 0;
        firstHit = null;
    }

    int[] copy (int[] x) {
        int[] y = new int[x.length];
        for (int i=0; i<x.length; i++) y[i] = x[i];
        return y;
    }

    public ArrayList<Double> getFitnessArray() {
        ArrayList<Double> fitnessArray = new ArrayList<>();
        for (Double x : fa) fitnessArray.add(x);
        return fitnessArray;
    }
}

