package gui.controllers;

import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.GraphEdgeList;
import com.brunomnsilva.smartgraph.graph.Vertex;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartStylableNode;
import gui.GUI;
import gui.graph.dag.DirectedGraph;
import gui.graph.data.*;
import gui.spe.ParsedOperator;
import gui.spe.ParsedSPE;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class GUIController {
    private GUI gui;
    private SmartGraphPanel<GraphOperator, GraphStream> graphView;
    private Graph<GraphOperator, GraphStream> graph;
    private final GraphOperator[] selectedOps = new GraphOperator[2];
    private Edge<GraphStream, GraphOperator> selectedEdge;
    private ParsedSPE parsedSPE;
    private GraphOperator singleClickedOperator;

    @FXML
    public AnchorPane aPMaster, aPGraph, aPDetails;
    @FXML
    public Button btnAddSource, btnAddOp, btnAddSink, btnConnect, btnDisconnect, btnModify;
    @FXML
    public TextField tFName, tfIdentifier, tFInput1, tFInput2, tFOutput1, tFOutput2;
    @FXML
    public ChoiceBox<String> cBType;
    @FXML
    public VBox vBDetails, vBInputs, vBOutputs;
    @FXML
    public HBox hBIdentifier;
    @FXML
    public MenuItem mIChangeSpe;
    @FXML
    public Label lblCurrentSPE;
    @FXML
    public TextArea tACode;

    public void init(GUI gui, ParsedSPE parsedSPE) {
        this.gui = gui;
        graph = new GraphEdgeList<>();
        selectedEdge = null;
        selectedOps[0] = null;
        selectedOps[1] = null;
        singleClickedOperator = null;
        this.parsedSPE = parsedSPE;
        lblCurrentSPE.setText("Current SPE: " + parsedSPE.getName());

        SmartPlacementStrategy strategy = new SmartCircularSortedPlacementStrategy();
        graphView = new SmartGraphPanel<>(graph, strategy);
        AnchorPane.setTopAnchor(graphView, 0.0);
        AnchorPane.setLeftAnchor(graphView, 0.0);
        AnchorPane.setRightAnchor(graphView, 0.0);
        AnchorPane.setBottomAnchor(graphView, 0.0);
        aPGraph.getChildren().add(graphView);

        setChoiceBoxItems();
        setDetails(null);
        initButtonListeners(gui);
        Platform.runLater(this::initGraph);
    }

    private void setDetails(@Nullable GraphOperator selectedOperator) {
        this.singleClickedOperator = selectedOperator;
        if (selectedOperator == null) {
            vBDetails.setVisible(false);
            vBDetails.setDisable(true);
        } else {
            vBDetails.setVisible(true);
            vBDetails.setDisable(false);
            tFName.setText(selectedOperator.getName());
            ParsedOperator po = selectedOperator.getOperatorType();
            setSelectedType(po);
            setCodeDetails(po);
        }
    }

    private void setCodeDetails(@Nullable ParsedOperator po) {
        if (po == null) {
            vBInputs.setVisible(false);
            vBOutputs.setVisible(false);
            hBIdentifier.setVisible(false);
        } else {
            hBIdentifier.setVisible(true);
            ParsedOperator.Definition def = po.getDefinition();
            btnModify.setDisable(!def.isModifiable());
            tfIdentifier.setText(def.getIdentifier());
            final int inputs = def.getInputCount();
            final int outputs = def.getOutputCount();
            if (inputs > 0) {
                vBInputs.setVisible(true);
                List<String> in = def.getInputPlaceholders();
                tFInput1.setText(in.get(0));
                if (inputs > 1) {
                    tFInput2.setDisable(false);
                    tFInput2.setText(in.get(1));
                } else {
                    tFInput2.setText("");
                    tFInput2.setDisable(true);
                }
            } else {
                vBInputs.setVisible(false);
            }
            if (outputs > 0) {
                vBOutputs.setVisible(true);
                List<String> in = def.getOutputPlaceholders();
                tFOutput1.setText(in.get(0));
                if (outputs > 1) {
                    tFOutput2.setDisable(false);
                    tFOutput2.setText(in.get(1));
                } else {
                    tFOutput2.setText("");
                    tFOutput2.setDisable(true);
                }
            } else {
                vBOutputs.setVisible(false);
            }
            tACode.setText(def.getCode());
        }
    }

    private void setSelectedType(ParsedOperator operatorType) {
        if (operatorType == null) {
            cBType.getSelectionModel().select(-1);
        } else {
            cBType.getSelectionModel().select(operatorType.getName());
        }
    }

    private void setChoiceBoxItems() {
        List<String> list = parsedSPE.getOperatorNames();
        list.add(0, "");
        cBType.setItems(FXCollections.observableArrayList(list));
        cBType.setOnAction(event -> {
            if (singleClickedOperator != null) {
                String selected = cBType.getSelectionModel().getSelectedItem();
                ParsedOperator po = findOperatorTypeFrom(selected);
                System.out.println("selected " + selected + ", " + po);
                singleClickedOperator.setOperatorType(po);
                setDetails(singleClickedOperator);
                graphView.update();
            }
        });
    }

    private ParsedOperator findOperatorTypeFrom(String name) {
        for (ParsedOperator pOp : parsedSPE.getOperators()) {
            if (pOp.getName().equals(name)) {
                return pOp.clone();
            }
        }
        return null;
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
            setDetails(smartGraphVertex.getUnderlyingVertex().element());
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

    private void doOnVertexClicked(@Nonnull Vertex<GraphOperator> vertex) {
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
            }
            setVertexSelected(!deselected, op);
            updateButtons();
            graphView.update();
        }
    }

    private void updateButtons() {
        btnConnect.setDisable(selectedOps[0] == null || selectedOps[1] == null);
    }

    private void initButtonListeners(GUI gui) {
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
                    // TODO cancel if creates a cycle
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
        mIChangeSpe.setOnAction(event -> {
            try {
                gui.changeScene(GUI.FXML_MAIN, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        btnModify.setOnAction(event -> {
            assert singleClickedOperator.getOperatorType() != null;
            ParsedOperator.Definition def = singleClickedOperator.getOperatorType().getDefinition();
            if (!def.isModifiable()) {
                return;
            }
            String result = showModifyPopupWindow(def);
            if (result != null) { // null if Done was not pressed
                def.setCodeMiddle(result);
                tACode.setText(def.getCode());
            }
        });
        TextField[] tfsIn = new TextField[]{tFInput1, tFInput2};
        TextField[] tfsOut = new TextField[]{tFOutput1, tFOutput2};
        for (int i = 0; i < tfsIn.length; i++) {
            TextField tf = tfsIn[i];
            int finalI = i;
            tf.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) { // out of focus
                    assert singleClickedOperator.getOperatorType() != null;
                    ParsedOperator.Definition def = singleClickedOperator.getOperatorType().getDefinition();
                    def.setInputPlaceholders(finalI, tf.getText());
                    tACode.setText(def.getCode());
                }
            });
        }
        for (int i = 0; i < tfsOut.length; i++) {
            TextField tf = tfsOut[i];
            int finalI = i;
            tf.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) { // out of focus
                    assert singleClickedOperator.getOperatorType() != null;
                    ParsedOperator.Definition def = singleClickedOperator.getOperatorType().getDefinition();
                    def.setOutputPlaceholders(finalI, tf.getText());
                    tACode.setText(def.getCode());
                }
            });
        }
        tfIdentifier.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // out of focus
                assert singleClickedOperator.getOperatorType() != null;
                ParsedOperator.Definition def = singleClickedOperator.getOperatorType().getDefinition();
                def.setIdentifier(tfIdentifier.getText());
                tACode.setText(def.getCode());
            }
        });
    }

    @Nullable
    private String showModifyPopupWindow(ParsedOperator.Definition def) { // From https://stackoverflow.com/a/37417736/7232269
        FXMLLoader loader = new FXMLLoader(GUI.class.getResource("popup.fxml"));

        // initializing the controller
        Parent layout;
        try {
            layout = loader.load();
            CodePopupController controller = loader.getController();
            Scene scene = new Scene(layout);
            // this is the popup stage
            Stage popupStage = new Stage();
            // Giving the popup controller access to the popup stage (to allow the controller to close the stage)
            controller.setStage(popupStage);
            if (this.gui != null) {
                popupStage.initOwner(gui.getPrimaryStage());
            }
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.setScene(scene);
            controller.init(def);
            popupStage.showAndWait();
            return controller.getResult();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void update() {
        if (graphView != null) {
            graphView.update();
        }
    }

}
