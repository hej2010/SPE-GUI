package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import gui.utils.Time;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsTabData {
    VBox paneContent;
    TextField tFTime;
    ChoiceBox<String> cBTime, cBVisibility;
    Button btnTimeSave, btnShow, btnHide, btnHideOthers, btnShowAll;

    private final List<String> seriesNames;
    private final Map<String, Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>>> map;
    private final Map<String, Boolean> visibleMap;
    private final XYChartPane<Number, Number> chartPane;

    private long from;

    public MetricsTabData(XYChartPane<Number, Number> chartPane, List<String> seriesNames) {
        this.map = new HashMap<>();
        this.visibleMap = new HashMap<>();
        this.chartPane = chartPane;
        this.seriesNames = seriesNames;

        for (String seriesName : seriesNames) {
            ObservableList<XYChart.Data<Number, Number>> arr = FXCollections.observableArrayList();
            Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>> pair = new Pair<>(arr, new XYChart.Series<>(seriesName, arr));
            map.put(seriesName, pair);
            chartPane.getChart().getData().add(pair.getValue());
        }
    }

    void init(ObservableMap<String, Object> namespace) {
        paneContent = (VBox) namespace.get("paneContent");
        paneContent.getChildren().add(getChartPane());

        tFTime = (TextField) namespace.get("tFTime");
        cBTime = (ChoiceBox<String>) namespace.get("cBTime");
        cBVisibility = (ChoiceBox<String>) namespace.get("cBVisibility");
        btnTimeSave = (Button) namespace.get("btnTimeSave");
        btnShow = (Button) namespace.get("btnShow");
        btnHide = (Button) namespace.get("btnHide");
        btnHideOthers = (Button) namespace.get("btnHideOthers");
        btnShowAll = (Button) namespace.get("btnShowAll");

        List<String> list = new ArrayList<>();
        list.add("Seconds");
        list.add("Minutes");
        list.add("Hours");
        list.add("Days");
        cBTime.getItems().addAll(list);
        cBTime.getSelectionModel().select(1);

        list = new ArrayList<>(seriesNames);
        cBVisibility.getItems().addAll(list);
        cBVisibility.getSelectionModel().select(0);

        btnTimeSave.setOnAction(event -> {
            updateTimeRange();
            updateGraphTimeRange();
        });
        btnShow.setOnAction(event -> updateSeriesVisibility(cBVisibility.getSelectionModel().getSelectedItem(), true));
        btnHide.setOnAction(event -> updateSeriesVisibility(cBVisibility.getSelectionModel().getSelectedItem(), false));
        btnHideOthers.setOnAction(event -> {
            String selected = cBVisibility.getSelectionModel().getSelectedItem();
            for (String s : map.keySet()) {
                if (s.equals(selected)) {
                    updateSeriesVisibility(selected, true);
                } else {
                    updateSeriesVisibility(s, false);
                }
            }
        });
        btnShowAll.setOnAction(event -> {
            for (String s : map.keySet()) {
                updateSeriesVisibility(s, true);
            }
        });
        updateTimeRange();
    }

    private void updateSeriesVisibility(String selectedItem, boolean show) {
        visibleMap.put(selectedItem, show);
        //map.get(selectedItem).getValue().getChart().setVisible(show);
        for (XYChart.Series<Number, Number> s : map.get(selectedItem).getValue().getChart().getData()) {
            System.out.println("for " + s.getName() + ": " + selectedItem);
            if (s.getName().equals(selectedItem)) {
                s.getNode().setVisible(show); // Toggle visibility of line
                for (XYChart.Data<Number, Number> d : s.getData()) {
                    updateNodeVisibility(d.getNode(), show);
                }
                break;
            }
        }
    }

    private void updateNodeVisibility(Node n, boolean show) {
        if (n != null) {
            n.setVisible(show); // Toggle visibility of every node in the series
        }
    }

    private void updateTimeRange() {
        String time = tFTime.getText().trim();
        if (time.isEmpty()) {
            time = "5";
            tFTime.setText(time);
        }
        long from = -1;
        try {
            from = Long.parseLong(time);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (from > 0) {
            switch (cBTime.getSelectionModel().getSelectedIndex()) {
                case 1:
                    from = from * Time.MINUTE_SECONDS;
                    break;
                case 2:
                    from = from * Time.HOUR_SECONDS;
                    break;
                case 3:
                    from = from * Time.DAY_SECONDS;
                    break;
            }
        } else {
            from = 5;
        }
        this.from = from;
    }

    public XYChartPane<Number, Number> getChartPane() {
        return chartPane;
    }

    private void updateGraphTimeRange() {
        Platform.runLater(() -> {
            ((NumericAxis) chartPane.getChart().getXAxis()).setUpperBound(System.currentTimeMillis() / 1000 + 1);
            ((NumericAxis) chartPane.getChart().getXAxis()).setLowerBound(System.currentTimeMillis() / 1000 - from - 1);
        });
    }

    public void onNewData(@Nonnull LiebreMetrics.FileData fileData, String fileName) {
        Platform.runLater(() -> {

            for (MetricsData v : fileData.getValues()) {
                if (v instanceof MetricsDataSingle) {
                    XYChart.Data<Number, Number> chartData = new XYChart.Data<>(v.timestamp, ((MetricsDataSingle) v).value);
                    map.get(fileName).getKey().add(chartData);
                    updateNodeVisibility(chartData.getNode(), visibleMap.getOrDefault(fileName, true));
                } else if (v instanceof MetricsDataLiebre) {
                    for (String s : ((MetricsDataLiebre) v).getFields()) {
                        XYChart.Data<Number, Number> chartData = new XYChart.Data<>(v.timestamp, ((MetricsDataLiebre) v).getValueFor(s));
                        map.get(s).getKey().add(chartData);
                        updateNodeVisibility(chartData.getNode(), visibleMap.getOrDefault(s, true));
                    }
                }
                //System.out.println("add " + v.getKey() + ", " + Double.valueOf(v.getValue()));
            }
            updateGraphTimeRange();
        });
    }
}
