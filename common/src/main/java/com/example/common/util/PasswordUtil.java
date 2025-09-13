package com.example.common.util;

public class PasswordUtil {

    /**
     * Validate password with rules:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character (@, #, $, %, etc.)
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }

        String passwordRegex =
                "^(?=.*[a-z])" +        // at least one lowercase
                        "(?=.*[A-Z])" +        // at least one uppercase
                        "(?=.*\\d)" +          // at least one digit
                        "(?=.*[@$!%*?&])" +    // at least one special character
                        ".{8,}$";              // at least 8 characters

        return password.matches(passwordRegex);
    }

    /**
     * Returns a message if password is invalid, otherwise null.
     */
    public static String validatePasswordMessage(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit";
        }
        if (!password.matches(".*[@$!%*?&].*")) {
            return "Password must contain at least one special character (@, $, !, %, *, ?, &)";
        }
        return null;
    }
}
