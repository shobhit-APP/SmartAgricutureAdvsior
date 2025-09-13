package com.example.common.util;

/**
 * Utility class for password validation.
 * Provides methods to check if a password meets specific complexity requirements
 * and to generate detailed error messages for invalid passwords.
 */
public class PasswordUtil {

    /**
     * Validates a password against complexity rules:
     * <ul>
     *     <li>At least 8 characters long</li>
     *     <li>At least one uppercase letter (A-Z)</li>
     *     <li>At least one lowercase letter (a-z)</li>
     *     <li>At least one digit (0-9)</li>
     *     <li>At least one special character (@, $, !, %, *, ?, &)</li>
     * </ul>
     *
     * @param password The password to validate.
     * @return {@code true} if the password meets all complexity rules, {@code false} otherwise.
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
     * Validates a password and returns an error message if it does not meet complexity rules.
     * The rules checked are:
     * <ul>
     *     <li>Password cannot be null or empty</li>
     *     <li>At least 8 characters long</li>
     *     <li>At least one uppercase letter (A-Z)</li>
     *     <li>At least one lowercase letter (a-z)</li>
     *     <li>At least one digit (0-9)</li>
     *     <li>At least one special character (@, $, !, %, *, ?, &)</li>
     * </ul>
     *
     * @param password The password to validate.
     * @return A string containing an error message if the password is invalid, or {@code null} if the password is valid.
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