// frontend/src/pages/Dashboard.tsx
import React, { useState, useEffect, useCallback } from 'react';
import { Music, Users, Heart, TrendingUp, Play, Clock, Star, Headphones } from 'lucide-react';
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
    const [likedTracks, setLikedTracks] = useState<Set<string | number>>(new Set()); // 좋아요한 트랙들

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

    // 사용자의 좋아요 목록 불러오기
    const loadUserLikes = async (userId: number) => {
        try {
            const response = await api.get(`/api/likes/user/${userId}`);
            const likes = response.data || [];
            const likedIds = likes.map((like: any) => like.songId || like.song?.id).filter(Boolean);
            setLikedTracks(new Set(likedIds));
        } catch (error) {
            console.warn('좋아요 목록 불러오기 실패:', error);
            // 에러가 나도 페이지는 정상 작동하도록
        }
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

            // 4) 사용자 좋아요 목록 먼저 불러오기 (추천 필터링을 위해)
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

            // 5) 추천 목록 (좋아요한 곡 제외)
            try {
                const rec = await api.get(`/api/recommendations/user/${userId}`);
                const allRecs = Array.isArray(rec.data) ? rec.data : FALLBACK_RECS;
                
                // 좋아요한 곡들 제외하고 필터링
                const filteredRecs = allRecs.filter(track => !currentLikedTracks.has(track.id));
                
                // 필터링 후 부족하면 FALLBACK_RECS에서 보충
                let finalRecs = filteredRecs.slice(0, 4);
                if (finalRecs.length < 4) {
                    const fallbackFiltered = FALLBACK_RECS.filter(track => !currentLikedTracks.has(track.id));
                    const needed = 4 - finalRecs.length;
                    finalRecs = [...finalRecs, ...fallbackFiltered.slice(0, needed)];
                }
                
                setRecommendations(finalRecs);
            } catch {
                // 에러 시에도 좋아요한 곡 제외
                const fallbackFiltered = FALLBACK_RECS.filter(track => !currentLikedTracks.has(track.id));
                setRecommendations(fallbackFiltered.slice(0, 4));
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
            setRecommendations(FALLBACK_RECS.slice(0, 1));
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
                        <button
                            className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors duration-200 flex items-center space-x-2"
                            onClick={loadDashboardData}
                        >
                            <TrendingUp className="w-4 h-4" />
                            <span>새로고침</span>
                        </button>
                    </div>

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
