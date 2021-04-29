package gui.controllers;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.visualisation.VisInfo;
import gui.metrics.LiebreFileMetrics;
import gui.spe.ParsedSPE;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class MetricsController {
    @FXML
    private AnchorPane aPtab1;
    private Stage stage = null;
    private XYChartPane<Number, Number> chartPane;

    private ParsedSPE parsedSPE;
    private List<Pair<Node<GraphOperator>, VisInfo>> visResult;
    private LiebreFileMetrics liebreFileMetrics;

    public void init(@Nonnull ParsedSPE parsedSPE, @Nonnull List<Pair<Node<GraphOperator>, VisInfo>> visResult) {
        this.parsedSPE = parsedSPE;
        this.visResult = visResult;

        chartPane = new XYChartPane<>(getLineChart());
        chartPane.setTitle("Data for query: " + "dsffd");
        chartPane.setCommonYAxis(false);
        chartPane.getPlugins().addAll(new CrosshairIndicator<>(), new DataPointTooltip<>());
        chartPane.getStylesheets().add("gui/mixed-chart-sample.css");

        aPtab1.getChildren().add(chartPane);
        final ObservableList<XYChart.Data<Number, Number>> observableList = FXCollections.observableArrayList();
        XYChart.Series<Number, Number> series = new XYChart.Series<>("Data 1", observableList);
        chartPane.getChart().getData().add(series);
        final long start = System.currentTimeMillis() / 1000;
        final double[] lowest = {Double.MAX_VALUE};
        final double[] highest = {Double.MIN_VALUE};

        this.liebreFileMetrics = new LiebreFileMetrics(Path.of("").toAbsolutePath().toFile(), getList(), fileData -> {
            if (fileData.getFileName().equals("myMap.RATE.csv")) {
                System.out.println("received " + fileData);
                Platform.runLater(() -> {
                            if (!observableList.addAll(LiebreFileMetrics.toChartData(fileData))) {
                                System.out.println("DID NOT CHANGE!");
                            }
                            for (Pair<Long, String> p : fileData.getValues()) {
                                double v = Double.parseDouble(p.getValue());
                                System.out.println("parsed " + v);
                                if (v < lowest[0]) {
                                    lowest[0] = v;
                                } else if (v > highest[0]) {
                                    highest[0] = v;
                                }
                            }
                            double diff = (highest[0] - lowest[0]) / 10;
                            ((NumericAxis) chartPane.getChart().getXAxis()).setUpperBound(System.currentTimeMillis() / 1000.0 + 1);
                            ((NumericAxis) chartPane.getChart().getXAxis()).setLowerBound(start);
                            ((NumericAxis) chartPane.getChart().getYAxis()).setLowerBound(lowest[0] - diff);
                            ((NumericAxis) chartPane.getChart().getYAxis()).setUpperBound(highest[0] + diff);
                        }
                );
            }
        });
        liebreFileMetrics.runAndListenAsync(true);
    }

    private List<GraphOperator> getList() {
        List<GraphOperator> ops = new LinkedList<>();
        for (Pair<Node<GraphOperator>, VisInfo> p : visResult) {
            ops.add(p.getKey().getItem());
        }
        return ops;
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

    /**
     * setting the stage of this view
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Closes the stage of this view
     */
    public void closeStage() { // TODO stop all threads etc
        liebreFileMetrics.stop();
        if (stage != null) {
            stage.close();
        }
    }
}
