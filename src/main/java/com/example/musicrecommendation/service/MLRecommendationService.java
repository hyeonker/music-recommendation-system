package com.example.musicrecommendation.service;

import com.example.musicrecommendation.config.RecommendationProperties;
import com.example.musicrecommendation.domain.UserBehaviorEvent;
import com.example.musicrecommendation.domain.UserMusicPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ML 기반 추천 시스템
 * - 딥러닝 모델 통합 준비
 * - 특성 엔지니어링
 * - 앙상블 모델 결합
 * - A/B 테스트 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MLRecommendationService {
    
    private final UserBehaviorTrackingService behaviorService;
    private final UserMusicPreferencesService preferencesService;
    private final AdvancedCollaborativeFilteringService collaborativeService;
    private final RecommendationProperties properties;
    
    /**
     * ML 기반 하이브리드 추천 (메인 엔트리포인트)
     */
    @Cacheable(value = "mlRecommendations", key = "#userId + '_' + #limit")
    public List<MLRecommendationItem> getMLBasedRecommendations(Long userId, int limit) {
        log.debug("사용자 {} ML 기반 추천 생성 시작", userId);
        
        try {
            // 1. 사용자 특성 벡터 생성
            UserFeatureVector userFeatures = buildUserFeatureVector(userId);
            
            // 2. 다중 알고리즘 추천 생성
            List<MLRecommendationItem> contentBased = getContentBasedRecommendations(userFeatures, limit);
            List<MLRecommendationItem> collaborative = getCollaborativeFilteringRecommendations(userId, limit);
            List<MLRecommendationItem> deepLearning = getDeepLearningRecommendations(userFeatures, limit);
            List<MLRecommendationItem> contextual = getContextualRecommendations(userFeatures, limit);
            
            // 3. 앙상블 모델로 결합
            List<MLRecommendationItem> ensemble = combineWithEnsemble(
                contentBased, collaborative, deepLearning, contextual, limit
            );
            
            // 4. 다양성 최적화
            List<MLRecommendationItem> diversified = optimizeDiversity(ensemble, limit);
            
            log.debug("사용자 {} ML 추천 {} 개 생성 완료", userId, diversified.size());
            return diversified;
            
        } catch (Exception e) {
            log.error("ML 기반 추천 생성 실패 (userId: {}): {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 사용자 특성 벡터 생성
     */
    private UserFeatureVector buildUserFeatureVector(Long userId) {
        UserMusicPreferences prefs = preferencesService.getUserPreferences(userId);
        UserBehaviorTrackingService.UserBehaviorAnalysis behavior = 
            behaviorService.analyzeUserBehavior(userId, 30);
        
        return UserFeatureVector.builder()
            .userId(userId)
            .genrePreferences(extractGenreVector(prefs))
            .audioFeaturePreferences(extractAudioFeatureVector(prefs))
            .temporalFeatures(extractTemporalFeatures(behavior))
            .engagementFeatures(extractEngagementFeatures(behavior))
            .contextualFeatures(extractContextualFeatures())
            .build();
    }
    
    /**
     * 컨텐츠 기반 추천 (개선된 버전)
     */
    private List<MLRecommendationItem> getContentBasedRecommendations(UserFeatureVector features, int limit) {
        List<MLRecommendationItem> recommendations = new ArrayList<>();
        
        // 장르 기반 추천
        Map<String, Double> genrePrefs = features.getGenrePreferences();
        for (Map.Entry<String, Double> entry : genrePrefs.entrySet()) {
            if (entry.getValue() > 0.5) {
                // 시뮬레이션된 장르 기반 추천
                for (int i = 0; i < Math.min(3, limit / genrePrefs.size()); i++) {
                    recommendations.add(MLRecommendationItem.builder()
                        .itemId("content_" + entry.getKey() + "_" + i)
                        .score(entry.getValue() * (0.8 + Math.random() * 0.2))
                        .confidence(0.75)
                        .algorithm("CONTENT_BASED")
                        .features(Map.of("genre", entry.getKey(), "content_score", entry.getValue()))
                        .build());
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * 협업 필터링 추천 (ML 래퍼)
     */
    private List<MLRecommendationItem> getCollaborativeFilteringRecommendations(Long userId, int limit) {
        return collaborativeService.getCollaborativeRecommendations(userId, limit)
            .stream()
            .map(item -> MLRecommendationItem.builder()
                .itemId(item.getItemId())
                .score(item.getScore())
                .confidence(item.getConfidence())
                .algorithm("COLLABORATIVE_FILTERING")
                .features(Map.of("cf_algorithm", item.getAlgorithm()))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * 딥러닝 기반 추천 (시뮬레이션)
     */
    private List<MLRecommendationItem> getDeepLearningRecommendations(UserFeatureVector features, int limit) {
        // 실제 구현에서는 TensorFlow/PyTorch 모델 호출
        // 현재는 시뮬레이션된 딥러닝 결과
        List<MLRecommendationItem> recommendations = new ArrayList<>();
        
        double baseScore = features.getEngagementFeatures().getOrDefault("engagement_score", 0.5);
        
        for (int i = 0; i < Math.min(limit, 10); i++) {
            double score = baseScore * (0.7 + Math.random() * 0.3);
            recommendations.add(MLRecommendationItem.builder()
                .itemId("dl_rec_" + i)
                .score(score)
                .confidence(0.85)
                .algorithm("DEEP_LEARNING")
                .features(Map.of(
                    "neural_score", score,
                    "embedding_similarity", Math.random() * 0.5 + 0.5
                ))
                .build());
        }
        
        return recommendations;
    }
    
    /**
     * 상황 인식 추천 (시간, 디바이스, 위치 기반)
     */
    private List<MLRecommendationItem> getContextualRecommendations(UserFeatureVector features, int limit) {
        List<MLRecommendationItem> recommendations = new ArrayList<>();
        
        Map<String, Double> contextualFeats = features.getContextualFeatures();
        double timeScore = contextualFeats.getOrDefault("time_score", 0.5);
        
        // 시간대별 추천
        int currentHour = LocalDateTime.now().getHour();
        String contextType = getContextType(currentHour);
        
        for (int i = 0; i < Math.min(limit, 5); i++) {
            recommendations.add(MLRecommendationItem.builder()
                .itemId("contextual_" + contextType + "_" + i)
                .score(timeScore * (0.6 + Math.random() * 0.4))
                .confidence(0.7)
                .algorithm("CONTEXTUAL")
                .features(Map.of(
                    "context_type", contextType,
                    "time_relevance", timeScore,
                    "hour", currentHour
                ))
                .build());
        }
        
        return recommendations;
    }
    
    /**
     * 앙상블 모델로 추천 결합
     */
    private List<MLRecommendationItem> combineWithEnsemble(
            List<MLRecommendationItem> contentBased,
            List<MLRecommendationItem> collaborative, 
            List<MLRecommendationItem> deepLearning,
            List<MLRecommendationItem> contextual,
            int limit) {
        
        Map<String, MLRecommendationItem> combined = new HashMap<>();
        
        // 가중치 설정 (설정 파일에서 가져온 값 사용)
        double contentWeight = properties.getWeights().getContentBased();     // 0.4
        double collaborativeWeight = properties.getWeights().getCollaborative(); // 0.3
        double deepLearningWeight = 0.2;  // 딥러닝
        double contextualWeight = 0.1;    // 상황 인식
        
        // 각 알고리즘별 가중 평균
        addWeightedRecommendations(combined, contentBased, contentWeight);
        addWeightedRecommendations(combined, collaborative, collaborativeWeight);
        addWeightedRecommendations(combined, deepLearning, deepLearningWeight);
        addWeightedRecommendations(combined, contextual, contextualWeight);
        
        return combined.values().stream()
            .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 다양성 최적화 (MMR - Maximal Marginal Relevance)
     */
    private List<MLRecommendationItem> optimizeDiversity(List<MLRecommendationItem> recommendations, int limit) {
        if (recommendations.isEmpty()) return recommendations;
        
        List<MLRecommendationItem> diversified = new ArrayList<>();
        List<MLRecommendationItem> remaining = new ArrayList<>(recommendations);
        
        // 첫 번째는 점수가 가장 높은 것
        MLRecommendationItem first = remaining.remove(0);
        diversified.add(first);
        
        double lambda = 0.7; // 관련성 vs 다양성 균형 (0.7 = 관련성 중시)
        
        while (diversified.size() < limit && !remaining.isEmpty()) {
            double maxMmr = Double.NEGATIVE_INFINITY;
            MLRecommendationItem bestItem = null;
            int bestIndex = -1;
            
            for (int i = 0; i < remaining.size(); i++) {
                MLRecommendationItem candidate = remaining.get(i);
                
                // MMR 계산: λ * 관련성 - (1-λ) * max(기존 아이템들과의 유사도)
                double relevance = candidate.getScore();
                double maxSimilarity = diversified.stream()
                    .mapToDouble(existing -> calculateSimilarity(candidate, existing))
                    .max()
                    .orElse(0.0);
                
                double mmr = lambda * relevance - (1 - lambda) * maxSimilarity;
                
                if (mmr > maxMmr) {
                    maxMmr = mmr;
                    bestItem = candidate;
                    bestIndex = i;
                }
            }
            
            if (bestItem != null) {
                diversified.add(bestItem);
                remaining.remove(bestIndex);
            } else {
                break;
            }
        }
        
        return diversified;
    }
    
    // Helper methods
    private Map<String, Double> extractGenreVector(UserMusicPreferences prefs) {
        Map<String, Double> genreVector = new HashMap<>();
        
        if (prefs.getGenrePreferences() != null) {
            prefs.getGenrePreferences().fields().forEachRemaining(field -> 
                genreVector.put(field.getKey(), field.getValue().asDouble())
            );
        }
        
        return genreVector;
    }
    
    private Map<String, Double> extractAudioFeatureVector(UserMusicPreferences prefs) {
        Map<String, Double> audioFeatures = new HashMap<>();
        
        if (prefs.getAudioFeatures() != null) {
            prefs.getAudioFeatures().fields().forEachRemaining(field ->
                audioFeatures.put(field.getKey(), field.getValue().asDouble())
            );
        }
        
        return audioFeatures;
    }
    
    private Map<String, Double> extractTemporalFeatures(
            UserBehaviorTrackingService.UserBehaviorAnalysis behavior) {
        Map<String, Double> temporal = new HashMap<>();
        temporal.put("most_active_hour", (double) behavior.getMostActiveHour());
        temporal.put("avg_session_length", behavior.getAverageSessionLength());
        temporal.put("weekend_preference", Math.random() * 0.5 + 0.25); // 시뮬레이션
        return temporal;
    }
    
    private Map<String, Double> extractEngagementFeatures(
            UserBehaviorTrackingService.UserBehaviorAnalysis behavior) {
        Map<String, Double> engagement = new HashMap<>();
        engagement.put("engagement_score", behavior.getEngagementScore());
        engagement.put("positivity_ratio", behavior.getPositivityRatio());
        engagement.put("total_events", (double) behavior.getTotalEvents());
        return engagement;
    }
    
    private Map<String, Double> extractContextualFeatures() {
        Map<String, Double> contextual = new HashMap<>();
        int currentHour = LocalDateTime.now().getHour();
        contextual.put("current_hour", (double) currentHour);
        contextual.put("time_score", getTimeScore(currentHour));
        contextual.put("is_weekend", LocalDateTime.now().getDayOfWeek().getValue() > 5 ? 1.0 : 0.0);
        return contextual;
    }
    
    private double getTimeScore(int hour) {
        // 시간대별 음악 선호도 가중치 (주관적)
        if (hour >= 6 && hour < 9) return 0.7;   // 아침: 밝은 음악
        if (hour >= 9 && hour < 12) return 0.6;  // 오전: 집중 음악
        if (hour >= 12 && hour < 14) return 0.5; // 점심: 편안한 음악
        if (hour >= 14 && hour < 18) return 0.6; // 오후: 활기찬 음악
        if (hour >= 18 && hour < 22) return 0.8; // 저녁: 다양한 음악
        return 0.4; // 밤/새벽: 조용한 음악
    }
    
    private String getContextType(int hour) {
        if (hour >= 6 && hour < 9) return "morning";
        if (hour >= 9 && hour < 12) return "work";
        if (hour >= 12 && hour < 14) return "lunch";
        if (hour >= 14 && hour < 18) return "afternoon";
        if (hour >= 18 && hour < 22) return "evening";
        return "night";
    }
    
    private void addWeightedRecommendations(Map<String, MLRecommendationItem> combined,
                                          List<MLRecommendationItem> recommendations, 
                                          double weight) {
        for (MLRecommendationItem item : recommendations) {
            String itemId = item.getItemId();
            if (combined.containsKey(itemId)) {
                MLRecommendationItem existing = combined.get(itemId);
                double newScore = existing.getScore() + (item.getScore() * weight);
                combined.put(itemId, existing.withScore(newScore).withAlgorithm("ENSEMBLE"));
            } else {
                combined.put(itemId, item.withScore(item.getScore() * weight));
            }
        }
    }
    
    private double calculateSimilarity(MLRecommendationItem item1, MLRecommendationItem item2) {
        // 간단한 유사도 계산 (실제로는 더 복잡한 특성 벡터 유사도 계산)
        String alg1 = item1.getAlgorithm();
        String alg2 = item2.getAlgorithm();
        
        if (alg1.equals(alg2)) return 0.8; // 같은 알고리즘이면 높은 유사도
        
        // 특성 기반 유사도 (시뮬레이션)
        return Math.random() * 0.3 + 0.1;
    }
    
    // Inner Classes
    public static class UserFeatureVector {
        private final Long userId;
        private final Map<String, Double> genrePreferences;
        private final Map<String, Double> audioFeaturePreferences;
        private final Map<String, Double> temporalFeatures;
        private final Map<String, Double> engagementFeatures;
        private final Map<String, Double> contextualFeatures;
        
        private UserFeatureVector(Long userId, Map<String, Double> genrePreferences,
                                Map<String, Double> audioFeaturePreferences,
                                Map<String, Double> temporalFeatures,
                                Map<String, Double> engagementFeatures,
                                Map<String, Double> contextualFeatures) {
            this.userId = userId;
            this.genrePreferences = genrePreferences;
            this.audioFeaturePreferences = audioFeaturePreferences;
            this.temporalFeatures = temporalFeatures;
            this.engagementFeatures = engagementFeatures;
            this.contextualFeatures = contextualFeatures;
        }
        
        public static UserFeatureVectorBuilder builder() {
            return new UserFeatureVectorBuilder();
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public Map<String, Double> getGenrePreferences() { return genrePreferences; }
        public Map<String, Double> getAudioFeaturePreferences() { return audioFeaturePreferences; }
        public Map<String, Double> getTemporalFeatures() { return temporalFeatures; }
        public Map<String, Double> getEngagementFeatures() { return engagementFeatures; }
        public Map<String, Double> getContextualFeatures() { return contextualFeatures; }
        
        public static class UserFeatureVectorBuilder {
            private Long userId;
            private Map<String, Double> genrePreferences = new HashMap<>();
            private Map<String, Double> audioFeaturePreferences = new HashMap<>();
            private Map<String, Double> temporalFeatures = new HashMap<>();
            private Map<String, Double> engagementFeatures = new HashMap<>();
            private Map<String, Double> contextualFeatures = new HashMap<>();
            
            public UserFeatureVectorBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }
            
            public UserFeatureVectorBuilder genrePreferences(Map<String, Double> genrePreferences) {
                this.genrePreferences = genrePreferences;
                return this;
            }
            
            public UserFeatureVectorBuilder audioFeaturePreferences(Map<String, Double> audioFeaturePreferences) {
                this.audioFeaturePreferences = audioFeaturePreferences;
                return this;
            }
            
            public UserFeatureVectorBuilder temporalFeatures(Map<String, Double> temporalFeatures) {
                this.temporalFeatures = temporalFeatures;
                return this;
            }
            
            public UserFeatureVectorBuilder engagementFeatures(Map<String, Double> engagementFeatures) {
                this.engagementFeatures = engagementFeatures;
                return this;
            }
            
            public UserFeatureVectorBuilder contextualFeatures(Map<String, Double> contextualFeatures) {
                this.contextualFeatures = contextualFeatures;
                return this;
            }
            
            public UserFeatureVector build() {
                return new UserFeatureVector(userId, genrePreferences, audioFeaturePreferences,
                                           temporalFeatures, engagementFeatures, contextualFeatures);
            }
        }
    }
    
    public static class MLRecommendationItem {
        private final String itemId;
        private final double score;
        private final double confidence;
        private final String algorithm;
        private final Map<String, Object> features;
        
        private MLRecommendationItem(String itemId, double score, double confidence, 
                                   String algorithm, Map<String, Object> features) {
            this.itemId = itemId;
            this.score = score;
            this.confidence = confidence;
            this.algorithm = algorithm;
            this.features = features != null ? features : new HashMap<>();
        }
        
        public static MLRecommendationItemBuilder builder() {
            return new MLRecommendationItemBuilder();
        }
        
        public MLRecommendationItem withScore(double newScore) {
            return new MLRecommendationItem(this.itemId, newScore, this.confidence, 
                                          this.algorithm, this.features);
        }
        
        public MLRecommendationItem withAlgorithm(String newAlgorithm) {
            return new MLRecommendationItem(this.itemId, this.score, this.confidence, 
                                          newAlgorithm, this.features);
        }
        
        // Getters
        public String getItemId() { return itemId; }
        public double getScore() { return score; }
        public double getConfidence() { return confidence; }
        public String getAlgorithm() { return algorithm; }
        public Map<String, Object> getFeatures() { return features; }
        
        public static class MLRecommendationItemBuilder {
            private String itemId;
            private double score;
            private double confidence;
            private String algorithm;
            private Map<String, Object> features;
            
            public MLRecommendationItemBuilder itemId(String itemId) {
                this.itemId = itemId;
                return this;
            }
            
            public MLRecommendationItemBuilder score(double score) {
                this.score = score;
                return this;
            }
            
            public MLRecommendationItemBuilder confidence(double confidence) {
                this.confidence = confidence;
                return this;
            }
            
            public MLRecommendationItemBuilder algorithm(String algorithm) {
                this.algorithm = algorithm;
                return this;
            }
            
            public MLRecommendationItemBuilder features(Map<String, Object> features) {
                this.features = features;
                return this;
            }
            
            public MLRecommendationItem build() {
                return new MLRecommendationItem(itemId, score, confidence, algorithm, features);
            }
        }
    }
}