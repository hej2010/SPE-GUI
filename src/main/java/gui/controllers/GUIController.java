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
import gui.graph.dag.Node;
import gui.graph.data.*;
import gui.graph.export.ExportManager;
import gui.spe.ParsedLiebreSPE;
import gui.spe.ParsedOperator;
import gui.spe.ParsedSPE;
import gui.spe.SPEParser;
import gui.utils.Files;
import gui.views.AutoCompleteTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

public class GUIController {
    private GUI gui;
    private SmartGraphPanel<GraphOperator, GraphStream> graphView;
    private Graph<GraphOperator, GraphStream> graph;
    private final GraphOperator[] selectedOps = new GraphOperator[2];
    private Edge<GraphStream, GraphOperator> selectedEdge;
    private ParsedSPE parsedSPE;
    private GraphOperator singleClickedOperator;
    private File selectedDirectory;

    @FXML
    public AnchorPane aPMaster, aPGraph, aPDetails;
    @FXML
    public Button btnAddSource, btnAddOp, btnAddSink, btnConnect, btnDisconnect, btnModify, btnSelectFile, btnGenerate;
    @FXML
    public TextField tfIdentifier;
    @FXML
    public AutoCompleteTextField<String> tFInput1, tFInput2, tFOutput1, tFOutput2;
    @FXML
    public ChoiceBox<String> cBTypeSource, cBTypeRegular, cBTypeSink;
    @FXML
    public VBox vBDetails, vBInputs, vBOutputs;
    @FXML
    public MenuItem mIChangeSpe, mIExport, mIImport;
    @FXML
    public Label lblCurrentSPE, lblSelectedFile, lblSavedTo, lblSavedToTitle;
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
        new Thread(() -> {
            try {
                initAutoCompletion();
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void initAutoCompletion() throws URISyntaxException, IOException {
        String fileName = (parsedSPE instanceof ParsedLiebreSPE ? "liebre" : "flink") + "-classes.txt";
        String file = Files.readFile(Paths.get(Paths.get(SPEParser.class.getClassLoader().getResource("gui").toURI()).toString(), fileName).toString());
        String javaFiles = Files.readFile(Paths.get(Paths.get(SPEParser.class.getClassLoader().getResource("gui").toURI()).toString(), "java-classes.txt").toString());
        String both = file + "," + javaFiles;

        SortedSet<String> set = new TreeSet<>(Arrays.asList(both.split(",")));

        tFInput1.setEntries(set);
        tFInput2.setEntries(set);
        tFOutput1.setEntries(set);
        tFOutput2.setEntries(set);

        tFInput1.getEntryMenu().setOnAction(e -> {
            ((MenuItem) e.getTarget()).addEventHandler(Event.ANY, event ->
            {
                if (tFInput1.getLastSelectedObject() != null) {
                    tFInput1.setText(tFInput1.getLastSelectedObject());
                    System.out.println(tFInput1.getLastSelectedObject());
                }
            });
        });
    }

    private void setDetails(@Nullable GraphOperator selectedOperator) {
        this.singleClickedOperator = selectedOperator;
        if (selectedOperator == null) {
            vBDetails.setVisible(false);
            vBDetails.setDisable(true);
        } else {
            vBDetails.setVisible(true);
            vBDetails.setDisable(false);
            tfIdentifier.setText(selectedOperator.getIdentifier());
            ParsedOperator po = selectedOperator.getCurrentOperator();
            setSelectedType(po, singleClickedOperator);
            setCodeDetails(po, selectedOperator.getIdentifier());
        }
    }

    private void setCodeDetails(@Nullable ParsedOperator po, @Nonnull String operatorIdentifier) {
        if (po == null) {
            vBInputs.setVisible(false);
            vBOutputs.setVisible(false);
            btnModify.setDisable(true);
            tACode.setText("");
        } else {
            ParsedOperator.Definition def = po.getDefinition();
            btnModify.setDisable(!def.isModifiable());
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
            tACode.setText(def.getCode(operatorIdentifier));
        }
    }

    private void setSelectedType(ParsedOperator parsedOperator, GraphOperator singleClickedOperator) {
        ChoiceBox<String> cBType = selectChoiceBox(singleClickedOperator);
        if (parsedOperator == null) {
            cBType.getSelectionModel().select(-1);
        } else {
            cBType.getSelectionModel().select(parsedOperator.getOperatorName());
        }
    }

    private ChoiceBox<String> selectChoiceBox(GraphOperator operator) {
        if (operator instanceof SourceOperator) {
            cBTypeSource.setVisible(true);
            cBTypeRegular.setVisible(false);
            cBTypeSink.setVisible(false);
            return cBTypeSource;
        } else if (operator instanceof Operator) {
            cBTypeSource.setVisible(false);
            cBTypeRegular.setVisible(true);
            cBTypeSink.setVisible(false);
            return cBTypeRegular;
        } else {
            cBTypeSource.setVisible(false);
            cBTypeRegular.setVisible(false);
            cBTypeSink.setVisible(true);
            return cBTypeSink;
        }
    }

    private void setChoiceBoxItems() {
        prepare(parsedSPE.getOperatorNames(ParsedOperator.TYPE_SOURCE_OPERATOR), cBTypeSource);
        prepare(parsedSPE.getOperatorNames(ParsedOperator.TYPE_REGULAR_OPERATOR), cBTypeRegular);
        prepare(parsedSPE.getOperatorNames(ParsedOperator.TYPE_SINK_OPERATOR), cBTypeSink);
    }

    private void prepare(@Nonnull List<String> list, @Nonnull ChoiceBox<String> choiceBox) {
        list.add(0, "");
        choiceBox.setItems(FXCollections.observableArrayList(list));
        choiceBox.setOnAction(event -> {
            if (singleClickedOperator != null) {
                String selected = choiceBox.getSelectionModel().getSelectedItem();
                singleClickedOperator.selectOperator(selected, parsedSPE.getOperators());
                setDetails(singleClickedOperator);
                graphView.update();
            }
        });
    }

    private void initGraph() {
        //IMPORTANT - Called after scene is displayed so we can have width and height values
        graphView.init();
        graphView.setAutomaticLayout(true);
        graphView.setRepulsionForce(100);
        graphView.setEdgeDoubleClickAction(smartGraphEdge -> {
            //System.out.println("Double click edge: " + smartGraphEdge.getUnderlyingEdge().element());
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
            //System.out.println("Double click vertex: " + v.element());
            //
        });
        graphView.setVertexSingleClickAction(smartGraphVertex -> {
            //System.out.println("Single click vertex: " + smartGraphVertex.getUnderlyingVertex().element());
            setDetails(smartGraphVertex.getUnderlyingVertex().element());
            //
        });

        graphView.setEdgeSingleClickAction(smartGraphEdge -> {
            //System.out.println("Single click edge: " + smartGraphEdge.getUnderlyingEdge().element());
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

    private void setVertexSelectedStyle(boolean selected, GraphOperator vertex) {
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
                    setVertexSelectedStyle(false, selectedOps[0]);
                    selectedOps[0].setSelectedIndex(-1);
                    selectedOps[0] = selectedOps[1];
                    selectedOps[0].setSelectedIndex(0);
                    selectedOps[1] = op;
                    op.setSelectedIndex(1);
                }
            }
            setVertexSelectedStyle(!deselected, op);
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
                graph.insertVertex(new Operator());
                update();
            }
        });
        btnAddSource.setOnAction(event -> {
            if (graph != null) {
                graph.insertVertex(new SourceOperator());
                update();
            }
        });
        btnAddSink.setOnAction(event -> {
            if (graph != null) {
                graph.insertVertex(new SinkOperator());
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
                    setVertexSelectedStyle(false, from);
                    setVertexSelectedStyle(false, to);
                    selectedOps[0] = null;
                    selectedOps[1] = null;
                    graphView.update();
                    btnConnect.setDisable(true);
                    //DirectedGraph d = DirectedGraph.fromGraphView(graph);
                    //System.out.println(d.toString());
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
        mIExport.setOnAction(event -> {
            JSONObject o = ExportManager.projectToJson(DirectedGraph.fromGraphView(graph));
            DirectoryChooser directoryChooser = new DirectoryChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString() + "/src/main/java/gui";
            directoryChooser.setInitialDirectory(new File(path));

            File dir = directoryChooser.showDialog(gui.getPrimaryStage());
            if (dir != null) {
                File file = new File(dir, "export-" + System.currentTimeMillis() + ".json");
                Files.writeFile(file, o.toString());
            }
            System.out.println(o);
        });
        mIImport.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString() + "/src/main/java/gui";
            fileChooser.setInitialDirectory(new File(path));

            File file = fileChooser.showOpenDialog(gui.getPrimaryStage());
            if (file != null) {
                List<Node<GraphOperator>> opsList = ExportManager.projectFromFile(file, parsedSPE);
                Set<String> addedIdentifiers = new HashSet<>();
                List<GraphOperator> addedNodes = new LinkedList<>();
                graph.clearGraph();
                if (opsList != null) {
                    addToGraph(opsList, null, addedIdentifiers, addedNodes);
                }
                graphView.update();
            }
        });
        btnModify.setOnAction(event -> {
            assert singleClickedOperator.getCurrentOperator() != null;
            ParsedOperator.Definition def = singleClickedOperator.getCurrentOperator().getDefinition();
            if (!def.isModifiable()) {
                return;
            }
            String result = showModifyPopupWindow(def, singleClickedOperator.getIdentifier());
            if (result != null) { // null if Done was not pressed
                def.setCodeMiddle(result);
                tACode.setText(def.getCode(singleClickedOperator.getIdentifier()));
            }
        });
        TextField[] tfsIn = new TextField[]{tFInput1, tFInput2};
        TextField[] tfsOut = new TextField[]{tFOutput1, tFOutput2};
        for (int i = 0; i < tfsIn.length; i++) {
            TextField tf = tfsIn[i];
            int finalI = i;
            tf.textProperty().addListener((observable, oldValue, newValue) -> {
                ParsedOperator op = singleClickedOperator.getCurrentOperator();
                if (op != null) {
                    ParsedOperator.Definition def = op.getDefinition();
                    def.setInputPlaceholders(finalI, tf.getText());
                    tACode.setText(def.getCode(singleClickedOperator.getIdentifier()));
                }
            });
        }
        for (int i = 0; i < tfsOut.length; i++) {
            TextField tf = tfsOut[i];
            int finalI = i;
            tf.textProperty().addListener((observable, oldValue, newValue) -> {
                ParsedOperator op = singleClickedOperator.getCurrentOperator();
                if (op != null) {
                    ParsedOperator.Definition def = op.getDefinition();
                    def.setOutputPlaceholders(finalI, tf.getText());
                    tACode.setText(def.getCode(singleClickedOperator.getIdentifier()));
                }
            });
        }
        tfIdentifier.textProperty().addListener((observable, oldValue, newValue) -> {
            singleClickedOperator.setIdentifier(tfIdentifier.getText());
            ParsedOperator op = singleClickedOperator.getCurrentOperator();
            if (op != null) {
                ParsedOperator.Definition def = op.getDefinition();
                tACode.setText(def.getCode(singleClickedOperator.getIdentifier()));
            }
        });
        btnSelectFile.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString() + "/src/main/java/gui";
            directoryChooser.setInitialDirectory(new File(path));

            selectedDirectory = directoryChooser.showDialog(gui.getPrimaryStage());
            if (selectedDirectory != null) {
                lblSelectedFile.setText(selectedDirectory.getPath());
                btnGenerate.setDisable(false);
                System.out.println("Selected: " + selectedDirectory.getPath());
            }
        });
        btnGenerate.setOnAction(event -> {
            if (selectedDirectory != null) {
                DirectedGraph d = DirectedGraph.fromGraphView(graph);
                String fileName = parsedSPE.getName() + System.currentTimeMillis();
                String fileNameWithSuffix = fileName + ".java";
                File file = new File(selectedDirectory, fileNameWithSuffix);
                String code = parsedSPE.generateCodeFrom(d, parsedSPE, fileName);
                String errorMessage = Files.writeFile(file, code);
                lblSavedToTitle.setVisible(true);
                if (errorMessage == null) {
                    // success
                    lblSavedToTitle.setText("Saved to:");
                    lblSavedTo.setText(file.getPath());
                } else {
                    // failed
                    lblSavedToTitle.setText("Error:");
                    lblSavedTo.setText(errorMessage);
                }
            }
        });
    }

    private void addToGraph(@Nonnull List<Node<GraphOperator>> opsList, @Nullable GraphOperator parent, Set<String> addedIdentifiers, List<GraphOperator> addedNodes) {
        for (Node<GraphOperator> node : opsList) {
            GraphOperator op = node.getItem();
            if (!addedIdentifiers.contains(op.getIdentifier())) {
                graph.insertVertex(op);
                addedIdentifiers.add(op.getIdentifier());
                addedNodes.add(op);

                List<Node<GraphOperator>> successors = node.getSuccessors();
                if (!successors.isEmpty()) {
                    addToGraph(successors, op, addedIdentifiers, addedNodes);
                }
            } else {
                for (GraphOperator go : addedNodes) {
                    if (go.getIdentifier().equals(op.getIdentifier())) {
                        op = go;
                        break;
                    }
                }
            }

            if (parent != null) {
                graph.insertEdge(parent, op, new Stream());
            }
        }
    }

    @Nullable
    private String showModifyPopupWindow(ParsedOperator.Definition def, String identifier) { // From https://stackoverflow.com/a/37417736/7232269
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
            controller.init(def, identifier);
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
