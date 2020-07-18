package App;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class OptionsController {
    @FXML TextField ipAddress;
    @FXML TextField port;
    @FXML Button saveButton;

    public void updateFields(String ipAddr, int port){
        ipAddress.setText(ipAddr);
        this.port.setText(String.valueOf(port));
    }

}
