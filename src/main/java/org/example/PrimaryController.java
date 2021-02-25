package org.example;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class PrimaryController {

    public Button primaryButton;

    @FXML
    private void switchToSecondary() throws IOException {
        GUI.setRoot("secondary");
    }
}
