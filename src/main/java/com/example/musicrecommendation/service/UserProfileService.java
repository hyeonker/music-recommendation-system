package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserProfile;
import com.example.musicrecommendation.repository.UserProfileRepository;
import com.example.musicrecommendation.web.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repo;

    public ProfileDto getOrInit(Long userId) {
        UserProfile up = repo.findByUserId(userId).orElseGet(() ->
                UserProfile.builder()
                        .userId(userId)
                        .favoriteArtists(new ArrayList<>())
                        .favoriteGenres(new ArrayList<>())
                        .attendedFestivals(new ArrayList<>())
                        .musicPreferences(new HashMap<>())
                        .build()
        );
        return toDto(up);
    }

    public ProfileDto save(Long userId, ProfileDto dto) {
        UserProfile up = repo.findByUserId(userId).orElseGet(() ->
                UserProfile.builder()
                        .userId(userId)
                        .favoriteArtists(new ArrayList<>())
                        .favoriteGenres(new ArrayList<>())
                        .attendedFestivals(new ArrayList<>())
                        .musicPreferences(new HashMap<>())
                        .build()
        );

        // ✅ 그대로 대입 (List/Map)
        up.setFavoriteArtists(
                dto.getFavoriteArtists() != null ? dto.getFavoriteArtists() : new ArrayList<>()
        );
        up.setFavoriteGenres(
                dto.getFavoriteGenres() != null ? dto.getFavoriteGenres() : new ArrayList<>()
        );
        up.setAttendedFestivals(
                dto.getAttendedFestivals() != null ? dto.getAttendedFestivals() : new ArrayList<>()
        );
        up.setMusicPreferences(
                dto.getMusicPreferences() != null ? dto.getMusicPreferences() : new HashMap<>()
        );

        up = repo.save(up);
        return toDto(up);
    }

    private ProfileDto toDto(UserProfile up) {
        return ProfileDto.builder()
                .userId(up.getUserId())
                .favoriteArtists(
                        up.getFavoriteArtists() != null ? up.getFavoriteArtists() : new ArrayList<>()
                )
                .favoriteGenres(
                        up.getFavoriteGenres() != null ? up.getFavoriteGenres() : new ArrayList<>()
                )
                .attendedFestivals(
                        up.getAttendedFestivals() != null ? up.getAttendedFestivals() : new ArrayList<>()
                )
                .musicPreferences(
                        up.getMusicPreferences() != null ? up.getMusicPreferences() : new HashMap<>()
                )
                .build();
    }
}
