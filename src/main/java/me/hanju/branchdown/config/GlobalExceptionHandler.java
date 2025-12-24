package me.hanju.branchdown.config;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import me.hanju.branchdown.api.dto.CommonResponseDto;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<CommonResponseDto<Void>> handleNoSuchElementException(NoSuchElementException e) {
    log.warn("NoSuchElementException: {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(CommonResponseDto.error(e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<CommonResponseDto<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
    log.warn("IllegalArgumentException: {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(CommonResponseDto.error(e.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<CommonResponseDto<Void>> handleIllegalStateException(IllegalStateException e) {
    log.error("IllegalStateException: {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(CommonResponseDto.error(e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<CommonResponseDto<Map<String, String>>> handleValidationException(
      MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    log.warn("Validation failed: {}", errors);
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(CommonResponseDto.error("Validation failed", errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<CommonResponseDto<Void>> handleException(Exception e) {
    log.error("Unexpected exception", e);
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(CommonResponseDto.error("Internal server error: " + e.getMessage()));
  }
}
