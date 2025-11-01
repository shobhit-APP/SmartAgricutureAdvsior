package com.smartagriculture.community.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Model.Expert;
import com.example.Authentication.repository.ExpertRepository;
import com.example.common.Exception.AnyException;
import com.smartagriculture.community.Interface.VideoTutorialService;
import com.smartagriculture.community.Model.VideoTutorial;
import com.smartagriculture.community.Repository.VideoTutorialRepository;
import com.smartagriculture.community.dto.VideoTutorialDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class VideoTutorialServiceImpl implements VideoTutorialService {

    private final VideoTutorialRepository videoRepository;
    private final ExpertRepository expertRepository;
    private final Cloudinary cloudinary;

    @Autowired
    public VideoTutorialServiceImpl(
            ExpertRepository expertRepository,
            VideoTutorialRepository videoRepository,
            @Value("${cloudinary.cloud.name}") String cloudName,
            @Value("${cloudinary.api.key}") String apiKey,
            @Value("${cloudinary.api.secret}") String apiSecret) {
        this.expertRepository = expertRepository;
        this.videoRepository = videoRepository;
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public VideoTutorial createVideo(VideoTutorialDto dto, MultipartFile thumbnail) {
        try {
            UserPrinciple currentUser = (UserPrinciple) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            Long userId = currentUser.getUserId();
            String role = currentUser.getUserRole().toString();

            if (dto.getVideoUrl() == null || dto.getVideoUrl().trim().isEmpty()) {
                throw new AnyException(400, "Video URL is required");
            }

            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                throw new AnyException(400, "Title is required");
            }

            String thumbnailUrl = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                try {
                    Map<?, ?> uploadResult = cloudinary.uploader().upload(
                            thumbnail.getBytes(),
                            ObjectUtils.asMap("folder", "AgriConnect/Video_thumbnail")
                    );
                    thumbnailUrl = uploadResult.get("secure_url").toString();
                } catch (Exception e) {
                    throw new AnyException(500, "Failed to upload thumbnail to Cloudinary");
                }
            }

            String uploadedBy = currentUser.getUsername();
            String designation = "Farmer";
            String expertField = "Farmer";

            if ("EXPERT".equalsIgnoreCase(role)) {
                Expert expert = expertRepository.findByUserId(userId)
                        .orElseThrow(() -> new AnyException(404, "Expert profile not found"));
                designation = expert.getField();
                expertField = expert.getField();
            }

            dto.setUploadedBy(uploadedBy);
            dto.setThumbnailUrl(thumbnailUrl);
            dto.setDesignation(designation);
            dto.setExpertField(expertField);

            VideoTutorial videoTutorial = toEntity(dto, userId);
            return videoRepository.save(videoTutorial);

        } catch (AnyException e) {
            throw e;
        } catch (Exception e) {
            throw new AnyException(500, "An unexpected error occurred while creating video tutorial");
        }
    }

    @Override
    public VideoTutorial getVideoById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Video not found with id: " + id));
    }

    @Override
    public List<VideoTutorial> getAllVideos() {
        return videoRepository.findAll();
    }

    @Override
    public List<VideoTutorial> searchVideos(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return videoRepository.findAll();
        }
        return videoRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    public List<VideoTutorial> getVideosByUploader(String uploadedBy) {
        if (uploadedBy == null || uploadedBy.trim().isEmpty()) {
            throw new AnyException(400, "Uploader name cannot be empty");
        }
        return videoRepository.findByUploadedBy(uploadedBy);
    }

    @Override
    public VideoTutorial updateVideo(Long id, VideoTutorialDto dto) {
        VideoTutorial video = getVideoById(id);
        UserPrinciple currentUser = (UserPrinciple) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long userId = currentUser.getUserId();
        if (!video.getUserId().equals(userId)) {
            throw new AnyException(403, "You can only update your own video");
        }

        if (dto.getVideoUrl() == null || dto.getVideoUrl().trim().isEmpty()) {
            throw new AnyException(400, "Video URL is required");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new AnyException(400, "Title is required");
        }

        video.setTitle(dto.getTitle());
        video.setDescription(dto.getDescription());
        video.setVideoUrl(dto.getVideoUrl());

        return videoRepository.save(video);
    }

    @Override
    public void deleteVideo(Long id) {
        VideoTutorial video = getVideoById(id);
        UserPrinciple currentUser = (UserPrinciple) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long userId = currentUser.getUserId();
        if (!video.getUserId().equals(userId)) {
            throw new AnyException(403, "You can only delete your own post");
        }

        videoRepository.delete(video);
    }

    @Override
    public VideoTutorialDto toDto(VideoTutorial entity) {
        if (entity == null) return null;

        return VideoTutorialDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .videoUrl(entity.getVideoUrl())
                .uploadedBy(entity.getUploadedBy())
                .designation(entity.getDesignation())
                .uploadDate(entity.getUploadDate().toString())
                .userId(entity.getUserId())
                .thumbnailUrl(entity.getThumbnailUrl())
                .expertField(entity.getExpertField())
                .build();
    }

    public VideoTutorial toEntity(VideoTutorialDto dto, Long userId) {
        if (dto == null) return null;

        VideoTutorial entity = new VideoTutorial();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setUploadedBy(dto.getUploadedBy());
        entity.setDesignation(dto.getDesignation());
        entity.setUserId(userId);
        entity.setUploadDate(LocalDateTime.now());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        entity.setExpertField(dto.getExpertField());

        return entity;
    }
}
