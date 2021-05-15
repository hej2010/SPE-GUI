package gui.controllers;

import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Vertex;
import com.brunomnsilva.smartgraph.graphview.SmartStylableNode;
import com.sun.management.OperatingSystemMXBean;
import gui.GUI;
import gui.controllers.spe.FlinkController;
import gui.graph.dag.DirectedGraph;
import gui.graph.dag.Node;
import gui.graph.data.*;
import gui.graph.export.ExportManager;
import gui.graph.visualisation.VisInfo;
import gui.graph.visualisation.VisualisationManager;
import gui.spe.ParsedFlinkSPE;
import gui.spe.ParsedLiebreSPE;
import gui.spe.ParsedOperator;
import gui.spe.ParsedSPE;
import gui.utils.Files;
import gui.utils.IOnDone;
import gui.views.AutoCompleteTextField;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

public class GUIController {
    private final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    private final MemoryMXBean memoryBean = ManagementFactory.getPlatformMXBean(MemoryMXBean.class);
    private GUI gui;
    private final GraphOperator[] selectedOps = new GraphOperator[2];
    private Edge<GraphStream, GraphOperator> selectedEdge;
    private ParsedSPE parsedSPE;
    private GraphOperator singleClickedOperator;
    private File selectedDirectory;
    private final List<TabData> tabs = new LinkedList<>();
    private TabData selectedTab;
    private File lastSelectedDirectory, lastSelectedMetricsDirectory;

    @FXML
    public AnchorPane aPMaster/*, aPGraph*/, aPDetails;
    @FXML
    public Button btnAddSource, btnAddOp, btnAddSink, btnConnect, btnDisconnect, btnModify, btnSelectFile, btnGenerate, btnAddTab, btnModifyVis, btnCheck, btnMetricsLiebre, btnMetricsGraphite;
    @FXML
    public TextField tfIdentifier;
    @FXML
    public AutoCompleteTextField<String> tFInput1, tFInput2, tFOutput1, tFOutput2;
    @FXML
    public ChoiceBox<String> cBTypeSource, cBTypeRegular, cBTypeSink;
    @FXML
    public VBox vBDetails, vBInputs, vBOutputs, vBDetailsVis;
    @FXML
    public MenuItem mIChangeSpe, mIExport, mIImport, mIVisFromFile;
    @FXML
    public Label lblCurrentSPE, lblSelectedFile, lblSavedTo, lblSavedToTitle, lblLeftStatus, lblRightStatus, lblVisInfo, lblVisType, lblVisOperator;
    @FXML
    public TextArea tACode, tACodeVis;
    @FXML
    public TabPane tabPane;

    public void init(GUI gui, ParsedSPE parsedSPE) {
        this.gui = gui;
        selectedEdge = null;
        selectedOps[0] = null;
        selectedOps[1] = null;
        singleClickedOperator = null;
        this.parsedSPE = parsedSPE;
        selectedTab = new TabData();
        tabs.add(selectedTab);
        lblCurrentSPE.setText("Current SPE: " + parsedSPE.getName());
        AnchorPane.setTopAnchor(tabPane, 0.0);
        AnchorPane.setLeftAnchor(tabPane, 0.0);
        AnchorPane.setRightAnchor(tabPane, 0.0);
        AnchorPane.setBottomAnchor(tabPane, 0.0);
        Tab tab = new Tab("Tab 1");
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Tab tab1 = tabPane.getTabs().get(newValue.intValue());
            TabData data = tabs.get(newValue.intValue());

            // TODO
            selectedTab = data;
            updateDetailsView(selectedTab.isVisualisedQuery());
        });
        //aPGraph.getChildren().add(graphView);
        tab.setContent(selectedTab.getGraphView());

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
        startMetricsTimer();
    }

    private void startMetricsTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            double usage = osBean.getProcessCpuLoad() * 100;
            MemoryUsage memUsage = memoryBean.getHeapMemoryUsage();
            DecimalFormat df = new DecimalFormat("#.00");
            long memUsed = memUsage.getUsed() / 1048576;
            lblLeftStatus.setText("CPU: " + df.format(usage) + "%");
            lblRightStatus.setText("RAM: " + memUsed + " MB");
            System.out.println(usage + ", " + memUsed);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void initAutoCompletion() throws URISyntaxException, IOException {
        String fileName = (parsedSPE instanceof ParsedLiebreSPE ? "liebre" : "flink") + "-classes.txt";
        String file = Files.readResource(fileName);
        String javaFiles = Files.readResource("java-classes.txt");
        String both = file + "," + javaFiles;

        SortedSet<String> set = new TreeSet<>(Arrays.asList(both.split(",")));

        tFInput1.setEntries(set);
        tFInput2.setEntries(set);
        tFOutput1.setEntries(set);
        tFOutput2.setEntries(set);

        setOnAction(tFInput1);
        setOnAction(tFInput2);
        setOnAction(tFOutput1);
        setOnAction(tFOutput2);
    }

    public static void setOnAction(AutoCompleteTextField<String> textField) {
        textField.getEntryMenu().setOnAction(e -> ((MenuItem) e.getTarget()).addEventHandler(Event.ANY, event -> {
            if (textField.getLastSelectedObject() != null) {
                textField.setText(textField.getLastSelectedObject());
            }
        }));
    }

    private void updateDetailsView(boolean isVisualisedQuery) {
        if (isVisualisedQuery) {
            btnMetricsLiebre.setDisable(false);
            vBDetails.setVisible(false);
            vBDetailsVis.setVisible(true);
        } else {
            btnMetricsLiebre.setDisable(parsedSPE instanceof ParsedLiebreSPE);
            vBDetails.setVisible(true);
            vBDetailsVis.setVisible(false);
        }
    }

    private void setDetails(@Nullable GraphOperator selectedOperator) {
        this.singleClickedOperator = selectedOperator;
        if (selectedTab.isVisualisedQuery()) {
            updateDetailsView(true);
            if (selectedOperator == null) {
                String fileName = "-";
                List<Pair<Node<GraphOperator>, VisInfo>> res = selectedTab.getVisResult();
                if (res != null && !res.isEmpty()) {
                    fileName = res.get(0).getValue().getFileName();
                }
                lblVisInfo.setText("File: " + fileName);
                lblVisType.setText("Type: -");
                lblVisOperator.setText("Operator: -");
                tACodeVis.setText("");
                btnModifyVis.setDisable(true);
            } else {
                VisInfo visInfo = selectedOperator.getVisInfo();
                if (visInfo != null) {
                    lblVisInfo.setText("File: " + visInfo.getFileName() + "\n"
                            + "Class: " + visInfo.getClassName() + "\n"
                            + "Method: " + visInfo.getMethodName());
                    lblVisType.setText("Type: " + visInfo.variableInfo.getOperatorType().getSimpleName());
                    lblVisOperator.setText("Operator: " + visInfo.variableInfo.getOperatorName());
                    tACodeVis.setText(visInfo.variableInfo.getVariableData());
                } else {
                    lblVisInfo.setText("File: -");
                    lblVisType.setText("Type: -");
                    lblVisOperator.setText("Operator: -");
                    tACodeVis.setText("");
                    btnModifyVis.setDisable(true);
                }
                btnModifyVis.setDisable(false);
            }
        } else {
            updateDetailsView(false);
            if (selectedOperator == null) {
                vBDetails.setVisible(false);
                vBDetails.setDisable(true);
            } else {
                vBDetails.setVisible(true);
                vBDetails.setDisable(false);
                tfIdentifier.setText(selectedOperator.getIdentifier().get());
                ParsedOperator po = selectedOperator.getCurrentOperator();
                setSelectedType(po, singleClickedOperator);
                setCodeDetails(selectedOperator);
            }
        }
    }

    private void setCodeDetails(@Nonnull GraphOperator op) {
        final ParsedOperator po = op.getCurrentOperator();
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
            tACode.setText(def.getCode(op));
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
                selectedTab.getGraphView().update();
            }
        });
    }

    private void initGraph() {
        //IMPORTANT - Called after scene is displayed so we can have width and height values
        selectedTab.getGraphView().init();
        selectedTab.getGraphView().setAutomaticLayout(true);
        selectedTab.getGraphView().setRepulsionForce(100);
        selectedTab.getGraphView().setEdgeDoubleClickAction(smartGraphEdge -> {
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
            selectedTab.getGraphView().update();
            //graph.vertices();
            //graph.edges();
            //
        });
        selectedTab.getGraphView().setVertexDoubleClickAction(smartGraphVertex -> {
            Vertex<GraphOperator> v = smartGraphVertex.getUnderlyingVertex();
            doOnVertexClicked(v);
            //System.out.println("Double click vertex: " + v.element());
            //
        });
        selectedTab.getGraphView().setVertexSingleClickAction(smartGraphVertex -> {
            //System.out.println("Single click vertex: " + smartGraphVertex.getUnderlyingVertex().element());
            setDetails(smartGraphVertex.getUnderlyingVertex().element());
            //
        });

        selectedTab.getGraphView().setEdgeSingleClickAction(smartGraphEdge -> {
            //System.out.println("Single click edge: " + smartGraphEdge.getUnderlyingEdge().element());
            //
        });
    }

    private void setEdgeSelected(boolean selected, Edge<GraphStream, GraphOperator> edge) {
        if (edge != null) {
            SmartStylableNode node = selectedTab.getGraphView().getStylableEdge(edge);
            if (node != null) {
                node.setStyleClass(selected ? "edge-selected" : "edge");
            }
        }
    }

    private void setVertexSelectedStyle(boolean selected, GraphOperator vertex) {
        if (vertex != null) {
            SmartStylableNode node = selectedTab.getGraphView().getStylableVertex(vertex);
            if (node != null) {
                node.setStyleClass(selected ?
                        "vertex-selected" : (vertex instanceof SourceOperator ?
                        "vertex-source" : (vertex instanceof SinkOperator ?
                        "vertex-sink" : "vertex")));
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
            selectedTab.getGraphView().update();
        }
    }

    private void updateButtons() {
        btnConnect.setDisable(selectedOps[0] == null || selectedOps[1] == null);
    }

    private void initButtonListeners(GUI gui) {
        btnAddOp.setOnAction(event -> insertVertexAndSetStyle(new Operator()));
        btnAddSource.setOnAction(event -> insertVertexAndSetStyle(new SourceOperator()));
        btnAddSink.setOnAction(event -> insertVertexAndSetStyle(new SinkOperator()));
        btnConnect.setOnAction(event -> {
            synchronized (selectedOps) {
                GraphOperator from = selectedOps[0];
                GraphOperator to = selectedOps[1];
                assert from != null && to != null;
                if (from instanceof SinkOperator || to instanceof SourceOperator) { // wrong stream direction
                    return;
                }
                // TODO cancel if creates a cycle
                selectedTab.getGraph().insertEdge(from, to, new Stream(from, to)); // add an edge between them
                from.setSelectedIndex(-1);
                to.setSelectedIndex(-1);
                setVertexSelectedStyle(false, from);
                setVertexSelectedStyle(false, to);
                selectedOps[0] = null;
                selectedOps[1] = null;
                selectedTab.getGraphView().update();
                btnConnect.setDisable(true);
                if (parsedSPE instanceof ParsedFlinkSPE) { // update identifiers
                    FlinkController.updateGraphOnConnect(from, to, selectedTab.getGraph());
                    if (singleClickedOperator != null) {
                        setCodeDetails(singleClickedOperator);
                    }
                }
                //DirectedGraph d = DirectedGraph.fromGraphView(graph);
                //System.out.println(d.toString());
            }
        });
        btnDisconnect.setOnAction(event -> {
            synchronized (this) {
                assert selectedEdge != null;
                if (parsedSPE instanceof ParsedFlinkSPE) { // update identifiers
                    selectedEdge.vertices()[1].element().setPrevIdentifier(null);
                }
                selectedTab.getGraph().removeEdge(selectedEdge);
                selectedEdge = null;
                selectedTab.getGraphView().update();
                btnDisconnect.setDisable(true);
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
            JSONObject o = ExportManager.projectToJson(DirectedGraph.fromGraphView(selectedTab.getGraph()), parsedSPE);
            DirectoryChooser directoryChooser = new DirectoryChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString();// + "/src/main/java/gui";
            if (lastSelectedDirectory != null) {
                path = lastSelectedDirectory.toString();
            }
            directoryChooser.setInitialDirectory(new File(path));

            File dir = directoryChooser.showDialog(gui.getPrimaryStage());
            if (dir != null) {
                lastSelectedDirectory = dir;
                File file = new File(dir, "export-" + parsedSPE.getName() + "-" + System.currentTimeMillis() + ".json");
                Files.writeFile(file, o.toString());
            }
        });
        mIImport.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString();// + "/src/main/java/gui";
            if (lastSelectedDirectory != null) {
                path = lastSelectedDirectory.toString();
            }
            fileChooser.setInitialDirectory(new File(path));
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("JSON", "*.json");
            fileChooser.getExtensionFilters().add(extensionFilter);

            File file = fileChooser.showOpenDialog(gui.getPrimaryStage());
            if (file != null) {
                lastSelectedDirectory = file.getParentFile();
                List<Node<GraphOperator>> opsList = ExportManager.projectFromFile(file, parsedSPE);
                Set<String> addedIdentifiers = new HashSet<>();
                List<GraphOperator> addedNodes = new LinkedList<>();
                addNewTab(file.getName(), () -> {
                    if (opsList != null) {
                        addToGraph(opsList, null, addedIdentifiers, addedNodes);
                    }
                    updateDetailsView(false);
                    selectedTab.getGraphView().update();
                });
            }
        });
        mIVisFromFile.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString();//.toAbsolutePath().normalize().toString() + "/src/main/java/gui";
            if (lastSelectedDirectory != null) {
                path = lastSelectedDirectory.toString();
            }
            fileChooser.setInitialDirectory(new File(path));
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Java", "*.java");
            fileChooser.getExtensionFilters().add(extensionFilter);

            File file = fileChooser.showOpenDialog(gui.getPrimaryStage());
            if (file != null) {
                lastSelectedDirectory = file.getParentFile();
                List<Pair<Node<GraphOperator>, VisInfo>> visResult = VisualisationManager.visualiseFromFile(file, parsedSPE);
                Set<String> addedIdentifiers = new HashSet<>();
                List<GraphOperator> addedNodes = new LinkedList<>();
                addNewTab(file.getName(), () -> {
                    if (visResult != null) {
                        addToGraph2(visResult, addedIdentifiers, addedNodes);
                    }
                    selectedTab.setVisResult(visResult);
                    updateDetailsView(true);
                    selectedTab.getGraphView().update();
                });
            }
        });
        btnModify.setOnAction(event -> {
            assert singleClickedOperator.getCurrentOperator() != null;
            ParsedOperator.Definition def = singleClickedOperator.getCurrentOperator().getDefinition();
            if (!def.isModifiable()) {
                return;
            }
            String result = showModifyPopupWindow(def, singleClickedOperator);
            if (result != null) { // null if Done was not pressed
                def.setCodeMiddle(result);
                tACode.setText(def.getCode(singleClickedOperator));
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
                    tACode.setText(def.getCode(singleClickedOperator));
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
                    tACode.setText(def.getCode(singleClickedOperator));
                }
            });
        }
        tfIdentifier.textProperty().addListener((observable, oldValue, newValue) -> {
            singleClickedOperator.setIdentifier(tfIdentifier.getText());
            ParsedOperator op = singleClickedOperator.getCurrentOperator();
            if (op != null) {
                ParsedOperator.Definition def = op.getDefinition();
                tACode.setText(def.getCode(singleClickedOperator));
            }
            selectedTab.getGraphView().update();
        });
        btnSelectFile.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString() + "/src/main/java/gui";
            directoryChooser.setInitialDirectory(new File(path));

            selectedDirectory = directoryChooser.showDialog(gui.getPrimaryStage());
            if (selectedDirectory != null) {
                lblSelectedFile.setText(selectedDirectory.getPath());
                btnGenerate.setDisable(false);
            }
        });
        btnGenerate.setOnAction(event -> {
            if (selectedDirectory != null) {
                DirectedGraph directedGraph = DirectedGraph.fromGraphView(selectedTab.getGraph());
                String fileName = parsedSPE.getFileName() + System.currentTimeMillis();
                String fileNameWithSuffix = fileName + ".java";
                File file = new File(selectedDirectory, fileNameWithSuffix);
                String code = parsedSPE.generateCodeFrom(directedGraph, parsedSPE, fileName);
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
        btnAddTab.setOnAction(event -> addNewTab("Tab " + (tabs.size() + 1), null));
        btnCheck.setOnAction(event -> {
            DirectedGraph directedGraph = DirectedGraph.fromGraphView(selectedTab.getGraph());
            StringBuilder sb = new StringBuilder();
            Set<GraphOperator> nonUniqueNames = new HashSet<>(), nullOperators = new HashSet<>();
            Map<String, List<Integer>> names = new HashMap<>();
            List<Pair<GraphOperator, InvalidInputStream>> invalidInputStreams = new LinkedList<>();
            for (Node<GraphOperator> n : directedGraph.getGraph()) {
                checkNode(null, n, names, nonUniqueNames, nullOperators, invalidInputStreams);
            }
            for (GraphOperator op : nonUniqueNames) {
                sb.append("Warn: Found non-unique identifier: ").append(op.getIdentifier().get()).append("\n");
            }
            for (GraphOperator op : nullOperators) {
                sb.append("Warn: Found operator with no type: ").append(op.getIdentifier().get()).append("\n");
            }
            for (Pair<GraphOperator, InvalidInputStream> p : invalidInputStreams) {
                sb.append("Warn: Operator ").append(p.getKey().getIdentifier().get()).append(" expects ").append(p.getValue().expectedIn)
                        .append(" but receives ").append(p.getValue().acutalIn).append(" from ").append(p.getValue().parentName).append("\n");
            }
            System.out.println(sb);
            if (sb.toString().isEmpty()) {
                showDialog(Alert.AlertType.INFORMATION, "No warnings found", "No warnings", "The graph looks OK!");
            } else {
                showDialog(Alert.AlertType.WARNING, "Warnings found", "Warning", sb.toString());
            }
        });
        btnMetricsLiebre.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            String path = Paths.get(".").toAbsolutePath().normalize().toString();// + "/src/main/java/gui";
            if (lastSelectedMetricsDirectory != null) {
                path = lastSelectedMetricsDirectory.toString();
            }
            directoryChooser.setInitialDirectory(new File(path));
            directoryChooser.setTitle("Select the csv output directory");
            File file = directoryChooser.showDialog(gui.getPrimaryStage());
            if (file == null) {
                return;
            }
            lastSelectedMetricsDirectory = file;
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_LIEBRE));
                Pane main = fxmlLoader.load();
                Scene scene = new Scene(main, 900, 600);

                LiebreMetricsController controller = fxmlLoader.getController();

                Stage stage = new Stage();
                stage.setTitle("Metrics");
                stage.setScene(scene);
                stage.show();
                stage.setOnCloseRequest(event1 -> controller.closeStage());
                controller.setStage(stage);

                assert selectedTab.getVisResult() != null;
                controller.init(parsedSPE, selectedTab.getVisResult(), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        btnMetricsGraphite.setOnAction(event -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_GRAPHITE));
                Pane main = fxmlLoader.load();
                Scene scene = new Scene(main, 900, 600);

                GraphiteMetricsController controller = fxmlLoader.getController();

                Stage stage = new Stage();
                stage.setTitle("Metrics");
                stage.setScene(scene);
                stage.show();
                stage.setOnCloseRequest(event1 -> controller.closeStage());
                controller.setStage(stage);

                //assert selectedTab.getVisResult() != null;
                //controller(parsedSPE, selectedTab.getVisResult());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void insertVertexAndSetStyle(GraphOperator operator) {
        selectedTab.getGraph().insertVertex(operator);
        update();
        Platform.runLater(() -> setVertexSelectedStyle(false, operator));
    }

    private void showDialog(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.setResizable(true);
        alert.initModality(Modality.NONE);

        alert.show();
    }

    private void checkNode(GraphOperator parent, Node<GraphOperator> n, Map<String, List<Integer>> names, Set<GraphOperator> nonUniqueNames, Set<GraphOperator> nullOperators, List<Pair<GraphOperator, InvalidInputStream>> invalidInputStreams) {
        final GraphOperator op = n.getItem();
        List<Integer> idList;
        String identifier = op.getIdentifier().get();
        if (names.containsKey(identifier)) {
            idList = names.get(identifier);
        } else {
            idList = new LinkedList<>();
        }
        for (Integer i : idList) {
            if (i != op.getId()) {
                nonUniqueNames.add(op);
            }
        }
        idList.add(op.getId());
        names.put(identifier, idList);
        final ParsedOperator parsedOp = op.getCurrentOperator();
        if (parsedOp == null) {
            nullOperators.add(op);
        } else {
            final ParsedOperator.Definition def = parsedOp.getDefinition();
            if (parent != null) {
                final ParsedOperator parsedOpParent = parent.getCurrentOperator();
                if (parsedOpParent != null) {
                    ParsedOperator.Definition defParent = parsedOpParent.getDefinition();
                    List<String> parentOutputs = defParent.getOutputPlaceholders();
                    String parentOutput = parentOutputs.isEmpty() ? null : parentOutputs.get(0);
                    List<String> inputs = def.getInputPlaceholders();
                    if (inputs.isEmpty()) {
                        inputs = def.getOutputPlaceholders();
                    }
                    String input = inputs.isEmpty() ? null : inputs.get(0);
                    if (parentOutput != null && !parentOutput.equals(input)) {
                        invalidInputStreams.add(new Pair<>(op, new InvalidInputStream(input, parentOutput, parent.getIdentifier().get())));
                    }
                }
            }
        }
        for (Node<GraphOperator> child : n.getSuccessors()) {
            checkNode(op, child, names, nonUniqueNames, nullOperators, invalidInputStreams);
        }
    }

    private static class InvalidInputStream {
        private final String expectedIn, acutalIn, parentName;

        private InvalidInputStream(String expectedIn, String acutalIn, String parentName) {
            this.expectedIn = expectedIn;
            this.acutalIn = acutalIn;
            this.parentName = parentName;
        }
    }

    private void addNewTab(String name, @Nullable IOnDone onDone) {
        Tab tab = new Tab(name);
        TabData data = new TabData();
        tabs.add(data);
        tab.setContent(data.getGraphView());
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().selectLast();
        this.selectedTab = data;
        setDetails(null);
        if (selectedTab.isVisualisedQuery()) {
            updateDetailsView(true);
        }
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            initGraph();
            if (onDone != null) {
                onDone.onDone();
            }
        }).start();
    }

    private void addToGraph(@Nonnull List<Node<GraphOperator>> opsList, @Nullable GraphOperator parent, Set<String> addedIdentifiers, List<GraphOperator> addedNodes) {
        for (Node<GraphOperator> node : opsList) {
            GraphOperator op = node.getItem();
            if (!addedIdentifiers.contains(op.getIdentifier().get())) {
                insertVertexAndSetStyle(op);
                addedIdentifiers.add(op.getIdentifier().get());
                addedNodes.add(op);

                List<Node<GraphOperator>> successors = node.getSuccessors();
                if (!successors.isEmpty()) {
                    addToGraph(successors, op, addedIdentifiers, addedNodes);
                }
            } else {
                for (GraphOperator go : addedNodes) {
                    if (go.getIdentifier().get().equals(op.getIdentifier().get())) {
                        op = go;
                        break;
                    }
                }
            }

            if (parent != null) {
                selectedTab.getGraph().insertEdge(parent, op, new Stream(parent, op));
            }
        }
    }

    private void addToGraph2(@Nonnull List<Pair<Node<GraphOperator>, VisInfo>> opsList, Set<String> addedIdentifiers, List<GraphOperator> addedNodes) {
        List<Node<GraphOperator>> l = new LinkedList<>();
        for (Pair<Node<GraphOperator>, VisInfo> p : opsList) {
            p.getKey().getItem().setVisInfo(p.getValue());
            l.add(p.getKey());
        }
        addToGraph(l, null, addedIdentifiers, addedNodes);
    }

    @Nullable
    private String showModifyPopupWindow(ParsedOperator.Definition def, GraphOperator operator) { // From https://stackoverflow.com/a/37417736/7232269
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
            controller.init(def, operator);
            popupStage.showAndWait();
            return controller.getResult();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void update() {
        selectedTab.getGraphView().update();
    }

}
