package org.example;

import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.GraphEdgeList;
import com.brunomnsilva.smartgraph.graph.Vertex;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartStylableNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.example.graph.dag.DirectedGraph;
import org.example.graph.data.*;

public class GUIController {
    private VBox main;
    private Scene scene;
    private SmartGraphPanel<GraphOperator, GraphStream> graphView;
    private Graph<GraphOperator, GraphStream> graph;
    private final GraphOperator[] selectedOps = new GraphOperator[2];
    private Edge<GraphStream, GraphOperator> selectedEdge;

    @FXML
    public AnchorPane aPMaster, aPGraph, aPDetails;
    @FXML
    public Button btnAddSource, btnAddOp, btnAddSink, btnConnect, btnDisconnect;

    public void init(VBox main, Scene scene) {
        this.main = main;
        this.scene = scene;

        graph = new GraphEdgeList<>();
        selectedEdge = null;

        SmartPlacementStrategy strategy = new SmartCircularSortedPlacementStrategy();
        graphView = new SmartGraphPanel<>(graph, strategy);
        AnchorPane.setTopAnchor(graphView, 0.0);
        AnchorPane.setLeftAnchor(graphView, 0.0);
        AnchorPane.setRightAnchor(graphView, 0.0);
        AnchorPane.setBottomAnchor(graphView, 0.0);
        aPGraph.getChildren().add(graphView);

        initButtonListeners();
        Platform.runLater(this::initGraph);
    }

    private void initGraph() {
        //IMPORTANT - Called after scene is displayed so we can have width and height values
        graphView.init();
        graphView.setEdgeDoubleClickAction(smartGraphEdge -> {
            System.out.println("Double click edge: " + smartGraphEdge.getUnderlyingEdge().element());
            Edge<GraphStream, GraphOperator> edge = smartGraphEdge.getUnderlyingEdge();
            if (selectedEdge == edge) {
                selectedEdge = null;
                setEdgeSelected(false, edge);
            } else {
                setEdgeSelected(false, selectedEdge);
                selectedEdge = edge;
                setEdgeSelected(true, edge);
            }
            btnDisconnect.setDisable(selectedEdge == null);
            graphView.update();
            graph.vertices();
            graph.edges();
            //
        });
        graphView.setVertexDoubleClickAction(smartGraphVertex -> {
            Vertex<GraphOperator> v = smartGraphVertex.getUnderlyingVertex();
            doOnVertexClicked(v);
            System.out.println("Double click vertex: " + v.element());
            //
        });
        graphView.setVertexSingleClickAction(smartGraphVertex -> {
            System.out.println("Single click vertex: " + smartGraphVertex.getUnderlyingVertex().element());
            //
        });

        graphView.setEdgeSingleClickAction(smartGraphEdge -> {
            System.out.println("Single click edge: " + smartGraphEdge.getUnderlyingEdge().element());
            //
        });
    }

    private void setEdgeSelected(boolean selected, Edge<GraphStream, GraphOperator> edge) {
        if (edge != null) {
            SmartStylableNode node = graphView.getStylableEdge(edge);
            if (node != null) {
                node.setStyleClass(selected ? "edge-selected" : "edge");
            }
        }
    }

    private void setVertexSelected(boolean selected, GraphOperator vertex) {
        if (vertex != null) {
            SmartStylableNode node = graphView.getStylableVertex(vertex);
            if (node != null) {
                node.setStyleClass(selected ? "vertex-selected" : "vertex");
            }
        }
    }

    private void doOnVertexClicked(Vertex<GraphOperator> vertex) {
        GraphOperator op = vertex.element();
        synchronized (selectedOps) {
            boolean deselected = false;
            for (int i = 0; i < 2; i++) {
                if (op.equals(selectedOps[i])) {
                    if (i == 0) {
                        if (selectedOps[1] != null) {
                            selectedOps[0] = selectedOps[1];
                            selectedOps[1] = null;
                            selectedOps[0].setSelectedIndex(0);
                        } else {
                            selectedOps[0] = null;
                        }
                    } else {
                        selectedOps[1] = null;
                    }
                    System.out.println("deselected " + op.toString());
                    op.setSelectedIndex(-1);
                    deselected = true;
                }
            }
            if (!deselected) {
                if (selectedOps[0] == null) {
                    if (op instanceof SinkOperator) {
                        return;
                    }
                    selectedOps[0] = op;
                    op.setSelectedIndex(0);
                } else if (selectedOps[1] == null) {
                    if (op instanceof SourceOperator) {
                        return;
                    }
                    selectedOps[1] = op;
                    op.setSelectedIndex(1);
                } else {
                    setVertexSelected(false, selectedOps[0]);
                    selectedOps[0].setSelectedIndex(-1);
                    selectedOps[0] = selectedOps[1];
                    selectedOps[0].setSelectedIndex(0);
                    selectedOps[1] = op;
                    op.setSelectedIndex(1);
                }
                System.out.println("selected " + op.toString());
            }
            setVertexSelected(!deselected, op);
            updateButtons();
            graphView.update();
        }
    }

    private void updateButtons() {
        btnConnect.setDisable(selectedOps[0] == null || selectedOps[1] == null);
    }

    private void initButtonListeners() {
        btnAddOp.setOnAction(event -> {
            if (graph != null) {
                graph.insertVertex(new Operator("OP"));
                update();
            }
        });
        btnAddSource.setOnAction(event -> {
            if (graph != null) {
                graph.insertVertex(new SourceOperator("Source"));
                update();
            }
        });
        btnAddSink.setOnAction(event -> {
            if (graph != null) {
                graph.insertVertex(new SinkOperator("Sink"));
                update();
            }
        });
        btnConnect.setOnAction(event -> {
            if (graph != null) {
                synchronized (selectedOps) {
                    GraphOperator from = selectedOps[0];
                    GraphOperator to = selectedOps[1];
                    assert from != null && to != null;
                    if (from instanceof SinkOperator || to instanceof SourceOperator) {
                        return;
                    }
                    graph.insertEdge(from, to, new Stream());
                    from.setSelectedIndex(-1);
                    to.setSelectedIndex(-1);
                    setVertexSelected(false, from);
                    setVertexSelected(false, to);
                    selectedOps[0] = null;
                    selectedOps[1] = null;
                    graphView.update();
                    btnConnect.setDisable(true);
                    DirectedGraph d = DirectedGraph.fromGraphView(graph);
                    System.out.println(d.toString());
                }
            }
        });
        btnDisconnect.setOnAction(event -> {
            if (graph != null) {
                synchronized (this) {
                    assert selectedEdge != null;
                    graph.removeEdge(selectedEdge);
                    selectedEdge = null;
                    graphView.update();
                    btnDisconnect.setDisable(true);
                }
            }
        });
    }

    private void update() {
        if (graphView != null) {
            graphView.update();
        }
    }

}
