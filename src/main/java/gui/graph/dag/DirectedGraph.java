package gui.graph.dag;

import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.Vertex;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.*;

public class DirectedGraph {
    private final List<Node<GraphOperator>> sourceOps;
    private final int nodeCount;

    private DirectedGraph(List<Node<GraphOperator>> sourceOps) {
        this.sourceOps = sourceOps;
        int count = 0;
        for (Node<GraphOperator> n : sourceOps) {
            count += countNodes(n);
        }
        nodeCount = count;
        //System.out.println("Nodes: " + nodeCount);
    }

    private int countNodes(Node<GraphOperator> n) {
        int count = 1;
        for (Node<GraphOperator> n2 : n.getSuccessors()) {
            count += countNodes(n2);
        }
        return count;
    }

    public List<Node<GraphOperator>> getGraph() {
        return sourceOps;
    }

    @Nonnull
    public static DirectedGraph fromGraphView(@Nonnull Graph<GraphOperator, GraphStream> graph) {
        final List<Node<GraphOperator>> sourcesList = new LinkedList<>();
        final Pair<Map<GraphOperator, List<GraphOperator>>, Map<GraphOperator, List<GraphOperator>>> edgesMaps = getEdgesMap(graph.edges());

        graph.vertices().forEach(v -> { // find all source operators
            if (!hasPredecessor(v, edgesMaps.getValue())) {
                sourcesList.add(new Node<>(v.element(), getSuccessorsFrom(v.element(), edgesMaps.getKey())));
            }
        });

        return new DirectedGraph(sourcesList);
    }

    /**
     * First in pair is outbound (maps operator to its outbound operators), second is inbound
     */
    private static Pair<Map<GraphOperator, List<GraphOperator>>, Map<GraphOperator, List<GraphOperator>>> getEdgesMap(Collection<Edge<GraphStream, GraphOperator>> edges) {
        Map<GraphOperator, List<GraphOperator>> inbound = new HashMap<>(); // Maps an operator to all its inbound edges (operators)
        Map<GraphOperator, List<GraphOperator>> outbound = new HashMap<>(); // Maps an operator to all its outbound edges (operators)
        for (Edge<GraphStream, GraphOperator> e : edges) {
            GraphOperator from = e.vertices()[0].element();
            GraphOperator to = e.vertices()[1].element();
            List<GraphOperator> outboundList, inboundList;
            if (outbound.containsKey(from)) {
                outboundList = outbound.get(from);
            } else {
                outboundList = new LinkedList<>();
            }
            if (inbound.containsKey(to)) {
                inboundList = inbound.get(to);
            } else {
                inboundList = new LinkedList<>();
            }
            outboundList.add(to);
            inboundList.add(from);
            outbound.put(from, outboundList);
            inbound.put(to, inboundList);
        }
        return new Pair<>(outbound, inbound);
    }

    /**
     * Given a node, recursively finds all successors
     *
     * @param sourceNode the node to find all successors for
     * @param outgoing   the Map of outgoing edges
     * @return a list of all successor Nodes
     */
    @Nonnull
    private static List<Node<GraphOperator>> getSuccessorsFrom(GraphOperator sourceNode, Map<GraphOperator, List<GraphOperator>> outgoing) {
        List<GraphOperator> successors = outgoing.get(sourceNode);
        if (successors == null) {
            return new LinkedList<>();
        }

        List<Node<GraphOperator>> successorsList = new LinkedList<>();
        successors.forEach(successor -> successorsList.add(new Node<>(successor, getSuccessorsFrom(successor, outgoing)))); // recursively find successors
        return successorsList;
    }

    /**
     * Checks if the given node has any incoming edges (i.e. not a source op)
     *
     * @param node     the node to check
     * @param incoming the map of operator->incoming
     * @return true if the node has no incoming edges
     */
    private static boolean hasPredecessor(Vertex<GraphOperator> node, Map<GraphOperator, List<GraphOperator>> incoming) {
        return incoming.containsKey(node.element()) && !incoming.get(node.element()).isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DirectedGraph{\n");
        sb.append("sourceOps=\n");
        for (Node<GraphOperator> n : sourceOps) {
            sb.append(n.toString()).append("\n");
        }
        return sb.append('}').toString();
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
