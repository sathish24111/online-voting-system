package com.college.voting.utils;

import java.util.regex.Pattern;

public class PasswordValidator {

    // Min 8 characters, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return pattern.matcher(password).matches();
    }
}
