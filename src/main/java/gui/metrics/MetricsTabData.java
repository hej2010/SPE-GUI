package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.*;

public class MetricsTabData {
    private final Map<String, Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>>> map;
    private final XYChartPane<Number, Number> chartPane;

    private final long start;
    //private double lowest;
    //private double highest;

    public MetricsTabData(XYChartPane<Number, Number> chartPane, List<String> seriesNames) {
        this.map = new HashMap<>();
        this.chartPane = chartPane;
        for (String seriesName : seriesNames) {
            ObservableList<XYChart.Data<Number, Number>> arr = FXCollections.observableArrayList();
            Pair<ObservableList<XYChart.Data<Number, Number>>, XYChart.Series<Number, Number>> pair = new Pair<>(arr, new XYChart.Series<>(seriesName, arr));
            map.put(seriesName, pair);
            chartPane.getChart().getData().add(pair.getValue());
        }
        this.start = System.currentTimeMillis() / 1000;
        //this.lowest = Double.MAX_VALUE;
        //this.highest = Double.MIN_VALUE;
    }

    public XYChartPane<Number, Number> getChartPane() {
        return chartPane;
    }

    public void onNewData(@Nonnull LiebreMetrics.FileData fileData, String fileName) {
        Platform.runLater(() -> {
            /*if (!map.get(fileName).getKey().addAll(toChartData(fileData, fileName))) { // TODO displaying the data increases CPU usage 10x ..
                for (MetricsData p : fileData.getValues()) {
                    double v = Double.parseDouble(p.getValue());
                    //System.out.println("parsed " + v);
                    if (v < lowest) {
                        lowest = v;
                    } else if (v > highest) {
                        highest = v;
                    }
                }
                //double diff = (highest - lowest) / 10;

                //((NumericAxis) chartPane.getChart().getYAxis()).setLowerBound(lowest - diff);
                //((NumericAxis) chartPane.getChart().getYAxis()).setUpperBound(highest + diff);
            }*/
            System.out.println("got " + fileData + ": " + fileName);
            for (MetricsData v : fileData.getValues()) {
                if (v instanceof MetricsDataSingle) {
                    map.get(fileName).getKey().add(new XYChart.Data<>(v.timestamp, (double) ((MetricsDataSingle) v).value));
                } else if (v instanceof MetricsDataLiebre) {
                    for (String s : ((MetricsDataLiebre) v).getFields()) {
                        map.get(s).getKey().add(new XYChart.Data<>(v.timestamp, ((MetricsDataLiebre) v).getValueFor(s)));
                    }
                }
                //System.out.println("add " + v.getKey() + ", " + Double.valueOf(v.getValue()));
            }
            ((NumericAxis) chartPane.getChart().getXAxis()).setUpperBound(System.currentTimeMillis() / 1000.0 + 1);
            ((NumericAxis) chartPane.getChart().getXAxis()).setLowerBound(start);
        });
    }

    private Collection<XYChart.Data<Number, Number>> toChartData(LiebreMetrics.FileData fileData, String name) {
        List<XYChart.Data<Number, Number>> list = new ArrayList<>();
        for (MetricsData v : fileData.getValues()) {
            if (v instanceof MetricsDataSingle) {
                list.add(new XYChart.Data<>(v.timestamp, (double) ((MetricsDataSingle) v).value));
            } else if (v instanceof MetricsDataLiebre) {
                list.add(new XYChart.Data<>(v.timestamp, ((MetricsDataLiebre) v).getValueFor(name)));
            }
            //System.out.println("add " + v.getKey() + ", " + Double.valueOf(v.getValue()));
        }
        return list;
    }
}
