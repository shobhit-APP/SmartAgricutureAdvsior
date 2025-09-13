//package com.example.Authentication.UTIL;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.SecretKeySpec;
//import java.util.Base64;
//
//@Component  // Make this class a Spring Bean
//public class MyEncryptionUtil {
//
//    @Value("${permission_secret_key}") // Inject the secret key from properties
//    private String secretKey;  // Non-static field to store the value
//
//    private static String SECRET_KEY;  // Static field to hold the injected key
//
//    // Initialize static SECRET_KEY after Spring injects the value
//    @PostConstruct
//    public void init() {
//        SECRET_KEY = secretKey;  // Set the static field from the injected value
//    }
//
//    public static String encrypt(String input) {
//        try {
//            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
//            Cipher cipher = Cipher.getInstance("AES");
//            cipher.init(Cipher.ENCRYPT_MODE, key);
//            byte[] enc = cipher.doFinal(input.getBytes());
//            return Base64.getEncoder().encodeToString(enc);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static String decrypt(String input) {
//        try {
//            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
//            Cipher cipher = Cipher.getInstance("AES");
//            cipher.init(Cipher.DECRYPT_MODE, key);
//            byte[] dec = cipher.doFinal(Base64.getDecoder().decode(input));
//            return new String(dec);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to decrypt permission: " + input, e);
//        }
//    }
//}
