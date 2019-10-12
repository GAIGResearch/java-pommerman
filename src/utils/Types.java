package utils;

import core.gameConfig.IGameConfig;
import core.gameConfig.OriginalGameConfig;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class Types {

    // Game Configuration constants.
    public static int MAX_GAME_TICKS = 800;         //Maximum duration of the game.
    public static int BOMB_LIFE = 10;               //Ticks until a bomb explodes.
    public static int FLAME_LIFE = 5;               //Ticks until a flame dissappears.
    public static int DEFAULT_BOMB_BLAST = 2;       //Default bombs create flames with this range.
    public static int DEFAULT_BOMB_AMMO = 1;        //Default number of simultaneous bombs an agent can put.
    public static boolean DEFAULT_BOMB_KICK = false;//Can agents kick bomb by default?
    public static int DEFAULT_VISION_RANGE = 2;    //-1 for full observability, >1 for PO.

    public static boolean COLLAPSE_BOARD = true;
    public static int COLLAPSE_START = 500;
    public static int COLLAPSE_STAGES = 4;
    public static int COLLAPSE_STEP = ((Types.MAX_GAME_TICKS - COLLAPSE_START) / COLLAPSE_STAGES);

    //Game configuration to use in the game, which determines victory conditions.
    private static IGameConfig gameConfig = new OriginalGameConfig();

    //Board configuration constants.
    public static int BOARD_SIZE = 11;              //Size of the board (n x n).
    public static int BOARD_NUM_RIGID = 20;         //Number of rigid blocks to put in the level.
    public static int BOARD_NUM_WOOD = 20;          //Number of wooden (destroyable) blocks for the level.
    public static int BOARD_NUM_ITEMS = 10;         //Number of items to put in level.
    public static int MAX_INACCESIBLE_TILES = 4;    //Number of inaccessible parts of the level allowed.
    public static int CORNER_DISTANCE = 1;          //Distance to the corner, in tiles, of the starting agent position.
    public static int BREATHING_SPACE = 2;          //Breathing space, L shaped tile section free at start around agent.

    // Visualization variables (used to display game for humans to see).
    public static int FRAME_DELAY = 100;
    public static int MAIN_SCREEN_SIZE = 550;
    public static int PO_SCREEN_SIZE = 165;
    public static int AVATAR_ICON_SIZE = 30;
    public static int CELL_SIZE_MAIN = MAIN_SCREEN_SIZE / BOARD_SIZE; //50
    public static int CELL_SIZE_PO = PO_SCREEN_SIZE / BOARD_SIZE; //15;

    // General variables for logging and debugging.
    public static boolean VERBOSE = false;
    public static boolean VERBOSE_FM_DEBUG = false;
    public static boolean VISUALS = true;
    public static boolean LOGGING_STATISTICS = false;

    public final static int NUM_PLAYERS = 4;  //Changing this is NOT going to work (Forward Model assumes 4 players).
    public static int NUM_ACTIONS = 6;        //Changing this is NOT going to work either.

    // Communication
    public static int MESSAGE_LENGTH = 5;
    public static int MESSAGE_HISTORY = 5; // only keep previous 5 messages

    public static IGameConfig getGameConfig() {return gameConfig;}

    /**
     * Different TILETYPES allowed in the game.
     * If more types are added, check methods in this enum to add them where they corresponds
     * (example: if new power-up is added, include it in getPowerUpTypes() so the board generator
     *  can place them in the game).
     */
    public enum TILETYPE {

        //Types and IDs
        PASSAGE(0),
        RIGID(1),
        WOOD(2),
        BOMB(3),
        FLAMES(4),
        FOG(5),
        EXTRABOMB(6),
        INCRRANGE(7),
        KICK(8),
        AGENTDUMMY(9),
        AGENT0(10),
        AGENT1(11),
        AGENT2(12),
        AGENT3(13);

        private int key;
        TILETYPE(int numVal) {  this.key = numVal;  }
        public int getKey() {  return key; }

        /**
         * Sprites (Image objects) to use in the game for the different elements.
         * @return the image to use
         */

        public Image getImage()
        {
            if      (key == PASSAGE.key) return ImageIO.GetInstance().getImage("img/passage.png");
            else if (key == RIGID.key) return ImageIO.GetInstance().getImage("img/rigid.png");
            else if (key == WOOD.key) return ImageIO.GetInstance().getImage("img/wood.png");
            else if (key == BOMB.key) return ImageIO.GetInstance().getImage("img/bomb.png");
            else if (key == FLAMES.key) return ImageIO.GetInstance().getImage("img/flames.png");
            else if (key == FOG.key) return ImageIO.GetInstance().getImage("img/fog.png");
            else if (key == EXTRABOMB.key) return ImageIO.GetInstance().getImage("img/extrabomb.png");
            else if (key == INCRRANGE.key) return ImageIO.GetInstance().getImage("img/incrrange.png");
            else if (key == KICK.key) return ImageIO.GetInstance().getImage("img/kick.png");
            else if (key == AGENTDUMMY.key) return ImageIO.GetInstance().getImage("img/skull1.png");
            else if (key == AGENT0.key) return ImageIO.GetInstance().getImage("img/agent0.png");
            else if (key == AGENT1.key) return ImageIO.GetInstance().getImage("img/agent1.png");
            else if (key == AGENT2.key) return ImageIO.GetInstance().getImage("img/agent2.png");
            else if (key == AGENT3.key) return ImageIO.GetInstance().getImage("img/agent3.png");
            else return null;
        }

        /**
         * Returns all agent types.
         * @return all agent types.
         */
        public static HashSet<TILETYPE> getAgentTypes() {
            HashSet<TILETYPE> types = new HashSet<>();
            types.add(AGENT0);
            types.add(AGENT1);
            types.add(AGENT2);
            types.add(AGENT3);
            return types;
        }

        /**
         * Returns all power up types.
         * @return all power up types.
         */
        public static HashSet<TILETYPE> getPowerUpTypes() {
            HashSet<TILETYPE> types = new HashSet<>();
            types.add(EXTRABOMB);
            types.add(INCRRANGE);
            types.add(KICK);
            return types;
        }

        /**
         * Checks if two boards (arrays of tiletypes) are the same
         * @param board1 one board to check
         * @param board2 the other board to check
         * @return true if they're equals.
         */
        public static boolean boardEquals(TILETYPE[][] board1, TILETYPE[][] board2) {

            if( (board1.length != board2.length) || (board1[0].length != board2[0].length))
                return false;

            for (int i = 0; i < board1.length; i++) {
                for (int i1 = 0; i1 < board1[i].length; i1++) {
                    TILETYPE b1i = board1[i][i1];
                    TILETYPE b2i = board2[i][i1];
                    if (b1i != null && b2i != null && b1i != b2i)
                        return false;
                }
            }
            return true;
        }
    }

    /**
     * Defines the game mode
     */
    public enum GAME_MODE {
        FFA(0),
        TEAM(1),
        TEAM_RADIO(2);

        private int key;
        GAME_MODE(int numVal) {  this.key = numVal; }
        public int getKey() { return key; }
    }

    /**
     * Defines the directions that game objects can have for movement.
     */
    public enum DIRECTIONS {
        NONE(0, 0),
        LEFT(-1, 0),
        RIGHT(1, 0),
        UP(0, -1),
        DOWN(0, 1);

        private int x, y;

        DIRECTIONS(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Vector2d toVec() {
            return new Vector2d(x, y);
        }

        public int x() {return x;}
        public int y() {return y;}
    }

    /**
     * Defines all actions in the game.
     */
    public enum ACTIONS {
        ACTION_STOP(0),
        ACTION_UP(1),
        ACTION_DOWN(2),
        ACTION_LEFT(3),
        ACTION_RIGHT(4),
        ACTION_BOMB(5);

        private int key;
        ACTIONS(int numVal) {  this.key = numVal; }
        public int getKey() {return this.key;}

        /**
         * Gets all actions of the game
         * @return all the actions in an array list.
         */
        public static ArrayList<ACTIONS> all()
        {
            ArrayList<ACTIONS> allActions = new ArrayList<ACTIONS>();
            allActions.add(ACTION_STOP);
            allActions.add(ACTION_UP);
            allActions.add(ACTION_DOWN);
            allActions.add(ACTION_LEFT);
            allActions.add(ACTION_RIGHT);
            allActions.add(ACTION_BOMB);
            return allActions;
        }

        /**
         * For directional actions, returns the corresponding direction.
         * @return the direction that represents the movement action. NONE if this is not a movement action.
         */
        public DIRECTIONS getDirection()
        {
            if(this == ACTION_UP)
                return DIRECTIONS.UP;
            else if(this == ACTION_DOWN)
                return DIRECTIONS.DOWN;
            else if(this == ACTION_LEFT)
                return DIRECTIONS.LEFT;
            else if(this == ACTION_RIGHT)
                return DIRECTIONS.RIGHT;
            else
                return DIRECTIONS.NONE;
        }
    }

    /**
     * Results of the game.
     */
    public enum RESULT {
        WIN(0),
        LOSS(1),
        TIE(2),
        INCOMPLETE(3);

        private int key;
        RESULT(int numVal) { this.key = numVal; }
        public int getKey() { return this.key; }

        /**
         * Returns the colour that represents such victory condition for the GUI.
         * @return colours of results.
         */
        public Color getColor() {
            if (key == WIN.key) return Color.green;
            if (key == LOSS.key) return Color.red;
            if (key == TIE.key) return Color.orange;
            return null;
        }
    }
}
