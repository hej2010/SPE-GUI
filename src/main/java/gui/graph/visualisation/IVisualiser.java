package gui.graph.visualisation;

import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public interface IVisualiser {
    @Nonnull
    List<Pair<Node<GraphOperator>, VisualisationManager.VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisualisationManager.VisInfo>> list);
}
