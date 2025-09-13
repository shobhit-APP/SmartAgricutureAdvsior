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

@Service
public class CropDiseaseService {

    @Autowired
    private CropDiseaseRepo cropDiseaseRepository;

    @Autowired
    private UserRepo userRepo;

    public Page<CropDisease> findAllDiseasesByUserId(Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByUserDetails1UserId(userId, pageable);
    }

    public Page<CropDisease> findByCropTypeAndUserId(String cropTypeEn, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeEnAndUserDetails1UserId(cropTypeEn, userId, pageable);
    }

    public Page<CropDisease> findByCauseEnContainingIgnoreCaseAndUserId(String causeEn, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCauseEnContainingIgnoreCaseAndUserDetails1UserId(causeEn, userId, pageable);
    }

    public Page<CropDisease> findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserId(String cropTypeEn, String causeEn, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserDetails1UserId(cropTypeEn, causeEn, userId, pageable);
    }

    public Page<CropDisease> findByCropTypeHiAndUserId(String cropTypeHi, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeHiAndUserDetails1UserId(cropTypeHi, userId, pageable);
    }

    public Page<CropDisease> findByCauseHiContainingIgnoreCaseAndUserId(String causeHi, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCauseHiContainingIgnoreCaseAndUserDetails1UserId(causeHi, userId, pageable);
    }

    public Page<CropDisease> findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserId(String cropTypeHi, String causeHi, Long userId, Pageable pageable) {
        return cropDiseaseRepository.findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserDetails1UserId(cropTypeHi, causeHi, userId, pageable);
    }

    public Optional<CropDisease> findByIdAndUserId(Long id, Long userId) {
        return cropDiseaseRepository.findByIdAndUserDetails1UserId(id, userId);
    }

    public List<String> findDistinctCropTypesByUserId(Long userId) {
        return cropDiseaseRepository.findDistinctCropTypesEnByUserId(userId);
    }

    public List<String> findDistinctCropTypesHiByUserId(Long userId) {
        return cropDiseaseRepository.findDistinctCropTypesHiByUserId(userId);
    }

    public CropDisease saveDisease(CropDisease disease, Long userId) {
        disease.setUserDetails1(userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
        return cropDiseaseRepository.save(disease);
    }
}