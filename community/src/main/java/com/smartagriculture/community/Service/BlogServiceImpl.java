package com.smartagriculture.community.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Model.Expert;
import com.example.Authentication.repository.ExpertRepository;
import com.example.common.Exception.AnyException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.smartagriculture.community.Interface.BlogPostService;
import com.smartagriculture.community.Model.BlogComment;
import com.smartagriculture.community.Model.BlogPost;
import com.smartagriculture.community.Repository.BlogCommentRepository;
import com.smartagriculture.community.Repository.BlogPostRepository;
import com.smartagriculture.community.dto.BlogCommentDTO;
import com.smartagriculture.community.dto.BlogPostDto;
import com.smartagriculture.community.dto.BlogShareResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final ExpertRepository expertRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final Cloudinary cloudinary;

    @Autowired
    public BlogServiceImpl(
            BlogPostRepository blogPostRepository,
            ExpertRepository expertRepository,
            BlogCommentRepository blogCommentRepository,
            @Value("${cloudinary.cloud.name}") String cloudName,
            @Value("${cloudinary.api.key}") String apiKey,
            @Value("${cloudinary.api.secret}") String apiSecret) {
        this.blogPostRepository = blogPostRepository;
        this.expertRepository = expertRepository;
        this.blogCommentRepository = blogCommentRepository;
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    private String uploadThumbnail(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "AgriConnect_Blogs/Thumbnails"));
        return (String) uploadResult.get("secure_url");
    }

    private BlogPostDto convertToDto(BlogPost post) {
        return BlogPostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorName(post.getAuthorName())
                .authorDesignation(post.getAuthorDesignation())
                .thumbnailUrl(post.getThumbnailUrl())
                .comments(blogCommentRepository.findByPostId(post.getId())
                        .stream()
                        .map(comment -> new BlogCommentDTO(
                                comment.getId(),
                                comment.getText(),
                                comment.getCommenterName(),
                                comment.getCreatedAt().toString()
                        ))
                        .collect(Collectors.toList())
                )
                .likeCount(post.getLikes())
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null)
                .build();
    }

    @Override
    @Transactional
    public BlogPostDto createPost(BlogPostDto dto, MultipartFile file, UserPrinciple user) {
        try {
            String authorName = user.getUsername();
            String authorDesignation = "FARMER";

            if ("EXPERT".equalsIgnoreCase(user.getUserRole().name())) {
                authorDesignation = expertRepository.findByUserId(user.getUserId())
                        .map(Expert::getField)
                        .orElseThrow(() -> new AnyException(404,
                                "Expert not found for user ID: " + user.getUserId()));
            }

            String thumbnailUrl = null;
            if (file != null && !file.isEmpty()) {
                thumbnailUrl = uploadThumbnail(file);
            }

            BlogPost post = BlogPost.builder()
                    .title(dto.getTitle())
                    .content(dto.getContent())
                    .authorName(authorName)
                    .authorDesignation(authorDesignation)
                    .thumbnailUrl(thumbnailUrl)
                    .UserId(user.getUserId())
                    .likes(0)
                    .build();

            BlogPost savedPost = blogPostRepository.save(post);
            return convertToDto(savedPost);

        } catch (IOException e) {
            throw new AnyException(
                    400,  "Thumbnail upload failed: " + e.getMessage());
        }
    }

    @Override
    public List<BlogPostDto> getPostById(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AnyException(
                        404,"Blog post not found with id: " + id));
        return Collections.singletonList(convertToDto(post));
    }

    @Override
    public List<BlogPostDto> getAllPosts() {
        List<BlogPost> posts = blogPostRepository.findAll();
        return posts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogPostDto> searchPosts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            keyword = "agriculture";
        }
        List<BlogPost> posts = blogPostRepository.searchBlogPosts(keyword);
        return posts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogPostDto> getTopLikedPosts() {
        List<BlogPost> posts = blogPostRepository.findTop5ByOrderByLikesDesc();
        return posts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<BlogPostDto> updatePost(Long id, BlogPostDto dto, UserPrinciple user) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AnyException(
                        404,"Blog post not found with id: " + id));

        if (!post.getUserId().equals(user.getUserId())) {
            throw new AnyException(
                    403, "You can only edit your own post");
        }

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new AnyException(
                400,"Title is required");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new AnyException(
                     400,"Content is required");
        }

        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent().trim());
        if (dto.getThumbnailUrl() != null) {
            post.setThumbnailUrl(dto.getThumbnailUrl());
        }

        BlogPost updatedPost = blogPostRepository.save(post);
        return Collections.singletonList(convertToDto(updatedPost));
    }

    @Override
    @Transactional
    public void deletePost(Long id, UserPrinciple user) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AnyException(
                       404,"Blog post not found with id: " + id));

        if (!post.getUserId().equals(user.getUserId())) {
            throw new AnyException(
                    403,"You can only delete your own post");
        }

        blogPostRepository.delete(post);
    }
    @Override
    public boolean likePost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AnyException(
                        404,
                        "Blog post not found with id: " + id));

        post.setLikes(post.getLikes() + 1);
        blogPostRepository.save(post);
        return true;
    }

    @Override
    public BlogShareResponse sharePost(Long id) {
        // Check if post exists
        blogPostRepository.findById(id)
                .orElseThrow(() -> new AnyException(
                       404,"Blog post not found with id: " + id));

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build().toUriString();
        String shareUrl = baseUrl + "/api/blogs/" + id;
        return new BlogShareResponse(shareUrl);
    }

    @Override
    public byte[] exportToPdf(Long id) throws Exception {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AnyException(
                        404, "Blog post not found with id: " + id));

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(bao);
        PdfDocument pdf = new PdfDocument(writer);
         Document document = new Document(pdf);

        document.add(new Paragraph("AgriConnect Blog").setFontSize(20).setBold());
        document.add(new Paragraph(post.getTitle()).setFontSize(18).setBold());
        document.add(new Paragraph("By: " + post.getAuthorName() +
                (post.getAuthorDesignation() != null ?
                        " (" + post.getAuthorDesignation() + ")" : "")));
        document.add(new Paragraph("\n" + post.getContent()).setFontSize(12));
        document.add(new Paragraph("\nLikes: " + post.getLikes()).setFontSize(10));

        document.close();
        return bao.toByteArray();
    }

    @Override
    @Transactional
    public void addComment(Long postId, BlogCommentDTO commentDto, UserPrinciple user) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new AnyException(
                        404,"Blog post not found with id: " + postId));

        String designation = "FARMER";
        if ("EXPERT".equalsIgnoreCase(user.getUserRole().name())) {
            designation = expertRepository.findByUserId(user.getUserId())
                    .map(Expert::getField)
                    .orElseThrow(() -> new AnyException(
                           404, "Expert not found for user ID: " + user.getUserId()));
        }

        BlogComment comment = BlogComment.builder()
                .text(commentDto.getText())
                .commenterName(user.getUsername() + " (" + designation + ")")
                .UserId(user.getUserId())
                .post(post)
                .build();

        blogCommentRepository.save(comment);
    }

    @Override
    public List<BlogCommentDTO> getComments(Long postId, UserPrinciple user) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new AnyException(
                        404,"Blog post not found with id: " + postId));

        return blogCommentRepository.findByPost(post).stream()
                .map(c -> BlogCommentDTO.builder()
                        .id(c.getId())
                        .text(c.getText())
                        .commenterName(c.getCommenterName())
                        .blogPostId(postId)
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }
}