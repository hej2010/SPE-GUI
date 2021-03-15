package gui.testing;

import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.GraphEdgeList;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SmartGraphTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception { // TODO https://github.com/brunomnsilva/JavaFXSmartGraph
        Graph<GraphOperator, GraphStream> g = new GraphEdgeList<>();

        SmartPlacementStrategy strategy = new SmartCircularSortedPlacementStrategy();
        SmartGraphPanel<GraphOperator, GraphStream> graphView = new SmartGraphPanel<>(g, strategy);
        Scene scene = new Scene(graphView, 1024, 768);

        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("JavaFXGraph Visualization");
        stage.setScene(scene);
        stage.show();

        //IMPORTANT - Called after scene is displayed so we can have width and height values
        graphView.init();
        graphView.setEdgeDoubleClickAction(smartGraphEdge -> {
            System.out.println("Double click edge: " + smartGraphEdge.getUnderlyingEdge().element());
            //
        });
        graphView.setVertexDoubleClickAction(smartGraphVertex -> {
            System.out.println("Double click vertex: " + smartGraphVertex.getUnderlyingVertex().element());
            //
        });

        /*SourceOperator s1 = new SourceOperator();
        Operator o1 = new Operator();
        Operator o2 = new Operator();
        g.insertVertex(s1);
        g.insertVertex(o1);
        g.insertVertex(o2);

        g.insertEdge(s1, o1, new Stream());
        g.insertEdge(o1, o2, new Stream());*/

        graphView.update();
    }
}
