package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 음악 좋아요 관련 비즈니스 로직을 담당하는 서비스
 */
@Service
@Transactional(readOnly = true)
public class UserSongLikeService {

    private final UserSongLikeRepository userSongLikeRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    public UserSongLikeService(UserSongLikeRepository userSongLikeRepository,
                               UserRepository userRepository,
                               SongRepository songRepository) {
        this.userSongLikeRepository = userSongLikeRepository;
        this.userRepository = userRepository;
        this.songRepository = songRepository;
    }

    /**
     * 곡에 좋아요 누르기
     * 이미 좋아요를 눌렀다면 토글 (좋아요 취소)
     *
     * @param userId 사용자 ID
     * @param songId 곡 ID
     * @return true: 좋아요 추가, false: 좋아요 취소
     */
    @Transactional
    public boolean toggleLike(Long userId, Long songId) {
        // 사용자와 곡 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("곡을 찾을 수 없습니다: " + songId));

        // 기존 좋아요 확인
        Optional<UserSongLike> existingLike = userSongLikeRepository.findByUserIdAndSongId(userId, songId);

        if (existingLike.isPresent()) {
            // 이미 좋아요 → 취소
            userSongLikeRepository.delete(existingLike.get());
            return false;
        } else {
            // 좋아요 추가
            UserSongLike newLike = new UserSongLike(user, song);
            userSongLikeRepository.save(newLike);
            return true;
        }
    }

    /**
     * 사용자가 좋아요한 곡 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 좋아요한 곡 목록
     */
    public Page<UserSongLike> getUserLikedSongs(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
        }

        return userSongLikeRepository.findByUserIdOrderByLikedAtDesc(userId, pageable);
    }

    /**
     * 특정 곡을 좋아요한 사용자 목록 조회
     *
     * @param songId 곡 ID
     * @return 좋아요한 사용자 목록
     */
    public List<UserSongLike> getSongLikedUsers(Long songId) {
        if (!songRepository.existsById(songId)) {
            throw new IllegalArgumentException("곡을 찾을 수 없습니다: " + songId);
        }

        return userSongLikeRepository.findBySongIdOrderByLikedAtDesc(songId);
    }

    /**
     * 특정 곡의 좋아요 수 조회
     *
     * @param songId 곡 ID
     * @return 좋아요 수
     */
    public long getSongLikeCount(Long songId) {
        return userSongLikeRepository.countBySongId(songId);
    }

    /**
     * 특정 사용자가 좋아요한 곡 수 조회
     *
     * @param userId 사용자 ID
     * @return 좋아요한 곡 수
     */
    public long getUserLikeCount(Long userId) {
        return userSongLikeRepository.countByUserId(userId);
    }

    /**
     * 사용자가 특정 곡에 좋아요를 눌렀는지 확인
     *
     * @param userId 사용자 ID
     * @param songId 곡 ID
     * @return 좋아요를 눌렀으면 true
     */
    public boolean isLikedByUser(Long userId, Long songId) {
        return userSongLikeRepository.existsByUserIdAndSongId(userId, songId);
    }

    /**
     * 가장 많이 좋아요 받은 곡들 TOP N 조회
     *
     * @param limit 조회할 개수
     * @return 곡 ID와 좋아요 수 목록
     */
    public List<UserSongLikeRepository.SongLikeCount> getTopLikedSongs(int limit) {
        return userSongLikeRepository.findTopLikedSongs(limit);
    }

    /**
     * 특정 사용자가 좋아요한 곡 ID 목록 조회
     * 추천 알고리즘에서 사용
     *
     * @param userId 사용자 ID
     * @return 좋아요한 곡 ID 목록
     */
    public List<Long> getUserLikedSongIds(Long userId) {
        return userSongLikeRepository.findLikedSongIdsByUserId(userId);
    }

    /**
     * 전체 좋아요 수 통계
     *
     * @return 총 좋아요 수
     */
    public long getTotalLikeCount() {
        return userSongLikeRepository.count();
    }
}