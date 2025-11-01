package com.smartagriculture.community.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Interface.OtpService;
import com.example.Authentication.Model.Expert;
import com.example.Authentication.Service.EmailServiceImpl;
import com.example.Authentication.repository.ExpertRepository;
import com.example.common.Exception.AnyException;
import com.smartagriculture.community.Interface.CommunityService;
import com.smartagriculture.community.Model.Comment;
import com.smartagriculture.community.Model.CommunityPost;
import com.smartagriculture.community.Model.SoilParameter;
import com.smartagriculture.community.Model.SoilReport;
import com.smartagriculture.community.Repository.*;
import com.smartagriculture.community.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service("communityService")
public class CommunityServiceImpl implements CommunityService {

    private final ExpertRepository expertRepository;
    private final SoilReportRepository soilReportRepository;
    private final CropReportRepository cropReportRepository;
    private final BlogPostRepository blogRepository;
    private final VideoTutorialRepository videoRepository;
    private final Cloudinary cloudinary;
    private final CommunityPostRepository postRepo;
    private final CommentRepository commentRepo;
    private final BlogCommentRepository blogCommentRepository;

    @Autowired
    public CommunityServiceImpl(
            ExpertRepository expertRepository,
            EmailServiceImpl emailService,
            SoilReportRepository soilReportRepository,
            CropReportRepository cropReportRepository,
            BlogPostRepository blogRepository,
            VideoTutorialRepository videoRepository,
            OtpService otpService,
            CommunityPostRepository postRepo,
            CommentRepository commentRepo,
            BlogCommentRepository blogCommentRepository,
            @Value("${cloudinary.cloud.name}") String cloudName,
            @Value("${cloudinary.api.key}") String apiKey,
            @Value("${cloudinary.api.secret}") String apiSecret) {

        this.expertRepository = expertRepository;
        this.soilReportRepository = soilReportRepository;
        this.cropReportRepository = cropReportRepository;
        this.blogRepository = blogRepository;
        this.videoRepository = videoRepository;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.blogCommentRepository = blogCommentRepository;
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public boolean isUserVerified(Long userId) {
        try {
            return expertRepository.findByUserId(userId)
                    .map(Expert::isVerified)
                    .orElse(false);
        } catch (Exception e) {
            throw new AnyException(500, "Error checking user verification: " + e.getMessage());
        }
    }

    @Override
    public List<SoilReportDto> getAllSoilReports() {
        return soilReportRepository.findAll()
                .stream()
                .map(this::toReportDto)
                .collect(Collectors.toList());
    }

    private SoilReportDto toReportDto(SoilReport report) {
        return SoilReportDto.builder()
                .id(report.getId())
                .reporterName(report.getReporterName())
                .designation(report.getDesignation())
                .region(report.getRegion())
                .soilType(report.getSoilType())
                .remarks(report.getRemarks())
                .reportDate(report.getReportDate().toString())
                .parameters(report.getParameters() != null ?
                        report.getParameters().stream()
                                .map(this::toParameterDto)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    private SoilParameterDto toParameterDto(SoilParameter param) {
        return SoilParameterDto.builder()
                .id(param.getId())
                .ph(param.getPh())
                .nitrogen(param.getNitrogen())
                .phosphorus(param.getPhosphorus())
                .potassium(param.getPotassium())
                .temperature(param.getTemperature())
                .humidity(param.getHumidity())
                .rainfall(param.getRainfall())
                .organicCarbon(param.getOrganicCarbon())
                .sulfur(param.getSulfur())
                .zinc(param.getZinc())
                .iron(param.getIron())
                .manganese(param.getManganese())
                .copper(param.getCopper())
                .soilTexture(param.getSoilTexture())
                .moistureContent(param.getMoistureContent())
                .soilReportId(param.getSoilReport() != null ? param.getSoilReport().getId() : null)
                .build();
    }

    @Override
    public List<CropReportDto> getAllCropReports() {
        return cropReportRepository.findAll()
                .stream()
                .map(report -> CropReportDto.builder()
                        .id(report.getId())
                        .reporterName(report.getReporterName())
                        .designation(report.getDesignation())
                        .cropName(report.getCropName())
                        .region(report.getRegion())
                        .cropHealth(report.getCropHealth())
                        .estimatedYield(report.getEstimatedYield())
                        .expertRemarks(report.getExpertRemarks())
                        .imageUrl(report.getImageUrl())
                        .createdAt(report.getCreatedAt().toString())
                        .updatedAt(report.getUpdatedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public SoilReportDto getReportById(Long id) {
        SoilReport report = soilReportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Soil report not found with id: " + id));
        return toReportDto(report);
    }

    @Override
    public List<BlogPostDto> getAllBlogPosts() {
        return blogRepository.findAll()
                .stream()
                .map(post -> BlogPostDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .authorName(post.getAuthorName())
                        .authorDesignation(post.getAuthorDesignation())
                        .likeCount(post.getLikes())
                        .thumbnailUrl(post.getThumbnailUrl())
                        .comments(blogCommentRepository.findByPostId(post.getId())
                                .stream()
                                .map(comment -> new BlogCommentDTO(
                                        comment.getId(),
                                        comment.getText(),
                                        comment.getCommenterName(),
                                        comment.getCreatedAt().toString()))
                                .collect(Collectors.toList()))
                        .createdAt(post.getCreatedAt().toString())
                        .updatedAt(post.getUpdatedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoTutorialDto> getAllVideoTutorials() {
        return videoRepository.findAll()
                .stream()
                .map(video -> VideoTutorialDto.builder()
                        .id(video.getId())
                        .title(video.getTitle())
                        .description(video.getDescription())
                        .videoUrl(video.getVideoUrl())
                        .thumbnailUrl(video.getThumbnailUrl())
                        .uploadedBy(video.getUploadedBy())
                        .designation(video.getDesignation())
                        .expertField(video.getExpertField())
                        .uploadDate(video.getUploadDate().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogCommentDTO> getAllCommentDto() {
        return blogCommentRepository.findAll()
                .stream()
                .map(c -> BlogCommentDTO.builder()
                        .id(c.getId())
                        .text(c.getText())
                        .commenterName(c.getCommenterName())
                        .blogPostId(c.getPost().getId())
                        .createdAt(c.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CommunityPostDto> getAllCommunityPost() {
        try {
            return postRepo.findAll()
                    .stream()
                    .map(this::toPostDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnyException(500, "Error fetching community posts: " + e.getMessage());
        }
    }

    @Override
    public CommunityPostDto getCommunityPostById(Long id) {
        CommunityPost post = postRepo.findById(id)
                .orElseThrow(() -> new AnyException(404, "Post not found"));
        return toPostDto(post);
    }

    @Override
    public void likePost(Long postId, Long userId) {
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new AnyException(404, "Post not found"));
        post.setLikesCount(post.getLikesCount() + 1);
        postRepo.save(post);
    }

    @Override
    public long getPostLikes(Long postId) {
        return postRepo.findById(postId)
                .map(CommunityPost::getLikesCount)
                .orElse(0);
    }

    @Override
    @Transactional
    public Long createCommunityPost(Long userId, String username, String role, String title, String content, MultipartFile thumbnail) {
        try {
            String field = null, thumbnailUrl = null, publicId = null;

            if ("EXPERT".equalsIgnoreCase(role)) {
                field = expertRepository.findByUserId(userId)
                        .map(Expert::getField)
                        .orElseThrow(() -> new AnyException(404, "Expert not found for user ID: " + userId));
            }

            if (thumbnail != null && !thumbnail.isEmpty()) {
                Map uploadResult = cloudinary.uploader().upload(thumbnail.getBytes(),
                        ObjectUtils.asMap("folder", "AgriConnect/Community_Posts"));
                thumbnailUrl = uploadResult.get("secure_url").toString();
                publicId = uploadResult.get("public_id").toString();
            }

            CommunityPost post = CommunityPost.builder()
                    .userId(userId)
                    .authorName(username)
                    .designation(field != null ? field : role)
                    .title(title)
                    .content(content)
                    .likesCount(0)
                    .thumbnailUrl(thumbnailUrl)
                    .cloudinaryPublicId(publicId)
                    .build();
            postRepo.save(post);
            return post.getId();

        } catch (IOException e) {
            throw new AnyException(400, "Failed to upload thumbnail to Cloudinary: " + e.getMessage());
        } catch (Exception e) {
            throw new AnyException(500, "Error while creating post: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Long addComment(Long postId, UserPrinciple user, String commentText) {
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new AnyException(404, "Post not found"));
        Comment comment = Comment.builder()
                .text(commentText)
                .commenterName(user.getUsername())
                .UserId(user.getUserId())
                .post(post)
                .build();
        return commentRepo.save(comment).getId();
    }

    private CommunityPostDto toPostDto(CommunityPost post) {
        List<CommentDto> commentDtos = Optional.ofNullable(post.getComments())
                .orElse(List.of())
                .stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());

        return CommunityPostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorName(post.getAuthorName())
                .designation(post.getDesignation())
                .likeCount(post.getLikesCount())
                .thumbnailUrl(post.getThumbnailUrl())
                .cloudinaryPublicId(post.getCloudinaryPublicId())
                .comments(commentDtos)
                .createdAt(post.getCreatedAt().toString())
                .updatedAt(post.getUpdatedAt().toString())
                .build();
    }

    private CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .commenterName(comment.getCommenterName())
                .postId(comment.getPost().getId())
                .createdAt(comment.getCreatedAt().toString())
                .build();
    }
}
