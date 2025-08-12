package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 정보 수정 요청 DTO
 * 현재는 이름과 프로필 이미지만 수정 가능
 */
@Schema(description = "사용자 정보 수정 요청")
public class UserUpdateRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 30, message = "이름은 1자 이상 30자 이하여야 합니다")
    @Schema(description = "수정할 사용자 이름", example = "원자혜", required = true)
    private String name;

    @Schema(description = "수정할 프로필 이미지 URL", example = "https://example.com/new-profile.jpg")
    private String profileImageUrl;

    /**
     * 기본 생성자
     */
    public UserUpdateRequest() {}

    /**
     * 모든 필드를 받는 생성자
     */
    public UserUpdateRequest(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    // === Getters and Setters ===
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
}