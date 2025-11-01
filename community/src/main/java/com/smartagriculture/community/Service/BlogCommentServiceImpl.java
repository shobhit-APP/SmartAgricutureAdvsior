package com.smartagriculture.community.Service;


import com.example.common.Exception.AnyException;
import com.smartagriculture.community.Interface.BlogCommentService;
import com.smartagriculture.community.Model.BlogComment;
import com.smartagriculture.community.Model.BlogPost;
import com.smartagriculture.community.Repository.BlogCommentRepository;
import com.smartagriculture.community.Repository.BlogPostRepository;
import com.smartagriculture.community.dto.BlogCommentDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogCommentServiceImpl implements BlogCommentService {

    private final BlogCommentRepository commentRepository;
    private final BlogPostRepository blogPostRepository;

    @Override
    public BlogCommentDTO addComment(Long blogPostId, BlogCommentDTO dto) {
        BlogPost post = blogPostRepository.findById(blogPostId).orElseThrow(()-> new AnyException(
                      404, "Blog post not found with id: " + blogPostId));

        BlogComment comment = BlogComment.builder()
                .text(dto.getText())
                .commenterName(dto.getCommenterName())
                .post(post)
                .build();

        comment = commentRepository.save(comment);
        return toDto(comment);
    }

    @Override
    public List<BlogCommentDTO> getCommentsByBlogPostId(Long blogPostId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(blogPostId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new AnyException(
                    404,"Comment not found with id: " + commentId);
        }
        commentRepository.deleteById(commentId);
    }

    private BlogCommentDTO toDto(BlogComment comment) {
        return BlogCommentDTO.builder()
                .id(comment.getId())
                .text(comment.getText())
                .commenterName(comment.getCommenterName())
                .blogPostId(comment.getPost().getId())
                .createdAt(comment.getCreatedAt().toString())
                .build();
    }
}