package App;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setTitle("Client Email");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Resources/fxml/MainLayout.fxml"));
            MainModel model = new MainModel();
            MainController MainController = new MainController(model);
            loader.setController(MainController);
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root, primaryScreenBounds.getMaxX() * 0.7, primaryScreenBounds.getMaxY() *0.7));
            primaryStage.setOnCloseRequest(e -> {
                model.saveMessages();
                model.poolShutdown();
                Platform.exit();
            });
            primaryStage.show();
            //testSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void testSocket() throws IOException {
        Runnable task = new Runnable() {
            public void run() {
                try {
                    Socket s = new Socket("127.0.0.1", 1337);
                    PrintWriter pr = new PrintWriter(s.getOutputStream());
                    String [] receivers = {"aa"};
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    while (true){
                        Thread.sleep(1000);
                        out.writeObject(new Email("aa", "aa", receivers, "aa"));
                        out.flush();
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        Thread t = new Thread(task); t.start();
    }
}
