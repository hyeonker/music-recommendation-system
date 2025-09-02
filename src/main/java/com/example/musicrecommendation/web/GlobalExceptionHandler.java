package com.example.musicrecommendation.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* === 외부 API 에러는 원본 상태코드/본문을 그대로 릴레이 === */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> relayUpstream(RestClientResponseException ex) {
        HttpHeaders headers = new HttpHeaders();
        // 가능하면 원본 헤더를 그대로 사용
        if (ex.getResponseHeaders() != null) {
            headers.putAll(ex.getResponseHeaders());
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return ResponseEntity
                .status(ex.getStatusCode())
                .headers(headers)
                .body(ex.getResponseBodyAsString());
    }

    // 400: @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body("validation error", msg, req.getRequestURI(), HttpStatus.BAD_REQUEST));
    }

    // 409: 낙관적 락 충돌(stale version) 또는 유니크 제약 위반 등
    @ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
    public ResponseEntity<Map<String, Object>> handleConflict(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body("conflict",
                        Optional.ofNullable(ex.getMessage()).orElse("constraint violation"),
                        req.getRequestURI(),
                        HttpStatus.CONFLICT));
    }

    // 404: 리소스를 찾을 수 없음
    @ExceptionHandler({ IllegalArgumentException.class, EmptyResultDataAccessException.class })
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body("not found",
                        Optional.ofNullable(ex.getMessage()).orElse("resource not found"),
                        req.getRequestURI(),
                        HttpStatus.NOT_FOUND));
    }

    // 500: 그 외 미처리 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("internal error",
                        Optional.ofNullable(ex.getMessage()).orElse(ex.getClass().getSimpleName()),
                        req.getRequestURI(),
                        HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /** 공통 응답 바디 생성 (null-safe, 불변 Map 사용 안 함) */
    private Map<String, Object> body(String message, String detail, String path, HttpStatus status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toString());
        m.put("status", status.value());
        m.put("error", status.getReasonPhrase());
        m.put("message", message);
        if (detail != null) m.put("detail", detail);
        if (path != null) m.put("path", path);
        return m;
    }
}
