package com.example.Authentication.Service;

import com.example.Authentication.Interface.EmailServiceInterface;
import com.example.Authentication.Interface.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements EmailServiceInterface {

    @Autowired
    private JavaMailSender mailSender;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean checkEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    @Override
    public boolean sendOtp(String email, String otp) {
        try {
            if (!checkEmail(email)) {
                return false;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your OTP - Smart Agriculture Advisor");
            message.setText("Dear User,\n\nYour OTP is: " + otp
                    + "\n\nThis OTP is valid for 10 minutes."
                    + "\n\nBest Regards,\nSmart Agriculture Advisor Team");
            message.setFrom("no-reply@smartagriadvisor.com");

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void sendPasswordUpdateConfirmation(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Updated Successfully - Smart Agriculture Advisor");
        message.setText("Dear User,\n\nYour password has been successfully updated."
                + "\n\nIf you did not perform this action, please contact support immediately."
                + "\n\nBest Regards,\nSmart Agriculture Advisor Team");
        message.setFrom("no-reply@smartagriadvisor.com");

        mailSender.send(message);
    }

    @Override
    public void sendVerificationLink(String email, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Verify Your Account - Smart Agriculture Advisor");
        message.setText("Dear User,\n\n"
                + "Thank you for registering with Smart Agriculture Advisor.\n"
                + "To complete your registration, please verify your email by clicking the link below:\n\n"
                + verificationLink + "\n\n"
                + "This link will expire in 1 hour.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Best Regards,\n"
                + "Smart Agriculture Advisor Team");
        message.setFrom("no-reply@smartagriadvisor.com");

        mailSender.send(message);
    }
}
