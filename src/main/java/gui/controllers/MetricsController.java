package gui.controllers;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.graph.dag.Node;
import gui.graph.data.GraphObject;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.graph.data.Stream;
import gui.graph.visualisation.VisInfo;
import gui.metrics.IOnNewMetricDataListener;
import gui.metrics.LiebreFileMetrics;
import gui.spe.ParsedSPE;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MetricsController implements IOnNewMetricDataListener {
    @FXML
    private TabPane tabPane;
    private Stage stage = null;

    private ParsedSPE parsedSPE;
    private List<Pair<Node<GraphOperator>, VisInfo>> visResult;
    private LiebreFileMetrics liebreFileMetrics;
    private List<MetricsTab> metricsTabs;

    public void init(@Nonnull ParsedSPE parsedSPE, @Nonnull List<Pair<Node<GraphOperator>, VisInfo>> visResult) {
        this.parsedSPE = parsedSPE;
        this.visResult = visResult;

        final List<GraphObject> graphObjects = getAllGraphObjects();

        this.liebreFileMetrics = new LiebreFileMetrics(Path.of("").toAbsolutePath().toFile(), graphObjects, this);

        final List<File> filesToRead = liebreFileMetrics.getFilesToRead();
        setUpTabs(filesToRead);

        liebreFileMetrics.runAndListenAsync(true);
    }

    private void setUpTabs(List<File> filesToRead) {
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
            metricsTabs.add(new MetricsTab("RATE", execs));
        }
        if (!execs.isEmpty()) {
            metricsTabs.add(new MetricsTab("EXEC", execs));
        }
        if (!ins.isEmpty()) {
            metricsTabs.add(new MetricsTab("IN", ins));
        }
        if (!outs.isEmpty()) {
            metricsTabs.add(new MetricsTab("OUT", outs));
        }
        for (MetricsTab t : metricsTabs) {
            Tab tab = new Tab(t.name);
            tab.setContent(t.getChartPane());
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
        liebreFileMetrics.stop();
        if (stage != null) {
            stage.close();
        }
    }

    @Override
    public void onNewData(LiebreFileMetrics.FileData fileData) {
        String[] names = fileData.getFileName().split("\\.", 2);
        //System.out.println("received " + fileData);
        MetricsTab tab = null;
        for (MetricsTab t : metricsTabs) {
            if (names[1].startsWith(t.name)) {
                tab = t;
                break;
            }
        }
        if (tab == null) {
            throw new RuntimeException("No tab found for " + fileData.getFileName() + "!");
        }

        tab.onNewData(fileData);
    }

    private static class MetricsTab {
        private final String name;
        private final Map<String, Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>>> map;
        private final XYChartPane<Number, Number> chartPane;

        private final long start;
        private double lowest;
        private double highest;

        private MetricsTab(String name, List<String> seriesNames) {
            this.name = name;
            this.map = new HashMap<>();
            this.chartPane = setupChartPane(name);
            for (String seriesName : seriesNames) {
                ObservableList<XYChart.Data<Number, Number>> arr = FXCollections.observableArrayList();
                Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>> pair = new Pair<>(arr, new XYChart.Series<>(seriesName, arr));
                map.put(seriesName, pair);
                chartPane.getChart().getData().add(pair.getValue());
            }
            this.start = System.currentTimeMillis() / 1000;
            this.lowest = Double.MAX_VALUE;
            this.highest = Double.MIN_VALUE;
        }

        private void onNewData(LiebreFileMetrics.FileData fileData) {
            Platform.runLater(() -> {
                        if (!map.get(fileData.getFileName().split("\\.", 2)[0]).getKey().addAll(LiebreFileMetrics.toChartData(fileData))) {
                            System.out.println("DID NOT CHANGE!");
                        }
                        for (Pair<Long, String> p : fileData.getValues()) {
                            double v = Double.parseDouble(p.getValue());
                            //System.out.println("parsed " + v);
                            if (v < lowest) {
                                lowest = v;
                            } else if (v > highest) {
                                highest = v;
                            }
                        }
                        double diff = (highest - lowest) / 10;
                        ((NumericAxis) chartPane.getChart().getXAxis()).setUpperBound(System.currentTimeMillis() / 1000.0 + 1);
                        ((NumericAxis) chartPane.getChart().getXAxis()).setLowerBound(start);
                        ((NumericAxis) chartPane.getChart().getYAxis()).setLowerBound(lowest - diff);
                        ((NumericAxis) chartPane.getChart().getYAxis()).setUpperBound(highest + diff);
                    }
            );
        }

        private XYChartPane<Number, Number> setupChartPane(String name) {
            XYChartPane<Number, Number> chartPane = new XYChartPane<>(getLineChart());
            chartPane.setTitle("Data for: " + name);
            chartPane.setCommonYAxis(false);
            chartPane.getPlugins().addAll(new CrosshairIndicator<>(), new DataPointTooltip<>());
            chartPane.getStylesheets().add("gui/mixed-chart-sample.css");
            return chartPane;
        }

        private LineChart<Number, Number> getLineChart() {
            LineChart<Number, Number> lineChart = new LineChart<>(createXAxis(false, new Pair<>((int) (System.currentTimeMillis() / 1000 - 600), (int) (System.currentTimeMillis() / 1000))), createYAxis());
            lineChart.getStyleClass().add("chart1");
            lineChart.setAnimated(true);
            lineChart.setCreateSymbols(true);
            lineChart.getYAxis().setLabel("Value");
            lineChart.getYAxis().setSide(Side.RIGHT);
            lineChart.getXAxis().setLabel("Time");
            //lineChart.getData().add(new XYChart.Series<>("Data 1", LiebreFileMetrics.toChartData()));
            return lineChart;
        }

        private NumericAxis createYAxis() {
            NumericAxis yAxis = new NumericAxis();
            yAxis.setAnimated(false);
            yAxis.setForceZeroInRange(false);
            yAxis.setAutoRangePadding(0.1);
            yAxis.setAutoRangeRounding(true);
            return yAxis;
        }

        private NumericAxis createXAxis(boolean autoFit, Pair<Integer, Integer> dateRange) {
            NumericAxis xAxis = new NumericAxis();
            xAxis.setAnimated(false);
            xAxis.setForceZeroInRange(false);
            xAxis.setAutoRangePadding(0.1); // TODO set range from selection
            xAxis.setAutoRangeRounding(autoFit);
            if (!autoFit) {
                xAxis.setAutoRanging(false);
                xAxis.setUpperBound(dateRange.getValue());
                xAxis.setLowerBound(dateRange.getKey());
            }
            return xAxis;
        }

        public XYChartPane<Number, Number> getChartPane() {
            return chartPane;
        }
    }
}
