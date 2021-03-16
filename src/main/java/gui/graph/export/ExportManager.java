package gui.graph.export;

import gui.graph.dag.DirectedGraph;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

public class ExportManager {

    public static JSONObject projectToJson(@Nonnull DirectedGraph dag) {
        JSONObject o = new JSONObject();
        JSONArray rootNodes = new JSONArray();
        List<Node<GraphOperator>> graph = dag.getGraph();
        for (Node<GraphOperator> op : graph) {
            rootNodes.put(getSuccessors(op));
        }
        o.put("nodes", rootNodes);
        return o;
    }

    private static JSONObject getSuccessors(@Nonnull Node<GraphOperator> node) {
        JSONObject o = new JSONObject();
        o.put("data", node.getItem().toJsonObject());
        JSONArray arr = new JSONArray();
        if (!node.getSuccessors().isEmpty()) {
            for (Node<GraphOperator> op : node.getSuccessors()) {
                arr.put(getSuccessors(op));
            }
        }
        if (!arr.isEmpty()) {
            o.put("successors", arr);
        }
        return o;
    }
}
