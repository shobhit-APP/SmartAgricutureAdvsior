package com.smartagriculture.community.Interface;

import com.example.Authentication.Components.UserPrinciple;
import com.smartagriculture.community.dto.BlogCommentDTO;
import com.smartagriculture.community.dto.BlogPostDto;
import com.smartagriculture.community.dto.BlogShareResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for managing blog posts, comments, likes, and shares.
 * <p>
 * This layer handles the business logic and delegates persistence operations
 * to the repository layer.
 */
public interface BlogPostService {

    /**
     * Creates a new blog post with optional Cloudinary thumbnail upload.
     *
     * @param dto   Blog post data transfer object containing post details
     * @param file  Optional thumbnail image
     * @param user  Authenticated user details
     * @return the created BlogPost entity
     */
    BlogPostDto createPost(BlogPostDto dto, MultipartFile file, UserPrinciple user);


    /**
     * Retrieves a blog post by its unique ID.
     *
     * @param id blog post ID
     * @return list containing the requested blog post (if found)
     */
    List<BlogPostDto> getPostById(Long id);

    /**
     * Retrieves all blog posts from the database.
     *
     * @return list of all BlogPosts
     */
    List<BlogPostDto> getAllPosts();

    /**
     * Searches blog posts based on title, content, or category.
     * The search is case-insensitive and supports partial matches.
     *
     * @param keyword the search keyword
     * @return list of matching blog posts
     */
    List<BlogPostDto> searchPosts(String keyword);

    /**
     * Retrieves the top liked posts.
     *
     * @return list of top liked BlogPosts
     */
    List<BlogPostDto> getTopLikedPosts();

    /**
     * Updates an existing blog post.
     *
     * @param id              blog post ID
     * @param dto             updated data transfer object
     * @param userPrinciples  authenticated user
     * @return list of updated BlogPosts
     */
    List<BlogPostDto> updatePost(Long id, BlogPostDto dto, UserPrinciple userPrinciples);

    /**
     * Deletes a blog post by ID.
     *
     * @param id              blog post ID
     * @param userPrinciples  authenticated user
     */
    void deletePost(Long id, UserPrinciple userPrinciples);

    /**
     * Adds a like to a specific blog post.
     *
     * @param id blog post ID
     * @return updated list of BlogPosts
     */
    boolean likePost(Long id);

    /**
     * Shares a specific blog post and returns share metadata.
     *
     * @param id blog post ID
     * @return share response details
     */
    BlogShareResponse sharePost(Long id);

    /**
     * Exports a specific blog post to PDF format.
     *
     * @param id blog post ID
     * @return byte array of generated PDF file
     * @throws Exception if export fails
     */
    byte[] exportToPdf(Long id) throws Exception;

    /**
     * Adds a comment to a blog post.
     *
     * @param id              blog post ID
     * @param commentDto      comment DTO
     * @param userPrinciples  authenticated user
     */
    void addComment(Long id, @Valid BlogCommentDTO commentDto, UserPrinciple userPrinciples);

    /**
     * Retrieves all comments for a specific blog post.
     *
     * @param id              blog post ID
     * @param userPrinciples  authenticated user
     * @return list of comments
     */
    List<BlogCommentDTO> getComments(Long id, UserPrinciple userPrinciples);
}
