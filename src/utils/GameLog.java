package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.GameState;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * An object that stores the minimal information necessary to reproduce a full replay of a game.
 * It can be serialized and stored to the gamelogs folder
 */
public class GameLog implements Serializable {
    private List<Types.ACTIONS[]> actionsArrayList = new ArrayList<>(Types.MAX_GAME_TICKS);
    private long seed;
    private int size;
    private Types.GAME_MODE gameMode;
    private final static String GAMELOGS_PATH = "res/gamelogs/ser";
    private final static String JSON_GAMELOGS_PATH = "res/gamelogs/";

    public static int REP = 0;

    public GameLog(long seed, int size, Types.GAME_MODE gameMode){
        this.seed = seed;
        this.size = size;
        this.gameMode = gameMode;
    }

    public void addActions(Types.ACTIONS[] actions){
        actionsArrayList.add(actions);
    }

    public GameState getStartingGameState(){
        return new GameState(seed, size, gameMode);
    }

    /**
     * Write this object to a file, so that it can be retrieved and replayed at a later point
     */
    public void serialize(){

        File file = new File(GAMELOGS_PATH);
        if (! file.exists()){
            file.mkdir();
        }

        if (file.listFiles() == null) {
            throw new Error("Folder specified at "+ GAMELOGS_PATH +" does not exist nor could be created.");
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestampString = timestamp.toString().replaceAll(":","-");
        timestampString = timestampString.replaceAll(" ", "_");
        String path = GAMELOGS_PATH + timestampString +"_"+  gameMode.name() + "["+size+"x"+size+"].ser";
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public void serializeJSON(String gameIdStr){
        File file = new File(JSON_GAMELOGS_PATH + gameIdStr + "/");
        if (! file.exists()){
            file.mkdir();
        }

        if (file.listFiles() == null) {
            throw new Error("Folder specified at "+ JSON_GAMELOGS_PATH +" does not exist nor could be created.");
        }

        String path = JSON_GAMELOGS_PATH  + gameIdStr + "/" + seed + "_"+ REP +"_"+  gameMode.name() + "["+size+"x"+size+"].json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);

        try {
            PrintWriter out = new PrintWriter(path);
            out.println(json);
            out.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * Read the last logfile that was serialized
     * @return the GameLog object that was serialized to that file
     */
    public static GameLog deserializeLast(){
        int id = 0;
        File logsFolder = new File(GAMELOGS_PATH);
        if (logsFolder.listFiles() != null)
            id = logsFolder.listFiles().length-1;
        if (id < 0)
            return null;
        else {
            return deserialize(id, logsFolder);
        }
    }

    /**
     * Read the last logfile that was serialized
     * @return the GameLog object that was serialized to that file
     */
    public static GameLog deserializeLastJSON(){
        int id = 0;
        File logsFolder = new File(JSON_GAMELOGS_PATH);
        if (logsFolder.listFiles() != null)
            id = logsFolder.listFiles().length-1;
        if (id < 0)
            return null;
        else {
            return deserializeJSON(id, logsFolder);
        }
    }

    private static GameLog deserializeJSON(int id, File logsFolder) {
        File log = null;
        if (logsFolder.listFiles() != null) {
            File[] fileArray = logsFolder.listFiles();
            Arrays.sort(fileArray, File::compareTo);
            log = fileArray[id];
        }
        return deserializeJSON(log.getAbsolutePath());
    }

    private static GameLog deserializeJSON(String absolutePath) {
        GameLog gameLog;
        try {
            String json = new Scanner(new File(absolutePath)).useDelimiter("\\Z").next();
            Gson gson = new Gson();
            gameLog = gson.fromJson(json, GameLog.class);
            return gameLog;
        } catch (IOException i) {
            i.printStackTrace();
        }
        return null;
    }

    /**
     * Deserialize the logfile at the given index of the gamelogs folder
     * when it is sorted by file name
     * @param index of the log file in the folder
     * @return the GameLog object that was serialized to that file
     */
    public static GameLog deserialize(int index, File logsFolder){
        File log = null;
        if (logsFolder.listFiles() != null) {
            File[] fileArray = logsFolder.listFiles();
            Arrays.sort(fileArray, File::compareTo);
            log = fileArray[index];
        }
        return deserialize(log.getAbsolutePath());
    }

    /**
     * Deserialize the logfile at the given index of the gamelogs folder
     * @param path of the log file in the folder
     * @return the GameLog object that was serialized to that file
     */
    public static GameLog deserialize(String path){
        GameLog gameLog;
        try {
            FileInputStream fileIn = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            gameLog = (GameLog) in.readObject();
            in.close();
            fileIn.close();
            System.out.println("Deserialized log at "+path);
            return gameLog;
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("GameLog class not found");
            c.printStackTrace();
        }
        return null;
    }

    public long getSeed() {
        return seed;
    }

    public int getSize() {
        return size;
    }

    public Types.GAME_MODE getGameMode() {
        return gameMode;
    }

    public List<Types.ACTIONS[]> getActions() {
        return actionsArrayList;
    }

    public GameLog copy() {
        GameLog copy = new GameLog(seed, size, gameMode);
        List<Types.ACTIONS[]> actionsArrayList = new ArrayList<>();
        for (Types.ACTIONS[] actions : this.actionsArrayList) {
            Types.ACTIONS[] copyArr = Arrays.copyOf(actions, actions.length);
            actionsArrayList.add(copyArr);
        }
        copy.actionsArrayList = actionsArrayList;
        return copy;
    }

    @Override
    public boolean equals(Object o){
        if (o.getClass() != getClass())
            return false;

        GameLog gl = (GameLog) o;

        if (gl.seed != seed)
            return false;
        if (gl.size != size)
            return false;
        if (gl.gameMode != gameMode)
            return false;
        if (gl.actionsArrayList.size() != actionsArrayList.size())
            return false;
        for (int i = 0; i < actionsArrayList.size(); i++) {
            Types.ACTIONS[] actions = actionsArrayList.get(i);
            Types.ACTIONS[] actions1 = gl.actionsArrayList.get(i);
            if (!Arrays.equals(actions,actions1))
                return false;
        }
        return true;
    }
}
