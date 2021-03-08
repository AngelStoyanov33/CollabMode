package com.nullpointerexception.collabmode.application;

import com.nullpointerexception.collabmode.controller.RegisterController;
import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static HostServicesDelegate hostServices;
    private static Stage currentStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
        BorderPane root = (BorderPane) loader.load();
        Scene scene = new Scene(root,900,725);
        scene.getStylesheets().add(String.valueOf(getClass().getClassLoader().getResource("css/application.css")));
        primaryStage.setScene(scene);
        primaryStage.setTitle("CollabMode v0.0.1-SNAPSHOT");
        primaryStage.show();
        primaryStage.getIcons().add(new Image("/assets/logo_wt.png"));
        currentStage = primaryStage;
        hostServices = HostServicesFactory.getInstance(this);
        RegisterController registerController = loader.getController();
        registerController.getSwitchToLogInButton().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    openLoginStage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static HostServicesDelegate getHostService() {
        return hostServices;
    }

    public static void openLoginStage() throws IOException {
        Stage loginStage = new Stage();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/Login.fxml"));
        BorderPane root = (BorderPane) loader.load();
        Scene scene = new Scene(root,900,725);
        scene.getStylesheets().add(String.valueOf(Main.class.getClassLoader().getResource("css/application.css")));
        loginStage.setScene(scene);
        loginStage.setTitle("CollabMode v0.0.1-SNAPSHOT");
        loginStage.show();
        loginStage.getIcons().add(new Image("/assets/logo_wt.png"));
        currentStage.close();
        currentStage = loginStage;
    }

    public static void openRegisterStage() throws IOException {
        Stage registerStage = new Stage();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/register.fxml"));
        BorderPane root = (BorderPane) loader.load();
        Scene scene = new Scene(root,900,725);
        scene.getStylesheets().add(String.valueOf(Main.class.getClassLoader().getResource("css/application.css")));
        registerStage.setScene(scene);
        registerStage.setTitle("CollabMode v0.0.1-SNAPSHOT");
        registerStage.show();
        registerStage.getIcons().add(new Image("/assets/logo_wt.png"));
        currentStage.close();
        currentStage = registerStage;
    }

    public static void openDashboardStage() throws IOException {
        Stage dashboardStage = new Stage();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
        BorderPane root = (BorderPane) loader.load();
        Scene scene = new Scene(root,900,725);
        scene.getStylesheets().add(String.valueOf(Main.class.getClassLoader().getResource("css/application.css")));
        dashboardStage.setScene(scene);
        dashboardStage.setTitle("CollabMode v0.0.1-SNAPSHOT");
        dashboardStage.show();
        dashboardStage.getIcons().add(new Image("/assets/logo_wt.png"));
        currentStage.close();
        currentStage = dashboardStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
