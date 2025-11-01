package com.smartagriculture.community.Interface;



import com.smartagriculture.community.dto.BlogCommentDTO;

import java.util.List;

public interface BlogCommentService {
    BlogCommentDTO addComment(Long blogPostId, BlogCommentDTO dto);
    List<BlogCommentDTO> getCommentsByBlogPostId(Long blogPostId);
    void deleteComment(Long commentId); // Optional: for future
}