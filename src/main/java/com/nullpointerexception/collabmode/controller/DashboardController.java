package com.nullpointerexception.collabmode.controller;

import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTreeView;
import com.nullpointerexception.collabmode.application.Main;
import com.nullpointerexception.collabmode.model.User;
import com.nullpointerexception.collabmode.service.FTPManager;
import com.nullpointerexception.collabmode.service.HTTPRequestManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import org.apache.commons.net.ftp.FTPFile;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reactfx.Subscription;
import org.reactfx.collection.ListModification;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardController {

    private static final String[] KEYWORDS = new String[] {
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

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
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

    @FXML private JFXTreeView<String> treeView;
    @FXML private MenuBar menuBar;
    @FXML private CodeArea codeArea;

    @FXML private Menu collaborateMenu;
    @FXML private MenuItem addTeam;
    @FXML private Menu myTeamMenu;
    @FXML private MenuItem createProjectItem;

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



        }


        if(currentUser.getTeamID() != 0 && currentUser.isTeamOwner()) {
            MenuItem transferOwnerItem = new MenuItem();
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
                        Main.openDashboardStage(token);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            });
            myTeamMenu.getItems().add(transferOwnerItem);
        }

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



//        executor = Executors.newSingleThreadExecutor();
//        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
//        Subscription cleanupWhenDone = codeArea.multiPlainChanges()
//                .successionEnds(java.time.Duration.ofMillis(500))
//                .supplyTask(this::computeHighlightingAsync)
//                .awaitLatest(codeArea.multiPlainChanges())
//                .filterMap(t -> {
//                    if(t.isSuccess()) {
//                        return Optional.of(t.get());
//                    } else {
//                        t.getFailure().printStackTrace();
//                        return Optional.empty();
//                    }
//                })
//                .subscribe(this::applyHighlighting);




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


        MenuItem addItem = new MenuItem("Add item");  // FIXME: Currently not working
        MenuItem deleteItem = new MenuItem("Delete");
        MenuItem addNewDirectory = new MenuItem("New folder");
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem refresh = new MenuItem("Refresh");

        refresh.setOnAction(event -> {
            loadFTPTree();
        });

        deleteItem.setOnAction(event -> {
            String path = getPathOfItem();
            System.out.println(path);
            ftpManager.deleteFile(path);
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

        treeView.setContextMenu(new ContextMenu(addItem, deleteItem, addNewDirectory, renameItem, refresh));

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
                        Main.openDashboardStage(token);
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
                    Main.openDashboardStage(token);
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
        path = path.replace("/File tree", "");
        return path;
    }

    public void loadFTPTree(){
        FTPFile[] files = ftpManager.getFiles();

        TreeItem<String> item = new TreeItem<String>("File tree");
        treeView.setRoot(item);
        item.setExpanded(true);

        for (FTPFile f : files) {
            if (f.isDirectory()) {
                item.getChildren().add(getNodesForDirectory(f));
            } else {
                item.getChildren().add(new TreeItem<String>(f.getName()));
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

    private class DefaultContextMenu extends ContextMenu
    {
        private MenuItem fold, unfold, print;

        public DefaultContextMenu()
        {
            fold = new MenuItem( "Fold selected text" );
            fold.setOnAction( AE -> { hide(); fold(); } );

            unfold = new MenuItem( "Unfold from cursor" );
            unfold.setOnAction( AE -> { hide(); unfold(); } );

            print = new MenuItem( "Print" );
            print.setOnAction( AE -> { hide(); print(); } );

            getItems().addAll( fold, unfold, print );
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

        private void print() {
            System.out.println( ((CodeArea) getOwnerNode()).getText() );
        }
    }
}

