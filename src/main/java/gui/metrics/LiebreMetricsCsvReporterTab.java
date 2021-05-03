package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.GUI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiebreMetricsCsvReporterTab implements MetricsTab {
    private final String name;
    private final Map<String, Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>>> map;
    private final XYChartPane<Number, Number> chartPane;
    private final Pane root;

    private final long start;
    private double lowest;
    private double highest;

    public LiebreMetricsCsvReporterTab(String name, List<String> seriesNames) throws IOException {
        this.name = name;
        this.map = new HashMap<>();
        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_CONTENT));
        root = fxmlLoader.load();

        Pane paneContent = (Pane) fxmlLoader.getNamespace().get("paneContent");
        this.chartPane = setupChartPane(name);
        paneContent.getChildren().add(this.chartPane);
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

    public void onNewData(@Nonnull LiebreFileMetrics.FileData fileData) {
        Platform.runLater(() -> {
                    if (!map.get(fileData.getFileName().split("\\.", 2)[0]).getKey()
                            .addAll(LiebreFileMetrics.toChartData(fileData))) { // TODO displaying the data increases CPU usage 10x ..
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

    public Pane getContent() {
        return root;
    }

    public String getName() {
        return name;
    }
}
