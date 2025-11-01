package com.smartagriculture.community.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.smartagriculture.community.Interface.BlogPostService;
import com.smartagriculture.community.dto.BlogCommentDTO;
import com.smartagriculture.community.dto.BlogPostDto;
import com.smartagriculture.community.dto.BlogShareResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller for handling all blog post and comment operations.
 * Provides APIs for creating, reading, updating, deleting, liking,
 * sharing, exporting, and commenting on blog posts.
 *
 * @author Shobhit Srivastava
 * @version 1.0
 */
@RestController
@PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
@RequestMapping("/blogs")
public class BlogController {

    @Autowired
    private BlogPostService blogService;

    /**
     * Create a new blog post with optional thumbnail.
     *
     * @param dto  BlogPostDto containing title and content.
     * @param file Optional thumbnail image.
     * @param user Authenticated user.
     * @return Created BlogPostDto with HTTP 201 status.
     */
    @PostMapping(value = "/create", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BlogPostDto> createPost(
            @RequestPart("data") BlogPostDto dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile file,
            @AuthenticationPrincipal UserPrinciple user) {
        BlogPostDto createdPost = blogService.createPost(dto, file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    /**
     * Get all blog posts.
     *
     * @return List of all BlogPostDto objects.
     */
    @GetMapping
    public ResponseEntity<List<BlogPostDto>> getAllPosts() {
        return ResponseEntity.ok(blogService.getAllPosts());
    }

    /**
     * Get a specific blog post by its ID.
     *
     * @param id BlogPost ID.
     * @return BlogPostDto object.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlogPostDto> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok((BlogPostDto) blogService.getPostById(id));
    }

    /**
     * Update an existing blog post.
     *
     * @param id   BlogPost ID.
     * @param dto  Updated BlogPostDto.
     * @param user Authenticated user.
     * @return Updated BlogPostDto.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BlogPostDto> updatePost(
            @PathVariable Long id,
            @RequestBody BlogPostDto dto,
            @AuthenticationPrincipal UserPrinciple user) {

        return ResponseEntity.ok((BlogPostDto) blogService.updatePost(id, dto, user));
    }

    /**
     * Delete a blog post.
     *
     * @param id   BlogPost ID.
     * @param user Authenticated user.
     * @return HTTP 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrinciple user) {

        blogService.deletePost(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search for blog posts using a keyword.
     * If keyword is empty, defaults to "agriculture".
     *
     * @param keyword Search term.
     * @return List of matching blog posts.
     */
    @GetMapping("/search")
    public ResponseEntity<List<BlogPostDto>> searchPosts(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(blogService.searchPosts(keyword));
    }

    /**
     * Get top 5 blog posts based on likes.
     *
     * @return List of top liked blog posts.
     */
    @GetMapping("/top-liked")
    public ResponseEntity<List<BlogPostDto>> getTopLikedPosts() {
        return ResponseEntity.ok(blogService.getTopLikedPosts());
    }

    /**
     * Like a blog post.
     *
     * @param id BlogPost ID.
     * @return Updated BlogPostDto with incremented like count.
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likePost(@PathVariable Long id) {
        boolean isLiked = blogService.likePost(id);
        if (isLiked) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Post liked successfully!"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "Failed to like post."));
        }
    }

    /**
     * Share a blog post (returns a sharable URL).
     *
     * @param id BlogPost ID.
     * @return BlogShareResponse containing share URL.
     */
    @GetMapping("/{id}/share")
    public ResponseEntity<BlogShareResponse> sharePost(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.sharePost(id));
    }

    /**
     * Export a blog post as a PDF.
     *
     * @param id BlogPost ID.
     * @return PDF file as byte stream.
     * @throws Exception if PDF generation fails.
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportToPdf(@PathVariable Long id) throws Exception {
        byte[] pdfBytes = blogService.exportToPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=blog_" + id + ".pdf")
                .body(pdfBytes);
    }

    /**
     * Add a comment to a specific blog post.
     *
     * @param postId     BlogPost ID.
     * @param commentDto Comment text.
     * @param user       Authenticated user.
     * @return HTTP 201 Created.
     */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long postId,
            @RequestBody BlogCommentDTO commentDto,
            @AuthenticationPrincipal UserPrinciple user) {

        blogService.addComment(postId, commentDto, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "message", "Comment added successfully"));
    }
    /**
     * Get all comments of a specific blog post.
     *
     * @param postId BlogPost ID.
     * @param user   Authenticated user.
     * @return List of BlogCommentDTO.
     */
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<BlogCommentDTO>> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrinciple user) {

        return ResponseEntity.ok(blogService.getComments(postId, user));
    }
}