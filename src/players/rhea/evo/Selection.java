package players.rhea.evo;

import players.rhea.utils.RHEAParams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.BiFunction;

import static players.rhea.utils.Constants.*;
import static players.rhea.utils.Utilities.index_ind_in_pop;

class Selection {
    private RHEAParams params;
    private Random random;
    private double prob;

    Selection(RHEAParams params, Random random) {
        this.params = params;
        this.random = random;
    }

    /**
     * Assumes population already sorted from highest value individual to lowest
     */
    Individual select(Individual[] population) {
        switch(params.selection_type) {
            case SELECT_RANK: return rank_selection(population);
            case SELECT_TOURNAMENT: return tournament_selection(population);
            case SELECT_ROULETTE:
            default: return roulette_selection(population);
        }
    }

    /**
     * Roulette selection
     * Probability for each individual corresponds to its value
     */
    private Individual roulette_selection(Individual[] population) {
        return apply_selection(population, this::roulette_prob);
    }

    private double roulette_prob(Individual[] population, int i) {
        if (population[i] != null)
            return population[i].get_value();
        return 0;
    }

    /**
     * Rank selection
     * Probability for each individual corresponds to its rank value (1 for lowest value, 2 for next lowest etc.)
     */
    private Individual rank_selection(Individual[] population) {
        return apply_selection(population, this::rank_prob);
    }

    private double rank_prob(Individual[] population, int i) {
        return population.length - i;
    }

    /**
     * Tournament selection
     * Individuals are randomly selected to play in the tournament, array is sorted from highest value to lowest.
     * Probability for each individual is p * ((1-p)^i), where p is a randomly chosen probability between 0 and 1
     */
    private Individual tournament_selection(Individual[] population) {
        Individual[] tournament_pop = new Individual[params.tournament_size];
        for (int i = 0; i < params.tournament_size; i++) {
            tournament_pop[i] = population[random.nextInt(population.length)];
        }
        Arrays.sort(tournament_pop, Comparator.reverseOrder());
        prob = random.nextFloat();

        return apply_selection(tournament_pop, this::tournament_prob);
    }

    private double tournament_prob(Individual[] population, int i) {
        return prob * (Math.pow((1 - prob), i));
    }

    /**
     * General selection method, using given function to apply probability depending on selection type
     * @param function - function to compute the correct selection probability, given population and individual index
     */
    private Individual apply_selection(Individual[] population, BiFunction<Individual[], Integer, Double> function) {
        double sum = 0;
        for (int i = 0; i < params.tournament_size; i++) {
            sum += function.apply(population, i);
        }

        if ((int)sum > 0) {  // It may be that all individuals have probability 0. Return random one in this case.
            double chosen = random.nextInt((int) sum);
            double newsum = 0;
            int max = population.length - 1;
            for (int i = max; i >= 0; i--) {
                if (newsum >= chosen) {
                    return population[i];
                }
                newsum += function.apply(population, i);
            }
        }

        return population[random.nextInt(population.length)];
    }

    /**
     * Testing. Test case:
     * Population with 4 individuals of length 4: [3333, 2222, 1111, 0000] and fitness values: [15, 10, 5, 0]
     * Rank, tournament and roulette selection. Repeated N times, showing the individual most likely to be selected
     * after N repetitions.
     */
    public static void main(String[] args) {
        RHEAParams params = new RHEAParams();
        params.individual_length = 4;
        params.population_size = 4;
        Random random = new Random();
        int fitness_multiplier = 5;
        int max_actions = params.population_size;
        int repetitions = 1000;

        Individual[] population = new Individual[params.population_size];
        for (int i = 0; i < params.population_size; i++) {
            int value = params.population_size - i - 1;
            population[i] = new Individual(params.individual_length, random, max_actions);
            int[] actions = new int[params.population_size];
            Arrays.fill(actions, value);
            population[i].actions = actions;
            population[i].set_value(value * fitness_multiplier);
        }

        int rank_sum = 0, tourn_sum = 0, roul_sum = 0;
        Selection s = new Selection(params, random);

        for (int i = 0; i < repetitions; i++) {
            params.selection_type = SELECT_RANK;
            rank_sum += index_ind_in_pop(s.select(population), population);

            params.selection_type = SELECT_TOURNAMENT;
            params.tournament_size = 2;
            tourn_sum += index_ind_in_pop(s.select(population), population);

            params.selection_type = SELECT_ROULETTE;
            roul_sum += index_ind_in_pop(s.select(population), population);
        }

        System.out.println("RANK SELECTION:" + population[rank_sum/repetitions].toString());
        System.out.println("TOURNAMENT SELECTION:" + population[tourn_sum/repetitions].toString());
        System.out.println("ROULETTE SELECTION:" + population[roul_sum/repetitions].toString());

    }

}