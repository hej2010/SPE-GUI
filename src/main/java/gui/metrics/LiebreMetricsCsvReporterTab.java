package gui.metrics;

import gui.GUI;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class LiebreMetricsCsvReporterTab extends MetricsTab {
    private final String name;
    private final Pane root;
    private final Map<String, MetricsTabData> mapToData;

    public LiebreMetricsCsvReporterTab(String name, List<String> seriesNames) throws IOException {
        this.name = name;
        this.mapToData = new HashMap<>();

        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_CONTENT2));
        root = fxmlLoader.load();

        TabPane tabPane = (TabPane) fxmlLoader.getNamespace().get("tabPane");

        for (String s : seriesNames) {
            List<String> seriesNames2 = new ArrayList<>(1);
            seriesNames2.addAll(getSeries());
            MetricsTabData data = new MetricsTabData(setupChartPane(s), seriesNames2);
            mapToData.put(s, data);

            Tab t = new Tab(s);
            fxmlLoader = new FXMLLoader(GUI.class.getResource(GUI.FXML_METRICS_CONTENT));
            AnchorPane anchorPane = fxmlLoader.load();
            data.init(fxmlLoader.getNamespace());

            t.setContent(anchorPane);
            tabPane.getTabs().add(t);
        }
    }

    private List<String> getSeries() {
        return Arrays.asList(LiebreMetrics.CSV_NAMES);
    }

    public void onNewData(@Nonnull LiebreMetrics.FileData fileData) {
        String name = fileData.getFileName().split("\\.", 2)[0];
        MetricsTabData data = mapToData.get(name);
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

    @Override
    public void stop() {
        for (MetricsTabData s : mapToData.values()) {
            s.stop();
        }
    }
}
