package com.rupayan.splitwise.models;

import java.sql.*;
import java.util.*;

import com.rupayan.splitwise.db.DatabaseConnection;

public class Expense {
    private int expenseId;  
    private String description;
    private User paidBy;
    private double amount;
    private Map<User, Double> amountOwedByUser;

    public Expense(String description, User paidBy, Double amount, Map<User, Double> amountOwedByUser) {
        this.description = description;
        this.paidBy = paidBy;
        this.amount = amount;
        this.amountOwedByUser = amountOwedByUser;
    }

    public Expense(int expenseId, String description, User paidBy, double amount, Map<User, Double> amountOwedByUser) {
        this.expenseId = expenseId;
        this.description = description;
        this.paidBy = paidBy;
        this.amount = amount;
        this.amountOwedByUser = amountOwedByUser;
    }
    public Expense(String description, User paidBy, double amount, Group group) {
        this.description = description;
        this.paidBy = paidBy;
        this.amount = amount;
        this.amountOwedByUser = new HashMap<>();

        // Initialize amountOwedByUser with group members
        for (User member : group.getMembers()) {
            if (!member.equals(paidBy)) { // Exclude the payer
                this.amountOwedByUser.put(member, 0.0); // Default owed amounts
            }
        }
    }


    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Map<User, Double> getAmountOwedByUser() {
        return amountOwedByUser;
    }

    public boolean saveToDatabase() {
        String expenseSql = "INSERT INTO expenses (description, paid_by, amount) VALUES (?, ?, ?)";
        String amountOwedSql = "INSERT INTO expense_owed (expense_id, user_email, amount_owed) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement expenseStmt = conn.prepareStatement(expenseSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement amountOwedStmt = conn.prepareStatement(amountOwedSql)) {

            expenseStmt.setString(1, description);
            expenseStmt.setString(2, paidBy.getEmail());
            expenseStmt.setDouble(3, amount);
            expenseStmt.executeUpdate();

            ResultSet generatedKeys = expenseStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                this.expenseId = generatedKeys.getInt(1); 
            } else {
                throw new SQLException("Failed to retrieve expense ID.");
            }

            for (Map.Entry<User, Double> entry : amountOwedByUser.entrySet()) {
                amountOwedStmt.setInt(1, expenseId);
                amountOwedStmt.setString(2, entry.getKey().getEmail());
                amountOwedStmt.setDouble(3, entry.getValue());
                amountOwedStmt.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Expense loadExpenseById(int expenseId) {
        String expenseSql = "SELECT * FROM expenses WHERE id = ?";
        String amountOwedSql = "SELECT user_email, amount_owed FROM expense_owed WHERE expense_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement expenseStmt = conn.prepareStatement(expenseSql);
             PreparedStatement amountOwedStmt = conn.prepareStatement(amountOwedSql)) {

            expenseStmt.setInt(1, expenseId);
            ResultSet expenseRs = expenseStmt.executeQuery();

            if (expenseRs.next()) {
                String description = expenseRs.getString("description");
                User paidBy = User.fetchByEmail(expenseRs.getString("paid_by"));
                double amount = expenseRs.getDouble("amount");

                Map<User, Double> amountOwedByUser = new HashMap<>();
                amountOwedStmt.setInt(1, expenseId);
                ResultSet owedRs = amountOwedStmt.executeQuery();
                while (owedRs.next()) {
                    User user = User.fetchByEmail(owedRs.getString("user_email"));
                    double owedAmount = owedRs.getDouble("amount_owed");
                    amountOwedByUser.put(user, owedAmount);
                }

                return new Expense(expenseId, description, paidBy, amount, amountOwedByUser);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Expense> loadExpensesByUser(User user) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT e.id FROM expenses e JOIN expense_owed eo ON e.id = eo.expense_id WHERE eo.user_email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getEmail());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int expenseId = rs.getInt("id");
                Expense expense = loadExpenseById(expenseId);
                if (expense != null) {
                    expenses.add(expense);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }
}


//package splitwise.models;
//import java.util.*;
//
//public class Expense{
//	private String description;
//	private User paidBy;
//	private double amount;
//	private Map<User,Double> amountOwedByUser;
//	
//	public Expense(String description,User paidBy,Double amount,Map<User,Double> amountOwedByUser) {
//		this.description=description;
//		this.paidBy=paidBy;
//		this.amount=amount;
//		this.amountOwedByUser=amountOwedByUser;
//	}
//	public User getPaidBy() {
//        return paidBy;
//    }
//
//    public double getAmount() {
//        return amount;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public Map<User, Double> getAmountOwedByUser() {
//        return amountOwedByUser;
//    }
//	
//}