package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * OAuth2 소셜 로그인을 통해 가입한 사용자 정보를 저장
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 이메일 (고유)
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * 사용자 실명
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 프로필 이미지 URL (소셜 로그인에서 가져옴)
     */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * OAuth2 제공자 (GOOGLE, GITHUB 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    /**
     * OAuth2 제공자에서의 사용자 고유 ID
     */
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    /**
     * 계정 생성일시 (자동 설정)
     */
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 계정 수정일시 (자동 업데이트)
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA 기본 생성자 (protected로 외부 생성 방지)
     */
    protected User() {}

    /**
     * OAuth2 로그인용 생성자
     *
     * @param email 이메일
     * @param name 실명
     * @param profileImageUrl 프로필 이미지 URL
     * @param provider OAuth2 제공자
     * @param providerId 제공자에서의 사용자 ID
     */
    public User(String email, String name, String profileImageUrl,
                AuthProvider provider, String providerId) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
    }

    // === Getters ===
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // === 비즈니스 메서드 ===

    /**
     * 프로필 정보 업데이트
     * OAuth2 로그인 시 변경된 정보가 있으면 업데이트
     *
     * @param name 새로운 이름
     * @param profileImageUrl 새로운 프로필 이미지 URL
     */
    public void updateProfile(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 사용자 표시용 문자열
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", provider=" + provider +
                '}';
    }
}