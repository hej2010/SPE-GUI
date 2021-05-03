package gui.metrics;

import javafx.scene.layout.Pane;

import javax.annotation.Nonnull;

public interface IMetricsTab {
    void onNewData(@Nonnull LiebreMetrics.FileData fileData);
    Pane getContent();
    String getName();
}
