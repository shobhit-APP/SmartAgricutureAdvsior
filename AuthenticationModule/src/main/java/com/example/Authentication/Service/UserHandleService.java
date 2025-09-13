package com.example.Authentication.Service;

import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserHandleService {
    @Autowired
    private RedisService redisService;
    @Autowired
    private UserRepo userRepo;

    private static final Logger logger = LoggerFactory.getLogger(UserHandleService.class);
    public void blockUser(Long userId, String flag) {
        if ("Hard".equalsIgnoreCase(flag)) {
            // HARD BLOCK: block in DB + Redis
            UserDetails1 userDetails1=new UserDetails1();
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

    public void unblockUser(Long userId) {
        UserDetails1 userDetails1=new UserDetails1();
        userDetails1.setUserId(userId);
        userDetails1.setStatus(UserDetails1.UserStatus.Active);
        userRepo.save(userDetails1);
        redisService.removeFromBlockedUsers(userId);
    }


}
