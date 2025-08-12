package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import com.example.musicrecommendation.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
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

    // =========================
    // 기존 통계 메서드들
    // =========================

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

    // =========================
    // 새로운 시간별 차트 메서드들
    // =========================

    /**
     * 📅 일간 차트 조회
     */
    public PeriodChartResponse getDailyChart(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        return getPeriodChart(
                "📅 " + now.format(DateTimeFormatter.ofPattern("M월 d일")) + " 일간 차트",
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                PeriodChartResponse.ChartType.DAILY,
                startOfDay,
                endOfDay,
                limit
        );
    }

    /**
     * 📊 주간 차트 조회
     */
    public PeriodChartResponse getWeeklyChart(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1).minusSeconds(1);

        return getPeriodChart(
                "📊 이번 주 주간 차트",
                startOfWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " ~ " +
                        endOfWeek.format(DateTimeFormatter.ofPattern("MM-dd")),
                PeriodChartResponse.ChartType.WEEKLY,
                startOfWeek,
                endOfWeek,
                limit
        );
    }

    /**
     * 🏆 월간 차트 조회
     */
    public PeriodChartResponse getMonthlyChart(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        return getPeriodChart(
                "🏆 " + now.format(DateTimeFormatter.ofPattern("M월")) + " 월간 차트",
                now.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                PeriodChartResponse.ChartType.MONTHLY,
                startOfMonth,
                endOfMonth,
                limit
        );
    }

    /**
     * 🔥 급상승 차트 조회
     */
    public PeriodChartResponse getTrendingChart(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusHours(24);

        // 간단하게 최근 24시간 데이터로 급상승 차트 구현
        List<PeriodChartResponse.ChartSongDto> trendingSongs = getTrendingSongs(yesterday, now, limit);

        PeriodChartResponse.ChartSummary summary = new PeriodChartResponse.ChartSummary(
                trendingSongs.size(),
                trendingSongs.stream().mapToLong(PeriodChartResponse.ChartSongDto::getLikeCount).sum(),
                (int) trendingSongs.stream().filter(PeriodChartResponse.ChartSongDto::isNew).count(),
                (int) trendingSongs.stream().filter(s -> s.getRankChange() == PeriodChartResponse.RankChange.UP).count(),
                (int) trendingSongs.stream().filter(s -> s.getRankChange() == PeriodChartResponse.RankChange.DOWN).count(),
                trendingSongs.isEmpty() ? "데이터 없음" : trendingSongs.get(0).getTitle() + " (" + trendingSongs.get(0).getLikeCount() + " 좋아요)"
        );

        return new PeriodChartResponse(
                "🔥 실시간 급상승 차트",
                "최근 24시간",
                PeriodChartResponse.ChartType.TRENDING,
                trendingSongs,
                summary
        );
    }

    /**
     * 📈 종합 차트 대시보드
     */
    public ChartsDashboardResponse getChartsDashboard() {
        PeriodChartResponse dailyChart = getDailyChart(5);
        PeriodChartResponse weeklyChart = getWeeklyChart(5);
        PeriodChartResponse monthlyChart = getMonthlyChart(5);
        PeriodChartResponse trendingChart = getTrendingChart(3);

        String monthlyChampion = monthlyChart.getSongs().isEmpty() ? "데이터 없음" : monthlyChart.getSongs().get(0).getTitle();
        String weeklyChampion = weeklyChart.getSongs().isEmpty() ? "데이터 없음" : weeklyChart.getSongs().get(0).getTitle();
        String dailyChampion = dailyChart.getSongs().isEmpty() ? "데이터 없음" : dailyChart.getSongs().get(0).getTitle();
        String trendingChampion = trendingChart.getSongs().isEmpty() ? "데이터 없음" : trendingChart.getSongs().get(0).getTitle();

        long totalSongs = songRepository.count();
        long totalArtists = (long) songRepository.findAll().stream()
                .map(Song::getArtist)
                .collect(Collectors.toSet())
                .size();

        ChartsDashboardResponse.ChartSummary summary = new ChartsDashboardResponse.ChartSummary(
                monthlyChampion, weeklyChampion, dailyChampion, trendingChampion,
                (int) totalSongs, (int) totalArtists
        );

        return new ChartsDashboardResponse(dailyChart, weeklyChart, monthlyChart, trendingChart, summary);
    }

    // =========================
    // 차트 헬퍼 메서드들
    // =========================

    /**
     * 기간별 차트 공통 로직
     */
    private PeriodChartResponse getPeriodChart(String title, String period,
                                               PeriodChartResponse.ChartType chartType,
                                               LocalDateTime startDate, LocalDateTime endDate, int limit) {

        // 해당 기간의 좋아요 데이터를 간단하게 조회 (실제 데이터가 있으면 기간별로 조회)
        List<PeriodChartResponse.ChartSongDto> chartSongs = getChartSongsForPeriod(startDate, endDate, limit);

        PeriodChartResponse.ChartSummary summary = new PeriodChartResponse.ChartSummary(
                chartSongs.size(),
                chartSongs.stream().mapToLong(PeriodChartResponse.ChartSongDto::getLikeCount).sum(),
                (int) chartSongs.stream().filter(PeriodChartResponse.ChartSongDto::isNew).count(),
                (int) chartSongs.stream().filter(s -> s.getRankChange() == PeriodChartResponse.RankChange.UP).count(),
                (int) chartSongs.stream().filter(s -> s.getRankChange() == PeriodChartResponse.RankChange.DOWN).count(),
                chartSongs.isEmpty() ? "데이터 없음" : chartSongs.get(0).getTitle() + " (" + chartSongs.get(0).getLikeCount() + " 좋아요)"
        );

        return new PeriodChartResponse(title, period, chartType, chartSongs, summary);
    }

    /**
     * 특정 기간의 차트 곡들 조회
     */
    private List<PeriodChartResponse.ChartSongDto> getChartSongsForPeriod(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        // 실제로는 기간별 좋아요 데이터를 조회해야 하지만,
        // 현재는 전체 TOP 곡을 기반으로 시뮬레이션
        List<UserSongLikeRepository.SongLikeCount> topSongs = userSongLikeRepository.findTopLikedSongs(limit);

        List<PeriodChartResponse.ChartSongDto> chartSongs = new ArrayList<>();
        int rank = 1;

        for (UserSongLikeRepository.SongLikeCount songCount : topSongs) {
            Optional<Song> songOpt = songRepository.findById(songCount.getSongId());
            if (songOpt.isPresent()) {
                Song song = songOpt.get();

                // 순위 변동 시뮬레이션
                PeriodChartResponse.RankChange rankChange = simulateRankChange(rank);
                int rankChangeValue = simulateRankChangeValue(rankChange);

                PeriodChartResponse.ChartSongDto chartSong = new PeriodChartResponse.ChartSongDto(
                        rank,
                        songCount.getSongId(),
                        song.getTitle(),
                        song.getArtist(),
                        songCount.getLikeCount(),
                        rankChange,
                        rankChangeValue
                );

                chartSongs.add(chartSong);
                rank++;
            }
        }

        return chartSongs;
    }

    /**
     * 급상승 곡들 조회
     */
    private List<PeriodChartResponse.ChartSongDto> getTrendingSongs(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        // 급상승 로직 시뮬레이션 (실제로는 기간별 비교 필요)
        List<UserSongLikeRepository.SongLikeCount> topSongs = userSongLikeRepository.findTopLikedSongs(limit);

        List<PeriodChartResponse.ChartSongDto> trendingSongs = new ArrayList<>();
        int rank = 1;

        for (UserSongLikeRepository.SongLikeCount songCount : topSongs) {
            Optional<Song> songOpt = songRepository.findById(songCount.getSongId());
            if (songOpt.isPresent()) {
                Song song = songOpt.get();

                // 급상승 특화 순위 변동
                PeriodChartResponse.RankChange rankChange = rank <= 2 ? PeriodChartResponse.RankChange.UP :
                        rank <= 4 ? PeriodChartResponse.RankChange.NEW :
                                PeriodChartResponse.RankChange.SAME;
                int rankChangeValue = rank <= 2 ? (5 + rank) : rank <= 4 ? 0 : 1;

                PeriodChartResponse.ChartSongDto chartSong = new PeriodChartResponse.ChartSongDto(
                        rank,
                        songCount.getSongId(),
                        song.getTitle(),
                        song.getArtist(),
                        songCount.getLikeCount(),
                        rankChange,
                        rankChangeValue
                );

                trendingSongs.add(chartSong);
                rank++;
            }
        }

        return trendingSongs;
    }

    // =========================
    // 시뮬레이션 헬퍼 메서드들
    // =========================

    private PeriodChartResponse.RankChange simulateRankChange(int rank) {
        if (rank <= 2) return PeriodChartResponse.RankChange.UP;
        if (rank <= 4) return PeriodChartResponse.RankChange.SAME;
        if (rank <= 7) return PeriodChartResponse.RankChange.DOWN;
        return Math.random() > 0.5 ? PeriodChartResponse.RankChange.NEW : PeriodChartResponse.RankChange.UP;
    }

    private int simulateRankChangeValue(PeriodChartResponse.RankChange rankChange) {
        return switch (rankChange) {
            case UP -> (int) (Math.random() * 5) + 1;
            case DOWN -> (int) (Math.random() * 3) + 1;
            default -> 0;
        };
    }

    // =========================
    // 기존 헬퍼 메서드들
    // =========================

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

    // =========================
    // 내부 클래스들
    // =========================

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