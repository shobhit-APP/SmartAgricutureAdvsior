package com.example.agriconnect.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.Authentication.repository.UserRepo;
import com.example.agriconnect.Repository.CropDiseaseRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.CropDisease;
import com.example.common.util.GeminiApiHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Service class for analyzing crop images to identify diseases and provide detailed analysis.
 * Integrates with Cloudinary for image storage, a Flask API for image analysis, and Gemini AI for
 * enhancing analysis results. Supports localization in English and Hindi, and saves results to the
 * database. Provides methods for image upload, analysis, status checking, and cleanup of old images.
 */
@Slf4j
@Service
public class CropImageAnalysisService {

    @Value("${flask.api.url3:http://localhost:8082}")
    private String flaskApiUrl3;

    @Autowired
    private CropDiseaseRepo cropDiseaseRepository;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GeminiApiHelper geminiApiHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    private final Cloudinary cloudinary;
    private final List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "jfif", "heic");

    /**
     * Constructs a new {@code CropImageAnalysisService} with Cloudinary configuration.
     *
     * @param cloudName the Cloudinary cloud name
     * @param apiKey    the Cloudinary API key
     * @param apiSecret the Cloudinary API secret
     */
    public CropImageAnalysisService(
            @Value("${cloudinary.cloud.name}") String cloudName,
            @Value("${cloudinary.api.key}") String apiKey,
            @Value("${cloudinary.api.secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    /**
     * Analyzes an uploaded crop image to identify diseases and provides detailed results.
     * Validates the image, uploads it to Cloudinary, sends it to a Flask API for analysis,
     * enhances results with Gemini AI, saves the analysis to the database, and returns
     * formatted results in English and Hindi.
     *
     * @param file   the image file to analyze (must be in allowed formats: jpg, jpeg, png, jfif, heic)
     * @param userId the ID of the user associated with the analysis
     * @return a {@link Map} containing analysis results (HTML and plain text in English and Hindi),
     *         image URL, filename, public ID, and success status
     * @throws IOException      if an error occurs during file processing
     * @throws AnyException     if the image is invalid, the user is not found, or analysis fails
     */
    public Map<String, Object> analyzeImage(MultipartFile file, Long userId) throws IOException {
        Map<String, Object> response;
        if (!isValidImageFile(file)) {
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Only image files are allowed.");
        }

        String filename = generateUniqueFilename(file.getOriginalFilename());

        try {
            // Upload to Cloudinary
            String publicId = "crop-images/" + filename;
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "folder", "crop-images"
                    ));
            String imageUrl = (String) uploadResult.get("secure_url");

            // Call Flask API with MultipartFile
            String flaskResponse = sendToFlaskAPI(file);
            JsonNode jsonNode = objectMapper.readTree(flaskResponse);

            // Process analysis
            response = processImageAnalysis(jsonNode, userId, imageUrl);
            response.put("imageUrl", imageUrl);
            response.put("filename", filename);
            response.put("publicId", publicId);

            log.info("Uploaded image to Cloudinary: publicId={}, url={}", publicId, imageUrl);

            return response;
        } catch (Exception e) {
            log.error("Error while analyzing image: {}", e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Image analysis service failed.");
        }
    }

    /**
     * Validates whether the provided file is a valid image file based on its extension.
     *
     * @param file the image file to validate
     * @return {@code true} if the file is non-empty and has an allowed extension (jpg, jpeg, png, jfif, heic),
     *         {@code false} otherwise
     */
    private boolean isValidImageFile(MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            return false;
        }
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        return allowedExtensions.contains(extension);
    }

    /**
     * Extracts the file extension from the provided filename.
     *
     * @param filename the name of the file
     * @return the file extension (without the dot), or an empty string if no extension is found
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * Generates a unique filename for the uploaded image by combining a UUID with the original file extension.
     *
     * @param originalFilename the original name of the uploaded file
     * @return a unique filename in the format {@code UUID.extension}
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + extension;
    }

    /**
     * Sends the image file to a Flask API for disease analysis.
     *
     * @param file the image file to send for analysis
     * @return the JSON response from the Flask API as a string
     * @throws AnyException if the API call fails or returns a non-200 status
     */
    private String sendToFlaskAPI(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    flaskApiUrl3 + "/analysis",
                    requestEntity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new AnyException(HttpStatus.BAD_GATEWAY.value(), "Image analysis API failed with status " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error calling Flask API: {}", e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to call image analysis API.");
        }
    }

    /**
     * Retrieves the analysis status for an image based on its filename.
     *
     * @param filename the unique filename of the image
     * @return the status of the analysis ("completed" if the image exists in Cloudinary, or "error.image_analysis_no_file" if not)
     */
    public String getAnalysisStatus(String filename) {
        try {
            String publicId = "crop-images/" + filename;
            Map result = cloudinary.api().resource(publicId, ObjectUtils.asMap("resource_type", "image"));
            return result != null ? "completed" : "error.image_analysis_no_file";
        } catch (Exception e) {
            log.error("Error checking status for filename: {}", filename, e);
            return "error.image_analysis_no_file";
        }
    }

    /**
     * Deletes images from Cloudinary that are older than the specified number of days.
     *
     * @param daysOld the age threshold (in days) for images to be deleted
     */
    public void cleanupOldFiles(int daysOld) {
        try {
            Map<String, Object> listParams = ObjectUtils.asMap(
                    "prefix", "crop-images/",
                    "resource_type", "image",
                    "max_results", 500
            );
            Map resources = cloudinary.api().resources(listParams);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resourceList = (List<Map<String, Object>>) resources.get("resources");

            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);

            for (Map<String, Object> resource : resourceList) {
                String createdAtStr = (String) resource.get("created_at");
                long createdAt = java.time.ZonedDateTime.parse(createdAtStr).toInstant().toEpochMilli();
                if (createdAt < cutoffTime) {
                    String publicId = (String) resource.get("public_id");
                    try {
                        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                        log.info("Deleted old image from Cloudinary: {}", publicId);
                    } catch (Exception e) {
                        log.error("Failed to delete image: {}", publicId, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning up old images in Cloudinary: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes the JSON response from the Flask API, enhances it with Gemini AI, saves the results to the database,
     * and generates formatted analysis results in English and Hindi.
     *
     * @param jsonNode the JSON response from the Flask API
     * @param userId   the ID of the user associated with the analysis
     * @param imageUrl the Cloudinary URL of the uploaded image
     * @return a {@link Map} containing formatted analysis results (HTML and plain text in English and Hindi),
     *         success status, and other metadata
     * @throws AnyException if the JSON response is invalid, the user is not found, or processing fails
     */
    public Map<String, Object> processImageAnalysis(JsonNode jsonNode, Long userId, String imageUrl) {
        Map<String, Object> response = new HashMap<>();
        try {
            JsonNode info = jsonNode.get("info");
            if (info == null) {
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid analysis response format.");
            }

            String originalCauseEn = getNodeValue(info, "cause_en", "Unknown cause");
            String originalCauseHi = getNodeValue(info, "cause_hi", "अज्ञात कारण");
            String originalNameEn = getNodeValue(info, "name_en", "Unknown disease");
            String originalNameHi = getNodeValue(info, "name_hi", "अज्ञात रोग");
            String originalSuggestionEn = getNodeValue(info, "suggestion_en", "No specific treatment suggestion available.");
            String originalSuggestionHi = getNodeValue(info, "suggestion_hi", "कोई विशिष्ट उपचार सुझाव उपलब्ध नहीं।");
            String originalSymptomsEn = getNodeValue(info, "symptoms_en", "No specific symptoms detected");
            String originalSymptomsHi = getNodeValue(info, "symptoms_hi", "कोई विशिष्ट लक्षण नहीं पाया गया");
            String originalCropTypeEn = getNodeValue(info, "crop_type_en", null);
            String originalCropTypeHi = getNodeValue(info, "crop_type_hi", null);

            // Enhance with Gemini AI
            Map<String, String> enhancedData = tryEnhanceWithGemini(originalNameEn, originalCropTypeEn, originalCropTypeHi);

            String finalCauseEn = enhancedData.getOrDefault("cause_en", originalCauseEn);
            String finalCauseHi = enhancedData.getOrDefault("cause_hi", originalCauseHi);
            String finalNameEn = enhancedData.getOrDefault("name_en", originalNameEn);
            String finalNameHi = enhancedData.getOrDefault("name_hi", originalNameHi);
            String finalSuggestionEn = enhancedData.getOrDefault("suggestion_en", originalSuggestionEn);
            String finalSuggestionHi = enhancedData.getOrDefault("suggestion_hi", originalSuggestionHi);
            String finalSymptomsEn = enhancedData.getOrDefault("symptoms_en", originalSymptomsEn);
            String finalSymptomsHi = enhancedData.getOrDefault("symptoms_hi", originalSymptomsHi);
            String finalCropTypeEn = enhancedData.getOrDefault("crop_type_en", originalCropTypeEn != null ? originalCropTypeEn : "Unknown");
            String finalCropTypeHi = enhancedData.getOrDefault("crop_type_hi", originalCropTypeHi != null ? originalCropTypeHi : "अज्ञात");

            // Generate HTML and plain text results
            response.put("analysisResultEn", generateResultHtml(finalNameEn, finalCauseEn, finalSymptomsEn, finalSuggestionEn, "en"));
            response.put("analysisResultHi", generateResultHtml(finalNameHi, finalCauseHi, finalSymptomsHi, finalSuggestionHi, "hi"));
            response.put("analysisTextEn", generatePlainTextResult(finalNameEn, finalCauseEn, finalSymptomsEn, finalSuggestionEn, finalCropTypeEn));
            response.put("analysisTextHi", generatePlainTextResult(finalNameHi, finalCauseHi, finalSymptomsHi, finalSuggestionHi, finalCropTypeHi));

            // Save to database
            CropDisease disease = new CropDisease();
            disease.setNameEn(finalNameEn);
            disease.setNameHi(finalNameHi);
            disease.setCauseEn(finalCauseEn);
            disease.setCauseHi(finalCauseHi);
            disease.setSymptomsEn(finalSymptomsEn);
            disease.setSymptomsHi(finalSymptomsHi);
            disease.setSuggestionEn(finalSuggestionEn);
            disease.setSuggestionHi(finalSuggestionHi);
            disease.setCropTypeEn(finalCropTypeEn);
            disease.setCropTypeHi(finalCropTypeHi);
            disease.setImagePath(imageUrl);
            disease.setUserDetails1(userRepo.findById(userId).orElseThrow(() ->
                    new AnyException(HttpStatus.NOT_FOUND.value(), "User not found")));
            cropDiseaseRepository.save(disease);
            log.info("Saved disease info: {}, cropTypeEn: {}, cropTypeHi: {}, imagePath: {}", finalNameEn, finalCropTypeEn, finalCropTypeHi, imageUrl);

            // Mobile-friendly response
            response.put("success", true);
            response.put("showResults", true);
            response.put("message", "Analysis completed successfully");
            response.put("diseaseName", finalNameEn);
            response.put("cropTypeEn", finalCropTypeEn);
            response.put("cropTypeHi", finalCropTypeHi);
            response.put("imageUrl", imageUrl);

        } catch (Exception e) {
            log.error("Error processing image analysis: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("showResults", false);
            response.put("error", "Failed to process image analysis: " + e.getMessage());
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to process image analysis.");
        }
        return response;
    }

    /**
     * Enhances disease analysis data using the Gemini AI API, falling back to original data if enhancement fails.
     *
     * @param diseaseName the name of the disease to enhance
     * @param cropTypeEn  the crop type in English (optional)
     * @param cropTypeHi  the crop type in Hindi (optional)
     * @return a {@link Map} containing enhanced disease information, or an empty map if enhancement fails
     */
    private Map<String, String> tryEnhanceWithGemini(String diseaseName, String cropTypeEn, String cropTypeHi) {
        try {
            if (diseaseName == null || diseaseName.trim().isEmpty() || "Unknown disease".equals(diseaseName)) {
                return new HashMap<>();
            }

            String prompt = buildEnhancementPrompt(diseaseName, cropTypeEn, cropTypeHi);
            String generatedText = geminiApiHelper.callGeminiApiForJson(prompt);

            Map<String, String> enhancedData = objectMapper.readValue(generatedText,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});

            if (validateEnhancedData(enhancedData)) {
                log.info("Successfully enhanced disease info with Gemini for: {}", diseaseName);
                return enhancedData;
            } else {
                log.warn("Gemini response incomplete for disease: {}, using original data", diseaseName);
                return new HashMap<>();
            }

        } catch (Exception e) {
            log.warn("Failed to enhance with Gemini for disease: {}, using original data. Error: {}",
                    diseaseName, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Builds a prompt for the Gemini AI API to enhance disease information.
     *
     * @param diseaseName the name of the disease to enhance
     * @param cropTypeEn  the crop type in English (optional)
     * @param cropTypeHi  the crop type in Hindi (optional)
     * @return a formatted prompt string for the Gemini AI API
     */
    private String buildEnhancementPrompt(String diseaseName, String cropTypeEn, String cropTypeHi) {
        String plantContext = (cropTypeEn != null && !cropTypeEn.trim().isEmpty()) ?
                " affecting " + cropTypeEn : "";

        return String.format(
                "Provide detailed information about the plant disease '%s'%s. " +
                        "Return a JSON object with exactly these fields:\n" +
                        "{\n" +
                        "  \"name_en\": \"Disease name in English\",\n" +
                        "  \"name_hi\": \"Disease name in Hindi (देवनागरी script)\",\n" +
                        "  \"cause_en\": \"Detailed cause/pathogen information in English (3-4 sentences)\",\n" +
                        "  \"cause_hi\": \"Detailed cause/pathogen information in Hindi (3-4 sentences)\",\n" +
                        "  \"symptoms_en\": \"Comprehensive symptoms description in English (4-5 sentences)\",\n" +
                        "  \"symptoms_hi\": \"Comprehensive symptoms description in Hindi (4-5 sentences)\",\n" +
                        "  \"suggestion_en\": \"Detailed management and treatment suggestions in English (5-6 sentences)\",\n" +
                        "  \"suggestion_hi\": \"Detailed management and treatment suggestions in Hindi (5-6 sentences)\",\n" +
                        "  \"crop_type_en\": \"Specific crop type affected by this disease in English (e.g., Wheat, Rice, Tomato)\",\n" +
                        "  \"crop_type_hi\": \"Specific crop type affected by this disease in Hindi (e.g., गेहूं, चावल, टमाटर)\"\n" +
                        "}\n\n" +
                        "Ensure all information is scientifically accurate, practical for farmers, and both English and Hindi content is complete and meaningful. " +
                        "If crop_type_en is not provided, infer the most likely crop type based on the disease. " +
                        "For Hindi translations, use proper agricultural terminology that Indian farmers would understand.",
                diseaseName, plantContext
        );
    }

    /**
     * Validates the enhanced data returned by the Gemini AI API to ensure all required fields are present and non-empty.
     *
     * @param data the enhanced data map to validate
     * @return {@code true} if all required fields are present and non-empty, {@code false} otherwise
     */
    private boolean validateEnhancedData(Map<String, String> data) {
        String[] requiredFields = {
                "name_en", "name_hi", "cause_en", "cause_hi",
                "symptoms_en", "symptoms_hi", "suggestion_en", "suggestion_hi",
                "crop_type_en", "crop_type_hi"
        };

        for (String field : requiredFields) {
            if (!data.containsKey(field) || data.get(field) == null || data.get(field).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts a value from a JSON node, returning a default value if the field is missing or empty.
     *
     * @param parentNode   the JSON node containing the field
     * @param fieldName    the name of the field to extract
     * @param defaultValue the default value to return if the field is missing or empty
     * @return the extracted field value, or the default value if not found
     */
    private String getNodeValue(JsonNode parentNode, String fieldName, String defaultValue) {
        if (parentNode.has(fieldName) && !parentNode.get(fieldName).isNull()) {
            String value = parentNode.get(fieldName).asText().trim();
            return !value.isEmpty() ? value : defaultValue;
        }
        return defaultValue;
    }

    /**
     * Generates an HTML-formatted string for the analysis results in the specified language.
     *
     * @param name      the disease name
     * @param cause     the cause of the disease
     * @param symptoms  the symptoms of the disease
     * @param suggestion the treatment or management suggestions
     * @param language  the language code ("en" for English, "hi" for Hindi)
     * @return an HTML string representing the formatted analysis results
     */
    private String generateResultHtml(String name, String cause, String symptoms, String suggestion, String language) {
        String diseaseLabel = language.equals("hi") ? "रोग:" : "Disease:";
        String causeLabel = language.equals("hi") ? "कारण:" : "Cause:";
        String symptomsLabel = language.equals("hi") ? "लक्षण:" : "Symptoms:";
        String suggestionLabel = language.equals("hi") ? "सुझाव:" : "Suggestion:";

        return String.format(
                "<div class='analysis-results'>" +
                        "<div class='result-item'>" +
                        "<span class='result-label'><i class='fas fa-virus'></i> %s</span>" +
                        "<span class='result-value'>%s</span>" +
                        "</div>" +
                        "<div class='result-item'>" +
                        "<span class='result-label'><i class='fas fa-search'></i> %s</span>" +
                        "<span class='result-value'>%s</span>" +
                        "</div>" +
                        "<div class='result-item'>" +
                        "<span class='result-label'><i class='fas fa-eye'></i> %s</span>" +
                        "<span class='result-value'>%s</span>" +
                        "</div>" +
                        "<div class='result-item'>" +
                        "<span class='result-label'><i class='fas fa-lightbulb'></i> %s</span>" +
                        "<span class='result-value'>%s</span>" +
                        "</div>" +
                        "</div>",
                diseaseLabel, escapeHtml(name),
                causeLabel, escapeHtml(cause),
                symptomsLabel, escapeHtml(symptoms),
                suggestionLabel, escapeHtml(suggestion)
        );
    }

    /**
     * Generates a plain text string for the analysis results.
     *
     * @param name      the disease name
     * @param cause     the cause of the disease
     * @param symptoms  the symptoms of the disease
     * @param suggestion the treatment or management suggestions
     * @param cropType  the crop type affected by the disease
     * @return a plain text string representing the analysis results
     */
    private String generatePlainTextResult(String name, String cause, String symptoms, String suggestion, String cropType) {
        return String.format(
                "Crop Type: %s\n\n" +
                        "Disease: %s\n\n" +
                        "Cause: %s\n\n" +
                        "Symptoms: %s\n\n" +
                        "Suggestions: %s",
                escapeHtml(cropType),
                escapeHtml(name),
                escapeHtml(cause),
                escapeHtml(symptoms),
                escapeHtml(suggestion)
        );
    }

    /**
     * Escapes HTML characters in the input string to prevent XSS attacks.
     *
     * @param input the input string to escape
     * @return the escaped string, or an empty string if the input is null
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}