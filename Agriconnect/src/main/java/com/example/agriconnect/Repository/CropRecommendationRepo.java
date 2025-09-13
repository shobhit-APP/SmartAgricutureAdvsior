
package com.example.agriconnect.Repository;

import com.example.common.Model.CropRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface CropRecommendationRepo extends JpaRepository<CropRecommendation, Integer> {

    /**
     * Fetch full CropRecommendation entities by user ID.
     *
     * @param userId the user ID
     * @return list of CropRecommendation entities
     */
    List<CropRecommendation> findByUserDetails1UserId(Long userId);

    /**
     * Fetch distinct PredictedCrop values (Strings) by user ID.
     *
     * @param userId the user ID
     * @return set of distinct PredictedCrop values
     */
    @Query("SELECT DISTINCT cr.predictedCrop FROM CropRecommendation cr WHERE cr.userDetails1.userId = :userId")
    Set<String> findDistinctPredictedCropsByUserId(@Param("userId") Long userId);

    /**
     * Fetch CropRecommendation entities filtered by user ID and PredictedCrop.
     *
     * @param userId the user ID
     * @param crop   the predicted crop
     * @return list of CropRecommendation entities
     */
    @Query("SELECT cr FROM CropRecommendation cr WHERE cr.userDetails1.userId = :userId AND cr.predictedCrop = :crop")
    List<CropRecommendation> findByUserIdAndPredictedCrop(@Param("userId") Long userId, @Param("crop") String crop);
//    List<CropRecommendation> findByHiContainingIgnoreCaseAndUserIdAndPredictedCrop(Long userId, String cropName);
//
//    List<CropRecommendation> findByEnContainingIgnoreCaseAndUserId(Long userId);
//    List<CropRecommendation> findByEnContainingIgnoreCaseAndUserIdAndPredictedCrop(Long userId, String cropName);
//    List<CropRecommendation> findByHiContainingIgnoreCaseAndUserId(Long userId);
}