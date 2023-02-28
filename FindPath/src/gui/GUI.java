package gui;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;

import diver.McDiver;
import graph.GameState;
import graph.Node;
import graph.Sewers;
import graph.Tile;

/** An instance is a GUI for the game. Run this <br>
 * file as a Java application to test the project. */
public class GUI extends JFrame {
    private static final long serialVersionUID= 1L;
    /** The GameState that oversees the GUI */
    GameState gameState;

    /** Width and height of the entire screen */
    public static int SCREEN_WIDTH= 1050, SCREEN_HEIGHT= 600;

    /** Width and height of the game portion (proportion of total) */
    public static final double GAME_WIDTH_PROP= 0.78, GAME_HEIGHT_PROP= 1.0;

    /** Frame rate of game (fps) and frames a single move takes */
    public static int FRAMES_PER_SECOND= 60, FRAMES_PER_MOVE= 25;

    /** The panel for generating and drawing the maze */
    protected MazePanel mazePanel;

    /** The panel for updating and drawing the diver */
    protected DiverSprite diver;

    /** The panel for showing stats / displaying options */
    protected OptionsPanel options;

    /** Panel that provides more info on selected tile */
    private TileSelectPanel tileSelect;

    /** The panel that holds all other panels */
    private JLayeredPane master;

    /** Width and height of the error pane (in pixels) */
    private static final int ERROR_WIDTH= 500, ERROR_HEIGHT= 150;

    /** // How much of the screen should the info take */
    private static final double INFO_SIZE= 0.58;/*was 0.5*/

    /** Constructor: a new display for sewer system with the diver at <br>
     * (diverRow, diverCol) using random number seed seed and overseen by GameState gs. */
    public GUI(Sewers sewers, int diverRow, int diverCol, long seed, GameState gs) {
        gameState= gs;
        // Initialize frame
        setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setLocation(150, 150);

        int GAME_WIDTH= (int) (GAME_WIDTH_PROP * SCREEN_WIDTH);
        int GAME_HEIGHT= (int) (GAME_HEIGHT_PROP * SCREEN_HEIGHT);
        int PANEL_WIDTH= SCREEN_WIDTH - GAME_WIDTH;

        // Create the maze
        mazePanel= new MazePanel(sewers, GAME_WIDTH, GAME_HEIGHT, this);
        mazePanel.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT); // gries changed to 0
        mazePanel.setVisited(diverRow, diverCol);

        // Create the diver
        diver= new DiverSprite(diverRow, diverCol, this);
        // diver.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT);
        diver.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT); // gries changed
        diver.setOpaque(false);

        // Create the panel for stats and options
        options= new OptionsPanel(GAME_WIDTH, 0, SCREEN_WIDTH - GAME_WIDTH,
            (int) (SCREEN_HEIGHT * INFO_SIZE), seed);  // gries

        // Create the panel for tile information
        tileSelect= new TileSelectPanel(GAME_WIDTH, (int) (SCREEN_HEIGHT * INFO_SIZE),
            SCREEN_WIDTH - GAME_WIDTH, (int) (SCREEN_HEIGHT * (1 - INFO_SIZE)), this);

        // Layer the diver and maze into master panel
        master= new JLayeredPane();
        master.add(mazePanel, Integer.valueOf(1));
        master.add(options, Integer.valueOf(1));
        master.add(tileSelect, Integer.valueOf(1));
        master.add(diver, Integer.valueOf(2));

        // Display GUI
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(master);
        setVisible(true);

        // Repaint the GUI to fit the new size
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                SCREEN_WIDTH= getWidth();
                SCREEN_HEIGHT= getHeight();
                int GAME_WIDTH= (int) (GAME_WIDTH_PROP * SCREEN_WIDTH);
                int GAME_HEIGHT= (int) (GAME_HEIGHT_PROP * SCREEN_HEIGHT);
                int PANEL_WIDTH= SCREEN_WIDTH - GAME_WIDTH;
                mazePanel.updateScreenSize(GAME_WIDTH, GAME_HEIGHT);
                mazePanel.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT); // gries changed from // 0
                diver.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT);  // gries changed from 0
                diver.repaint();
                options.setBounds(GAME_WIDTH, 0, SCREEN_WIDTH - GAME_WIDTH,
                    (int) (SCREEN_HEIGHT * INFO_SIZE)); // gries changed from GAME_WIDTH
                tileSelect.updateLoc(GAME_WIDTH, (int) (SCREEN_HEIGHT * INFO_SIZE), // gries changed
                                                                                    // from
                    // GAME_WIDTH
                    SCREEN_WIDTH - GAME_WIDTH, (int) (SCREEN_HEIGHT * (1 - INFO_SIZE)));
            }

            @Override
            public void componentMoved(ComponentEvent e) {}

            @Override
            public void componentShown(ComponentEvent e) {}

            @Override
            public void componentHidden(ComponentEvent e) {}
        });
    }

    /** Return the MazePanel associated with this GUI. */
    public MazePanel getMazePanel() {
        return mazePanel;
    }

    /** Return the OptionsPanel associated with this GUI. */
    public OptionsPanel getOptionsPanel() {
        return options;
    }

    /** Move the diver on the GUI to destination dest.<br>
     * Note: This blocks until the diver has moved.<br>
     * Precondition: dest is adjacent to the diver's current location */
    public void moveTo(Node dest) {
        try {
            mazePanel.setVisited(dest.getTile().row(), dest.getTile().column());
            diver.moveTo(dest);
        } catch (InterruptedException e) {
            throw new RuntimeException("GUI moveTo: Must wait for move to finish");
        }
    }

    /** Update the bonus multiplier as displayed by the GUI by bonus */
    public void updateBonus(double bonus) {
        options.updateBonus(bonus);
    }

    /** Update the number of coins picked up as displayed on the GUI.
     *
     * @param coins the number of coins to be displayed
     * @param score the player's current score */
    public void updateCoins(int coins, int score) {
        options.updateCoins(coins, score);
        tileSelect.repaint();
    }

    /** Update the steps remaining as displayed on the GUI to stepsLeft. */
    public void updateStepsToGo(int stepsLeft) {
        options.updateStepsLeft(stepsLeft);
    }

    /** What is the specification? */
    public void updateSewer(Sewers c, int numStepsToGo) {
        mazePanel.setSewer(c);
        options.updateMaxStepsLeft(numStepsToGo);
        updateStepsToGo(numStepsToGo);
        tileSelect.repaint();
    }

    /** Set the sewer system to be all light or all dark, depending on light. */
    public void setLighting(boolean light) {
        mazePanel.setLighting(light);
    }

    /** Return an image representing tile type. */
    public BufferedImage getIcon(Tile.TileType tileType) {
        return mazePanel.getIcon(tileType);
    }

    /** Return an icon for the coins on tile n, or null if no coins. */
    public BufferedImage getCoinIcon(Node n) {
        return mazePanel.getCoinsIcon(n);
    }

    /** Select node n on the GUI.<br>
     * This displays information on that node's panel on the screen. */
    public void selectNode(Node n) {
        tileSelect.selectNode(n);
    }

    /** Display error e to the player. */
    public void displayError(String e) {
        JOptionPane.showMessageDialog(mazePanel, e);
    }

    /** The main program. Run seek and scram on a random seed, <br>
     * except for: If there is an argument -s <seed>, run on that seed. */
    public static void main(String[] args) {
        List<String> argList= new ArrayList<>(Arrays.asList(args));
        int seedIndex= argList.indexOf("-s");
        long seed= 0;
        if (seedIndex >= 0) {
            try {
                seed= Long.parseLong(argList.get(seedIndex + 1));
            } catch (NumberFormatException e) {
                System.err.println("Error, -s must be followed by a numerical seed");
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error, -s must be followed by a seed");
                return;
            }
        }

        GameState.runNewGame(seed, true, new McDiver());
    }
}
