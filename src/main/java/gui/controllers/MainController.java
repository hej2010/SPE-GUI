package gui.controllers;

import gui.GUI;
import gui.spe.SPEParser;
import gui.utils.SPE;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class MainController {

    @FXML
    public Button btnStart;
    @FXML
    public ChoiceBox<SPE> cBChoose;

    public void init(@NotNull GUI gui) {
        List<SPE> engines = SPE.asList();
        cBChoose.setItems(FXCollections.observableArrayList(engines));
        btnStart.setOnAction(event -> {
            SPE selected = cBChoose.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    gui.changeScene(GUI.FXML_GUI, SPEParser.parseSPE(selected));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
