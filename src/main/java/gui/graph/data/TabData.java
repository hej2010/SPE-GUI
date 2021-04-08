package gui.graph.data;

import com.brunomnsilva.smartgraph.graph.DigraphEdgeList;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;

import javax.annotation.Nonnull;

public class TabData {
    private SmartGraphPanel<GraphOperator, GraphStream> graphView;
    private Graph<GraphOperator, GraphStream> graph;

    public TabData() {
        graph = new DigraphEdgeList<>();
        graphView = new SmartGraphPanel<>(graph, new SmartCircularSortedPlacementStrategy());
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
