package com.rupayan.splitwise.models;

import java.sql.*;
import java.util.*;

import com.rupayan.splitwise.db.DatabaseConnection;

public class Group {
    private int groupId; 
    private String name;
    private User creator;
    private List<User> members;

    public Group(String name, User creator) {
        this.name = name;
        this.creator = creator;
        this.members = new ArrayList<>();
        this.members.add(creator); 
    }
    public Group(int groupId, String groupName) {
        this.groupId = groupId;
        this.name = groupName;
        this.members = new ArrayList<>();
    }

    public int getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public User getCreator() {
        return creator;
    }

    public List<User> getMembers() {
        return members;
    }

    public boolean addMember(User user) {
        if (this.groupId <= 0) {
            System.out.println("Error: Invalid group ID. Please ensure the group is saved to the database first.");
            return false;
        }

        Connection conn = null;
        PreparedStatement checkGroupStmt = null;
        PreparedStatement checkMemberStmt = null;
        PreparedStatement memberStmt = null;
        ResultSet groupResult = null;
        ResultSet memberResult = null;
        ResultSet userResult = null;

        try {
            conn = DatabaseConnection.getConnection();
            String userSql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                userStmt.setString(1, user.getEmail());
                userResult = userStmt.executeQuery();

                if (userResult.next()) {
                    int userId = userResult.getInt("id");

                    String checkMemberSql = "SELECT COUNT(*) FROM user_group WHERE group_id = ? AND user_id = ?";
                    checkMemberStmt = conn.prepareStatement(checkMemberSql);
                    checkMemberStmt.setInt(1, this.groupId);
                    checkMemberStmt.setInt(2, userId);
                    memberResult = checkMemberStmt.executeQuery();

                    if (memberResult.next() && memberResult.getInt(1) == 0) {
                        String addMemberSql = "INSERT INTO user_group (group_id, user_id) VALUES (?, ?)";
                        memberStmt = conn.prepareStatement(addMemberSql);
                        memberStmt.setInt(1, this.groupId);
                        memberStmt.setInt(2, userId);
                        memberStmt.executeUpdate();
                        System.out.println("User " + user.getEmail() + " added to group " + this.name);
                        return true;
                    } else {
                        System.out.println("User " + user.getEmail() + " is already a member of this group.");
                        return false;
                    }
                } else {
                    System.out.println("Error: User with email " + user.getEmail() + " not found.");
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (groupResult != null) groupResult.close();
                if (userResult != null) userResult.close();
                if (memberResult != null) memberResult.close();
                if (checkGroupStmt != null) checkGroupStmt.close();
                if (checkMemberStmt != null) checkMemberStmt.close();
                if (memberStmt != null) memberStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



    public void removeMember(User user) {
        members.remove(user);
    }

    public boolean isCreator(User user) {
        return creator.getEmail().equalsIgnoreCase(user.getEmail());
    }

    public List<User> getAllMembers() {
        return new ArrayList<>(members); // Return a copy of the list of members
    }
    public boolean saveToDatabase() {
        String groupSql = "INSERT INTO groups (name, creator_id) VALUES (?, ?)";
        String memberSql = "INSERT INTO user_group (group_id, user_id) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement groupStmt = null;
        PreparedStatement memberStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Find creator_id based on the email
            String creatorEmail = creator.getEmail();
            String userSql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                userStmt.setString(1, creatorEmail);
                rs = userStmt.executeQuery();

                if (rs.next()) {
                    int creatorId = rs.getInt("id");

                    // Insert group into groups table
                    groupStmt = conn.prepareStatement(groupSql, Statement.RETURN_GENERATED_KEYS);
                    groupStmt.setString(1, name);
                    groupStmt.setInt(2, creatorId);
                    groupStmt.executeUpdate();

                    // Retrieve the generated group ID
                    try (ResultSet generatedKeys = groupStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            this.groupId = generatedKeys.getInt(1); // Store the group ID
                        } else {
                            throw new SQLException("Failed to retrieve group ID.");
                        }
                    }

                    // Insert the creator into the user_group table
                    memberStmt = conn.prepareStatement(memberSql);
                    memberStmt.setInt(1, groupId);
                    memberStmt.setInt(2, creatorId);
                    memberStmt.executeUpdate();

                    conn.commit(); 
                    return true;
                } else {
                    System.out.println("Error: Creator not found in the users table.");
                    return false;
                }
            }
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.out.println("SQLException: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (groupStmt != null) groupStmt.close();
                if (memberStmt != null) memberStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); 
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

 
    public static Group loadGroupByName(String groupName) {
        String groupSql = "SELECT * FROM groups WHERE name = ?";
        String memberSql = "SELECT user_id FROM user_group WHERE group_id = ?";
        
        Group group = null;
        Connection conn = null;
        PreparedStatement groupStmt = null;
        PreparedStatement memberStmt = null;
        ResultSet groupRs = null;
        ResultSet memberRs = null;

        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null || conn.isClosed()) {
                System.out.println("Connection is null or closed at the start!");
                return null;
            }

            groupStmt = conn.prepareStatement(groupSql);
            groupStmt.setString(1, groupName);
            groupRs = groupStmt.executeQuery();

            if (groupRs.next()) {
                int groupId = groupRs.getInt("id");
                String name = groupRs.getString("name");
                int creatorId = groupRs.getInt("creator_id");

                User creator = User.fetchById(creatorId);
                if (creator == null) {
                    System.out.println("Creator with ID " + creatorId + " not found!");
                    return null;
                }

                group = new Group(name, creator);
                group.groupId = groupId;

                System.out.println("Group " + name + " found with ID: " + groupId);

                memberStmt = conn.prepareStatement(memberSql);
                memberStmt.setInt(1, groupId);
                memberRs = memberStmt.executeQuery();

                while (memberRs.next()) {
                    int userId = memberRs.getInt("user_id");
                    User member = User.fetchById(userId);
                    if (member != null) {
                        group.addMember(member);
                        System.out.println("Added member " + member.getEmail() + " to the group.");
                    } else {
                        System.out.println("Error: User with ID " + userId + " not found.");
                    }
                }
            } else {
                System.out.println("No group found with name: " + groupName);
            }

        } catch (SQLException e) {
            System.out.println("SQLException occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (memberRs != null) memberRs.close();
                if (groupRs != null) groupRs.close();
                if (groupStmt != null) groupStmt.close();
                if (memberStmt != null) memberStmt.close();
                if (conn != null) {
                    conn.close(); 
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return group;
    }





    public static Group loadGroupById(int groupId) {
        String groupSql = "SELECT * FROM groups WHERE id = ?";
        String memberSql = "SELECT user_id FROM user_group WHERE group_id = ?"; 

        Group group = null;

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null || conn.isClosed()) {
                System.out.println("Connection is closed or null!");
                return null;
            }

            try (PreparedStatement groupStmt = conn.prepareStatement(groupSql)) {
                groupStmt.setInt(1, groupId);
                ResultSet groupRs = groupStmt.executeQuery();

                if (groupRs.next()) {
                    String name = groupRs.getString("name");
                    int creatorId = groupRs.getInt("creator_id");

                    User creator = User.fetchById(creatorId);

                    group = new Group(name, creator);
                    group.groupId = groupId;

                    try (PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
                        memberStmt.setInt(1, groupId); 
                        ResultSet memberRs = memberStmt.executeQuery();

                        while (memberRs.next()) {
                            int userId = memberRs.getInt("user_id");  
                            User member = User.fetchById(userId); 
                            group.addMember(member); 
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return group;
    }
}

