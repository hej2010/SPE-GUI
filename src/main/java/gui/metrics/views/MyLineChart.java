package gui.metrics.views;

import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;

import java.util.ArrayList;
import java.util.List;

public class MyLineChart<X, Y> extends LineChart<X, Y> {

    public MyLineChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    public MyLineChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
    }

    /**
     * Sets the height/width of axis, not considering hidden points/series
     */
    @Override
    protected void updateAxisRange() { // https://stackoverflow.com/a/48659295/7232269
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        List<X> xData = null;
        List<Y> yData = null;
        if (xa.isAutoRanging()) xData = new ArrayList<X>();
        if (ya.isAutoRanging()) yData = new ArrayList<Y>();
        if (xData != null || yData != null) {
            for (Series<X, Y> series : getData()) {
                if (series.getNode().isVisible()) { // consider only visible series
                    for (Data<X, Y> data : series.getData()) {
                        if (xData != null) xData.add(data.getXValue());
                        if (yData != null) yData.add(data.getYValue());
                    }
                }
            }
            // RT-32838 No need to invalidate range if there is one data item - whose value is zero.
            if (xData != null && !(xData.size() == 1 && getXAxis().toNumericValue(xData.get(0)) == 0)) {
                xa.invalidateRange(xData);
            }
            if (yData != null && !(yData.size() == 1 && getYAxis().toNumericValue(yData.get(0)) == 0)) {
                ya.invalidateRange(yData);
            }

        }
    }
}
