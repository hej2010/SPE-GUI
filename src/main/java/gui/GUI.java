package gui;

import gui.controllers.GUIController;
import gui.controllers.MainController;
import gui.spe.ParsedSPE;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * JavaFX App
 */
public class GUI extends Application {
    public static final String FXML_MAIN = "/start_screen.fxml";
    public static final String FXML_GUI = "/gui-vis.fxml";
    public static final String FXML_METRICS_GRAPHITE = "/metrics.fxml";
    public static final String FXML_METRICS_LIEBRE = "/metrics2.fxml";
    public static final String FXML_METRICS_CONTENT = "/metrics2-content.fxml";
    public static final String FXML_METRICS_CONTENT2 = "/metrics2-content2.fxml";
    public static final boolean DEBUG = true;

    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("SPE-GUI");
        //primaryStage.getIcons().add() // TODO icon

        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(FXML_MAIN));
        Pane main = fxmlLoader.load();
        Scene scene = new Scene(main, 900, 600);
        MainController controller = fxmlLoader.getController();
        if (DEBUG) {
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.SHIFT) {
                    System.err.println("--------- SHIFT PRESSED --------");
                }
            });
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        controller.init(this);
    }

    public void changeScene(String fxml, @Nullable ParsedSPE selectedItem) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource(fxml));
        Parent pane = fxmlLoader.load();

        primaryStage.getScene().setRoot(pane);
        if (fxml.equals(FXML_GUI)) {
            GUIController controller = fxmlLoader.getController();
            assert selectedItem != null;
            controller.init(this, selectedItem);
        } else {
            MainController controller = fxmlLoader.getController();
            controller.init(this);
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

}