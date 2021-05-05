package gui.metrics;

import gui.utils.Time;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class MetricsTab {

    public abstract void onNewData(@Nonnull LiebreMetrics.FileData fileData);

    public abstract Pane getContent();

    public abstract String getName();
}
