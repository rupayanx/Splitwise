package com.rupayan.splitwise.validations;

public class inputValidation{
	public static boolean validatePhoneNo(String phoneNo) {
		return phoneNo != null && phoneNo.matches("\\d{10}");
	}
	
	public static boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }

    public static boolean validatePassword(String password) {
        return password != null && password.length() >= 6;
    }
	
	
}