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

    @Column(length = 500)
    private String imageUrl;

    // 🔹 낙관적 락
    @Version
    @Column(nullable = false)
    private Long version;

    public Song(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    public Song(String title, String artist, String imageUrl) {
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
    }

    /** 수정용 도메인 메서드 */
    public void change(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    public void changeWithImage(String title, String artist, String imageUrl) {
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getVersion() {
        return version;
    }
}
