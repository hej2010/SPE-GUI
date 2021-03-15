package gui.controllers;

import gui.spe.ParsedOperator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.ResourceBundle;

public class CodePopupController implements Initializable {
    @FXML
    private Label lblCodeBefore, lblCodeAfter;
    @FXML
    private TextArea tACode;
    @FXML
    private Button btnDone;
    private Stage stage = null;
    private String result = null;

    public void init(@Nonnull ParsedOperator.Definition definition) {
        lblCodeBefore.setText(definition.getCodeBefore(true));
        lblCodeAfter.setText(definition.getCodeAfter());
        tACode.setText(definition.getCodeMiddle(true));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnDone.setOnAction((event) -> {
            result = tACode.getText();
            closeStage();
        });
    }

    public String getResult() {
        return result;
    }

    /**
     * setting the stage of this view
     *
     * @param stage
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
