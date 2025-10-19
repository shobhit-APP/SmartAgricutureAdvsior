package com.example.Authentication.Interface;

/**
 * Interface defining all email-related operations for authentication
 * and user communication in the Smart Agriculture Advisor system.
 */
public interface EmailServiceInterface {

    /**
     * Validates whether an email address is syntactically correct.
     *
     * @param email the email address to validate
     * @return true if valid, false otherwise
     */
    boolean checkEmail(String email);

    /**
     * Sends an OTP (One-Time Password) to the user for verification or login.
     *
     * @param email the recipient email address
     * @param otp   the one-time password
     * @return true if the email was sent successfully, false otherwise
     */
    boolean sendOtp(String email, String otp);

    /**
     * Sends a password reset OTP email to the user.
     *
     * @param email the recipient email address
     * @param otp   the OTP to reset the password
     * @return true if sent successfully, false otherwise
     */
    boolean sendPasswordResetOtp(String email, String otp);

    /**
     * Sends a verification link email for account activation.
     *
     * @param email            the recipient email address
     * @param verificationLink the verification link to be included
     */
    void sendVerificationLink(String email, String verificationLink);

    /**
     * Sends a confirmation email after password update.
     *
     * @param email the recipient email address
     */
    void sendPasswordUpdateConfirmation(String email);

    /**
     * Sends a custom HTML email with a given subject and content.
     *
     * @param email   the recipient email address
     * @param subject the subject of the email
     * @param content the HTML content of the email
     * @return true if sent successfully, false otherwise
     */
    boolean sendCustomEmail(String email, String subject, String content);
}
