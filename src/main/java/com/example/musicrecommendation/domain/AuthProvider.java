package com.example.musicrecommendation.domain;

/**
 * 인증 제공자를 구분하는 열거형
 * OAuth2 소셜 로그인과 아이디/비밀번호 로그인을 지원
 */
public enum AuthProvider {
    GOOGLE,
    GITHUB,
    KAKAO,
    NAVER,
    LOCAL  // 아이디/비밀번호 로그인
}