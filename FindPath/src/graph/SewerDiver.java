package graph;

/** An abstract class representing what methods a sewer diver<br>
 * must implement in order to be used in solving the game. */
public abstract class SewerDiver {

    /** Have McDiver find the ring in the sewer, in as few steps as possible. <br>
     * Once the ring is found, return from method seek in order to pick it up. <br>
     * If McDiver continues to move after finding the ring rather than returning, <br>
     * it will not count. If McDiver returns from this function while not standing on top <br>
     * of the ring, it counts as a failure.
     *
     * There is no limit to how many steps McDiver can take, but McDiver will receive<br>
     * a score bonus multiplier for finding the ring in fewer steps.
     *
     * At every step, McDiver knows only the current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the ring at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToRing() in SeekState.<br>
     * McDiver is standing on the ring when distanceToRing() is 0.
     *
     * Use function moveTo(long id) in SeekState to move to a neighboring tile <br>
     * by its ID. Doing this will change state to reflect McDiver's new position.
     *
     * A suggested first implementation that will always find the ring, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first walk.
     *
     * @param state the information available at the current state */
    public abstract void seek(SeekState state);

    /** Get McDiver out of the sewer within a certain number of steps, trying to <br>
     * collect as many coins as possible along the way. Your solution must ALWAYS <br>
     * get out before using up all the steps, and this should be prioritized <br>
     * above collecting coins.
     *
     * McDiver now has access to the entire underlying graph, which can be accessed<br>
     * through ScramState. currentNode() and exit() return Node objects of<br>
     * interest, and allNodes() returns a collection of all nodes on the graph.
     *
     * Look at interface ScramState.<br>
     * You can find out how many steps are left for McDiver to take using function<br>
     * stepsToGo(). Each time McDiver traverses an edge, the steps-to-go are<br>
     * decremented by the weight of that edge. Coins on a tile will be automatically<br>
     * picked up when moveTo() moves McDiver to a destination node adjacent <br>
     * to your current node.
     *
     * McDiver must return from this function while standing at the exit. Failing to <br>
     * do so within the steps left or returning from the wrong location will be <br>
     * considered a failed run.
     *
     * McDiver will always have enough time to get out using the shortest path from <br>
     * the starting position to the exit, although this will not collect many coins. <br>
     * For this reason, using Dijkstra's to plot the shortest path to the <br>
     * exit is a good starting solution.
     *
     * @param state the information available at the current state */
    public abstract void scram(ScramState state);
}
