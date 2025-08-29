package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "user_profile")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // ✅ jsonb 컬럼 매핑 (Hibernate 6)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_artists", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> favoriteArtists;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_genres", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> favoriteGenres;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attended_festivals", columnDefinition = "jsonb", nullable = false)
    private List<String> attendedFestivals;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "music_preferences", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> musicPreferences;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
