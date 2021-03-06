package com.nullpointerexception.collabmode.controller;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import com.nullpointerexception.collabmode.application.Main;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import com.nullpointerexception.collabmode.util.EmailUtils;
import com.nullpointerexception.collabmode.util.PasswordUtils;
import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class RegisterController {
    // Matching variables
    @FXML private JFXTextField fullNameInput;
    @FXML private JFXTextField emailInput;
    @FXML private JFXPasswordField passwordInput;
    @FXML private JFXPasswordField passwordConfirmInput;
    @FXML private JFXCheckBox tosAgree;
    @FXML private JFXCheckBox newsletterAgree;
    @FXML private Button signUpButton;
    @FXML private WebView imageSlideShow;
    @FXML private Hyperlink tosHyperlink;
    @FXML private Hyperlink newsletterHyperlink;

    @FXML private Button switchToLogInButton;

    // NewsletterStatus set to false (default)
    private boolean newsletterStatus = false;

    @FXML public void initialize(){
        // Load the WebView for the image slideshow
        WebEngine webEngine = imageSlideShow.getEngine();
        webEngine.load("http://192.168.0.107:8080/imageSwitcher"); //TODO: Change when the Spring Boot app is made

        tosHyperlink.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Main.getHostService().showDocument("http://localhost:8080/collabmode/tos/"); //TODO: Change when the Spring Boot app is made
            }
        });

        newsletterHyperlink.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Main.getHostService().showDocument("http://localhost:8080/collabmode/newsletter/"); //TODO: Change when the Spring Boot app is made
            }
        });

        // Data validation
        signUpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String fullName = fullNameInput.getText().trim();
                String email = emailInput.getText().trim();
                String password = passwordInput.getText().trim();
                String passwordConfirm = passwordConfirmInput.getText().trim();

                if(fullName.isEmpty()){
                    fullNameInput.setFocusColor(Color.RED);
                    fullNameInput.setUnFocusColor(Color.RED);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Name error");
                    alert.setContentText("Full name field can't be empty!");
                    alert.showAndWait();
                    return;
                }
                if(!EmailUtils.isValid(email)){
                    emailInput.setFocusColor(Color.RED);
                    emailInput.setUnFocusColor(Color.RED);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Email address error");
                    alert.setContentText("Invalid email address!");
                    alert.showAndWait();
                    return;
                }
                List<String> errors = new ArrayList<String>();
                if(!PasswordUtils.isSecure(password, passwordConfirm, errors)) {
                    passwordInput.setUnFocusColor(Color.RED);
                    passwordConfirmInput.setUnFocusColor(Color.RED);
                    passwordInput.setFocusColor(Color.RED);
                    passwordConfirmInput.setFocusColor(Color.RED);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Password error");
                    alert.setContentText(errors.get(0));
                    alert.showAndWait();
                    return;
                }
                if(!tosAgree.isSelected()){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Terms of service agreement error");
                    alert.setContentText("You must accept CollabMode™'s  TOS before registering!");
                    alert.showAndWait();
                    return;
                }
                if(newsletterAgree.isSelected()){
                    newsletterStatus = true;
                }

            }
        });

    }
    public Button getSwitchToLogInButton() {
        return switchToLogInButton;
    }
}
