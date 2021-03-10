package org.example.graph.dag;

import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.Vertex;
import org.example.graph.data.GraphOperator;
import org.example.graph.data.GraphStream;
import org.example.graph.data.SourceOperator;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class DirectedGraph {
    private final List<Node<GraphOperator>> sourceOps;

    private DirectedGraph(List<Node<GraphOperator>> sourceOps) {
        this.sourceOps = sourceOps;
    }

    public List<Node<GraphOperator>> getGraph() {
        return sourceOps;
    }

    @Nonnull
    public static DirectedGraph fromGraphView(@Nonnull Graph<GraphOperator, GraphStream> graph) {
        final Collection<Vertex<GraphOperator>> ops = graph.vertices();
        final List<Node<GraphOperator>> sourcesList = new LinkedList<>();

        graph.vertices().forEach(v -> { // find all source operators
            if (v.element() instanceof SourceOperator) {
                sourcesList.add(new Node<>(v.element(), getSuccessorsFrom(v, graph)));
            }
        });

        return new DirectedGraph(sourcesList);
    }

    /**
     * Given a node, recursively finds all successors
     *
     * @param node  the node to find all successors for
     * @param graph the graph
     * @return a list of all successor Nodes
     */
    @Nonnull
    private static List<Node<GraphOperator>> getSuccessorsFrom(Vertex<GraphOperator> node, Graph<GraphOperator, GraphStream> graph) {
        List<Vertex<GraphOperator>> foundSuccessors = findSuccessorsFor(node, graph);
        List<Node<GraphOperator>> successorsList = new LinkedList<>();
        foundSuccessors.forEach(successor -> successorsList.add(new Node<>(successor.element(), getSuccessorsFrom(successor, graph)))); // recursively find successors
        return successorsList;
    }

    @Nonnull
    private static List<Vertex<GraphOperator>> findSuccessorsFor(Vertex<GraphOperator> node, Graph<GraphOperator, GraphStream> graph) {
        List<Vertex<GraphOperator>> successors = new LinkedList<>();

        Collection<Edge<GraphStream, GraphOperator>> incidentEdges = graph.incidentEdges(node);

        incidentEdges.forEach(e -> {
            if (e.vertices()[0].element().equals(node.element())) { // if the edge starts at this node
                successors.add(e.vertices()[1]);
            }
        });

        return successors;
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
}
