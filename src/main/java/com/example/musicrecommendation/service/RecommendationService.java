package com.example.musicrecommendation.service;

import org.springframework.stereotype.Service;

/**
 * 기본 추천 서비스 (간단한 임시 구현)
 */
@Service
public class RecommendationService {

    /**
     * 개인화 추천
     */
    public Object getPersonalizedRecommendations(Long userId, int limit) {
        return new Object() {
            public final String title = "개인화 추천";
            public final String description = "사용자 " + userId + "를 위한 개인화 추천";
            public final int count = limit;
            public final String message = "추천 시스템이 준비 중입니다";
        };
    }

    /**
     * 유사곡 추천
     */
    public Object getSimilarSongs(Long songId, int limit) {
        return new Object() {
            public final String title = "유사곡 추천";
            public final String description = "곡 ID " + songId + "와 비슷한 곡들";
            public final int count = limit;
            public final String message = "유사곡 분석 중입니다";
        };
    }

    /**
     * 인기곡 추천
     */
    public Object getPopularRecommendations(String genre, int limit) {
        String genreText = genre != null ? genre : "전체";
        return new Object() {
            public final String title = "인기곡 추천";
            public final String description = genreText + " 장르의 인기 곡들";
            public final int count = limit;
            public final String message = "인기곡 데이터 수집 중입니다";
        };
    }

    /**
     * 종합 추천
     */
    public Object getHybridRecommendations(Long userId, int limit) {
        return new Object() {
            public final String title = "종합 추천";
            public final String description = "사용자 " + userId + "를 위한 종합 추천";
            public final int count = limit;
            public final String message = "종합 분석 중입니다";
        };
    }
}