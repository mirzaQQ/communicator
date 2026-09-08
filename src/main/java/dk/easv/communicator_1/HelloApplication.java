package dk.easv.communicator_1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    public void start(Stage stage) throws Exception {
        openWindow("Login1");
        openWindow("Login2");

    }

    private void openWindow(String title) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("gui/login-panel.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        // HelloController controller = fxmlLoader.getController();
        Stage stage = new Stage();
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }
//    @Override
//    public void start(Stage stage) throws Exception {
//        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("gui/login-panel.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//       // HelloController controller = fxmlLoader.getController();
//        stage.setTitle("Login");
//        stage.setScene(scene);
//        stage.show();
//       // DiscordBot.start(controller);
//
//
//
//    }


}
