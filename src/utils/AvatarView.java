package utils;

import core.Game;
import objects.Avatar;
import objects.GameObject;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static utils.GameView.drawImage;
import static utils.Types.TILETYPE.*;
import static utils.Types.VERBOSE;
import static utils.Utils.deepCopy;

public class AvatarView extends JComponent {

    private int cellSize;
    private GameObject[] avatars;
    @SuppressWarnings("FieldCanBeLocal")
    private int offsetX = 40, offsetY = 25;
    private boolean[] alive;
    private Game game;

    /**
     * Dimensions of the window.
     */
    private Dimension size;

    AvatarView(Game game, GameObject[] avatars)
    {
        this.game = game;
        alive = new boolean[avatars.length];
        this.cellSize = Types.AVATAR_ICON_SIZE; //(int) (cellSize * 2 / 3.0);
        this.size = new Dimension(avatars.length * (this.cellSize + offsetX), this.cellSize + offsetY*2);
        copyObjects(avatars);
        update(avatars);
    }


    public void paintComponent(Graphics gx)
    {
        Graphics2D g = (Graphics2D) gx;
        paintWithGraphics(g);
    }

    private void paintWithGraphics(Graphics2D g)
    {
        //For a better graphics, enable this: (be aware this could bring performance issues depending on your HW & OS).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Paint avatars in a row
        if (avatars != null) {
            for (int i = 0; i < avatars.length; i++) {
                GameObject o = avatars[i];
                if (o != null) {
                    int x = i * (cellSize + offsetX);
                    int y = 0;

                    if (!alive[i]) {
                        // If this avatar died, paint it with reduced opacity
                        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
                        g.setComposite(comp);
                    } else {
                        // Full opacity otherwise
                        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
                        g.setComposite(comp);
                    }

                    // Draw this avatar image
                    Rectangle rect = new Rectangle(x + cellSize/4, y + offsetY, cellSize, cellSize);
                    Image objImage = o.getImage();
                    drawImage(g, objImage, rect);

                    // Return to full opacity
                    Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
                    g.setComposite(comp);

                    // Draw win state
                    if (((Avatar)o).getWinner() != Types.RESULT.INCOMPLETE) {
                        g.setStroke(new BasicStroke(3));
                        g.setColor(((Avatar) o).getWinner().getColor());
                        g.drawRect(rect.x, rect.y + offsetY, rect.width, rect.height);
                    }

                    if (!alive[i]) {
                        // Draw a skull on top of dead avatars.
                        int wh = cellSize / 2;
                        rect = new Rectangle(x + wh, y + offsetY, wh, wh);
                        drawImage(g, Objects.requireNonNull(AGENTDUMMY.getImage()), rect);
                    }

                    _drawExtras(g, (Avatar)o, x, y + cellSize);
                } else {
                    if (VERBOSE) {
                        System.out.println("Avatar is null");
                    }
                }
            }
        }
    }

    private void _drawExtras(Graphics2D g, Avatar a, int x, int y) {
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(1));

        int fontSize = g.getFont().getSize();
        int wh = cellSize / 2;
        int spacingPowerups = 5;

        String ammo = "" + a.getAmmo();
        int offset1 = fontSize * ammo.length();
        String blast = "" + a.getBlastStrength();
        int offset2 = fontSize * blast.length();

        Rectangle rect = new Rectangle(x, y + offsetY, wh, wh);
        drawImage(g, Objects.requireNonNull(EXTRABOMB.getImage()), rect);

        g.drawString(ammo, x + offset1, y + fontSize + offsetY);

        rect = new Rectangle(x + offset1 + spacingPowerups, y + offsetY, wh, wh);
        drawImage(g, Objects.requireNonNull(INCRRANGE.getImage()), rect);

        g.drawString(blast, x + offset1 + offset2 + spacingPowerups, y + fontSize + offsetY);

        if (a.canKick()) {
            wh = cellSize / 2;
            rect = new Rectangle(x + offset1 + offset2 + spacingPowerups*2, y + offsetY, wh, wh);
            drawImage(g, Objects.requireNonNull(KICK.getImage()), rect);
        }

        String fullName = game.getPlayers().get(a.getPlayerID()-10).getClass().getName();
        String agentName = "";
        try {
            agentName = fullName.split("\\.")[1];
            agentName = fullName.split("\\.")[2];
        } catch (Exception ignored) {}

        agentName = agentName.replace("Player", "");
        g.drawString(agentName, x, y-wh);
    }

    /**
     * @param avatars in the game
     */
    void paint(GameObject[] avatars)
    {
        update(avatars);
        this.repaint();
    }

    private void copyObjects(GameObject[] avatars)
    {
        this.avatars = deepCopy(avatars);
    }

    private void update(GameObject[] avatars)
    {
        this.avatars = new GameObject[this.avatars.length];
        for (int i = 0; i < avatars.length; i++) {
            alive[i] = avatars[i].getLife() != 0;
            this.avatars[i] = avatars[i];
        }
    }

    boolean[] getAlive() {
        return alive;
    }

    /**
     * Gets the dimensions of the window.
     * @return the dimensions of the window.
     */
    public Dimension getPreferredSize() {
        return size;
    }

}
