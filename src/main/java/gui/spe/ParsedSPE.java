package gui.spe;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import gui.graph.dag.DirectedGraph;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ParsedSPE {
    private final String name;
    private final List<ParsedOperator> operators;
    private final List<String> baseImports, baseDefinition;
    private final Map<String, List<String>> operatorImportsMap;
    private final Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap;

    ParsedSPE(@Nonnull String name, @Nonnull List<ParsedOperator> operators, @Nonnull List<String> baseImports, @Nonnull List<String> baseDefinition,
              @Nonnull Map<String, List<String>> operatorImportsMap, @Nonnull Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap) {
        this.name = name;
        this.operators = operators;
        this.baseImports = baseImports;
        this.baseDefinition = baseDefinition;
        this.operatorImportsMap = operatorImportsMap;
        this.codeToOpMap = codeToOpMap;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getFileName() {
        return name.replace(" ", "");
    }

    @Nonnull
    public List<ParsedOperator> getOperators() {
        return operators;
    }

    @Nonnull
    public List<String> getOperatorNames(int type) {
        List<String> name = new LinkedList<>();
        for (ParsedOperator o : operators) {
            if (type == o.getType()) {
                name.add(o.getOperatorName());
            }
        }
        return name;
    }

    @Nonnull
    public Map<String, Pair<Class<? extends GraphOperator>, String>> getCodeToOpMap() {
        return codeToOpMap;
    }

    @Nonnull
    public List<String> getBaseImports() {
        return baseImports;
    }

    @Nonnull
    public List<String> getImportsForOperator(@Nonnull String op) {
        return operatorImportsMap.getOrDefault(op, new LinkedList<>());
    }

    @Nonnull
    public List<String> getBaseDefinition() {
        return baseDefinition;
    }

    @Nonnull
    public abstract String generateCodeFrom(@Nonnull DirectedGraph directedGraph, @Nonnull ParsedSPE parsedSPE, @Nonnull String fileName);

    @Nonnull
    protected String getFormattedCode(@Nonnull StringBuilder sb) {
        //return sb.toString();
        try {
            return new Formatter().formatSourceAndFixImports(sb.toString());
        } catch (FormatterException e) {
            e.printStackTrace();
            return sb.toString();
        }
    }

    /**
     * Recursively adds all node imports
     */
    protected void addNodeImports(@Nonnull StringBuilder sb, @Nonnull List<Node<GraphOperator>> graph, @Nonnull ParsedSPE parsedSPE) {
        for (Node<GraphOperator> op : graph) {
            ParsedOperator pop = op.getItem().getCurrentOperator();
            if (pop != null) {
                List<String> imports = parsedSPE.getImportsForOperator(pop.getOperatorName());
                addStringRow(sb, imports, true);
            }
            addNodeImports(sb, op.getSuccessors(), parsedSPE);
        }
    }

    protected <E> void addStringRow(@Nonnull StringBuilder sb, @Nonnull List<E> list, boolean isImports) {
        for (E e : list) {
            if (isImports) {
                sb.append("import ").append(e).append(";\n");
            } else {
                sb.append(e).append("\n");
            }
        }
    }

    protected void addNodeCode(@Nonnull StringBuilder sb, @Nonnull List<Node<GraphOperator>> graph, @Nonnull Set<String> addedNodes) {
        for (Node<GraphOperator> node : graph) {
            GraphOperator op = node.getItem();
            ParsedOperator pop = op.getCurrentOperator();
            if (pop != null) {
                ParsedOperator.Definition definition = pop.getDefinition();
                if (!addedNodes.contains(op.getIdentifier().get())) {
                    sb.append(definition.getCode(op)).append("\n");
                    addedNodes.add(op.getIdentifier().get());
                }
            } else {
                sb.append("//").append(op.getIdentifier()).append("\n");
            }
            addNodeCode(sb, node.getSuccessors(), addedNodes);
        }
    }
}
