package gui.controllers;

import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.visualisation.VisInfo;
import gui.spe.ParsedOperator;
import gui.spe.ParsedSPE;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MetricsController implements Initializable {
    @FXML
    private AnchorPane aPtab1;
    private Stage stage = null;

    private ParsedSPE parsedSPE;
    private List<Pair<Node<GraphOperator>, VisInfo>> visResult;

    public void init(@Nonnull ParsedSPE parsedSPE, @Nonnull List<Pair<Node<GraphOperator>, VisInfo>> visResult) {
        this.parsedSPE = parsedSPE;
        this.visResult = visResult;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * setting the stage of this view
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Closes the stage of this view
     */
    private void closeStage() {
        if (stage != null) {
            stage.close();
        }
    }
}
