package gui.metrics.graphite;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RenderDatapoint {
    private final int timestamp;
    private final double value;

    public RenderDatapoint(int timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }

    public static List<RenderDatapoint> fromJson(JSONArray array) {
        List<RenderDatapoint> list = new LinkedList<>();
        if (array != null && !array.isEmpty()) {
            for (Object o : array) {
                JSONArray o1 = (JSONArray) o;
                if (!o1.isNull(0)) {
                    BigDecimal value = (BigDecimal) o1.get(0);
                    int time = (int) o1.get(1);
                    list.add(new RenderDatapoint(time, value.doubleValue()));
                }
            }
        }
        return list;
    }

    public static ObservableList<XYChart.Data<Number, Number>> toChartData(List<RenderDatapoint> datapoints) {
        List<XYChart.Data<Number, Number>> list = new ArrayList<>();
        for (RenderDatapoint d : datapoints) {
            list.add(new XYChart.Data<>(d.getTimestamp(), d.getValue()));
        }
        return FXCollections.observableArrayList(list);
    }
}
