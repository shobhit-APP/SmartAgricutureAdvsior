package com.example.Authentication.Service;

import com.example.Authentication.Interface.OtpService;
import com.example.Authentication.Model.Otpdata;
import com.example.Authentication.enums.OtpPurpose;
import com.example.Authentication.repository.OtpRepository;
import com.example.common.Exception.AnyException;
import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;

/**
 * Service implementation for handling OTP (One-Time Password) generation, verification, and sending.
 * Implements the {@link OtpService} interface to provide OTP-related functionality for login, registration,
 * and password reset purposes.
 */
@Slf4j
@Service
public class OtpServiceImpl implements OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.phoneNumber}")
    private String fromPhone;

    private final OtpRepository otpRepository;

    /**
     * Constructs an {@code OtpServiceImpl} with the specified OTP repository.
     *
     * @param otpRepository The {@link OtpRepository} used for storing and retrieving OTP data.
     */
    public OtpServiceImpl(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    /**
     * Generates and stores a 6-digit OTP for the given identifier and purpose.
     * Validates the identifier based on the OTP purpose (phone number for LOGIN, email for REGISTRATION or FORGOT_PASSWORD).
     * Stores the OTP with a 5-minute expiry in the repository.
     *
     * @param identifier The identifier (phone number or email) associated with the OTP.
     * @param purpose    The purpose of the OTP (e.g., LOGIN, REGISTRATION, FORGOT_PASSWORD).
     * @return The generated 6-digit OTP as a string.
     * @throws AnyException If the identifier is invalid or an error occurs during OTP generation/storage.
     */
    @Override
    @Transactional
    public String generateAndStoreOtp(String identifier, OtpPurpose purpose) {
        // Validate identifier based on purpose
        if (purpose == OtpPurpose.LOGIN && (identifier == null || identifier.contains("@"))) {
            logger.error("Invalid phone number for LOGIN OTP: {}", identifier);
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid phone number for login OTP");
        }
        if ((purpose == OtpPurpose.REGISTRATION || purpose == OtpPurpose.FORGOT_PASSWORD) &&
                (identifier == null || !identifier.contains("@"))) {
            logger.error("Invalid email for {} OTP: {}", purpose, identifier);
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid email for " + purpose + " OTP");
        }

        try {
            logger.info("Generating OTP for identifier: {}, purpose: {}", identifier, purpose);
            String otp = String.format("%06d", new Random().nextInt(900000) + 100000); // 6-digit OTP
            Otpdata otpData = new Otpdata();
            otpData.setOtp(otp);
            otpData.setPhoneNumber(purpose == OtpPurpose.LOGIN ? identifier : null); // Phone for LOGIN
            otpData.setEmail(purpose == OtpPurpose.REGISTRATION || purpose == OtpPurpose.FORGOT_PASSWORD ? identifier : null); // Email for REGISTRATION, FORGOT_PASSWORD
            otpData.setPurpose(purpose.name());
            otpData.setExpiryTime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).plusMinutes(5));
            otpRepository.save(otpData);
            logger.info("OTP generated and stored for identifier: {}", identifier);
            return otp;
        } catch (Exception e) {
            logger.error("Failed to generate OTP for identifier: {}, purpose: {}", identifier, purpose, e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to generate OTP: " + e.getMessage());
        }
    }

    /**
     * Verifies the provided OTP for the given identifier and purpose.
     * Checks if the OTP exists, is not expired, and matches the entered OTP.
     *
     * @param identifier  The identifier (phone number or email) associated with the OTP.
     * @param otpEntered  The OTP entered by the user.
     * @param purpose     The purpose of the OTP (e.g., LOGIN, REGISTRATION, FORGOT_PASSWORD).
     * @return {@code true} if the OTP is valid and not expired, {@code false} otherwise.
     */
    @Override
    @Transactional
    public boolean verifyOtp(String identifier, String otpEntered, OtpPurpose purpose) {
        if ((purpose == OtpPurpose.LOGIN || purpose == OtpPurpose.FORGOT_PASSWORD) &&
                (identifier == null || identifier.contains("@"))) {
            logger.error("Invalid phone number for {} OTP verification: {}", purpose, identifier);
            return false;
        }

        if (purpose == OtpPurpose.REGISTRATION &&
                (identifier == null || !identifier.contains("@"))) {
            logger.error("Invalid email for {} OTP verification: {}", purpose, identifier);
            return false;
        }

        try {
            logger.info("Verifying OTP for identifier: {}, purpose: {}", identifier, purpose);

            // Fetch OTP based on purpose
            Otpdata otpData = (purpose == OtpPurpose.LOGIN || purpose == OtpPurpose.FORGOT_PASSWORD)
                    ? otpRepository.findByPhoneNumberAndPurpose(identifier, purpose.name()).orElse(null)
                    : otpRepository.findByEmailAndPurpose(identifier, purpose.name()).orElse(null);

            if (otpData == null) {
                logger.warn("No OTP found for identifier: {}, purpose: {}", identifier, purpose);
                return false;
            }

            if (otpData.isExpired()) {
                logger.warn("OTP expired for identifier: {}, purpose: {}", identifier, purpose);
                otpRepository.delete(otpData);
                return false;
            }

            if (!otpData.getOtp().equals(otpEntered)) {
                logger.warn("Invalid OTP entered for identifier: {}, purpose: {}", identifier, purpose);
                return false;
            }
            return true;

        } catch (Exception e) {
            logger.error("OTP verification failed for identifier: {}, purpose: {}", identifier, purpose, e);
            return false;
        }
    }

    /**
     * Verifies the OTP for login purposes using the provided phone number.
     *
     * @param phoneNumber The phone number associated with the OTP.
     * @param otpEntered  The OTP entered by the user.
     * @return {@code true} if the OTP is valid and not expired, {@code false} otherwise.
     */
    @Override
    public boolean verifyLoginOtp(String phoneNumber, String otpEntered) {
        return verifyOtp(phoneNumber, otpEntered, OtpPurpose.LOGIN);
    }

    /**
     * Verifies the OTP for forgot password purposes using the provided phone number.
     *
     * @param phoneNumber The phone number associated with the OTP.
     * @param otpEntered  The OTP entered by the user.
     * @return {@code true} if the OTP is valid and not expired, {@code false} otherwise.
     */
    @Override
    public boolean verifyOtp(String phoneNumber, String otpEntered) {
        return verifyOtp(phoneNumber, otpEntered, OtpPurpose.FORGOT_PASSWORD);
    }

    /**
     * Deletes all OTPs associated with the given identifier (phone number or email).
     *
     * @param identifier The identifier (phone number or email) for which OTPs should be deleted.
     * @throws AnyException If an error occurs during OTP deletion.
     */
    @Override
    @Transactional
    public void deleteOtpByIdentifier(String identifier) {
        try {
            logger.info("Deleting OTP for identifier: {}", identifier);
            if (identifier.contains("@")) {
                otpRepository.deleteByEmail(identifier);
            } else {
                otpRepository.deleteByPhoneNumber(identifier);
            }
            logger.info("OTP deleted successfully for identifier: {}", identifier);
        } catch (Exception e) {
            logger.error("Failed to delete OTP for identifier: {}", identifier, e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete OTP: " + e.getMessage());
        }
    }

    /**
     * Sends an OTP to the specified phone number using Twilio's messaging service.
     *
     * @param toPhone The phone number to which the OTP should be sent.
     * @param otp     The OTP to be sent.
     * @throws AnyException If an error occurs while sending the OTP via Twilio or due to unexpected issues.
     */
    @Override
    public void sendOtp(String toPhone, String otp) {
        try {
            logger.info("Sending OTP to phone: {}", toPhone);
            Twilio.init(accountSid, authToken);
            String body = "Your OTP is: " + otp + " (valid for 5 minutes).";

            Message.creator(
                    new com.twilio.type.PhoneNumber(toPhone),
                    new com.twilio.type.PhoneNumber(fromPhone),
                    body
            ).create();

            logger.info("OTP sent successfully to phone: {}", toPhone);
        } catch (TwilioException e) {
            logger.error("Failed to send OTP to phone: {}. Twilio error: {}", toPhone, e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to send OTP: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending OTP to phone: {}. Error: {}", toPhone, e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error sending OTP: " + e.getMessage());
        }
    }
}