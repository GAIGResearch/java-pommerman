package players.rhea.evo;

import players.rhea.GameInterface;
import players.rhea.utils.RHEAParams;
import players.rhea.utils.Utilities;

import java.util.*;

import static players.rhea.utils.Constants.*;

public class Evolution {
    private RHEAParams params;
    private Random random;

    private Mutation mutationClass;
    private Crossover crossoverClass;
    private Selection selectionClass;

    private int nIterations;
    private Individual[] population;

    private GameInterface gInterface;

    public Evolution(RHEAParams params, Random random, GameInterface gInterface) {
        this.params = params;
        this.random = random;
        mutationClass = new Mutation(params, random);
        crossoverClass = new Crossover(params, random);
        selectionClass = new Selection(params, random);
        nIterations = 0;

        this.gInterface = gInterface;
    }

    public void init(int max_actions) {
        nIterations = 0;
        if (params.shift_buffer && population != null) {
            shift_population(max_actions);
        } else {
            init_population(max_actions);
            if (params.init_type != INIT_RANDOM) {
                seed();
            }
        }
    }

    /**
     * Performs 1 iteration of EA.
     * @return - best action after 1 iteration.
     */
    public int iteration() {
//        System.out.println(Arrays.toString(population));
        nIterations++;

        // Generate offspring
        Individual[] offspring = generate_offspring();

        // Update population
        combine_and_sort_population(offspring);

        return getBestAction(0);
    }

    public int getBestAction(int idx) {
        return population[0].get_action(idx);
    }

    public int getNIterations() { return nIterations; }

    //------ private

    private void seed() {
        for (int i = 0; i < params.population_size; i++) {
            if (i > 0) {
                population[i] = population[0].copy();
                mutationClass.findGenesToMutate();
                gInterface.evaluate(population[i], mutationClass, params.evaluate_update);
            } else {
                gInterface.seed(population[i], params.init_type);
                gInterface.evaluate(population[i], null, params.evaluate_update);
            }
        }
    }

    private void init_population(int max_actions) {
        population = new Individual[params.population_size];
        for (int i = 0; i < params.population_size; i++) {
            population[i] = new Individual(params.individual_length, random, max_actions);
            if (params.init_type == INIT_RANDOM) {
                population[i].randomize();
                gInterface.evaluate(population[i], null, params.evaluate_update);
            }
        }
    }

    private Individual select(Individual[] population) {
        return selectionClass.select(population);
    }

    private Individual select(Individual[] population, Individual ignore) {
        Individual[] reduced_pop = new Individual[population.length - 1];
        int idx = 0;
        for (Individual individual : population) {
            if (!individual.equals(ignore)) {
                reduced_pop[idx] = individual;
                idx++;
            }
        }

        return select(reduced_pop);
    }

    private Individual crossover(Individual[] population){
        Individual parent1 = select(population);
        Individual parent2 = select(population, parent1);

        return crossoverClass.cross(parent1, parent2);
    }

    private Individual[] generate_offspring() {
        Individual[] offspring = new Individual[params.offspring_count];
        for (int i = 0; i < params.offspring_count; i++) {
            if (params.genetic_operator == MUTATION_ONLY || params.population_size <= 2) {
                offspring[i] = population[random.nextInt(population.length)].copy();
            } else {
                offspring[i] = crossover(population);
            }
            if (params.genetic_operator != CROSSOVER_ONLY) {
                mutationClass.findGenesToMutate();
                gInterface.evaluate(offspring[i], mutationClass, params.evaluate_update);
            } else {
                gInterface.evaluate(offspring[i], null, params.evaluate_update);
            }
        }
        return offspring;
    }

    /**
     * Assumes population and offspring are already sorted in descending order by individual fitness
     * @param offspring - offspring created from parents population
     */
    @SuppressWarnings("unchecked")
    private void combine_and_sort_population(Individual[] offspring){
        int startIdx = 0;

        // Make sure we have enough individuals to choose from for the next population
        if (params.offspring_count < params.population_size) params.keep_parents_next_gen = true;

        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
            // First no_elites individuals remain the same, the rest are replaced
            startIdx = params.no_elites;
        }

        if (params.keep_parents_next_gen) {
            // Reevaluate current population
            if (params.reevaluate_pop) {
                for (Individual i : population) {
                    gInterface.evaluate(i, null, params.evaluate_update);
                }
            }
            // If we should keep best individuals of parents + offspring, then combine array
            offspring = Utilities.add_array_to_array(population, offspring, startIdx);
            Arrays.sort(offspring, Comparator.reverseOrder());
        }

        // Combine population with offspring, we keep only best individuals. If parents should not be kept, new
        // population is only best POP_SIZE offspring individuals.
        int nextIdx = 0;
        for (int i = startIdx; i < params.population_size; i++) {
            population[i] = offspring[nextIdx].copy();
            nextIdx ++;
        }

        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
            // If parents were kept to new generation and we had elites, population needs sorting again
            Arrays.sort(population, Comparator.reverseOrder());
        }
    }

    private void shift_population(int max_actions) {
        // Remove first action of all individuals and add a new random one at the end
        for (int i = 0; i < params.population_size; i++) {
            for (int j = 1; j < params.individual_length; j++) {
                population[i].set_action(j - 1, population[i].get_action(j));
            }
            population[i].set_action(params.individual_length - 1, random.nextInt(max_actions));
            gInterface.evaluate(population[i], null, EVALUATE_UPDATE_AVERAGE);
//            population[i].discount_value(params.shift_discount);
        }
    }
}
