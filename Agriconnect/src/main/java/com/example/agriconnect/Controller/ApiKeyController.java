package com.example.agriconnect.Controller;


import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class ApiKeyController {

    @Value("${api.key}")
    private String apikey;
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/get-api-key")
    @ResponseBody
    public String getApikey()
    {
        return apikey;
    }
}
