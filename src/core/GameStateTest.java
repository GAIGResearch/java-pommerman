package core;

import utils.Types;

class GameStateTest {

    @org.junit.jupiter.api.Test
    void toStringTest() {
        GameState gs = new GameState(System.currentTimeMillis(), 11, Types.GAME_MODE.FFA, true);
        gs.init();
        System.out.println(gs.model);
    }
}