package dk.easv.communicator_1.gui;

import dk.easv.communicator_1.Service.LoginService;
import dk.easv.communicator_1.UIHelper.UiHelper;
import dk.easv.communicator_1.be.user;
import dk.easv.communicator_1.exceptions.LoginException;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Objects;

public class LoginController {

    @FXML private Label lblError;
    @FXML private TextField UsernameInput;
    @FXML private TextField EmailInput;
    @FXML private Button LogRegButton;
    @FXML private Label lblLogReg;
    @FXML private TextField PasswordInput;
    @FXML private HBox EmailBox;
    private final LoginService loginService = new LoginService();

    public void LogRegAppear(MouseEvent mouseEvent) {
        if (Objects.equals(lblLogReg.getText(), "Don't have an account")){
            EmailBox.setVisible(true);
            lblLogReg.setText("Log in with an account");
            LogRegButton.setText("Register");
        }
        else {
            EmailBox.setVisible(false);
            lblLogReg.setText("Don't have an account");
            LogRegButton.setText("Login");
        }
    }

    public void LoginRegisterOnClick(ActionEvent actionEvent) throws LoginException {
        try {

            if (Objects.equals(LogRegButton.getText(), "Login")) {

                user loggedInUser = loginService.login(
                        UsernameInput.getText(),
                        PasswordInput.getText()
                );

                UiHelper uiHelper = new UiHelper();
                uiHelper.openChatPanel(loggedInUser);
                ((Stage) UsernameInput.getScene().getWindow()).close();

            } else {

                loginService.validateFields(
                        UsernameInput.getText(),
                        BCrypt.hashpw(PasswordInput.getText(), BCrypt.gensalt(12)),
                        EmailInput.getText()
                );


            }

            lblError.setText("");

        } catch (LoginException e) {
            lblError.setText(e.getMessage());
        } catch (Exception e) {
            lblError.setText(e.getCause().getMessage());
        }
    }
}
