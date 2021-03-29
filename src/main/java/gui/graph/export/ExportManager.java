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
    public static final String EXPORT_SPE = "spe";
    public static final String EXPORT_NAME = "name";
    public static final String EXPORT_PREV_NAME = "prev_name";
    public static final String EXPORT_OPS = "ops";
    public static final String EXPORT_DATA = "data";
    public static final String EXPORT_TYPE = "type";
    public static final String EXPORT_NODES = "nodes";
    public static final String EXPORT_MIDDLE = "mid";
    public static final String EXPORT_IN = "in";
    public static final String EXPORT_OUT = "out";
    public static final String EXPORT_SUCCESSORS = "suc";
    public static final String EXPORT_DEFINITION = "def";

    public static JSONObject projectToJson(@Nonnull DirectedGraph dag, ParsedSPE spe) {
        JSONObject o = new JSONObject();
        JSONArray rootNodes = new JSONArray();
        List<Node<GraphOperator>> graph = dag.getGraph();
        for (Node<GraphOperator> op : graph) {
            rootNodes.put(getSuccessors(op));
        }
        o.put(EXPORT_NODES, rootNodes);
        o.put(EXPORT_SPE, spe.getName());
        return o;
    }

    private static JSONObject getSuccessors(@Nonnull Node<GraphOperator> node) {
        JSONObject o = new JSONObject();
        o.put(EXPORT_DATA, node.getItem().toJsonObject());
        JSONArray arr = new JSONArray();
        if (!node.getSuccessors().isEmpty()) {
            for (Node<GraphOperator> op : node.getSuccessors()) {
                arr.put(getSuccessors(op));
            }
        }
        if (!arr.isEmpty()) {
            o.put(EXPORT_SUCCESSORS, arr);
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
        JSONArray arr = o.getJSONArray(EXPORT_NODES);
        String spe = o.getString(EXPORT_SPE);
        if (!parsedSPE.getName().equals(spe)) {
            throw new RuntimeException("Wrong SPE selected! Wanted " + parsedSPE.getName() + " but found " + spe);
        }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject node = arr.getJSONObject(i);
            list.add(getNode(node, parsedSPE));
        }

        return list;
    }

    private static Node<GraphOperator> getNode(JSONObject node, ParsedSPE parsedSPE) {
        JSONObject data = node.getJSONObject(EXPORT_DATA);
        String name = data.getString(EXPORT_NAME);
        int type = data.getInt(EXPORT_TYPE);
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
        if (node.has(EXPORT_SUCCESSORS)) {
            JSONArray arr = node.getJSONArray(EXPORT_SUCCESSORS);
            for (int i = 0; i < arr.length(); i++) {
                successors.add(getNode(arr.getJSONObject(i), parsedSPE));
            }
        }
        return new Node<>(op, successors);
    }
}
