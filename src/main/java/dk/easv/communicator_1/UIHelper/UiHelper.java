package dk.easv.communicator_1.UIHelper;

import dk.easv.communicator_1.HelloApplication;
import dk.easv.communicator_1.Service.DiscordBot;
import dk.easv.communicator_1.be.conversation_members;
import dk.easv.communicator_1.be.conversations;
import dk.easv.communicator_1.be.user;
import dk.easv.communicator_1.gui.HelloController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;

public class UiHelper {
    String uname;
    ArrayList<user> user;
    ArrayList<conversation_members> conversation_members;
    ArrayList<conversations> conversations;

    public UiHelper(String uname, ArrayList<user> user, ArrayList<conversation_members> conversation_members, ArrayList<conversations> conversations){
        this.uname = uname;
        this.user = user;
        this.conversation_members = conversation_members;
        this.conversations = conversations;
    }

    public UiHelper(){

    }

    public void openChatPanel(user loggedInUser) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("gui/chat-panel.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        HelloController controller = fxmlLoader.getController();
        controller.setCurrentUser(loggedInUser);
        stage.setTitle("Chat - " + loggedInUser.getUsername());
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> DiscordBot.unregister(controller));
        stage.show();
        DiscordBot.start(controller);
    }
}
