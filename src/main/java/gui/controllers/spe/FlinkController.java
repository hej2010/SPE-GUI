package gui.controllers.spe;

import com.brunomnsilva.smartgraph.graph.Graph;
import gui.graph.dag.DirectedGraph;
import gui.graph.dag.Node;
import gui.graph.data.*;

import java.util.List;

public class FlinkController {

    public static void updateGraphOnConnect(GraphOperator from, GraphOperator to, Graph<GraphOperator, GraphStream> graph) {
        DirectedGraph dag = DirectedGraph.fromGraphView(graph);
        List<Node<GraphOperator>> list = dag.getGraph();
        to.setPrevIdentifier(from.getIdentifier());
        if (from instanceof SourceOperator || from instanceof Operator) {
            updateSuccessors(findNode(to, list), list);
        }
    }

    private static void updateSuccessors(Node<GraphOperator> node, List<Node<GraphOperator>> successors) {
        if (node == null || successors == null || successors.isEmpty()) {
            return;
        }

        for (Node<GraphOperator> n : node.getSuccessors()) {
            n.getItem().setPrevIdentifier(node.getItem().getIdentifier());
            updateSuccessors(n, n.getSuccessors());
        }
    }

    private static Node<GraphOperator> findNode(GraphOperator to, List<Node<GraphOperator>> list) {
        if (list != null && !list.isEmpty()) {
            for (Node<GraphOperator> n : list) {
                if (to.equals(n.getItem())) {
                    return n;
                }
            }
        }

        return null;
    }

}
