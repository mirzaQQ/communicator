package dk.easv.communicator_1.Service;

import dk.easv.communicator_1.be.user;
import dk.easv.communicator_1.exceptions.LoginException;
import repo.impl.UserRepo;
import repo.repositories.IUserRepo;

import java.util.List;

public class LoginService {
    private final IUserRepo userRepo = new UserRepo();

    public boolean isEmpty(String field) {
        return field == null || field.trim().isEmpty();
    }

    public void validateFields(String... fields) throws LoginException {
        for (String field : fields) {
            if (isEmpty(field)) {
                throw new LoginException("All fields must be filled.");

            }
        }
        if (fields.length == 3){
           // createUser(fields[0], fields[1], fields[2]);
            userRepo.createUser(fields[0], fields[1], fields[2]);
        }

    }

    public user login(String username, String password) throws LoginException {
        validateFields(username, password);
        return userRepo.loginUser(username, password);
    }

    public List<user> getAllUsers() {
        return userRepo.getAllUsers();
    }

}
