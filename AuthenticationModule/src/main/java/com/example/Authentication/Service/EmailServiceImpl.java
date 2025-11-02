package com.example.Authentication.Service;

import com.example.Authentication.Interface.EmailServiceInterface;
import com.example.Authentication.Model.Expert;
import com.example.common.Model.UserDetails1;
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
    @Override
    public void sendApprovalEmail(UserDetails1 user, String Status, Expert expert) {
        String subject = "🎉 Expert Verification Approved - AgriConnect";
        String htmlBody = createApprovalEmailHtml(user, Status,expert);
        sendHtmlEmail(user.getUserEmail(), subject, htmlBody);
    }

    @Override
    public void sendRejectionEmail(UserDetails1 user, String Status, String reason) {
        String subject = "Expert Verification Status - AgriConnect";
        String htmlBody = createRejectionEmailHtml(user,Status, reason);
        sendHtmlEmail(user.getUserEmail(), subject, htmlBody);
    }

    private String createApprovalEmailHtml(UserDetails1 user, String status, Expert expert) {
        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f0fdf4; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 30px rgba(34, 197, 94, 0.15); }" +
                "        .header { background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%); padding: 40px 30px; text-align: center; color: white; }" +
                "        .header-icon { font-size: 60px; margin-bottom: 15px; }" +
                "        .header h1 { margin: 0; font-size: 28px; font-weight: 700; }" +
                "        .content { padding: 40px 30px; }" +
                "        .greeting { font-size: 20px; color: #15803d; font-weight: 600; margin-bottom: 20px; }" +
                "        .message { font-size: 16px; color: #166534; line-height: 1.8; margin-bottom: 25px; }" +
                "        .info-box { background: #f0fdf4; border-left: 4px solid #22c55e; padding: 20px; border-radius: 8px; margin: 25px 0; }" +
                "        .info-item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #dcfce7; }" +
                "        .info-item:last-child { border-bottom: none; }" +
                "        .info-label { font-weight: 600; color: #15803d; }" +
                "        .info-value { color: #16a34a; }" +
                "        .success-badge { display: inline-block; background: #22c55e; color: white; padding: 12px 25px; border-radius: 25px; font-weight: 600; margin: 20px 0; }" +
                "        .cta-button { display: inline-block; background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 10px; font-weight: 600; margin: 20px 0; box-shadow: 0 4px 15px rgba(34, 197, 94, 0.3); }" +
                "        .features { margin: 30px 0; }" +
                "        .feature { display: flex; align-items: center; margin: 15px 0; }" +
                "        .feature-icon { background: #dcfce7; color: #16a34a; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 15px; font-size: 18px; }" +
                "        .feature-text { color: #166534; font-size: 15px; }" +
                "        .footer { background: #f9fafb; padding: 25px; text-align: center; color: #6b7280; font-size: 13px; border-top: 1px solid #e5e7eb; }" +
                "        .footer a { color: #16a34a; text-decoration: none; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <div class='header-icon'>🎉</div>" +
                "            <h1>Congratulations!</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p class='greeting'>Dear " + user.getUsername()+ ",</p>" +
                "            <p class='message'>" +
                "                We are thrilled to inform you that your expert verification application has been <strong>APPROVED</strong>! " +
                "                You are now a certified expert on AgriConnect platform." +
                "            </p>" +
                "            <div class='success-badge'>✓ VERIFIED EXPERT</div>" +
                "            <div class='info-box'>" +
                "                <h3 style='margin-top: 0; color: #15803d;'>Your Expert Profile</h3>" +
                "                <div class='info-item'>" +
                "                    <span class='info-label'>Field of Expertise:</span>" +
                "                    <span class='info-value'>" + expert.getField() + "</span>" +
                "                </div>" +
                "                <div class='info-item'>" +
                "                    <span class='info-label'>Organization:</span>" +
                "                    <span class='info-value'>" + (expert.getOrganization() != null ? expert.getOrganization() : "N/A") + "</span>" +
                "                </div>" +
                "                <div class='info-item'>" +
                "                    <span class='info-label'>Experience:</span>" +
                "                    <span class='info-value'>" + expert.getExperience_years() + "</span>" +
                "                </div>" +
                "            </div>" +
                "            <h3 style='color: #15803d; margin-top: 30px;'>What's Next?</h3>" +
                "            <div class='features'>" +
                "                <div class='feature'>" +
                "                    <div class='feature-icon'>📝</div>" +
                "                    <div class='feature-text'>Create and share expert content</div>" +
                "                </div>" +
                "                <div class='feature'>" +
                "                    <div class='feature-icon'>📊</div>" +
                "                    <div class='feature-text'>Post soil and crop reports</div>" +
                "                </div>" +
                "                <div class='feature'>" +
                "                    <div class='feature-icon'>🎥</div>" +
                "                    <div class='feature-text'>Upload educational tutorials</div>" +
                "                </div>" +
                "                <div class='feature'>" +
                "                    <div class='feature-icon'>💬</div>" +
                "                    <div class='feature-text'>Engage with farming community</div>" +
                "                </div>" +
                "            </div>" +
                "            <div style='text-align: center; margin-top: 30px;'>" +
                "                <a href='http://localhost:8080/community' class='cta-button'>Start Contributing Now</a>" +
                "            </div>" +
                "            <p class='message' style='margin-top: 30px;'>" +
                "                Thank you for joining AgriConnect as an expert. We look forward to your valuable contributions!" +
                "            </p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p><strong>AgriConnect Team</strong></p>" +
                "            <p>Connecting Farmers with Knowledge</p>" +
                "            <p>Need help? <a href='mailto:support@agriconnect.com'>Contact Support</a></p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    private String createRejectionEmailHtml(UserDetails1 user, String status, String reason) {
        String displayReason = (reason != null && !reason.isEmpty())
                ? reason
                : "The submitted credentials did not meet our verification requirements.";

        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #fef2f2; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 30px rgba(239, 68, 68, 0.15); }" +
                "        .header { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); padding: 40px 30px; text-align: center; color: white; }" +
                "        .header-icon { font-size: 60px; margin-bottom: 15px; }" +
                "        .header h1 { margin: 0; font-size: 28px; font-weight: 700; }" +
                "        .content { padding: 40px 30px; }" +
                "        .greeting { font-size: 20px; color: #92400e; font-weight: 600; margin-bottom: 20px; }" +
                "        .message { font-size: 16px; color: #78350f; line-height: 1.8; margin-bottom: 25px; }" +
                "        .warning-box { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 8px; margin: 25px 0; }" +
                "        .warning-box h3 { margin-top: 0; color: #92400e; }" +
                "        .warning-box p { color: #78350f; margin: 0; }" +
                "        .info-box { background: #f0fdf4; border-left: 4px solid #22c55e; padding: 20px; border-radius: 8px; margin: 25px 0; }" +
                "        .info-box h3 { margin-top: 0; color: #15803d; }" +
                "        .info-item { margin: 10px 0; color: #166534; }" +
                "        .cta-button { display: inline-block; background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 10px; font-weight: 600; margin: 20px 0; box-shadow: 0 4px 15px rgba(245, 158, 11, 0.3); }" +
                "        .footer { background: #f9fafb; padding: 25px; text-align: center; color: #6b7280; font-size: 13px; border-top: 1px solid #e5e7eb; }" +
                "        .footer a { color: #d97706; text-decoration: none; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <div class='header-icon'>📋</div>" +
                "            <h1>Verification Status:</h1>" + status +

                "        </div>" +
                "        <div class='content'>" +
                "            <p class='greeting'>Dear " + user.getUsername() + ",</p>" +
                "            <p class='message'>" +
                "                Thank you for applying for expert verification on AgriConnect platform. " +
                "                After careful review of your application, we regret to inform you that we are unable to approve your verification at this time." +
                "            </p>" +
                "            <div class='warning-box'>" +
                "                <h3>Reason for Rejection:</h3>" +
                "                <p>" + displayReason + "</p>" +
                "            </div>" +
                "            <div class='info-box'>" +
                "                <h3>What You Can Do:</h3>" +
                "                <div class='info-item'>✓ Review the rejection reason carefully</div>" +
                "                <div class='info-item'>✓ Update your credentials and documents</div>" +
                "                <div class='info-item'>✓ Reapply with complete and accurate information</div>" +
                "                <div class='info-item'>✓ Contact support if you have questions</div>" +
                "            </div>" +
                "            <p class='message'>" +
                "                We encourage you to reapply once you have the required credentials and documentation. " +
                "                Our verification process ensures the quality and credibility of experts on our platform." +
                "            </p>" +
                "            <div style='text-align: center; margin-top: 30px;'>" +
                "                <a href='http://localhost:8080/community' class='cta-button'>Reapply for Verification</a>" +
                "            </div>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p><strong>AgriConnect Team</strong></p>" +
                "            <p>Need assistance? <a href='mailto:support@agriconnect.com'>Contact Support</a></p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
    /**
     * Sends an acknowledgment email in HTML format to a user/expert
     * after marking their profile as pending review.
     *
     * @param toEmail  Recipient email address
     * @param userName Name of the user/expert
     */
    @Override
    public void sendPendingReviewAcknowledgement(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Profile Under Review - AgriConnect");

            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f7f8fa; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background-color: white; border-radius: 10px; box-shadow: 0 0 8px rgba(0,0,0,0.1); padding: 20px;">
                        <h2 style="color: #2d7a46;">Hello, %s 👋</h2>
                        <p style="font-size: 15px; color: #333;">
                            Thank you for your submission! Your profile has been marked as
                            <b>Pending Review</b> by our team.
                        </p>
                        <p style="font-size: 15px; color: #333;">
                            Our experts will verify your information shortly. You’ll receive another
                            email once your profile has been successfully reviewed and verified.
                        </p>
                        <div style="margin-top: 25px; text-align: center;">
                            <a href="https://agriconnect.in" style="background-color: #2d7a46; color: white; padding: 10px 20px; border-radius: 6px; text-decoration: none;">
                                Visit AgriConnect
                            </a>
                        </div>
                        <p style="margin-top: 20px; font-size: 13px; color: gray;">
                            This is an automated email. Please do not reply.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(userName);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Failed to send pending review acknowledgment: " + e.getMessage());
        }
    }
}
