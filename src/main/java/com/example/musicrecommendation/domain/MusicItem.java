package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 음악 아이템 (곡, 앨범, 아티스트, 플레이리스트)
 */
@Entity
@Table(name = "music_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = {"externalId", "itemType"})
public class MusicItem {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId; // Spotify ID 등
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private MusicItemType itemType;
    
    @Column(name = "name", nullable = false, length = 500)
    private String name;
    
    @Column(name = "artist_name", length = 500)
    private String artistName;
    
    @Column(name = "album_name", length = 500)
    private String albumName;
    
    @Column(name = "genre", length = 100)
    private String genre;
    
    @Column(name = "release_date")
    private LocalDate releaseDate;
    
    @Column(name = "duration_ms")
    private Integer durationMs;
    
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;
    
    @Column(name = "external_url", columnDefinition = "text")
    private String externalUrl;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
    
    public enum MusicItemType {
        TRACK("곡"),
        ALBUM("앨범"), 
        ARTIST("아티스트"),
        PLAYLIST("플레이리스트");
        
        private final String displayName;
        
        MusicItemType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}