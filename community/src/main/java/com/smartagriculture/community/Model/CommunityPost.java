package com.smartagriculture.community.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "community_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Content cannot be empty")
    @Column(nullable = false, length = 2000)
    private String content;

    @NotBlank(message = "Author name is required")
    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Expert field / designation (e.g., Agronomist, Soil Expert) */
    @Column(name = "designation")
    private String designation;

    /** Optional Cloudinary URL for the thumbnail */
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    /** Optional Cloudinary public ID for management */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}