package gui.graph.export;

import gui.graph.dag.DirectedGraph;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import gui.graph.data.SinkOperator;
import gui.graph.data.SourceOperator;
import gui.spe.ParsedOperator;
import gui.spe.ParsedSPE;
import gui.utils.Files;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
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

    public static List<Node<GraphOperator>> projectFromFile(@Nonnull File file, @Nonnull ParsedSPE parsedSPE) {
        List<Node<GraphOperator>> list = new LinkedList<>();
        String s;
        try {
            s = Files.readFile(file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not open file " + file.toString());
            return null;
        }
        JSONObject o = new JSONObject(s);
        JSONArray arr = o.getJSONArray("nodes");

        for (int i = 0; i < arr.length(); i++) {
            JSONObject node = arr.getJSONObject(i);
            list.add(getNode(node, parsedSPE));
        }

        return list;
    }

    /**
    {nodes
        [
            { // node
                data: {
                    ops: {}
                    name: "name"
                },
                successors: [{...}]
            }
        ]
    }
     */
    private static Node<GraphOperator> getNode(JSONObject node, ParsedSPE parsedSPE){
        JSONObject data = node.getJSONObject("data");
        String name = data.getString("name");
        int type = data.getInt("type");
        GraphOperator op;
        if (type == ParsedOperator.TYPE_SOURCE_OPERATOR) {
            op = new SourceOperator(name);
        } else if (type == ParsedOperator.TYPE_REGULAR_OPERATOR) {
            op = new Operator(name);
        } else {
            op = new SinkOperator(name);
        }
        op = op.fromJsonObject(data, parsedSPE);
        List<Node<GraphOperator>> successors = new LinkedList<>();
        if (node.has("successors")) {
            JSONArray arr = node.getJSONArray("successors");
            for (int i = 0; i < arr.length(); i++) {
                successors.add(getNode(arr.getJSONObject(i), parsedSPE));
            }
        }
        return new Node<>(op, successors);
    }
}
