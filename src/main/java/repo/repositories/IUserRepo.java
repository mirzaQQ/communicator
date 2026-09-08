package repo.repositories;

import dk.easv.communicator_1.be.user;
import dk.easv.communicator_1.exceptions.LoginException;

import java.util.List;

public interface IUserRepo {
    void createUser(String uname, String passwd, String email);
    user loginUser(String uname, String passwd) throws LoginException;
    List<user> getAllUsers();
}
