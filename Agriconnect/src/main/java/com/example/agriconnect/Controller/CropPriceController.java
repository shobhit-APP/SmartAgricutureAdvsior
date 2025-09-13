package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.CropPriceFacade;
import com.example.agriconnect.Service.MarketServices;
import com.example.common.Model.Crop;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@Slf4j
@RestController
@RequestMapping("/api")
public class CropPriceController {
    @Autowired
    private MarketServices services;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private HttpServletRequest request;

        private final CropPriceFacade cropPriceFacade;

        public CropPriceController(CropPriceFacade cropPriceFacade) {
            this.cropPriceFacade = cropPriceFacade;
        }

        @GetMapping("/csrf_token")
        public CsrfToken getCsrfToken(HttpServletRequest request) {
            return (CsrfToken) request.getAttribute("_csrf");
        }


        @GetMapping("/dashboard")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<?> getAllMarketDetails(@AuthenticationPrincipal UserPrinciple userPrinciples,
                                                     @RequestParam(required = false) String state) {
            if (userPrinciples == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            return cropPriceFacade.getDashboard(userPrinciples, state);
        }

        @GetMapping("/predict")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<?> pricePredictionModel(@RequestParam(value = "lat", required = false) Double latitude,
                                                      @RequestParam(value = "lon", required = false) Double longitude) {
            return cropPriceFacade.getPredictionPage(latitude, longitude);
        }

        @PostMapping("/predict")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<?> predict(@AuthenticationPrincipal UserPrinciple userPrinciples,
                                         @Valid @RequestBody Crop crop) {
            if (userPrinciples == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }
            return cropPriceFacade.predictCrop(userPrinciples, crop);
        }

}