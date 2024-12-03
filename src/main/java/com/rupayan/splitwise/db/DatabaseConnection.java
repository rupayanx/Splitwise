package com.rupayan.splitwise.db;

import java.sql.*;

public class DatabaseConnection {
    // This will give us a fresh connection every time
    public static Connection getConnection() {
        Connection conn = null;
        try {
            String userName = "rupayan";
            String password = "rupayan@123456";
            String url = "jdbc:sqlserver://localhost:1433;databaseName=splitwise";

            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            conn = DriverManager.getConnection(url, userName, password);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Cannot connect to database server");
            e.printStackTrace();
        }
        return conn;  // Return a fresh connection
    }
    
    // Optional: If needed, you can close a connection after you're done with it
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing the database connection");
                e.printStackTrace();
            }
        }
    }
}
