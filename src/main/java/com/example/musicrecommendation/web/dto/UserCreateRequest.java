package com.example.musicrecommendation.web.dto;

import com.example.musicrecommendation.domain.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 사용자 생성 요청 DTO
 * 관리자가 직접 사용자를 생성하거나, 테스트용으로 사용
 * (실제로는 OAuth2 자동 생성이 주요 방식)
 */
@Schema(description = "사용자 생성 요청")
public class UserCreateRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    private String email;

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 100, message = "이름은 1자 이상 100자 이하여야 합니다")
    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    private String name;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @NotNull(message = "OAuth2 제공자는 필수입니다")
    @Schema(description = "OAuth2 제공자", example = "GOOGLE", required = true)
    private AuthProvider provider;

    @NotBlank(message = "제공자 ID는 필수입니다")
    @Schema(description = "OAuth2 제공자에서의 사용자 ID", example = "google-user-123", required = true)
    private String providerId;

    /**
     * 기본 생성자
     */
    public UserCreateRequest() {}

    /**
     * 모든 필드를 받는 생성자
     */
    public UserCreateRequest(String email, String name, String profileImageUrl,
                             AuthProvider provider, String providerId) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
    }

    // === Getters and Setters ===
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}