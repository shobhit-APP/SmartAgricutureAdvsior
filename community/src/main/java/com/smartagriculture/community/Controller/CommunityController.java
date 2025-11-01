package com.smartagriculture.community.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Interface.OtpService;
import com.example.Authentication.Interface.UserService;
import com.example.Authentication.Model.Expert;
import com.example.Authentication.Service.EmailServiceImpl;
import com.example.Authentication.repository.ExpertRepository;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import com.smartagriculture.community.Interface.*;
import com.smartagriculture.community.util.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {


    private final UserService userService;
    private final Message message;
    private final CommunityService communityService;
    private final OtpService otpService;
    private final CropReportService cropReportService;
    private final EmailServiceImpl emailService;
    private final VideoTutorialService videoTutorialService;
    private final SoilReportService soilReportService;
    private final BlogPostService blogPostService;
    private final ExpertRepository expertRepository;
    @GetMapping
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or hasRole('FARMER') or hasRole('EXPERT')) and @communityService.isUserVerified(#userId)")
    public ResponseEntity<Map<String, Object>> getCommunityData(@AuthenticationPrincipal UserPrinciple userPrinciples) {
        Map<String, Object> response = new HashMap<>();

        if (userPrinciples == null) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Long userId = userPrinciples.getUserId();
            UserDetails1 user = userService.findById(userId);

            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String userRole = user.getRole() != null ? user.getRole().name().toUpperCase() : "FARMER";
            boolean isVerified = false;

            if ("EXPERT".equals(userRole) && user.getUserEmail() != null) {
                try {
                    isVerified = communityService.isUserVerified(user.getUserId());
                } catch (AnyException e) {
                    System.err.println("Verification check failed: " + e.getMessage());
                }
            }
            response.put("success", true);
            response.put("userName", user.getUsername() != null ? user.getUsername() : "Guest");
            response.put("userEmail", user.getUserEmail() != null ? user.getUserEmail() : "");
            response.put("userRole", userRole);
            boolean isAdminOrFarmer = userRole.equals(UserDetails1.UserRole.ADMIN.toString()) ||
                    userRole.equals(UserDetails1.UserRole.FARMER.toString());
            response.put("isVerified", isAdminOrFarmer || isVerified);
            if (isVerified ||isAdminOrFarmer) {
                response.put("communityPost", communityService.getAllCommunityPost()); // Fixed method name
                response.put("soilReports", communityService.getAllSoilReports());
                response.put("cropReports", communityService.getAllCropReports());
                response.put("blogPosts", communityService.getAllBlogPosts());
                response.put("videoTutorials", communityService.getAllVideoTutorials());
                response.put("blogComments", communityService.getAllCommentDto());
            } else {
                response.put("communityPost", new ArrayList<>());
                response.put("soilReports", new ArrayList<>());
                response.put("cropReports", new ArrayList<>());
                response.put("blogPosts", new ArrayList<>());
                response.put("videoTutorials", new ArrayList<>());
                response.put("blogComments", new ArrayList<>());
            }


            return ResponseEntity.ok(response);

        } catch (AnyException e) {
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            response.put("isVerified", false);
            response.put("userRole", "FARMER");
            response.put("communityPosts", new ArrayList<>());
            response.put("soilReports", new ArrayList<>());
            response.put("cropReports", new ArrayList<>());
            response.put("blogPosts", new ArrayList<>());
            response.put("videoTutorials", new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/post/{type}/{id}")
    @PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
    public ResponseEntity<Map<String, Object>> getPostDetails(
            @PathVariable String type,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrinciple user) {

        Map<String, Object> res = new HashMap<>();
        if (user == null) return message.unauthorized(res.toString());

        try {
            Object data = switch (type.toLowerCase()) {
                case "soil" -> soilReportService.getReportById(id);
                case "crop" -> cropReportService.getListOfCropReportById(id);
                case "blog" -> blogPostService.getPostById(id);
                case "tutorial" -> videoTutorialService.getVideoById(id);
                case "community" -> communityService.getCommunityPostById(id);
                default -> {
                    res.put("success", false);
                    res.put("message", "Invalid type");
                    yield res;
                }
            };

            res.put("success", true);
            res.put("data", data);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return message.error(res, e.getMessage());
        }
    }


    /**
     * Creates a new community post (with optional thumbnail).
     */
    @PostMapping("/posts/create")
    @PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
    public ResponseEntity<Map<String, Object>> createCommunityPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @AuthenticationPrincipal UserPrinciple user) {

        if (user == null) {
            return message.respond(false, "Unauthorized", null, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        try {
            String userEmail = user.getEmail();
            String userRole = user.getUserRole().name().toUpperCase();
            Optional<Expert> expertOpt = expertRepository.findByUserId(user.getUserId());

            if ("EXPERT".equalsIgnoreCase(userRole)) {
                if (expertOpt.isEmpty() || !expertOpt.get().isVerified()) {
                    return message.respond(false, "Expert verification pending or not approved", null, "FORBIDDEN", HttpStatus.FORBIDDEN);
                }
            }

            if (title.trim().isEmpty() || content.trim().isEmpty()) {
                return message.respond(false, "Title and content are required", null, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
            }

            Long postId = communityService.createCommunityPost(
                    user.getUserId(),
                    user.getUsername(),
                    userRole,
                    title,
                    content,
                    thumbnail
            );

            return message.respond(true, "Post created successfully", Map.of("postId", postId), null, HttpStatus.OK);

        } catch (Exception e) {
            return message.respond(false, "Server error: " + e.getMessage(), null, "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/posts/{id}/like")
    @PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
    public ResponseEntity<Map<String, Object>> likePost(@PathVariable Long id, @AuthenticationPrincipal UserPrinciple user) {
        Map<String, Object> res = new HashMap<>();
        if (user == null) return message.unauthorized(res.toString());

        try {
            communityService.likePost(id, user.getUserId());
            res.put("success", true);
            res.put("likes", communityService.getPostLikes(id));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return message.error(res, e.getMessage());
        }
    }
    @PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
    @PostMapping("/posts/{id}/comment")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> req,
            @AuthenticationPrincipal UserPrinciple user) {

        Map<String, Object> res = new HashMap<>();
        if (user == null) return message.unauthorized(res.toString());

        String comment = req.get("comment");
        if (comment == null || comment.trim().isEmpty()) return message.badRequest("Comment required");

        try {
            communityService.addComment(id,user,comment);
            res.put("success", true);
            res.put("message", "Comment added");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return message.error(res, e.getMessage());
        }
    }

}
