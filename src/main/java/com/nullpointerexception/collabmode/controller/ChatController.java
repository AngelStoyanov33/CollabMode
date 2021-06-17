package com.nullpointerexception.collabmode.controller;

import com.nullpointerexception.collabmode.model.User;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class ChatController {

    private @FXML WebView chatView;
    private static User userRef = null;

    @FXML public void initialize() {
        if(userRef != null) {
            WebEngine webEngine = chatView.getEngine();
            webEngine.load(HTTPRequestManager.SERVER_LOCATION + "/?name=" + userRef.getFullName() + "&topic=/topic/public");

        }

    }

    public static void setUser(User instance){
        ChatController.userRef = instance;
    }

}
