package org.yetyman.controls.treeview.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TreeViewTestApp extends Application {
    private Stage primaryStage;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var controller = new TreeViewTestController();
        controller.getStylesheets().add("styles.css");
        controller.setPrefSize(600, 600);
//        controller.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        controller.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        this.primaryStage = primaryStage;
        this.primaryStage.setScene(new Scene(controller));
        primaryStage.show();
    }

}
