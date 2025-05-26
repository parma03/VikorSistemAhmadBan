package com.example.vikorsistemahmadban.api;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCConnection {
    private static final String TAG = "MySQLConnection";

    // Database connection parameters
    private static final String DB_HOST = "10.0.2.2"; // untuk emulator Android
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "db_ahmadban";
    private static final String DB_USER = "parma03";
    private static final String DB_PASSWORD = "8056174";

    // Connection URL dengan parameter tambahan untuk keamanan dan kompatibilitas
    private static final String CONNECTION_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&useUnicode=true" +
                    "&characterEncoding=UTF-8" +
                    "&autoReconnect=true" +
                    "&useJDBCCompliantTimezoneShift=true" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC";

    public Connection getConnection() {
        Connection conn = null;
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            Log.d(TAG, "Attempting to connect to: " + CONNECTION_URL);

            // Establish connection
            conn = DriverManager.getConnection(CONNECTION_URL, DB_USER, DB_PASSWORD);

            if (conn != null && !conn.isClosed()) {
                Log.d(TAG, "Database connection successful!");
            }

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "MySQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            Log.e(TAG, "SQL Exception: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            Log.e(TAG, "Error Code: " + e.getErrorCode());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "General Exception: " + e.getMessage());
            e.printStackTrace();
        }

        return conn;
    }

    // Method untuk menutup koneksi dengan aman
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    Log.d(TAG, "Database connection closed successfully");
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error closing connection: " + e.getMessage());
            }
        }
    }

    // Method untuk test koneksi
    public boolean testConnection() {
        Connection conn = null;
        try {
            conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            Log.e(TAG, "Connection test failed: " + e.getMessage());
            return false;
        } finally {
            closeConnection(conn);
        }
    }
}