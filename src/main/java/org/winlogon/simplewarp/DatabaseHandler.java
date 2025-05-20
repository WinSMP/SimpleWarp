package org.winlogon.simplewarp;

import com.github.walker84837.JResult.Result;
import com.github.walker84837.JResult.ResultUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Handles the connection to the SQLite database, which stores the warps.

 * @author walker84837
 */
public class DatabaseHandler {
    private final File dataFolder;
    private Connection connection;

    public DatabaseHandler(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    /**
     * Connects to the SQLite database.

     * @return Result<Void, Exception>
     */
    public Result<Void, Exception> connectToDatabase() {
        return ResultUtils.tryCatch(() -> {
            String url = "jdbc:sqlite:" + this.dataFolder + "/warps.db";
            connection = DriverManager.getConnection(url);
            createTableIfNotExists().unwrap();
            return null;
        });
    }

    /**
     * Creates the warps table if it doesn't exist.

     * @return Result<Void, Exception>
     */
    private Result<Void, Exception> createTableIfNotExists() {
        return ResultUtils.tryCatch(() -> {
            String sql = """
                CREATE TABLE IF NOT EXISTS warps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    world TEXT NOT NULL)
                """;
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
            return null;
        });
    }

    /**
     * Retrieves a connection to the SQLite database.

     * @return Result<Connection, Exception>
     */
    public Result<Connection, Exception> getConnection() {
        return ResultUtils.tryCatch(() -> {
            if (connection == null || connection.isClosed()) {
                connectToDatabase().unwrap();
            }
            return connection;
        });
    }

    /**
     * Closes the connection to the SQLite database.

     * @return Result<Void, Exception>
     */
    public Result<Void, Exception> closeConnection() {
        return ResultUtils.tryCatch(() -> {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            return null;
        });
    }
}
