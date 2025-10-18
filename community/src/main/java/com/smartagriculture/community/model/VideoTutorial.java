package com.smartagriculture.community.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "video_tutorials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoTutorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Video title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 2000)
    private String description;

    @NotBlank(message = "Video URL is required")
    @Column(name = "video_url", nullable = false)
    private String videoUrl;

    @NotBlank(message = "Uploaded by field is required")
    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @Column(name = "designation")
    private String designation; // Expert / Farmer / Admin

    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
