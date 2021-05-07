package gui.metrics.liebre;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.metrics.views.MyLineChart;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import javax.annotation.Nonnull;

public abstract class MetricsTab {

    public abstract void onNewData(@Nonnull LiebreMetrics.FileData fileData);

    public abstract Pane getContent();

    public abstract String getName();

    XYChartPane<Number, Number> setupChartPane(String name) {
        XYChartPane<Number, Number> chartPane = new XYChartPane<>(getLineChart());
        chartPane.setTitle("Data for: " + name);
        chartPane.setCommonYAxis(false);
        chartPane.getPlugins().addAll(new CrosshairIndicator<>(), new DataPointTooltip<>());
        chartPane.getStylesheets().add("gui/mixed-chart-sample.css");
        return chartPane;
    }

    private LineChart<Number, Number> getLineChart() {
        LineChart<Number, Number> lineChart = new MyLineChart<>(createXAxis(false, new Pair<>((int) (System.currentTimeMillis() / 1000 - 600), (int) (System.currentTimeMillis() / 1000))), createYAxis());
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
        xAxis.setAutoRangeRounding(true);
        if (!autoFit) {
            xAxis.setAutoRanging(false);
            xAxis.setUpperBound(dateRange.getValue());
            xAxis.setLowerBound(dateRange.getKey());
        }
        return xAxis;
    }

    public abstract void stop();
}
