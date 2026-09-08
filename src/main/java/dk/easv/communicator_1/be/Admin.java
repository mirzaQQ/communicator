package dk.easv.communicator_1.be;

public class Admin extends ParentUser {

    public Admin(int id) {
        super(id);
    }

    @Override
    public Roles getRole() {
        return Roles.ADMIN;
    }

    public Admin(int id, String username, String password, String email, Boolean isAdmin) {
        super(id, username, password, email, isAdmin);
    }

    public Admin(int id, String username, String Password, String email) {
        super(id, username, Password, email);
    }


}
