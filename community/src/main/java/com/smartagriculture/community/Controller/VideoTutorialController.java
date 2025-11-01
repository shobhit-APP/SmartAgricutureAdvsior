package com.smartagriculture.community.Controller;


import com.smartagriculture.community.Interface.VideoTutorialService;
import com.smartagriculture.community.Model.VideoTutorial;
import com.smartagriculture.community.dto.VideoTutorialDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
public class VideoTutorialController {

    private final VideoTutorialService videoService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoTutorialDto> createVideo(
            @Valid @RequestPart("dto") VideoTutorialDto dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile) {
        VideoTutorial video = videoService.createVideo(dto, thumbnailFile);
        return new ResponseEntity<>(videoService.toDto(video), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<VideoTutorial>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoTutorial> getVideo(@PathVariable Long id) {
        return ResponseEntity.ok(videoService.getVideoById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<VideoTutorial>> search(@RequestParam String q) {
        return ResponseEntity.ok(videoService.searchVideos(q));
    }

    @GetMapping("/uploader")
    public ResponseEntity<List<VideoTutorial>> getByUploader(@RequestParam String name) {
        return ResponseEntity.ok(videoService.getVideosByUploader(name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VideoTutorial> updateVideo(@PathVariable Long id, @Valid @RequestBody VideoTutorialDto dto) {
        return ResponseEntity.ok(videoService.updateVideo(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.noContent().build();
    }
}