package dk.easv.communicator_1.be;

public abstract class ParentUser {
    protected int id;
    protected String username;
    protected String password;
    protected String email;
    protected Boolean isAdmin;
    protected String uuid;

    public ParentUser(int id, String username, String password, String email, Boolean isAdmin){
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.isAdmin = isAdmin;

    }

    public ParentUser(int id, String username, String Password, String email){
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public ParentUser(int id, String username, String Password, String email, String uuid){
        this(id, username, Password, email);
        this.uuid = uuid;
    }

    public ParentUser (int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public abstract Roles getRole();
}

