package dk.easv.communicator_1.be;

public class user extends ParentUser{
    public user(int id, String username, String Password, String email) {
        super(id, username, Password, email);
    }

    public user(int id, String username, String Password, String email, String uuid) {
        super(id, username, Password, email, uuid);
    }

    public user(int id) {
        super(id);
    }

    @Override
    public Roles getRole() {
        return Roles.USER;
    }
}
