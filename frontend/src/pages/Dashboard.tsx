// frontend/src/pages/Dashboard.tsx
import React, { useState, useEffect, useCallback } from 'react';
import { Music, Users, Heart, Play, Star, Headphones, RefreshCw, AlertCircle } from 'lucide-react';
import api from '../api/client';

interface Recommendation {
    id: number | string;
    title: string;
    artist: string;
    image: string;
    genre: string;
    score: number;
    isLiked?: boolean; // 좋아요 상태 추가
    spotifyId?: string; // Spotify 트랙 ID 추가
}

interface DashboardStats {
    recommendationAccuracy: number; // 추천 정확도 (%)
    matchedUsers: number;
    favoriteGenres: string[];
}

interface RefreshLimits {
    daily: number;
    hourly: number;
    cooldown: number; // seconds
}

interface RefreshState {
    dailyCount: number;
    hourlyCount: number;
    lastRefresh: number;
    nextAllowed: number;
}

const DEFAULT_REFRESH_LIMITS: RefreshLimits = {
    daily: 10,
    hourly: 3,
    cooldown: 30
};

const STORAGE_KEYS = {
    REFRESH_STATE: 'music_recommendations_refresh_state',
    CACHED_RECS: 'music_recommendations_cache'
};

// 사용자별 캐시 키 생성
const getUserSpecificKey = (userId: number, baseKey: string) => {
    return `${baseKey}_user_${userId}`;
};

// 더미 데이터 제거 - 실제 추천 시스템 사용
const FALLBACK_RECS: Recommendation[] = [];

const Dashboard: React.FC = () => {
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [systemStatus, setSystemStatus] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [likedTracks, setLikedTracks] = useState<Set<string | number>>(new Set());
    const [refreshLoading, setRefreshLoading] = useState(false);
    const [showRefreshStatus, setShowRefreshStatus] = useState(false);
    const [refreshLimits, setRefreshLimits] = useState<{
        daily: {
            used: number;
            remaining: number;
            max: number;
        };
        hourly: {
            used: number;
            remaining: number;
            max: number;
        };
        resetDate: string;
    } | null>(null);

    const imageFallback = (e: React.SyntheticEvent<HTMLImageElement>, text: string) => {
        const img = e.currentTarget;
        img.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(text)}&backgroundColor=random`;
    };

    // YouTube에서 음악 검색
    const playOnYouTube = (track: Recommendation) => {
        const searchQuery = `${track.artist} ${track.title}`;
        const youtubeSearchUrl = `https://www.youtube.com/results?search_query=${encodeURIComponent(searchQuery)}`;
        window.open(youtubeSearchUrl, '_blank');
    };

    // Spotify 앱에서 열기
    const openInSpotify = (track: Recommendation) => {
        if (track.spotifyId) {
            const spotifyAppUrl = `spotify:track:${track.spotifyId}`;
            window.location.href = spotifyAppUrl;
        } else {
            // spotifyId가 없으면 일반 트랙 ID 사용
            const spotifyAppUrl = `spotify:track:${track.id}`;
            window.location.href = spotifyAppUrl;
        }
    };

    // 좋아요 토글 함수
    const toggleTrackLike = async (trackId: string | number, trackInfo?: Recommendation) => {
        try {
            const isCurrentlyLiked = likedTracks.has(trackId);
            
            if (isCurrentlyLiked) {
                // 좋아요 해제
                await api.delete(`/api/likes/song/${trackId}`);
                setLikedTracks(prev => {
                    const newSet = new Set(prev);
                    newSet.delete(trackId);
                    return newSet;
                });
            } else {
                // 좋아요 추가
                const likeData = {
                    songId: trackId,
                    title: trackInfo?.title || `Track ${trackId}`,
                    artist: trackInfo?.artist || 'Unknown Artist',
                    imageUrl: trackInfo?.image || '',
                    externalId: String(trackId)
                };
                
                await api.post('/api/likes/song', likeData);
                setLikedTracks(prev => {
                    const newSet = new Set(prev);
                    newSet.add(trackId);
                    return newSet;
                });
                
                // 좋아요 추가 후 해당 곡을 추천 목록에서 제거
                setRecommendations(prev => prev.filter(track => track.id !== trackId));
            }
        } catch (error) {
            console.error('좋아요 처리 실패:', error);
            alert('좋아요 처리 중 오류가 발생했습니다.');
        }
    };


    const canRefresh = (): boolean => {
        // 서버에서 제한 관리하므로 프론트엔드에서는 남은 횟수 기반으로만 판단
        if (refreshLimits) {
            if (refreshLimits.daily.remaining <= 0 || refreshLimits.hourly.remaining <= 0) {
                return false;
            }
        }
        return true;
    };

    // 사용자별 캐시된 추천 음악 로드 (새로고침 시에는 무시)
    const loadCachedRecommendations = (userId: number, isRefresh: boolean = false) => {
        // 새로고침일 때는 캐시 무시하고 새로운 데이터 로드
        if (isRefresh) {
            const userCacheKey = getUserSpecificKey(userId, STORAGE_KEYS.CACHED_RECS);
            localStorage.removeItem(userCacheKey);
            return false;
        }
        
        const userCacheKey = getUserSpecificKey(userId, STORAGE_KEYS.CACHED_RECS);
        const cached = localStorage.getItem(userCacheKey);
        if (cached) {
            try {
                const { data, timestamp, cachedUserId }: { 
                    data: Recommendation[], 
                    timestamp: number,
                    cachedUserId: number 
                } = JSON.parse(cached);
                
                // 캐시된 userId와 현재 userId가 일치하고, 캐시가 유효한지 확인
                if (cachedUserId === userId && Date.now() - timestamp < 4 * 60 * 60 * 1000) {
                    console.log(`✅ 사용자 ${userId}의 캐시된 추천 로드`);
                    setRecommendations(data);
                    return true;
                } else {
                    // 캐시가 만료되었거나 다른 사용자의 캐시면 삭제
                    localStorage.removeItem(userCacheKey);
                }
            } catch {
                localStorage.removeItem(userCacheKey);
            }
        }
        return false;
    };

    // 사용자별 추천 음악 캐시 저장
    const cacheRecommendations = (userId: number, recs: Recommendation[]) => {
        const userCacheKey = getUserSpecificKey(userId, STORAGE_KEYS.CACHED_RECS);
        const cacheData = {
            data: recs,
            timestamp: Date.now(),
            cachedUserId: userId
        };
        localStorage.setItem(userCacheKey, JSON.stringify(cacheData));
        console.log(`💾 사용자 ${userId}의 추천 캐시 저장: ${recs.length}개`);
    };

    const loadRecommendationsAsync = async (isRefresh: boolean = false) => {
        if (isRefresh) {
            setRefreshLoading(true);
        }
        
        try {
            // 사용자 ID 가져오기
            let userId: number | null = null;
            
            // OAuth 로그인 확인
            try {
                const me = await api.get('/api/auth/me');
                if (me?.data?.authenticated && me?.data?.user?.id) {
                    userId = Number(me.data.user.id);
                }
            } catch {}
            
            // OAuth 로그인이 안되어 있으면 로컬 로그인 확인
            if (!userId) {
                try {
                    const localMe = await api.get('/api/auth/local/me');
                    if (localMe?.data?.success && localMe?.data?.user?.id) {
                        userId = Number(localMe.data.user.id);
                    }
                } catch {}
            }
            
            // 로그인되지 않은 경우 오류 처리
            if (!userId) {
                console.error('❌ 사용자 인증 실패 - 로그인이 필요합니다');
                return;
            }

            // 좋아요 목록 로드
            let currentLikedTracks = new Set<string | number>();
            try {
                const response = await api.get(`/api/likes/user/${userId}`);
                const likes = response.data?.content || response.data || [];
                const likedIds = likes.map((like: any) => like.externalId || like.songId || like.song?.id).filter(Boolean);
                currentLikedTracks = new Set<string | number>(likedIds);
                setLikedTracks(currentLikedTracks);
            } catch (error) {
                console.warn('좋아요 목록 불러오기 실패:', error);
            }

            // 제한 알림 플래그 (중복 알림 방지)
            let hasShownLimitAlert = false;
            
            // Progressive Loading - 5개씩 단계별 로딩
            const loadBatch = async (offset: number, limit: number, shouldRefresh: boolean = false) => {
                try {
                    const refreshParam = shouldRefresh ? '&refresh=true' : '';
                    const rec = await api.get(`/api/recommendations/user/${userId}?offset=${offset}&limit=${limit}${refreshParam}`);
                    
                    // 서버 응답 헤더에서 새로고침 제한 정보 추출 (일일 + 시간당)
                    console.log('🔍 API 응답 헤더 디버깅:', rec.headers);
                    if (rec.headers) {
                        const dailyUsed = rec.headers['x-refresh-used'];
                        const dailyRemaining = rec.headers['x-refresh-remaining'];
                        const dailyMax = rec.headers['x-refresh-max'];
                        const hourlyUsed = rec.headers['x-hourly-used'];
                        const hourlyRemaining = rec.headers['x-hourly-remaining'];
                        const hourlyMax = rec.headers['x-hourly-max'];
                        const resetDate = rec.headers['x-refresh-reset-date'];
                        const limitExceeded = rec.headers['x-limit-exceeded'];
                        
                        console.log('📊 헤더값들:', {
                            dailyUsed, dailyRemaining, dailyMax,
                            hourlyUsed, hourlyRemaining, hourlyMax,
                            resetDate, limitExceeded
                        });
                        
                        if (dailyUsed !== undefined && dailyRemaining !== undefined && 
                            hourlyUsed !== undefined && hourlyRemaining !== undefined) {
                            const limits = {
                                daily: {
                                    used: parseInt(dailyUsed),
                                    remaining: parseInt(dailyRemaining),
                                    max: parseInt(dailyMax)
                                },
                                hourly: {
                                    used: parseInt(hourlyUsed),
                                    remaining: parseInt(hourlyRemaining),
                                    max: parseInt(hourlyMax)
                                },
                                resetDate: resetDate || ''
                            };
                            console.log('✅ 새로고침 제한 설정:', limits);
                            setRefreshLimits(limits);
                        } else {
                            console.log('❌ 필수 헤더가 없습니다');
                        }
                    } else {
                        console.log('❌ 응답 헤더가 없습니다');
                    }
                    
                    // 제한 초과로 인한 빈 응답인지 확인
                    if (rec.data && typeof rec.data === 'object' && !Array.isArray(rec.data)) {
                        // 서버에서 {success: false, message: "...", recommendations: []} 형태로 응답할 때
                        if (rec.data.success === false && rec.data.message) {
                            console.log('🚫 새로고침 제한 초과:', rec.data.message);
                            
                            // 제한 초과 알림 표시 (중복 방지)
                            if (isRefresh && !hasShownLimitAlert) {
                                alert(`⏰ ${rec.data.message}`);
                                setShowRefreshStatus(true);
                                setTimeout(() => setShowRefreshStatus(false), 5000);
                                hasShownLimitAlert = true;
                            }
                            
                            return []; // 빈 배열 반환
                        }
                        return rec.data.recommendations || [];
                    }
                    
                    return Array.isArray(rec.data) ? rec.data : [];
                } catch (error) {
                    console.error('배치 로딩 실패:', error);
                    return [];
                }
            };

            let allRecs: Recommendation[] = [];
            let limitExceeded = false;
            
            // 첫 번째 배치 (새로고침일 때만 refresh=true 전송)
            const firstBatch = await loadBatch(0, 5, isRefresh);
            allRecs = [...firstBatch];
            
            // 첫 번째 배치에서 제한 확인 - 제한이 걸리면 두 번째 배치 호출 안함
            if (firstBatch.length === 0 && refreshLimits && 
                (refreshLimits.daily.remaining <= 0 || refreshLimits.hourly.remaining <= 0)) {
                limitExceeded = true;
            }
            
            // 두 번째 배치 (제한이 없을 때만 호출, 항상 일반 조회)
            if (!limitExceeded) {
                const secondBatch = await loadBatch(5, 5, false); // 두 번째부터는 항상 일반 조회
                allRecs = [...allRecs, ...secondBatch];
            }
            
            // 중복 제거 및 필터링 (더미 데이터 사용 안함)
            const filteredRecs = allRecs.filter(track => !currentLikedTracks.has(track.id));
            
            // ID 기반 중복 제거
            const uniqueRecs = filteredRecs.filter((track, index, self) => 
                index === self.findIndex(t => t.id === track.id)
            );
            
            // 제목+아티스트 기반 중복 제거 (더 엄격한 중복 체크)
            const finalRecs = uniqueRecs.filter((track, index, self) => 
                index === self.findIndex(t => 
                    t.title.toLowerCase().trim() === track.title.toLowerCase().trim() && 
                    t.artist.toLowerCase().trim() === track.artist.toLowerCase().trim()
                )
            ).slice(0, 8);
            
            setRecommendations(finalRecs);
            
            // 캐시 저장
            if (finalRecs.length > 0) {
                cacheRecommendations(userId, finalRecs);
            }
            
        } catch (error) {
            console.error('추천 음악 로딩 실패:', error);
            
            // Error Fallback - API 실패 시 빈 상태로 유지 (더미 데이터 사용 안함)
            setRecommendations([]);
        } finally {
            if (isRefresh) {
                setRefreshLoading(false);
            }
        }
    };

    const handleRefresh = async () => {
        if (!canRefresh()) {
            alert('새로고침 제한에 도달했습니다.');
            return;
        }
        
        setRefreshLoading(true);
        
        try {
            await loadRecommendationsAsync(true);
        } catch (error) {
            console.error('새로고침 실패:', error);
            // 서버에서 제한 에러가 온 경우
            if (error && typeof error === 'object' && 'response' in error) {
                const response = (error as any).response;
                if (response?.status === 429 || response?.data?.message) {
                    alert(response.data.message || '새로고침 제한에 도달했습니다.');
                    setShowRefreshStatus(true);
                    setTimeout(() => setShowRefreshStatus(false), 5000);
                } else {
                    alert('새로고침 중 오류가 발생했습니다.');
                }
            } else {
                alert('새로고침 중 오류가 발생했습니다.');
            }
        } finally {
            setRefreshLoading(false);
        }
    };

    const getRefreshButtonText = () => {
        if (refreshLimits) {
            if (refreshLimits.daily.remaining <= 0) {
                return '📅 오늘 새로고침 완료';
            }
            if (refreshLimits.hourly.remaining <= 0) {
                return '🕐 1시간 후 새로고침 가능';
            }
            const minRemaining = Math.min(refreshLimits.daily.remaining, refreshLimits.hourly.remaining);
            return `🔄 새로고침 (${minRemaining}회 남음)`;
        }
        return '🔄 새로고침';
    };


    const loadDashboardData = useCallback(async () => {
        setLoading(true);
        try {
            // 1) 사용자 ID
            let userId: number | null = null;
            
            // OAuth 로그인 확인
            try {
                const me = await api.get('/api/auth/me');
                if (me?.data?.authenticated && me?.data?.user?.id) {
                    userId = Number(me.data.user.id);
                }
            } catch {}
            
            // OAuth 로그인이 안되어 있으면 로컬 로그인 확인
            if (!userId) {
                try {
                    const localMe = await api.get('/api/auth/local/me');
                    if (localMe?.data?.success && localMe?.data?.user?.id) {
                        userId = Number(localMe.data.user.id);
                    }
                } catch {}
            }
            
            // 로그인되지 않은 경우 오류 처리
            if (!userId) {
                console.error('❌ 사용자 인증 실패 - 로그인이 필요합니다');
                return;
            }

            // 2) 시스템 상태
            let sys: any = null;
            try {
                const sysRes = await api.get('/api/realtime-matching/system-status');
                sys = sysRes.data ?? null;
                setSystemStatus(sys);
            } catch {
                setSystemStatus(null);
            }

            // 3) 매칭 상태 (통계 보조)
            let matchStatus: any = null;
            try {
                const ms = await api.get(`/api/realtime-matching/status/${userId}`);
                matchStatus = ms.data ?? null;
            } catch {
                matchStatus = null;
            }

            // 캐시된 추천 음악 먼저 로드 (즉시 표시)
            const hasCached = loadCachedRecommendations(userId, false);
            
            // 캐시가 없을 때만 서버에서 추천 음악 가져오기
            if (!hasCached) {
                setTimeout(() => loadRecommendationsAsync(false), 100);
            }

            // 4) 사용자 통계 데이터 수집
            let recommendationAccuracy = 0;
            let userMatchCount = 0; 
            let userFavoriteGenres: string[] = [];
            
            // 추천 정확도 계산 (좋아요한 곡 비율)
            try {
                // 좋아요한 총 곡 수
                const likesResponse = await api.get(`/api/likes/user/${userId}`);
                const totalLikes = likesResponse.data?.content?.length || likesResponse.data?.length || 0;
                
                // 최근 추천받은 곡 수 (최근 7일간)
                const recResponse = await api.get(`/api/recommendations/user/${userId}?offset=0&limit=50`);
                const recentRecommendations = Array.isArray(recResponse.data) ? recResponse.data.length : 0;
                
                // 고도화된 추천 정확도 계산
                if (recentRecommendations > 0) {
                    const recommendations = Array.isArray(recResponse.data) ? recResponse.data : [];
                    
                    // 1. 직접 좋아요 기반 정확도 (기존)
                    const recommendedTrackIds = new Set(recommendations.map((track: any) => String(track.id)));
                    const likesResponse2 = await api.get(`/api/likes/user/${userId}`);
                    const userLikes = likesResponse2.data?.content || likesResponse2.data || [];
                    const likedTrackIds = userLikes.map((like: any) => String(like.externalId || like.songId || like.song?.id)).filter(Boolean);
                    const directMatches = likedTrackIds.filter((id: string) => recommendedTrackIds.has(id)).length;
                    
                    // 2. 아티스트 매칭률 (좋아하는 아티스트의 곡 추천 성공률)
                    const likedArtists = new Set(userLikes.map((like: any) => like.song?.artist).filter(Boolean));
                    const artistMatches = recommendations.filter((track: any) => likedArtists.has(track.artist)).length;
                    
                    // 3. 고점수 추천 비율 (80점 이상으로 기준 완화)
                    const highScoreRecommendations = recommendations.filter((track: any) => track.score >= 80).length;
                    
                    // 4. 평균 점수 기반 추천 품질
                    const avgScore = recommendations.reduce((sum: number, track: any) => sum + (track.score || 0), 0) / recommendations.length;
                    const scoreQuality = Math.min(avgScore, 100); // 평균 점수를 그대로 품질 지표로 사용
                    
                    // 5. 장르 기반 매칭 (선호 장르와 일치)
                    const profileResponse = await api.get(`/api/users/${userId}/profile`);
                    const userGenres = new Set(
                        (profileResponse.data?.favoriteGenres || [])
                            .map((genre: any) => typeof genre === 'string' ? genre : genre.name)
                            .filter(Boolean)
                    );
                    const genreMatches = recommendations.filter((track: any) => 
                        track.genre && (
                            userGenres.has(track.genre) || 
                            track.genre.includes('선호') || 
                            track.genre.includes('리뷰')
                        )
                    ).length;
                    
                    // 복합 정확도 계산 (더 관대한 가중 평균)
                    const directAccuracy = (directMatches / recentRecommendations) * 100;
                    const artistAccuracy = (artistMatches / recentRecommendations) * 100;
                    const scoreAccuracy = (highScoreRecommendations / recentRecommendations) * 100;
                    const genreAccuracy = (genreMatches / recentRecommendations) * 100;
                    
                    // 개선된 가중치: 직접 좋아요(20%), 아티스트(30%), 평균점수(25%), 고점수(15%), 장르(10%)
                    recommendationAccuracy = Math.round(
                        directAccuracy * 0.2 + 
                        artistAccuracy * 0.3 + 
                        scoreQuality * 0.25 +
                        scoreAccuracy * 0.15 + 
                        genreAccuracy * 0.1
                    );
                    
                    // 최소 보정: 아무것도 매칭되지 않으면 점수 기반으로라도 계산
                    if (recommendationAccuracy === 0 && recommendations.length > 0) {
                        const avgScore = recommendations.reduce((sum: number, track: any) => sum + (track.score || 0), 0) / recommendations.length;
                        recommendationAccuracy = Math.max(Math.round(avgScore * 0.8), 30); // 평균 점수의 80%로 추정
                    }
                } else if (totalLikes > 0) {
                    // 추천받은 곡이 없지만 좋아요가 있으면 추정치
                    recommendationAccuracy = Math.min(40 + (totalLikes * 1), 75);
                } else {
                    recommendationAccuracy = 0;
                }
                
                console.log(`추천 정확도: ${recommendationAccuracy}% (좋아요: ${totalLikes}곡)`);
            } catch (error) {
                console.warn('추천 정확도 계산 실패:', error);
                recommendationAccuracy = 0;
            }

            // 사용자 개인별 매칭 통계 (실제 매칭된 총 횟수)
            try {
                const matchStatsResponse = await api.get(`/api/matching/stats/user/${userId}`);
                if (matchStatsResponse.data?.totalMatches !== undefined) {
                    userMatchCount = matchStatsResponse.data.totalMatches;
                    console.log('사용자 개인 매칭 횟수:', userMatchCount);
                } else {
                    userMatchCount = 0;
                    console.warn('개인 매칭 통계 데이터가 없습니다');
                }
            } catch (error) {
                console.warn('개인 매칭 통계 조회 실패:', error);
                // 폴백: 전체 시스템 매칭 통계 사용
                userMatchCount = sys?.matchingSystem?.statistics?.totalMatched ?? 0;
            }
            
            // 사용자 선호 장르 (프로필에서 가져오기)
            try {
                const profileResponse = await api.get(`/api/users/${userId}/profile`);
                if (profileResponse.data?.favoriteGenres && Array.isArray(profileResponse.data.favoriteGenres)) {
                    const genreNames = profileResponse.data.favoriteGenres.map((genre: any) => {
                        if (typeof genre === 'string') {
                            return genre;
                        } else if (genre && typeof genre === 'object' && genre.name) {
                            return genre.name;
                        }
                        return '';
                    }).filter(Boolean);
                    
                    // 중복 제거 및 최대 3개까지만 표시
                    const uniqueGenres = Array.from(new Set<string>(genreNames));
                    userFavoriteGenres = uniqueGenres.slice(0, 3);
                }
            } catch (error) {
                console.warn('사용자 선호 장르 조회 실패:', error);
                userFavoriteGenres = [];
            }
            
            // 기본값 설정 (데이터가 없을 때)
            if (userFavoriteGenres.length === 0) {
                userFavoriteGenres = ['장르 설정 필요'];
            }

            // 5) 통계 계산 (실제 데이터 사용)
            const calculated: DashboardStats = {
                recommendationAccuracy: recommendationAccuracy,
                matchedUsers: userMatchCount,
                favoriteGenres: userFavoriteGenres,
            };
            setStats(calculated);
        } catch (e) {
            // 전체 실패 폴백 - 실제 데이터 기반 기본값
            console.error('Dashboard 데이터 로딩 전체 실패:', e);
            setSystemStatus(null);
            setStats({
                recommendationAccuracy: 0,
                matchedUsers: 0,
                favoriteGenres: ['장르 설정 필요'],
            });
            // 전체 실패 시 빈 배열로 설정 (캐시 없이)
            setRecommendations([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadDashboardData();
    }, [loadDashboardData]);

    
    

    if (loading) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-white mx-auto mb-4" />
                    <div className="text-white text-xl">대시보드를 불러오는 중...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900">
            <div className="container mx-auto px-4 py-8">
                {/* 헤더 */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-white mb-2">음악 추천 대시보드</h1>
                    <p className="text-blue-200 text-lg">당신만을 위한 맞춤 음악을 발견하세요</p>
                </div>

                {/* 시스템 상태 */}
                {systemStatus && (
                    <div className="mb-6 bg-white/10 backdrop-blur-lg rounded-2xl p-4 border border-white/20">
                        <div className="flex items-center justify-between">
                            <div className="text-white">
                                <h3 className="font-bold">시스템 상태</h3>
                                <p className="text-sm text-blue-200">{systemStatus.message}</p>
                            </div>
                            <div className="flex items-center space-x-2">
                                <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
                                <span className="text-green-400 text-sm">온라인</span>
                            </div>
                        </div>
                    </div>
                )}

                {/* 통계 카드 */}
                {stats && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">추천 정확도</p>
                                    <p className="text-3xl font-bold text-white">{stats.recommendationAccuracy}%</p>
                                </div>
                                <Star className="w-12 h-12 text-yellow-400" />
                            </div>
                        </div>

                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">매칭된 사용자</p>
                                    <p className="text-3xl font-bold text-white">{stats.matchedUsers}</p>
                                </div>
                                <Users className="w-12 h-12 text-blue-400" />
                            </div>
                        </div>

                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">선호 장르</p>
                                    <p className="text-xl font-bold text-white">{stats.favoriteGenres.join(', ')}</p>
                                </div>
                                <Headphones className="w-12 h-12 text-pink-400" />
                            </div>
                        </div>
                    </div>
                )}

                {/* 추천 음악 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-8 border border-white/20">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-2xl font-bold text-white flex items-center space-x-2">
                            <Star className="w-6 h-6 text-yellow-400" />
                            <span>오늘의 추천 음악</span>
                        </h2>
                        <div className="flex items-center space-x-3">
                            {showRefreshStatus && (
                                <div className="bg-amber-500/20 text-amber-200 px-3 py-1 rounded-lg text-sm flex items-center space-x-1">
                                    <AlertCircle className="w-4 h-4" />
                                    <span>새로고침 제한</span>
                                </div>
                            )}
                            <button
                                className={`px-4 py-2 rounded-lg transition-all duration-200 flex items-center space-x-2 ${
                                    canRefresh() && !refreshLoading
                                        ? 'bg-purple-600 hover:bg-purple-700 text-white' 
                                        : 'bg-gray-600 text-gray-300 cursor-not-allowed'
                                }`}
                                onClick={handleRefresh}
                                disabled={!canRefresh() || refreshLoading}
                            >
                                {refreshLoading ? (
                                    <>
                                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
                                        <span>✨ 새로운 추천 로딩중...</span>
                                    </>
                                ) : (
                                    <>
                                        <RefreshCw className="w-4 h-4" />
                                        <span className="text-sm">{getRefreshButtonText()}</span>
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                    
                    {/* 서버 기반 새로고침 정보 */}
                    {refreshLimits && (
                        <div className="mb-4 text-center">
                            <div className={`inline-flex items-center space-x-4 rounded-lg px-4 py-2 text-sm ${
                                refreshLimits.daily.remaining <= 0 
                                    ? 'bg-red-500/20 text-red-200' 
                                    : refreshLimits.hourly.remaining <= 0 
                                        ? 'bg-amber-500/20 text-amber-200'
                                        : 'bg-white/5 text-blue-200'
                            }`}>
                                <span>📊 일일: {refreshLimits.daily.used}/{refreshLimits.daily.max}</span>
                                <span>⏱️ 시간당: {refreshLimits.hourly.used}/{refreshLimits.hourly.max}</span>
                                <span>🔄 남은 횟수: {Math.min(refreshLimits.daily.remaining, refreshLimits.hourly.remaining)}회</span>
                                {refreshLimits.daily.remaining <= 0 ? (
                                    <span className="font-semibold">📅 일일 제한 완료</span>
                                ) : refreshLimits.hourly.remaining <= 0 ? (
                                    <span className="font-semibold">🕐 1시간 후 새로고침 가능</span>
                                ) : (
                                    refreshLimits.resetDate && (
                                        <span>🕐 리셋: {new Date(refreshLimits.resetDate).toLocaleDateString()}</span>
                                    )
                                )}
                            </div>
                        </div>
                    )}

                    {/* 제한 초과 시 특별 메시지 */}
                    {refreshLimits && (refreshLimits.daily.remaining <= 0 || refreshLimits.hourly.remaining <= 0) && (
                        <div className="mb-6 text-center">
                            <div className={`p-4 rounded-xl border-2 ${
                                refreshLimits.daily.remaining <= 0 
                                    ? 'bg-red-500/10 border-red-500/30 text-red-200'
                                    : 'bg-amber-500/10 border-amber-500/30 text-amber-200'
                            }`}>
                                <div className="flex items-center justify-center space-x-2 mb-2">
                                    <AlertCircle className="w-5 h-5" />
                                    <span className="font-semibold text-lg">
                                        {refreshLimits.daily.remaining <= 0 ? '일일 새로고침 완료!' : '시간당 제한 도달!'}
                                    </span>
                                </div>
                                <p className="text-sm opacity-90">
                                    {refreshLimits.daily.remaining <= 0 
                                        ? '오늘 할당된 추천 음악을 모두 받으셨습니다. 내일 다시 새로운 추천을 받아보세요!'
                                        : '시간당 새로고침 한도에 도달했습니다. 1시간 후에 다시 시도해주세요!'
                                    }
                                </p>
                                {refreshLimits.daily.remaining > 0 && refreshLimits.hourly.remaining <= 0 && (
                                    <p className="text-xs mt-2 opacity-75">
                                        💡 팁: 현재 표시된 추천 음악들을 즐겨보세요!
                                    </p>
                                )}
                            </div>
                        </div>
                    )}

                    {refreshLoading && (
                        <div className="mb-6 text-center">
                            <div className="inline-flex items-center space-x-2 bg-purple-500/20 text-purple-200 px-4 py-2 rounded-lg">
                                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-purple-200" />
                                <span>새로운 음악을 찾고 있어요...</span>
                            </div>
                        </div>
                    )}
                    
                    {recommendations.length === 0 && !refreshLoading ? (
                        <div className="text-center py-12">
                            <div className="bg-white/5 rounded-xl p-8">
                                <Music className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                                <h3 className="text-xl text-white mb-2">추천 음악이 없습니다</h3>
                                <p className="text-gray-300 mb-4">
                                    {refreshLimits && (refreshLimits.daily.remaining <= 0 || refreshLimits.hourly.remaining <= 0) 
                                        ? '새로고침 제한으로 인해 새로운 추천을 받을 수 없습니다.'
                                        : '프로필을 설정하거나 음악을 좋아요하면 더 나은 추천을 받을 수 있어요!'
                                    }
                                </p>
                                {(!refreshLimits || (refreshLimits.daily.remaining > 0 && refreshLimits.hourly.remaining > 0)) && (
                                    <button
                                        onClick={handleRefresh}
                                        className="bg-purple-600 hover:bg-purple-700 text-white px-6 py-2 rounded-lg transition-colors"
                                    >
                                        새로고침 시도
                                    </button>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                            {recommendations.map((track) => (
                            <div
                                key={track.id}
                                className="bg-white/10 rounded-xl p-4 hover:bg-white/20 transition-all duration-300 cursor-pointer group"
                                title={`${track.artist} - ${track.title}`}
                            >
                                <div className="relative mb-4">
                                    <img
                                        src={track.image}
                                        alt={track.title}
                                        className="w-full h-48 object-cover rounded-lg"
                                        onError={(e) => imageFallback(e, `${track.artist} ${track.title}`)}
                                    />
                                    <div className="absolute inset-0 bg-black/50 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                                        <button
                                            onClick={() => playOnYouTube(track)}
                                            className="bg-red-600/80 hover:bg-red-600 rounded-full p-3 transition-colors"
                                            title="YouTube에서 검색"
                                        >
                                            <Play className="w-8 h-8 text-white" />
                                        </button>
                                    </div>
                                    <div className="absolute top-2 right-2 bg-green-500 text-white px-2 py-1 rounded-full text-xs font-bold">
                                        {track.score}%
                                    </div>
                                </div>

                                <h3 className="text-white font-bold text-lg mb-1 truncate">{track.title}</h3>
                                <p className="text-blue-200 text-sm mb-2 truncate">{track.artist}</p>
                                <div className="flex items-center justify-between">
                                    <span className="bg-purple-600 text-white px-2 py-1 rounded-full text-xs">{track.genre}</span>
                                    <div className="flex space-x-2">
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                openInSpotify(track);
                                            }}
                                            className="text-green-400 hover:text-green-300 transition-colors"
                                            title="Spotify 앱에서 듣기"
                                        >
                                            <Music className="w-5 h-5" />
                                        </button>
                                        <div title={likedTracks.has(track.id) ? "좋아요 취소" : "좋아요"}>
                                            <Heart 
                                                className={`w-5 h-5 cursor-pointer transition-colors ${
                                                    likedTracks.has(track.id) 
                                                        ? 'text-red-500 fill-current' 
                                                        : 'text-red-400 hover:text-red-300'
                                                }`}
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    toggleTrackLike(track.id, track);
                                                }}
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                        </div>
                    )}
                </div>

            </div>
        </div>
    );
};

export default Dashboard;
