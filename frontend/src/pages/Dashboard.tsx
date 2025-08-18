import React, { useState, useEffect, useCallback } from 'react';
import {
    Music,
    Users,
    Heart,
    TrendingUp,
    Play,
    Clock,
    Star,
    Headphones
} from 'lucide-react';
import axios from 'axios';

interface Recommendation {
    id: number;
    title: string;
    artist: string;
    image: string;
    genre: string;
    score: number;
}

interface DashboardStats {
    totalRecommendations: number;
    matchedUsers: number;
    favoriteGenres: string[];
    listeningTime: number;
}

const Dashboard: React.FC = () => {
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [systemStatus, setSystemStatus] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    // 대시보드 데이터 로딩
    const loadDashboardData = useCallback(async () => {
        try {
            setLoading(true);

            // 시스템 상태 조회
            const systemResponse = await axios.get('http://localhost:9090/api/realtime-matching/system-status');
            console.log('시스템 상태:', systemResponse.data);
            setSystemStatus(systemResponse.data);

            // 매칭 상태 조회 (대시보드 통계용)
            const matchingResponse = await axios.get('http://localhost:9090/api/realtime-matching/status/1');
            console.log('매칭 상태:', matchingResponse.data);

            // 추천 시스템 API 호출 시도 (있다면)
            try {
                const recommendationResponse = await axios.get('http://localhost:9090/api/recommendations/user/1');
                console.log('추천 데이터:', recommendationResponse.data);
                // 성공하면 실제 데이터 사용
                if (recommendationResponse.data && Array.isArray(recommendationResponse.data)) {
                    setRecommendations(recommendationResponse.data.slice(0, 4));
                } else {
                    throw new Error('추천 API 형식 불일치');
                }
            } catch (recError) {
                console.log('추천 API 없음, 모의 데이터 사용');
                // 추천 API가 없으면 모의 데이터 사용
                setRecommendations([
                    {
                        id: 1,
                        title: "Spring Day",
                        artist: "BTS",
                        image: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop",
                        genre: "K-Pop",
                        score: 95
                    },
                    {
                        id: 2,
                        title: "Blinding Lights",
                        artist: "The Weeknd",
                        image: "https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=300&h=300&fit=crop",
                        genre: "Pop",
                        score: 89
                    },
                    {
                        id: 3,
                        title: "Good 4 U",
                        artist: "Olivia Rodrigo",
                        image: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop",
                        genre: "Pop Rock",
                        score: 87
                    },
                    {
                        id: 4,
                        title: "Levitating",
                        artist: "Dua Lipa",
                        image: "https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=300&h=300&fit=crop",
                        genre: "Dance Pop",
                        score: 92
                    }
                ]);
            }

            // 대시보드 통계 계산 (시스템 상태 기반)
            const calculatedStats: DashboardStats = {
                totalRecommendations: 150,
                matchedUsers: systemStatus?.matchingSystem?.totalMatches || 23,
                favoriteGenres: ["K-Pop", "Pop", "Rock"],
                listeningTime: 847
            };

            setStats(calculatedStats);

        } catch (error) {
            console.error('대시보드 데이터 로딩 실패:', error);

            // 에러 시 기본 데이터 설정
            setStats({
                totalRecommendations: 150,
                matchedUsers: 23,
                favoriteGenres: ["K-Pop", "Pop", "Rock"],
                listeningTime: 847
            });

            setRecommendations([
                {
                    id: 1,
                    title: "Spring Day",
                    artist: "BTS",
                    image: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop",
                    genre: "K-Pop",
                    score: 95
                }
            ]);
        } finally {
            setLoading(false);
        }
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        loadDashboardData();
    }, [loadDashboardData]);

    if (loading) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-white mx-auto mb-4"></div>
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
                    <h1 className="text-4xl font-bold text-white mb-2">
                        음악 추천 대시보드
                    </h1>
                    <p className="text-blue-200 text-lg">
                        당신만을 위한 맞춤 음악을 발견하세요
                    </p>
                </div>

                {/* 시스템 상태 표시 */}
                {systemStatus && (
                    <div className="mb-6 bg-white/10 backdrop-blur-lg rounded-2xl p-4 border border-white/20">
                        <div className="flex items-center justify-between">
                            <div className="text-white">
                                <h3 className="font-bold">시스템 상태</h3>
                                <p className="text-sm text-blue-200">{systemStatus.message}</p>
                            </div>
                            <div className="flex items-center space-x-2">
                                <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
                                <span className="text-green-400 text-sm">온라인</span>
                            </div>
                        </div>
                    </div>
                )}

                {/* 통계 카드들 */}
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

                {/* 추천 음악 섹션 */}
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
                            >
                                <div className="relative mb-4">
                                    <img
                                        src={track.image}
                                        alt={track.title}
                                        className="w-full h-48 object-cover rounded-lg"
                                    />
                                    <div className="absolute inset-0 bg-black/50 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                                        <Play className="w-12 h-12 text-white" />
                                    </div>
                                    <div className="absolute top-2 right-2 bg-green-500 text-white px-2 py-1 rounded-full text-xs font-bold">
                                        {track.score}%
                                    </div>
                                </div>

                                <h3 className="text-white font-bold text-lg mb-1 truncate">
                                    {track.title}
                                </h3>
                                <p className="text-blue-200 text-sm mb-2 truncate">
                                    {track.artist}
                                </p>
                                <div className="flex items-center justify-between">
                  <span className="bg-purple-600 text-white px-2 py-1 rounded-full text-xs">
                    {track.genre}
                  </span>
                                    <Heart className="w-5 h-5 text-red-400 hover:text-red-300 cursor-pointer" />
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
                                {systemStatus?.matchingSystem?.queueCount || 25}
                            </p>
                            <p className="text-gray-400 text-sm">대기 중</p>
                        </div>
                        <div>
                            <p className="text-2xl font-bold text-green-400">
                                {systemStatus?.matchingSystem?.activeMatches || 12}
                            </p>
                            <p className="text-gray-400 text-sm">매칭 완료</p>
                        </div>
                        <div>
                            <p className="text-2xl font-bold text-purple-400">
                                {systemStatus?.chatSystem?.activeChatRooms || 87}
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