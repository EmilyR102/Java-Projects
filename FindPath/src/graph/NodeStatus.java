package graph;

import java.util.Objects;

public final class NodeStatus implements Comparable<NodeStatus> {
    /** This node's id */
    private final long id;

    /** distance to ring */
    private final int distance;

    /** Constructor: an instance with id nodeId and distance dist to the ring */
    /* package */ NodeStatus(long nodeId, int dist) {
        id= nodeId;
        distance= dist;
    }

    /** Return the Id of the Node that corresponds to this NodeStatus. */
    public long getId() {
        return id;
    }

    /** Return the distance to the ring from the Node that corresponds <br>
     * to this NodeStatus. */
    public int getDistanceToRing() {
        return distance;
    }

    /** If the distances of this and other are equal, return neg, 0 or pos <br>
     * depending on whether this id is <, = or > other's id.<br>
     * Otherwise, return neg, or pos number depending on whether this's <br>
     * distance is <, or > other's distance. */
    @Override
    public int compareTo(NodeStatus other) {
        // if (distance != other.distance) return Integer.compare(distance, other.distance);
        if (distance == other.distance) return Long.compare(id, other.id);
        return distance - other.distance;
    }

    /** Return true iff ob is a NodeStatus with the same id as this one.<br>
     * We don't have to be concerned with ob being a subclass of NodeStatus<br>
     * because this class is declared final. */
    @Override
    public boolean equals(Object ob) {
        if (ob == this) return true;
        if (!(ob instanceof NodeStatus)) return false;
        return id == ((NodeStatus) ob).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
