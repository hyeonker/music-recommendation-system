package com.example.musicrecommendation.domain;

/**
 * OAuth2 소셜 로그인 제공자를 구분하는 열거형
 * 나중에 Google, GitHub, Kakao, Naver 등 확장 가능
 */
public enum AuthProvider {
    GOOGLE,
    GITHUB,
    KAKAO,
    NAVER
}