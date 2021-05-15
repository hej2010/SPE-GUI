package gui.controllers;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.metrics.graphite.GraphiteMetricsQuery;
import gui.metrics.graphite.GraphiteRenderQuery;
import gui.metrics.graphite.RenderDatapoint;
import gui.utils.Time;
import gui.views.AutoCompleteTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class GraphiteMetricsController implements IWindowListener {
    @FXML
    private AutoCompleteTextField<String> tFQuery;
    @FXML
    private TextField tFQueryFrom, tFQueryTo;
    @FXML
    private Button btnGo;
    @FXML
    private TabPane tabPane;
    @FXML
    private Label lError;
    @FXML
    private ChoiceBox<String> cBFrom, cBTo;
    @FXML
    private CheckBox cBAutoFit;

    private Stage stage = null;

    public GraphiteMetricsController() {
    }

    @FXML
    private void initialize() {
        setUI();
    }

    private void setUI() {
        btnGo.setOnAction(e -> {
            getMetrics();
        });
        ObservableList<String> dateChoices = FXCollections.observableArrayList("s", "min", "h", "d", "w", "mon", "y");
        cBFrom.setItems(dateChoices);
        cBTo.setItems(dateChoices);
        cBFrom.getSelectionModel().select(2);
        cBTo.getSelectionModel().select(2);
        cBAutoFit.setSelected(true);
        fetchMetrics();
    }

    private void fetchMetrics() {
        new Thread(() -> {
            List<String> list = GraphiteMetricsQuery.run();
            //System.out.println("got " + list);
            if (list != null) {
                Platform.runLater(() -> {
                    tFQuery.setEntries(new TreeSet<>(list));
                    tFQuery.setMaxEntries(20);
                    GUIController.setOnAction(tFQuery);
                });
            }
        }).start();
    }

    private void getMetrics() {
        Pair<Pair<String, String>, Pair<Integer, Integer>> dateRange = getSelectedDateRange();
        Pair<String, String> stringTime = dateRange.getKey();
        Pair<Integer, Integer> doubleTime = dateRange.getValue();
        final String query = tFQuery.getText();
        Map<String, String> map = new HashMap<>();
        map.put("format", "json");
        map.put("target", query); // liebre.name.I1.EXEC.count
        map.put("from", stringTime.getValue());
        map.put("until", stringTime.getKey());
        List<GraphiteRenderQuery> q = GraphiteRenderQuery.run(map);
        //System.out.println(q);
        if (q == null) {
            lError.setText("Error");
        } else {
            lError.setText("");
            handleData(q, doubleTime, query);
        }
    }

    private Pair<Pair<String, String>, Pair<Integer, Integer>> getSelectedDateRange() {
        final int now = (int) (System.currentTimeMillis() / 1000);
        String from = tFQueryFrom.getText();
        if (from.isBlank()) {
            from = "-1";
        }
        String selected = cBFrom.getSelectionModel().getSelectedItem();
        final int relativeFrom = getTimeInSeconds(now, from, selected);
        int relativeTo = now;
        from = from + selected;
        String to = tFQueryTo.getText();
        if (!to.isBlank()) {
            selected = cBTo.getSelectionModel().getSelectedItem();
            relativeTo = getTimeInSeconds(now, to, selected);
            to = to + selected;
        }
        return new Pair<>(new Pair<>(from, to), new Pair<>(relativeFrom, relativeTo));
    }

    private int getTimeInSeconds(int now, String timeString, String dateFormatString) {
        if (timeString.startsWith("-")) {
            timeString = timeString.substring(1);
        }
        double value = -1;
        try {
            value = Double.parseDouble(timeString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        int time = Time.fromString(dateFormatString);
        //System.out.println("Relative time: " + value + " seconds? " + timeString + ":" + dateFormatString);
        if (value == -1) {
            return now;
        }
        return (int) (now - value * time);
    }

    private void handleData(List<GraphiteRenderQuery> queryList, Pair<Integer, Integer> dateRange, String query) {
        LineChart<Number, Number> lineChart = new LineChart<>(createXAxis(cBAutoFit.isSelected(), dateRange), createYAxis());
        lineChart.getStyleClass().add("chart1");
        lineChart.setAnimated(true);
        lineChart.setCreateSymbols(true);
        lineChart.getYAxis().setLabel("Value");
        lineChart.getYAxis().setSide(Side.RIGHT);
        lineChart.getXAxis().setLabel("Time");
        int count = 1;
        for (GraphiteRenderQuery q : queryList) {
            String target = q.getTarget();
            String seriesName = "Data " + count++;
            if (target != null) {
                String[] s = q.getTarget().split("\\.");
                seriesName = s[s.length - 1];
            }
            lineChart.getData().add(new XYChart.Series<>(seriesName, RenderDatapoint.toChartData(q.getDataPoints())));
        }

        showGraph(lineChart, query);
    }

    private void showGraph(LineChart<Number, Number> lineChart, String query) {
        XYChartPane<Number, Number> chartPane = new XYChartPane<>(lineChart);
        chartPane.setTitle("Data for query: " + query);
        chartPane.setCommonYAxis(false);
        chartPane.getPlugins().addAll(new CrosshairIndicator<>(), new DataPointTooltip<>());
        chartPane.getStylesheets().add("mixed-chart-sample.css");

        int size = tabPane.getTabs().size();
        Tab tab = new Tab("Tab " + size);
        tabPane.getTabs().add(tab);
        tab.setContent(chartPane);
        tabPane.getSelectionModel().select(size);
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

    public void closeStage() {
        if (stage != null) {
            stage.close();
        }
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
