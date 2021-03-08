package com.nullpointerexception.collabmode.controller;

import com.nullpointerexception.collabmode.application.Main;
import com.nullpointerexception.collabmode.model.User;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.IOException;

public class DashboardController {

    private final static int INTERVAL = 2 * 60; //2 minutes (120 seconds)
    private static String token = "";
    private static User currentUser = null;

@FXML Text testText;

    @FXML public void initialize(){
        checkTokenValidity();
        try {
        fetchUserDetails();
        } catch (IOException e) {
            e.printStackTrace();
        }
        testText.setText("ID: " + currentUser.getId() + "\n Name: " + currentUser.getFullName() + "\n Email: " + currentUser.getEmail());

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

}
