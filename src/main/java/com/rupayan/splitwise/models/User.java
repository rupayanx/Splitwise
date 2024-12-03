package com.rupayan.splitwise.models;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.rupayan.splitwise.db.DatabaseConnection;

public class User {
    private int id;
    private String name;
    private String phoneNo;
    private String email;
    private String password;
    private double totalOwedAmount;
    private List<Expense> expenseHistory;

    public User(String name, String phoneNo, String email, String password) {
        this.name = name;
        this.phoneNo = phoneNo;
        this.email = email;
        this.password = password;
        this.totalOwedAmount = 0.0;
        this.expenseHistory = new ArrayList<>();
    }
    public User(int id, String name, String phoneNo, String email, String password, double totalOwedAmount) {
        this.id = id;
        this.name = name;
        this.phoneNo = phoneNo;
        this.email = email;
        this.password = password;
        this.totalOwedAmount = totalOwedAmount;
        this.expenseHistory = new ArrayList<>();
    }

    public boolean saveToDatabase() {
        String sql = "INSERT INTO users (name, phone_no, email, password, total_owed_amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, phoneNo);
            stmt.setString(3, email);
            stmt.setString(4, password);
            stmt.setDouble(5, totalOwedAmount);
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                this.id = generatedKeys.getInt(1);
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User fetchByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("phone_no"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDouble("total_owed_amount")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User fetchById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("phone_no"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDouble("total_owed_amount")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateProfile(String newName, String newPhoneNo, String newPassword) {
        this.name = newName;
        this.phoneNo = newPhoneNo;
        this.password = newPassword;
        
        String sql = "UPDATE users SET name = ?, phone_no = ?, password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newName);
            stmt.setString(2, newPhoneNo);
            stmt.setString(3, newPassword);
            stmt.setInt(4, id);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addOwedAmount(double amount) {
        this.totalOwedAmount += amount;
        String sql = "UPDATE users SET total_owed_amount = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, totalOwedAmount);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addExpense(Expense expense) {
        expenseHistory.add(expense);
        String sql = "INSERT INTO user_expenses (user_id, expense_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, expense.getExpenseId()); 
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public List<Expense> getExpenseHistory() {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT e.id, e.description, e.paid_by, e.amount FROM expenses e JOIN user_expenses ue ON e.id = ue.expense_id WHERE ue.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User paidBy = User.fetchById(rs.getInt("paid_by")); 
                Expense expense = new Expense(
                    rs.getInt("id"),
                    rs.getString("description"),
                    paidBy,
                    rs.getDouble("amount"),
                    new HashMap<>()
                );
                expenses.add(expense);
            }
            this.expenseHistory = expenses;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public String getPassword() {
        return password;
    }

    public double getTotalOwedAmount() {
        return totalOwedAmount;
    }
}



//package splitwise.models;
//import java.util.*;
//
//public class User {
//	private String name;
//	private String phone_no;
//	private String email;
//	private String password;
//	private double totalOwedAmount = 0.0;
//    private List<Expense> expenseHistory = new ArrayList<>();
//	
//	public User(String name,String phone_no,String email,String password) {
//		this.name=name;
//		this.phone_no=phone_no;
//		this.email=email;
//		this.password=password;
//		
//	}
//	
//	public String getEmail() {
//		return email;
//	}
//	public String getName() {
//        return name;
//    }
//
//    public String getPhoneNo() {
//        return phone_no;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//	
//	public void updateProfile(String newName, String newPhoneNo, String newPassword) {
//        this.name = newName;
//        this.phone_no = newPhoneNo;
//        this.password = newPassword;
//    }
//	
//	public void addOwedAmount(double amount) {
//        this.totalOwedAmount+=amount;
//    }
//
//    public double getTotalOwedAmount() {
//        return totalOwedAmount;
//    }
//
//    public void addExpense(Expense expense) {
//        expenseHistory.add(expense);
//    }
//
//    public List<Expense> getExpenseHistory() {
//        return new ArrayList<>(expenseHistory);
//    }
//	
//}