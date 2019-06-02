package utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameLogTest {

    @Test
    void deserializeLast() {
        GameLog log = GameLog.deserializeLast();
        //Check if the outputted path in System.out corresponds with the last serialized log in your folder
    }
}