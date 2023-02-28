package diver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import graph.Node;
import graph.NodeStatus;
import graph.ScramState;
import graph.SeekState;
import graph.SewerDiver;

public class McDiver extends SewerDiver {

    /** Find the ring in as few steps as possible. Once you get there, <br>
     * you must return from this function in order to pick<br>
     * it up. If you continue to move after finding the ring rather <br>
     * than returning, it will not count.<br>
     * If you return from this function while not standing on top of the ring, <br>
     * it will count as a failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the ring in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the ring at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToRing() in state.<br>
     * You know you are standing on the ring when distanceToRing() is 0.
     *
     * Use function moveTo(long id) in state to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the ring, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first walk. <br>
     * Some modification is necessary to make the search better, in general. */
    @Override
    public void seek(SeekState state) {
        // TODO : Look for the ring and return.
        // DO NOT WRITE ALL THE CODE HERE. DO NOT MAKE THIS METHOD RECURSIVE.
        // Instead, write your method (it may be recursive) elsewhere, with a
        // good specification, and call it from this one.
        //
        // Working this way provides you with flexibility. For example, write
        // one basic method, which always works. Then, make a method that is a
        // copy of the first one and try to optimize in that second one.
        // If you don't succeed, you can always use the first one.
        //
        // Use this same process on the second method, scram.
        HashSet<Long> set= new HashSet<>();
        dfsPathWalk(state, set);
    }

    /** A DFS recursive method that will visit every reachable node along <br>
     * a path of unvisited nodes.<br>
     * Start from state and traverse by shortest distance to ring then return <br>
     * when McDiver is on the ring. */
    private void dfsPathWalk(SeekState state, HashSet<Long> visited) {
        if (state.distanceToRing() == 0) return; // on ring
        List<NodeStatus> ls= new ArrayList<>();
        ls.addAll(state.neighbors());
        Collections.sort(ls);
        long start= state.currentLocation(); // keep McDiver's start location
        visited.add(start);
        for (NodeStatus x : ls) {
            long id= x.getId();
            if (!visited.contains(id)) {
                state.moveTo(id);
                dfsPathWalk(state, visited);
                if (state.distanceToRing() == 0) return;
                state.moveTo(start);
            }
        }
    }

    /** Scram --get out of the sewer system before the steps are all used, trying to <br>
     * collect as many coins as possible along the way. McDiver must ALWAYS <br>
     * get out before the steps are all used, and this should be prioritized above<br>
     * collecting coins.
     *
     * You now have access to the entire underlying graph, which can be accessed<br>
     * through ScramState. currentNode() and exit() return Node objects<br>
     * of interest, and allNodes() returns a collection of all nodes on the graph.
     *
     * You have to get out of the sewer system in the number of steps given by<br>
     * stepsToGo(); for each move along an edge, this number is <br>
     * decremented by the weight of the edge taken.
     *
     * Use moveTo(n) to move to a node n that is adjacent to the current node.<br>
     * When n is moved-to, coins on node n are automatically picked up.
     *
     * You must return from this function while standing at the exit. Failing <br>
     * to do so before steps run out or returning from the wrong node will be<br>
     * considered a failed run.
     *
     * Initially, there are enough steps to get from the starting point to the<br>
     * exit using the shortest path, although this will not collect many coins.<br>
     * For this reason, a good starting solution is to use the shortest path to<br>
     * the exit. */
    @Override
    public void scram(ScramState state) {
        // TODO: Get out of the sewer system before the steps are used up.
        // DO NOT WRITE ALL THE CODE HERE. Instead, write your method elsewhere,
        // with a good specification, and call it from this one.
        coinsByDistHeap(state);
    }

    /** Return total coins by adding each node's coins from the list. */
    private int coinSum(List<Node> p) {
        int sum= 0;
        for (Node n : p) {
            sum+= n.getTile().coins();
        }
        return sum;
    }

    /** Walk a path from McDiver's current node to the last node. */
    private void pathWalk(ScramState s, List<Node> p) {
        for (Node n : p) {
            if (n != s.currentNode()) s.moveTo(n); // move to next node
        }
    }

    /** Walk several paths in the graph to every reachable node from McDiver's <br>
     * current node. Traverse paths by highest priority, where priority is the <br>
     * total coins on the shortest path from McDiver's current node <br>
     * to that node over that shortest path's distance.<br>
     * Walk to exit and finish there if stepsToGo() is greater than the <br>
     * total distance from McDiver's current node to<br>
     * that node with the distance from that node to the exit. */
    private void coinsByDistHeap(ScramState sc) {
        Heap<Node> m= new Heap<>(false); // make a maxHeap
        Node curr= sc.currentNode();
        for (Node n : sc.allNodes()) {
            if (n != curr) {
                List<Node> sp= A7.dijkstra(curr, n);
                m.insert(n, coinSum(sp) / A7.sumOfPath(sp));
            }
        }

        while (m.size > 0) {
            Node n= m.poll(); // get node with highest priority from heap
            List<Node> d= A7.dijkstra(curr, n);
            List<Node> d2= A7.dijkstra(n, sc.exit());
            if (A7.sumOfPath(d) + A7.sumOfPath(d2) < sc.stepsToGo()) {
                pathWalk(sc, d);
                curr= sc.currentNode(); // update current node to the node walked to
                for (int i= 0; i < m.size; i++ ) {
                    Node new_n= m.b[i].value;
                    d= A7.dijkstra(curr, new_n);
                    m.changePriority(new_n, coinSum(d) / A7.sumOfPath(d));
                }
            }
        }
        pathWalk(sc, A7.dijkstra(curr, sc.exit())); // walk to exit
    }
}