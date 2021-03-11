package se.chalmers.datx05.testing;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class CombosApp extends Application {

    private final ComboBox<Pair<String, String>> account = new ComboBox<>();

    private final static Pair<String, String> EMPTY_PAIR = new Pair<>("", "");

    @Override
    public void start(Stage primaryStage) throws Exception {

        Label accountsLabel = new Label("Account:");
        account.setPrefWidth(200);
        Button saveButton = new Button("Save");

        HBox hbox = new HBox(
                accountsLabel,
                account,
                saveButton);
        hbox.setSpacing(10.0d);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(40));

        Scene scene = new Scene(hbox);

        initCombo();

        saveButton.setOnAction((evt) -> {
            if (account.getValue().equals(EMPTY_PAIR)) {
                System.out.println("no save needed; no item selected");
            } else {
                System.out.println("saving " + account.getValue());
            }
        });

        primaryStage.setTitle("CombosApp");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initCombo() {

        List<Pair<String, String>> accounts = new ArrayList<>();

        accounts.add(new Pair<>("Auto Expense", "60000"));
        accounts.add(new Pair<>("Interest Expense", "61000"));
        accounts.add(new Pair<>("Office Expense", "62000"));
        accounts.add(new Pair<>("Salaries Expense", "63000"));

        account.getItems().add(EMPTY_PAIR);
        account.getItems().addAll(accounts);
        account.setValue(EMPTY_PAIR);

        Callback<ListView<Pair<String, String>>, ListCell<Pair<String, String>>> factory = (lv) -> new ListCell<>() {
            @Override
            protected void updateItem(Pair<String, String> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                } else {
                    setText(item.getKey());
                }
            }
        };

        account.setCellFactory(factory);
        account.setButtonCell(factory.call(null));
    }

    public static void main(String[] args) {
        launch(args);
    }

}
