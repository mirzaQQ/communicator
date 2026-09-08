package repo.impl;

import dk.easv.communicator_1.ConnectionManager.ConnectionManager;
import dk.easv.communicator_1.Service.MessageCrypto;
import dk.easv.communicator_1.be.conversations;
import repo.repositories.IConversationRepo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class ConversationRepo implements IConversationRepo {
    private final ConnectionManager connectionManager = new ConnectionManager();
    private static final Object CONVERSATION_LOCK = new Object();

    private static final String FIND_CONVERSATION = """
            SELECT c.conversation_id, c.conversation_uuid, c.encryption_key
            FROM conversations c
            JOIN conversation_members cm1 ON c.conversation_id = cm1.conversation_id
            JOIN conversation_members cm2 ON c.conversation_id = cm2.conversation_id
            WHERE cm1.user_id = ? AND cm2.user_id = ?
            ORDER BY c.conversation_id ASC
            LIMIT 1
            """;

    private static final String FIND_BY_UUID = """
            SELECT conversation_id, conversation_uuid, encryption_key
            FROM conversations
            WHERE conversation_uuid = ?
            LIMIT 1
            """;

    public ConversationRepo() {
        initSchema();
    }

    private void initSchema() {
        try (Connection conn = connectionManager.connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS conversations (
                        conversation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        conversation_uuid TEXT,
                        encryption_key TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS conversation_members (
                        conversation_id INTEGER NOT NULL,
                        user_id INTEGER NOT NULL,
                        PRIMARY KEY (conversation_id, user_id)
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS conversation_pair (
                        user_low INTEGER NOT NULL,
                        user_high INTEGER NOT NULL,
                        conversation_id INTEGER NOT NULL,
                        PRIMARY KEY (user_low, user_high)
                    )
                    """);
            addColumnIfMissing(stmt, "conversations", "conversation_uuid", "TEXT");
            addColumnIfMissing(stmt, "conversations", "encryption_key", "TEXT");
            repairConversationIds(conn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize conversation schema", e);
        }
    }

    /**
     * DataGrip created conversations with MySQL-style INT AUTO_INCREMENT.
     * SQLite ignores that, so conversation_id stays NULL and JOINs never match.
     */
    private void repairConversationIds(Connection conn) throws SQLException {
        boolean hasNullIds;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM conversations WHERE conversation_id IS NULL")) {
            hasNullIds = rs.next() && rs.getInt(1) > 0;
        }
        if (!hasNullIds) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE conversations_repaired (
                        conversation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        conversation_uuid TEXT,
                        encryption_key TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            stmt.execute("""
                    INSERT INTO conversations_repaired (conversation_id, conversation_uuid, encryption_key, created_at)
                    SELECT rowid, conversation_uuid, encryption_key, created_at FROM conversations
                    """);
            stmt.execute("DROP TABLE conversations");
            stmt.execute("ALTER TABLE conversations_repaired RENAME TO conversations");
        }
    }

    private void addColumnIfMissing(Statement stmt, String table, String column, String type) {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (SQLException ignored) {
            // Column already exists
        }
    }

    @Override
    public conversations findOrCreateConversation(int userId1, int userId2) {

        synchronized (CONVERSATION_LOCK) {

            int userLow = Math.min(userId1, userId2);
            int userHigh = Math.max(userId1, userId2);

            try (Connection conn = connectionManager.connect()) {

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("BEGIN IMMEDIATE");
                }

                try {

                    conversations existing =
                            findExisting(conn, userLow, userHigh);

                    if (existing == null) {
                        existing = create(conn, userLow, userHigh);
                    }

                    conversations ready =
                            ensureCryptoFields(conn, existing);

                    // At this point ALL statements/result sets
                    // created by the above methods must be closed.

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("COMMIT");
                    }

                    return ready;

                } catch (Exception e) {

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ROLLBACK");
                    } catch (SQLException ignored) {
                    }

                    throw e;
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public conversations findByUuid(String conversationUuid) {
        try (Connection conn = connectionManager.connect();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_UUID)) {
            ps.setString(1, conversationUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapConversation(rs);
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isMember(int conversationId, int userId) {
        if (conversationId <= 0) {
            return false;
        }
        try (Connection conn = connectionManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM conversation_members WHERE conversation_id = ? AND user_id = ? LIMIT 1"
            )) {
            ps.setInt(1, conversationId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private conversations findExisting(Connection conn, int userLow, int userHigh) throws SQLException {
        try (PreparedStatement pair = conn.prepareStatement(
                "SELECT c.conversation_id, c.conversation_uuid, c.encryption_key " +
                        "FROM conversation_pair p " +
                        "JOIN conversations c ON c.conversation_id = p.conversation_id " +
                        "WHERE p.user_low = ? AND p.user_high = ?"
        )) {
            pair.setInt(1, userLow);
            pair.setInt(2, userHigh);
            try (ResultSet pairRs = pair.executeQuery()) {
                if (pairRs.next()) {
                    return mapConversation(pairRs);
                }
            }
        }

        conversations found = null;
        try (PreparedStatement members = conn.prepareStatement(FIND_CONVERSATION)) {
            members.setInt(1, userLow);
            members.setInt(2, userHigh);
            try (ResultSet memberRs = members.executeQuery()) {
                if (memberRs.next()) {
                    found = mapConversation(memberRs);
                }
            }
        }
        if (found != null) {
            linkPair(conn, userLow, userHigh, found.getConversation_id());
            return found;
        }
        return null;
    }

    private conversations create(Connection conn, int userLow, int userHigh) throws SQLException {
        String uuid = UUID.randomUUID().toString();
        String key = MessageCrypto.generateKey();
        int conversationId;
        try (PreparedStatement insertConv = conn.prepareStatement(
                "INSERT INTO conversations (conversation_uuid, encryption_key) VALUES (?, ?) RETURNING conversation_id"
        )) {
            insertConv.setString(1, uuid);
            insertConv.setString(2, key);
            try (ResultSet keys = insertConv.executeQuery()) {
                if (!keys.next()) {
                    throw new RuntimeException("Failed to create conversation");
                }
                conversationId = keys.getInt(1);
            }
        }
        if (conversationId <= 0) {
            throw new RuntimeException("Failed to create conversation: missing conversation_id");
        }
        insertMember(conn, conversationId, userLow);
        insertMember(conn, conversationId, userHigh);
        linkPair(conn, userLow, userHigh, conversationId);
        return new conversations(conversationId, uuid, key);
    }

    private conversations ensureCryptoFields(Connection conn, conversations current) throws SQLException {
        String uuid = current.getConversation_uuid();
        String key = current.getEncryption_key();
        if (uuid != null && !uuid.isBlank() && key != null && !key.isBlank()) {
            return current;
        }

        String newUuid = uuid == null || uuid.isBlank() ? UUID.randomUUID().toString() : uuid;
        String newKey = key == null || key.isBlank() ? MessageCrypto.generateKey() : key;
        try (PreparedStatement update = conn.prepareStatement(
                "UPDATE conversations SET conversation_uuid = ?, encryption_key = ? " +
                        "WHERE conversation_id = ? AND (conversation_uuid IS NULL OR conversation_uuid = '' " +
                        "OR encryption_key IS NULL OR encryption_key = '')"
        )) {
            update.setString(1, newUuid);
            update.setString(2, newKey);
            update.setInt(3, current.getConversation_id());
            update.executeUpdate();
        }

        try (PreparedStatement reload = conn.prepareStatement(
                "SELECT conversation_id, conversation_uuid, encryption_key FROM conversations WHERE conversation_id = ?"
        )) {
            reload.setInt(1, current.getConversation_id());
            try (ResultSet rs = reload.executeQuery()) {
                if (rs.next()) {
                    return mapConversation(rs);
                }
            }
        }
        return new conversations(current.getConversation_id(), newUuid, newKey);
    }

    private void linkPair(Connection conn, int userLow, int userHigh, int conversationId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO conversation_pair (user_low, user_high, conversation_id) VALUES (?, ?, ?)"
        )) {
            ps.setInt(1, userLow);
            ps.setInt(2, userHigh);
            ps.setInt(3, conversationId);
            ps.executeUpdate();
        }
    }

    private void insertMember(Connection conn, int conversationId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO conversation_members (conversation_id, user_id) VALUES (?, ?)"
        )) {
            ps.setInt(1, conversationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private conversations mapConversation(ResultSet rs) throws SQLException {
        return new conversations(
                rs.getInt("conversation_id"),
                rs.getString("conversation_uuid"),
                rs.getString("encryption_key")
        );
    }
}
