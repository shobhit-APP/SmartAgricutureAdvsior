
package com.smartagriculture.community.Interface;


import com.smartagriculture.community.Model.VideoTutorial;
import com.smartagriculture.community.dto.VideoTutorialDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

    public interface VideoTutorialService {

        VideoTutorial createVideo(VideoTutorialDto dto, MultipartFile multipartFile);

        VideoTutorial getVideoById(Long id);

        List<VideoTutorial> getAllVideos();

        List<VideoTutorial> searchVideos(String keyword);

        VideoTutorial updateVideo(Long id, VideoTutorialDto dto);

        void deleteVideo(Long id);
        List<VideoTutorial> getVideosByUploader(String uploadedBy);

        VideoTutorialDto toDto(VideoTutorial entity);
    }
