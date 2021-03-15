package gui.spe;

import gui.graph.dag.DirectedGraph;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class ParsedFlinkSPE extends ParsedSPE {

    ParsedFlinkSPE(@NotNull String name, @NotNull List<ParsedOperator> operators, @NotNull List<String> baseImports, @NotNull List<String> baseDefinition, @NotNull Map<String, List<String>> operatorImportsMap) {
        super(name, operators, baseImports, baseDefinition, operatorImportsMap);
    }

    @Nonnull
    @Override
    public String generateCodeFrom(@NotNull DirectedGraph directedGraph) {

        return "todo";
    }

    // TODO the connection method etc is different for each SPE
}
