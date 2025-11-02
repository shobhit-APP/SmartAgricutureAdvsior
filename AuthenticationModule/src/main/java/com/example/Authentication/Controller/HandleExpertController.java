package com.example.Authentication.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Interface.ExpertService;
import com.example.Authentication.dto.ExpertDto;
import com.example.common.Model.UserDetails1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/experts")
@RequiredArgsConstructor
public class HandleExpertController {

    private final ExpertService expertService;
    @GetMapping("/review_expert")
    public ResponseEntity<Map<String, Object>> getExpertApplications(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @RequestParam(required = false, defaultValue = "all") String status) {

        Map<String, Object> response = new HashMap<>();

        if (userPrinciples == null || userPrinciples.getUserRole() != UserDetails1.UserRole.ADMIN) {
            response.put("success", false);
            response.put("message", "Access denied: Admin privileges required.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            List<ExpertDto> applications = switch (status.toLowerCase()) {
                case "pending" -> expertService.getPendingApplications();
                case "approved" -> expertService.getApprovedExperts();
                case "rejected" -> expertService.getRejectedExperts();
                case "verified" -> expertService.getVerifiedExperts();
                default -> expertService.getAllApplications();
            };

            response.put("success", true);
            response.put("applications", applications);
            response.put("count", applications.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching applications: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅ 2. Get specific expert details by ID
    @GetMapping("/{expertId}")
    public ResponseEntity<Map<String, Object>> getExpertDetails(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @PathVariable Long expertId) {

        Map<String, Object> response = new HashMap<>();

        if (userPrinciples == null || userPrinciples.getUserRole() != UserDetails1.UserRole.ADMIN) {
            response.put("success", false);
            response.put("message", "Access denied: Admin privileges required.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            ExpertDto expert = expertService.getExpertById(expertId);
            response.put("success", true);
            response.put("expert", expert);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // ✅ 3. Approve / Reject / Under Review
    @PostMapping("/handle_expert")
    public ResponseEntity<Map<String, Object>> handleExpertVerification(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();

        if (userPrinciples == null || userPrinciples.getUserRole() != UserDetails1.UserRole.ADMIN) {
            response.put("success", false);
            response.put("message", "Access denied: Admin privileges required.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            Long expertId = Long.parseLong(request.get("expertId").toString());
            String action = request.get("action").toString();
            String reason = request.containsKey("reason") ? request.get("reason").toString() : null;

            Map<String, Object> result = expertService.handleExpertVerification(expertId, action, reason);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
