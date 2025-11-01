package com.smartagriculture.community.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.Authentication.Model.Expert;
import com.example.Authentication.repository.ExpertRepository;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.smartagriculture.community.Interface.CropReportService;
import com.smartagriculture.community.Model.CropReport;
import com.smartagriculture.community.Repository.CropReportRepository;
import com.smartagriculture.community.dto.CropReportDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CropReportServiceImpl implements CropReportService {

    private final CropReportRepository cropReportRepository;
    private final ExpertRepository expertRepository;
    private final UserRepo userRepository;
    private final Cloudinary cloudinary;

    @Autowired
    public CropReportServiceImpl(
            CropReportRepository cropReportRepository,
            ExpertRepository expertRepository,
            UserRepo userRepository,
            @Value("${cloudinary.cloud.name}") String cloudName,
            @Value("${cloudinary.api.key}") String apiKey,
            @Value("${cloudinary.api.secret}") String apiSecret) {

        this.cropReportRepository = cropReportRepository;
        this.expertRepository = expertRepository;
        this.userRepository = userRepository;
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    // ✅ Upload image to Cloudinary
    private String uploadImageToCloudinary(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "AnyException/Crop_Reports")
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new AnyException(400, "Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }

    // ✅ Create Report
    @Override
    public CropReportDto createReport(CropReportDto dto, MultipartFile image, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AnyException(404, "User not found"));

        if (dto.getCropName() == null || dto.getCropName().trim().isEmpty())
            throw new AnyException(400, "Crop name is required");

        if (dto.getRegion() == null || dto.getRegion().trim().isEmpty())
            throw new AnyException(400, "Region is required");

        String designation = "FARMER";
        Optional<Expert> expertOptional = expertRepository.findByUserId(userId);
        if (expertOptional.isPresent() && user.getRole().name().equalsIgnoreCase("EXPERT")) {
            designation = expertOptional.get().getField();
        }

        String imageUrl = uploadImageToCloudinary(image);

        CropReport report = CropReport.builder()
                .reporterName(user.getUsername())
                .designation(designation)
                .cropName(dto.getCropName())
                .region(dto.getRegion())
                .cropHealth(dto.getCropHealth())
                .estimatedYield(dto.getEstimatedYield())
                .expertRemarks(dto.getExpertRemarks())
                .imageUrl(imageUrl)
                .userId(userId)
                .build();

        CropReport saved = cropReportRepository.save(report);
        return toDto(saved);
    }

    // ✅ Update Report
    @Override
    public CropReportDto updateReport(Long id, CropReportDto dto, MultipartFile image, Long userId) {
        CropReport existing = cropReportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Report not found"));

        if (!existing.getUserId().equals(userId)) {
            throw new AnyException(403, "You can only update your own report");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AnyException(404, "User not found"));

        String designation = "FARMER";
        Optional<Expert> expertOptional = expertRepository.findByUserId(userId);
        if (expertOptional.isPresent() && user.getRole().name().equalsIgnoreCase("EXPERT")) {
            designation = expertOptional.get().getField();
        }

        String imageUrl = existing.getImageUrl();
        if (image != null && !image.isEmpty()) {
            imageUrl = uploadImageToCloudinary(image);
        }

        existing.setReporterName(user.getUsername());
        existing.setDesignation(designation);
        existing.setCropName(dto.getCropName());
        existing.setRegion(dto.getRegion());
        existing.setCropHealth(dto.getCropHealth());
        existing.setEstimatedYield(dto.getEstimatedYield());
        existing.setExpertRemarks(dto.getExpertRemarks());
        existing.setImageUrl(imageUrl);

        return toDto(cropReportRepository.save(existing));
    }

    // ✅ Get Report by ID (only if owned by user)
    @Override
    public CropReportDto getReportByIdForUser(Long id, Long userId) {
        CropReport report = cropReportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Report not found"));

        if (!report.getUserId().equals(userId)) {
            throw new AnyException(403, "You can only view your own report");
        }

        return toDto(report);
    }

    // ✅ Get All Reports by User
    @Override
    public List<CropReportDto> getAllReportsByUserId(Long userId) {
        return cropReportRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Search Reports by Crop Name for User
    @Override
    public List<CropReportDto> searchByCropForUser(String cropName, Long userId) {
        return cropReportRepository.findByCropNameContainingIgnoreCaseAndUserId(cropName, userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Search Reports by Region for User
    @Override
    public List<CropReportDto> searchByRegionForUser(String region, Long userId) {
        return cropReportRepository.findByRegionContainingIgnoreCaseAndUserId(region, userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Delete Report
    @Override
    public void deleteReport(Long id, Long userId) {
        CropReport report = cropReportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Report not found"));

        if (!report.getUserId().equals(userId)) {
            throw new AnyException(403, "You can only delete your own report");
        }

        cropReportRepository.delete(report);
    }

    @Override
    public CropReportDto getCropReportById(Long id) {
        CropReport cropReport = cropReportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Crop report not found with id: " + id));
        return toDto(cropReport);
    }

    @Override
    public List<CropReportDto> getListOfCropReportById(Long userId) {
        List<CropReport> reports = cropReportRepository.findByUserId(userId);
        return reports.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CropReportDto toDto(CropReport entity) {
        return CropReportDto.builder()
                .id(entity.getId())
                .reporterName(entity.getReporterName())
                .designation(entity.getDesignation())
                .cropName(entity.getCropName())
                .region(entity.getRegion())
                .cropHealth(entity.getCropHealth())
                .estimatedYield(entity.getEstimatedYield())
                .expertRemarks(entity.getExpertRemarks())
                .imageUrl(entity.getImageUrl())
                .createdAt(entity.getCreatedAt().toString())
                .updatedAt(entity.getUpdatedAt().toString())
                .build();
    }
}
