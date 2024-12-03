package com.rupayan.splitwise.services;

import java.sql.*;
import java.util.*;

import com.rupayan.splitwise.db.DatabaseConnection;
import com.rupayan.splitwise.models.*;
import com.rupayan.splitwise.validations.*;

public class UserRegistration {
    private Connection conn;

    public UserRegistration(DatabaseConnection dbConn) {
        this.conn = dbConn.getConnection();
    }

    public String registerUser(String name, String phoneNo, String email, String password) {
        if (!inputValidation.validatePhoneNo(phoneNo)) {
            return "Error: Phone number must be a 10-digit number.";
        }
        if (!inputValidation.validateEmail(email)) {
            return "Error: Email must contain '@'.";
        }
        if (!inputValidation.validatePassword(password)) {
            return "Error: Password must be at least 6 characters.";
        }

        String checkQuery = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "Error: A user with this email already exists.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Unable to verify existing users.";
        }

        // Register the new user
        String insertQuery = "INSERT INTO users (name, phone_no, email, password) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
            stmt.setString(1, name);
            stmt.setString(2, phoneNo);
            stmt.setString(3, email);
            stmt.setString(4, password);
            stmt.executeUpdate();
            return "User successfully registered.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Unable to register the user.";
        }
    }

    public User findUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                String phoneNo = rs.getString("phone_no");
                String password = rs.getString("password");
                return new User(name, phoneNo, email, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public double getTotalOwedAmount(String email) {
        User user = findUserByEmail(email);
        if (user != null) {
            return user.getTotalOwedAmount();
        }
        return 0.0;
    }

    public List<Expense> getUserExpenseHistory(String email) {
        User user = findUserByEmail(email);
        if (user != null) {
            return user.getExpenseHistory();
        }
        return new ArrayList<>();
    }

    public String updateUserProfile(String email, String newName, String newPhoneNo, String newPassword) {
        User user = findUserByEmail(email);
        if (user == null) {
            return "Error: User not found.";
        }

        String updateQuery = "UPDATE users SET name = ?, phone_no = ?, password = ? WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setString(1, newName);
            stmt.setString(2, newPhoneNo);
            stmt.setString(3, newPassword);
            stmt.setString(4, email);
            stmt.executeUpdate();
            return "Profile updated successfully.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Unable to update profile.";
        }
    }
}



//package registration;
//
//import java.util.*;
//import splitwise.models.*;
//import splitwise.validations.*;
//import splitwise.models.User;
//
//public class user_registration {
//	private List<User> users=new ArrayList<>();
//	
//	public String registerUser(String name,String phone_no,String email,String password) {
//		if (!inputValidation.validatePhoneNo(phone_no)) {
//            return "Error: Phone number must be a 10-digit number";
//        }
//        if (!inputValidation.validateEmail(email)) {
//            return "Error: Email must contain '@'";
//        }
//        if (!inputValidation.validatePassword(password)) {
//            return "Error: Password must be at least 6 characters";
//        }
//		
//		Iterator<User> iterator = users.iterator();
//		
//		while (iterator.hasNext()) {
//            User user = iterator.next();
//            if (user.getEmail().equalsIgnoreCase(email)) {
//                return "Error: A user with this email already exists.";
//            }
//        }
//		
//		User user=new User(name,phone_no,email,password);
//		users.add(user);
//		
//		return "User successfully registered";
//	}
//	
//	public User findUserByEmail(String email) {
//		Iterator<User> iterator = users.iterator();
//	    while (iterator.hasNext()) {
//	        User user = iterator.next();
//	        if (user.getEmail().equalsIgnoreCase(email)) {
//	            return user;
//	        }
//	    }
//	    return null;
//    }
//	
//	public double getTotalOwedAmount(String email) {
//	    User user = findUserByEmail(email);
//	    if (user != null) {
//	        return user.getTotalOwedAmount();
//	    } else {
//	        return 0.0;
//	    }
//	}
//
//	public List<Expense> getUserExpenseHistory(String email) {
//	    User user = findUserByEmail(email);
//	    if (user != null) {
//	        return user.getExpenseHistory();
//	    } else {
//	        return new ArrayList<>();
//	    }
//	}
//	
//	public String updateUserProfile(String email, String newName, String newPhoneNo, String newPassword) {
//	        User user = findUserByEmail(email);
//	        if (user != null) {
//	            user.updateProfile(newName, newPhoneNo, newPassword);
//	            return "Profile updated successfully.";
//	        } else {
//	            return "Error: User not found.";
//	        }
//	    }
//}