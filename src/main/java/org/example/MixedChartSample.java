package org.example;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MixedChartSample extends Application {
    private static final List<String> DAYS = new ArrayList<>(
            Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Mixed Chart Sample");

        BarChart<String, Number> barChart = new BarChart<>(createXAxis(), createYAxis());
        barChart.getStyleClass().add("chart1");
        barChart.setAnimated(false);
        barChart.getYAxis().setLabel("Data 1");
        barChart.getYAxis().setSide(Side.LEFT);
        barChart.getData().add(new XYChart.Series<>("Data 1", createTestData(3)));

        LineChart<String, Number> lineChart = new LineChart<>(createXAxis(), createYAxis());
        lineChart.getStyleClass().add("chart2");
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true);
        lineChart.getYAxis().setLabel("Data 2");
        lineChart.getYAxis().setSide(Side.RIGHT);
        lineChart.getData().add(new XYChart.Series<>("Data 2", createTestData(10)));

        ScatterChart<String, Number> scatterChart = new ScatterChart<>(createXAxis(), createYAxis());
        scatterChart.getStyleClass().add("chart3");
        scatterChart.setAnimated(false);
        scatterChart.getYAxis().setLabel("Data 3");
        scatterChart.getYAxis().setSide(Side.RIGHT);
        scatterChart.getData().add(new XYChart.Series<>("Data 3", createTestData(20)));

        XYChartPane<String, Number> chartPane = new XYChartPane<>(barChart);
        chartPane.setTitle("Mixed chart types");
        chartPane.setCommonYAxis(false);
        chartPane.getOverlayCharts().addAll(lineChart, scatterChart);
        chartPane.getPlugins().addAll(new CrosshairIndicator<>(), new DataPointTooltip<>());
        chartPane.getStylesheets().add("org/example/mixed-chart-sample.css");

        BorderPane borderPane = new BorderPane(chartPane);
        Scene scene = new Scene(borderPane, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private NumericAxis createYAxis() {
        NumericAxis yAxis = new NumericAxis();
        yAxis.setAnimated(false);
        yAxis.setForceZeroInRange(false);
        yAxis.setAutoRangePadding(0.1);
        yAxis.setAutoRangeRounding(true);
        return yAxis;
    }

    private CategoryAxis createXAxis() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAnimated(false);
        return xAxis;
    }

    private ObservableList<XYChart.Data<String, Number>> createTestData(double refVal) {
        Random rnd = new Random();
        List<XYChart.Data<String, Number>> data = new ArrayList<>();
        for (int i = 0; i < DAYS.size(); i++) {
            data.add(new XYChart.Data<>(DAYS.get(i), refVal - Math.abs(3 - i) + rnd.nextDouble()));
        }
        return FXCollections.observableArrayList(data);
    }

    public static void main(String[] args) {
        launch(args);
    }
}