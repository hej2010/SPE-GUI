package gui.controllers;

import gui.graph.dag.Node;
import gui.graph.data.GraphObject;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.graph.data.Stream;
import gui.graph.visualisation.VisInfo;
import gui.metrics.liebre.*;
import gui.utils.Files;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class LiebreMetricsController implements IOnNewMetricDataListener, IWindowListener {
    @FXML
    private TabPane tabPane;
    private Stage stage = null;

    private List<Pair<Node<GraphOperator>, VisInfo>> visResult;
    private LiebreMetrics liebreMetrics;
    private List<MetricsTab> metricsTabs;

    public void init(@Nonnull List<Pair<Node<GraphOperator>, VisInfo>> visResult, File directory, boolean isFromStart) {
        this.visResult = visResult;

        final List<GraphObject> graphObjects = getAllGraphObjects();

        this.liebreMetrics = new LiebreMetrics(directory, graphObjects, this);

        final List<File> filesToRead = liebreMetrics.getFilesToRead();
        try {
            setUpTabs(filesToRead);
        } catch (IOException e) {
            e.printStackTrace();
        }

        liebreMetrics.runAndListenAsync(!isFromStart);
    }

    private void setUpTabs(List<File> filesToRead) throws IOException {
        this.metricsTabs = new LinkedList<>();
        List<String> rates = new LinkedList<>();
        List<String> execs = new LinkedList<>();
        List<String> execsSimple = new LinkedList<>();
        List<String> ins = new LinkedList<>();
        List<String> outs = new LinkedList<>();
        for (File f : filesToRead) {
            String[] name = f.getName().split("\\.", 2);
            if (name[1].endsWith("RATE.csv")) {
                rates.add(name[0]);
            } else if (name[1].endsWith("EXEC.csv")) {
                String line = Files.readFirstLineOfFile(f);
                //System.out.println("line is " + line + " for file " + f.getName());
                if (line.startsWith("t")) {
                    execs.add(name[0]);
                } else {
                    execsSimple.add(name[0]);
                }
            } else if (name[1].endsWith("IN.csv")) {
                ins.add(name[0]);
            } else if (name[1].endsWith("OUT.csv")) {
                outs.add(name[0]);
            }
        }

        if (!rates.isEmpty()) {
            metricsTabs.add(new LiebreMetricsFileTab("RATE", rates));
        }
        if (!execs.isEmpty()) {
            metricsTabs.add(new LiebreMetricsCsvReporterTab("EXEC", execs));
        }
        if (!execsSimple.isEmpty()) {
            metricsTabs.add(new LiebreMetricsFileTab("EXEC", execsSimple));
        }
        if (!ins.isEmpty()) {
            metricsTabs.add(new LiebreMetricsFileTab("IN", ins));
        }
        if (!outs.isEmpty()) {
            metricsTabs.add(new LiebreMetricsFileTab("OUT", outs));
        }
        for (MetricsTab t : metricsTabs) {
            Tab tab = new Tab(t.getName());
            tab.setContent(t.getContent());
            tabPane.getTabs().add(tab);
        }
    }

    private List<GraphObject> getAllGraphObjects() {
        List<GraphObject> ops = new LinkedList<>();
        ////System.out.println("vis: " + visResult.size());
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
        for (MetricsTab t : metricsTabs) {
            t.stop();
        }
        if (stage != null) {
            stage.close();
        }
    }

    @Override
    public void onNewData(LiebreMetrics.FileData fileData) {
        String[] names = fileData.getFileName().split("\\.", 2);
        MetricsTab tab = null;
        for (MetricsTab t : metricsTabs) {
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
