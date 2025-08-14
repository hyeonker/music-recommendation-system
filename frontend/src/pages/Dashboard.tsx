import React, { useState, useEffect } from 'react';
import { Music, Heart, Play, Shuffle, TrendingUp, Users, MessageCircle } from 'lucide-react';
import axios from 'axios';
import { toast } from 'react-hot-toast';

const Dashboard = () => {
    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState({
        totalSongs: 0,
        totalUsers: 0,
        activeMatches: 0
    });

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        try {
            setLoading(true);

            // 추천곡 데이터 로드
            const recResponse = await axios.get('/api/enhanced-recommendations/trending?limit=6');
            if (recResponse.data.success) {
                setRecommendations(recResponse.data.tracks || []);
            }

            // 시스템 통계 로드
            const statsResponse = await axios.get('/api/simple-stats/overview');
            if (statsResponse.data.success) {
                setStats(statsResponse.data.stats || stats);
            }

        } catch (error) {
            console.error('대시보드 데이터 로드 에러:', error);
            toast.error('데이터를 불러오는 중 오류가 발생했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handlePlayMusic = (track: any) => {
        toast.success(`🎵 ${track.name || '음악'} 재생!`, {
            icon: '🎶',
        });
    };

    const handleLikeMusic = (track: any) => {
        toast.success(`💖 ${track.name || '음악'}을 좋아요!`, {
            icon: '❤️',
        });
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-96">
                <div className="loading-spinner"></div>
            </div>
        );
    }

    return (
        <div className="space-y-8 animate-fade-in">
            {/* 헤더 섹션 */}
            <div className="text-center space-y-4">
                <h1 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-blue-400 via-purple-500 to-pink-500 bg-clip-text text-transparent">
                    🎵 Music Dashboard
                </h1>
                <p className="text-lg text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">
                    당신을 위한 완벽한 음악 추천과 새로운 사람들과의 만남이 기다립니다
                </p>
            </div>

            {/* 통계 카드들 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="glass-card p-6 text-center">
                    <div className="flex items-center justify-center w-12 h-12 bg-blue-500/20 rounded-full mx-auto mb-4">
                        <Music className="h-6 w-6 text-blue-500" />
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800 dark:text-white">{stats.totalSongs.toLocaleString()}</h3>
                    <p className="text-gray-600 dark:text-gray-400">보유 음악</p>
                </div>

                <div className="glass-card p-6 text-center">
                    <div className="flex items-center justify-center w-12 h-12 bg-purple-500/20 rounded-full mx-auto mb-4">
                        <Users className="h-6 w-6 text-purple-500" />
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800 dark:text-white">{stats.totalUsers.toLocaleString()}</h3>
                    <p className="text-gray-600 dark:text-gray-400">활성 사용자</p>
                </div>

                <div className="glass-card p-6 text-center">
                    <div className="flex items-center justify-center w-12 h-12 bg-pink-500/20 rounded-full mx-auto mb-4">
                        <MessageCircle className="h-6 w-6 text-pink-500" />
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800 dark:text-white">{stats.activeMatches.toLocaleString()}</h3>
                    <p className="text-gray-600 dark:text-gray-400">활성 매치</p>
                </div>
            </div>

            {/* 빠른 액션 버튼들 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="glass-card p-8 text-center group hover:scale-105 transition-transform duration-300">
                    <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full mx-auto mb-6 group-hover:animate-pulse">
                        <Users className="h-8 w-8 text-white" />
                    </div>
                    <h3 className="text-xl font-bold text-gray-800 dark:text-white mb-2">음악 매칭 시작</h3>
                    <p className="text-gray-600 dark:text-gray-400 mb-6">
                        비슷한 음악 취향을 가진 사람들과 만나보세요
                    </p>
                    <button
                        className="btn-primary w-full"
                        onClick={() => window.location.href = '/matching'}
                    >
                        매칭 시작하기
                    </button>
                </div>

                <div className="glass-card p-8 text-center group hover:scale-105 transition-transform duration-300">
                    <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-r from-pink-500 to-red-500 rounded-full mx-auto mb-6 group-hover:animate-pulse">
                        <MessageCircle className="h-8 w-8 text-white" />
                    </div>
                    <h3 className="text-xl font-bold text-gray-800 dark:text-white mb-2">채팅 참여</h3>
                    <p className="text-gray-600 dark:text-gray-400 mb-6">
                        매칭된 사람들과 음악 이야기를 나눠보세요
                    </p>
                    <button
                        className="btn-primary w-full"
                        onClick={() => window.location.href = '/chat'}
                    >
                        채팅하기
                    </button>
                </div>
            </div>

            {/* 추천 음악 섹션 */}
            <div className="space-y-6">
                <div className="flex items-center justify-between">
                    <h2 className="text-2xl font-bold text-gray-800 dark:text-white flex items-center">
                        <TrendingUp className="h-6 w-6 mr-2 text-blue-500" />
                        실시간 트렌딩 음악
                    </h2>
                    <button
                        className="btn-secondary"
                        onClick={loadDashboardData}
                    >
                        <Shuffle className="h-4 w-4 mr-2" />
                        새로고침
                    </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {recommendations.length > 0 ? (
                        recommendations.slice(0, 6).map((track: any, index) => (
                            <div key={index} className="music-card music-card-hover">
                                <div className="flex items-start space-x-4">
                                    <div className="w-16 h-16 bg-gradient-to-br from-blue-400 to-purple-600 rounded-lg flex items-center justify-center text-white font-bold text-xl">
                                        {index + 1}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <h3 className="font-semibold text-gray-800 dark:text-white truncate">
                                            {track.name || `추천곡 ${index + 1}`}
                                        </h3>
                                        <p className="text-gray-600 dark:text-gray-400 text-sm truncate">
                                            {track.artists?.[0]?.name || '다양한 아티스트'}
                                        </p>
                                        <p className="text-gray-500 dark:text-gray-500 text-xs truncate">
                                            {track.album?.name || '인기 앨범'}
                                        </p>
                                    </div>
                                </div>

                                <div className="mt-4 flex items-center justify-between">
                                    <button
                                        className="flex items-center space-x-2 text-blue-500 hover:text-blue-600 transition-colors"
                                        onClick={() => handlePlayMusic(track)}
                                    >
                                        <Play className="h-4 w-4" />
                                        <span className="text-sm">재생</span>
                                    </button>

                                    <button
                                        className="flex items-center space-x-2 text-pink-500 hover:text-pink-600 transition-colors"
                                        onClick={() => handleLikeMusic(track)}
                                    >
                                        <Heart className="h-4 w-4" />
                                        <span className="text-sm">좋아요</span>
                                    </button>
                                </div>
                            </div>
                        ))
                    ) : (
                        // 플레이스홀더 카드들
                        Array.from({ length: 6 }).map((_, index) => (
                            <div key={index} className="music-card">
                                <div className="flex items-start space-x-4">
                                    <div className="w-16 h-16 bg-gradient-to-br from-blue-400 to-purple-600 rounded-lg flex items-center justify-center text-white font-bold text-xl">
                                        {index + 1}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <h3 className="font-semibold text-gray-800 dark:text-white">
                                            추천곡 {index + 1}
                                        </h3>
                                        <p className="text-gray-600 dark:text-gray-400 text-sm">
                                            인기 아티스트
                                        </p>
                                        <p className="text-gray-500 dark:text-gray-500 text-xs">
                                            베스트 앨범
                                        </p>
                                    </div>
                                </div>

                                <div className="mt-4 flex items-center justify-between">
                                    <button
                                        className="flex items-center space-x-2 text-blue-500 hover:text-blue-600 transition-colors"
                                        onClick={() => handlePlayMusic({ name: `추천곡 ${index + 1}` })}
                                    >
                                        <Play className="h-4 w-4" />
                                        <span className="text-sm">재생</span>
                                    </button>

                                    <button
                                        className="flex items-center space-x-2 text-pink-500 hover:text-pink-600 transition-colors"
                                        onClick={() => handleLikeMusic({ name: `추천곡 ${index + 1}` })}
                                    >
                                        <Heart className="h-4 w-4" />
                                        <span className="text-sm">좋아요</span>
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* 하단 추가 정보 */}
            <div className="glass-card p-8 text-center">
                <h3 className="text-xl font-bold text-gray-800 dark:text-white mb-4">
                    🎯 더 많은 기능들
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-sm text-gray-600 dark:text-gray-400">
                    <div>
                        <strong className="text-blue-500">스마트 매칭:</strong> AI가 분석한 음악 취향으로 완벽한 파트너 찾기
                    </div>
                    <div>
                        <strong className="text-purple-500">실시간 채팅:</strong> 매칭된 사람과 즉시 음악 이야기 나누기
                    </div>
                    <div>
                        <strong className="text-pink-500">음악 공유:</strong> Spotify 연동으로 실제 음악 추천하기
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;