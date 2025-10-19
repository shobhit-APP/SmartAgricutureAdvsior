package com.example.Authentication.Service;

import com.example.Authentication.Interface.EmailServiceInterface;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailServiceInterface {

    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder; // if you actually need it; otherwise remove

    /**
     * Validates an email address using a regex pattern.
     *
     * @param email the email to validate
     * @return true if valid
     */
    @Override
    public boolean checkEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Send OTP as an HTML email.
     *
     * @param email recipient email
     * @param otp   one time password
     * @return true if sent successfully
     */
    @Override
    public boolean sendOtp(String email, String otp) {
        if (!checkEmail(email)) {
            log.warn("Invalid email: {}", email);
            return false;
        }
        String subject = "Your OTP - Smart Agriculture Advisor";
        String html = buildOtpHtml(otp);
        return sendHtmlEmail(email, subject, html);
    }

    /**
     * Send password update confirmation (HTML).
     *
     * @param email recipient email
     */
    @Override
    public void sendPasswordUpdateConfirmation(String email) {
        if (!checkEmail(email)) {
            log.warn("Invalid email for password update confirmation: {}", email);
            return;
        }
        String subject = "Password Updated Successfully - Smart Agriculture Advisor";
        String html = "<!doctype html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;'>" +
                "<h2>Password Updated</h2>" +
                "<p>Dear User,</p>" +
                "<p>Your password has been successfully updated. If you did not perform this action, please contact support immediately.</p>" +
                "<p>Best regards,<br/>Smart Agriculture Advisor Team</p>" +
                "</div></body></html>";
        sendHtmlEmail(email, subject, html);
    }

    /**
     * Send account verification link (HTML).
     *
     * @param email            recipient email
     * @param verificationLink verification URL
     */
    @Override
    public void sendVerificationLink(String email, String verificationLink) {
        if (!checkEmail(email)) {
            log.warn("Invalid email for verification link: {}", email);
            return;
        }
        String subject = "Verify Your Account - Smart Agriculture Advisor";
        String html = buildVerificationHtml(verificationLink);
        sendHtmlEmail(email, subject, html);
    }

    /**
     * Sends a password reset OTP as HTML.
     *
     * @param email recipient email
     * @param otp   otp code
     * @return true if sent
     */
    @Override
    public boolean sendPasswordResetOtp(String email, String otp) {
        if (!checkEmail(email)) {
            log.warn("Invalid email for password reset OTP: {}", email);
            return false;
        }
        String subject = "Password Reset OTP - Smart Agriculture Advisor";
        String html = buildPasswordResetHtml(otp);
        return sendHtmlEmail(email, subject, html);
    }

    /**
     * Send a custom HTML email.
     *
     * @param email   recipient
     * @param subject subject
     * @param content html content
     * @return true if sent
     */
    @Override
    public boolean sendCustomEmail(String email, String subject, String content) {
        if (!checkEmail(email)) {
            log.warn("Invalid email for custom email: {}", email);
            return false;
        }
        return sendHtmlEmail(email, subject, content);
    }

    /**
     * Core method to send HTML email using MimeMessageHelper.
     *
     * @param to          recipient
     * @param subject     subject
     * @param htmlContent html body
     * @return true if sent
     */
    private boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("no-reply@smartagriadvisor.com");

            mailSender.send(message);
            log.info("HTML email sent to {}", to);
            return true;
        } catch (MessagingException mex) {
            log.error("MessagingException while sending email to {}: {}", to, mex.getMessage());
            return false;
        } catch (Exception ex) {
            log.error("Unexpected error while sending email to {}: {}", to, ex.getMessage());
            return false;
        }
    }


    private String buildOtpHtml(String otp) {
        return "<!doctype html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;'>" +
                "<h1 style='color:#1F2937;'>Your OTP Code</h1>" +
                "<p>Hi " + escapeHtml("User") + ",</p>" +
                "<p>Please use the following OTP to proceed:</p>" +
                "<div style='background:#F3F4F6;padding:20px;text-align:center;border-radius:8px;margin:20px 0;'>" +
                "<h2 style='margin:0;font-size:32px;letter-spacing:6px;'>" + escapeHtml(otp) + "</h2>" +
                "</div>" +
                "<p>This OTP will expire in " + escapeHtml("10 minutes") + ".</p>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "<p>Best regards,<br/>Smart Agriculture Advisor Team</p>" +
                "</div></body></html>";
    }

    private String buildVerificationHtml(String link) {
        return "<!doctype html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;'>" +
                "<h1 style='color:#4F46E5;'>Verify Your Email</h1>" +
                "<p>Hi " + escapeHtml("User") + ",</p>" +
                "<p>Click the button below to verify your account. This link will expire in " + escapeHtml("1 hour") + ".</p>" +
                "<div style='text-align:center;margin:20px 0;'>" +
                "<a href='" + escapeHtml(link) + "' style='display:inline-block;padding:12px 24px;border-radius:6px;text-decoration:none;background:#4F46E5;color:#fff;'>Verify Email</a>" +
                "</div>" +
                "<p>If you did not sign up, please ignore this message.</p>" +
                "<p>Best regards,<br/>Smart Agriculture Advisor Team</p>" +
                "</div></body></html>";
    }

    private String buildPasswordResetHtml(String otp) {
        return "<!doctype html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;'>" +
                "<h1 style='color:#DC2626;'>Password Reset Request</h1>" +
                "<p>Hi " + escapeHtml("User") + ",</p>" +
                "<p>You requested a password reset. Use this OTP to reset your password:</p>" +
                "<div style='background:#FEF2F2;padding:20px;text-align:center;border-radius:8px;margin:20px 0;border:2px solid #FCA5A5;'>" +
                "<h2 style='margin:0;font-size:32px;letter-spacing:6px;color:#DC2626;'>" + escapeHtml(otp) + "</h2>" +
                "</div>" +
                "<p>This OTP will expire in " + escapeHtml("10 minutes") + ".</p>" +
                "<p>If you didn't request this, please secure your account immediately.</p>" +
                "<p>Best regards,<br/>Smart Agriculture Advisor Team</p>" +
                "</div></body></html>";
    }

    /**
     * Minimal HTML escaping to avoid breaking the template.
     * For production use, prefer Apache Commons StringEscapeUtils or similar.
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
