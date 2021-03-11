package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.controllers.GUIController;
import org.example.controllers.MainController;
import org.example.utils.SPE;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * JavaFX App
 */
public class GUI extends Application {
    public static final String FXML_MAIN = "start_screen.fxml";
    public static final String FXML_GUI = "gui.fxml";

    // Docs: https://fxdocs.github.io/docs/html5/

    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;

        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(FXML_MAIN));
        Pane main = fxmlLoader.load();
        Scene scene = new Scene(main, 900, 600);

        MainController controller = fxmlLoader.getController();

        primaryStage.setScene(scene);
        primaryStage.show();

        controller.init(this);
    }

    public void changeScene(String fxml, @Nullable SPE selectedItem) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(fxml));
        Parent pane = fxmlLoader.load();

        primaryStage.getScene().setRoot(pane);
        if (fxml == FXML_GUI) {
            GUIController controller = fxmlLoader.getController();
            controller.init(this, selectedItem);
        } else {
            MainController controller = fxmlLoader.getController();
            controller.init(this);
        }
    }

}