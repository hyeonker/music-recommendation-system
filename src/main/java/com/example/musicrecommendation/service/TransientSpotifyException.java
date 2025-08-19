package com.example.musicrecommendation.service;

/**
 * 5xx/429/네트워크 등 "일시적 오류"를 나타내는 예외.
 * Resilience4j Retry의 타겟으로 사용.
 */
public class TransientSpotifyException extends RuntimeException {
    public TransientSpotifyException(String message) {
        super(message);
    }
    public TransientSpotifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
