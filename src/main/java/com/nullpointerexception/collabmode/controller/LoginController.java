package com.nullpointerexception.collabmode.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import com.nullpointerexception.collabmode.application.Main;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import com.nullpointerexception.collabmode.service.Serializer;
import com.nullpointerexception.collabmode.util.EmailUtils;
import com.nullpointerexception.collabmode.util.PasswordUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.json.JSONObject;

import java.io.IOException;

public class LoginController {
    @FXML private JFXTextField emailAddressInput;
    @FXML private JFXPasswordField passwordPassField;
    @FXML private JFXButton forgotPasswordButton;
    @FXML private JFXButton signInButton;
    @FXML private JFXButton switchToRegisterButton;
    @FXML private WebView imageSlideShow;

    @FXML public void initialize(){
        // Load the WebView for the image slideshow
        WebEngine webEngine = imageSlideShow.getEngine();
        webEngine.load(HTTPRequestManager.SERVER_LOCATION + "/imageSwitcher");

        switchToRegisterButton.setOnAction(event -> {
            try {
                Main.openRegisterStage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        signInButton.setOnAction(event -> {
            String emailAddress = emailAddressInput.getText().trim();
            String password = passwordPassField.getText().trim();

            if(!EmailUtils.isValid(emailAddress)){
                emailAddressInput.setFocusColor(Color.RED);
                emailAddressInput.setUnFocusColor(Color.RED);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Email address error");
                alert.setContentText("Invalid email address!");
                alert.showAndWait();
                return;
            }

            password = PasswordUtils.hash(password);
            JSONObject json = new JSONObject();
            json.put("emailAddress", emailAddress);
            json.put("password", password);
            HTTPRequestManager httpRequestManager = new HTTPRequestManager();
            try {
                String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/login",  json.toString());
                JSONObject responseToJson = new JSONObject(response);
                if(!responseToJson.get("status").toString().equals("ok")){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Login error");
                    alert.setContentText(responseToJson.get("errorMessage").toString());
                    alert.showAndWait();
                    return;
                }else{
                    Serializer serializer = new Serializer();
                    serializer.serializeToken(responseToJson.get("token").toString());
                    Main.openDashboardStage(responseToJson.get("token").toString(), "Java");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
