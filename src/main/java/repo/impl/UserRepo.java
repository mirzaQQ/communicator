package repo.impl;
import java.sql.Connection;


import dk.easv.communicator_1.ConnectionManager.ConnectionManager;
import dk.easv.communicator_1.be.user;
import dk.easv.communicator_1.exceptions.LoginException;
import org.mindrot.jbcrypt.BCrypt;
import repo.repositories.IUserRepo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepo implements IUserRepo {
    ConnectionManager connectionManager = new ConnectionManager();
    private final String createuser= "INSERT INTO users (username, password_hash, email, discord_uuid) VALUES (?, ?, ?, ?)";
    private final String loginUser = "SELECT id, username, email, password_hash, discord_uuid FROM users WHERE username = ?";
    private final String getAllUsers = "SELECT id, username, email, discord_uuid FROM users";

    public UserRepo() {
        ensureUuidColumn();
    }

    private void ensureUuidColumn() {
        try (Connection conn = connectionManager.connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE users ADD COLUMN discord_uuid TEXT");
        } catch (Exception ignored) {
            // Column already exists
        }
    }

    public void createUser(String uname, String passwd, String email){
        try (Connection conn = (Connection) connectionManager.connect()) {
            PreparedStatement ps = conn.prepareStatement(createuser);
            ps.setString(1, uname);
            ps.setString(2, passwd);
            ps.setString(3, email);
            ps.setString(4, UUID.randomUUID().toString());
            ps.execute();
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public user loginUser(String uname, String passwd) throws LoginException {
        try (Connection conn = (Connection) connectionManager.connect()) {
            PreparedStatement ps = conn.prepareStatement(loginUser);
            ps.setString(1, uname);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(passwd, storedHash)) {
                    return mapUser(conn, rs);
                }
                throw new LoginException("Invalid credentials");
            }
            throw new LoginException("User does not exist");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<user> getAllUsers() {
        List<user> users = new ArrayList<>();
        try (Connection conn = connectionManager.connect()) {
            PreparedStatement ps = conn.prepareStatement(getAllUsers);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapUser(conn, rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    private user mapUser(Connection conn, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String uuid = rs.getString("discord_uuid");
        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
            PreparedStatement update = conn.prepareStatement("UPDATE users SET discord_uuid = ? WHERE id = ?");
            update.setString(1, uuid);
            update.setInt(2, id);
            update.executeUpdate();
        }
        return new user(id, rs.getString("username"), "", rs.getString("email"), uuid);
    }
}
