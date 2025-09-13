package com.example.Authentication.UTIL;

import org.springframework.stereotype.Component;

@Component
public class validateNull {
      public boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
