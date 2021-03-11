package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import org.example.GUI;
import org.example.utils.SPE;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
                    gui.changeScene(GUI.FXML_GUI, selected);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
