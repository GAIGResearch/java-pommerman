package players;

import core.GameState;
import utils.Types;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Random;

public class HumanPlayer extends Player {
    private Random random;
    private KeyController keyboard;

    public HumanPlayer(KeyController ki, int id) {
        super(0, id);
        keyboard = ki;
    }

    public KeyController getKeyAdapter() {return keyboard;}

    @Override
    public Types.ACTIONS act(GameState gs)
    {
        return keyboard.getNextAction();
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    @Override
    public Player copy() {
        return new HumanPlayer(getKeyAdapter().copy(), playerID);
    }
}
