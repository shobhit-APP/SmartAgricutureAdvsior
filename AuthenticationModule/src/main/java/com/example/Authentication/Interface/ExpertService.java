package com.example.Authentication.Interface;
import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.dto.ExpertDto;
import com.example.common.Exception.AnyException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ExpertService {
    List<ExpertDto> getPendingApplications();
    List<ExpertDto> getAllApplications();
    ExpertDto getExpertById(Long expertId);
    Map<String, Object> handleExpertVerification(Long expertId, String action, String reason);
    ExpertDto getExpertByUserId(Long userId);
    List<ExpertDto> getVerifiedExperts();
    List<ExpertDto> getRejectedExperts();
    void submitExpertVerification(ExpertDto expertDto, MultipartFile uploadId, UserPrinciple userPrinciples, MultipartFile profile_image) throws AnyException;
}