package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class GUI extends Application {

    // Docs: https://fxdocs.github.io/docs/html5/

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource("gui.fxml"));
        VBox main = fxmlLoader.load();
        Scene scene = new Scene(main, 900, 600);

        GUIController controller = fxmlLoader.getController();

        stage.setScene(scene);
        stage.show();

        controller.init(main, scene);
    }

}