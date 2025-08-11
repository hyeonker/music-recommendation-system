package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "songs")
@Getter @Setter @NoArgsConstructor
public class Song {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String title;
    @Column(nullable = false) private String artist;

    public Song(String title, String artist) {
        this.title = title; this.artist = artist;
    }
}
