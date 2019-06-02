package players.rhea.utils;

public class FMBudget {
    private int remainingBudget;
    private int maxBudget;
    private int averageUsage;
    private int nIters;

    public FMBudget(int maxBudget) {
        this.maxBudget = maxBudget;
        this.remainingBudget = maxBudget;
        averageUsage = 0;
        nIters = 0;
    }

    public void reset() {
        remainingBudget = maxBudget;
    }

    public int remaining() {
        return remainingBudget;
    }

    public void use() {
        remainingBudget--;
    }

    public void use(int amount) {
        remainingBudget -= amount;
    }

    public int getUsed() { return maxBudget - remainingBudget; }

    /**
     * Calculates average number of FM calls spent per iteration.
     * @return - true if enough budget is left for another iteration, false otherwise.
     */
    public boolean enoughBudgetIteration() {
        if (nIters == 0) averageUsage = 0; else averageUsage = (maxBudget - remainingBudget) / nIters;
        return remainingBudget >= averageUsage;
    }

    /**
     * We finished an iteration, so increasing the iteration count.
     */
    public void endIteration() {
        nIters++;
    }
}
