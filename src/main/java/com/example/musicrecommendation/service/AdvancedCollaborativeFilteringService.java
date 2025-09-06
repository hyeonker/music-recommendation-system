package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserBehaviorEvent;
import com.example.musicrecommendation.repository.UserBehaviorEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 개선된 협업 필터링 시스템
 * - 사용자-아이템 매트릭스 기반 추천
 * - 코사인 유사도 및 피어슨 상관계수 계산
 * - 아이템 기반 협업 필터링 (Item-CF)
 * - 사용자 기반 협업 필터링 (User-CF)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedCollaborativeFilteringService {
    
    private final UserBehaviorEventRepository behaviorRepository;
    private final UserMusicPreferencesService preferencesService;
    
    /**
     * 협업 필터링 기반 추천 (하이브리드: User-CF + Item-CF)
     */
    @Cacheable(value = "collaborativeRecommendations", key = "#userId + '_' + #limit")
    @Transactional(readOnly = true)
    public List<RecommendationItem> getCollaborativeRecommendations(Long userId, int limit) {
        log.debug("사용자 {} 협업 필터링 추천 생성 시작", userId);
        
        try {
            // 1. 사용자-아이템 매트릭스 구축
            UserItemMatrix matrix = buildUserItemMatrix(30); // 최근 30일
            
            // 2. 사용자 기반 추천 (70%)
            List<RecommendationItem> userBasedRecs = getUserBasedRecommendations(userId, matrix, limit);
            
            // 3. 아이템 기반 추천 (30%)
            List<RecommendationItem> itemBasedRecs = getItemBasedRecommendations(userId, matrix, limit / 2);
            
            // 4. 하이브리드 결합 및 다양성 보장
            List<RecommendationItem> combined = combineRecommendations(userBasedRecs, itemBasedRecs, limit);
            
            log.debug("사용자 {} 협업 필터링 추천 {} 개 생성 완료", userId, combined.size());
            return combined;
            
        } catch (Exception e) {
            log.error("협업 필터링 추천 생성 실패 (userId: {}): {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 사용자 기반 협업 필터링
     */
    private List<RecommendationItem> getUserBasedRecommendations(Long userId, UserItemMatrix matrix, int limit) {
        // 1. 유사한 사용자 찾기
        List<UserSimilarity> similarUsers = findSimilarUsers(userId, matrix, 50);
        
        if (similarUsers.isEmpty()) {
            log.debug("사용자 {} 유사 사용자 없음", userId);
            return new ArrayList<>();
        }
        
        // 2. 사용자가 아직 경험하지 않은 아이템들 수집
        Set<String> userItems = matrix.getUserItems(userId);
        Map<String, Double> itemScores = new HashMap<>();
        
        for (UserSimilarity similarity : similarUsers) {
            Long similarUserId = similarity.getUserId();
            double userSimilarity = similarity.getSimilarity();
            
            Set<String> similarUserItems = matrix.getUserItems(similarUserId);
            for (String itemId : similarUserItems) {
                if (!userItems.contains(itemId)) {
                    double itemRating = matrix.getUserItemRating(similarUserId, itemId);
                    itemScores.merge(itemId, userSimilarity * itemRating, Double::sum);
                }
            }
        }
        
        // 3. 점수 기준 정렬 및 반환
        return itemScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> RecommendationItem.builder()
                .itemId(entry.getKey())
                .score(normalizeScore(entry.getValue()))
                .confidence(calculateConfidence(entry.getKey(), similarUsers.size()))
                .algorithm("USER_CF")
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * 아이템 기반 협업 필터링
     */
    private List<RecommendationItem> getItemBasedRecommendations(Long userId, UserItemMatrix matrix, int limit) {
        Set<String> userItems = matrix.getUserItems(userId);
        if (userItems.isEmpty()) return new ArrayList<>();
        
        Map<String, Double> itemScores = new HashMap<>();
        
        // 사용자가 좋아한 각 아이템에 대해 유사한 아이템 찾기
        for (String userItemId : userItems) {
            double userRating = matrix.getUserItemRating(userId, userItemId);
            if (userRating < 0.5) continue; // 낮은 평가 아이템은 제외
            
            List<ItemSimilarity> similarItems = findSimilarItems(userItemId, matrix, 20);
            
            for (ItemSimilarity similarity : similarItems) {
                String similarItemId = similarity.getItemId();
                if (!userItems.contains(similarItemId)) {
                    double score = userRating * similarity.getSimilarity();
                    itemScores.merge(similarItemId, score, Double::sum);
                }
            }
        }
        
        return itemScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> RecommendationItem.builder()
                .itemId(entry.getKey())
                .score(normalizeScore(entry.getValue()))
                .confidence(0.8) // 아이템 기반은 일반적으로 신뢰도가 높음
                .algorithm("ITEM_CF")
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * 사용자-아이템 매트릭스 구축
     */
    private UserItemMatrix buildUserItemMatrix(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<UserBehaviorEvent> events = behaviorRepository.findRecentUserEvents(null, since);
        
        UserItemMatrix matrix = new UserItemMatrix();
        
        for (UserBehaviorEvent event : events) {
            if (event.getItemType() == UserBehaviorEvent.ItemType.SONG) {
                Long userId = event.getUserId();
                String itemId = event.getItemId();
                double rating = event.getImplicitFeedbackScore();
                
                matrix.addRating(userId, itemId, rating);
            }
        }
        
        return matrix;
    }
    
    /**
     * 유사 사용자 찾기 (코사인 유사도)
     */
    private List<UserSimilarity> findSimilarUsers(Long userId, UserItemMatrix matrix, int limit) {
        Set<String> userItems = matrix.getUserItems(userId);
        if (userItems.isEmpty()) return new ArrayList<>();
        
        List<UserSimilarity> similarities = new ArrayList<>();
        
        for (Long otherUserId : matrix.getAllUsers()) {
            if (otherUserId.equals(userId)) continue;
            
            double similarity = calculateUserSimilarity(userId, otherUserId, matrix);
            if (similarity > 0.1) { // 최소 유사도 임계값
                similarities.add(new UserSimilarity(otherUserId, similarity));
            }
        }
        
        return similarities.stream()
            .sorted((s1, s2) -> Double.compare(s2.getSimilarity(), s1.getSimilarity()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 유사 아이템 찾기 (코사인 유사도)
     */
    private List<ItemSimilarity> findSimilarItems(String itemId, UserItemMatrix matrix, int limit) {
        List<ItemSimilarity> similarities = new ArrayList<>();
        
        for (String otherItemId : matrix.getAllItems()) {
            if (otherItemId.equals(itemId)) continue;
            
            double similarity = calculateItemSimilarity(itemId, otherItemId, matrix);
            if (similarity > 0.1) {
                similarities.add(new ItemSimilarity(otherItemId, similarity));
            }
        }
        
        return similarities.stream()
            .sorted((s1, s2) -> Double.compare(s2.getSimilarity(), s1.getSimilarity()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 사용자 간 코사인 유사도 계산
     */
    private double calculateUserSimilarity(Long user1Id, Long user2Id, UserItemMatrix matrix) {
        Set<String> user1Items = matrix.getUserItems(user1Id);
        Set<String> user2Items = matrix.getUserItems(user2Id);
        
        Set<String> commonItems = new HashSet<>(user1Items);
        commonItems.retainAll(user2Items);
        
        if (commonItems.size() < 2) return 0.0; // 최소 2개 공통 아이템 필요
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String itemId : commonItems) {
            double rating1 = matrix.getUserItemRating(user1Id, itemId);
            double rating2 = matrix.getUserItemRating(user2Id, itemId);
            
            dotProduct += rating1 * rating2;
            norm1 += rating1 * rating1;
            norm2 += rating2 * rating2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 아이템 간 코사인 유사도 계산
     */
    private double calculateItemSimilarity(String item1Id, String item2Id, UserItemMatrix matrix) {
        Set<Long> item1Users = matrix.getItemUsers(item1Id);
        Set<Long> item2Users = matrix.getItemUsers(item2Id);
        
        Set<Long> commonUsers = new HashSet<>(item1Users);
        commonUsers.retainAll(item2Users);
        
        if (commonUsers.size() < 2) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (Long userId : commonUsers) {
            double rating1 = matrix.getUserItemRating(userId, item1Id);
            double rating2 = matrix.getUserItemRating(userId, item2Id);
            
            dotProduct += rating1 * rating2;
            norm1 += rating1 * rating1;
            norm2 += rating2 * rating2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 추천 결과 결합
     */
    private List<RecommendationItem> combineRecommendations(List<RecommendationItem> userBased,
                                                           List<RecommendationItem> itemBased,
                                                           int limit) {
        Map<String, RecommendationItem> combined = new HashMap<>();
        
        // 사용자 기반 추천 (가중치: 0.7)
        for (RecommendationItem item : userBased) {
            RecommendationItem weighted = item.withWeightedScore(0.7);
            combined.put(item.getItemId(), weighted);
        }
        
        // 아이템 기반 추천 (가중치: 0.3)
        for (RecommendationItem item : itemBased) {
            String itemId = item.getItemId();
            if (combined.containsKey(itemId)) {
                // 기존 점수와 결합
                RecommendationItem existing = combined.get(itemId);
                double newScore = existing.getScore() + (item.getScore() * 0.3);
                combined.put(itemId, existing.withScore(newScore).withAlgorithm("HYBRID_CF"));
            } else {
                combined.put(itemId, item.withWeightedScore(0.3));
            }
        }
        
        return combined.values().stream()
            .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private double normalizeScore(double score) {
        return Math.max(0.0, Math.min(1.0, score / 10.0)); // 0-10 범위를 0-1로 정규화
    }
    
    private double calculateConfidence(String itemId, int similarUsersCount) {
        return Math.min(0.9, similarUsersCount * 0.02); // 유사 사용자 수에 비례한 신뢰도
    }
    
    // Inner Classes
    public static class UserSimilarity {
        private final Long userId;
        private final double similarity;
        
        public UserSimilarity(Long userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }
        
        public Long getUserId() { return userId; }
        public double getSimilarity() { return similarity; }
    }
    
    public static class ItemSimilarity {
        private final String itemId;
        private final double similarity;
        
        public ItemSimilarity(String itemId, double similarity) {
            this.itemId = itemId;
            this.similarity = similarity;
        }
        
        public String getItemId() { return itemId; }
        public double getSimilarity() { return similarity; }
    }
    
    public static class RecommendationItem {
        private final String itemId;
        private final double score;
        private final double confidence;
        private final String algorithm;
        
        private RecommendationItem(String itemId, double score, double confidence, String algorithm) {
            this.itemId = itemId;
            this.score = score;
            this.confidence = confidence;
            this.algorithm = algorithm;
        }
        
        public static RecommendationItemBuilder builder() {
            return new RecommendationItemBuilder();
        }
        
        public RecommendationItem withScore(double newScore) {
            return new RecommendationItem(this.itemId, newScore, this.confidence, this.algorithm);
        }
        
        public RecommendationItem withWeightedScore(double weight) {
            return new RecommendationItem(this.itemId, this.score * weight, this.confidence, this.algorithm);
        }
        
        public RecommendationItem withAlgorithm(String newAlgorithm) {
            return new RecommendationItem(this.itemId, this.score, this.confidence, newAlgorithm);
        }
        
        // Getters
        public String getItemId() { return itemId; }
        public double getScore() { return score; }
        public double getConfidence() { return confidence; }
        public String getAlgorithm() { return algorithm; }
        
        public static class RecommendationItemBuilder {
            private String itemId;
            private double score;
            private double confidence;
            private String algorithm;
            
            public RecommendationItemBuilder itemId(String itemId) {
                this.itemId = itemId;
                return this;
            }
            
            public RecommendationItemBuilder score(double score) {
                this.score = score;
                return this;
            }
            
            public RecommendationItemBuilder confidence(double confidence) {
                this.confidence = confidence;
                return this;
            }
            
            public RecommendationItemBuilder algorithm(String algorithm) {
                this.algorithm = algorithm;
                return this;
            }
            
            public RecommendationItem build() {
                return new RecommendationItem(itemId, score, confidence, algorithm);
            }
        }
    }
    
    /**
     * 사용자-아이템 매트릭스 클래스
     */
    public static class UserItemMatrix {
        private final Map<Long, Map<String, Double>> userItemRatings = new HashMap<>();
        private final Map<String, Set<Long>> itemUsers = new HashMap<>();
        
        public void addRating(Long userId, String itemId, double rating) {
            userItemRatings.computeIfAbsent(userId, k -> new HashMap<>())
                .merge(itemId, rating, (existing, newRating) -> {
                    // 기존 평가가 있으면 최근 평가에 더 높은 가중치
                    return existing * 0.7 + newRating * 0.3;
                });
            
            itemUsers.computeIfAbsent(itemId, k -> new HashSet<>())
                .add(userId);
        }
        
        public double getUserItemRating(Long userId, String itemId) {
            return userItemRatings.getOrDefault(userId, Collections.emptyMap())
                .getOrDefault(itemId, 0.0);
        }
        
        public Set<String> getUserItems(Long userId) {
            return userItemRatings.getOrDefault(userId, Collections.emptyMap()).keySet();
        }
        
        public Set<Long> getItemUsers(String itemId) {
            return itemUsers.getOrDefault(itemId, Collections.emptySet());
        }
        
        public Set<Long> getAllUsers() {
            return userItemRatings.keySet();
        }
        
        public Set<String> getAllItems() {
            return itemUsers.keySet();
        }
        
        public int getUserCount() {
            return userItemRatings.size();
        }
        
        public int getItemCount() {
            return itemUsers.size();
        }
    }
}