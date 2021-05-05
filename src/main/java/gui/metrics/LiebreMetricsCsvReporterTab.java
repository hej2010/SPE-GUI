package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.GUI;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class LiebreMetricsCsvReporterTab implements IMetricsTab {
    private final String name;
    private final Pane root;
    private final Map<String, MetricsTabData> mapToData;

    public LiebreMetricsCsvReporterTab(String name, List<String> seriesNames) throws IOException {
        this.name = name;
        this.mapToData = new HashMap<>();

        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_CONTENT2));
        root = fxmlLoader.load();

        TabPane tabPane = (TabPane) fxmlLoader.getNamespace().get("tabPane");

        for (String s : seriesNames) {
            List<String> seriesNames2 = new ArrayList<>(1);
            seriesNames2.addAll(getSeries());
            MetricsTabData data = new MetricsTabData(setupChartPane(s), seriesNames2);
            mapToData.put(s, data);

            Tab t = new Tab(s);
            fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_CONTENT));
            AnchorPane anchorPane = fxmlLoader.load();
            data.init(fxmlLoader.getNamespace());

            t.setContent(anchorPane);
            tabPane.getTabs().add(t);
        }
    }

    private List<String> getSeries() {
        return Arrays.asList(LiebreMetrics.CSV_NAMES);
    }

    public void onNewData(@Nonnull LiebreMetrics.FileData fileData) {
        String name = fileData.getFileName().split("\\.", 2)[0];
        MetricsTabData data = mapToData.get(name);
        if (data != null) {
            data.onNewData(fileData, name);
        }
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
        lineChart.setMaxHeight(200);
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
