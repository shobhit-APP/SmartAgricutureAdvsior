package com.smartagriculture.community.Interface;


import com.example.Authentication.Components.UserPrinciple;
import com.example.common.Exception.AnyException;
import com.smartagriculture.community.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommunityService {
    boolean isUserVerified(Long userId) throws AnyException;
    List<SoilReportDto> getAllSoilReports();
    SoilReportDto getReportById(Long id);
    List<CropReportDto> getAllCropReports();
    List<BlogPostDto> getAllBlogPosts();
    List<VideoTutorialDto> getAllVideoTutorials();
    List<BlogCommentDTO> getAllCommentDto();
        CommunityPostDto getCommunityPostById(Long id);
        void likePost(Long postId, Long userId);
        long getPostLikes(Long postId);

    Long createCommunityPost(Long userId, String username, String role, String title, String content, MultipartFile thumbnail);

    Long addComment(Long postId, UserPrinciple U, String commentText);
    List<CommunityPostDto>getAllCommunityPost();
}