package com.rupayan.splitwise.models;

import java.sql.*;

import com.rupayan.splitwise.db.DatabaseConnection;

public class Balance {
    private int id;
    private User user;
    private Group group;
    private double balance;

  
    public Balance(User user, Group group, double balance) {
        this.user = user;
        this.group = group;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }


    public boolean saveToDatabase() {
        String balanceSql = "INSERT INTO balances (user_id, group_id, balance) VALUES (?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE balance = ?";
        Connection conn = null;
        PreparedStatement balanceStmt = null;

        try {
            conn = DatabaseConnection.getConnection(); 
            conn.setAutoCommit(false);

            balanceStmt = conn.prepareStatement(balanceSql);
            balanceStmt.setInt(1, user.getId());
            balanceStmt.setInt(2, group.getGroupId());
            balanceStmt.setDouble(3, balance);
            balanceStmt.setDouble(4, balance); 
            int rowsUpdated = balanceStmt.executeUpdate();
            conn.commit();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("SQLException: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (balanceStmt != null) balanceStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    DatabaseConnection.closeConnection(conn); 
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    
    public static double getBalance(int userId, int groupId) {
        String sql = "SELECT balance FROM balances WHERE user_id = ? AND group_id = ?";
        double balance = 0.0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    balance = rs.getDouble("balance");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error fetching balance: " + e.getMessage());
            e.printStackTrace();
        }

        return balance;
    }
    public static Balance getBalance(User user, Group group) {
        String sql = "SELECT * FROM balances WHERE user_id = ? AND group_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user.getId());
            stmt.setInt(2, group.getGroupId());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance"); 
                return new Balance(user, group, balance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; 
    }

    
    public double getBalanceForUser(User user) {
        String sql = "SELECT SUM(amount_owed) - SUM(amount_paid) AS balance " +
                     "FROM balances " +
                     "WHERE user_id = ? AND group_id = ?";

        double balance = 0.0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user.getId());
            stmt.setInt(2, group.getGroupId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                balance = rs.getDouble("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return balance;
    }



}
