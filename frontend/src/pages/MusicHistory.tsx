import React, { useState, useEffect } from 'react';
import { Music, Heart, Search, Calendar, User, ExternalLink, MessageSquare } from 'lucide-react';
import api from '../api/client';

interface MusicShare {
    id: number;
    trackName: string;
    artistName: string;
    sharedByName: string;
    sharedByUserId: number;
    spotifyUrl?: string;
    albumName?: string;
    albumImageUrl?: string;
    sharedAt: string;
    isLiked: boolean;
    notes?: string;
}

interface MusicHistoryResponse {
    success: boolean;
    message: string;
    data: {
        content: MusicShare[];
        totalElements: number;
        totalPages: number;
        size: number;
        number: number;
    };
}

const MusicHistory: React.FC = () => {
    const [musicHistory, setMusicHistory] = useState<MusicShare[]>([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [filter, setFilter] = useState<'all' | 'liked'>('all');

    // 음악 히스토리 로드
    const loadMusicHistory = async (page: number = 0, isLikedFilter: boolean = false) => {
        try {
            setLoading(true);
            let url = '/api/music-sharing-history';
            
            if (isLikedFilter) {
                url += '/liked';
            }
            
            const params = new URLSearchParams({
                page: page.toString(),
                size: '20'
            });
            
            const response = await api.get(`${url}?${params}`);
            const data: MusicHistoryResponse = response.data;
            
            if (data.success && data.data) {
                setMusicHistory(data.data.content);
                setTotalPages(data.data.totalPages);
                setCurrentPage(data.data.number);
            } else {
                setMusicHistory([]);
                setTotalPages(0);
            }
        } catch (error) {
            console.error('음악 히스토리 로드 실패:', error);
            setMusicHistory([]);
        } finally {
            setLoading(false);
        }
    };

    // 음악 좋아요 토글
    const toggleLike = async (historyId: number) => {
        try {
            const response = await api.post(`/api/music-sharing-history/${historyId}/like`);
            if (response.data.success) {
                // UI 업데이트
                setMusicHistory(prev => prev.map(item => 
                    item.id === historyId 
                        ? { ...item, isLiked: !item.isLiked }
                        : item
                ));
            }
        } catch (error) {
            console.error('좋아요 처리 실패:', error);
        }
    };

    // 메모 추가/수정
    const addNote = async (historyId: number, notes: string) => {
        try {
            const response = await api.post(`/api/music-sharing-history/${historyId}/note`, { notes });
            if (response.data.success) {
                setMusicHistory(prev => prev.map(item => 
                    item.id === historyId 
                        ? { ...item, notes }
                        : item
                ));
            }
        } catch (error) {
            console.error('메모 저장 실패:', error);
        }
    };

    // 필터 변경
    const handleFilterChange = (newFilter: 'all' | 'liked') => {
        setFilter(newFilter);
        loadMusicHistory(0, newFilter === 'liked');
    };

    // 페이지 변경
    const handlePageChange = (page: number) => {
        loadMusicHistory(page, filter === 'liked');
    };

    // 초기 로드
    useEffect(() => {
        loadMusicHistory();
    }, []);

    // 날짜 포맷팅
    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
            <div className="max-w-4xl mx-auto">
                {/* 헤더 */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-white mb-2 flex items-center gap-3">
                        <Music className="w-8 h-8 text-purple-400" />
                        음악 공유 히스토리
                    </h1>
                    <p className="text-gray-300">
                        매칭을 통해 받은 음악들을 모아보고 관리하세요
                    </p>
                </div>

                {/* 필터 및 검색 */}
                <div className="mb-6 space-y-4">
                    {/* 필터 탭 */}
                    <div className="flex gap-2">
                        <button
                            onClick={() => handleFilterChange('all')}
                            className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                                filter === 'all'
                                    ? 'bg-purple-600 text-white'
                                    : 'bg-white/10 text-gray-300 hover:bg-white/20'
                            }`}
                        >
                            전체
                        </button>
                        <button
                            onClick={() => handleFilterChange('liked')}
                            className={`px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2 ${
                                filter === 'liked'
                                    ? 'bg-purple-600 text-white'
                                    : 'bg-white/10 text-gray-300 hover:bg-white/20'
                            }`}
                        >
                            <Heart className="w-4 h-4" />
                            좋아요한 음악
                        </button>
                    </div>

                </div>

                {/* 로딩 */}
                {loading && (
                    <div className="text-center py-8">
                        <div className="inline-block w-8 h-8 border-4 border-purple-400 border-t-transparent rounded-full animate-spin"></div>
                        <p className="text-gray-300 mt-2">음악 히스토리를 불러오는 중...</p>
                    </div>
                )}

                {/* 음악 목록 */}
                {!loading && (
                    <div className="space-y-4">
                        {musicHistory.length === 0 ? (
                            <div className="text-center py-12">
                                <Music className="w-16 h-16 text-gray-500 mx-auto mb-4" />
                                <p className="text-gray-400 text-lg">
                                    {filter === 'liked' ? '좋아요한 음악이 없습니다' : '공유받은 음악이 없습니다'}
                                </p>
                                <p className="text-gray-500 mt-2">
                                    매칭을 통해 다른 사용자와 음악을 공유해보세요!
                                </p>
                            </div>
                        ) : (
                            musicHistory.map((music) => (
                                <div key={music.id} className="bg-white/10 backdrop-blur-md rounded-xl p-6 border border-white/20">
                                    <div className="flex items-start gap-4">
                                        {/* 앨범 이미지 */}
                                        {music.albumImageUrl ? (
                                            <img
                                                src={music.albumImageUrl}
                                                alt={`${music.albumName} 앨범 커버`}
                                                className="w-16 h-16 rounded-lg object-cover"
                                            />
                                        ) : (
                                            <div className="w-16 h-16 bg-gray-600 rounded-lg flex items-center justify-center">
                                                <Music className="w-8 h-8 text-gray-400" />
                                            </div>
                                        )}

                                        {/* 음악 정보 */}
                                        <div className="flex-1">
                                            <div className="flex items-start justify-between">
                                                <div>
                                                    <h3 className="text-lg font-semibold text-white mb-1">
                                                        {music.trackName}
                                                    </h3>
                                                    <p className="text-gray-300 mb-2">{music.artistName}</p>
                                                    {music.albumName && (
                                                        <p className="text-sm text-gray-400 mb-2">
                                                            앨범: {music.albumName}
                                                        </p>
                                                    )}
                                                </div>

                                                {/* 좋아요 버튼 */}
                                                <button
                                                    onClick={() => toggleLike(music.id)}
                                                    className={`p-2 rounded-lg transition-colors ${
                                                        music.isLiked
                                                            ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30'
                                                            : 'bg-white/10 text-gray-400 hover:bg-white/20 hover:text-red-400'
                                                    }`}
                                                >
                                                    <Heart className={`w-5 h-5 ${music.isLiked ? 'fill-current' : ''}`} />
                                                </button>
                                            </div>

                                            {/* 공유 정보 */}
                                            <div className="flex items-center gap-4 text-sm text-gray-400 mb-3">
                                                <div className="flex items-center gap-1">
                                                    <User className="w-4 h-4" />
                                                    <span>{music.sharedByName}님이 공유</span>
                                                </div>
                                                <div className="flex items-center gap-1">
                                                    <Calendar className="w-4 h-4" />
                                                    <span>{formatDate(music.sharedAt)}</span>
                                                </div>
                                            </div>

                                            {/* 스포티파이 링크 */}
                                            {music.spotifyUrl && (
                                                <div className="mb-3">
                                                    <a
                                                        href={music.spotifyUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="inline-flex items-center gap-2 px-3 py-1 bg-green-600/20 text-green-400 rounded-lg text-sm hover:bg-green-600/30 transition-colors"
                                                    >
                                                        <ExternalLink className="w-4 h-4" />
                                                        Spotify에서 듣기
                                                    </a>
                                                </div>
                                            )}

                                            {/* 메모 */}
                                            <div className="mt-3">
                                                <details className="group">
                                                    <summary className="flex items-center gap-2 text-sm text-gray-400 cursor-pointer hover:text-gray-300 transition-colors">
                                                        <MessageSquare className="w-4 h-4" />
                                                        <span>{music.notes ? '메모 보기/수정' : '메모 추가'}</span>
                                                    </summary>
                                                    <div className="mt-2 pl-6">
                                                        <textarea
                                                            defaultValue={music.notes || ''}
                                                            onBlur={(e) => {
                                                                const newNotes = e.target.value.trim();
                                                                if (newNotes !== music.notes) {
                                                                    addNote(music.id, newNotes);
                                                                }
                                                            }}
                                                            placeholder="이 음악에 대한 메모를 남겨보세요..."
                                                            className="w-full p-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm placeholder-gray-500 focus:outline-none focus:border-purple-400 resize-none"
                                                            rows={2}
                                                        />
                                                    </div>
                                                </details>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}

                        {/* 페이지네이션 */}
                        {totalPages > 1 && (
                            <div className="flex justify-center gap-2 mt-8">
                                <button
                                    onClick={() => handlePageChange(currentPage - 1)}
                                    disabled={currentPage === 0}
                                    className="px-4 py-2 bg-white/10 text-white rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-white/20 transition-colors"
                                >
                                    이전
                                </button>
                                
                                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                                    const pageNum = Math.max(0, Math.min(totalPages - 5, currentPage - 2)) + i;
                                    return (
                                        <button
                                            key={pageNum}
                                            onClick={() => handlePageChange(pageNum)}
                                            className={`px-4 py-2 rounded-lg transition-colors ${
                                                pageNum === currentPage
                                                    ? 'bg-purple-600 text-white'
                                                    : 'bg-white/10 text-white hover:bg-white/20'
                                            }`}
                                        >
                                            {pageNum + 1}
                                        </button>
                                    );
                                })}

                                <button
                                    onClick={() => handlePageChange(currentPage + 1)}
                                    disabled={currentPage >= totalPages - 1}
                                    className="px-4 py-2 bg-white/10 text-white rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-white/20 transition-colors"
                                >
                                    다음
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default MusicHistory;