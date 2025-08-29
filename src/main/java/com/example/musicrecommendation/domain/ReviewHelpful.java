package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_helpful")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewHelpful {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "review_id", nullable = false)
    private Long reviewId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}