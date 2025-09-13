package com.example.Authentication.Controller;

import com.example.Authentication.Service.RedisService;
import com.example.Authentication.Service.UserHandleService;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/handle")
@Tag(name = "User Handle API", description = "Administrative endpoints for managing user block status")
public class UserHandleController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserHandleService userHandleService;

    @Autowired
    private UserRepo userRepo;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/block")
    @Operation(
            summary = "Block a user",
            description = "Blocks a user by ID with a specified flag, restricted to admin users."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User blocked successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid user ID or flag",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - admin role required",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user lacks admin privileges",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> blockUser(
            @Parameter(description = "User ID and flag (e.g., reason for blocking)", required = true)
            @RequestBody Map<String, String> request) {
        Long userId = Long.valueOf(request.get("userId"));
        String flag = request.get("flag");

        // Validate input parameters
        if (flag == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User ID and flag are required"));
        }

        try {
            // Block user with specified flag
            userHandleService.blockUser(userId, flag);
            return ResponseEntity.ok().body(Map.of("message", "User blocked successfully"));
        } catch (AnyException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/unblock")
    @Operation(
            summary = "Unblock a user",
            description = "Unblocks a user by ID, restricted to admin users."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User unblocked successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid user ID",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - admin role required",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user lacks admin privileges",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> unblockUser(
            @Parameter(description = "User ID to unblock", required = true)
            @RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        // Validate input parameter
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User ID is required"));
        }
        try {
            // Unblock user
            userHandleService.unblockUser(userId);
            return ResponseEntity.ok().body(Map.of("message", "User unblocked successfully"));
        } catch (AnyException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/check-blocked")
    @Operation(
            summary = "Check if a user is blocked",
            description = "Checks if a user is blocked in Redis and retrieves their database status, restricted to admin users."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User block status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing key or value",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - admin role required",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user lacks admin privileges",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> checkBlockedUser(
            @Parameter(description = "Key for checking block status (e.g., 'userId')", required = true, example = "userId")
            @RequestParam("key") String key,
            @Parameter(description = "User ID to check", required = true, example = "1")
            @RequestParam("value") Long value) {
        // Validate input parameters
        if (key == null || value == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Key and value are required"));
        }

        // Fetch user details
        UserDetails1 user = userRepo.findByUserId(value);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        // Check block status in Redis and database
        boolean isBlockedInRedis = redisService.isUserBlocked(user.getUserId());
        String dbStatus = user.getStatus().name();

        return ResponseEntity.ok().body(Map.of(
                "userId", user.getUserId(),
                "isBlockedInRedis", isBlockedInRedis,
                "databaseStatus", dbStatus
        ));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/blocked-users/count")
    @Operation(
            summary = "Get blocked users count and list",
            description = "Retrieves the count and list of blocked user IDs, restricted to admin users."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Blocked users count and list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - admin role required",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user lacks admin privileges",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> getBlockedUsersCount() {
        // Retrieve blocked users count and list from Redis
        long count = redisService.getBlockedUsersCount();
        Set<Long> blockedUsers = redisService.getAllBlockedUsers();
        return ResponseEntity.ok().body(Map.of(
                "blockedUsersCount", count,
                "blockedUsers", blockedUsers
        ));
    }
}