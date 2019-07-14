package objects;

import utils.Types;
import utils.Vector2d;

import java.awt.Image;
import java.util.List;

/**
 * The superclass for game objects.
 * Models directly all game objects that do not contain special features.
 * Examples of game objects modeled directly by this class is walls, empty corridor and fog.
 */
public class GameObject {

    Vector2d position;
    Vector2d desiredCoordinate; // Should be initialized in constructor
    int life = 1;
    private utils.Types.TILETYPE type = Types.TILETYPE.PASSAGE;
    int id;

    private Image img;


    public GameObject(utils.Types.TILETYPE type, int x, int y){
        this.type = type;
        this.img = type.getImage();
        this.desiredCoordinate = new Vector2d(x, y);
    }

    public GameObject(utils.Types.TILETYPE type){
        this.type = type;
        this.img = type.getImage();
        this.desiredCoordinate = new Vector2d();
    }

    public GameObject(int x, int y){
        this.img = type.getImage();
        this.desiredCoordinate = new Vector2d(x, y);
    }

    public GameObject(){
        this.img = type.getImage();
        this.desiredCoordinate = new Vector2d();
    }

    public static boolean boardEquals(GameObject[][] board1, GameObject[][] board2) {
        for (int i = 0; i < board1.length; i++) {
            for (int i1 = 0; i1 < board1[i].length; i1++) {
                GameObject b1i = board1[i][i1];
                GameObject b2i = board2[i][i1];
                if (b1i != null && b2i != null && !b1i.equals(b2i))
                    return false;
            }
        }
        return true;
    }

    public static boolean arrayEquals(GameObject[] list1, GameObject[] list2){
        if (list1.length != list2.length)
            return false;
        for (int i = 0; i < list1.length; i++) {
            if (!list1[i].equals(list2[i]))
                return false;
        }
        return true;
    }

    public static boolean listEquals(List<GameObject> list1, List<GameObject> list2){
        if (list1.size() != list2.size())
            return false;
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i)))
                return false;
        }
        return true;
    }


    /**
     * Get the utils.Types type of this object
     * @return
     */
    public utils.Types.TILETYPE getType() {
        return type;
    }

    /**
     * Update the current state of this object
     */
    public void tick(){
        desiredCoordinate = position.copy();
    }

    /**
     * Get the life of this object. Life 0 means object should be removed.
     * @return
     */
    public int getLife(){
        return life;
    }

    public Vector2d getDesiredCoordinate(){
        return desiredCoordinate;
    }

    public void setDesiredCoordinate(Vector2d desiredCoordinate) {
        if (desiredCoordinate != null) {
            this.desiredCoordinate = desiredCoordinate.copy();
        }
    }

    public Vector2d getPosition() {
        return position;
    }

    public void setPosition(Vector2d position) {
        if (position != null) {
            this.position = position.copy();
        }
    }

    public void setPositionNull() {
        this.position = null;
    }

    public void setDesiredCoordinateNull() {
        this.desiredCoordinate = null;
    }

    public void setLife(int life) {
        this.life = life;
    }

    /**
     * Copies are handled in such a way that Object.equals() considers copies to be equal to the original.
     * @return
     */
    public GameObject copy(){
        GameObject copy = new GameObject(type);
        copy.life = life;

        copy.desiredCoordinate = desiredCoordinate.copy();
        copy.id = hashCode();

        if (position != null) {
            copy.position = position.copy();
        }

        return copy;
    }

    public Image getImage() { return img;}

    /**
     * If this object is a copy, the id has been set to a non 0 value, the hashCode of the original
     * @return
     */
    public boolean isCopy(){
        return id != 0;
    }

    @Override
    public int hashCode(){
        if (id == 0){
            return super.hashCode();
        }
        return id;
    }

    @Override
    public boolean equals(Object o){
        if (o.getClass() != getClass())
            return false;
        GameObject go = (GameObject)o;
        if (type != go.type)
            return false;
        if (life != go.life)
            return false;
        if (position != null && go.position != null && !position.equals(go.position))
            return false;
        if (desiredCoordinate != null && go.desiredCoordinate != null && !desiredCoordinate.equals(go.desiredCoordinate))
            return false;
        return true;
    }
}
