package com.example.Authentication.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Interface.EmailServiceInterface;
import com.example.Authentication.Interface.ExpertService;
import com.example.Authentication.Interface.OtpService;
import com.example.Authentication.Model.Expert;
import com.example.Authentication.dto.ExpertDto;
import com.example.Authentication.enums.OtpPurpose;
import com.example.Authentication.repository.ExpertRepository;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class ExpertServiceImpl implements ExpertService {

    private final ExpertRepository expertRepository;
    private final UserRepo userRepository;
    private final EmailServiceInterface emailService;
    private final Cloudinary cloudinary;
    private final OtpService otpService;

    @Autowired
    public ExpertServiceImpl(
            ExpertRepository expertRepository,
            UserRepo userRepository,
            EmailServiceInterface emailService,
            @Value("${cloudinary.cloud.name}") String cloudName,
            @Value("${cloudinary.api.key}") String apiKey,
            @Value("${cloudinary.api.secret}") String apiSecret, OtpService otpService
    ) {
        this.expertRepository = expertRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.otpService = otpService;
        this.cloudinary = new Cloudinary(com.cloudinary.utils.ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public List<ExpertDto> getPendingApplications() {
        List<Expert> pendingExperts = expertRepository.findByPendingReviewTrue();
        return pendingExperts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpertDto> getAllApplications() {
        List<Expert> allExperts = expertRepository.findAll();
        return allExperts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpertDto> getVerifiedExperts() {
        List<Expert> verifiedExperts = expertRepository.findByIsVerifiedTrue();
        return verifiedExperts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpertDto> getRejectedExperts() {
        List<Expert> rejectedExperts = expertRepository.findByIsVerifiedFalseAndPendingReviewFalse();
        return rejectedExperts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ExpertDto getExpertById(Long expertId) {
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new RuntimeException("Expert not found"));
        return convertToDto(expert);
    }

    @Override
    @Transactional
    public Map<String, Object> handleExpertVerification(Long expertId, String action, String reason) {
        Map<String, Object> response = new HashMap<>();

        try {
            Expert expert = expertRepository.findById(expertId)
                    .orElseThrow(() -> new RuntimeException("Expert not found"));
            UserDetails1 user = userRepository.findById(expert.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            switch (action.toUpperCase()) {
                case "APPROVE":
                    expert.setVerified(true);
                    expert.setPendingReview(false);
                    expertRepository.save(expert);
                    emailService.sendApprovalEmail(user, "APPROVED",expert);
                    response.put("success", true);
                    response.put("message", "Expert approved successfully");
                    response.put("action", "APPROVED");
                    break;

                case "REJECT":
                    expert.setVerified(false);
                    expert.setPendingReview(false);
                    expertRepository.save(expert);
                    emailService.sendRejectionEmail(user, "REJECT",reason);
                    response.put("success", true);
                    response.put("message", "Expert application rejected");
                    response.put("action", "REJECTED");
                    break;

                case "UNDER_REVIEW":
                    expert.setPendingReview(true);
                    expertRepository.save(expert);
                    response.put("success", true);
                    response.put("message", "Marked as under review");
                    response.put("action", "UNDER_REVIEW");
                    break;
                default:
                    response.put("success", false);
                    response.put("message", "Invalid action. Use: APPROVE, REJECT, or UNDER_REVIEW");
                    return response;
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    @Override
    public ExpertDto getExpertByUserId(Long userId) {
        Expert expert = expertRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Expert not found for user"));
        return convertToDto(expert);
    }

    // Helper method to convert Entity to DTO
    private ExpertDto convertToDto(Expert expert) {
        UserDetails1 user = userRepository.findById(expert.getUserId()).orElse(null);

        return ExpertDto.builder()
                .id(expert.getId())
                .userId(expert.getUserId())
                .userName(user != null ? user.getUsername() : "Unknown")
                .userEmail(user != null ? user.getUserEmail() : "Unknown")
                .userRole(user != null ? user.getRole().name() : "EXPERT")
                .field(expert.getField())
                .experience_years(expert.getExperience_years())
                .organization(expert.getOrganization())
                .profileImage_path(expert.getProfileImage_path())
                .uploadId_path(expert.getUploadId_path())
                .isVerified(expert.isVerified())
                .pendingReview(expert.isPendingReview())
                .build();
    }
    @Override
    @Transactional
    public void submitExpertVerification(ExpertDto expertDto, MultipartFile uploadId, UserPrinciple userPrinciples, MultipartFile U) {
        try {
            Expert expert = new Expert();
            expert.setField(expertDto.getField());
            expert.setOrganization(expertDto.getOrganization());
            expert.setExperience_years(expertDto.getExperience_years());
            expert.setVerified(false);
            expert.setPendingReview(true);
            expert.setUserId(userPrinciples.getUserId());

            if (uploadId != null && !uploadId.isEmpty()) {
                Map uploadIdResult = cloudinary.uploader().upload(uploadId.getBytes(),
                        com.cloudinary.utils.ObjectUtils.asMap("folder", "AgriConnect/Expert_IDs"));
                Map uploadImageResult = cloudinary.uploader().upload(U.getBytes(),
                        ObjectUtils.asMap("folder", "AgriConnect/Expert_Images"));
                String idImageUrl = (String) uploadIdResult.get("secure_url");
                String profileImageUrl = (String) uploadImageResult.get("secure_url");
                expert.setUploadId_path(idImageUrl);
                expert.setProfileImage_path(profileImageUrl);
            }
            otpService.generateAndStoreOtp(userPrinciples.getEmail(), OtpPurpose.EXPERT_VERIFICATION);
            expertRepository.save(expert);

        } catch (IOException e) {
            throw new AnyException(400,"Error uploading expert ID/image: " + e.getMessage());
        } catch (Exception e) {
            throw new AnyException(
                 500,"Failed to submit expert verification: " + e.getMessage());
        }
    }
}