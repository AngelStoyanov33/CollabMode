package com.nullpointerexception.collabmode.controller;

import com.jfoenix.controls.JFXTreeView;
import com.nullpointerexception.collabmode.application.Main;
import com.nullpointerexception.collabmode.model.User;
import com.nullpointerexception.collabmode.service.FTPManager;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import com.nullpointerexception.collabmode.service.MQTTManager;
import com.nullpointerexception.collabmode.util.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.json.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reactfx.collection.ListModification;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DashboardController {

    private static String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static String PAREN_PATTERN = "\\(|\\)";
    private static String BRACE_PATTERN = "\\{|\\}";
    private static String BRACKET_PATTERN = "\\[|\\]";
    private static String SEMICOLON_PATTERN = "\\;";
    private static String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    private ExecutorService executor;

    private final static int INTERVAL = 2 * 60; //2 minutes (120 seconds)
    private static String token = "";
    private static User currentUser = null;
    private static FTPManager ftpManager;
    private static File currentFile = null;
    private static String currentFileLocationOnFTP = "";
    private static String mode = "Java";

    private static Stage stage;

    @FXML private JFXTreeView<String> treeView;
    @FXML private MenuBar menuBar;
    @FXML private CodeArea codeArea;

    @FXML private AnchorPane anchorPaneArea;

    @FXML private ChoiceBox<String> choiceBox;
    @FXML private TabPane tabPane;
    @FXML private Menu collaborateMenu;
    @FXML private MenuItem addTeam;
    @FXML private Menu myTeamMenu;
    @FXML private MenuItem createProjectItem;

    @FXML private MenuItem invite;
    @FXML private MenuItem join;

    @FXML
    public void initialize(){
        Logger logger = new Logger();

        checkTokenValidity();
        try {
            fetchUserDetails();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(currentUser.getTeamID() != 0) {
            HTTPRequestManager httpRequestManager = new HTTPRequestManager();
            JSONObject json = new JSONObject();
            json.put("token", token);
            try {
                String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/getTeamName", json.toString());
                json = new JSONObject(response);
            }catch (IOException e){
                e.printStackTrace();
            }
            ftpManager = new FTPManager(FTPManager.FTP_SERVER_ADDRESS, 21, json.get("teamName").toString(), "");
            loadFTPTree();

            MenuItem leaveTeamItem = new MenuItem();
            leaveTeamItem.setText("Leave");
            leaveTeamItem.setOnAction(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "");
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.getDialogPane().setContentText("Are you sure you want to leave your team?");
                alert.getDialogPane().setHeaderText("Hold up!");
                Optional<ButtonType> alertResult = alert.showAndWait();
                if (ButtonType.OK.equals(alertResult.get())) {
                    JSONObject secondJson = new JSONObject();
                    secondJson.put("token", token);
                    try{
                        String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/leave", secondJson.toString());
                        secondJson = new JSONObject(response);
                        if(!secondJson.get("status").toString().equals("ok")){
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Leave error");
                            errorAlert.setContentText(secondJson.get("errorMessage").toString());
                            errorAlert.showAndWait();
                            return;
                        }
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }else if(ButtonType.CANCEL.equals(alertResult.get())){
                    return;
                }
            });
            myTeamMenu.getItems().add(leaveTeamItem);

        }
        new Thread(() -> {
            MQTTManager mqttManager = new MQTTManager("developmentID");
            try {
                mqttManager.subscribe("test/topic");
                mqttManager.publish("test/topic", "hellow");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }).start();

        loadHighlight(mode);

        if(currentUser.getTeamID() != 0 && currentUser.isTeamOwner()) {
            MenuItem transferOwnerItem = new MenuItem();
            MenuItem kickTeammates = new MenuItem();
            transferOwnerItem.setText("Transfer ownership");
            transferOwnerItem.setOnAction(event -> {
                ArrayList<User> users = null;
                List<String> choices = new ArrayList<>();
                try {
                    users = fetchUsersByTeamID(currentUser.getTeamID());
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
                    try {
                        System.out.println("Your choice: " + result.get());
                        HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                        JSONObject json = new JSONObject();
                        int newOwnerID = 0;
                        json.put("token", token);
                        for(User user : users){
                            if(user.getFullName().equals(result.get())) {
                                newOwnerID = user.getId();
                                break;
                            }
                        }
                        json.put("newOwnerID", newOwnerID);
                        String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/transferTeamOwnerShip", json.toString());
                        json = new JSONObject(response);
                        if (!json.get("status").toString().equals("ok")) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Transfer ownership error");
                            alert.setContentText(json.get("errorMessage").toString());
                            alert.showAndWait();
                            return;
                        }
                        fetchUserDetails();
                        Main.openDashboardStage(token, "Java");
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            });

            kickTeammates.setText("Kick");
            kickTeammates.setOnAction(event -> {
                ArrayList<User> users = null;
                List<String> choices = new ArrayList<>();
                try {
                    users = fetchUsersByTeamID(currentUser.getTeamID());
                    if(users == null){
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Kick error");
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
                dialog.setTitle("Kick a teammate");
                dialog.setHeaderText("Kick a team member from the team");
                dialog.setContentText("Kick:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()){
                    try {
                        System.out.println("Your choice: " + result.get());
                        HTTPRequestManager httpRequestManager = new HTTPRequestManager();
                        JSONObject json = new JSONObject();
                        int kickedMemberID = 0;
                        json.put("token", token);
                        for(User user : users){
                            if(user.getFullName().equals(result.get())) {
                                kickedMemberID = user.getId();
                                break;
                            }
                        }
                        json.put("kickedMemberID", kickedMemberID);
                        String response = httpRequestManager.sendJSONRequest(HTTPRequestManager.SERVER_LOCATION + "/kick", json.toString());
                        json = new JSONObject(response);
                        if (!json.get("status").toString().equals("ok")) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Kick error");
                            alert.setContentText(json.get("errorMessage").toString());
                            alert.showAndWait();
                            return;
                        }
                        fetchUserDetails();
                        Main.openDashboardStage(token, mode);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            });


            myTeamMenu.getItems().add(transferOwnerItem);
            myTeamMenu.getItems().add(kickTeammates);
        }
        MenuItem logout = new MenuItem();
        logout.setText("Logout");
        collaborateMenu.getItems().add(logout);
        logout.setOnAction(event -> {
            String dataFolder = System.getenv("APPDATA");
            File authFile = new File(dataFolder + "\\CollabMode\\auth.ser");
            if(authFile.exists()){
                if(authFile.isFile()){
                    if(!authFile.delete()){
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Logout error");
                        alert.setContentText("Could not logout, try again later");
                        alert.showAndWait();
                    }
                }
            }
            DashboardController.setToken("");
            try {
                Main.openLoginStage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



        VirtualizedScrollPane sp = new VirtualizedScrollPane(codeArea);
        anchorPaneArea.getChildren().add(sp);

        anchorPaneArea.setLeftAnchor(sp, 0.0);
        anchorPaneArea.setRightAnchor(sp, 0.0);
        anchorPaneArea.setBottomAnchor(sp, 0.0);
        anchorPaneArea.setTopAnchor(sp, 0.0);


        codeArea.richChanges()
                .filter(change -> !change.isIdentity())
                .successionEnds(java.time.Duration.ofMillis(500))
                .subscribe(ignore -> {
                    if(tabPane.getTabs().size() != 0) {
                        System.out.println("CHANGE DETECTED");

                    }
                });



        choiceBox.getItems().add("Java");
        choiceBox.getItems().add("C++");
        choiceBox.setValue(mode);
        choiceBox.getSelectionModel()
                .selectedItemProperty()
                .addListener( (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    System.out.println(newValue);
                    if(newValue.equals("Java")){
                        setMode(newValue);
                        loadHighlight("Java");
                        String codeAreaContentCopy = codeArea.getText();
                        codeArea.deleteText(0, codeArea.getLength());
                        codeArea.appendText(codeAreaContentCopy);
                    }else if(newValue.equals("C++")){
                        setMode(newValue);
                        loadHighlight("C++");
                        String codeAreaContentCopy = codeArea.getText();
                        codeArea.deleteText(0, codeArea.getLength());
                        codeArea.appendText(codeAreaContentCopy);

                    }
                });

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setContextMenu( new DefaultContextMenu() );
        codeArea.getVisibleParagraphs().addModificationObserver
                (
                        new VisibleParagraphStyler<>( codeArea, this::computeHighlighting )
                );

        // auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile( "^\\s+" );
        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, KE ->
        {
            if ( KE.getCode() == KeyCode.ENTER ) {
                int caretPosition = codeArea.getCaretPosition();
                int currentParagraph = codeArea.getCurrentParagraph();
                Matcher m0 = whiteSpace.matcher( codeArea.getParagraph( currentParagraph-1 ).getSegments().get( 0 ) );
                if ( m0.find() ) Platform.runLater( () -> codeArea.insertText( caretPosition, m0.group() ) );
            }
        });



        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
            codeArea.replaceText(0, 0, codeArea.getText());
        }));
        timeline.play();


        createProjectItem.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setTitle("Create a project");
            textInputDialog.getDialogPane().setContentText("Enter project name:");
            Optional<String> result = textInputDialog.showAndWait();
            TextField input = textInputDialog.getEditor();
            if (input.getText() != null || input.getText().length() == 0) {
                String path = getPathOfItem();
                System.out.println(path + "/" + input.getText());
                if (!ftpManager.addNewDirectory(path + "/" + input.getText())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Create project error");
                    alert.setContentText("Couldn't create the project. Try again later.");
                    alert.showAndWait();
                    return;
                }
                loadFTPTree();
            }
        });


        MenuItem addItem = new MenuItem("Add item");
        MenuItem deleteItem = new MenuItem("Delete");
        MenuItem addNewDirectory = new MenuItem("New folder");
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem refresh = new MenuItem("Refresh");
        MenuItem load = new MenuItem("Load file");

        refresh.setOnAction(event -> {
            loadFTPTree();
        });

        deleteItem.setOnAction(event -> {
            String path = getPathOfItem();
            System.out.println(path);
            ftpManager.deleteFile(path);
            loadFTPTree();
        });

        load.setOnAction(event -> {
            Tab tab = new Tab();
            tab.setText(getPathOfItem());
            tab.setClosable(true);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tabPane.getTabs().size() - 1);
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
            //TODO: Listener onclick tab (on select)

            String path = getPathOfItem();
            System.out.println(path);
            ftpManager.downloadFile(path);
            String tempFolder = System.getProperty("java.io.tmpdir");
            Path tempFolderPath = Paths.get(tempFolder + "\\.collabmode");
            File downloadedFile = new File(tempFolderPath.toString() + "\\" + Paths.get(path).getFileName());
            FileInputStream inputStream = null;
            Scanner sc = null;
            currentFile = downloadedFile;
            currentFileLocationOnFTP = path;
            try {
                inputStream = new FileInputStream(downloadedFile);
                sc = new Scanner(inputStream, "UTF-8");
                codeArea.deleteText(0, codeArea.getLength());
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    codeArea.appendText(line);
                    codeArea.appendText("\n");
                }
                if (sc.ioException() != null) {
                    throw sc.ioException();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (sc != null) {
                    sc.close();
                }
            }

            loadFTPTree();
        });

        addNewDirectory.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setTitle("Create a directory");
            textInputDialog.getDialogPane().setContentText("Directory name:");
            Optional<String> result = textInputDialog.showAndWait();
            TextField input = textInputDialog.getEditor();
            if (input.getText() != null || input.getText().length() == 0) {
                String path = getPathOfItem();
                System.out.println(path + "/" + input.getText());
                if (!ftpManager.addNewDirectory(path + "/" + input.getText())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Add directory error");
                    alert.setContentText("Couldn't create the directory. Try again later.");
                    alert.showAndWait();
                    return;
                }
                loadFTPTree();
            }
        });

        renameItem.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setTitle("Rename a directory");
            textInputDialog.getDialogPane().setContentText("Directory name:");
            Optional<String> result = textInputDialog.showAndWait();
            TextField input = textInputDialog.getEditor();
            if (input.getText() != null || input.getText().length() == 0) {
                String path = getPathOfItem();
                String newPath = path;
                int index = newPath.lastIndexOf('/');
                newPath = newPath.substring(0,index);
                newPath += "/";
                newPath += input.getText();

                ftpManager.rename(path, newPath); //TODO: Check if error
            }
            loadFTPTree();
        });

        addItem.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setTitle("Create a new item");
            textInputDialog.getDialogPane().setContentText("Item name:");
            Optional<String> result = textInputDialog.showAndWait();
            TextField input = textInputDialog.getEditor();
            if (input.getText() != null || input.getText().length() == 0) {

                String tempFolder = System.getProperty("java.io.tmpdir");
                Path tempFolderPath = Paths.get(tempFolder + "\\.collabmode");
                File newItem = new File(tempFolderPath.toString() + "\\" + input.getText() + ".tmp");
                try {
                    newItem.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String path = getPathOfItem();
                String itemToBeUploaded = newItem.getAbsolutePath();
                System.out.println(path);
                System.out.println(itemToBeUploaded);
                ftpManager.uploadFile(itemToBeUploaded, path); // TODO: error check

                newItem.delete();

                loadFTPTree();
            }
        });

        treeView.setContextMenu(new ContextMenu(addItem, deleteItem, addNewDirectory, renameItem, refresh, load));

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
                        fetchUserDetails();
                        Main.openDashboardStage(token, "Java");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        invite.setOnAction(event -> {
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
                    Main.openDashboardStage(token, "Java");
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
                    alert.setTitle("Session error");
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

    public void setupAccelerator(){
        if(codeArea != null){
            Scene scene = codeArea.getScene();
            if(scene != null){
                scene.getAccelerators().put(KeyCombination.keyCombination("CTRL+S"), new Runnable() {
                    @Override
                    public void run() {
                        if(currentFile != null) {
                            if(ftpManager != null) {
                                PrintWriter writer = null;
                                try {
                                    System.out.println("Here");
                                    writer = new PrintWriter(currentFile.getAbsolutePath(), "UTF-8");
                                    writer.print(codeArea.getText());
                                    writer.close();
                                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                ftpManager.deleteFile(currentFileLocationOnFTP);
                                ftpManager.uploadFile(currentFile.getAbsolutePath(), currentFileLocationOnFTP.substring(0, currentFileLocationOnFTP.lastIndexOf('/')));
                            }
                        }
                    }
                });
                scene.getAccelerators().put(KeyCombination.keyCombination("CTRL+F"), new Runnable() {
                    @Override
                    public void run() {
                        TextInputDialog textInputDialog = new TextInputDialog();
                        textInputDialog.setTitle("Find");
                        textInputDialog.getDialogPane().setContentText("Find");
                        textInputDialog.getDialogPane().setHeaderText("Tell me what to search for");
                        Optional<String> result = textInputDialog.showAndWait();
                        TextField input = textInputDialog.getEditor();
                        if (input.getText() != null || input.getText().length() == 0) {
                            String text = codeArea.getText();
                            String wordToFind = input.getText();
                            Pattern word = Pattern.compile(wordToFind);
                            Matcher match = word.matcher(text);
                            while (match.find()) {
                                System.out.println("Found " + wordToFind + " at index "+ match.start() +" - "+ (match.end()-1));
                                //codeArea.setStyle(0, match.start(), match.end()-1, Collections.singleton("-rtfx-background-color: red;"));
                                codeArea.setStyleClass(match.start(), (match.end()), "test");
                            }
                        }
                    }
                });

                scene.getAccelerators().put(KeyCombination.keyCombination("CTRL+R"), new Runnable() {
                    @Override
                    public void run() {
                        TextInputDialog textInputDialog = new TextInputDialog();
                        textInputDialog.setTitle("Find & Replace");
                        textInputDialog.getDialogPane().setContentText("Find & Replace");
                        textInputDialog.getDialogPane().setHeaderText("Tell me what to search for");
                        Optional<String> result = textInputDialog.showAndWait();
                        TextField input = textInputDialog.getEditor();
                        if (input.getText() != null || input.getText().length() == 0) {
                            TextInputDialog textInputDialog2 = new TextInputDialog();
                            textInputDialog2.setTitle("Find & Replace");
                            textInputDialog2.getDialogPane().setContentText("Find & Replace");
                            textInputDialog2.getDialogPane().setHeaderText("Tell me what to replace it to");
                            Optional<String> result2 = textInputDialog2.showAndWait();
                            TextField input2 = textInputDialog2.getEditor();
                            if (input2.getText() != null || input2.getText().length() == 0) {
                                String text = codeArea.getText();
                                String wordToFind = input.getText();
                                Pattern word = Pattern.compile(wordToFind);
                                Matcher match = word.matcher(text);
                                while (match.find()) {
                                    System.out.println("Found " + wordToFind + " at index "+ match.start() +" - "+ (match.end()-1));
                                    //codeArea.setStyle(0, match.start(), match.end()-1, Collections.singleton("-rtfx-background-color: red;"));
                                    codeArea.replaceText(match.start(), match.end(), input2.getText());
                                }
                            }



                        }
                    }
                });
            }
        }
    }


    public TreeItem<String> getNodesForDirectory(FTPFile dir){
        TreeItem<String> root = new TreeItem<>(dir.getName(), new ImageView(new Image("/assets/folder_icon.png")));
        for (FTPFile f : ftpManager.getFilesByPath(dir.getName())) {
            if (f.isDirectory()) {
                root.getChildren().add(getNodesForDirectory(f));
            } else {
                if(FilenameUtils.getExtension(f.getName()).equals("java")) {
                    root.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/Java-icon.png"))));
                }else if(FilenameUtils.getExtension(f.getName()).equals("c")){
                    root.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/c-programming.png"))));
                }else if(FilenameUtils.getExtension(f.getName()).equals("py")){
                    root.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/python_icon.png"))));
                }else {
                    root.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/file_icon.png"))));
                }
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
        path = path.replace("/File tree", "");
        return path;
    }

    public void loadFTPTree(){
        FTPFile[] files = ftpManager.getFiles();

        TreeItem<String> item = new TreeItem<String>("File tree", new ImageView(new Image("/assets/tree_icon.png")));
        treeView.setRoot(item);
        item.setExpanded(true);

        for (FTPFile f : files) {
            if (f.isDirectory()) {
                item.getChildren().add(getNodesForDirectory(f));
            }else {
                if(FilenameUtils.getExtension(f.getName()).equals("java")) {
                    item.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/Java-icon.png"))));
                }else if(FilenameUtils.getExtension(f.getName()).equals("c")){
                    item.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/c-programming.png"))));
                }else if(FilenameUtils.getExtension(f.getName()).equals("py")){
                    item.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/python_icon.png"))));
                }else {
                    item.getChildren().add(new TreeItem<String>(f.getName(), new ImageView(new Image("/assets/file_icon.png"))));
                }
            }
        }
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("BRACE") != null ? "brace" :
                                            matcher.group("BRACKET") != null ? "bracket" :
                                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                                            matcher.group("STRING") != null ? "string" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private class VisibleParagraphStyler<PS, SEG, S> implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>>
    {
        private final GenericStyledArea<PS, SEG, S> area;
        private final Function<String,StyleSpans<S>> computeStyles;
        private int prevParagraph, prevTextLength;

        public VisibleParagraphStyler( GenericStyledArea<PS, SEG, S> area, Function<String,StyleSpans<S>> computeStyles )
        {
            this.computeStyles = computeStyles;
            this.area = area;
        }

        @Override
        public void accept( ListModification<? extends Paragraph<PS, SEG, S>> lm )
        {
            if ( lm.getAddedSize() > 0 )
            {
                int paragraph = Math.min( area.firstVisibleParToAllParIndex() + lm.getFrom(), area.getParagraphs().size()-1 );
                String text = area.getText( paragraph, 0, paragraph, area.getParagraphLength( paragraph ) );

                if ( paragraph != prevParagraph || text.length() != prevTextLength )
                {
                    int startPos = area.getAbsolutePosition( paragraph, 0 );
                    Platform.runLater( () -> area.setStyleSpans( startPos, computeStyles.apply( text ) ) );
                    prevTextLength = text.length();
                    prevParagraph = paragraph;
                }
            }
        }
    }

    private class DefaultContextMenu extends ContextMenu {
        private MenuItem fold, unfold, copy, paste, cut, undo, redo;

        public DefaultContextMenu()
        {
            copy = new MenuItem("Copy");
            copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
            copy.setOnAction(AE -> {
                copy();
            });

            paste = new MenuItem("Paste");
            paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
            paste.setOnAction(AE -> {
                paste();
            });

            cut = new MenuItem("Cut");
            cut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
            cut.setOnAction(AE -> {
                cut();
            });

            undo = new MenuItem("Undo");
            undo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
            undo.setOnAction(AE -> {
                undo();
            });

            redo = new MenuItem("Redo");
            redo.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
            redo.setOnAction(AE -> {
                redo();
            });

            fold = new MenuItem( "Fold" );
            fold.setOnAction( AE -> { hide(); fold(); } );

            unfold = new MenuItem( "Unfold" );
            unfold.setOnAction( AE -> { hide(); unfold(); } );

            getItems().addAll( fold, unfold, copy, paste, cut, undo, redo);
        }

        /**
         * Folds multiple lines of selected text, only showing the first line and hiding the rest.
         */
        private void fold() {
            ((CodeArea) getOwnerNode()).foldSelectedParagraphs();
        }

        /**
         * Unfold the CURRENT line/paragraph if it has a fold.
         */
        private void unfold() {
            CodeArea area = (CodeArea) getOwnerNode();
            area.unfoldParagraphs( area.getCurrentParagraph() );
        }

        private void copy(){
            ((CodeArea) getOwnerNode()).copy();
        }

        private void paste(){
            ((CodeArea) getOwnerNode()).paste();
        }

        private void cut(){
            ((CodeArea) getOwnerNode()).cut();
        }

        private void undo(){
            if(((CodeArea) getOwnerNode()).isUndoAvailable()){
                ((CodeArea) getOwnerNode()).undo();
            }
        }

        private void redo(){
            if(((CodeArea) getOwnerNode()).isRedoAvailable()){
                ((CodeArea) getOwnerNode()).redo();
            }
        }
    }

    public static void setMode(String mode){
        DashboardController.mode = mode;
    }

    public static void loadHighlight(String mode){
        if(mode.equals("Java")) {
            KEYWORDS = new String[]{
                    "abstract", "assert", "boolean", "break", "byte",
                    "case", "catch", "char", "class", "const",
                    "continue", "default", "do", "double", "else",
                    "enum", "extends", "final", "finally", "float",
                    "for", "goto", "if", "implements", "import",
                    "instanceof", "int", "interface", "long", "native",
                    "new", "package", "private", "protected", "public",
                    "return", "short", "static", "strictfp", "super",
                    "switch", "synchronized", "this", "throw", "throws",
                    "transient", "try", "void", "volatile", "while"
            };
        }else if(mode.equals("C++")){
            KEYWORDS = new String[]{
                    "asm", "auto", "bool", "break", "case", "catch",
                    "char", "class", "const", "const_char", "continue",
                    "default", "delete", "do", "double", "dynamic_cast",
                    "else", "enum", "explicit", "export", "extern", "false",
                    "float", "for", "friend", "goto", "if", "inline", "int",
                    "long", "mutable", "namespace", "new", "operator",
                    "private", "protected", "public", "register", "reinterpret_cast",
                    "return", "short", "signed", "sizeof", "static", "static_cast",
                    "struct", "switch", "template", "this", "throw", "true",
                    "try", "typedef", "typeid", "typename", "union", "unsigned",
                    "using", "virtual", "void", "volatile", "wchar_t", "while",
                    "And", "bitor", "not_eq", "xor", "and_eq", "compl", "or",
                    "xor_eq", "bitand", "not", "or_eq", "#include", "#define"
            };
        }

        KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
        PAREN_PATTERN = "\\(|\\)";
        BRACE_PATTERN = "\\{|\\}";
        BRACKET_PATTERN = "\\[|\\]";
        SEMICOLON_PATTERN = "\\;";
        STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
        COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

        PATTERN = Pattern.compile(
                "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                        + "|(?<PAREN>" + PAREN_PATTERN + ")"
                        + "|(?<BRACE>" + BRACE_PATTERN + ")"
                        + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                        + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                        + "|(?<STRING>" + STRING_PATTERN + ")"
                        + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
        );
    }

}

