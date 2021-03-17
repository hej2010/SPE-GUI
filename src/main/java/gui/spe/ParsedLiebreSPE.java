package gui.spe;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import gui.graph.dag.DirectedGraph;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class ParsedLiebreSPE extends ParsedSPE {

    ParsedLiebreSPE(@NotNull String name, @NotNull List<ParsedOperator> operators, @NotNull List<String> baseImports, @NotNull List<String> baseDefinition, @NotNull Map<String, List<String>> operatorImportsMap) {
        super(name, operators, baseImports, baseDefinition, operatorImportsMap);
    }

    @Nonnull
    @Override
    public String generateCodeFrom(@Nonnull DirectedGraph directedGraph, @Nonnull ParsedSPE parsedSPE, @Nonnull String fileName) {
        StringBuilder sb = new StringBuilder();

        addStringRow(sb, parsedSPE.getBaseImports(), true);

        List<Node<GraphOperator>> graph = directedGraph.getGraph();
        addNodeImports(sb, graph, parsedSPE);

        sb.append("\npublic class ").append(fileName).append(" {\npublic static void main(String[] args) {\n");
        addStringRow(sb, parsedSPE.getBaseDefinition(), false);
        addNodeCode(sb, graph);
        sb.append("\n");
        connect(sb, graph);
        sb.append("\nq.activate();\n}\n}");

        try {
            return new Formatter().formatSourceAndFixImports(sb.toString());
        } catch (FormatterException e) {
            e.printStackTrace();
            return sb.toString();
        }
    }

    /**
     * Connects the nodes
     */
    private void connect(StringBuilder sb, List<Node<GraphOperator>> graph) {
        for (Node<GraphOperator> node : graph) {
            List<Node<GraphOperator>> connectedWith = node.getSuccessors();
            for (Node<GraphOperator> successor : connectedWith) {
                ParsedOperator pop = node.getItem().getCurrentOperator();
                ParsedOperator popSuccessor = successor.getItem().getCurrentOperator();
                if (pop != null && popSuccessor != null) {
                    sb.append("\nq.connect(").append(pop.getDefinition().getIdentifier()).append(", ").append(popSuccessor.getDefinition().getIdentifier()).append(");");
                }
            }
            connect(sb, connectedWith);
        }
    }

    private void addNodeCode(StringBuilder sb, List<Node<GraphOperator>> graph) {
        for (Node<GraphOperator> node : graph) {
            ParsedOperator pop = node.getItem().getCurrentOperator();
            if (pop != null) {
                ParsedOperator.Definition definition = pop.getDefinition();
                sb.append(definition.getCode()).append("\n");
            }
            addNodeCode(sb, node.getSuccessors());
        }
    }

    /**
     * Recursively adds all node imports
     */
    private void addNodeImports(@Nonnull StringBuilder sb, @Nonnull List<Node<GraphOperator>> graph, @Nonnull ParsedSPE parsedSPE) {
        for (Node<GraphOperator> op : graph) {
            ParsedOperator pop = op.getItem().getCurrentOperator();
            if (pop != null) {
                List<String> imports = parsedSPE.getImportsForOperator(pop.getOperatorName());
                addStringRow(sb, imports, true);
            }
            addNodeImports(sb, op.getSuccessors(), parsedSPE);
        }
    }

    private <E> void addStringRow(@Nonnull StringBuilder sb, @Nonnull List<E> list, boolean isImports) {
        for (E e : list) {
            if (isImports) {
                sb.append("import ").append(e).append(";\n");
            } else {
                sb.append(e).append("\n");
            }
        }
    }

}
