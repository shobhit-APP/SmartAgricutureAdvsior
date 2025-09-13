package com.example.common.util;

import com.example.common.Exception.AnyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class GeminiValidationService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiValidationService.class);

    private final GeminiApiHelper geminiApiHelper;

    public GeminiValidationService(GeminiApiHelper geminiApiHelper) {
        this.geminiApiHelper = geminiApiHelper;
    }

    public boolean isValidCropImage(MultipartFile file) throws AnyException {
        try {
            if (file == null || file.isEmpty()) {
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Uploaded file is empty");
            }

            logger.info("Validating image with Gemini API...");
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            // Build inline data for request
            Map<String, Object> inlineData = Map.of(
                    "mime_type", mimeType,
                    "data", base64Image
            );

            String textPrompt = "Is this image a crop or plant? Answer YES or NO.";
            String resultText = geminiApiHelper.callGeminiApiWithInlineData(textPrompt, inlineData);

            logger.info("Gemini validation result: {}", resultText);
            return resultText.trim().equalsIgnoreCase("YES") || resultText.toUpperCase().contains("YES");

        } catch (AnyException e) {
            logger.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to validate image with Gemini", e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to validate crop image");
        }
    }
}