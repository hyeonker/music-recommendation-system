package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.MusicSharingHistory;
import com.example.musicrecommendation.repository.MusicSharingHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MusicSharingHistoryService {
    
    private final MusicSharingHistoryRepository musicSharingHistoryRepository;
    
    public MusicSharingHistoryService(MusicSharingHistoryRepository musicSharingHistoryRepository) {
        this.musicSharingHistoryRepository = musicSharingHistoryRepository;
    }
    
    /**
     * 음악 공유 기록 저장
     */
    public MusicSharingHistory saveMusicShare(Long receiverUserId, Long sharedByUserId, String sharedByName,
                                            String trackName, String artistName, String spotifyUrl,
                                            String chatRoomId, String matchingSessionId) {
        
        // 중복 공유 확인 (선택적)
        Optional<MusicSharingHistory> existing = musicSharingHistoryRepository
                .findByUserIdAndSharedByUserIdAndTrackNameAndArtistName(
                        receiverUserId, sharedByUserId, trackName, artistName);
        
        if (existing.isPresent()) {
            // 이미 공유된 곡이면 기존 기록의 공유 시간만 업데이트
            MusicSharingHistory existingRecord = existing.get();
            existingRecord.setSharedAt(LocalDateTime.now());
            existingRecord.setChatRoomId(chatRoomId);
            existingRecord.setMatchingSessionId(matchingSessionId);
            return musicSharingHistoryRepository.save(existingRecord);
        }
        
        // 새로운 공유 기록 생성
        MusicSharingHistory musicShare = new MusicSharingHistory(
                receiverUserId, sharedByUserId, sharedByName, trackName, artistName, spotifyUrl);
        musicShare.setChatRoomId(chatRoomId);
        musicShare.setMatchingSessionId(matchingSessionId);
        
        return musicSharingHistoryRepository.save(musicShare);
    }
    
    /**
     * 상세한 음악 정보와 함께 공유 기록 저장
     */
    public MusicSharingHistory saveMusicShareWithDetails(Long receiverUserId, Long sharedByUserId, String sharedByName,
                                                       String trackName, String artistName, String spotifyUrl,
                                                       String albumName, String albumImageUrl, String previewUrl,
                                                       Integer durationMs, Integer popularity, String[] genres,
                                                       String chatRoomId, String matchingSessionId) {
        
        MusicSharingHistory musicShare = new MusicSharingHistory(
                receiverUserId, sharedByUserId, sharedByName, trackName, artistName, spotifyUrl);
        
        musicShare.setAlbumName(albumName);
        musicShare.setAlbumImageUrl(albumImageUrl);
        musicShare.setPreviewUrl(previewUrl);
        musicShare.setDurationMs(durationMs);
        musicShare.setPopularity(popularity);
        musicShare.setGenres(genres);
        musicShare.setChatRoomId(chatRoomId);
        musicShare.setMatchingSessionId(matchingSessionId);
        
        return musicSharingHistoryRepository.save(musicShare);
    }
    
    /**
     * 사용자가 받은 음악 공유 히스토리 조회
     */
    @Transactional(readOnly = true)
    public Page<MusicSharingHistory> getUserMusicHistory(Long userId, Pageable pageable) {
        return musicSharingHistoryRepository.findByUserIdOrderBySharedAtDesc(userId, pageable);
    }
    
    /**
     * 사용자가 좋아요한 음악들만 조회
     */
    @Transactional(readOnly = true)
    public Page<MusicSharingHistory> getUserLikedMusic(Long userId, Pageable pageable) {
        return musicSharingHistoryRepository.findByUserIdAndIsLikedTrueOrderByLikedAtDesc(userId, pageable);
    }
    
    /**
     * 음악 검색
     */
    @Transactional(readOnly = true)
    public Page<MusicSharingHistory> searchMusicHistory(Long userId, String keyword, Pageable pageable) {
        return musicSharingHistoryRepository.searchMusicHistory(userId, keyword, pageable);
    }
    
    /**
     * 두 사용자 간의 음악 공유 히스토리
     */
    @Transactional(readOnly = true)
    public Page<MusicSharingHistory> getMusicSharingBetweenUsers(Long userId1, Long userId2, Pageable pageable) {
        return musicSharingHistoryRepository.findMusicSharingBetweenUsers(userId1, userId2, pageable);
    }
    
    /**
     * 음악 좋아요/좋아요 취소
     */
    public MusicSharingHistory toggleLikeMusic(Long historyId, Long userId) {
        Optional<MusicSharingHistory> optionalHistory = musicSharingHistoryRepository.findById(historyId);
        
        if (optionalHistory.isEmpty()) {
            throw new IllegalArgumentException("음악 공유 기록을 찾을 수 없습니다.");
        }
        
        MusicSharingHistory history = optionalHistory.get();
        
        // 본인의 기록인지 확인
        if (!history.getUserId().equals(userId)) {
            throw new IllegalArgumentException("다른 사용자의 음악 기록은 수정할 수 없습니다.");
        }
        
        // 좋아요 토글
        history.setIsLiked(!history.getIsLiked());
        
        return musicSharingHistoryRepository.save(history);
    }
    
    /**
     * 메모 추가/수정
     */
    public MusicSharingHistory addNoteToMusic(Long historyId, Long userId, String notes) {
        Optional<MusicSharingHistory> optionalHistory = musicSharingHistoryRepository.findById(historyId);
        
        if (optionalHistory.isEmpty()) {
            throw new IllegalArgumentException("음악 공유 기록을 찾을 수 없습니다.");
        }
        
        MusicSharingHistory history = optionalHistory.get();
        
        // 본인의 기록인지 확인
        if (!history.getUserId().equals(userId)) {
            throw new IllegalArgumentException("다른 사용자의 음악 기록은 수정할 수 없습니다.");
        }
        
        history.setNotes(notes);
        
        return musicSharingHistoryRepository.save(history);
    }
    
    /**
     * 사용자의 음악 공유 통계 조회
     */
    @Transactional(readOnly = true)
    public Object getMusicSharingStats(Long userId) {
        Long receivedCount = musicSharingHistoryRepository.countReceivedMusic(userId);
        Long sharedCount = musicSharingHistoryRepository.countSharedMusic(userId);
        Long likedCount = musicSharingHistoryRepository.countLikedMusic(userId);
        
        // 가장 많이 공유받은 곡들 (top 5)
        List<Object[]> topTracks = musicSharingHistoryRepository.findMostSharedTracks(
                userId, PageRequest.of(0, 5));
        
        // 가장 많이 음악을 공유해준 사용자들 (top 5)
        List<Object[]> topSharers = musicSharingHistoryRepository.findTopSharers(
                userId, PageRequest.of(0, 5));
        
        return new Object() {
            public final Long receivedMusicCount = receivedCount;
            public final Long sharedMusicCount = sharedCount;
            public final Long likedMusicCount = likedCount;
            public final List<Object[]> mostSharedTracks = topTracks;
            public final List<Object[]> topMusicSharers = topSharers;
        };
    }
    
    /**
     * 특정 기간의 음악 공유 히스토리
     */
    @Transactional(readOnly = true)
    public Page<MusicSharingHistory> getMusicHistoryByPeriod(Long userId, LocalDateTime startDate, 
                                                           LocalDateTime endDate, Pageable pageable) {
        return musicSharingHistoryRepository.findByUserIdAndSharedAtBetweenOrderBySharedAtDesc(
                userId, startDate, endDate, pageable);
    }
}