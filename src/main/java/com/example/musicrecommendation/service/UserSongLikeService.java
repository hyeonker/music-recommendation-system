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
     * 외부 음악 서비스 곡에 대한 좋아요 토글
     * Song이 존재하지 않으면 자동으로 생성
     * @return ToggleResult with boolean liked and Long actualSongId
     */
    @Transactional
    public ToggleResult toggleExternalSong(Long userId, Long songId, String title, String artist, String imageUrl) {
        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // Song 찾기 또는 생성 (제목+아티스트 기반)
        Song song = songRepository.findByTitleAndArtist(title, artist)
                .orElseGet(() -> {
                    // 존재하지 않으면 새로 생성
                    Song newSong = new Song(title, artist, imageUrl);
                    return songRepository.save(newSong);
                });

        // 기존 좋아요 확인 (실제 Song ID 사용)
        Optional<UserSongLike> existingLike = userSongLikeRepository.findByUserIdAndSongId(userId, song.getId());

        if (existingLike.isPresent()) {
            // 이미 좋아요 → 취소
            userSongLikeRepository.delete(existingLike.get());
            return new ToggleResult(false, song.getId());
        } else {
            // 좋아요 추가
            UserSongLike newLike = new UserSongLike(user, song);
            userSongLikeRepository.save(newLike);
            return new ToggleResult(true, song.getId());
        }
    }
    
    /**
     * 외부 음악 좋아요 토글 결과
     */
    public static class ToggleResult {
        private final boolean liked;
        private final Long actualSongId;
        
        public ToggleResult(boolean liked, Long actualSongId) {
            this.liked = liked;
            this.actualSongId = actualSongId;
        }
        
        public boolean isLiked() {
            return liked;
        }
        
        public Long getActualSongId() {
            return actualSongId;
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

        return userSongLikeRepository.findByUserIdWithSongAndUserOrderByLikedAtDesc(userId, pageable);
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

    /**
     * 외부 서비스(Spotify 등)의 곡에 좋아요 추가
     * 곡이 존재하지 않으면 새로 생성
     *
     * @param userId 사용자 ID
     * @param externalId 외부 서비스 ID (예: Spotify ID)
     * @param title 곡 제목
     * @param artist 아티스트명
     * @param imageUrl 앨범 표지 URL
     * @return true: 좋아요 추가됨, false: 이미 좋아요 상태
     */
    @Transactional
    public boolean likeExternalSong(Long userId, String externalId, String title, String artist, String imageUrl) {
        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 외부 ID를 기반으로 Song 찾거나 생성
        Song song = findOrCreateSongByExternalId(externalId, title, artist, imageUrl);

        // 이미 좋아요 상태인지 확인
        Optional<UserSongLike> existingLike = userSongLikeRepository.findByUserIdAndSongId(userId, song.getId());
        
        if (existingLike.isPresent()) {
            return false; // 이미 좋아요 상태
        }

        // 좋아요 추가
        UserSongLike newLike = new UserSongLike(user, song);
        userSongLikeRepository.save(newLike);
        return true;
    }

    /**
     * 외부 서비스의 곡 좋아요 취소
     *
     * @param userId 사용자 ID
     * @param externalId 외부 서비스 ID
     * @return true: 좋아요 취소됨, false: 좋아요 상태가 아니었음
     */
    @Transactional
    public boolean unlikeExternalSong(Long userId, String externalId) {
        // 외부 ID로 Song 찾기
        Song song = findSongByExternalId(externalId);
        if (song == null) {
            return false; // 곡이 존재하지 않음
        }

        // 좋아요 찾기
        Optional<UserSongLike> existingLike = userSongLikeRepository.findByUserIdAndSongId(userId, song.getId());
        
        if (existingLike.isPresent()) {
            userSongLikeRepository.delete(existingLike.get());
            return true; // 좋아요 취소됨
        }

        return false; // 좋아요 상태가 아니었음
    }

    /**
     * 외부 ID로 Song 찾거나 생성
     */
    private Song findOrCreateSongByExternalId(String externalId, String title, String artist, String imageUrl) {
        // 외부 ID는 Song 테이블에 직접 저장되지 않으므로, 
        // 제목과 아티스트를 조합한 고유성으로 판단
        // 더 정확한 방법은 Song 테이블에 externalId 컬럼을 추가하는 것
        
        // 임시로 제목과 아티스트가 동일한 곡이 있는지 확인
        Optional<Song> existingSong = songRepository.findByTitleAndArtist(title, artist);
        
        if (existingSong.isPresent()) {
            Song song = existingSong.get();
            // 기존 곡이 있으면 imageUrl을 업데이트 (없는 경우에만)
            if (song.getImageUrl() == null && imageUrl != null) {
                song.changeWithImage(song.getTitle(), song.getArtist(), imageUrl);
                return songRepository.save(song);
            }
            return song;
        }

        // 새로운 곡 생성
        Song newSong = new Song(title, artist, imageUrl);
        return songRepository.save(newSong);
    }

    /**
     * 외부 ID로 Song 찾기
     */
    private Song findSongByExternalId(String externalId) {
        // 실제로는 externalId로 찾아야 하지만, 현재 구조에서는 불가능
        // 임시로 해시코드를 이용한 매핑을 사용
        // 이는 완벽하지 않은 방법이므로 나중에 개선 필요
        
        Long songId = (long) externalId.hashCode();
        return songRepository.findById(songId).orElse(null);
    }
}