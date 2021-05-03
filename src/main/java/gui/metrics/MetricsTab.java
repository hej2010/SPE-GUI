package gui.metrics;

import javafx.scene.layout.Pane;

import javax.annotation.Nonnull;

public interface MetricsTab {
    void onNewData(@Nonnull LiebreFileMetrics.FileData fileData);
    Pane getContent();
    String getName();
}
