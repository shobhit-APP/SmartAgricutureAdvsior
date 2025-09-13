package com.example.agriconnect.Repository;

import com.example.common.Model.CropDisease;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CropDiseaseRepo  extends JpaRepository<CropDisease, Long> {

    Page<CropDisease> findByUserDetails1UserId(Long userId, Pageable pageable);

    Page<CropDisease> findByCropTypeEnAndUserDetails1UserId(String cropTypeEn, Long userId, Pageable pageable);

    Page<CropDisease> findByCropTypeHiAndUserDetails1UserId(String cropTypeHi, Long userId, Pageable pageable);

    Page<CropDisease> findByCauseEnContainingIgnoreCaseAndUserDetails1UserId(String causeEn, Long userId, Pageable pageable);

    Page<CropDisease> findByCauseHiContainingIgnoreCaseAndUserDetails1UserId(String causeHi, Long userId, Pageable pageable);

    @Query("SELECT cd FROM CropDisease cd WHERE cd.cropTypeEn = :cropTypeEn AND LOWER(cd.causeEn) LIKE LOWER(CONCAT('%', :causeEn, '%')) AND cd.userDetails1.userId = :userId")
    Page<CropDisease> findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserDetails1UserId(@Param("cropTypeEn") String cropTypeEn, @Param("causeEn") String causeEn, @Param("userId") Long userId, Pageable pageable);

    @Query("SELECT cd FROM CropDisease cd WHERE cd.cropTypeHi = :cropTypeHi AND LOWER(cd.causeHi) LIKE LOWER(CONCAT('%', :causeHi, '%')) AND cd.userDetails1.userId = :userId")
    Page<CropDisease> findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserDetails1UserId(@Param("cropTypeHi") String cropTypeHi, @Param("causeHi") String causeHi, @Param("userId") Long userId, Pageable pageable);

    Optional<CropDisease> findByIdAndUserDetails1UserId(Long id, Long userId);

    @Query("SELECT DISTINCT cd.cropTypeEn FROM CropDisease cd WHERE cd.userDetails1.userId = :userId")
    List<String> findDistinctCropTypesEnByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT cd.cropTypeHi FROM CropDisease cd WHERE cd.userDetails1.userId = :userId")
    List<String> findDistinctCropTypesHiByUserId(@Param("userId") Long userId);

    List<CropDisease> findByUserDetails1UserId(Long userId);

    List<CropDisease> findByCropTypeEnAndUserDetails1UserId(String cropTypeEn, Long userId);

    List<CropDisease> findByCropTypeHiAndUserDetails1UserId(String cropTypeHi, Long userId);

    List<CropDisease> findByCauseEnContainingIgnoreCaseAndUserDetails1UserId(String causeEn, Long userId);

    List<CropDisease> findByCauseHiContainingIgnoreCaseAndUserDetails1UserId(String causeHi, Long userId);

    @Query("SELECT cd FROM CropDisease cd WHERE cd.cropTypeEn = :cropTypeEn AND LOWER(cd.causeEn) LIKE LOWER(CONCAT('%', :causeEn, '%')) AND cd.userDetails1.userId = :userId")
    List<CropDisease> findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserDetails1UserId(@Param("cropTypeEn") String cropTypeEn, @Param("causeEn") String causeEn, @Param("userId") Long userId);

    @Query("SELECT cd FROM CropDisease cd WHERE cd.cropTypeHi = :cropTypeHi AND LOWER(cd.causeHi) LIKE LOWER(CONCAT('%', :causeHi, '%')) AND cd.userDetails1.userId = :userId")
    List<CropDisease> findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserDetails1UserId(@Param("cropTypeHi") String cropTypeHi, @Param("causeHi") String causeHi, @Param("userId") Long userId);
}