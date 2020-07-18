package App;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setTitle("Email server");
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Resources/fxml/MainLayout.fxml"));
            MainModel model = new MainModel();
            MainController mainController = new MainController(model);
            loader.setController(mainController);
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root, primaryScreenBounds.getMaxX() * 0.7, primaryScreenBounds.getMaxY() * 0.7));
            primaryStage.setOnCloseRequest(e -> {
                model.saveMessages();
                mainController.poolShutdown();
                Platform.exit();
            });
            primaryStage.show();
        }catch (Exception e){e.printStackTrace();}

    }


    public static void main(String[] args) {
        launch(args);
    }
}
