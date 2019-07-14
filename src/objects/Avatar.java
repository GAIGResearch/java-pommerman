package objects;

import utils.Types;
import utils.Vector2d;

import java.util.Arrays;

import static utils.Types.*;


public class Avatar extends GameObject {
    private int playerID;
    private boolean canKick = DEFAULT_BOMB_KICK;
    private int ammo = DEFAULT_BOMB_AMMO;
    private int blastStrength = DEFAULT_BOMB_BLAST;
    private int visionRange = DEFAULT_VISION_RANGE;
    private Types.RESULT winner = Types.RESULT.INCOMPLETE;

    private Types.TILETYPE[] enemies;
    private Types.TILETYPE[] teammates;

    private int team; // Only relevant in non-FFA games

    public Avatar(int pId, Types.GAME_MODE gameMode) {
        super(Types.TILETYPE.values()[pId]);
        playerID = pId;

        if(gameMode != null) {
            // Setup team, enemies and teammates
            team = getGameConfig().getTeam(gameMode, playerID);
            enemies = getGameConfig().getEnemies(gameMode, playerID);
            teammates = getGameConfig().getTeammates(gameMode, playerID);
        }
    }
    public Avatar(int pId, int x, int y, boolean canKick, int ammo, int blastStrength, Types.GAME_MODE gameMode){
        // todo new constructor for building from observation
        // contain all info that we get
        super(Types.TILETYPE.values()[pId], x, y);
        super.position = new Vector2d(x, y);
        playerID = pId;
        this.canKick = canKick;
        this.ammo = ammo;
        this.blastStrength = blastStrength;
        if(gameMode != null) {
            // Setup team, enemies and teammates
            team = getGameConfig().getTeam(gameMode, playerID);
            enemies = getGameConfig().getEnemies(gameMode, playerID);
            teammates = getGameConfig().getTeammates(gameMode, playerID);
        }
    }

    public void reset() {
        canKick = DEFAULT_BOMB_KICK;
        ammo = DEFAULT_BOMB_AMMO;
        blastStrength = DEFAULT_BOMB_BLAST;
    }

    @Override
    public GameObject copy() {
        Avatar copy = new Avatar(playerID, null);
        copy.canKick = canKick;
        copy.ammo = ammo;
        copy.blastStrength = blastStrength;
        copy.life = life;
        copy.id = hashCode();
        copy.winner = winner;
        copy.visionRange = visionRange;

        if (desiredCoordinate != null) {
            copy.desiredCoordinate = desiredCoordinate.copy();
        }
        else
            copy.desiredCoordinate = null;

        if (position != null) {
            copy.position = position.copy();
        }
        else
            copy.position = null;

        copy.team = team;
        copy.enemies = enemies.clone();
        copy.teammates = teammates.clone();

        return copy;
    }

    public int getBlastStrength() {
        return blastStrength;
    }

    public int getAmmo() {
        return ammo;
    }

    public boolean canKick() {
        return canKick;
    }

    public int getPlayerID() {
        return playerID;
    }

    public Types.RESULT getWinner() {
        return winner;
    }

    public void addBlastStrength() {
        this.blastStrength++;
    }

    public void setBlastStrength(int blastStrength) {
        this.blastStrength = blastStrength;
    }

    public void addAmmo() {
        this.ammo++;
    }

    public void reduceAmmo() {
        this.ammo--;
    }

    public void setAmmo(int ammo) {
        this.ammo = ammo;
    }

    public void setCanKick() {
        this.canKick = true;
    }

    public void setWinner(Types.RESULT winner) {
        this.winner = winner;
    }

    public int getVisionRange() {
        return visionRange;
    }

    public void setVisionRange(int visionRange) {
        this.visionRange = visionRange;
    }

    public int getTeam() {
        return team;
    }

    public Types.TILETYPE[] getEnemies() {
        return enemies;
    }

    public Types.TILETYPE[] getTeammates() {
        return teammates;
    }

    @Override
    public boolean equals(Object o){
        if (o.getClass() != getClass())
            return false;
        Avatar go = (Avatar) o;
        if (getType() != go.getType())
            return false;
        if (getLife() != go.getLife())
            return false;
        if (getPosition() != null && go.getPosition() != null && !getPosition().equals(go.getPosition()))
            return false;
        if (getDesiredCoordinate() != null && go.getDesiredCoordinate() != null && !getDesiredCoordinate().equals(go.getDesiredCoordinate()))
            return false;
        if (getPlayerID() != go.getPlayerID())
            return false;
        if (canKick() != go.canKick())
            return false;
        if (getAmmo() != go.getAmmo())
            return false;
        if (getBlastStrength() != go.getBlastStrength())
            return false;
        if (getVisionRange() != go.getVisionRange())
            return false;
        if (getWinner() != go.getWinner())
            return false;
        if (getTeam() != go.getTeam())
            return false;
        if (!Arrays.equals(getTeammates(), go.getTeammates()))
            return false;
        if (!Arrays.equals(getEnemies(), go.getEnemies()))
            return false;
        return true;
    }
}
