package gui.graph.visualisation;

import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LiebreVisualiser implements IVisualiser{

    @NotNull
    @Override
    public List<Pair<Node<GraphOperator>, VisualisationManager.VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisualisationManager.VisInfo>> list) {
        return null;
    }
}
