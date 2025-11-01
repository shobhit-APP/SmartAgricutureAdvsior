package com.smartagriculture.community.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a video tutorial uploaded by a user (Farmer, Expert, or Admin).
 * Stores metadata including uploader details and Cloudinary URL for the video.
 */
@Entity
@Table(name = "video_tutorials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoTutorial {

    /** Unique identifier for the video tutorial. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Title of the video tutorial. */
    @NotBlank(message = "Video title is required")
    @Column(nullable = false)
    private String title;

    /** Description providing an overview of the video content. */
    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 2000)
    private String description;

    /** Cloudinary or external URL of the uploaded video. */
    @NotBlank(message = "Video URL is required")
    @Column(name = "video_url", nullable = false)
    private String videoUrl;

    /** Name of the user who uploaded the video. */
    @NotBlank(message = "Uploaded by field is required")
    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    /** Designation of the uploader — e.g., Expert, Farmer, or Admin. */
    @Column(name = "designation")
    private String designation;

    /** Upload timestamp. Automatically set during record creation. */
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    /** ID of the user (used for relational mapping or audits). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "thumbnailUrl" ,nullable = false)
    private String thumbnailUrl;

    /** Field to store expert-specific details (optional). */
    @Column(name = "expert_field")
    private String expertField;

    /**
     * Automatically sets the upload date before persisting a new record.
     */
    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
