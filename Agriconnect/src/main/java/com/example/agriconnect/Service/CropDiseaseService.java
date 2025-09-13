package com.example.agriconnect.Service;

import com.example.Authentication.repository.UserRepo;
import com.example.agriconnect.Repository.CropDiseaseRepo;
import com.example.common.Model.CropDisease;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing crop disease data associated with users.
 * Provides methods to retrieve, filter, and save crop disease records,
 * supporting pagination and localization for English and Hindi.
 */
@Service
public class CropDiseaseService {

    @Autowired
    private CropDiseaseRepo cropDiseaseRepository;

    @Autowired
    private UserRepo userRepo;

    /**
     * Retrieves a paginated list of all crop diseases associated with a specific user.
     *
     * @param userId   the ID of the user whose crop diseases are to be retrieved
     * @param pageable the pagination and sorting information
     * @return a {@link Page} of {@link CropDisease} objects associated with the user
     */
    public Page<CropDisease> findAllDiseasesByUserId(Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByUserDetails1UserId(userId, pageable);
    }

    /**
     * Retrieves a paginated list of crop diseases for a specific user, filtered by crop type in English.
     *
     * @param cropTypeEn the crop type in English to filter by
     * @param userId     the ID of the user whose crop diseases are to be retrieved
     * @param pageable   the pagination and sorting information
     * @return a {@link Page} of {@link CropDisease} objects matching the crop type and user
     */
    public Page<CropDisease> findByCropTypeAndUserId(String cropTypeEn, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeEnAndUserDetails1UserId(cropTypeEn, userId, pageable);
    }

    /**
     * Retrieves a paginated list of crop diseases for a specific user, filtered by a cause in English (case-insensitive).
     *
     * @param causeEn  the cause of the disease in English to filter by (case-insensitive)
     * @param userId   the ID of the user whose crop diseases are to be retrieved
     * @param pageable the pagination and sorting information
     * @return a {@link Page} of {@link CropDisease} objects matching the cause and user
     */
    public Page<CropDisease> findByCauseEnContainingIgnoreCaseAndUserId(String causeEn, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCauseEnContainingIgnoreCaseAndUserDetails1UserId(causeEn, userId, pageable);
    }

    /**
     * Retrieves a paginated list of crop diseases for a specific user, filtered by crop type and cause in English (case-insensitive).
     *
     * @param cropTypeEn the crop type in English to filter by
     * @param causeEn    the cause of the disease in English to filter by (case-insensitive)
     * @param userId     the ID of the user whose crop diseases are to be retrieved
     * @param pageable   the pagination and sorting information
     * @return a {@link Page} of {@link CropDisease} objects matching the crop type, cause, and user
     */
    public Page<CropDisease> findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserId(String cropTypeEn, String causeEn, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserDetails1UserId(cropTypeEn, causeEn, userId, pageable);
    }

    /**
     * Retrieves a paginated list of crop diseases for a specific user, filtered by crop type in Hindi.
     *
     * @param cropTypeHi the crop type in Hindi to filter by
     * @param userId     the ID of the user whose crop diseases are to be retrieved
     * @param pageable   the pagination and sorting information
     * @return a {@link Page} of {@link CropDisease} objects matching the crop type and user
     */
    public Page<CropDisease> findByCropTypeHiAndUserId(String cropTypeHi, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeHiAndUserDetails1UserId(cropTypeHi, userId, pageable);
    }

    /**
     * Retrieves a paginated list of crop diseases for a specific user, filtered by a cause in Hindi (case-insensitive).
     *
     * @param causeHi  the cause of the disease in Hindi to filter by (case-insensitive)
     * @param userId   the ID of the user whose crop diseases are to be retrieved
     * @param pageable the pagination and sorting information
     * @return a {@link Page} of {@link CropDisease} objects matching the cause and user
     */
    public Page<CropDisease> findByCauseHiContainingIgnoreCaseAndUserId(String causeHi, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCauseHiContainingIgnoreCaseAndUserDetails1UserId(causeHi, userId, pageable);
    }

    /**
     * Retrieves a paginated list of crop diseases for a specific user, filtered by crop type and cause in Hindi (case-insensitive).
     *
     * @param cropTypeHi the crop type in Hindi to filter by
     * @param causeHi    the cause of the disease in Hindi to filter by (case-insensitive)
     * @param userId     the ID of the user whose crop diseases are to be retrieved
     * @param pageable   the pagination and sorting information
     * @return a {@link Page} of {@link CropDisease} objects matching the crop type, cause, and user
     */
    public Page<CropDisease> findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserId(String cropTypeHi, String causeHi, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserDetails1UserId(cropTypeHi, causeHi, userId, pageable);
    }

    /**
     * Retrieves a specific crop disease by its ID and associated user ID.
     *
     * @param id     the ID of the crop disease to retrieve
     * @param userId the ID of the user associated with the crop disease
     * @return an {@link Optional} containing the {@link CropDisease} if found, or empty if not found
     */
    public Optional<CropDisease> findByIdAndUserId(Long id, Long userId) {
        return cropDiseaseRepository.findByIdAndUserDetails1UserId(id, userId);
    }

    /**
     * Retrieves a list of distinct crop types in English associated with a specific user.
     *
     * @param userId the ID of the user whose crop types are to be retrieved
     * @return a {@link List} of distinct crop type names in English
     */
    public List<String> findDistinctCropTypesByUserId(Long userId) {
        return cropDiseaseRepository.findDistinctCropTypesEnByUserId(userId);
    }

    /**
     * Retrieves a list of distinct crop types in Hindi associated with a specific user.
     *
     * @param userId the ID of the user whose crop types are to be retrieved
     * @return a {@link List} of distinct crop type names in Hindi
     */
    public List<String> findDistinctCropTypesHiByUserId(Long userId) {
        return cropDiseaseRepository.findDistinctCropTypesHiByUserId(userId);
    }

    /**
     * Saves a new crop disease record associated with a specific user.
     *
     * @param disease the {@link CropDisease} object to save
     * @param userId  the ID of the user to associate with the crop disease
     * @return the saved {@link CropDisease} object
     * @throws RuntimeException if the user with the specified ID is not found
     */
    public CropDisease saveDisease(CropDisease disease, Long userId) {
        disease.setUserDetails1(userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
        return cropDiseaseRepository.save(disease);
    }
}