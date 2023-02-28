package graph;

import java.util.Collection;

/** The state of the game while looking for the ring.<br>
 * In order to determine the next move, you need to call the various methods<br>
 * of this interface. To move through the sewer system, you need to call moveTo(long).
 *
 * An instance provides all the information necessary<br>
 * to search through the sewer system and find the ring. */
public interface SeekState {
    /** Return the unique identifier associated with McDiver's current location. */
    long currentLocation();

    /** Return an unordered collection of NodeStatus objects<br>
     * associated with all direct neighbors of McDiver's current location.<br>
     * Each status contains a unique identifier for the neighboring node<br>
     * as well as the distance of that node to the ring along the grid<br>
     * <br>
     * (NB: This is NOT the distance in the graph, it is only the number<br>
     * of rows and columns away from the ring.)<br>
     * <br>
     * It is possible to move directly to any node identifier in this collection. */
    Collection<NodeStatus> neighbors();

    /** Return McDiver's current distance along the grid (NOT THE GRAPH) <br>
     * from the ring. */
    int distanceToRing();

    /** Change McDiver's current location to the node given by id.<br>
     * Throw an IllegalArgumentException if the node with id id is <br>
     * not adjacent to McDiver's current location. */
    void moveTo(long id);
}
