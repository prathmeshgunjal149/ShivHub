package com.shivhub.backend.controller;

import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keeps domain validation failures meaningful without exposing stack traces or implementation details. */
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException error){return body(HttpStatus.BAD_REQUEST,error.getMessage());}
 @ExceptionHandler(SecurityException.class) public ResponseEntity<Map<String,String>> forbidden(SecurityException error){return body(HttpStatus.FORBIDDEN,error.getMessage());}
 @ExceptionHandler(OptimisticLockingFailureException.class) public ResponseEntity<Map<String,String>> conflict(OptimisticLockingFailureException error){return body(HttpStatus.CONFLICT,"This request was updated by another user. Refresh and try again.");}
 private ResponseEntity<Map<String,String>> body(HttpStatus status,String message){return ResponseEntity.status(status).body(Map.of("message",message==null||message.isBlank()?status.getReasonPhrase():message));}
}
