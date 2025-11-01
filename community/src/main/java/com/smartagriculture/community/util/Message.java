package com.smartagriculture.community.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
public class Message {
    public ResponseEntity<Map<String, Object>> respond(
            boolean success,
            String message,
            Object data,
            String code,
            HttpStatus status) {

        Map<String, Object> res = new HashMap<>();
        res.put("success", success);
        res.put("message", message);
        if (data != null) res.put("data", data);
        if (code != null) res.put("code", code);

        return ResponseEntity.status(status != null ? status : (success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR))
                .body(res);
    }
    public ResponseEntity<Map<String, Object>> ok(String message, Object data) {
        return respond(true, message, data, null, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, Object>> badRequest(String message) {
        return respond(false, message, null, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }


    public ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return respond(false, message, null, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

     public ResponseEntity<Map<String, Object>> forbidden(Map<String, Object> res, String message) {
        return respond(false, message, null, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public ResponseEntity<Map<String, Object>> error(Map<String, Object> res, String message) {
        return respond(false, message, null, "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
