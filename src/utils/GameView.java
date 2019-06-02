package utils;

import javax.swing.*;
import java.awt.*;

import static utils.Types.*;

public class GameView extends JComponent {

    private int cellSize, gridSize;
    private Types.TILETYPE[][] objs;
    private int[][] bombLife;
    private Image backgroundImg;

    /**
     * Dimensions of the window.
     */
    private Dimension dimension;

    GameView(Types.TILETYPE[][] objects, int cellSize)
    {
        this.cellSize = cellSize;
        this.gridSize = objects.length;
        this.dimension = new Dimension(gridSize * cellSize, gridSize * cellSize);
        copyObjects(objects, new int[gridSize][gridSize]);
        backgroundImg = Types.TILETYPE.PASSAGE.getImage();
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

        g.setColor(Color.BLACK);
        g.fillRect(0, dimension.height, dimension.width, dimension.height);

        for(int i = 0; i < gridSize; ++i) {
            for(int j = 0; j < gridSize; ++j) {
                Types.TILETYPE gobj = objs[i][j];

                if (gobj == null) {
                    if (VERBOSE) {
                        System.out.println("OBJECT is NULL");
                    }
                } else {
                    Rectangle rect = new Rectangle(j*cellSize, i*cellSize, cellSize, cellSize);
                    if(gobj != Types.TILETYPE.PASSAGE) {
                        //Background:
                        drawImage(g, backgroundImg, rect);
                    }

                    // Actual image (admits transparencies).
                    Image objImage = gobj.getImage();
                    if (objImage != null) {
                        if (gobj == Types.TILETYPE.BOMB) {
                            drawBomb(g, objImage, rect, bombLife[i][j], cellSize);
                        } else {
                            drawImage(g, objImage, rect);
                        }
                    }
                }
            }
        }

        g.setColor(Color.BLACK);
        //player.draw(g); //if we want to give control to the agent to paint something (for debug), start here.
    }

    static void drawImage(Graphics2D gphx, Image img, Rectangle r)
    {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        float scaleX = (float)r.width/w;
        float scaleY = (float)r.height/h;

        gphx.drawImage(img, r.x, r.y, (int) (w*scaleX), (int) (h*scaleY), null);
    }

    private static void drawBomb(Graphics2D gphx, Image img, Rectangle r, int bombLife, int cellSize) {
        drawImage(gphx, img, r);

        // Draw bomb life
        int scale = cellSize/BOMB_LIFE;

        int startX = r.x;
        int width = scale * bombLife;
        int height = cellSize/10;
        int startY = r.y + r.height - height;

        int darkLineY = r.y + r.height - 1;

        gphx.setColor(Color.red);
        gphx.fillRect(startX, startY, width, height);
        gphx.setColor(Color.black);
        gphx.drawLine(startX, darkLineY, startX + width, darkLineY);
        gphx.setColor(new Color(255, 94, 84));
        gphx.drawLine(startX, startY, startX + width, startY);
    }

    /**
     * All the objects in a board.
     * @param objects in the game
     */
    void paint(Types.TILETYPE[][] objects, int[][] bombLife)
    {
        copyObjects(objects, bombLife);
        this.repaint();
    }

    private void copyObjects(Types.TILETYPE[][] objects, int[][] bombs)
    {
        objs = new Types.TILETYPE[gridSize][gridSize];
        bombLife = new int[gridSize][gridSize];

        for (int i = 0; i < gridSize; ++i) {
            System.arraycopy(objects[i], 0, objs[i], 0, gridSize);
            System.arraycopy(bombs[i], 0, bombLife[i], 0, gridSize);
        }
    }

    /**
     * Gets the dimensions of the window.
     * @return the dimensions of the window.
     */
    public Dimension getPreferredSize() {
        return dimension;
    }


    public static void main(String[] args)
    {
        Types.TILETYPE[][] gobj = new Types.TILETYPE[4][4];

        //gobj[0][0] = new GameObject();
        gobj[0][1] = Types.TILETYPE.PASSAGE;
        gobj[0][2] = Types.TILETYPE.PASSAGE;
        gobj[0][3] = Types.TILETYPE.PASSAGE;

        gobj[1][0] = Types.TILETYPE.BOMB;
        gobj[1][1] = Types.TILETYPE.FLAMES;
        gobj[1][2] = Types.TILETYPE.WOOD;
        gobj[1][3] = Types.TILETYPE.RIGID;

        gobj[2][0] = Types.TILETYPE.EXTRABOMB;
        gobj[2][1] = Types.TILETYPE.INCRRANGE;
        gobj[2][2] = Types.TILETYPE.KICK;
        gobj[2][3] = Types.TILETYPE.PASSAGE;

        gobj[3][0] = Types.TILETYPE.AGENT0;
        gobj[3][1] = Types.TILETYPE.AGENT1;
        gobj[3][2] = Types.TILETYPE.AGENT2;
        gobj[3][3] = Types.TILETYPE.AGENT3;

        GameView view = new GameView(gobj, Types.CELL_SIZE_MAIN);

        JEasyFrame frame = new JEasyFrame(view, "Java-Pommerman", true);
        WindowInput wi = new WindowInput();
        frame.addWindowListener(wi);
        wi.windowClosed = false;

        for (int i = 0; i < 2000; ++i)
        {
            view.paint(gobj, new int[4][4]);
            try {
                Thread.sleep(50);
            } catch(Exception e)
            {
                System.out.println("EXCEPTION " + e);
            }
        }

        System.out.println("DONE");

    }
}
