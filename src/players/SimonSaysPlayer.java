package players;

import core.GameState;
import utils.Types;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class SimonSaysPlayer extends Player {

    public static final Types.ACTIONS[] DEFAULT_ACTIONS = new Types.ACTIONS[]{
            Types.ACTIONS.ACTION_UP,
            Types.ACTIONS.ACTION_UP,
            Types.ACTIONS.ACTION_DOWN,
            Types.ACTIONS.ACTION_DOWN,
            Types.ACTIONS.ACTION_LEFT,
            Types.ACTIONS.ACTION_RIGHT,
            Types.ACTIONS.ACTION_LEFT,
            Types.ACTIONS.ACTION_RIGHT,
            Types.ACTIONS.ACTION_BOMB,
            Types.ACTIONS.ACTION_UP,
            Types.ACTIONS.ACTION_DOWN
    };
    Queue<Types.ACTIONS> actionsQueue;

    public SimonSaysPlayer(int Idx, Queue<Types.ACTIONS> actions){
        super(0, Idx);
        actionsQueue = actions;
    }

    public SimonSaysPlayer(int Idx){
        super(0, Idx);
        actionsQueue = new ArrayDeque<>();
        actionsQueue.addAll(Arrays.asList(DEFAULT_ACTIONS));
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        Types.ACTIONS action = actionsQueue.poll();
        if (action == null)
            action = Types.ACTIONS.ACTION_STOP;
        return action;
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    @Override
    public Player copy() {
        Queue<Types.ACTIONS> copyActionQueue = new ArrayDeque<>(actionsQueue.size());
        copyActionQueue.addAll(actionsQueue);
        SimonSaysPlayer copy = new SimonSaysPlayer(playerID, copyActionQueue);
        return copy;
    }

    public static int defaultSequenceLength(){
        return DEFAULT_ACTIONS.length;
    }
}
