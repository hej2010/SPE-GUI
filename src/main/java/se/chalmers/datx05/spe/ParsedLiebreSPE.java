package se.chalmers.datx05.spe;

import org.jetbrains.annotations.NotNull;
import se.chalmers.datx05.graph.dag.DirectedGraph;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class ParsedLiebreSPE extends ParsedSPE {

    ParsedLiebreSPE(@NotNull String name, @NotNull List<ParsedOperator> operators, @NotNull List<String> baseImports, @NotNull List<String> baseDefinition, @NotNull Map<String, List<String>> operatorImportsMap) {
        super(name, operators, baseImports, baseDefinition, operatorImportsMap);
    }

    @Nonnull
    @Override
    public String generateCodeFrom(@NotNull DirectedGraph directedGraph) {

        return "todo";
    }

    // TODO the connection method etc is different for each SPE
}
