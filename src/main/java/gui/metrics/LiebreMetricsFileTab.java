package gui.metrics;

import cern.extjfx.chart.NumericAxis;
import cern.extjfx.chart.XYChartPane;
import cern.extjfx.chart.plugins.CrosshairIndicator;
import cern.extjfx.chart.plugins.DataPointTooltip;
import gui.GUI;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class LiebreMetricsFileTab extends MetricsTab {
    private final String name;
    private final Pane root;
    private final MetricsTabData data;

    public LiebreMetricsFileTab(String name, List<String> seriesNames) throws IOException {
        this.name = name;
        data = new MetricsTabData(setupChartPane(name), seriesNames);
        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_CONTENT));
        root = fxmlLoader.load();

        data.init(fxmlLoader.getNamespace());
    }

    public void onNewData(@Nonnull LiebreMetrics.FileData fileData) {
        String name = fileData.getFileName().split("\\.", 2)[0];
        if (data != null) {
            data.onNewData(fileData, name);
        }
    }

    public Pane getContent() {
        return root;
    }

    public String getName() {
        return name;
    }
}
