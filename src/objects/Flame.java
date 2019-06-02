package objects;

import utils.Types;

import static utils.Types.FLAME_LIFE;

public class Flame extends GameObject {

    public int playerIdx;

    public Flame() {
        super(Types.TILETYPE.FLAMES);
        life = FLAME_LIFE;
    }

    @Override
    public void tick(){
        this.life--;
        desiredCoordinate = position.copy();
    }

    @Override
    public GameObject copy() {
        Flame copy = new Flame();
        copy.life = life;
        if (position != null) {
            copy.position = position.copy();
        }
        copy.desiredCoordinate = desiredCoordinate.copy();
        copy.id = hashCode();
        return copy;
    }
}
