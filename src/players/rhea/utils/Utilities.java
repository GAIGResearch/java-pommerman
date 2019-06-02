package players.rhea.utils;

import players.rhea.evo.Individual;

import java.util.Arrays;
import java.util.Random;

public abstract class Utilities {

    // Method for getting the maximum value of double array
    public static double get_max (double[] inputArray){
        double maxValue = inputArray[0];
        int arrayLength = inputArray.length;
        for (int i = 1; i < arrayLength; i++){
            if(inputArray[i] > maxValue){
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }

    // Method for getting the minimum value of double array
    public static double get_min (double[] inputArray){
        double minValue = inputArray[0];
        int arrayLength = inputArray.length;
        for (int i = 1; i < arrayLength; i++){
            if(inputArray[i] < minValue){
                minValue = inputArray[i];
            }
        }
        return minValue;
    }

    // Method for getting the average value of double array
    public static double get_avg (double[] inputArray){
        double sum = inputArray[0];
        int arrayLength = inputArray.length;
        for (int i = 1; i < arrayLength; i++){
            sum += inputArray[i];
        }
        return sum / arrayLength;
    }

    // Method for getting the index of the maximum value of double array
    public static int get_idx_of_max (double[] inputArray){
        double maxValue = inputArray[0];
        int arrayLength = inputArray.length;
        int maxIdx = 0;
        for (int i = 1; i < arrayLength; i++){
            if(inputArray[i] > maxValue){
                maxValue = inputArray[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public static Individual[] add_array_to_array(Individual[] first, Individual[] second, int skip) {
        int length1 = first.length;
        int length2 = second.length;
        Individual[] newArray = new Individual[length1 + length2 - skip];
        System.arraycopy(first, skip, newArray, 0, length1 - skip);
        System.arraycopy(second, 0, newArray, length1 - skip, length2);
        return newArray;
    }

    public static double[] add_array_to_array(double[] first, double[] second, int skip) {
        int length1 = first.length;
        int length2 = second.length;
        double[] newArray = new double[length1 + length2 - skip];
        System.arraycopy(first, skip, newArray, 0, length1 - skip);
        System.arraycopy(second, 0, newArray, length1 - skip, length2);
        return newArray;
    }

    public static double[] counts_to_percentage(int[] counts) {
        int countLength = counts.length;
        double[] percentage = new double[countLength];
        double sum = 0;
        for (int count : counts) {
            sum += count;
        }
        if (sum > 0) {
            for (int i = 0; i < countLength; i++) {
                percentage[i] = counts[i] / sum;
            }
        }
        return percentage;
    }

    public static Individual[] deep_copy_pop(Individual[] population) {
        int popSize = population.length;
        Individual[] newPop = new Individual[popSize];
        for (int i = 0; i < popSize; i++) {
            newPop[i] = population[i].copy();
        }
        return newPop;
    }

    public static void print_pop(Individual[] population) {
        System.out.println(Arrays.toString(population) + "\n");
    }

    public static int index_ind_in_pop(Individual i, Individual[] population) {
        int popSize = population.length;
        for (int ind = 0; ind < popSize; ind++) {
            if (i.equals(population[ind])) return ind;
        }
        return -1;
    }

    public static double[][] getRandomDistribution(int N, int M) {
        double[][] distribution = new double[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                distribution[i][j] = 1.0/M;
            }
        }
        return distribution;
    }

    public static int apply_softmax_policy(double[] policy, Random random) {
        double sum = 0, psum = 0;
        int policyLength = policy.length;
        for (double v : policy) {
            sum += Math.pow(Math.E, -(v + 1));
        }
        double prob = random.nextFloat();
        for (int i = 0; i < policyLength; i++) {
            psum += Math.pow(Math.E, -(policy[i] + 1)) / sum;
            if (psum > prob) {
                return i;
            }
        }
        return -1;
    }

    public static int apply_greedy_policy(double[] policy) {
        double maxQ = -1;
        int idx = -1;
        int policyLength = policy.length;
        for (int j = 0; j < policyLength; j++) {
            double Q = policy[j];
            if (Q > maxQ) {
                maxQ = Q;
                idx = j;
            }
        }
        return idx;
    }
}
