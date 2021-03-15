package com.nullpointerexception.collabmode.controller;


import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTreeView;
import com.nullpointerexception.collabmode.application.Main;
import com.nullpointerexception.collabmode.model.User;
import com.nullpointerexception.collabmode.service.FTPManager;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.apache.commons.net.ftp.FTPFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    private final static int INTERVAL = 2 * 60; //2 minutes (120 seconds)
    private static String token = "";
    private static User currentUser = null;
    private static FTPManager ftpManager;

    @FXML private JFXTreeView<String> treeView;
    @FXML private MenuBar menuBar;
    @FXML private JFXTextArea textArea;

    @FXML private Menu collaborateMenu;
    @FXML private MenuItem addTeam;
    @FXML private Menu myTeamMenu;

    @FXML private MenuItem invite;
    @FXML private MenuItem join;

    @FXML
    public void initialize(){
        checkTokenValidity();
        try {
            fetchUserDetails();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ftpManager = new FTPManager("localhost", 21, "TestUser7", "");
        FTPFile[] files = ftpManager.getFiles();

        TreeItem<String> item = new TreeItem<String>("Test");
        treeView.setRoot(item);

        for (FTPFile f : files) {
            if (f.isDirectory()) {
                item.getChildren().add(getNodesForDirectory(f));
            } else {
                item.getChildren().add(new TreeItem<String>(f.getName()));
            }
        }

        if(currentUser.getTeamID() != 0 && currentUser.isTeamOwner()) {
            MenuItem transferOwnerItem = new MenuItem();
            transferOwnerItem.setText("Transfer ownership");
            transferOwnerItem.setOnAction(event -> {
                List<String> choices = new ArrayList<>();
                try {
                    ArrayList<User> users = fetchUsersByTeamID(currentUser.getTeamID());
                    if(users == null){
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Transfer ownership error");
                        alert.setContentText("You're not currently in team!");
                        alert.showAndWait();
                        return;
                    }
                    for(User user : users){
                        if(user.getId() != currentUser.getId()) {
                            choices.add(user.getFullName());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
                dialog.setTitle("Transfer the team ownership");
                dialog.setHeaderText("Transfer the team ownership");
                dialog.setContentText("Choose your new team leader:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()){
                    System.out.println("Your choice: " + result.get()); //TODO: Send transfer ownership request
                }
            });
            myTeamMenu.getItems().add(transferOwnerItem);
        }


        MenuItem entry1 = new MenuItem("Add item"); //Currently not working
        MenuItem entry2 = new MenuItem("Delete"); //Currently not working
        MenuItem entry3 = new MenuItem("New folder");

        entry3.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setTitle("Create a directory");
            textInputDialog.getDialogPane().setContentText("Directory name:");
            Optional<String> result = textInputDialog.showAndWait();
            TextField input = textInputDialog.getEditor();
            if (input.getText() != null || input.getText().length() == 0) {
                String path = getPathOfItem();
                System.out.println(path + "/" + input.getText());
                ftpManager.addNewDirectory(path + "/" + input.getText()); //TODO: Check if error
            }
        });

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

        treeView.setContextMenu(new ContextMenu(entry1, entry2, entry3));

        addTeam.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Create a team");
                textInputDialog.getDialogPane().setContentText("Team name:");
                Optional<String> result = textInputDialog.showAndWait();
                TextField input = textInputDialog.getEditor();
                if (input.getText() != null || input.getText().length() == 0) {
                    HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                    JSONObject json = new JSONObject();
                    json.put("token", token);
                    json.put("teamName", input.getText());
                    try {
                        String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/createTeam", json.toString());
                        json = new JSONObject(response);
                        if(!json.get("status").toString().equals("ok")){
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Add team error");
                            alert.setContentText(json.get("errorMessage").toString());
                            alert.showAndWait();
                            return;
                        }
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
                    String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/sendInvite", json.toString());
                    json = new JSONObject(response);
                    if(!json.get("status").toString().equals("ok")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invite others error");
                        alert.setContentText(json.get("errorMessage").toString());
                        alert.showAndWait();
                        return;
                    }
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
                    String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/joinTeam", json.toString());
                    json = new JSONObject(response);
                    if(!json.get("status").toString().equals("ok")){
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Join team error");
                        alert.setContentText(json.get("errorMessage").toString());
                        alert.showAndWait();
                        return;
                    }
                    fetchUserDetails();
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
        String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/getUser", json.toString());
        json = new JSONObject(response);
        if (json.get("status").toString().equals("ok")) {
            currentUser = new User(Integer.parseInt(json.get("userID").toString()), json.get("userFullName").toString(), json.get("userEmail").toString(),
                    Integer.parseInt(json.get("userTeamID").toString()), Boolean.parseBoolean(json.get("userIsTeamOwner").toString()));
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Еrror");
            alert.setContentText(json.get("errorMessage").toString());
            alert.showAndWait();
            return;

        }
    }

    private ArrayList<User> fetchUsersByTeamID(int teamID) throws IOException{
        if(teamID != 0) {
            ArrayList<User> users = new ArrayList<>();
            HTTPRequestManager httpRequestManager = new HTTPRequestManager();
            JSONObject json = new JSONObject();
            json.put("token", token);
            json.put("teamID", teamID);
            String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/getUsersFromTeam", json.toString());
            json = new JSONObject(response);
            if (json.get("status").toString().equals("ok")) {
                JSONArray jsonArray = json.getJSONArray("users");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject itr = jsonArray.getJSONObject(i);
                    users.add(new User(Integer.parseInt(itr.get("id").toString()), itr.get("fullName").toString(), itr.get("email").toString(),
                            Integer.parseInt(itr.get("teamID").toString()), Boolean.parseBoolean(itr.get("isOwner").toString())));
                }
            }
            return users;
        }else{
            return null;
        }
    }

    public static void setToken(String token) {
        DashboardController.token = token;
    }

    private void checkTokenValidity() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(INTERVAL), ev -> {
            HTTPRequestManager httpRequestManager = new HTTPRequestManager();
            JSONObject json = new JSONObject();
            json.put("token", token);
            try {
                String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/checkTokenValidity", json.toString());
                json = new JSONObject(response);
                if (!json.get("status").toString().equals("ok")) {
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


    public TreeItem<String> getNodesForDirectory(FTPFile dir){
        TreeItem<String> root = new TreeItem<>(dir.getName());
        for (FTPFile f : ftpManager.getFilesByPath(dir.getName())) {
            if (f.isDirectory()) {
                root.getChildren().add(getNodesForDirectory(f));
            } else {
                root.getChildren().add(new TreeItem<String>(f.getName()));
            }
        }
        return root;
    }

    public String getPathOfItem(){
        StringBuilder pathBuilder = new StringBuilder();
        for (TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
             item != null ; item = item.getParent()) {

            pathBuilder.insert(0, item.getValue());
            pathBuilder.insert(0, "/");
        }
        String path = pathBuilder.toString();
        path = path.replace("/Test", "");
        return path;
    }

}
