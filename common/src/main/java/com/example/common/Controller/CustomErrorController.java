package com.example.common.Controller;

import com.example.common.Exception.AnyException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle custom error responses for the application.
 */
@RestController
public class CustomErrorController {
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    /**
     * Handles error requests and returns a direct error message as a string.
     *
     * @param request the HTTP request containing error attributes
     * @return ResponseEntity with a direct error message
     */
    @GetMapping("/error")
    public ResponseEntity<String> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; // Default status
        String errorMessage = "Unexpected error occurred";

        if (throwable != null) {
            logger.error("Error occurred: {}", throwable.getMessage(), throwable);
            if (throwable instanceof AnyException anyException) {
                errorMessage = anyException.getMessage() != null ? anyException.getMessage() : "Custom error occurred";
                httpStatus = HttpStatus.resolve(anyException.getStatusCode()) != null
                        ? HttpStatus.resolve(anyException.getStatusCode())
                        : HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());
                httpStatus = HttpStatus.resolve(statusCode) != null ? HttpStatus.resolve(statusCode) : HttpStatus.INTERNAL_SERVER_ERROR;
                errorMessage = switch (statusCode) {
                    case 400 -> "Bad Request";
                    case 401 -> "Unauthorized";
                    case 403 -> "Forbidden";
                    case 404 -> "Not Found";
                    case 405 -> "Method Not Allowed";
                    case 409 -> "Conflict";
                    case 410 -> "Gone";
                    case 415 -> "Unsupported Media Type";
                    case 429 -> "Too Many Requests";
                    case 500 -> "Internal Server Error";
                    case 502 -> "Bad Gateway";
                    case 503 -> "Service Unavailable";
                    default -> "Unexpected error occurred";
                };
            } catch (NumberFormatException e) {
                logger.warn("Invalid status code format: {}", status, e);
                errorMessage = "Invalid status code";
            }
        }

        logger.debug("Returning error response: status={}, message={}", httpStatus.value(), errorMessage);
        return ResponseEntity.status(httpStatus).body(errorMessage);
    }
}