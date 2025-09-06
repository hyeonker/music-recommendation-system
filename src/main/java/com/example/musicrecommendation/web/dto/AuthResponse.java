package com.example.musicrecommendation.web.dto;

import com.example.musicrecommendation.domain.User;

/**
 * 인증 응답 DTO
 */
public class AuthResponse {

    private boolean success;
    private String message;
    private UserResponse user;

    public AuthResponse() {}

    public AuthResponse(boolean success, String message, UserResponse user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public static AuthResponse success(String message, User user) {
        return new AuthResponse(true, message, UserResponse.from(user));
    }

    public static AuthResponse failure(String message) {
        return new AuthResponse(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}