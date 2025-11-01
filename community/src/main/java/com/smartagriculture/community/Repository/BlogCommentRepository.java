package com.smartagriculture.community.Repository;


import com.smartagriculture.community.Model.BlogComment;
import com.smartagriculture.community.Model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {
    List<BlogComment> findByPostId(Long postId);
    List<BlogComment> findByPostIdOrderByCreatedAtDesc(Long postId);
    List<BlogComment> findByPost(BlogPost post);
}