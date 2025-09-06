import React, { useState, useEffect } from 'react';
import { Search, Heart, Play, Music, Loader, Star } from 'lucide-react';
import { toast } from 'react-hot-toast';
import api from '../api/client';
import { validateMusicSearch } from '../utils/security';

interface SpotifyTrack {
    id: string;
    name: string;
    artists: Array<{ name: string }>;
    album: {
        name: string;
        images: Array<{ url: string; height: number; width: number }>;
    };
    preview_url: string | null;
    external_urls: {
        spotify: string;
    };
    // duration_ms: number; // 청취 기능 없으므로 제거
    popularity: number;
}

interface SearchResult {
    id: string;
    title: string;
    artist: string;
    album: string;
    image: string;
    previewUrl: string | null;
    spotifyUrl: string;
    // duration: number; // 청취 기능 없으므로 제거
    popularity: number;
    isLiked?: boolean;
}

const MusicDiscovery: React.FC = () => {
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [likedTracks, setLikedTracks] = useState<Set<string>>(new Set());

    const loadUserLikes = async () => {
        try {
            let userId: number | null = null;
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

            const response = await api.get(`/api/likes/user/${userId}`);
            const likes = response.data?.content || [];
            // 좋아요된 곡들을 title+artist 조합으로 Set 생성
            const likedSongs = likes.map((like: any) => {
                if (like.song) {
                    return `${like.song.title}::${like.song.artist}`.toLowerCase();
                }
                return null;
            }).filter(Boolean);
            setLikedTracks(new Set(likedSongs));
        } catch (error) {
            console.warn('좋아요 목록 불러오기 실패:', error);
        }
    };

    const toggleTrackLike = async (track: SearchResult) => {
        try {
            const trackKey = `${track.title}::${track.artist}`.toLowerCase();
            const isCurrentlyLiked = likedTracks.has(trackKey);

            if (isCurrentlyLiked) {
                // 이미 좋아요한 음악을 취소할 때 확인
                const shouldRemove = window.confirm(
                    `"${track.title} - ${track.artist}"를 좋아요 목록에서 제거하시겠습니까?`
                );
                
                if (!shouldRemove) {
                    return; // 사용자가 취소한 경우
                }

                // 좋아요 취소 처리 (title과 artist query parameter 추가)
                await api.delete(`/api/likes/song/${track.id}?title=${encodeURIComponent(track.title)}&artist=${encodeURIComponent(track.artist)}`);
                setLikedTracks(prev => {
                    const newSet = new Set(prev);
                    newSet.delete(trackKey);
                    return newSet;
                });
                
                // 취소 완료 메시지
                toast.success(
                    `"${track.title} - ${track.artist}"를 좋아요 목록에서 제거했습니다.`,
                    { 
                        duration: 2000,
                        icon: '💔'
                    }
                );
            } else {
                // 새로운 좋아요 추가
                const likeData = {
                    songId: track.id,
                    title: track.title,
                    artist: track.artist,
                    imageUrl: track.image,
                    externalId: track.id,
                    album: track.album,
                    previewUrl: track.previewUrl,
                    spotifyUrl: track.spotifyUrl
                };

                await api.post('/api/likes/song', likeData);
                setLikedTracks(prev => {
                    const newSet = new Set(prev);
                    newSet.add(trackKey);
                    return newSet;
                });
                
                // 성공 메시지
                toast.success(
                    `"${track.title} - ${track.artist}"를 좋아요 목록에 추가했습니다!`,
                    { 
                        duration: 2000,
                        icon: '✅'
                    }
                );
            }
        } catch (error) {
            console.error('좋아요 처리 실패:', error);
            toast.error('좋아요 처리 중 오류가 발생했습니다.');
        }
    };

    const searchMusic = async () => {
        if (!searchQuery.trim()) return;

        const validation = validateMusicSearch(searchQuery);
        if (!validation.isValid) {
            setError(validation.error || '검색어가 유효하지 않습니다.');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            // 검색 전에 최신 좋아요 목록을 다시 불러오기
            console.log('🔄 검색 전 좋아요 목록 새로고침...');
            
            // 좋아요 목록을 다시 불러오기 (최신 상태로 업데이트)
            let updatedLikedTracks = new Set<string>();
            try {
                let userId: number | null = null;
                try {
                    const me = await api.get('/api/auth/me');
                    if (me?.data?.authenticated && me?.data?.user?.id) {
                        userId = Number(me.data.user.id);
                    }
                } catch {}
                
                if (!userId) {
                    try {
                        const localMe = await api.get('/api/auth/local/me');
                        if (localMe?.data?.success && localMe?.data?.user?.id) {
                            userId = Number(localMe.data.user.id);
                        }
                    } catch {}
                }
                
                if (userId) {
                    const response = await api.get(`/api/likes/user/${userId}`);
                    const likes = response.data?.content || [];
                    // 좋아요된 곡들을 title+artist 조합으로 Set 생성
                    const likedSongs = likes.map((like: any) => {
                        if (like.song) {
                            return `${like.song.title}::${like.song.artist}`.toLowerCase();
                        }
                        return null;
                    }).filter(Boolean);
                    updatedLikedTracks = new Set(likedSongs);
                    setLikedTracks(updatedLikedTracks);
                    console.log('✅ 좋아요 목록 업데이트 완료:', Array.from(updatedLikedTracks));
                }
            } catch (error) {
                console.warn('좋아요 목록 새로고침 실패:', error);
            }
            
            const response = await api.get(`/api/spotify/search?q=${encodeURIComponent(validation.sanitized)}&type=track&limit=20`);
            
            if (response.data?.tracks?.items) {
                const results: SearchResult[] = response.data.tracks.items.map((track: SpotifyTrack) => ({
                    id: track.id,
                    title: track.name,
                    artist: track.artists.map(a => a.name).join(', '),
                    album: track.album.name,
                    image: track.album.images[0]?.url || '',
                    previewUrl: track.preview_url,
                    spotifyUrl: track.external_urls.spotify,
                    // duration: track.duration_ms, // 청취 기능 없으므로 제거
                    popularity: track.popularity,
                    isLiked: updatedLikedTracks.has(`${track.name}::${track.artists.map(a => a.name).join(', ')}`.toLowerCase()) // title+artist 조합으로 체크
                }));
                
                setSearchResults(results);
                
                // 검색 결과에서 이미 좋아요한 곡들이 있는지 콘솔에 표시 (디버깅용)
                const alreadyLiked = results.filter(track => updatedLikedTracks.has(`${track.title}::${track.artist}`.toLowerCase()));
                if (alreadyLiked.length > 0) {
                    console.log(`🔍 검색 결과 중 ${alreadyLiked.length}개 곡이 이미 좋아요 목록에 있습니다:`, 
                        alreadyLiked.map(t => `${t.title} - ${t.artist}`));
                    console.log('❤️ 현재 좋아요 목록:', Array.from(updatedLikedTracks));
                } else {
                    console.log('🔍 검색 결과 중 좋아요한 곡이 없습니다.');
                }
            } else {
                setSearchResults([]);
            }
        } catch (error) {
            console.error('음악 검색 실패:', error);
            setError('음악 검색 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const playOnYouTube = (track: SearchResult) => {
        const searchQuery = `${track.artist} ${track.title}`;
        const youtubeSearchUrl = `https://www.youtube.com/results?search_query=${encodeURIComponent(searchQuery)}`;
        window.open(youtubeSearchUrl, '_blank');
    };

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            searchMusic();
        }
    };

    useEffect(() => {
        loadUserLikes();
    }, []);

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900">
            <div className="container mx-auto px-4 py-8">
                {/* 헤더 */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-white mb-2 flex items-center justify-center space-x-3">
                        <Music className="w-10 h-10 text-purple-400" />
                        <span>음악 탐색</span>
                    </h1>
                    <p className="text-blue-200 text-lg">새로운 음악을 발견하고 좋아요를 표시하세요</p>
                </div>

                {/* 검색 영역 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20 mb-8">
                    <div className="flex space-x-4">
                        <div className="relative flex-1">
                            <Search className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                            <input
                                type="text"
                                placeholder="정확한 곡명을 입력해주세요..."
                                className="w-full pl-10 pr-4 py-3 bg-white/20 border border-white/30 rounded-lg text-white placeholder-gray-300 focus:outline-none focus:ring-2 focus:ring-purple-400"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                onKeyPress={handleKeyPress}
                            />
                        </div>
                        <button
                            onClick={searchMusic}
                            disabled={loading || !searchQuery.trim()}
                            className="bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 text-white px-6 py-3 rounded-lg transition-colors duration-200 flex items-center space-x-2"
                        >
                            {loading ? (
                                <Loader className="w-5 h-5 animate-spin" />
                            ) : (
                                <Search className="w-5 h-5" />
                            )}
                            <span>{loading ? '검색 중...' : '검색'}</span>
                        </button>
                    </div>

                    {error && (
                        <div className="mt-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-200">
                            {error}
                        </div>
                    )}
                </div>

                {/* 검색 결과 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-8 border border-white/20">
                    <h2 className="text-2xl font-bold text-white mb-6">검색 결과</h2>
                    
                    {searchResults.length === 0 && !loading && (
                        <div className="text-center py-16">
                            <Music className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                            <p className="text-gray-400 text-lg">검색 결과가 없습니다.</p>
                            <p className="text-gray-500 text-sm mt-2">다른 검색어로 시도해보세요.</p>
                        </div>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                        {searchResults.map((track) => (
                            <div
                                key={track.id}
                                className="bg-white/10 rounded-xl p-4 hover:bg-white/20 transition-all duration-300 group"
                            >
                                <div className="relative mb-4">
                                    <img
                                        src={track.image || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(track.title)}&backgroundColor=random`}
                                        alt={track.title}
                                        className="w-full h-48 object-cover rounded-lg"
                                        onError={(e) => {
                                            const img = e.currentTarget;
                                            img.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(track.title)}&backgroundColor=random`;
                                        }}
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
                                    <div className="absolute top-2 right-2 flex space-x-1">
                                        <div className="bg-green-500 text-white px-2 py-1 rounded-full text-xs font-bold flex items-center">
                                            <Star className="w-3 h-3 mr-1" />
                                            {track.popularity}
                                        </div>
                                    </div>
                                </div>

                                <h3 className="text-white font-bold text-lg mb-1 truncate" title={track.title}>
                                    {track.title}
                                </h3>
                                <p className="text-blue-200 text-sm mb-1 truncate" title={track.artist}>
                                    {track.artist}
                                </p>
                                <p className="text-gray-300 text-xs mb-3 truncate" title={track.album}>
                                    {track.album}
                                </p>

                                <div className="flex items-center justify-end">
                                    <div className="flex space-x-2">
                                        <button
                                            onClick={() => {
                                                const spotifyAppUrl = `spotify:track:${track.id}`;
                                                window.location.href = spotifyAppUrl;
                                            }}
                                            className="text-green-400 hover:text-green-300 transition-colors"
                                            title="Spotify 앱에서 듣기"
                                        >
                                            <Music className="w-5 h-5" />
                                        </button>
                                        {(() => {
                                            const trackKey = `${track.title}::${track.artist}`.toLowerCase();
                                            const isLiked = likedTracks.has(trackKey);
                                            return (
                                                <div title={isLiked ? "좋아요 취소 (클릭 시 확인 후 제거)" : "좋아요 추가"}>
                                                    <Heart
                                                        className={`w-5 h-5 cursor-pointer transition-colors ${
                                                            isLiked
                                                                ? 'text-red-500 fill-current'
                                                                : 'text-red-400 hover:text-red-300'
                                                        }`}
                                                        onClick={() => toggleTrackLike(track)}
                                                    />
                                                </div>
                                            );
                                        })()}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default MusicDiscovery;