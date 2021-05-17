package gui.metrics.liebre;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsTabData {
    VBox paneContent;
    TextField tFTime;
    ChoiceBox<String> cBTime, cBVisibility;
    Button btnTimeSave, btnShow, btnHide, btnHideOthers, btnShowAll;

    private final List<String> seriesNames;
    private final Map<String, XYChart.Series<Number, Number>> map;
    private final Map<String, Boolean> visibleMap;
    private final XYChartPane<Number, Number> chartPane;
    private final Map<String, List<XYChart.Data<Number, Number>>> newData;

    private long from;
    private final ScheduledExecutorService exec;

    public MetricsTabData(XYChartPane<Number, Number> chartPane, List<String> seriesNames) {
        this.map = new HashMap<>();
        this.visibleMap = new HashMap<>();
        this.chartPane = chartPane;
        this.seriesNames = seriesNames;
        this.newData = new HashMap<>();

        for (String seriesName : seriesNames) {
            ObservableList<XYChart.Data<Number, Number>> arr = FXCollections.observableArrayList();
            XYChart.Series<Number, Number> series = new XYChart.Series<>(seriesName, arr);
            map.put(seriesName, series);
            newData.put(seriesName, new LinkedList<>());
            chartPane.getChart().getData().add(series);
        }
        //System.out.println("newdata initialized to " + newData.keySet());

        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(this::addNewData, 2, 1, TimeUnit.SECONDS);
    }

    private void addNewData() {
        Platform.runLater(() -> {
            synchronized (newData) {
                for (String s : newData.keySet()) {
                    map.get(s).getData().addAll(newData.get(s));
                    for (XYChart.Data<Number, Number> a : newData.get(s)) {
                        updateNodeVisibility(a.getNode(), visibleMap.getOrDefault(s, true));
                    }
                    newData.get(s).clear();
                }
            }
        });
    }

    void init(ObservableMap<String, Object> namespace) {
        paneContent = (VBox) namespace.get("paneContent");
        XYChartPane<Number, Number> chartPane = getChartPane();
        paneContent.getChildren().add(chartPane);
        VBox.setVgrow(chartPane, Priority.ALWAYS);

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
        btnShow.setOnAction(event -> {
            updateSeriesVisibility(cBVisibility.getSelectionModel().getSelectedItem(), true);
            this.chartPane.getChart().getYAxis().setAutoRanging(true);
        });
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
            this.chartPane.getChart().getYAxis().setAutoRanging(true);
            for (String s : map.keySet()) {
                updateSeriesVisibility(s, true);
            }
        });
        updateTimeRange();
    }

    private void updateSeriesVisibility(String selectedItem, boolean show) {
        visibleMap.put(selectedItem, show);
        for (XYChart.Series<Number, Number> s : map.get(selectedItem).getChart().getData()) {
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
            synchronized (newData) {
                for (MetricsData v : fileData.getValues()) {
                    if (v instanceof MetricsDataSingle) {
                        if (newData.get(fileName) == null) {
                            //System.out.println("newData is null for " + fileName);
                            continue;
                        }
                        //newData.get(fileName).add(new XYChart.Data<>(v.timestamp, ((MetricsDataSingle) v).value));
                    } else if (v instanceof MetricsDataLiebre) {
                        for (String s : ((MetricsDataLiebre) v).getFields()) {
                            if (newData.get(s) == null) {
                                //System.out.println("newData s is null for " + s);
                                continue;
                            }
                            //newData.get(s).add(new XYChart.Data<>(v.timestamp, ((MetricsDataLiebre) v).getValueFor(s)));
                        }
                    }
                }
            }
            updateGraphTimeRange();
        });
    }

    public void stop() {
        exec.shutdownNow();
    }
}
