package com.rupayan.splitwise.services;

import java.sql.*;

import java.util.*;

import com.rupayan.splitwise.db.DatabaseConnection;
import com.rupayan.splitwise.models.*;
//
import java.util.*;

public class ExpenseTracker {

    private Map<Integer, Group> groups = new HashMap<>(); 
    private Map<Integer, User> users = new HashMap<>();  
    private DatabaseConnection database; 
    
    public ExpenseTracker(DatabaseConnection db) {
        this.database = db;
    }

    // Method to add an expense
//    public String addExpense(int groupId, int userId, double amount, String description) {
//        // Retrieve the group and user by ID
//        Group group = groups.get(groupId);
//        User user = users.get(userId);
//
//        if (group == null || user == null) {
//            return "Invalid group or user.";
//        }
//
//        // Create a new expense and save to the database
//        Expense expense = new Expense(description, user, amount, group);
//        if (expense.saveToDatabase()) {
//            // Expense successfully saved, proceed with balance updates
//            double splitAmount = amount / group.getMembers().size(); // Split the expense equally among all members
//            
//            for (User member : group.getMembers()) {
//                if (member != user) { // Skip the user who paid
//                    updateBalance(member, group, -splitAmount); // Member owes the payer
//                }
//            }
//            
//            // Update payer's balance (payer is owed money)
//            updateBalance(user, group, amount - splitAmount * (group.getMembers().size() - 1)); 
//
//            return "Expense added successfully.";
//        } else {
//            return "Failed to add expense.";
//        }
//    }

 // Assuming the method to add expense is like this:
//    public String addExpense(int userId, double amount, String description, int groupId, List<String> involvedEmails) {
//        // Step 1: Validate the group
//        Group group = Group.loadGroupById(groupId);
//        if (group == null) {
//            return "Error: Group not found.";
//        }
//
//        // Step 2: Validate the payer
//        User payer = User.fetchById(userId);
////        if (payer == null || !group.getMembers().contains(payer)) {
////            return "Error: Payer not found or not a member of the group.";
////        }
//
//        // Step 3: Validate involved members
//        List<User> involvedMembers = new ArrayList<>();
////        for (String email : involvedEmails) {
////            User member = User.fetchByEmail(email);
////            if (member == null || !group.getMembers().contains(member)) {
////                return "Error: User with email " + email + " is not part of the group.";
////            }
////            involvedMembers.add(member);
////        }
//
//        // Add the payer to the involved members (if not already included)
//        if (!involvedMembers.contains(payer)) {
//            involvedMembers.add(payer);
//        }
//
//        // Step 4: Insert the expense into the database
//        String expenseSql = "INSERT INTO expenses (description, paid_by, amount, group_id) VALUES (?, ?, ?, ?)";
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement stmt = conn.prepareStatement(expenseSql, Statement.RETURN_GENERATED_KEYS)) {
//
//            stmt.setString(1, description);
//            stmt.setInt(2, userId);
//            stmt.setDouble(3, amount);
//            stmt.setInt(4, groupId);
//
//            int rowsAffected = stmt.executeUpdate();
//
//            if (rowsAffected > 0) {
//                ResultSet rs = stmt.getGeneratedKeys();
//                if (rs.next()) {
//                    int expenseId = rs.getInt(1); // Retrieve the generated expense ID
//
//                    // Step 5: Calculate split amount
//                    double splitAmount = amount / involvedMembers.size(); // Divide the amount evenly
//
//                    // Step 6: Update balances
//                    for (User member : involvedMembers) {
//                        if (member.getId() == userId) {
//                            // Payer's balance increases by the amount they paid minus their share
//                            updateBalance(member, group, amount - splitAmount);
//                        } else {
//                            // Other members' balances decrease by their share
//                            updateBalance(member, group, -splitAmount);
//                        }
//                    }
//
//                    return "Expense added successfully with ID: " + expenseId;
//                }
//            }
//            return "Error: Failed to add expense.";
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return "SQLException: " + e.getMessage();
//        }
//    }
//    
    public String addExpense(int payerId, double amount, String description, int groupId, List<String> involvedEmails) {
        User payer = User.fetchById(payerId);
        if (payer == null) {
            return "Error: Payer not found.";
        }

        Group group = null;
        if (groupId != -1) { 
            group = Group.loadGroupById(groupId);
            if (group == null) {
                return "Error: Group not found.";
            }
        }

        List<User> involvedMembers = new ArrayList<>();
        for (String email : involvedEmails) {
            User member = User.fetchByEmail(email);
            if (member == null) {
                return "Error: User with email " + email + " not found.";
            }
            involvedMembers.add(member);
        }


        if (!involvedMembers.contains(payer)) {
            involvedMembers.add(payer);
        }

  
        String expenseSql = "INSERT INTO expenses (description, paid_by, amount, group_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(expenseSql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, description);
            stmt.setInt(2, payerId);
            stmt.setDouble(3, amount);
            stmt.setInt(4, groupId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int expenseId = rs.getInt(1);

                    double splitAmount = amount / involvedMembers.size();
                    System.out.println("Split amount per member: " + splitAmount);

                    for (User member : involvedMembers) {
                        if (member.getId() == payerId) {
                            double payerBalanceChange = amount - splitAmount;
                            updateBalance(member, group, payerBalanceChange);
                        } else {
                            updateBalance(member, group, -splitAmount);
                        }
                    }

                    return "Expense added successfully with ID: " + expenseId;
                }
            }

            return "Error: Failed to add expense.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "SQLException: " + e.getMessage();
        }
    }



    private void updateBalance(User user, Group group, double amount) {
        String selectSql = "SELECT balance FROM balances WHERE user_id = ? AND group_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setInt(1, user.getId());
            selectStmt.setInt(2, group.getGroupId());
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                double currentBalance = rs.getDouble("balance");
                double newBalance = currentBalance + amount;

                String updateSql = "UPDATE balances SET balance = ? WHERE user_id = ? AND group_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, newBalance);
                    updateStmt.setInt(2, user.getId());
                    updateStmt.setInt(3, group.getGroupId());
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    public void settleUpIndividual(int groupId) {
        Group group = groups.get(groupId);
        if (group == null) {
            System.out.println("Group not found.");
            return;
        }

        Map<User, Double> balances = new HashMap<>();
        for (User member : group.getMembers()) {
            Balance balance = Balance.getBalance(member, group); 
            balances.put(member, balance != null ? balance.getBalance() : 0.0);
        }

        List<Transaction> transactions = new ArrayList<>();
        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            User user = entry.getKey();
            double balance = entry.getValue();
            if (balance < 0) {
                User creditor = findCreditor(balances);
                if (creditor != null) {
                    transactions.add(new Transaction(user, creditor, -balance));
                    updateBalance(user, group, -balance);
                    updateBalance(creditor, group, balance);
                }
            }
        }

        for (Transaction t : transactions) {
            System.out.println(t);
        }
    }

    public void settleUpGroup(int groupId) {
        Group group = Group.loadGroupById(groupId);
        if (group == null) {
            System.out.println("Error: Group not found.");
            return;
        }

        List<User> members = group.getMembers();
        System.out.println("Group members validated: " + members);

        Map<User, Double> balances = new HashMap<>();

        String sql = "SELECT user_id, balance FROM balances WHERE group_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, group.getGroupId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                double balance = rs.getDouble("balance");

                User user = User.fetchById(userId);
                if (user != null) {
                    balances.put(user, balance);
                    System.out.println("Balance for " + user.getName() + ": " + balance);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        List<User> creditors = new ArrayList<>();
        List<User> debtors = new ArrayList<>();

        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            if (entry.getValue() > 0) {
                creditors.add(entry.getKey());
            } else if (entry.getValue() < 0) {
                debtors.add(entry.getKey());
            }
        }

        System.out.println("Creditors: " + creditors);
        System.out.println("Debtors: " + debtors);

        if (creditors.isEmpty() && debtors.isEmpty()) {
            System.out.println("No transactions needed. Balances are already settled.");
            return;
        }

        for (User debtor : debtors) {
            double debtorBalance = balances.get(debtor);

            for (User creditor : creditors) {
                double creditorBalance = balances.get(creditor);

                if (debtorBalance == 0 || creditorBalance == 0) {
                    continue;
                }

                double settlementAmount = Math.min(-debtorBalance, creditorBalance);

                System.out.println(debtor.getName() + " pays " + settlementAmount + " to " + creditor.getName());

                updateBalance(debtor, group, settlementAmount);
                updateBalance(creditor, group, -settlementAmount);

                debtorBalance += settlementAmount;
                creditorBalance -= settlementAmount;

                balances.put(debtor, debtorBalance);
                balances.put(creditor, creditorBalance);
            }
        }

        System.out.println("Settlement completed for group.");
    }



    private User findCreditor(Map<User, Double> balances) {
        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            if (entry.getValue() > 0) {
                return entry.getKey();
            }
        }
        return null;
    }
    public int getGroupIdByName(String groupName) {
        String query = "SELECT id FROM groups WHERE name = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, groupName);  
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            } else {
                return -1; 
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1; 
        }
    }
    
    
}



