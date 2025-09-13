package com.example.Authentication.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private static final String BLOCKED_USERS_SET = "blockedUsersId";

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate<String, Long> redisTemplate;

    public RedisService(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToBlockedUsers(Long userId) {
        try {
            redisTemplate.opsForSet().add(BLOCKED_USERS_SET, userId);
            logger.info("User ID {} added to Redis blocked set", userId);
        } catch (Exception e) {
            logger.error("Failed to add user ID {} to Redis blocked set", userId, e);
        }
    }

    public void removeFromBlockedUsers(Long userId) {
        try {
            redisTemplate.opsForSet().remove(BLOCKED_USERS_SET, userId);
            logger.info("User ID {} removed from Redis blocked set", userId);
        } catch (Exception e) {
            logger.error("Failed to remove user ID {} from Redis blocked set", userId, e);
        }
    }

    public boolean isUserBlocked(Long userId) {
        try {
            boolean result = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(BLOCKED_USERS_SET, userId));
            logger.info("Checked block status for user ID {}: {}", userId, result);
            return result;
        } catch (Exception e) {
            logger.warn("Redis unavailable while checking block status for user ID {}. Falling back to DB logic.", userId, e);
            return false; // Let DB handle fallback in service layer
        }
    }

    public long getBlockedUsersCount() {
        try {
            Long size = redisTemplate.opsForSet().size(BLOCKED_USERS_SET);
            logger.info("Total blocked users count from Redis: {}", size);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.error("Failed to retrieve blocked users count from Redis", e);
            return 0;
        }
    }

    public Set<Long> getAllBlockedUsers() {
        try {
            Set<Long> members = redisTemplate.opsForSet().members(BLOCKED_USERS_SET);
            logger.info("Fetched all blocked user IDs from Redis: {}", members);
            return members;
        } catch (Exception e) {
            logger.error("Failed to fetch blocked user IDs from Redis", e);
            return Set.of();
        }
    }
}
