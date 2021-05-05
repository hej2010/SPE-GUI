package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.GUI;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class LiebreMetricsFileTab extends MetricsTab {
    private final String name;
    private final Pane root;
    private final MetricsTabData data;

    //private final long start;
    //private double lowest;
    //private double highest;

    public LiebreMetricsFileTab(String name, List<String> seriesNames) throws IOException {
        this.name = name;
        data = new MetricsTabData(setupChartPane(name), seriesNames);
        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_CONTENT));
        root = fxmlLoader.load();

        paneContent = (VBox) fxmlLoader.getNamespace().get("paneContent");
        paneContent.getChildren().add(data.getChartPane());
        tFTime = (TextField) fxmlLoader.getNamespace().get("tFTime");
        cBTime = (ChoiceBox<String>) fxmlLoader.getNamespace().get("cBTime");
        btnTimeSave = (Button) fxmlLoader.getNamespace().get("btnTimeSave");
        super.init();

        //this.start = System.currentTimeMillis() / 1000;
        //this.lowest = Double.MAX_VALUE;
        //this.highest = Double.MIN_VALUE;
    }

    public void onNewData(@Nonnull LiebreMetrics.FileData fileData) {
        String name = fileData.getFileName().split("\\.", 2)[0];
        if (data != null) {
            data.onNewData(fileData, name, super.getFromTimestampInSeconds());
        }
    }

    @Override
    void updateGraphTimeRange(long from) {

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
        lineChart.setAnimated(false);
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
