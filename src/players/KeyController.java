package players;

import utils.Types;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

public class KeyController extends KeyAdapter {

    private Queue<Types.ACTIONS> actionsQueue;
    private HashMap<Integer, Types.ACTIONS> keyMap;
    private int focusedPlayer = -1;
    private boolean primaryKeys;

    public KeyController(boolean primaryKeys)
    {
        this.primaryKeys = primaryKeys;
        actionsQueue = new ArrayDeque<>();
        keyMap = new HashMap<>();

        if (primaryKeys) {
            keyMap.put(KeyEvent.VK_LEFT, Types.ACTIONS.ACTION_LEFT);
            keyMap.put(KeyEvent.VK_UP, Types.ACTIONS.ACTION_UP);
            keyMap.put(KeyEvent.VK_DOWN, Types.ACTIONS.ACTION_DOWN);
            keyMap.put(KeyEvent.VK_RIGHT, Types.ACTIONS.ACTION_RIGHT);
            keyMap.put(KeyEvent.VK_SPACE, Types.ACTIONS.ACTION_BOMB);
        } else {
            keyMap.put(KeyEvent.VK_A, Types.ACTIONS.ACTION_LEFT);
            keyMap.put(KeyEvent.VK_W, Types.ACTIONS.ACTION_UP);
            keyMap.put(KeyEvent.VK_S, Types.ACTIONS.ACTION_DOWN);
            keyMap.put(KeyEvent.VK_D, Types.ACTIONS.ACTION_RIGHT);
            keyMap.put(KeyEvent.VK_SHIFT, Types.ACTIONS.ACTION_BOMB);
        }

    }

    public Types.ACTIONS getNextAction(){
        if (!actionsQueue.isEmpty()){
            return actionsQueue.poll();
        } else {
            return Types.ACTIONS.ACTION_STOP;
        }
    }

    public int getFocusedPlayer() {
        return focusedPlayer;
    }

    /**
     * Invoked when a key has been typed.
     * This event occurs when a key press is followed by a key release.
     */
    public void keyTyped(KeyEvent e) {}

    /**
     * Invoked when a key has been pressed.
     */
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        Types.ACTIONS candidate = keyMap.get(key);
        if (candidate != null)
            actionsQueue.add(candidate);
        else {
            // If human is not a player and presses 1, 2, 3, 4, the main view shows what that player sees.
            // If human is not a player and presses 0, the main view shows the fully observable true game state.
            if (key >= KeyEvent.VK_0 && key <= KeyEvent.VK_4) {
                focusedPlayer = key - KeyEvent.VK_0 - 1;
            }
        }
    }

    /**
     * Invoked when a key has been released.
     */
    public void keyReleased(KeyEvent e) { }

    public KeyController copy() {
        KeyController copy = new KeyController(primaryKeys);
        copy.actionsQueue.addAll(actionsQueue);
        return copy;
    }
}