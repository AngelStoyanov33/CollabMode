package com.nullpointerexception.collabmode.application;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

    private static HostServicesDelegate hostServices;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
        BorderPane root = (BorderPane) loader.load();
        Scene scene = new Scene(root,900,725);
        scene.getStylesheets().add(String.valueOf(getClass().getClassLoader().getResource("css/application.css")));
        primaryStage.setScene(scene);
        primaryStage.setTitle("CollabMode v0.0.1-SNAPSHOT");
        primaryStage.show();

        hostServices = HostServicesFactory.getInstance(this);
    }

    public static HostServicesDelegate getHostService() {
        return hostServices;
    }


    public static void main(String[] args) {
        launch(args);
    }
}
