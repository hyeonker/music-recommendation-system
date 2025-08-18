import React, { useState, useEffect } from 'react';
import { Save, Music, Search, X, Plus, Star, Heart, BarChart3 } from 'lucide-react';
import axios from 'axios';
import { toast } from 'react-hot-toast';
import SpotifyService from '../services/SpotifyService';

interface Artist {
    id: string;
    name: string;
    image?: string;
    followers?: number;
    genres?: string[];
    popularity?: number;
    spotifyId?: string;
    spotifyUrl?: string;
}

interface Genre {
    id: string;
    name: string;
    description?: string;
}

interface UserProfile {
    id: number;
    name: string;
    email?: string;
    favoriteArtists: Artist[];
    favoriteGenres: Genre[];
    attendedFestivals: string[];
    musicPreferences: {
        preferredEra: string;
        moodPreferences: string[];
        listeningFrequency: string;
        concertFrequency: string;
        discoveryMethod: string;
    };
}

// 아티스트 이미지 서비스
class ArtistImageService {
    private static knownArtists: Record<string, string> = {
        // K-Pop
        'bts': 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&h=300&fit=crop',
        '아이유': 'https://images.unsplash.com/photo-1494790108755-2616c27b5518?w=300&h=300&fit=crop',
        'blackpink': 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300&h=300&fit=crop',
        'twice': 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=300&h=300&fit=crop',
        // 팝
        'taylor swift': 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop',
        'ariana grande': 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=300&h=300&fit=crop',
        'ed sheeran': 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&h=300&fit=crop',
        'billie eilish': 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&h=300&fit=crop',
        // 록/메탈
        'queen': 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop',
        'the beatles': 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&h=300&fit=crop',
        'coldplay': 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&h=300&fit=crop',
    };

    static getArtistImage(artistName: string): string {
        const normalizedName = artistName.toLowerCase().trim();

        // 알려진 아티스트 확인
        if (this.knownArtists[normalizedName]) {
            return this.knownArtists[normalizedName];
        }

        // 이름 기반 해시로 일관된 이미지 생성
        const hash = this.simpleHash(artistName);
        const colors = ['bg-red-500', 'bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-pink-500', 'bg-yellow-500'];
        const colorIndex = hash % colors.length;
        console.log(`Color index for ${artistName}: ${colorIndex}`); // 사용하도록 로그 추가

        // 텍스트 기반 아바타 URL 생성 (UI Avatars 서비스 사용)
        const initials = artistName.split(' ').map(word => word[0]).join('').slice(0, 2).toUpperCase();
        return `https://ui-avatars.com/api/?name=${initials}&background=random&color=fff&size=300`;
    }

    private static simpleHash(str: string): number {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return Math.abs(hash);
    }
}

const Profile: React.FC = () => {
    const [profile, setProfile] = useState<UserProfile>({
        id: 1,
        name: '뮤직러버',
        favoriteArtists: [],
        favoriteGenres: [],
        attendedFestivals: [],
        musicPreferences: {
            preferredEra: '',
            moodPreferences: [],
            listeningFrequency: '',
            concertFrequency: '',
            discoveryMethod: ''
        }
    });

    const [artistSearchQuery, setArtistSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<Artist[]>([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [spotifyService] = useState(() => SpotifyService.getInstance());

    // 사용 가능한 장르 목록 (확장)
    const availableGenres: Genre[] = [
        { id: 'kpop', name: 'K-Pop', description: '한국 대중음악' },
        { id: 'pop', name: 'Pop', description: '팝 음악' },
        { id: 'rock', name: 'Rock', description: '록 음악' },
        { id: 'alternative', name: 'Alternative', description: '얼터너티브 록' },
        { id: 'indie', name: 'Indie', description: '인디 음악' },
        { id: 'hiphop', name: 'Hip-Hop', description: '힙합' },
        { id: 'rap', name: 'Rap', description: '랩' },
        { id: 'rnb', name: 'R&B', description: '알앤비' },
        { id: 'soul', name: 'Soul', description: '소울' },
        { id: 'funk', name: 'Funk', description: '펑크' },
        { id: 'jazz', name: 'Jazz', description: '재즈' },
        { id: 'blues', name: 'Blues', description: '블루스' },
        { id: 'classical', name: 'Classical', description: '클래식' },
        { id: 'electronic', name: 'Electronic', description: '일렉트로닉' },
        { id: 'edm', name: 'EDM', description: '일렉트로닉 댄스 뮤직' },
        { id: 'house', name: 'House', description: '하우스' },
        { id: 'techno', name: 'Techno', description: '테크노' },
        { id: 'trance', name: 'Trance', description: '트랜스' },
        { id: 'dubstep', name: 'Dubstep', description: '덥스텝' },
        { id: 'trap', name: 'Trap', description: '트랩' },
        { id: 'reggae', name: 'Reggae', description: '레게' },
        { id: 'ska', name: 'Ska', description: '스카' },
        { id: 'punk', name: 'Punk', description: '펑크 록' },
        { id: 'metal', name: 'Metal', description: '메탈' },
        { id: 'hardcore', name: 'Hardcore', description: '하드코어' },
        { id: 'psychedelic', name: 'Psychedelic', description: '사이키델릭' },
        { id: 'ambient', name: 'Ambient', description: '앰비언트' },
        { id: 'synthwave', name: 'Synthwave', description: '신스웨이브' },
        { id: 'vaporwave', name: 'Vaporwave', description: '베이퍼웨이브' },
        { id: 'lofi', name: 'Lo-Fi', description: '로파이' },
        { id: 'folk', name: 'Folk', description: '포크' },
        { id: 'country', name: 'Country', description: '컨트리' },
        { id: 'bluegrass', name: 'Bluegrass', description: '블루그래스' },
        { id: 'world', name: 'World Music', description: '월드 뮤직' },
        { id: 'latin', name: 'Latin', description: '라틴' },
        { id: 'afrobeat', name: 'Afrobeat', description: '아프로비트' },
        { id: 'reggaeton', name: 'Reggaeton', description: '레게톤' },
        { id: 'gospel', name: 'Gospel', description: '가스펠' },
        { id: 'newage', name: 'New Age', description: '뉴에이지' },
        { id: 'soundtrack', name: 'Soundtrack', description: '사운드트랙' }
    ];

    // 주요 음악 페스티벌 목록
    const musicFestivals = [
        // 해외 메이저 페스티벌
        { id: 'coachella', name: 'Coachella', location: '미국 캘리포니아', type: 'Multi-genre' },
        { id: 'glastonbury', name: 'Glastonbury', location: '영국', type: 'Rock/Pop' },
        { id: 'lollapalooza', name: 'Lollapalooza', location: '미국 시카고', type: 'Multi-genre' },
        { id: 'bonnaroo', name: 'Bonnaroo', location: '미국 테네시', type: 'Multi-genre' },
        { id: 'burning_man', name: 'Burning Man', location: '미국 네바다', type: 'Electronic/Art' },
        { id: 'tomorrowland', name: 'Tomorrowland', location: '벨기에', type: 'Electronic' },
        { id: 'ultra', name: 'Ultra Music Festival', location: '미국 마이애미', type: 'Electronic' },
        { id: 'edc', name: 'Electric Daisy Carnival', location: '미국 라스베가스', type: 'Electronic' },
        { id: 'reading_leeds', name: 'Reading & Leeds', location: '영국', type: 'Rock/Alternative' },
        { id: 'download', name: 'Download Festival', location: '영국', type: 'Rock/Metal' },

        // 일본 페스티벌
        { id: 'fuji_rock', name: '후지 락 페스티벌', location: '일본 니가타', type: 'Rock/Multi-genre' },
        { id: 'summer_sonic', name: '섬머소닉', location: '일본 도쿄/오사카', type: 'Multi-genre' },
        { id: 'rock_in_japan', name: 'Rock in Japan', location: '일본 이바라키', type: 'Rock/J-Pop' },

        // 한국 페스티벌
        { id: 'pentaport', name: '인천 펜타포트 락페스티벌', location: '인천', type: 'Rock/Multi-genre' },
        { id: 'grand_mint', name: '그랜드 민트 페스티벌', location: '서울', type: 'Indie/Folk' },
        { id: 'waterbomb', name: '워터밤', location: '서울', type: 'Hip-Hop/Electronic' },
        { id: 'dmzpeace', name: 'DMZ 피스 트레인 뮤직 페스티벌', location: '철원', type: 'Rock/Electronic/Indie' },
        { id: 'kb_rapbeat', name: 'KB 랩비트 페스티벌', location: '서울', type: 'Hip-Hop' },

        // 기타 아시아
        { id: 'wonderfruit', name: 'Wonderfruit', location: '태국', type: 'Multi-genre/Art' },
        { id: 'djakarta_warehouse', name: 'Djakarta Warehouse Project', location: '인도네시아', type: 'Electronic' },
        { id: 'clockenflap', name: 'Clockenflap', location: '홍콩', type: 'Multi-genre' },

        // 유럽 기타
        { id: 'primavera', name: 'Primavera Sound', location: '스페인 바르셀로나', type: 'Indie/Alternative' },
        { id: 'roskilde', name: 'Roskilde Festival', location: '덴마크', type: 'Multi-genre' },
        { id: 'exit', name: 'EXIT Festival', location: '세르비아', type: 'Electronic/Rock' },
        { id: 'sziget', name: 'Sziget Festival', location: '헝가리 부다페스트', type: 'Multi-genre' }
    ];

    // 무드 선택지
    const moodOptions = [
        { value: 'energetic', label: '에너지틱' },
        { value: 'calm', label: '차분한' },
        { value: 'happy', label: '즐거운' },
        { value: 'melancholy', label: '감상적인' },
        { value: 'romantic', label: '로맨틱' },
        { value: 'focus', label: '집중' },
        { value: 'workout', label: '운동' },
        { value: 'party', label: '파티' }
    ];

    // 콘서트/페스티벌 참여 빈도
    const concertFrequencyOptions = [
        { value: 'monthly', label: '월 1회 이상' },
        { value: 'few_months', label: '몇 달에 1회' },
        { value: 'yearly', label: '연 1-2회' },
        { value: 'rarely', label: '가끔' },
        { value: 'never', label: '거의 안 감' }
    ];

    // 음악 시대 선호도
    const eraOptions = [
        { value: '2020s', label: '2020년대' },
        { value: '2010s', label: '2010년대' },
        { value: '2000s', label: '2000년대' },
        { value: '90s', label: '90년대' },
        { value: '80s', label: '80년대' },
        { value: '70s', label: '70년대' },
        { value: 'classic', label: '클래식' },
        { value: 'mixed', label: '다양함' }
    ];

    // 음악 청취 빈도
    const listeningFrequencyOptions = [
        { value: 'daily', label: '매일' },
        { value: 'few_times_week', label: '주 2-3회' },
        { value: 'weekly', label: '주 1회' },
        { value: 'occasionally', label: '가끔' }
    ];

    // 음악 발견 방법
    const discoveryMethodOptions = [
        { value: 'streaming', label: '스트리밍 추천' },
        { value: 'friends', label: '친구 추천' },
        { value: 'social_media', label: '소셜미디어' },
        { value: 'radio', label: '라디오' },
        { value: 'live_shows', label: '라이브 공연' },
        { value: 'music_blogs', label: '음악 블로그' }
    ];

    // 아티스트 검색 (개선된 이미지 지원)
    const searchArtists = async (query: string) => {
        if (!query.trim()) {
            setSearchResults([]);
            return;
        }

        try {
            setIsSearching(true);

            // 백엔드 아티스트 검색 API 시도
            const response = await axios.get(`http://localhost:9090/api/artists/search?q=${encodeURIComponent(query)}`);

            if (response.data && response.data.length > 0) {
                // 백엔드에서 데이터를 받은 경우
                const artistsWithImages = response.data.map((artist: any) => ({
                    ...artist,
                    image: artist.image || ArtistImageService.getArtistImage(artist.name)
                }));
                setSearchResults(artistsWithImages);
            } else {
                // 백엔드에 데이터가 없으면 더미 데이터 생성
                const dummyArtists: Artist[] = [
                    {
                        id: `search_${Date.now()}`,
                        name: query,
                        image: ArtistImageService.getArtistImage(query),
                        followers: Math.floor(Math.random() * 1000000),
                        genres: ['검색결과']
                    }
                ];
                setSearchResults(dummyArtists);
            }
        } catch (error) {
            // API 실패 시 더미 데이터로 대체
            const dummyArtists: Artist[] = [
                {
                    id: `search_${Date.now()}`,
                    name: query,
                    image: ArtistImageService.getArtistImage(query),
                    followers: Math.floor(Math.random() * 1000000),
                    genres: ['검색결과']
                }
            ];
            setSearchResults(dummyArtists);
        } finally {
            setIsSearching(false);
        }
    };

    // 아티스트 추가
    const addArtist = (artist: Artist) => {
        if (profile.favoriteArtists.find(a => a.id === artist.id)) {
            toast.error('이미 추가된 아티스트입니다');
            return;
        }

        setProfile(prev => ({
            ...prev,
            favoriteArtists: [...prev.favoriteArtists, artist]
        }));

        setArtistSearchQuery('');
        setSearchResults([]);
        toast.success(`${artist.name}이(가) 추가되었습니다`);
    };

    // 아티스트 제거
    const removeArtist = (artistId: string) => {
        setProfile(prev => ({
            ...prev,
            favoriteArtists: prev.favoriteArtists.filter(a => a.id !== artistId)
        }));
    };

    // 장르 토글
    const toggleGenre = (genre: Genre) => {
        setProfile(prev => {
            const exists = prev.favoriteGenres.find(g => g.id === genre.id);

            if (exists) {
                return {
                    ...prev,
                    favoriteGenres: prev.favoriteGenres.filter(g => g.id !== genre.id)
                };
            } else {
                return {
                    ...prev,
                    favoriteGenres: [...prev.favoriteGenres, genre]
                };
            }
        });
    };

    // 페스티벌 토글
    const toggleFestival = (festivalId: string) => {
        setProfile(prev => {
            const exists = prev.attendedFestivals.includes(festivalId);

            if (exists) {
                return {
                    ...prev,
                    attendedFestivals: prev.attendedFestivals.filter(id => id !== festivalId)
                };
            } else {
                return {
                    ...prev,
                    attendedFestivals: [...prev.attendedFestivals, festivalId]
                };
            }
        });
    };

    // 무드 토글
    const toggleMood = (mood: string) => {
        setProfile(prev => ({
            ...prev,
            musicPreferences: {
                ...prev.musicPreferences,
                moodPreferences: prev.musicPreferences.moodPreferences.includes(mood)
                    ? prev.musicPreferences.moodPreferences.filter(m => m !== mood)
                    : [...prev.musicPreferences.moodPreferences, mood]
            }
        }));
    };

    // 프로필 저장
    const saveProfile = async () => {
        try {
            setIsSaving(true);

            // 백엔드 API 호출
            await axios.put(`http://localhost:9090/api/users/${profile.id}/profile`, profile);

            toast.success('프로필이 저장되었습니다!');
        } catch (error) {
            console.error('프로필 저장 실패:', error);
            toast.error('프로필 저장에 실패했습니다');
        } finally {
            setIsSaving(false);
        }
    };

    // 아티스트 검색 디바운싱
    useEffect(() => {
        const timer = setTimeout(() => {
            if (artistSearchQuery) {
                searchArtists(artistSearchQuery);
            }
        }, 500);

        return () => clearTimeout(timer);
    }, [artistSearchQuery]);

    // 프로필 로드
    useEffect(() => {
        const loadProfile = async () => {
            try {
                const response = await axios.get(`http://localhost:9090/api/users/${profile.id}/profile`);
                if (response.data) {
                    setProfile(response.data);
                }
            } catch (error) {
                console.log('프로필 로드 실패 (더미 데이터 사용):', error);
            }
        };

        loadProfile();
    }, []); // profile.id 의존성 제거

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 p-6">
            <div className="max-w-4xl mx-auto">
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-white mb-2">음악 프로필</h1>
                    <p className="text-blue-200">당신의 음악 취향을 알려주세요</p>
                </div>

                {/* 기본 정보 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-4">기본 정보</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-blue-200 text-sm mb-2">이름</label>
                            <input
                                type="text"
                                value={profile.name}
                                onChange={(e) => setProfile(prev => ({ ...prev, name: e.target.value }))}
                                className="w-full px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="닉네임을 입력하세요"
                            />
                        </div>
                        <div>
                            <label className="block text-blue-200 text-sm mb-2">이메일</label>
                            <input
                                type="email"
                                value={profile.email || ''}
                                onChange={(e) => setProfile(prev => ({ ...prev, email: e.target.value }))}
                                className="w-full px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="이메일을 입력하세요"
                            />
                        </div>
                    </div>
                </div>

                {/* 좋아하는 아티스트 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-4">
                        좋아하는 아티스트 ({profile.favoriteArtists.length})
                    </h2>

                    {/* 아티스트 검색 */}
                    <div className="relative mb-4">
                        <div className="flex items-center">
                            <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                            <input
                                type="text"
                                value={artistSearchQuery}
                                onChange={(e) => setArtistSearchQuery(e.target.value)}
                                className="w-full pl-10 pr-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="아티스트를 검색하세요..."
                            />
                        </div>

                        {/* 검색 결과 */}
                        {searchResults.length > 0 && (
                            <div className="absolute top-full left-0 right-0 mt-1 bg-white/95 backdrop-blur-lg rounded-lg shadow-lg border border-white/30 z-10">
                                {searchResults.map((artist) => (
                                    <div
                                        key={artist.id}
                                        onClick={() => addArtist(artist)}
                                        className="flex items-center p-3 hover:bg-blue-50 cursor-pointer border-b border-gray-200 last:border-b-0"
                                    >
                                        <img
                                            src={artist.image}
                                            alt={artist.name}
                                            className="w-10 h-10 rounded-full object-cover mr-3"
                                            onError={(e) => {
                                                const target = e.currentTarget;
                                                // 실패 시 기본 아바타로 폴백
                                                target.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(artist.name)}&backgroundColor=random`;
                                            }}
                                        />
                                        <div className="flex-1">
                                            <div className="font-medium text-gray-900">{artist.name}</div>
                                            <div className="text-sm text-gray-500">
                                                {artist.followers && `팔로워 ${artist.followers.toLocaleString()}명`}
                                                {artist.popularity && ` • 인기도 ${artist.popularity}/100`}
                                                {artist.genres && artist.genres.length > 0 && ` • ${artist.genres.slice(0, 2).join(', ')}`}
                                            </div>
                                        </div>
                                        <Plus className="h-4 w-4 text-blue-500" />
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* 선택된 아티스트들 */}
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                        {profile.favoriteArtists.map((artist) => (
                            <div key={artist.id} className="relative group">
                                <div className="bg-white/20 rounded-lg p-3 text-center">
                                    <img
                                        src={artist.image}
                                        alt={artist.name}
                                        className="w-16 h-16 rounded-full mx-auto mb-2 object-cover"
                                        onError={(e) => {
                                            const target = e.currentTarget;
                                            // 첫 번째 시도: DiceBear 아바타
                                            if (!target.src.includes('dicebear')) {
                                                target.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(artist.name)}&backgroundColor=random`;
                                            }
                                        }}
                                    />
                                    <div className="text-white text-sm font-medium">{artist.name}</div>
                                    <button
                                        onClick={() => removeArtist(artist.id)}
                                        className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
                                    >
                                        <X className="h-3 w-3" />
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* 좋아하는 장르 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-4">
                        좋아하는 장르 ({profile.favoriteGenres.length})
                    </h2>
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                        {availableGenres.map((genre) => {
                            const isSelected = profile.favoriteGenres.find(g => g.id === genre.id);
                            return (
                                <button
                                    key={genre.id}
                                    onClick={() => toggleGenre(genre)}
                                    className={`p-3 rounded-lg text-sm font-medium transition-all ${
                                        isSelected
                                            ? 'bg-blue-600 text-white border-2 border-blue-400'
                                            : 'bg-white/20 text-white border-2 border-transparent hover:bg-white/30'
                                    }`}
                                >
                                    {genre.name}
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* 참여한 음악 페스티벌 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-4">
                        참여한 음악 페스티벌 ({profile.attendedFestivals.length})
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                        {musicFestivals.map((festival) => {
                            const isSelected = profile.attendedFestivals.includes(festival.id);
                            return (
                                <button
                                    key={festival.id}
                                    onClick={() => toggleFestival(festival.id)}
                                    className={`p-3 rounded-lg text-left transition-all ${
                                        isSelected
                                            ? 'bg-purple-600 text-white border-2 border-purple-400'
                                            : 'bg-white/20 text-white border-2 border-transparent hover:bg-white/30'
                                    }`}
                                >
                                    <div className="font-medium text-sm">{festival.name}</div>
                                    <div className="text-xs opacity-80">{festival.location}</div>
                                    <div className="text-xs opacity-60">{festival.type}</div>
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* 음악 선호도 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-6">음악 선호도</h2>

                    {/* 선호하는 시대 */}
                    <div className="mb-6">
                        <label className="block text-blue-200 text-sm mb-3">선호하는 음악 시대</label>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                            {eraOptions.map((era) => (
                                <button
                                    key={era.value}
                                    onClick={() => setProfile(prev => ({
                                        ...prev,
                                        musicPreferences: { ...prev.musicPreferences, preferredEra: era.value }
                                    }))}
                                    className={`p-3 rounded-lg text-sm font-medium transition-all ${
                                        profile.musicPreferences.preferredEra === era.value
                                            ? 'bg-green-600 text-white border-2 border-green-400'
                                            : 'bg-white/20 text-white border-2 border-transparent hover:bg-white/30'
                                    }`}
                                >
                                    {era.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* 음악 청취 빈도 */}
                    <div className="mb-6">
                        <label className="block text-blue-200 text-sm mb-3">음악 청취 빈도</label>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                            {listeningFrequencyOptions.map((freq) => (
                                <button
                                    key={freq.value}
                                    onClick={() => setProfile(prev => ({
                                        ...prev,
                                        musicPreferences: { ...prev.musicPreferences, listeningFrequency: freq.value }
                                    }))}
                                    className={`p-3 rounded-lg text-sm font-medium transition-all ${
                                        profile.musicPreferences.listeningFrequency === freq.value
                                            ? 'bg-yellow-600 text-white border-2 border-yellow-400'
                                            : 'bg-white/20 text-white border-2 border-transparent hover:bg-white/30'
                                    }`}
                                >
                                    {freq.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* 콘서트/페스티벌 참여 빈도 */}
                    <div className="mb-6">
                        <label className="block text-blue-200 text-sm mb-3">콘서트/페스티벌 참여 빈도</label>
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                            {concertFrequencyOptions.map((freq) => (
                                <button
                                    key={freq.value}
                                    onClick={() => setProfile(prev => ({
                                        ...prev,
                                        musicPreferences: { ...prev.musicPreferences, concertFrequency: freq.value }
                                    }))}
                                    className={`p-3 rounded-lg text-sm font-medium transition-all ${
                                        profile.musicPreferences.concertFrequency === freq.value
                                            ? 'bg-red-600 text-white border-2 border-red-400'
                                            : 'bg-white/20 text-white border-2 border-transparent hover:bg-white/30'
                                    }`}
                                >
                                    {freq.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* 음악 발견 방법 */}
                    <div className="mb-6">
                        <label className="block text-blue-200 text-sm mb-3">주로 새로운 음악을 발견하는 방법</label>
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                            {discoveryMethodOptions.map((method) => (
                                <button
                                    key={method.value}
                                    onClick={() => setProfile(prev => ({
                                        ...prev,
                                        musicPreferences: { ...prev.musicPreferences, discoveryMethod: method.value }
                                    }))}
                                    className={`p-3 rounded-lg text-sm font-medium transition-all ${
                                        profile.musicPreferences.discoveryMethod === method.value
                                            ? 'bg-indigo-600 text-white border-2 border-indigo-400'
                                            : 'bg-white/20 text-white border-2 border-transparent hover:bg-white/30'
                                    }`}
                                >
                                    {method.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* 선호하는 무드 (다중 선택) */}
                    <div className="mb-6">
                        <label className="block text-blue-200 text-sm mb-3">
                            선호하는 음악 무드 ({profile.musicPreferences.moodPreferences.length})
                        </label>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                            {moodOptions.map((mood) => {
                                const isSelected = profile.musicPreferences.moodPreferences.includes(mood.value);
                                return (
                                    <button
                                        key={mood.value}
                                        onClick={() => toggleMood(mood.value)}
                                        className={`p-3 rounded-lg text-sm font-medium transition-all ${
                                            isSelected
                                                ? 'bg-pink-600 text-white border-2 border-pink-400'
                                                : 'bg-white/20 text-white border-2 border-transparent hover:bg-white/30'
                                        }`}
                                    >
                                        {mood.label}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                </div>

                {/* 프로필 요약 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-4">프로필 요약</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <div className="flex items-center mb-3">
                                <Music className="h-5 w-5 text-blue-400 mr-2" />
                                <span className="text-blue-200">좋아하는 아티스트</span>
                            </div>
                            <div className="text-white text-sm">
                                {profile.favoriteArtists.length > 0
                                    ? profile.favoriteArtists.map(a => a.name).join(', ')
                                    : '아직 선택하지 않음'
                                }
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center mb-3">
                                <Star className="h-5 w-5 text-purple-400 mr-2" />
                                <span className="text-blue-200">좋아하는 장르</span>
                            </div>
                            <div className="text-white text-sm">
                                {profile.favoriteGenres.length > 0
                                    ? profile.favoriteGenres.map(g => g.name).join(', ')
                                    : '아직 선택하지 않음'
                                }
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center mb-3">
                                <Heart className="h-5 w-5 text-red-400 mr-2" />
                                <span className="text-blue-200">참여한 페스티벌</span>
                            </div>
                            <div className="text-white text-sm">
                                {profile.attendedFestivals.length > 0
                                    ? `${profile.attendedFestivals.length}개 페스티벌`
                                    : '아직 선택하지 않음'
                                }
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center mb-3">
                                <BarChart3 className="h-5 w-5 text-green-400 mr-2" />
                                <span className="text-blue-200">음악 활동</span>
                            </div>
                            <div className="text-white text-sm">
                                청취: {listeningFrequencyOptions.find(f => f.value === profile.musicPreferences.listeningFrequency)?.label || '미설정'} |
                                공연: {concertFrequencyOptions.find(f => f.value === profile.musicPreferences.concertFrequency)?.label || '미설정'}
                            </div>
                        </div>
                    </div>
                </div>

                {/* 저장 버튼 */}
                <div className="text-center">
                    <button
                        onClick={saveProfile}
                        disabled={isSaving}
                        className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 disabled:opacity-50 text-white font-bold py-3 px-8 rounded-full transition-all transform hover:scale-105 flex items-center mx-auto"
                    >
                        <Save className="h-5 w-5 mr-2" />
                        {isSaving ? '저장 중...' : '프로필 저장'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Profile;