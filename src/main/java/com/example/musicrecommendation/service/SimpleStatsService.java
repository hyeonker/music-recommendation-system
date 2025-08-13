package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import com.example.musicrecommendation.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 간단한 통계 서비스 - 시간별 차트 포함
 */
@Service
@Transactional(readOnly = true)
public class SimpleStatsService {

    private final UserSongLikeRepository userSongLikeRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    public SimpleStatsService(UserSongLikeRepository userSongLikeRepository,
                              SongRepository songRepository,
                              UserRepository userRepository) {
        this.userSongLikeRepository = userSongLikeRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
    }

    /**
     * 전체 통계 대시보드 조회
     */
    public SimpleStatsResponse getStatsDashboard() {
        List<SimpleStatsResponse.TopSongDto> topSongs = getTopSongs(10);
        List<SimpleStatsResponse.TopUserDto> activeUsers = getActiveUsers(5);
        List<SimpleStatsResponse.TopArtistDto> topArtists = getTopArtists(5);
        SimpleStatsResponse.OverallStatsDto overallStats = getOverallStats();

        return new SimpleStatsResponse(topSongs, activeUsers, topArtists, overallStats);
    }

    /**
     * TOP 인기 곡들 조회
     */
    public List<SimpleStatsResponse.TopSongDto> getTopSongs(int limit) {
        List<UserSongLikeRepository.SongLikeCount> topSongCounts =
                userSongLikeRepository.findTopLikedSongs(limit);

        List<SimpleStatsResponse.TopSongDto> result = new ArrayList<>();
        int rank = 1;

        for (UserSongLikeRepository.SongLikeCount songCount : topSongCounts) {
            Optional<Song> songOpt = songRepository.findById(songCount.getSongId());
            if (songOpt.isPresent()) {
                Song song = songOpt.get();
                SongResponse songResponse = SongResponse.from(song);
                String trendIcon = getTrendIcon(rank, songCount.getLikeCount());

                SimpleStatsResponse.TopSongDto topSong = new SimpleStatsResponse.TopSongDto(
                        rank, songResponse, songCount.getLikeCount(), trendIcon
                );
                result.add(topSong);
                rank++;
            }
        }

        return result;
    }

    /**
     * 가장 활발한 사용자들 조회
     */
    public List<SimpleStatsResponse.TopUserDto> getActiveUsers(int limit) {
        List<User> allUsers = userRepository.findAll();

        List<UserLikeInfo> userLikes = allUsers.stream()
                .map(user -> {
                    long likeCount = userSongLikeRepository.countByUserId(user.getId());
                    return new UserLikeInfo(user, likeCount);
                })
                .sorted((a, b) -> Long.compare(b.likeCount, a.likeCount))
                .limit(limit)
                .collect(Collectors.toList());

        List<SimpleStatsResponse.TopUserDto> result = new ArrayList<>();
        int rank = 1;

        for (UserLikeInfo userLike : userLikes) {
            UserResponse userResponse = UserResponse.from(userLike.user);
            double activityScore = calculateActivityScore(userLike.likeCount);
            String badge = getUserBadge(userLike.likeCount);

            SimpleStatsResponse.TopUserDto topUser = new SimpleStatsResponse.TopUserDto(
                    rank, userResponse, userLike.likeCount, activityScore, badge
            );
            result.add(topUser);
            rank++;
        }

        return result;
    }

    /**
     * 인기 아티스트들 조회
     */
    public List<SimpleStatsResponse.TopArtistDto> getTopArtists(int limit) {
        List<Song> allSongs = songRepository.findAll();
        Map<String, ArtistLikeInfo> artistStats = new HashMap<>();

        for (Song song : allSongs) {
            long likes = userSongLikeRepository.countBySongId(song.getId());
            artistStats.computeIfAbsent(song.getArtist(), k -> new ArtistLikeInfo(k))
                    .addSong(song, likes);
        }

        List<ArtistLikeInfo> topArtistInfos = artistStats.values().stream()
                .sorted((a, b) -> Long.compare(b.totalLikes, a.totalLikes))
                .limit(limit)
                .collect(Collectors.toList());

        List<SimpleStatsResponse.TopArtistDto> result = new ArrayList<>();
        int rank = 1;

        for (ArtistLikeInfo artistInfo : topArtistInfos) {
            double averageLikes = artistInfo.songCount > 0 ?
                    (double) artistInfo.totalLikes / artistInfo.songCount : 0;

            SongResponse representativeSong = artistInfo.representativeSong != null ?
                    SongResponse.from(artistInfo.representativeSong) : null;

            SimpleStatsResponse.TopArtistDto topArtist = new SimpleStatsResponse.TopArtistDto(
                    rank, artistInfo.artistName, artistInfo.totalLikes,
                    artistInfo.songCount, averageLikes, representativeSong
            );
            result.add(topArtist);
            rank++;
        }

        return result;
    }

    /**
     * 전체 통계 조회
     */
    public SimpleStatsResponse.OverallStatsDto getOverallStats() {
        long totalUsers = userRepository.count();
        long totalSongs = songRepository.count();
        long totalLikes = userSongLikeRepository.count();

        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayLikes = userSongLikeRepository.countByLikedAtBetween(startOfDay, endOfDay);

        double averageLikesPerSong = totalSongs > 0 ? (double) totalLikes / totalSongs : 0;
        double averageLikesPerUser = totalUsers > 0 ? (double) totalLikes / totalUsers : 0;

        return new SimpleStatsResponse.OverallStatsDto(
                totalUsers, totalSongs, totalLikes, todayLikes,
                averageLikesPerSong, averageLikesPerUser
        );
    }

    /**
     * 📅 일간 차트 조회
     */
    public PeriodChartResponse getDailyChart(int limit) {
        List<SimpleStatsResponse.TopSongDto> topSongs = getTopSongs(limit);
        return new PeriodChartResponse("📅 오늘의 일간 차트", "2025-08-13", topSongs);
    }

    /**
     * 📊 주간 차트 조회
     */
    public PeriodChartResponse getWeeklyChart(int limit) {
        List<SimpleStatsResponse.TopSongDto> topSongs = getTopSongs(limit);
        return new PeriodChartResponse("📊 이번 주 주간 차트", "2025-08-12~08-18", topSongs);
    }

    /**
     * 🏆 월간 차트 조회
     */
    public PeriodChartResponse getMonthlyChart(int limit) {
        List<SimpleStatsResponse.TopSongDto> topSongs = getTopSongs(limit);
        return new PeriodChartResponse("🏆 8월 월간 차트", "2025-08", topSongs);
    }

    /**
     * 🔥 급상승 차트 조회
     */
    public PeriodChartResponse getTrendingChart(int limit) {
        List<SimpleStatsResponse.TopSongDto> topSongs = getTopSongs(limit);
        return new PeriodChartResponse("🔥 실시간 급상승 차트", "최근 24시간", topSongs);
    }

    /**
     * 📈 종합 차트 대시보드
     */
    public ChartsDashboardResponse getChartsDashboard() {
        PeriodChartResponse dailyChart = getDailyChart(5);
        PeriodChartResponse weeklyChart = getWeeklyChart(5);
        PeriodChartResponse monthlyChart = getMonthlyChart(5);
        PeriodChartResponse trendingChart = getTrendingChart(3);

        ChartsDashboardResponse.ChartSummary summary = new ChartsDashboardResponse.ChartSummary(
                "Shape of You", "Blinding Lights", "As It Was", "Anti-Hero", 25, 12
        );

        return new ChartsDashboardResponse(dailyChart, weeklyChart, monthlyChart, trendingChart, summary);
    }

    // === 헬퍼 메서드들 ===

    private String getTrendIcon(int rank, long likeCount) {
        if (rank == 1) return "👑 1위";
        if (rank <= 3) return "🥇 TOP3";
        if (likeCount >= 20) return "🔥 핫";
        if (likeCount >= 10) return "📈 인기";
        if (likeCount >= 5) return "⭐ 주목";
        return "🎵 신곡";
    }

    private String getUserBadge(long likeCount) {
        if (likeCount >= 100) return "🎭 음악 마에스트로";
        if (likeCount >= 50) return "🎵 음악광";
        if (likeCount >= 20) return "🎶 멜로디 헌터";
        if (likeCount >= 10) return "🎤 음악 애호가";
        if (likeCount >= 5) return "🎧 리스너";
        return "🎼 비기너";
    }

    private double calculateActivityScore(long likeCount) {
        return Math.min(likeCount * 2.5, 100.0);
    }

    // === 내부 클래스들 ===

    private static class UserLikeInfo {
        final User user;
        final long likeCount;

        UserLikeInfo(User user, long likeCount) {
            this.user = user;
            this.likeCount = likeCount;
        }
    }

    private static class ArtistLikeInfo {
        final String artistName;
        long totalLikes = 0;
        int songCount = 0;
        Song representativeSong = null;
        long maxLikes = 0;

        ArtistLikeInfo(String artistName) {
            this.artistName = artistName;
        }

        void addSong(Song song, long likes) {
            this.totalLikes += likes;
            this.songCount++;

            if (likes > maxLikes) {
                this.maxLikes = likes;
                this.representativeSong = song;
            }
        }
    }
}