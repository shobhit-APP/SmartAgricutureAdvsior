package com.smartagriculture.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoTutorialDto {

    private Long id;

    @NotBlank(message = "Video title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Video URL is required")
    private String videoUrl;

    @NotBlank(message = "Uploaded by field is required")
    private String uploadedBy;

    private String designation;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String uploadDate; // will be string-formatted date

    private Long userId;

    private String thumbnailUrl;

    private String expertField;
}
