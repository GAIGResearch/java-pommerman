package players.rhea.evo;

import players.rhea.utils.RHEAParams;
import utils.Utils;

import java.util.Arrays;
import java.util.Random;

public class Individual implements Comparable {
    private int length;
    int[] actions;
    private int max_actions;
    private Random gen;
    private double epsilon = 1e-6;

    private double value;

    public Individual(int length, Random gen, int max_actions) {
        actions = new int[length];
        this.gen = gen;
        this.length = length;
        this.max_actions = max_actions;
    }

    void randomize() {
        for (int i = 0; i < length; i++) {
            actions[i] = gen.nextInt(max_actions);
        }
    }

    /**
     * Samples an individual according to given distribution and random generator
     * @param distribution - double array of shape (individual_length x max_actions) showing the probability of each
     *                     action appearing in each gene
     * @param random - random generator
     * @return - new individual sampled from given distribution
     */
    public static Individual sample_individual(double[][] distribution, Random random) {
        Individual i = new Individual(distribution.length, random, distribution[0].length);
        int nActions = distribution.length;
        for (int a = 0; a < nActions; a++) {
            double prob = random.nextDouble();
            double total = 0;
            int nOptions = distribution[a].length;
            for (int p = 0; p < nOptions; p++) {
                total += distribution[a][p];
                if (total >= prob) {
                    i.actions[a] = p;
                    break;
                }
            }
        }
        return i;
    }

    public int get_action(int idx) {
        return actions[idx];
    }

    public void set_action(int idx, int newAction) {
        actions[idx] = newAction;
    }

    public void set_actions(int[] newActions) {
        actions = newActions.clone();
    }

    public int[] get_actions() {
        return actions;
    }

    public int get_max_actions() {
        return max_actions;
    }

    public int get_length() {
        return length;
    }

    public void set_value(double value) {
        this.value = value;
    }

    public double get_value() {
        return this.value;
    }

    public void discount_value(double discount) {
        value *= discount;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Individual) {
            return Double.compare(value, ((Individual) o).value);
        }
        return 0;
    }

    public Individual copy () {
        Individual a = new Individual(length, gen, max_actions);
        a.set_value(value);
        a.set_actions(actions);
        return a;
    }

    @Override
    public String toString() {
        return "(" + value + ": " + Arrays.toString(actions) + ")";
    }

    public String fullString() {
        return "(" + value + ": " + Arrays.toString(actions)
                + " / " + max_actions + "; " + gen.toString() + ")";
    }

    /**
     * Testing. Test case:
     * 1 individuals of length 10.
     * Testing randomize, sample_individual, compareTo and deep_copy_pop methods.
     */
    public static void main(String[] args) {
        RHEAParams params = new RHEAParams();
        params.individual_length = 10;
        Random random = new Random();
        int max_actions = 5;
        int repetitions = 10;

        Individual ind = new Individual(params.individual_length, random, max_actions);
        System.out.println("Original: " + ind.fullString());

        Individual copy = ind.copy();
        System.out.println("Copy: 1" + copy.fullString());

        copy.randomize();
        copy.set_value(10);
        System.out.println("Random: " + copy.fullString());

        System.out.println("\nComparison: " + (ind.compareTo(copy) < 0));  // Expected lower value
        System.out.println("Comparison: " + (copy.compareTo(ind) > 0));  // Expected higher value

        System.out.println("\nDistribution:");
        double[][] distribution = new double[params.individual_length][];
        for (int i = 0; i < params.individual_length; i++) {
            distribution[i] = new double[max_actions];
            for (int j = 0; j < max_actions; j++) {
                distribution[i][j] = 1.0 / max_actions;
            }
            System.out.println("Gene " + i + ": " + Arrays.toString(distribution[i]));
        }
        System.out.println("\nSampled individuals:");
        for (int i = 0; i < repetitions; i++) {
            System.out.println("Sampled: " + Individual.sample_individual(distribution, random).toString());
        }
    }
}
