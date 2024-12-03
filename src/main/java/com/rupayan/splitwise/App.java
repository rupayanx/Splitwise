package com.rupayan.splitwise;
import java.sql.*;
import java.util.*;

import com.rupayan.splitwise.db.DatabaseConnection;
import com.rupayan.splitwise.models.*;
import com.rupayan.splitwise.services.*;
/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	Scanner scanner = new Scanner(System.in);

        DatabaseConnection dbConn = new DatabaseConnection();
        if (dbConn.getConnection() == null) {
            System.out.println("Database connection could not be established. Exiting application.");
            return;
        }

        
        UserRegistration register = new UserRegistration(dbConn);
        ExpenseTracker expense = new ExpenseTracker(dbConn);
     
        List<Group> groups = new ArrayList<>(); 

        while (true) {
            String input = scanner.nextLine();
            String[] tokens = input.split(" ", 2);
            String command = tokens[0];

            switch (command) {
                case "register":
                    System.out.print("Enter name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter phone number: ");
                    String phoneNumber = scanner.nextLine();
                    System.out.print("Enter email: ");
                    String email = scanner.nextLine();
                    System.out.print("Enter password: ");
                    String password = scanner.nextLine();

                    String newUser = register.registerUser(name, phoneNumber, email, password);
                    System.out.println(newUser);
                    break;

                case "update_profile":
                    System.out.print("Enter your email: ");
                    String updateRequesterEmail = scanner.nextLine();
                    User updateRequester = register.findUserByEmail(updateRequesterEmail);

                    if (updateRequester == null) {
                        System.out.println("User not found.");
                        break;
                    }

                    System.out.print("Enter new name (leave blank to keep current): ");
                    String newName = scanner.nextLine();
                    System.out.print("Enter new phone number (leave blank to keep current): ");
                    String newPhoneNo = scanner.nextLine();
                    System.out.print("Enter new password (leave blank to keep current): ");
                    String newPassword = scanner.nextLine();

                    if (newName.isEmpty()) newName = updateRequester.getName();
                    if (newPhoneNo.isEmpty()) newPhoneNo = updateRequester.getPhoneNo();
                    if (newPassword.isEmpty()) newPassword = updateRequester.getPassword();

                    String updateResult = register.updateUserProfile(updateRequesterEmail, newName, newPhoneNo, newPassword);
                    System.out.println(updateResult);
                    break;

                case "create_group":
                    if (tokens.length < 2) {
                        System.out.println("Enter a correct group name.");
                        break;
                    }

                    System.out.print("Enter email of the user creating the group: ");
                    String creatorEmail = scanner.nextLine();
                    User creator = register.findUserByEmail(creatorEmail);

                    if (creator == null) {
                        System.out.println("Creator user not found.");
                        break;
                    }

                    String groupName = tokens[1];
                    Group group = new Group(groupName, creator);

                    if (group.saveToDatabase()) {
                        groups.add(group); 
                        System.out.println("Group " + groupName + " created successfully by " + creator.getEmail());
                    } else {
                        System.out.println("Failed to save group " + groupName + " to the database.");
                    }
                    break;



                case "add_member":
                    System.out.print("Enter group name: ");
                    String addMemberGroupName = scanner.nextLine();
                    Group groupToAddMember = findGroupByName(groups, addMemberGroupName);

                    if (groupToAddMember == null) {
                        System.out.println("Group not found.");
                        break;
                    }

                    System.out.print("Enter email of the user adding the member: ");
                    String addMemberRequesterEmail = scanner.nextLine();
                    User addMemberRequester = register.findUserByEmail(addMemberRequesterEmail);

                    if (addMemberRequester == null || !groupToAddMember.isCreator(addMemberRequester)) {
                        System.out.println("Access denied: Only the creator can add members to this group.");
                        break;
                    }

                    System.out.print("Enter email of the user to add: ");
                    String newMemberEmail = scanner.nextLine();
                    User newMember = register.findUserByEmail(newMemberEmail);

                    if (newMember == null) {
                        System.out.println("User not found.");
                    } else {
                        boolean success = groupToAddMember.addMember(newMember);
                        if (success) {
                            System.out.println("User " + newMemberEmail + " added to group " + addMemberGroupName);
                        } else {
                            System.out.println("User " + newMemberEmail + " was not added to the group.");
                        }
                    }
                    break;




                case "view_group_members":
                    System.out.print("Enter group name: ");
                    String viewGroupName = scanner.nextLine();
                    Group viewGroup = findGroupByName(groups, viewGroupName);

                    if (viewGroup == null) {
                        System.out.println("Group not found.");
                    } else {
                        System.out.println("Members of group " + viewGroupName + ":");
                        for (User member : viewGroup.getAllMembers()) {
                            System.out.println("- " + member.getName());
                        }
                    }
                    break;

 
//                
                case "add_expense":
                    System.out.println("Enter your email:");
                    String payerEmail = scanner.nextLine();

                    System.out.println("Is this expense for (1) individual or (2) group?");
                    int expenseType = Integer.parseInt(scanner.nextLine());

                    System.out.println("Enter the description of the expense:");
                    String description = scanner.nextLine();

                    System.out.println("Enter the amount:");
                    double amount = Double.parseDouble(scanner.nextLine());

                    if (expenseType == 1) {
                        // Individual Expense
                        System.out.println("Enter the other user's email:");
                        String payeeEmail = scanner.nextLine();

                        User payer = User.fetchByEmail(payerEmail);
                        User payee = User.fetchByEmail(payeeEmail);

                        if (payer == null || payee == null) {
                            System.out.println("Error: One or both users not found.");
                            break;
                        }

                        // Add the expense for individual
                        String result = expense.addExpense(
                            payer.getId(),        // Payer's ID
                            amount,               // Expense amount
                            description,          // Description
                            -1,                   // No group ID (-1 indicates individual expense)
                            List.of(payeeEmail)   // Only the payee is involved
                        );
                        System.out.println(result);

                    } else if (expenseType == 2) {
                        // Group Expense
                        System.out.println("Enter the group name:");
                        String group_Name = scanner.nextLine();

                        Group grp = Group.loadGroupByName(group_Name);
                        if (grp == null) {
                            System.out.println("Error: Group not found.");
                            break;
                        }

                        User payer = User.fetchByEmail(payerEmail);
                        if (payer == null) {
                            System.out.println("Error: Payer not found.");
                            break;
                        }

                        System.out.println("Enter the emails of users involved in the transaction, separated by commas:");
                        String involvedEmailsInput = scanner.nextLine();
                        List<String> involvedEmails = Arrays.asList(involvedEmailsInput.split("\\s*,\\s*"));

                        // Add the expense for group
                        String result = expense.addExpense(
                            payer.getId(),        // Payer's ID
                            amount,               // Expense amount
                            description,          // Description
                            grp.getGroupId(),   // Group ID
                            involvedEmails        // List of involved users
                        );
                        System.out.println(result);

                    } else {
                        System.out.println("Invalid expense type selected.");
                    }
                    break;





//                case "settle_up_individual":
//                    System.out.println("Enter the email of User 1:");
//                    String email1 = scanner.next();
//                    System.out.println("Enter the email of User 2:");
//                    String email2 = scanner.next();
//
//                    User user1 = User.fetchByEmail(email1);
//                    User user2 = User.fetchByEmail(email2);
//
//                    if (user1 == null || user2 == null) {
//                        System.out.println("Error: One or both users not found.");
//                        break;
//                    }
//
//                    // Settle up between the two users
//                    expense.settleUpIndividual(user1, user2);
//                    break;

                case "settle_up_group":
                    System.out.print("Enter Group Name: ");
                    String group_Name = scanner.nextLine(); 
                    int groupId = expense.getGroupIdByName(group_Name); 
                  

                    if (groupId == -1) { 
                        System.out.println("Error: Group not found.");
                        break;
                    }


                    Group grp = Group.loadGroupById(groupId);
                    if (grp == null) {
                        System.out.println("Error: Group not found.");
                        break;
                    }

                    expense.settleUpGroup(groupId); 


                    

                    break;















                case "exit":
                    System.out.println("Exit Application");
                   // dbConn.closeConnection(); 
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid input");
                    break;
                    
                    
            }
            
        }
    }

    public static Group findGroupByName(List<Group> groups, String groupName) {
        for (Group group : groups) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                return group;
            }
        }

        Group groupFromDatabase = Group.loadGroupByName(groupName);
        if (groupFromDatabase != null) {
            groups.add(groupFromDatabase);
        }
        return groupFromDatabase;
    }

}
    	
