package players.rhea.evo;

import gnu.trove.set.hash.TIntHashSet;
import players.rhea.utils.RHEAParams;
import utils.Utils;

import java.util.Random;

import static players.rhea.utils.Constants.*;

public class Mutation {
    private RHEAParams params;
    private int gene_size;
    private int nGenes;
    private Random random;
    private TIntHashSet genesToMutate;

    Mutation(RHEAParams params, Random random) {
        this.params = params;
        this.random = random;

        // Make sure the gene size is minimum 1 and maximum individual length
        if (params.gene_size != 1) {
            gene_size = Utils.clamp(1, params.gene_size, params.individual_length);
        } else {
            gene_size = params.gene_size;
        }
        nGenes = params.individual_length / gene_size;
    }

    public TIntHashSet getGenesToMutate() {
        return genesToMutate;
    }

    void findGenesToMutate() {
        TIntHashSet genesToMutate;

        if (params.mutation_type == MUTATION_BIT) {
            genesToMutate = one_bit_mutation();
        } else if (params.mutation_type == MUTATION_BIAS) {
            genesToMutate = softmax_mutation();
        } else {
            genesToMutate = uniform_mutation();
        }

        this.genesToMutate = genesToMutate;
    }

    /**
     * Mutates genes uniformly at random. Each gene has /params.mutation_rate/ chance to be mutated to a new random value.
     */
    private TIntHashSet uniform_mutation() {
        TIntHashSet genesToMutate = new TIntHashSet();
        for (int i = 0; i < nGenes; i++) {
            if (random.nextFloat() < params.mutation_rate) {
                genesToMutate.add(i);
            }
        }
        return genesToMutate;
    }

    /**
     * Mutates /mutation_gene_count/ genes to a new value.
     */
    private TIntHashSet one_bit_mutation() {
        TIntHashSet genesToMutate = new TIntHashSet();
        for (int i = 0; i < params.mutation_gene_count; i++) {
            int idx = random.nextInt(nGenes);
            genesToMutate.add(idx);
        }
        return genesToMutate;
    }

    private TIntHashSet softmax_mutation() {
        TIntHashSet genesToMutate = new TIntHashSet();

        // bias mutations towards the beginning of the array of individuals, softmax
        double sum = 0, psum = 0;
        for (int i = 0; i < nGenes; i++) {
            sum += Math.pow(Math.E, -(i + 1));
        }
        double prob = Math.random();
        for (int i = 0; i < nGenes; i++) {
            psum += Math.pow(Math.E, -(i + 1)) / sum;
            if (psum > prob) {
                genesToMutate.add(i);
                break;
            }
        }
        return genesToMutate;
    }

    /**
     * Changes given gene to new random value in range [0, max_value). New gene will NOT be the same as current.
     * @param idx - index of gene to mutate
     */
    public void mutateGeneToNewValue(Individual ind, int idx) {
        int max_actions = ind.get_max_actions();

        for (int j = 0; j < gene_size; j++) {
            int[] idxList = new int[max_actions - 1];
            int count = 0;
            for (int i = 0; i < max_actions; i++) {
                if (i != ind.actions[idx]) {
                    idxList[count] = i;
                    count++;
                }
            }
            ind.actions[idx + j] = idxList[random.nextInt(idxList.length)];
        }
    }
}
