package com.example.musicrecommendation.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* === 외부 API 에러는 원본 상태코드/본문을 그대로 릴레이 === */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> relayUpstream(RestClientResponseException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // ex.getStatusCode() 가 400/401/403/429/500 등 원본 상태로 내려감
        return new ResponseEntity<>(ex.getResponseBodyAsString(), headers, ex.getStatusCode());
    }

    // 400: @Valid 검증 실패
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        var msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Validation error");
        return Map.of("message", msg);
    }

    // 409: 낙관적 락 충돌(stale version) 또는 유니크 제약 위반 등
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
    public Map<String, Object> handleConflict(Exception ex) {
        return Map.of("message", "conflict", "detail", ex.getMessage());
    }

    // 404: 리소스를 찾을 수 없음
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({ IllegalArgumentException.class, EmptyResultDataAccessException.class })
    public Map<String, Object> handleNotFound(Exception ex) {
        return Map.of("message", "not found", "detail", ex.getMessage());
    }

    // 500: 그 외 미처리 예외
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleUnknown(Exception ex) {
        return Map.of("message", "internal error", "detail", ex.getMessage());
    }
}
