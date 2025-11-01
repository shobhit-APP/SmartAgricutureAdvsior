package com.smartagriculture.community.Repository;


import com.smartagriculture.community.Model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    @Query("SELECT b FROM BlogPost b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.authorDesignation) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<BlogPost> searchBlogPosts(@Param("keyword") String keyword);

    List<BlogPost> findTop5ByOrderByLikesDesc();
}