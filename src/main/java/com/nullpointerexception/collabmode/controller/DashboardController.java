package com.nullpointerexception.collabmode.controller;

import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTreeView;
import com.nullpointerexception.collabmode.application.Main;
import com.nullpointerexception.collabmode.model.User;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class DashboardController {

    private final static int INTERVAL = 2 * 60; //2 minutes (120 seconds)
    private static String token = "";
    private static User currentUser = null;

    @FXML private JFXTreeView<String> treeView;
    @FXML private MenuBar menuBar;
    @FXML private JFXTextArea textArea;

    @FXML private Menu collaborateMenu;
    @FXML private MenuItem addTeam;
    @FXML private MenuItem invite;
    @FXML private MenuItem join;

    @FXML public void initialize(){
        checkTokenValidity();
        try {
        fetchUserDetails();
        } catch (IOException e) {
            e.printStackTrace();
        }
        treeView.setRoot(getNodesForDirectory(new File("D:\\")));
        MenuItem entry1 = new MenuItem("Add item");
        MenuItem entry2 = new MenuItem("Delete");
        entry1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    File myObj = new File("D:\\");
                    if (myObj.createNewFile()) {
                        System.out.println("File created: " + myObj.getName());
                    } else {
                        System.out.println("File already exists.");
                    }
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            }
        });

        treeView.setContextMenu(new ContextMenu(entry1, entry2));

        addTeam.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Create a team");
                textInputDialog.getDialogPane().setContentText("Team name:");
                Optional<String> result = textInputDialog.showAndWait();
                TextField input = textInputDialog.getEditor();
                if(input.getText() != null || input.getText().length() == 0){
                    HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                    JSONObject json = new JSONObject();
                    json.put("token", token);
                    json.put("teamName", input.getText());
                    try {
                        String response =  httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/createTeam", json.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        invite.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Invite others to your team");
                textInputDialog.getDialogPane().setContentText("Team code:");
                TextField input = textInputDialog.getEditor();
                input.setEditable(false);
                input.setDisable(true);
                HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                JSONObject json = new JSONObject();
                json.put("token", token);
                try {
                    String response =  httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/sendInvite", json.toString());
                    json = new JSONObject(response);
                    System.out.println(json.get("teamCode").toString());
                    input.setText(json.get("teamCode").toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Optional<String> result = textInputDialog.showAndWait();

            }
        });

        join.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Join a team");
                textInputDialog.getDialogPane().setContentText("Enter team code:");
                Optional<String> result = textInputDialog.showAndWait();
                TextField input = textInputDialog.getEditor();
                HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                JSONObject json = new JSONObject();
                json.put("token", token);
                json.put("teamCode", input.getText());
                try {
                    String response =  httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/joinTeam", json.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void fetchUserDetails() throws IOException {
        HTTPRequestManager httpRequestManager = new HTTPRequestManager();
        JSONObject json = new JSONObject();
        json.put("token", token);
        String response =  httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/getUser", json.toString());
        json = new JSONObject(response);
        if(json.get("status").toString().equals("ok")) {
            currentUser = new User(Integer.parseInt(json.get("userID").toString()), json.get("userFullName").toString(), json.get("userEmail").toString());
        }

    }
    public static void setToken(String token){
        DashboardController.token = token;
    }
    private void checkTokenValidity(){
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(INTERVAL), ev -> {
            HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                JSONObject json = new JSONObject();
                json.put("token", token);
                try {
                    String response =  httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/checkTokenValidity", json.toString());
                    json = new JSONObject(response);
                    if(!json.get("status").toString().equals("ok")){
                        token = "";
                        currentUser = null;
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Registration error");
                        alert.setContentText(json.get("errorMessage").toString());
                        alert.showAndWait();
                        Main.openLoginStage();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }));
        timeline.play();


    }

    public TreeItem<String> getNodesForDirectory(File directory) {
        TreeItem<String> root = new TreeItem<String>(directory.getName());
        for(File f : directory.listFiles()) {
            if(f.isDirectory()) {
                root.getChildren().add(getNodesForDirectory(f));
            } else {
                root.getChildren().add(new TreeItem<String>(f.getName()));
            }
        }
        return root;
    }

}
