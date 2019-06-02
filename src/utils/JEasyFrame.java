package utils;

import javax.swing.*;
import java.awt.*;

/**
 * Frame for the graphics.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class JEasyFrame extends JFrame {

    /**
     * Constructor
     * @param comp Main component of the frame.
     * @param title Title of the window.
     */
    public JEasyFrame(Component comp, String title, boolean closeAppOnClosingWindow) {
        super(title);
        if (comp != null) {
            getContentPane().add(BorderLayout.CENTER, comp);
        }
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if(closeAppOnClosingWindow){
        	setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
        repaint();
    }

    /**
     * Closes this component.
     */
    public void quit()
    {
        System.exit(0);
    }
}

