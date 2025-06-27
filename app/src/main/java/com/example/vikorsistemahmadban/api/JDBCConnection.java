package com.example.vikorsistemahmadban.api;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCConnection {
    private static final String TAG = "MySQLConnection";

    // Database connection parameters
    private static final String EMULATOR_HOST = "10.0.2.2";
    private static final String REAL_DEVICE_HOST  = "192.168.93.36";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "db_ahmadban";
    private static final String DB_USER = "parma03";
    private static final String DB_PASSWORD = "8056174Bo$";

    // Method untuk mendeteksi apakah berjalan di emulator atau device fisik
    private static boolean isEmulator() {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT));
    }

    // Method untuk mendapatkan host yang tepat
    private static String getDbHost() {
        String host = isEmulator() ? EMULATOR_HOST : REAL_DEVICE_HOST;
        Log.d(TAG, "Running on: " + (isEmulator() ? "Emulator" : "Real Device"));
        Log.d(TAG, "Using host: " + host);
        return host;
    }

    // Method untuk membuat connection URL
    private static String getConnectionUrl() {
        String host = getDbHost();
        return "jdbc:mysql://" + host + ":" + DB_PORT + "/" + DB_NAME +
                "?useSSL=false" +
                "&allowPublicKeyRetrieval=true" +
                "&useUnicode=true" +
                "&characterEncoding=UTF-8" +
                "&autoReconnect=true" +
                "&useJDBCCompliantTimezoneShift=true" +
                "&useLegacyDatetimeCode=false" +
                "&serverTimezone=UTC";
    }

    public Connection getConnection() {
        Connection conn = null;
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            String connectionUrl = getConnectionUrl();
            Log.d(TAG, "Attempting to connect to: " + connectionUrl);

            // Establish connection
            conn = DriverManager.getConnection(connectionUrl, DB_USER, DB_PASSWORD);

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

    // Method alternatif untuk manual override host
    public Connection getConnection(String customHost) {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String connectionUrl = "jdbc:mysql://" + customHost + ":" + DB_PORT + "/" + DB_NAME +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&useUnicode=true" +
                    "&characterEncoding=UTF-8" +
                    "&autoReconnect=true" +
                    "&useJDBCCompliantTimezoneShift=true" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC";

            Log.d(TAG, "Attempting to connect to custom host: " + connectionUrl);

            conn = DriverManager.getConnection(connectionUrl, DB_USER, DB_PASSWORD);

            if (conn != null && !conn.isClosed()) {
                Log.d(TAG, "Database connection successful with custom host!");
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

    // Method untuk test koneksi dengan custom host
    public boolean testConnection(String customHost) {
        Connection conn = null;
        try {
            conn = getConnection(customHost);
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            Log.e(TAG, "Connection test failed with custom host: " + e.getMessage());
            return false;
        } finally {
            closeConnection(conn);
        }
    }

    // Method untuk mendapatkan informasi koneksi saat ini
    public String getConnectionInfo() {
        return "Device Type: " + (isEmulator() ? "Emulator" : "Real Device") +
                "\nHost: " + getDbHost() +
                "\nPort: " + DB_PORT +
                "\nDatabase: " + DB_NAME;
    }
}