package com.smartagriculture.community.Repository;


import com.smartagriculture.community.Model.VideoTutorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoTutorialRepository extends JpaRepository<VideoTutorial, Long> {

    List<VideoTutorial> findByTitleContainingIgnoreCase(String k);
    List<VideoTutorial> findByUploadedBy(String uploadedBy);
}