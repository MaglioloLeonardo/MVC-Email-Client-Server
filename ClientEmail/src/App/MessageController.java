package App;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageController implements Initializable {
    @FXML ChoiceBox<String> senderChoice;
    @FXML TextField recipientTextField;
    @FXML TextField subjectTextField;
    @FXML TextArea textEditor;
    @FXML Label errorLabel;
    @FXML Button sendButton;
    ObservableList<String> accounts;
    Email toSend;

    public MessageController(){
        accounts = FXCollections.observableArrayList();
        accounts.addAll(Arrays.asList(Settings.localAccounts));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        senderChoice.setItems(accounts);
        senderChoice.setValue(Settings.localAccounts[0]);
        senderChoice.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue)->{
                updateToSend();
        });

    }

    public void updateFields(String account, String to, String subject,  String message){
        assert recipientTextField != null && textEditor != null;
        senderChoice.setValue(account);
        recipientTextField.setText(to);
        subjectTextField.setText(subject);
        textEditor.setText(message);
        fieldsCheck();
    }

    public void fieldsCheck(){
        assert errorLabel != null && recipientTextField != null && sendButton != null;
        if(validateEmailAddresses(recipientTextField.getText())){
            sendButton.setDisable(false);
            updateToSend();
            if(subjectTextField.getText().compareTo("") == 0){
                errorLabel.setText("Alert: Missing subject");
            }else{
                errorLabel.setText("");
            }
        }else{
            sendButton.setDisable(true);
            if(recipientTextField.getText().compareTo("") == 0){
                errorLabel.setText("Error: Missing 'To' address");
            }else{
                errorLabel.setText("Error: Invalid email addresses");
            }
        }
    }

    //LOCALS____________________________
    private boolean validateEmailAddresses(String emailAddresses) {
        Pattern regexPattern = Pattern.compile("[(a-zA-Z-0-9-\\_\\+\\.)]+@[(a-z-A-z)]+\\.[(a-zA-z)]{2,3}( )*" +
                "(( )+[(a-zA-Z-0-9-\\_\\+\\.)]+@[(a-z-A-z)]+\\.[(a-zA-z)]{2,3}[ ]*)*");
        Matcher regMatcher = regexPattern.matcher(emailAddresses);
        if(regMatcher.matches()) {
            return true;
        } else return false;
    }

    private void updateToSend(){
        toSend = new Email(subjectTextField.getText(), senderChoice.getValue(), recipientTextField.getText().split("( )+"),textEditor.getText());
    }
}
