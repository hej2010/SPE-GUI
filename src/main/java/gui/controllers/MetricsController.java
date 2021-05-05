package gui.controllers;

import gui.graph.dag.Node;
import gui.graph.data.GraphObject;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.graph.data.Stream;
import gui.graph.visualisation.VisInfo;
import gui.metrics.*;
import gui.spe.ParsedSPE;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class MetricsController implements IOnNewMetricDataListener {
    @FXML
    private TabPane tabPane;
    private Stage stage = null;

    private ParsedSPE parsedSPE;
    private List<Pair<Node<GraphOperator>, VisInfo>> visResult;
    private LiebreMetrics liebreMetrics;
    private List<IMetricsTab> metricsTabs;

    public void init(@Nonnull ParsedSPE parsedSPE, @Nonnull List<Pair<Node<GraphOperator>, VisInfo>> visResult) {
        this.parsedSPE = parsedSPE;
        this.visResult = visResult;

        final List<GraphObject> graphObjects = getAllGraphObjects();

        this.liebreMetrics = new LiebreMetrics(Path.of("").toAbsolutePath().toFile(), graphObjects, this);

        final List<File> filesToRead = liebreMetrics.getFilesToRead();
        try {
            setUpTabs(filesToRead);
        } catch (IOException e) {
            e.printStackTrace();
        }

        liebreMetrics.runAndListenAsync(true);
    }

    private void setUpTabs(List<File> filesToRead) throws IOException {
        this.metricsTabs = new LinkedList<>();
        List<String> rates = new LinkedList<>();
        List<String> execs = new LinkedList<>();
        List<String> ins = new LinkedList<>();
        List<String> outs = new LinkedList<>();
        for (File f : filesToRead) {
            String[] name = f.getName().split("\\.", 2);
            if (name[1].endsWith("RATE.csv")) {
                rates.add(name[0]);
            } else if (name[1].endsWith("EXEC.csv")) {
                execs.add(name[0]);
            } else if (name[1].endsWith("IN.csv")) {
                ins.add(name[0]);
            } else if (name[1].endsWith("OUT.csv")) {
                outs.add(name[0]);
            }
        }

        if (!rates.isEmpty()) {
            metricsTabs.add(new LiebreMetricsFileTab("RATE", execs));
        }
        if (!execs.isEmpty()) {
            metricsTabs.add(new LiebreMetricsCsvReporterTab("EXEC", execs)); // TODO change THIS ONLY depending on which metrics it reports (read first line of EXEC file)
        }
        if (!ins.isEmpty()) {
            metricsTabs.add(new LiebreMetricsFileTab("IN", ins));
        }
        if (!outs.isEmpty()) {
            metricsTabs.add(new LiebreMetricsFileTab("OUT", outs));
        }
        for (IMetricsTab t : metricsTabs) {
            Tab tab = new Tab(t.getName());
            tab.setContent(t.getContent());
            tabPane.getTabs().add(tab);
        }
    }

    private List<GraphObject> getAllGraphObjects() {
        List<GraphObject> ops = new LinkedList<>();
        System.out.println("vis: " + visResult.size());
        for (Pair<Node<GraphOperator>, VisInfo> p : visResult) {
            ops.add(p.getKey().getItem());
        }
        ops.addAll(getConnected());
        return ops;
    }

    private List<GraphStream> getConnected() {
        List<GraphStream> list = new LinkedList<>();
        for (Pair<Node<GraphOperator>, VisInfo> p : visResult) {
            for (Node<GraphOperator> n : p.getKey().getSuccessors()) {
                Stream s = new Stream(p.getKey().getItem(), n.getItem());
                if (!list.contains(s)) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * setting the stage of this view
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Closes the stage of this view
     */
    public void closeStage() {
        liebreMetrics.stop();
        if (stage != null) {
            stage.close();
        }
    }

    @Override
    public void onNewData(LiebreMetrics.FileData fileData) {
        String[] names = fileData.getFileName().split("\\.", 2);
        //System.out.println("received " + fileData);
        IMetricsTab tab = null;
        for (IMetricsTab t : metricsTabs) {
            if (names[1].startsWith(t.getName())) {
                tab = t;
                break;
            }
        }
        if (tab == null) {
            throw new RuntimeException("No tab found for " + fileData.getFileName() + "!");
        }

        tab.onNewData(fileData);
    }

}
