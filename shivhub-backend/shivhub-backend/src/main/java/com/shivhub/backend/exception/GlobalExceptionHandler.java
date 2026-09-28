package com.shivhub.backend.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/*
 * =========================================================
 * GlobalExceptionHandler
 * =========================================================
 *
 * This class handles exceptions thrown from our services
 * and controllers.
 *
 * Instead of returning a confusing error page, we return
 * a clean JSON response.
 *
 * Example:
 *
 * {
 *     "message": "Your seller account is waiting for Admin approval"
 * }
 *
 * =========================================================
 */

@RestControllerAdvice
public class GlobalExceptionHandler {


    /*
     * =========================================================
     * RUNTIME EXCEPTION
     * =========================================================
     *
     * Handles RuntimeException thrown from services.
     *
     * Example:
     *
     * throw new RuntimeException(
     *     "Your seller account is waiting for Admin approval"
     * );
     *
     * =========================================================
     */

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException exception) {


        /*
         * Create response body.
         */

        Map<String, String> response =
                new HashMap<>();


        /*
         * Put exception message into JSON.
         */

        response.put(
                "message",
                exception.getMessage()
        );


        /*
         * Return HTTP 400 Bad Request.
         *
         * We will use 400 for business validation errors
         * such as duplicate email, invalid login, pending seller,
         * etc.
         */

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}