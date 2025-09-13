package com.example.Authentication.Service;

import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Service class for handling user account operations such as blocking and unblocking users.
 * Interacts with the database and Redis to manage user status and blocked user lists.
 */
@Service
public class UserHandleService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserRepo userRepo;

    private static final Logger logger = LoggerFactory.getLogger(UserHandleService.class);

    /**
     * Blocks a user either temporarily (soft block) or permanently (hard block).
     * A hard block updates the user's status in the database to {@link UserDetails1.UserStatus#Blocked}
     * and adds the user to the Redis blocked users list. A soft block only adds the user to the Redis blocked users list.
     *
     * @param userId The ID of the user to block.
     * @param flag   The type of block: "Hard" for permanent block (database and Redis) or "Soft" for temporary block (Redis only).
     * @throws AnyException If the provided flag is invalid (not "Hard" or "Soft").
     */
    public void blockUser(Long userId, String flag) {
        if ("Hard".equalsIgnoreCase(flag)) {
            // HARD BLOCK: block in DB + Redis
            UserDetails1 userDetails1 = new UserDetails1();
            userDetails1.setUserId(userId);
            userDetails1.setStatus(UserDetails1.UserStatus.Blocked);
            userRepo.save(userDetails1);
            redisService.addToBlockedUsers(userId);
        } else if ("Soft".equalsIgnoreCase(flag)) {
            redisService.addToBlockedUsers(userId);
        } else {
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid flag. Allowed values are 'Hard' or 'Soft'");
        }
    }

    /**
     * Unblocks a user by setting their status to {@link UserDetails1.UserStatus#Active} in the database
     * and removing them from the Redis blocked users list.
     *
     * @param userId The ID of the user to unblock.
     */
    public void unblockUser(Long userId) {
        UserDetails1 userDetails1 = new UserDetails1();
        userDetails1.setUserId(userId);
        userDetails1.setStatus(UserDetails1.UserStatus.Active);
        userRepo.save(userDetails1);
        redisService.removeFromBlockedUsers(userId);
    }
}