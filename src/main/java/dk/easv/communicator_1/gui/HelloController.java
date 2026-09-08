package dk.easv.communicator_1.gui;

import dk.easv.communicator_1.Service.ChatService;
import dk.easv.communicator_1.Service.LoginService;
import dk.easv.communicator_1.Service.MessageService;
import dk.easv.communicator_1.be.conversations;
import dk.easv.communicator_1.be.user;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HelloController implements Initializable {

    @FXML private ListView<user> userLister;
    @FXML private Label lblUsername;
    @FXML private TextArea txtChatId;
    @FXML private TextField txtMessagePanelId;

    private user currentUser;
    private user selectedUser;
    private conversations activeConversation;

    private final LoginService loginService = new LoginService();
    private final ChatService chatService = new ChatService();

    public void setCurrentUser(user currentUser) {
        this.currentUser = currentUser;
        lblUsername.setText(currentUser.getUsername());
        loadUsers();
    }

    public user getCurrentUser() {
        return currentUser;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtChatId.setEditable(false);
        txtChatId.setWrapText(true);

        userLister.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(user item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername());
            }
        });

        userLister.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> {
            if (newUser != null) {
                openConversation(newUser);
            }
        });
    }

    private void loadUsers() {
        Task<List<user>> task = new Task<>() {
            @Override
            protected List<user> call() {
                return loginService.getAllUsers().stream()
                        .filter(u -> u.getId() != currentUser.getId())
                        .collect(Collectors.toList());
            }
        };
        task.setOnSucceeded(event -> userLister.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(event -> task.getException().printStackTrace());
        startBackgroundTask(task);
    }

    private void openConversation(user otherUser) {
        selectedUser = otherUser;
        txtChatId.clear();
        txtChatId.appendText("Chat with " + otherUser.getUsername() + "\n\n");

        Task<conversations> task = new Task<>() {
            @Override
            protected conversations call() {
                return chatService.openConversation(currentUser.getId(), otherUser.getId());
            }
        };
        task.setOnSucceeded(event -> {
            activeConversation = task.getValue();
            MessageService.loadConversationHistory(
                    activeConversation.getConversation_uuid(),
                    activeConversation.getEncryption_key(),
                    messages -> Platform.runLater(() -> messages.forEach(msg ->
                            txtChatId.appendText(formatIncoming(msg.senderUuid(), msg.content()) + "\n")
                    ))
            );
        });
        task.setOnFailed(event -> task.getException().printStackTrace());
        startBackgroundTask(task);
    }

    public void btnSend(ActionEvent actionEvent) {
        if (selectedUser == null || activeConversation == null) {
            txtChatId.appendText("Select a user from the list first.\n");
            return;
        }

        String msg = txtMessagePanelId.getText().trim();
        if (msg.isEmpty()) {
            return;
        }
        txtMessagePanelId.clear();

        conversations conversation = activeConversation;
        String senderUuid = currentUser.getUuid();

        Task<Void> taskSend = new Task<>() {
            @Override
            protected Void call() {
                MessageService.sendEncrypted(
                        conversation.getConversation_uuid(),
                        conversation.getEncryption_key(),
                        senderUuid,
                        msg
                );
                return null;
            }
        };
        taskSend.setOnSucceeded(event -> txtChatId.appendText("You: " + msg + "\n"));
        taskSend.setOnFailed(event -> {
            txtMessagePanelId.setText(msg);
            taskSend.getException().printStackTrace();
        });
        startBackgroundTask(taskSend);
    }

    public void onDiscordMessage(String conversationUuid, String ciphertext) {
        if (currentUser == null || selectedUser == null) {
            return;
        }

        conversations conversation = activeConversation;
        if (conversation == null || !conversationUuid.equals(conversation.getConversation_uuid())) {
            conversation = chatService.findByUuid(conversationUuid);
        }
        if (conversation == null) {
            return;
        }

        boolean viewingThisChat = activeConversation != null
                && conversationUuid.equals(activeConversation.getConversation_uuid());
        boolean bothParticipants = chatService.isMember(conversation.getConversation_id(), currentUser.getId())
                && chatService.isMember(conversation.getConversation_id(), selectedUser.getId());
        if (!viewingThisChat && !bothParticipants) {
            return;
        }

        MessageService.DecryptedMessage decrypted = MessageService.decryptPayload(
                conversation.getEncryption_key(),
                ciphertext
        );
        if (decrypted == null) {
            return;
        }
        if (currentUser.getUuid() != null && decrypted.senderUuid().equals(currentUser.getUuid())) {
            return;
        }

        activeConversation = conversation;
        Platform.runLater(() -> txtChatId.appendText(formatIncoming(decrypted.senderUuid(), decrypted.content()) + "\n"));
    }




    private String formatIncoming(String senderUuid, String content) {
        if (currentUser.getUuid() != null && senderUuid.equals(currentUser.getUuid())) {
            return "You: " + content;
        }
        if (selectedUser != null) {
            return selectedUser.getUsername() + ": " + content;
        }
        return senderUuid + ": " + content;
    }

    private void startBackgroundTask(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
