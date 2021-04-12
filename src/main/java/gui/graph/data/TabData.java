package gui.graph.data;

import com.brunomnsilva.smartgraph.graph.DigraphEdgeList;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import gui.graph.dag.Node;
import gui.graph.visualisation.VisInfo;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class TabData {
    private SmartGraphPanel<GraphOperator, GraphStream> graphView;
    private Graph<GraphOperator, GraphStream> graph;
    private List<Pair<Node<GraphOperator>, VisInfo>> visResult;
    private boolean isVisualisedQuery;

    public TabData() {
        graph = new DigraphEdgeList<>();
        graphView = new SmartGraphPanel<>(graph, new SmartCircularSortedPlacementStrategy());
        visResult = null;
        isVisualisedQuery = false;
    }

    public boolean isVisualisedQuery() {
        return isVisualisedQuery;
    }

    public void setVisResult(List<Pair<Node<GraphOperator>, VisInfo>> visResult) {
        this.visResult = visResult;
        this.isVisualisedQuery = true;
    }

    @Nullable
    public List<Pair<Node<GraphOperator>, VisInfo>> getVisResult() {
        return visResult;
    }

    @Nonnull
    public Graph<GraphOperator, GraphStream> getGraph() {
        return graph;
    }

    @Nonnull
    public SmartGraphPanel<GraphOperator, GraphStream> getGraphView() {
        return graphView;
    }

    @Override
    public String toString() {
        return "TabData{" +
                "graphView=" + graphView +
                ", graph=" + graph +
                '}';
    }
}
