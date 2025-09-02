// frontend/src/pages/Dashboard.tsx
import React, { useState, useEffect, useCallback } from 'react';
import { Music, Users, Heart, TrendingUp, Play, Clock, Star, Headphones, RefreshCw, AlertCircle } from 'lucide-react';
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
    totalRecommendations: number;
    matchedUsers: number;
    favoriteGenres: string[];
    listeningTime: number; // minutes
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

const REFRESH_LIMITS: RefreshLimits = {
    daily: 10,
    hourly: 3,
    cooldown: 30
};

const STORAGE_KEYS = {
    REFRESH_STATE: 'music_recommendations_refresh_state',
    CACHED_RECS: 'music_recommendations_cache'
};

const FALLBACK_RECS: Recommendation[] = [
    {
        id: 1,
        title: 'Spring Day',
        artist: 'BTS',
        image: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop',
        genre: 'K-Pop',
        score: 95,
    },
    {
        id: 2,
        title: 'Blinding Lights',
        artist: 'The Weeknd',
        image: 'https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=300&h=300&fit=crop',
        genre: 'Pop',
        score: 89,
    },
    {
        id: 3,
        title: 'Good 4 U',
        artist: 'Olivia Rodrigo',
        image: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop',
        genre: 'Pop Rock',
        score: 87,
    },
    {
        id: 4,
        title: 'Levitating',
        artist: 'Dua Lipa',
        image: 'https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=300&h=300&fit=crop',
        genre: 'Dance Pop',
        score: 92,
    },
];

const Dashboard: React.FC = () => {
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [systemStatus, setSystemStatus] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [likedTracks, setLikedTracks] = useState<Set<string | number>>(new Set());
    const [refreshState, setRefreshState] = useState<RefreshState>({ dailyCount: 0, hourlyCount: 0, lastRefresh: 0, nextAllowed: 0 });
    const [refreshLoading, setRefreshLoading] = useState(false);
    const [showRefreshStatus, setShowRefreshStatus] = useState(false);

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

    // 새로고침 상태 관리
    const initializeRefreshState = () => {
        const stored = localStorage.getItem(STORAGE_KEYS.REFRESH_STATE);
        if (stored) {
            try {
                const parsed: RefreshState = JSON.parse(stored);
                const now = Date.now();
                const today = new Date().toDateString();
                const storedDate = new Date(parsed.lastRefresh).toDateString();
                const currentHour = new Date().getHours();
                const storedHour = new Date(parsed.lastRefresh).getHours();
                
                // 날짜가 바뀌면 일일 카운트 리셋
                if (today !== storedDate) {
                    parsed.dailyCount = 0;
                }
                
                // 시간이 바뀌면 시간당 카운트 리셋
                if (currentHour !== storedHour) {
                    parsed.hourlyCount = 0;
                }
                
                setRefreshState(parsed);
                return parsed;
            } catch {
                // 파싱 실패시 초기값 반환
            }
        }
        
        const initial: RefreshState = { dailyCount: 0, hourlyCount: 0, lastRefresh: 0, nextAllowed: 0 };
        setRefreshState(initial);
        return initial;
    };

    const updateRefreshState = (newState: RefreshState) => {
        setRefreshState(newState);
        localStorage.setItem(STORAGE_KEYS.REFRESH_STATE, JSON.stringify(newState));
    };

    const canRefresh = (state: RefreshState): { allowed: boolean; reason?: string; waitTime?: number } => {
        const now = Date.now();
        
        // 쿨다운 체크
        if (now < state.nextAllowed) {
            const waitSeconds = Math.ceil((state.nextAllowed - now) / 1000);
            return { allowed: false, reason: 'cooldown', waitTime: waitSeconds };
        }
        
        // 일일 제한 체크
        if (state.dailyCount >= REFRESH_LIMITS.daily) {
            return { allowed: false, reason: 'daily_limit' };
        }
        
        // 시간당 제한 체크
        if (state.hourlyCount >= REFRESH_LIMITS.hourly) {
            return { allowed: false, reason: 'hourly_limit' };
        }
        
        return { allowed: true };
    };

    // 캐시된 추천 음악 로드 (새로고침 시에는 무시)
    const loadCachedRecommendations = (isRefresh: boolean = false) => {
        // 새로고침일 때는 캐시 무시하고 새로운 데이터 로드
        if (isRefresh) {
            localStorage.removeItem(STORAGE_KEYS.CACHED_RECS);
            return false;
        }
        
        const cached = localStorage.getItem(STORAGE_KEYS.CACHED_RECS);
        if (cached) {
            try {
                const { data, timestamp }: { data: Recommendation[], timestamp: number } = JSON.parse(cached);
                // 캐시 유효 시간을 10분으로 단축 (더 자주 새로운 추천)
                if (Date.now() - timestamp < 10 * 60 * 1000) {
                    setRecommendations(data);
                    return true;
                }
            } catch {
                localStorage.removeItem(STORAGE_KEYS.CACHED_RECS);
            }
        }
        return false;
    };

    // 추천 음악 캐시 저장
    const cacheRecommendations = (recs: Recommendation[]) => {
        const cacheData = {
            data: recs,
            timestamp: Date.now()
        };
        localStorage.setItem(STORAGE_KEYS.CACHED_RECS, JSON.stringify(cacheData));
    };

    const loadRecommendationsAsync = async (isRefresh: boolean = false) => {
        if (isRefresh) {
            setRefreshLoading(true);
        }
        
        try {
            // 사용자 ID 가져오기
            let userId = 1;
            try {
                const me = await api.get('/api/auth/me');
                if (me?.data?.authenticated && me?.data?.user?.id) {
                    userId = Number(me.data.user.id);
                }
            } catch {}

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

            // Progressive Loading - 5개씩 단계별 로딩
            const loadBatch = async (offset: number, limit: number) => {
                try {
                    const rec = await api.get(`/api/recommendations/user/${userId}?offset=${offset}&limit=${limit}`);
                    return Array.isArray(rec.data) ? rec.data : [];
                } catch {
                    return [];
                }
            };

            let allRecs: Recommendation[] = [];
            
            // 첫 번째 배치 (즉시 표시)
            const firstBatch = await loadBatch(0, 5);
            allRecs = [...firstBatch];
            
            if (allRecs.length > 0) {
                const filteredFirst = allRecs.filter(track => !currentLikedTracks.has(track.id));
                setRecommendations(prev => isRefresh ? filteredFirst : [...prev, ...filteredFirst]);
            }
            
            // 두 번째 배치 (지연 로딩)
            setTimeout(async () => {
                const secondBatch = await loadBatch(5, 5);
                if (secondBatch.length > 0) {
                    allRecs = [...allRecs, ...secondBatch];
                    const filteredSecond = secondBatch.filter(track => !currentLikedTracks.has(track.id));
                    setRecommendations(prev => [...prev, ...filteredSecond]);
                }
            }, 500);
            
            // 필터링 후 부족하면 FALLBACK_RECS에서 보충
            const filteredRecs = allRecs.filter(track => !currentLikedTracks.has(track.id));
            let finalRecs = filteredRecs.slice(0, 8);
            
            if (finalRecs.length < 4) {
                const fallbackFiltered = FALLBACK_RECS.filter(track => !currentLikedTracks.has(track.id));
                const needed = Math.max(4 - finalRecs.length, 0);
                finalRecs = [...finalRecs, ...fallbackFiltered.slice(0, needed)];
            }
            
            setRecommendations(finalRecs.slice(0, 8));
            
            // 캐시 저장
            if (finalRecs.length > 0) {
                cacheRecommendations(finalRecs);
            }
            
        } catch (error) {
            console.error('추천 음악 로딩 실패:', error);
            
            // Error Fallback - API 실패 시 인기 차트로 대체
            const fallbackFiltered = FALLBACK_RECS.filter(track => !likedTracks.has(track.id));
            setRecommendations(fallbackFiltered.slice(0, 4));
        } finally {
            if (isRefresh) {
                setRefreshLoading(false);
            }
        }
    };

    const handleRefresh = async () => {
        const currentState = refreshState;
        const refreshCheck = canRefresh(currentState);
        
        if (!refreshCheck.allowed) {
            let message = '';
            switch (refreshCheck.reason) {
                case 'cooldown':
                    message = `⏰ ${refreshCheck.waitTime}초 후 새로고침 가능해요!`;
                    break;
                case 'daily_limit':
                    message = `📅 오늘의 새로고침을 모두 사용했어요! (${REFRESH_LIMITS.daily}회)\n내일 새벽에 자동으로 새로운 추천이 준비될 예정이에요! 🎵`;
                    break;
                case 'hourly_limit':
                    message = `🕐 시간당 새로고침 한도를 초과했어요! (${REFRESH_LIMITS.hourly}회)\n1시간 후에 다시 시도해주세요!`;
                    break;
            }
            alert(message);
            setShowRefreshStatus(true);
            setTimeout(() => setShowRefreshStatus(false), 5000);
            return;
        }
        
        const now = Date.now();
        const newState: RefreshState = {
            ...currentState,
            dailyCount: currentState.dailyCount + 1,
            hourlyCount: currentState.hourlyCount + 1,
            lastRefresh: now,
            nextAllowed: now + (REFRESH_LIMITS.cooldown * 1000)
        };
        
        updateRefreshState(newState);
        // 새로고침 시 캐시 무시하고 새로운 데이터 로드
        await loadRecommendationsAsync(true);
    };

    const getRefreshButtonText = () => {
        const remaining = REFRESH_LIMITS.daily - refreshState.dailyCount;
        const hourlyRemaining = REFRESH_LIMITS.hourly - refreshState.hourlyCount;
        const nextAllowed = Math.max(0, Math.ceil((refreshState.nextAllowed - Date.now()) / 1000));
        
        if (nextAllowed > 0) {
            return `⏰ ${nextAllowed}초 후 새로고침 가능`;
        }
        
        if (remaining <= 0) {
            return '📅 오늘 새로고침 완료';
        }
        
        if (hourlyRemaining <= 0) {
            return '🕐 1시간 후 새로고침 가능';
        }
        
        return `🔄 새로고침 (오늘 ${remaining}/${REFRESH_LIMITS.daily}회 남음)`;
    };

    const loadDashboardData = useCallback(async () => {
        setLoading(true);
        try {
            // 1) 사용자 ID
            let userId = 1;
            try {
                const me = await api.get('/api/auth/me');
                if (me?.data?.authenticated && me?.data?.user?.id) {
                    userId = Number(me.data.user.id);
                }
            } catch {
                // ignore → fallback 1
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
            const hasCached = loadCachedRecommendations(false);
            
            if (!hasCached) {
                // 캐시가 없으면 비동기로 로드
                setTimeout(() => loadRecommendationsAsync(false), 100);
            }

            // 5) 통계 계산 (setState 비동기 고려 없이 로컬 sys 사용)
            const calculated: DashboardStats = {
                totalRecommendations: 150, // 예: 서버에서 내려주면 대체
                matchedUsers:
                    sys?.matchingSystem?.totalMatches ??
                    matchStatus?.totalMatches ??
                    23,
                favoriteGenres: ['K-Pop', 'Pop', 'Rock'],
                listeningTime: 847,
            };
            setStats(calculated);
        } catch (e) {
            // 전체 실패 폴백
            setSystemStatus(null);
            setStats({
                totalRecommendations: 150,
                matchedUsers: 23,
                favoriteGenres: ['K-Pop', 'Pop', 'Rock'],
                listeningTime: 847,
            });
            // 전체 실패 시에도 캐시 시도
            if (!loadCachedRecommendations(false)) {
                setRecommendations(FALLBACK_RECS.slice(0, 4));
            }
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        initializeRefreshState();
        loadDashboardData();
    }, [loadDashboardData]);
    
    // 실시간 쿨다운 타이머
    useEffect(() => {
        if (refreshState.nextAllowed > Date.now()) {
            const interval = setInterval(() => {
                setRefreshState(prev => ({ ...prev })); // 리렌더링 트리거
            }, 1000);
            
            return () => clearInterval(interval);
        }
    }, [refreshState.nextAllowed]);

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
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">총 추천곡</p>
                                    <p className="text-3xl font-bold text-white">{stats.totalRecommendations}</p>
                                </div>
                                <Music className="w-12 h-12 text-purple-400" />
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
                                    <p className="text-blue-200 text-sm font-medium">청취 시간</p>
                                    <p className="text-3xl font-bold text-white">{stats.listeningTime}분</p>
                                </div>
                                <Clock className="w-12 h-12 text-green-400" />
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
                                    canRefresh(refreshState).allowed 
                                        ? 'bg-purple-600 hover:bg-purple-700 text-white' 
                                        : 'bg-gray-600 text-gray-300 cursor-not-allowed'
                                }`}
                                onClick={handleRefresh}
                                disabled={!canRefresh(refreshState).allowed || refreshLoading}
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
                    
                    {/* 새로고침 정보 */}
                    <div className="mb-4 text-center">
                        <div className="inline-flex items-center space-x-4 bg-white/5 rounded-lg px-4 py-2 text-sm text-blue-200">
                            <span>📊 일일: {refreshState.dailyCount}/{REFRESH_LIMITS.daily}</span>
                            <span>⏱️ 시간당: {refreshState.hourlyCount}/{REFRESH_LIMITS.hourly}</span>
                            {refreshState.lastRefresh > 0 && (
                                <span>🕐 마지막: {new Date(refreshState.lastRefresh).toLocaleTimeString()}</span>
                            )}
                        </div>
                    </div>

                    {refreshLoading && (
                        <div className="mb-6 text-center">
                            <div className="inline-flex items-center space-x-2 bg-purple-500/20 text-purple-200 px-4 py-2 rounded-lg">
                                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-purple-200" />
                                <span>새로운 음악을 찾고 있어요...</span>
                            </div>
                        </div>
                    )}
                    
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
                </div>

                {/* 실시간 활동 */}
                <div className="mt-8 bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                    <h3 className="text-xl font-bold text-white mb-4">실시간 활동</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-center">
                        <div>
                            <p className="text-2xl font-bold text-blue-400">
                                {systemStatus?.matchingSystem?.queueCount ?? 25}
                            </p>
                            <p className="text-gray-400 text-sm">대기 중</p>
                        </div>
                        <div>
                            <p className="text-2xl font-bold text-green-400">
                                {systemStatus?.matchingSystem?.activeMatches ?? 12}
                            </p>
                            <p className="text-gray-400 text-sm">매칭 완료</p>
                        </div>
                        <div>
                            <p className="text-2xl font-bold text-purple-400">
                                {systemStatus?.chatSystem?.activeChatRooms ?? 87}
                            </p>
                            <p className="text-gray-400 text-sm">활성 채팅</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
