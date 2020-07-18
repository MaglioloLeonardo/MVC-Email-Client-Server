package App;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private TextFlow messageRenderer;
    @FXML private MenuItem connectButton;
    @FXML private Label connectionStatus;
    @FXML private TreeView<String> emailFoldersTreeView;
    @FXML private TableView<Email> emailTableView;
    @FXML private TableColumn<Email, String> subjectCol;
    @FXML private TableColumn<Email, String> senderCol;
    @FXML private TableColumn<Email, Float> sizeCol;
    @FXML private TableColumn<Email, Date> dateCol;
    MainModel model;
    String currentAccount = Settings.localAccounts[0];
    String currentTreeField = "Inbox";

    boolean connectToggle = false;
    NetworkController networkController;
    Thread threadNetController = new Thread();

    public MainController(MainModel model){
        assert(this.model == null && model instanceof MainModel);
        this.model = model;

        if(validateIPV4(Settings.defaultIP))
            model.IpAddr.set(Settings.defaultIP);
        else{
            System.out.println("ERROR: Invalid defaultIP on Settings class");
            model.IpAddr.set("127.0.0.1");
        }

        if(validatePort(Settings.defaultPort))
            model.port.set(Settings.defaultPort);
        else{
            System.out.println("ERROR: Invalid defaultPort on Settings class");
            model.port.set(1337);
        }

        threadNetController.setDaemon(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connectionStatus.textProperty().bind(model.state);
        treeGenerator();
        subjectCol.setCellValueFactory(new PropertyValueFactory<Email, String>("subject"));
        senderCol.setCellValueFactory(new PropertyValueFactory<Email, String>("sender"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<Email, Float>("size"));
        dateCol.setCellValueFactory(new PropertyValueFactory<Email, Date>("date"));
        dateCol.setSortType(TableColumn.SortType.ASCENDING);


        model.resumeMessages();
        //model.loadSampleMessages();

        treeSelectorUpdate();
        model.setEmailsOnTable(currentAccount, currentTreeField, emailTableView);

    }

    public void printData(MouseEvent event){
        Email selected = getSelectedEmail();
        if(event.getButton().name().equals("PRIMARY") &&  selected != null){
            Text header = new Text(headerGen(selected));
            header.setStyle("-fx-font-weight:bold; -fx-fill: DIMGREY;");

            Text message = new Text("\n\n" + selected.getText());

            messageRenderer.getChildren().clear();
            messageRenderer.getChildren().add(header);
            messageRenderer.getChildren().add(message);
        }
    }

    public void newMessage(ActionEvent event) throws IOException {
        messageWindowGen(currentAccount, "", "", "");
    }

    public void copy(ActionEvent event){
        Email selected = emailTableView.getSelectionModel().getSelectedItem();
        if(selected != null){
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(selected.getText());
            clipboard.setContent(content);
        }
    }

    public void delete(ActionEvent event){
        Email selected = getSelectedEmail();
        if(selected != null &&  emailTableView.getItems() != null){
            emailTableView.getItems().remove(selected);
        }
    }

    public void deleteAll(ActionEvent event){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("You are about to delete all your messages in '" + currentTreeField +"' of '" + currentAccount + "'");
        alert.setContentText("Do you confirm?");
        Optional<ButtonType> result = alert.showAndWait();
        if (((Optional) result).get() == ButtonType.OK && emailTableView.getItems() != null){
            model.removeAllMessages(currentAccount, currentTreeField);
            messageRenderer.getChildren().clear();
        }
    }

    public void reply(ActionEvent event){
        Email selected = getSelectedEmail();
        if(selected != null){
            String toSend = "______REPLY______\n" + headerGen(selected) + ":\n" + selected.getText() + "\n__________________\n" ;
            messageWindowGen(currentAccount, selected.getSender(),"RE:" + selected.getSubject(), toSend);
        }
    }

    public void forward(ActionEvent event){
        Email selected = getSelectedEmail();
        if(selected != null){
            messageWindowGen(currentAccount,"", selected.getSubject(), selected.getText());
        }
    }

    public void settings(){
        //model.resumeMessages();
        //treeSelectorUpdate();
        Stage options = new Stage();
        OptionsController optionsController = new OptionsController();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Resources/fxml/OptionsLayout.fxml"));
            loader.setController(optionsController);
            AnchorPane root = loader.load();
            optionsController.updateFields(model.IpAddr.get(), model.port.get());
            Scene scene = new Scene(root);
            options.setScene(scene);
            options.show();
            optionsController.saveButton.setOnAction(e->{
                String newIpAddr = optionsController.ipAddress.getText();
                int newPort = Integer.parseInt(optionsController.port.getText());
                if(validateIPV4(newIpAddr) && validatePort(newPort)){
                    model.IpAddr.set(newIpAddr);
                    model.port.set(newPort);
                }else{
                    String error = "";
                    if(!validateIPV4(newIpAddr))error = "Invalid IPV4 address; ";
                    if(!validatePort(newPort))error += "Invalid port";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error Dialog"); alert.setHeaderText("Syntax error:");
                    alert.setContentText(error);
                    alert.showAndWait();
                }
            });
        }catch (Exception e){e.printStackTrace();}
    }

    public void connect(){
        if(!connectToggle){
            networkController = new NetworkController(model.IpAddr, model.port, model.state , Settings.localAccounts, model.listTable);
            networkController.start();
            connectButton.setText("Disconnect");
            connectToggle = true;
        }else{
            networkController.stop();
            model.state.set("Disconnected");
            connectButton.setText("Connect");
            connectToggle = false;
        }
    }

    //Local-------------------------------------
    private String headerGen(Email message){
        assert message != null;
        return "Subject: " + message.getSubject() + "       Sender: " + message.getSender() +"       Receivers:" + message.getStringreceivers()
                +  "       Size:" + message.getSize() + " (byte)       Date: " + message.getDate();
    }

    private Email getSelectedEmail(){
        assert emailTableView != null;
        return emailTableView.getSelectionModel().getSelectedItem();
    }

    private void messageWindowGen(String account, String to, String subject,  String message){
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        Stage newMessage = new Stage();
        newMessage.setTitle("New message");
        MessageController messageController = new MessageController();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Resources/fxml/ComposeMessageLayout.fxml"));
            loader.setController(messageController);
            AnchorPane root = loader.load();
            messageController.updateFields(account, to, subject, message);
            Scene scene = new Scene(root, primaryScreenBounds.getMaxX() * 0.5, primaryScreenBounds.getMaxY() *0.5);
            newMessage.setScene(scene);
            newMessage.show();

            messageController.sendButton.setOnAction(e->{
                if(messageController.toSend != null)model.addMessage(messageController.senderChoice.getValue(), "Pending", messageController.toSend);
            });
        }catch (IOException e){e.printStackTrace();}
    }

    private void treeGenerator(){
        TreeItem<String> root = new TreeItem<>("root");
        root.setExpanded(true);
        emailFoldersTreeView.setRoot(root); emailFoldersTreeView.setShowRoot(false);

        //Update tableView
        emailFoldersTreeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue)->{
            waitForResume();
            TreeItem<String> parent = emailFoldersTreeView.getSelectionModel().getSelectedItem().getParent();

            if(parent.getValue().compareTo("root") != 0){
                currentAccount =  parent.getValue();
            }else currentAccount = emailFoldersTreeView.getSelectionModel().getSelectedItem().getValue();

            if(parent != null){
                switch(newValue.getValue()){
                    case "Inbox":{
                        model.setEmailsOnTable(parent.getValue(), "Inbox", emailTableView);
                        currentTreeField = "Inbox";
                        senderCol.setCellValueFactory(new PropertyValueFactory<Email, String>("sender"));
                        senderCol.setText("Sender");break;
                    }

                    case "Sent":{
                        model.setEmailsOnTable(parent.getValue(), "Sent", emailTableView);
                        currentTreeField = "Sent";
                        senderCol.setCellValueFactory(new PropertyValueFactory<Email, String>("stringreceivers"));
                        senderCol.setText("Receivers");break;
                    }

                    case "Pending":{
                        model.setEmailsOnTable(parent.getValue(), "Pending", emailTableView);
                        currentTreeField = "Pending";
                        senderCol.setCellValueFactory(new PropertyValueFactory<Email, String>("stringreceivers"));
                        senderCol.setText("Receivers");break;
                    }

                    default:{
                        if(newValue != null && parent != null && parent .getValue()!= "root"){
                            currentTreeField = newValue.getValue();
                            model.setEmailsOnTable(parent.getValue(), newValue.getValue(), emailTableView);
                            senderCol.setCellValueFactory(new PropertyValueFactory<Email, String>("stringreceivers"));
                            senderCol.setText("Receivers");break;
                        }
                    }
                }

            }
        });

        //Set dynamic context menu
       emailFoldersTreeView.setOnMouseReleased(e ->{
               ContextMenu contextTree = new ContextMenu();
               TreeItem<String> selected = emailFoldersTreeView.getSelectionModel().getSelectedItem();
               MenuItem newMessage = new MenuItem("New message"); contextTree.getItems().add(newMessage);
               if(selected == null || selected.getParent().getValue().compareTo("root") == 0){
                   newMessage.setOnAction(action -> messageWindowGen(currentAccount, "","", ""));
               }else{
                   newMessage.setOnAction(action -> messageWindowGen(currentAccount, "","", ""));
                   MenuItem deleteAll = new MenuItem("Delete all");
                   deleteAll.setOnAction(this::deleteAll);
                   contextTree.getItems().add(deleteAll);
               }
               emailFoldersTreeView.setContextMenu(contextTree);
       });

       //generating all TreeItem
        for(String account:Settings.localAccounts){
            TreeItem<String> item = new TreeItem<>(account);
            if(account.compareTo(Settings.localAccounts[0]) == 0){
                item.setExpanded(true);
            }else {
                item.setExpanded(false);
            }
            root.getChildren().add(item);
            for(String field: Settings.treeFields){
                item.getChildren().add(new TreeItem<>(field));
            }

        }
    }

    public void treeSelectorUpdate() {
        String nameField = "Inbox", nameAccount = Settings.localAccounts[0] ;
        if(emailFoldersTreeView.getSelectionModel().getSelectedItem() != null){
            TreeItem<String> parent = emailFoldersTreeView.getSelectionModel().getSelectedItem().getParent();
            if(parent.getValue().compareTo("root") != 0){
                nameField = emailFoldersTreeView.getSelectionModel().getSelectedItem().getValue();
                nameAccount = parent.getValue();
            }
        }
        waitForResume();
        model.setEmailsOnTable(nameAccount, nameField, emailTableView);
    }

    public void waitForResume(){
        try{
            for(Thread t: Thread.getAllStackTraces().keySet()){
                if(t.getName().compareTo(Settings.resumeThreadName) == 0){
                    t.join();
                    break;
                }
            }
        }catch (Exception e){}
    }

    public static boolean validateIPV4(final String ip){
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
        return ip.matches(PATTERN);
    }

    public static boolean validatePort(final int port){
        if(port > 0 && port < 65535)
            return true;
        else return false;
    }

}


