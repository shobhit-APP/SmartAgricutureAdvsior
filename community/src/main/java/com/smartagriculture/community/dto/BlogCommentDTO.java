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
public class BlogCommentDTO {

    private Long id;

    @NotBlank(message = "Comment text cannot be empty")
    private String text;

    @NotBlank(message = "Commenter name is required")
    private String commenterName;

    private Long blogPostId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String createdAt;

    public BlogCommentDTO(Long id, String text, String commenterName, String string) {
        this.commenterName=commenterName;
        this.createdAt=string;
        this.id=id;
        this.text=text;
    }
}