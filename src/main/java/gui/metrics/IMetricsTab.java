package gui.metrics;

import javafx.scene.layout.Pane;

import javax.annotation.Nonnull;

public interface IMetricsTab {

    public abstract void onNewData(@Nonnull LiebreMetrics.FileData fileData);

    public abstract Pane getContent();

    public abstract String getName();
}
