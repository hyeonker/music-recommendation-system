package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "songs")
@Getter
@NoArgsConstructor
public class Song {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String artist;

    // 🔹 낙관적 락
    @Version
    @Column(nullable = false)
    private Long version;

    public Song(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    /** 수정용 도메인 메서드 */
    public void change(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }
    // === Getters ===
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public Long getVersion() {
        return version;
    }
}
