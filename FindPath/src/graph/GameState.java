package graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import diver.McDiver;
import gui.GUI;

public class GameState implements SeekState, ScramState {

    private enum Phase {
        SEEK, SCRAM;
    }

    @SuppressWarnings("serial")
    private static class OutOfTimeException extends RuntimeException {}

    static boolean shouldPrint= true;

    /** minimum and maximum number of rows */
    public static final int MIN_ROWS= 8, MAX_ROWS= 25;

    /** minimum and maximum number of columns */
    public static final int MIN_COLS= 12, MAX_COLS= 40;

    /** Time-out time for seek and scram phases */
    public static final long SEEK_TIMEOUT= 10, SCRAM_TIMEOUT= 15;

    /** Minimum and maximum bonuses */
    public static final double MIN_BONUS= 1.0, MAX_BONUS= 1.3;

    /** extra time factor. bigger is nicer - addition to total multiplier */
    private static final double EXTRA_TIME_FACTOR= 0.3;

    private static final double NO_BONUS_LENGTH= 3;

    /** The seek- and scram- sewers */
    private final Sewers seekSewer, scramSewer;

    private final SewerDiver sewerDiver;

    private final Optional<GUI> gui;

    private final long seed;

    private Node position;

    /** steps taken so far, steps left, and coins collected */
    private int stepsTaken, stepsToGo, coinsCollected;

    private Phase phase;
    private boolean seekSucceeded= false;
    private boolean scramSucceeded= false;
    private boolean seekErred= false;
    private boolean scramErred= false;
    private boolean seekTimedOut= false;
    private boolean scramTimedOut= false;

    private int minSeekDistance;
    private int minScramDistance;

    private int seekStepsLeft= 0;
    private int scramStepsLeft= 0;

    private int minSeekSteps;

    /** = "scram succeeded" */
    public boolean scramSucceeded() {
        return scramSucceeded;
    }

    /** Constructor: a new GameState object for sewerDiver sd. <br>
     * This constructor takes a path to files storing serialized sewers <br>
     * and simply loads these sewers. */
    GameState(Path seekSewerPath, Path scramSewerPath, SewerDiver sd)
        throws IOException {
        seekSewer= Sewers.deserialize(Files.readAllLines(seekSewerPath));
        minSeekSteps= seekSewer.minPathLengthToRing(seekSewer.entrance());
        scramSewer= Sewers.deserialize(Files.readAllLines(scramSewerPath));

        sewerDiver= sd;

        position= seekSewer.entrance();
        stepsTaken= 0;
        stepsToGo= Integer.MAX_VALUE;
        coinsCollected= 0;

        seed= -1;

        phase= Phase.SEEK;
        gui= Optional.of(new GUI(seekSewer, position.getTile().row(),
            position.getTile().column(), 0, this));
    }

    /** Constructor: a new random game instance with or without a GUI. */
    private GameState(boolean useGui, SewerDiver sd) {
        this(new Random().nextLong(), useGui, sd);
    }

    /** Constructor: a new game instance using seed seed with or without a GUI, <br>
     * and with sewerDiver sd used to solve the game. */
    /* package */ GameState(long seed, boolean useGui, SewerDiver sd) {
        Random rand= new Random(seed);
        int ROWS= rand.nextInt(MAX_ROWS - MIN_ROWS + 1) + MIN_ROWS;
        int COLS= rand.nextInt(MAX_COLS - MIN_COLS + 1) + MIN_COLS;
        seekSewer= Sewers.digExploreSewer(ROWS, COLS, rand);
        minSeekSteps= seekSewer.minPathLengthToRing(seekSewer.entrance());
        Tile ringTile= seekSewer.ring().getTile();
        scramSewer= Sewers.digGetOutSewer(ROWS, COLS, ringTile.row(), ringTile.column(), rand);

        position= seekSewer.entrance();
        stepsTaken= 0;
        stepsToGo= Integer.MAX_VALUE;
        coinsCollected= 0;

        sewerDiver= sd;
        phase= Phase.SEEK;

        this.seed= seed;

        if (useGui) {
            gui= Optional.of(new GUI(seekSewer, position.getTile().row(),
                position.getTile().column(), seed, this));
        } else {
            gui= Optional.empty();
        }
    }

    /** Run through the game, one step at a time. <br>
     * Will run scram() only if seek() succeeds. <br>
     * Will fail in case of timeout. */
    void runWithTimeLimit() {
        seekWithTimeLimit();
        if (!seekSucceeded) {
            seekStepsLeft= seekSewer.minPathLengthToRing(position);
            scramStepsLeft= scramSewer.minPathLengthToRing(scramSewer.entrance());
        } else {
            scramWithTimeLimit();
            if (!scramSucceeded) {
                scramStepsLeft= scramSewer.minPathLengthToRing(position);
            }
        }
    }

    /** Run through the game, one step at a time. <br>
     * Will run scram() only if seek() succeeds. <br>
     * Does not use a timeout and will wait as long as necessary. */
    void run() {
        seekNoTimeout();
        if (!seekSucceeded) {
            seekStepsLeft= seekSewer.minPathLengthToRing(position);
            scramStepsLeft= scramSewer.minPathLengthToRing(scramSewer.entrance());
        } else {
            scramNoTimeout();
            if (!scramSucceeded) {
                scramStepsLeft= scramSewer.minPathLengthToRing(position);
            }
        }
    }

    /** Run only the seek phase. Uses timeout. */
    void runSeekWithTimeout() {
        seekWithTimeLimit();
        if (!seekSucceeded) {
            seekStepsLeft= seekSewer.minPathLengthToRing(position);
        }
    }

    /** Run only the scram phase. Uses timeout. */
    void runScramWithTimeout() {
        scramWithTimeLimit();
        if (!scramSucceeded) {
            scramStepsLeft= scramSewer.minPathLengthToRing(position);
        }
    }

    @SuppressWarnings("deprecation")
    /** Wrap a call seek() with the timeout functionality. */
    private void seekWithTimeLimit() {
        FutureTask<Void> ft= new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() {
                seekNoTimeout();
                return null;
            }
        });

        Thread t= new Thread(ft);
        t.start();
        try {
            ft.get(SEEK_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            t.stop();
            seekTimedOut= true;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("ERROR");
            // Shouldn't happen
        }
    }

    /** Run the sewerDiver's seek() function with no timeout. */
    /* package */ void seekNoTimeout() {
        phase= Phase.SEEK;
        stepsTaken= 0;
        seekSucceeded= false;
        position= seekSewer.entrance();
        minSeekDistance= seekSewer.minPathLengthToRing(position);
        gui.ifPresent((g) -> g.setLighting(false));
        gui.ifPresent((g) -> g.updateSewer(seekSewer, 0));
        gui.ifPresent((g) -> g.moveTo(position));

        try {
            sewerDiver.seek(this);
            // Verify that we returned at the correct location
            if (position.equals(seekSewer.ring())) {
                seekSucceeded= true;
            } else {
                errPrintln("seek(...) returned at the wrong location.");
                gui.ifPresent(
                    (g) -> g.displayError("seek(f..) returned at the wrong location."));
            }
        } catch (Throwable t) {
            if (t instanceof ThreadDeath) return;
            errPrintln("seek(...) threw an exception.");
            errPrintln("Here is the output.");
            t.printStackTrace();
            gui.ifPresent((g) -> g.displayError(
                "seek(...) threw an exception. See the console output."));
            seekErred= true;
        }
    }

    @SuppressWarnings("deprecation")
    /** Wrap a call scram() with the timeout functionality. */
    private void scramWithTimeLimit() {
        FutureTask<Void> ft= new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() {
                scramNoTimeout();
                return null;
            }
        });

        Thread t= new Thread(ft);
        t.start();
        try {
            ft.get(SCRAM_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            t.stop();
            scramTimedOut= true;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("ERROR"); // Shouldn't happen
        }
    }

    /** Handle the logic for running the sewerDiver's scram() procedure with no timeout. */
    /* package */ void scramNoTimeout() {
        phase= Phase.SCRAM;
        Tile ringTile= seekSewer.ring().getTile();
        position= scramSewer.nodeAt(ringTile.row(), ringTile.column());
        minScramDistance= scramSewer.minPathLengthToRing(position);
        stepsToGo= computeStepsToScram();
        gui.ifPresent((g) -> g.getOptionsPanel().changePhaseLabel("Scram phase"));
        gui.ifPresent((g) -> g.setLighting(true));
        gui.ifPresent((g) -> g.updateSewer(scramSewer, stepsToGo));

        // Pick up coins on start phase (if any)
        Node cn= currentNode();
        int coins= cn.getTile().coins();
        if (coins > 0) {
            grabCoins();
        }

        try {
            sewerDiver.scram(this);
            // Verify that the diver returned at the correct location
            if (!position.equals(scramSewer.ring())) {
                errPrintln("scram(..) returned at the wrong location.");
                gui.ifPresent((g) -> g
                    .displayError("scram(...) returned at the wrong location."));
                return;
            }

            scramSucceeded= true;
            gui.ifPresent((g) -> g.getOptionsPanel().changePhaseLabel("Scram done!"));
            System.out.println("Scram Succeeded!");
            // Since the exit has been reached, turn off painting the
            GUI g= gui.isPresent() ? gui.get() : null;
            gui.MazePanel mp= g == null ? null : g.getMazePanel();
            if (mp != null) mp.repaint();

        } catch (OutOfTimeException e) {
            errPrintln("scram(...) ran out of steps before returning!");
            gui.ifPresent((g) -> g
                .displayError("scram(...) ran out of steps before returning!"));
        } catch (Throwable t) {
            if (t instanceof ThreadDeath) return;
            errPrintln("scram(...) threw an exception:");
            t.printStackTrace();
            gui.ifPresent((g) -> g.displayError(
                "scram(...) threw an exception. See the console output."));
            scramErred= true;
        }

        outPrintln("Coins collected   : " + getCoinsCollected());
        DecimalFormat df= new DecimalFormat("#.##");
        outPrintln("Bonus multiplier : " + df.format(computeBonusFactor()));
        outPrintln("Score            : " + getScore());
    }

    /** Making sure the sewerDiver always has the minimum steps needed to get out, <br>
     * add a factor of extra steps proportional to the size of the sewer. */
    private int computeStepsToScram() {
        int minStepsToScram= scramSewer.minPathLengthToRing(position);
        return (int) (minStepsToScram + EXTRA_TIME_FACTOR *
            (Sewers.MAX_EDGE_WEIGHT + 1) * scramSewer.numOpenTiles() / 2);

    }

    /** Compare the sewerDiver's performance on the scram() phase to the <br>
     * theoretical minimum, compute their bonus factor on a call from <br>
     * MIN_BONUS to MAX_BONUS. <br>
     * Bonus should be minimum if take longer than NO_BONUS_LENGTH times optimal. */
    private double computeBonusFactor() {
        double findDiff= (stepsTaken - minSeekSteps) / (double) minSeekSteps;
        if (findDiff <= 0) return MAX_BONUS;
        double multDiff= MAX_BONUS - MIN_BONUS;
        return Math.max(MIN_BONUS, MAX_BONUS - findDiff / NO_BONUS_LENGTH * multDiff);
    }

    /** See moveTo(Node&lt;TileData&gt; n)
     *
     * @param id The Id of the neighboring Node to move to */
    @Override
    public void moveTo(long id) {
        if (phase != Phase.SEEK) {
            throw new IllegalStateException("moveTo(ID) can only be called while scramming!");
        }

        for (Node n : position.neighbors()) {
            if (n.getId() == id) {
                position= n;
                stepsTaken++ ;
                gui.ifPresent((g) -> g.updateBonus(computeBonusFactor()));
                gui.ifPresent((g) -> g.moveTo(n));
                return;
            }
        }
        throw new IllegalArgumentException("moveTo: Node must be adjacent to position");
    }

    /** Return the unique id of the current location. */
    @Override
    public long currentLocation() {
        if (phase != Phase.SEEK) {
            throw new IllegalStateException(
                "currentLocation() can be called only while scramming!");
        }

        return position.getId();
    }

    /** Return a collection of NodeStatus objects that contain the unique ID of the node and the
     * distance from that node to the ring. */
    @Override
    public Collection<NodeStatus> neighbors() {
        if (phase != Phase.SEEK) {
            throw new IllegalStateException("neighbors() can be called only while scramming!");
        }

        Collection<NodeStatus> options= new ArrayList<>();
        for (Node n : position.neighbors()) {
            int distance= computeDistanceToRing(n.getTile().row(), n.getTile().column());
            options.add(new NodeStatus(n.getId(), distance));
        }
        return options;
    }

    /** Return the Manhattan distance from (row, col) to the ring */
    private int computeDistanceToRing(int row, int col) {
        return Math.abs(row - seekSewer.ring().getTile().row()) +
            Math.abs(col - seekSewer.ring().getTile().column());
    }

    /** Return the Manhattan distance from the current location <br>
     * to the ring location on the map. */
    @Override
    public int distanceToRing() {
        if (phase != Phase.SEEK) {
            throw new IllegalStateException(
                "distanceToRing() can be called only while scramming!");
        }

        return computeDistanceToRing(position.getTile().row(), position.getTile().column());
    }

    @Override
    public Node currentNode() {
        if (phase != Phase.SCRAM) {
            throw new IllegalStateException("currentNode: Error, " +
                "current Node may not be accessed unless scramming");
        }
        return position;
    }

    @Override
    public Node exit() {
        if (phase != Phase.SCRAM) {
            throw new IllegalStateException("getEntrance: Error, " +
                "current Node may not be accessed unless scramming");
        }
        return scramSewer.ring();
    }

    @Override
    public Collection<Node> allNodes() {
        if (phase != Phase.SCRAM) {
            throw new IllegalStateException("getVertices: Error, " +
                "Vertices may not be accessed unless scramming");
        }
        return Collections.unmodifiableSet(scramSewer.graph());
    }

    /** Attempt to move the sewerDiver from the current position to the<br>
     * <tt>Node</tt> <tt>n</tt>. Throw an <tt>IllegalArgumentException</tt> <br>
     * if <tt>n</tt> is not neighboring. <br>
     * Increment the steps taken if successful. */
    @Override
    public void moveTo(Node n) {
        if (phase != Phase.SCRAM) {
            throw new IllegalStateException("Call moveTo(Node) only when scramming!");
        }
        int distance= position.edge(n).length;
        if (stepsToGo - distance < 0) throw new OutOfTimeException();

        if (!position.neighbors().contains(n))
            throw new IllegalArgumentException("moveTo: Node must be adjacent to position");
        position= n;
        stepsToGo-= distance;
        gui.ifPresent((g) -> g.updateStepsToGo(stepsToGo));
        gui.ifPresent((g) -> { g.moveTo(n); });
        grabCoins();
    }

    /** Pick up coins. <br>
     * Coins on a Node n are picked up automatically when the scram phase starts and<br>
     * when a call moveTo(n) is executed. */
    void grabCoins() {
        if (phase != Phase.SCRAM) {
            throw new IllegalStateException("Call grabCoins() only when scramming!");
        }
        coinsCollected+= position.getTile().takeCoins();
        gui.ifPresent((g) -> g.updateCoins(coinsCollected, getScore()));
    }

    @Override
    /** Return the number of steps remaining to scram. */
    public int stepsToGo() {
        if (phase != Phase.SCRAM) {
            throw new IllegalStateException(
                "stepsToGo() can be called only while scramming!");
        }
        return stepsToGo;
    }

    /* package */ int getCoinsCollected() {
        return coinsCollected;
    }

    /** Return the player's current score. */
    /* package */ int getScore() {
        return (int) (computeBonusFactor() * coinsCollected);
    }

    /* package */ boolean getSeekSucceeded() {
        return seekSucceeded;
    }

    /* package */ boolean getScramSucceeded() {
        return scramSucceeded;
    }

    /* package */ boolean getSeekErrored() {
        return seekErred;
    }

    /* package */ boolean getScramErrored() {
        return scramErred;
    }

    /* package */ boolean getSeekTimeout() {
        return seekTimedOut;
    }

    /* package */ boolean getScramTimeout() {
        return scramTimedOut;
    }

    /* package */ int getMinSeekDistance() {
        return minSeekDistance;
    }

    /* package */ int getMinScramDistance() {
        return minScramDistance;
    }

    /* package */ int getSeekStepsLeft() {
        return seekStepsLeft;
    }

    /* package */ int getScramStepsLeft() {
        return scramStepsLeft;
    }

    /** Given seed, whether or not to use the GUI, and an instance of <br>
     * a solution to use, run the game. */
    public static int runNewGame(long seed, boolean useGui, SewerDiver solution) {
        GameState state;
        if (seed != 0) {
            state= new GameState(seed, useGui, solution);
        } else {
            state= new GameState(useGui, solution);
        }
        outPrintln("Seed : " + state.seed);
        state.run();
        return state.getScore();
    }

    /** Execute seek and scram on a random seed, except that: <br>
     * (1) If there is a parameter -s <seed>, run on that seed OR <br>
     * (2) If there is a parameter -n <count>, run count times on random seeds. */
    public static void main(String[] args) throws IOException {
        List<String> argList= new ArrayList<>(Arrays.asList(args));
        int repeatNumberIndex= argList.indexOf("-n");
        int numTimesToRun= 1;
        if (repeatNumberIndex >= 0) {
            try {
                numTimesToRun= Math.max(Integer.parseInt(argList.get(repeatNumberIndex + 1)), 1);
            } catch (Exception e) {
                // numTimesToRun = 1
            }
        }
        int seedIndex= argList.indexOf("-s");
        long seed= 0;
        if (seedIndex >= 0) {
            try {
                seed= Long.parseLong(argList.get(seedIndex + 1));
            } catch (NumberFormatException e) {
                errPrintln("Error, -s must be followed by a numerical seed");
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                errPrintln("Error, -s must be followed by a seed");
                return;
            }
        }

        int totalScore= 0;
        for (int i= 0; i < numTimesToRun; i++ ) {
            totalScore+= runNewGame(seed, false, new McDiver());
            if (seed != 0) seed= new Random(seed).nextLong();
            outPrintln("");
        }

        outPrintln("Average score : " + totalScore / numTimesToRun);
    }

    static void outPrintln(String s) {
        if (shouldPrint) System.out.println(s);
    }

    static void errPrintln(String s) {
        if (shouldPrint) System.err.println(s);
    }
}
