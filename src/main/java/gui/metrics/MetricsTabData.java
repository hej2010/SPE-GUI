package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import gui.utils.Time;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    ChoiceBox<String> cBTime;
    Button btnTimeSave;

    private final Map<String, Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>>> map;
    private final XYChartPane<Number, Number> chartPane;

    private long from, lastUpdated = 0;

    public MetricsTabData(XYChartPane<Number, Number> chartPane, List<String> seriesNames) {
        this.map = new HashMap<>();
        this.chartPane = chartPane;
        for (String seriesName : seriesNames) {
            ObservableList<XYChart.Data<Number, Number>> arr = FXCollections.observableArrayList();
            Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>> pair = new Pair<>(arr, new XYChart.Series<>(seriesName, arr));
            map.put(seriesName, pair);
            chartPane.getChart().getData().add(pair.getValue());
        }
        //this.lowest = Double.MAX_VALUE;
        //this.highest = Double.MIN_VALUE;
    }

    void init() {
        List<String> list = new ArrayList<>();
        list.add("Seconds");
        list.add("Minutes");
        list.add("Hours");
        list.add("Days");
        cBTime.getItems().addAll(list);
        cBTime.getSelectionModel().select(1);

        btnTimeSave.setOnAction(e -> {
                    updateTimeRange();
                    updateGraphTimeRange(from);
                }
        );
    }

    long getFromTimestampInSeconds() {
        updateTimeRange();
        return System.currentTimeMillis() / 1000 - from - 1;
    }

    private void updateTimeRange() {
        long now = System.currentTimeMillis();
        if (now - lastUpdated >= 1000) {
            lastUpdated = now;
        } else {
            return;
        }
        String time = tFTime.getText().trim();
        if (time.isEmpty()) {
            time = "5";
            tFTime.setText(time);
        }
        from = -1;
        try {
            from = Long.parseLong(time);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        System.out.println("parsed " + from);
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
        System.out.println("updated from to " + from);
    }

    public XYChartPane<Number, Number> getChartPane() {
        return chartPane;
    }

    public void onNewData(@Nonnull LiebreMetrics.FileData fileData, String fileName, long from) {
        Platform.runLater(() -> {
            for (MetricsData v : fileData.getValues()) {
                if (v instanceof MetricsDataSingle) {
                    map.get(fileName).getKey().add(new XYChart.Data<>(v.timestamp, ((MetricsDataSingle) v).value));
                } else if (v instanceof MetricsDataLiebre) {
                    for (String s : ((MetricsDataLiebre) v).getFields()) {
                        map.get(s).getKey().add(new XYChart.Data<>(v.timestamp, ((MetricsDataLiebre) v).getValueFor(s)));
                    }
                }
                //System.out.println("add " + v.getKey() + ", " + Double.valueOf(v.getValue()));
            }
            ((NumericAxis) chartPane.getChart().getXAxis()).setUpperBound(System.currentTimeMillis() / 1000.0 + 1);
            ((NumericAxis) chartPane.getChart().getXAxis()).setLowerBound(from);
        });
    }
}
