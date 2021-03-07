package com.nullpointerexception.collabmode.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import com.nullpointerexception.collabmode.util.EmailUtils;
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
        webEngine.load("http://localhost:8080/collabmode/portal/"); //TODO: Change when the Spring Boot app is made

        signInButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
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

                JSONObject json = new JSONObject();
                json.put("emailAddress", emailAddress);
                json.put("password", password);
                HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                try {
                    httpRequestManager.sendJSONRequest("http://192.168.0.107:8080/register",  json.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
