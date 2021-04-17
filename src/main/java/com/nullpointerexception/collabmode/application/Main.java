package com.nullpointerexception.collabmode.application;

import com.nullpointerexception.collabmode.controller.DashboardController;
import com.nullpointerexception.collabmode.controller.RegisterController;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import com.nullpointerexception.collabmode.service.Serializer;
import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends Application {

    private static HostServicesDelegate hostServices;
    private static Stage currentStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        hostServices = HostServicesFactory.getInstance(this);
        String dataFolder = System.getenv("APPDATA");
        Files.createDirectories(Paths.get(dataFolder + "\\CollabMode"));
        File authFile = new File(dataFolder + "\\CollabMode\\auth.ser");
        if(authFile.exists()){
            if(authFile.isFile()){
                Serializer serializer = new Serializer();
                String token = serializer.deserializeToken();
                HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                JSONObject json = new JSONObject();
                json.put("token", token);
                try {
                    String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/checkTokenValidity", json.toString());
                    json = new JSONObject(response);
                    if (json.get("status").toString().equals("ok")) {
                        Main.openDashboardStage(token);
                    }else{
                        Main.openRegisterStage();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }else{
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
////            BorderPane root = (BorderPane) loader.load();
////            Scene scene = new Scene(root,900,725);
////            scene.getStylesheets().add(String.valueOf(getClass().getClassLoader().getResource("css/application.css")));
////            primaryStage.setScene(scene);
////            primaryStage.setTitle("CollabMode v0.0.1-SNAPSHOT");
////            primaryStage.show();
////
////            primaryStage.getIcons().add(new Image("/assets/logo_wt.png"));
////            currentStage = primaryStage;
            Main.openRegisterStage();
        }
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
        if(currentStage != null) {
            currentStage.close();
        }
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
        if(currentStage != null) {
            currentStage.close();
        }
        currentStage = registerStage;
    }

    public static void openDashboardStage(String token) throws IOException {
        Stage dashboardStage = new Stage();
        DashboardController.setToken(token);
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
        BorderPane root = (BorderPane) loader.load();
        Scene scene = new Scene(root,900,725);
        scene.getStylesheets().add(String.valueOf(Main.class.getClassLoader().getResource("css/application.css")));
        scene.getStylesheets().add(String.valueOf(Main.class.getClassLoader().getResource("css/java-keywords.css")));
        DashboardController dashboardController = (DashboardController) loader.getController();
        dashboardStage.setScene(scene);
        dashboardStage.setTitle("CollabMode v0.0.1-SNAPSHOT");
        dashboardStage.show();
        dashboardStage.getIcons().add(new Image("/assets/logo_wt.png"));
        if(currentStage != null) {
            currentStage.close();
        }
        currentStage = dashboardStage;
    }

    public static Stage getCurrentStage() {
        return currentStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
