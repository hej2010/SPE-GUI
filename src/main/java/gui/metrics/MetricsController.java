package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Pair;
import gui.metrics.graphite.GraphiteRenderQuery;
import gui.metrics.graphite.RenderDatapoint;
import gui.utils.Time;

import java.util.HashMap;
import java.util.Map;

public class MetricsController {

    @FXML
    private TextField tFQuery, tFQueryFrom, tFQueryTo;
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

    public MetricsController() {
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
    }

    private void getMetrics() {
        Pair<Pair<String, String>, Pair<Integer, Integer>> dateRange = getSelectedDateRange();
        Pair<String, String> stringTime = dateRange.getKey();
        Pair<Integer, Integer> doubleTime = dateRange.getValue();
        Map<String, String> map = new HashMap<>();
        map.put("format", "json");
        map.put("target", tFQuery.getText()); // liebre.name.I1.EXEC.count
        map.put("from", stringTime.getValue());
        map.put("until", stringTime.getKey());
        GraphiteRenderQuery q = GraphiteRenderQuery.run(map);
        System.out.println(q);
        if (q == null) {
            lError.setText("Error");
        } else {
            lError.setText("");
            handleData(q, doubleTime);
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

    private void handleData(GraphiteRenderQuery q, Pair<Integer, Integer> dateRange) {
        LineChart<Number, Number> lineChart = new LineChart<>(createXAxis(cBAutoFit.isSelected(), dateRange), createYAxis());
        lineChart.getStyleClass().add("chart1");
        lineChart.setAnimated(true);
        lineChart.setCreateSymbols(true);
        lineChart.getYAxis().setLabel("Value");
        lineChart.getYAxis().setSide(Side.RIGHT);
        lineChart.getXAxis().setLabel("Time");
        lineChart.getData().add(new XYChart.Series<>("Data 1", RenderDatapoint.toChartData(q.getDataPoints())));

        showGraph(lineChart, q);
    }

    private void showGraph(LineChart<Number, Number> lineChart, GraphiteRenderQuery q) {
        XYChartPane<Number, Number> chartPane = new XYChartPane<>(lineChart);
        chartPane.setTitle("Data for query: " + q.getTarget());
        chartPane.setCommonYAxis(false);
        chartPane.getPlugins().addAll(new CrosshairIndicator<>(), new DataPointTooltip<>());
        chartPane.getStylesheets().add("gui/mixed-chart-sample.css");

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

}
