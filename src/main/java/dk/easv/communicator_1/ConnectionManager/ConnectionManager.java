package dk.easv.communicator_1.ConnectionManager;

import java.sql.Connection;

import java.sql.DriverManager;


public class ConnectionManager {



        private static final String URL = "jdbc:sqlite:../../DataGripProjects/default/identifier.sqlite";

        public static Connection connect() throws Exception {
            Connection connection = DriverManager.getConnection(URL);
            try (java.sql.Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000");
                stmt.execute("PRAGMA journal_mode = WAL");
            }
            return connection;
        }
    }

