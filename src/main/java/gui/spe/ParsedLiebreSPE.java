package gui.spe;

import gui.graph.dag.DirectedGraph;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParsedLiebreSPE extends ParsedSPE {

    ParsedLiebreSPE(@NotNull String name, @NotNull List<ParsedOperator> operators, @NotNull List<String> baseImports, @NotNull List<String> baseDefinition, @NotNull Map<String, List<String>> operatorImportsMap) {
        super(name, operators, baseImports, baseDefinition, operatorImportsMap);
    }

    @Nonnull
    @Override
    public String generateCodeFrom(@Nonnull DirectedGraph directedGraph, @Nonnull ParsedSPE parsedSPE, @Nonnull String fileName) {
        StringBuilder sb = new StringBuilder();
        Set<String> addedNodes = new HashSet<>();

        addStringRow(sb, parsedSPE.getBaseImports(), true);

        List<Node<GraphOperator>> graph = directedGraph.getGraph();
        addNodeImports(sb, graph, parsedSPE);

        sb.append("\npublic class ").append(fileName).append(" {\npublic static void main(String[] args) {\n");
        addStringRow(sb, parsedSPE.getBaseDefinition(), false);
        addNodeCode(sb, graph, addedNodes);
        sb.append("\n");
        connect(sb, graph);
        sb.append("\nq.activate();\n}\n}");

        return getFormattedCode(sb);
    }

    /**
     * Connects the nodes
     */
    void connect(StringBuilder sb, List<Node<GraphOperator>> graph) {
        for (Node<GraphOperator> node : graph) {
            List<Node<GraphOperator>> connectedWith = node.getSuccessors();
            for (Node<GraphOperator> successor : connectedWith) {
                GraphOperator op = node.getItem();
                GraphOperator popSuccessor = successor.getItem();
                sb.append("\nq.connect(").append(op.getIdentifier()).append(", ").append(popSuccessor.getIdentifier()).append(");");
            }
            connect(sb, connectedWith);
        }
    }

}
