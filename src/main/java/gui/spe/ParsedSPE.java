package gui.spe;

import gui.graph.dag.DirectedGraph;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class ParsedSPE {
    final String name;
    final List<ParsedOperator> operators;
    final List<String> baseImports, baseDefinition;
    final Map<String, List<String>> operatorImportsMap;

    ParsedSPE(@Nonnull String name, @Nonnull List<ParsedOperator> operators, @Nonnull List<String> baseImports,
              @Nonnull List<String> baseDefinition, @Nonnull Map<String, List<String>> operatorImportsMap) {
        this.name = name;
        this.operators = operators;
        this.baseImports = baseImports;
        this.baseDefinition = baseDefinition;
        this.operatorImportsMap = operatorImportsMap;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public List<ParsedOperator> getOperators() {
        return operators;
    }

    @Nonnull
    public List<String> getOperatorNames() {
        List<String> name = new LinkedList<>();
        for (ParsedOperator o : operators) {
            name.add(o.getName());
        }
        return name;
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
    public abstract String generateCodeFrom(@Nonnull DirectedGraph directedGraph);
}
