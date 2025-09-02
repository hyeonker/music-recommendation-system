// frontend/src/pages/Profile.tsx
import React, { useState, useEffect, useRef } from 'react';
import { Save, Music, Search, X, Plus, Star, Heart, BarChart3 } from 'lucide-react';
import api from '../api/client';
import { toast } from 'react-hot-toast';
import { useUser } from '../context/UserContext';
import { validateInput, sanitizeText } from '../utils/security';
import {
    fetchProfile as apiFetchProfile,
    saveProfile as apiSaveProfile,
    fetchMe,
    fetchMyBasicInfo,
    updateMyBasicInfo,
} from '../api/userProfile';

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
    id: number; // 화면용 id == 서버 userId
    name: string;
    email?: string | null;
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

/** 서버/화면 데이터를 안전한 기본값으로 보정 */
const normalizeProfile = (
    p: Partial<UserProfile> | null | undefined,
    fallbackUserId: number = 1
): UserProfile => {
    const base: UserProfile = {
        id: fallbackUserId,
        name: '뮤직러버',
        email: null,
        favoriteArtists: [],
        favoriteGenres: [],
        attendedFestivals: [],
        musicPreferences: {
            preferredEra: '',
            moodPreferences: [],
            listeningFrequency: '',
            concertFrequency: '',
            discoveryMethod: '',
        },
    };
    if (!p) return base;

    return {
        id: (p.id as number) ?? base.id,
        name: p.name ?? base.name,
        email: p.email ?? base.email,
        favoriteArtists: p.favoriteArtists ?? base.favoriteArtists,
        favoriteGenres: p.favoriteGenres ?? base.favoriteGenres,
        attendedFestivals: p.attendedFestivals ?? base.attendedFestivals,
        musicPreferences: {
            preferredEra: p.musicPreferences?.preferredEra ?? base.musicPreferences.preferredEra,
            moodPreferences: p.musicPreferences?.moodPreferences ?? base.musicPreferences.moodPreferences,
            listeningFrequency: p.musicPreferences?.listeningFrequency ?? base.musicPreferences.listeningFrequency,
            concertFrequency: p.musicPreferences?.concertFrequency ?? base.musicPreferences.concertFrequency,
            discoveryMethod: p.musicPreferences?.discoveryMethod ?? base.musicPreferences.discoveryMethod,
        },
    };
};

/** 어떤 응답이 와도 Artist[]로 정규화 */
const toArtistArray = (data: any): Artist[] => {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.items)) return data.items;
    if (Array.isArray(data?.artists)) return data.artists;
    if (Array.isArray(data?.data)) return data.data;
    return [];
};

const Profile: React.FC = () => {
    // ⭐ 로그인 사용자 ID 별도 관리
    const [userId, setUserId] = useState<number>(1);
    const [profile, setProfile] = useState<UserProfile>(normalizeProfile(null, userId));
    const { setUser } = useUser();

    // 검색/저장/로딩 상태
    const [artistSearchQuery, setArtistSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<Artist[]>([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [nameError, setNameError] = useState<string>('');
    const [isCheckingName, setIsCheckingName] = useState(false);
    const [likedSongs, setLikedSongs] = useState<any[]>([]);
    const [isLoadingLikedSongs, setIsLoadingLikedSongs] = useState(false);
    const [isAdmin, setIsAdmin] = useState(false);

    // 드롭다운/요청 취소 refs
    const dropdownRef = useRef<HTMLDivElement | null>(null);
    const abortRef = useRef<AbortController | null>(null);

    // --- 데이터셋 ---
    const availableGenres: Genre[] = [
        { id: 'kpop', name: 'K-Pop', description: '한국 대중음악' },
        { id: 'pop', name: 'Pop', description: '팝 음악' },
        { id: 'rock', name: 'Rock', description: '록 음악' },
        { id: 'alternative', name: 'Alternative', description: '얼터너티브 록' },
        { id: 'indie', name: 'Indie', description: '인디 음악' },
        { id: 'hiphop', name: 'Hip-Hop', description: '핍합' },
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
        { id: 'soundtrack', name: 'Soundtrack', description: '사운드트랙' },
    ];

    const musicFestivals = [
        // 한국 페스티벌
        { id: 'pentaport', name: '인천 펜타포트 락페스티벌', location: '인천 송도', type: 'Rock/Multi-genre' },
        { id: 'waterbomb', name: '워터밤', location: '서울', type: 'Hip-Hop/Electronic' },
        { id: 'lets_rock', name: '렛츠락 페스티벌', location: '서울 난지한강공원', type: 'Rock' },
        { id: 'busan_rock', name: '부산국제록페스티벌', location: '부산 삼락생태공원', type: 'Rock/Metal' },
        { id: 'seoul_jazz', name: '서울재즈페스티벌', location: '서울 올림픽공원', type: 'Jazz' },
        { id: 'edc_korea', name: 'EDC Korea', location: '과천 서울랜드', type: 'Electronic' },
        { id: 'world_dj', name: '월드 DJ 페스티벌 (월디페)', location: '서울', type: 'Electronic' },
        { id: 'beautiful_mint', name: '뷰티풀민트라이프', location: '서울 올림픽공원', type: 'Indie/Folk' },
        { id: 'dmz_peace', name: 'DMZ 피스트레인 뮤직페스티벌', location: '철원', type: 'Multi-genre' },
        { id: 'zandari', name: '잔다리 페스타', location: '서울', type: 'Indie' },
        { id: 'kb_rapbeat', name: 'KB 랩비트 페스티벌', location: '서울', type: 'Hip-Hop' },
        
        // 미국 주요 페스티벌
        { id: 'coachella', name: 'Coachella', location: '미국 캘리포니아', type: 'Multi-genre' },
        { id: 'lollapalooza', name: 'Lollapalooza', location: '미국 시카고', type: 'Multi-genre' },
        { id: 'bonnaroo', name: 'Bonnaroo', location: '미국 테네시', type: 'Multi-genre' },
        { id: 'sxsw', name: 'SXSW', location: '미국 텍사스 오스틴', type: 'Multi-genre' },
        { id: 'ultra', name: 'Ultra Music Festival', location: '미국 마이애미', type: 'Electronic' },
        { id: 'edc_vegas', name: 'EDC Las Vegas', location: '미국 라스베가스', type: 'Electronic' },
        { id: 'electric_forest', name: 'Electric Forest', location: '미국 미시간', type: 'Electronic' },
        { id: 'burning_man', name: 'Burning Man', location: '미국 네바다', type: 'Electronic/Art' },
        
        // 영국 주요 페스티벌  
        { id: 'glastonbury', name: 'Glastonbury', location: '영국 서머셋', type: 'Multi-genre' },
        { id: 'reading_leeds', name: 'Reading & Leeds', location: '영국', type: 'Rock/Alternative' },
        { id: 'download', name: 'Download Festival', location: '영국 도닝턴', type: 'Rock/Metal' },
        { id: 'wireless', name: 'Wireless Festival', location: '영국 런던', type: 'Hip-Hop/R&B' },
        { id: 'truck', name: 'Truck Festival', location: '영국 옥스퍼드셔', type: 'Indie/Alternative' },
        { id: 'latitude', name: 'Latitude Festival', location: '영국 서퍽', type: 'Multi-genre' },
        { id: 'creamfields', name: 'Creamfields', location: '영국 체셔', type: 'Electronic' },
        { id: 'isle_of_wight', name: 'Isle of Wight Festival', location: '영국 와이트섬', type: 'Rock/Pop' },
        
        // 유럽 주요 페스티벌
        { id: 'tomorrowland', name: 'Tomorrowland', location: '벨기에', type: 'Electronic' },
        { id: 'primavera', name: 'Primavera Sound', location: '스페인 바르셀로나', type: 'Indie/Alternative' },
        { id: 'sziget', name: 'Sziget Festival', location: '헝가리 부다페스트', type: 'Multi-genre' },
        { id: 'roskilde', name: 'Roskilde Festival', location: '덴마크', type: 'Multi-genre' },
        { id: 'exit', name: 'EXIT Festival', location: '세르비아 노비사드', type: 'Electronic/Rock' },
        { id: 'rock_werchter', name: 'Rock Werchter', location: '벨기에', type: 'Rock/Pop' },
        { id: 'pukkelpop', name: 'Pukkelpop', location: '벨기에', type: 'Alternative/Electronic' },
        
        // 일본/아시아 페스티벌
        { id: 'fuji_rock', name: '후지 락 페스티벌', location: '일본 니가타', type: 'Rock/Multi-genre' },
        { id: 'summer_sonic', name: '섬머소닉', location: '일본 도쿄/오사카', type: 'Multi-genre' },
        { id: 'rock_in_japan', name: 'Rock in Japan', location: '일본 이바라키', type: 'Rock/J-Pop' },
        { id: 'clockenflap', name: 'Clockenflap', location: '홍콩', type: 'Multi-genre' },
        { id: 'wonderfruit', name: 'Wonderfruit', location: '태국', type: 'Multi-genre/Art' },
    ];

    const moodOptions = [
        { value: 'energetic', label: '에너지틱' },
        { value: 'calm', label: '차분한' },
        { value: 'happy', label: '즐거운' },
        { value: 'melancholy', label: '감상적인' },
        { value: 'romantic', label: '로맨틱' },
        { value: 'focus', label: '집중' },
        { value: 'workout', label: '운동' },
        { value: 'party', label: '파티' },
    ];

    const concertFrequencyOptions = [
        { value: 'monthly', label: '월 1회 이상' },
        { value: 'few_months', label: '몇 달에 1회' },
        { value: 'yearly', label: '연 1-2회' },
        { value: 'rarely', label: '가끔' },
        { value: 'never', label: '거의 안 감' },
    ];

    const eraOptions = [
        { value: '2020s', label: '2020년대' },
        { value: '2010s', label: '2010년대' },
        { value: '2000s', label: '2000년대' },
        { value: '90s', label: '90년대' },
        { value: '80s', label: '80년대' },
        { value: '70s', label: '70년대' },
        { value: 'classic', label: '클래식' },
        { value: 'mixed', label: '다양함' },
    ];

    const listeningFrequencyOptions = [
        { value: 'daily', label: '매일' },
        { value: 'few_times_week', label: '주 2-3회' },
        { value: 'weekly', label: '주 1회' },
        { value: 'occasionally', label: '가끔' },
    ];

    const discoveryMethodOptions = [
        { value: 'streaming', label: '스트리밍 추천' },
        { value: 'friends', label: '친구 추천' },
        { value: 'social_media', label: '소셜미디어' },
        { value: 'radio', label: '라디오' },
        { value: 'live_shows', label: '라이브 공연' },
        { value: 'music_blogs', label: '음악 블로그' },
    ];

    // --- Spotify 검색 (취소/로딩/결과없음 UX) ---
    const searchArtists = async (query: string) => {
        if (!query.trim()) {
            setSearchResults([]);
            return;
        }

        if (abortRef.current) abortRef.current.abort();
        const controller = new AbortController();
        abortRef.current = controller;

        try {
            setIsSearching(true);
            const { data } = await api.get('/api/spotify/search/artists', {
                params: { q: query, limit: 3 },
                signal: controller.signal,
            });
            setSearchResults(toArtistArray(data));
        } catch (e: any) {
            if (e?.code === 'ERR_CANCELED' || e?.name === 'CanceledError' || e?.message === 'canceled') return;
            console.error('Spotify 검색 실패:', e);
            toast.error('검색에 실패했습니다.');
        } finally {
            setIsSearching(false);
        }
    };

    const addArtist = (artist: Artist) => {
        const current = profile.favoriteArtists ?? [];
        if (current.find((a) => a.id === artist.id)) {
            toast.error('이미 추가된 아티스트입니다');
            return;
        }
        setProfile((prev) =>
            normalizeProfile({ ...prev, favoriteArtists: [...(prev.favoriteArtists ?? []), artist] }, userId)
        );
        setArtistSearchQuery('');
        setSearchResults([]);
        toast.success(`${artist.name}이(가) 추가되었습니다`);
    };

    const removeArtist = (artistId: string) => {
        setProfile((prev) =>
            normalizeProfile(
                { ...prev, favoriteArtists: (prev.favoriteArtists ?? []).filter((a) => a.id !== artistId) },
                userId
            )
        );
    };

    const toggleGenre = (genre: Genre) => {
        setProfile((prev) => {
            const list = prev.favoriteGenres ?? [];
            const exists = list.find((g) => g.id === genre.id);
            return normalizeProfile(
                { ...prev, favoriteGenres: exists ? list.filter((g) => g.id !== genre.id) : [...list, genre] },
                userId
            );
        });
    };

    const toggleFestival = (festivalId: string) => {
        setProfile((prev) => {
            const list = prev.attendedFestivals ?? [];
            const exists = list.includes(festivalId);
            return normalizeProfile(
                { ...prev, attendedFestivals: exists ? list.filter((id) => id !== festivalId) : [...list, festivalId] },
                userId
            );
        });
    };

    const toggleMood = (mood: string) => {
        setProfile((prev) => {
            const mp =
                prev.musicPreferences ?? {
                    preferredEra: '',
                    moodPreferences: [],
                    listeningFrequency: '',
                    concertFrequency: '',
                    discoveryMethod: '',
                };
            const moods = mp.moodPreferences ?? [];
            const next = moods.includes(mood) ? moods.filter((m) => m !== mood) : [...moods, mood];
            return normalizeProfile({ ...prev, musicPreferences: { ...mp, moodPreferences: next } }, userId);
        });
    };

    const validateName = (name: string) => {
        if (!name || name.trim().length === 0) {
            return '';
        }
        
        const trimmedName = name.trim();
        
        // 길이 체크 (2-15자)
        if (trimmedName.length < 2) {
            return '닉네임은 최소 2자 이상이어야 합니다';
        }
        if (trimmedName.length > 15) {
            return '닉네임은 최대 15자까지 가능합니다';
        }
        
        // 관리자는 금지된 단어 체크 제외
        if (isAdmin) {
            return '';
        }
        
        // 금지된 단어 체크
        const forbiddenWords = [
            '운영자', '관리자', '어드민', 'admin', 'administrator', 
            '매니저', 'manager', '스태프', 'staff', '마스터', 'master',
            '시스템', 'system', '루트', 'root', '슈퍼', 'super',
            '모더레이터', 'moderator', 'mod', '개발자', 'developer',
            'dev', '테스트', 'test', '공지', '공식', 'official'
        ];
        
        const lowerName = trimmedName.toLowerCase();
        for (const word of forbiddenWords) {
            if (lowerName.includes(word.toLowerCase())) {
                return '사용할 수 없는 닉네임입니다';
            }
        }
        
        return '';
    };

    const checkNameExists = async (name: string) => {
        if (!name || name.trim().length === 0) {
            setNameError('');
            return;
        }
        
        // 먼저 로컬 검증
        const localValidationError = validateName(name);
        if (localValidationError) {
            setNameError(localValidationError);
            return;
        }
        
        try {
            setIsCheckingName(true);
            const response = await fetch(`/api/users/check-name/${encodeURIComponent(name.trim())}`, {
                credentials: 'include'
            });
            
            if (response.ok) {
                const data = await response.json();
                
                // 서버에서 관리자 권한을 확인한 경우 항상 허용
                if (data.isAdmin) {
                    setNameError('');
                    return;
                }
                
                if (data.exists) {
                    setNameError('이미 사용 중인 닉네임입니다');
                } else {
                    setNameError('');
                }
            } else if (response.status === 400) {
                // 서버측 유효성 검사 실패
                const data = await response.json();
                setNameError(data.error || '사용할 수 없는 닉네임입니다');
            } else if (response.status === 404) {
                // API 엔드포인트가 없는 경우 - 서버가 준비되지 않았으므로 조용히 무시
                setNameError('');
            } else {
                setNameError('닉네임 확인 중 오류가 발생했습니다');
            }
        } catch (error) {
            console.error('닉네임 확인 오류:', error);
            // 네트워크 오류인 경우 조용히 무시 (서버가 준비되지 않았을 수 있음)
            setNameError('');
        } finally {
            setIsCheckingName(false);
        }
    };

    // 사용자의 좋아요한 음악 로드
    const loadLikedSongs = async () => {
        setIsLoadingLikedSongs(true);
        try {
            const response = await api.get(`/api/likes/user/${userId}`);
            const likes = response.data?.content || response.data || [];
            setLikedSongs(likes);
        } catch (error) {
            console.error('좋아요한 음악 로드 실패:', error);
            setLikedSongs([]);
        } finally {
            setIsLoadingLikedSongs(false);
        }
    };

    // 좋아요 취소 함수
    const handleUnlikeSong = async (like: any) => {
        try {
            const songId = like.song?.id;
            if (!songId) {
                throw new Error('곡 ID를 찾을 수 없습니다.');
            }

            // API 호출로 좋아요 취소
            await api.post('/api/likes/toggle', null, {
                params: { userId, songId }
            });

            // 성공 시 목록에서 제거
            setLikedSongs(prev => prev.filter(item => item.id !== like.id));
            
            // 성공 메시지
            toast.success(`"${like.song?.title || like.title}"의 좋아요를 취소했습니다.`);
        } catch (error) {
            console.error('좋아요 취소 실패:', error);
            toast.error('좋아요 취소 중 오류가 발생했습니다.');
        }
    };

    // 앨범 표지 URL 생성 함수
    const getAlbumCoverUrl = (like: any) => {
        // 먼저 실제 앨범 표지 URL이 있는지 확인
        const realImageUrl = like.song?.imageUrl || like.imageUrl;
        if (realImageUrl && realImageUrl.startsWith('http')) {
            return realImageUrl;
        }
        
        // 실제 앨범 표지가 없으면 기본 이미지 사용
        const title = like.song?.title || like.title || 'Music';
        const artist = like.song?.artist || like.artist || 'Artist';
        return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(title + ' ' + artist)}&backgroundColor=6366f1,8b5cf6,ec4899,f59e0b,10b981,f97316&backgroundType=gradientLinear`;
    };

    const onSaveProfile = async () => {
        // 운영자가 아닌 경우에만 이름 중복 체크
        if (!isAdmin && nameError) {
            toast.error('닉네임 중복을 해결해주세요');
            return;
        }
        
        try {
            setIsSaving(true);
            
            // 1. 프로필 데이터 저장
            const payload = {
                userId,
                favoriteArtists: profile.favoriteArtists,
                favoriteGenres: profile.favoriteGenres,
                attendedFestivals: profile.attendedFestivals,
                musicPreferences: profile.musicPreferences,
            };
            const saved = await apiSaveProfile(userId, payload);
            
            // 2. 기본 사용자 정보(이름) 저장
            if (profile.name) {
                await updateMyBasicInfo(profile.name);
                
                // 3. UserContext 업데이트 (네비게이션 바에 즉시 반영)
                setUser({
                    id: userId,
                    name: profile.name
                });
            }
            
            // saved는 ServerProfile 타입이므로 as any로 합치고 normalize로 보정
            setProfile((prev) => normalizeProfile({ ...prev, ...(saved as any) }, userId));
            toast.success('프로필이 저장되었습니다!');
        } catch (e: any) {
            console.error('프로필 저장 실패:', e);
            if (e.response?.status === 400) {
                toast.error('이미 사용 중인 닉네임입니다');
            } else {
                toast.error('프로필 저장에 실패했습니다');
            }
        } finally {
            setIsSaving(false);
        }
    };

    // --- Effects ---

    // 1) 로그인 사용자 ID 가져오기
    useEffect(() => {
        (async () => {
            try {
                const me = await fetchMe(); // { authenticated, user?: { id, ... } }
                if (me?.authenticated && me?.user?.id) {
                    setUserId(me.user.id);
                    
                    // 관리자 권한 확인
                    try {
                        const adminResponse = await fetch('/api/admin/reports/stats', {
                            credentials: 'include'
                        });
                        setIsAdmin(adminResponse.ok);
                    } catch {
                        setIsAdmin(false);
                    }
                } else {
                    setUserId(1);
                    setIsAdmin(false);
                }
            } catch {
                setUserId(1);
                setIsAdmin(false);
            }
        })();
    }, []);

    // 2) userId가 정해지면 서버에서 프로필과 사용자 기본 정보를 모두 fetch
    useEffect(() => {
        if (!userId) return;
        (async () => {
            try {
                // 프로필 정보와 기본 사용자 정보를 병렬로 가져오기
                const [profileData, basicInfo] = await Promise.all([
                    apiFetchProfile(userId),
                    fetchMyBasicInfo().catch(() => null) // 실패해도 계속 진행
                ]);
                
                // 기본 정보에서 실제 이름을 가져와서 사용
                const actualName = basicInfo?.name || '뮤직러버'; // fallback
                
                // 전역 사용자 정보도 업데이트
                setUser({ id: userId, name: actualName });
                
                // 전역 이벤트로 모든 페이지에 이름 변경 알림
                window.dispatchEvent(new CustomEvent('userNameChanged', { 
                    detail: { id: userId, name: actualName } 
                }));
                
                setProfile((prev) => normalizeProfile({ 
                    ...prev, 
                    ...(profileData as any), 
                    id: userId,
                    name: actualName 
                }, userId));
            } catch (e) {
                console.log('프로필 로드 실패 (초기값 사용):', e);
                setProfile((prev) => normalizeProfile(prev, userId));
            } finally {
                setIsLoading(false);
            }
        })();
        
        // 좋아요한 음악도 함께 로드
        if (userId) {
            loadLikedSongs();
        }
    }, [userId]);

    // 검색 디바운스
    useEffect(() => {
        const t = setTimeout(() => {
            if (artistSearchQuery) searchArtists(artistSearchQuery);
        }, 400);
        return () => clearTimeout(t);
    }, [artistSearchQuery]);

    // 이름 중복 체크 디바운스
    useEffect(() => {
        const t = setTimeout(() => {
            if (profile.name && profile.name.trim().length > 0) {
                checkNameExists(profile.name.trim());
            }
        }, 500);
        return () => clearTimeout(t);
    }, [profile.name]);

    // ESC로 드롭다운/검색 취소
    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape' && (searchResults.length || isSearching)) {
                setSearchResults([]);
                setArtistSearchQuery('');
                abortRef.current?.abort();
            }
        };
        document.addEventListener('keydown', onKey);
        return () => document.removeEventListener('keydown', onKey);
    }, [searchResults.length, isSearching]);

    // 바깥 클릭으로 드롭다운 닫기
    useEffect(() => {
        const onDown = (e: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
                setSearchResults([]);
            }
        };
        document.addEventListener('mousedown', onDown);
        return () => document.removeEventListener('mousedown', onDown);
    }, []);

    // 언마운트 시 검색 요청 취소
    useEffect(() => () => abortRef.current?.abort(), []);

    // --- UI 바인딩 ---
    const favArtists = profile.favoriteArtists ?? [];
    const favGenres = profile.favoriteGenres ?? [];
    const attended = profile.attendedFestivals ?? [];
    const mp =
        profile.musicPreferences ?? {
            preferredEra: '',
            moodPreferences: [],
            listeningFrequency: '',
            concertFrequency: '',
            discoveryMethod: '',
        };

    const results = Array.isArray(searchResults) ? searchResults : [];
    const shouldShowDropdown = !!artistSearchQuery.trim() && (isSearching || results.length > 0);

    // 로딩 화면
    if (isLoading) {
        return (
            <div className="min-h-[50vh] grid place-items-center">
                <div className="loading-spinner" />
            </div>
        );
    }

    // --- UI ---
    return (
        <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 p-6 pb-12 relative">
            <div className="max-w-4xl mx-auto">
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-white mb-2">음악 프로필</h1>
                    <p className="text-blue-200">당신의 음악 취향을 알려주세요</p>
                </div>

                {/* 기본 정보 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-4">기본 정보</h2>
                    <div className="max-w-md">
                        <div>
                            <label className="block text-blue-200 text-sm mb-2">이름</label>
                            <div className="relative">
                                <input
                                    type="text"
                                    value={profile.name ?? ''}
                                    maxLength={15}
                                    onChange={(e) => {
                                        const rawValue = e.target.value;
                                        
                                        // 보안 검증
                                        const validation = validateInput(rawValue, '닉네임');
                                        if (!validation.isValid) {
                                            setNameError(validation.error || '올바르지 않은 입력입니다');
                                            return;
                                        }
                                        
                                        const newName = validation.sanitized;
                                        setProfile((prev) => normalizeProfile({ ...prev, name: newName }, userId));
                                        
                                        // 빈 값일 때만 즉시 에러 초기화
                                        if (newName.trim().length === 0) {
                                            setNameError('');
                                        }
                                    }}
                                    className={`w-full px-4 py-2 bg-white/20 border ${
                                        nameError ? 'border-red-400' : 'border-white/30'
                                    } rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 ${
                                        nameError ? 'focus:ring-red-500' : 'focus:ring-blue-500'
                                    }`}
                                    placeholder="닉네임을 입력하세요 (2-15자)"
                                />
                                {isCheckingName && (
                                    <div className="absolute right-3 top-3">
                                        <div className="w-4 h-4 border-2 border-white/50 border-t-white rounded-full animate-spin"></div>
                                    </div>
                                )}
                            </div>
                            {nameError && (
                                <p className="text-red-400 text-xs mt-1">{nameError}</p>
                            )}
                        </div>
                    </div>
                </div>

                {/* 좋아하는 아티스트 */}
                <div
                    className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20 relative z-[100]"
                    style={{ isolation: 'isolate' }}
                >
                    <h2 className="text-xl font-bold text-white mb-4">
                        좋아하는 아티스트 ({favArtists.length})
                    </h2>

                    {/* 아티스트 검색 */}
                    <div className="relative mb-4">
                        <div className="flex items-center">
                            <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                            <input
                                type="text"
                                value={artistSearchQuery}
                                onChange={(e) => {
                                    const rawValue = e.target.value;
                                    const validation = validateInput(rawValue, '검색어');
                                    if (validation.isValid) {
                                        setArtistSearchQuery(validation.sanitized);
                                    }
                                }}
                                className="w-full pl-10 pr-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder={isSearching ? '검색 중…' : '아티스트를 검색하세요...'}
                            />
                        </div>

                        {/* 검색 결과 드롭다운 */}
                        {(shouldShowDropdown ||
                            (!isSearching && artistSearchQuery.trim() && results.length === 0)) && (
                            <div
                                ref={dropdownRef}
                                className="absolute left-0 right-0 mt-2 bg-white rounded-lg shadow-2xl border border-gray-200 z-[200] max-h-96 overflow-hidden flex flex-col"
                            >
                                <div className="flex justify-between items-center p-3 border-b bg-gradient-to-r from-blue-50 to-purple-50">
                                    <div>
                                        <h3 className="font-semibold text-gray-800 text-sm">아티스트 검색 결과</h3>
                                        <p className="text-xs text-gray-600">
                                            {isSearching ? '로딩 중…' : `${results.length}개 결과`}
                                        </p>
                                    </div>
                                    <button
                                        onClick={() => {
                                            setSearchResults([]);
                                            setArtistSearchQuery('');
                                        }}
                                        className="text-gray-400 hover:text-gray-600 p-1 rounded-full"
                                    >
                                        <X className="h-4 w-4" />
                                    </button>
                                </div>

                                <div className="flex-1 overflow-y-auto">
                                    {isSearching && (
                                        <div className="p-4 text-center text-sm text-gray-500">검색 중…</div>
                                    )}

                                    {!isSearching && results.length === 0 && artistSearchQuery.trim() && (
                                        <div className="p-4 text-center text-sm text-gray-500">결과가 없습니다</div>
                                    )}

                                    {!isSearching &&
                                        results.length > 0 &&
                                        results.map((artist) => (
                                            <div
                                                key={artist.id}
                                                onClick={() => addArtist(artist)}
                                                className="flex items-center p-3 hover:bg-gradient-to-r hover:from-blue-50 hover:to-purple-50 cursor-pointer border-b border-gray-100 last:border-b-0 transition-colors"
                                            >
                                                <img
                                                    src={artist.image}
                                                    alt={artist.name}
                                                    className="w-10 h-10 rounded-full object-cover mr-3 shadow-md"
                                                    onError={(e) => {
                                                        const t = e.currentTarget as HTMLImageElement;
                                                        t.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(
                                                            artist.name
                                                        )}&backgroundColor=random`;
                                                    }}
                                                />
                                                <div className="flex-1 min-w-0">
                                                    <div className="font-semibold text-gray-900 text-sm">{artist.name}</div>
                                                    <div className="text-xs text-gray-600">
                                                        {typeof artist.followers === 'number'
                                                            ? `팔로워 ${artist.followers.toLocaleString()}명`
                                                            : null}
                                                        {typeof artist.popularity === 'number'
                                                            ? `${typeof artist.followers === 'number' ? ' • ' : ''}인기도 ${
                                                                artist.popularity
                                                            }/100`
                                                            : null}
                                                    </div>
                                                    {artist.genres?.length ? (
                                                        <div className="text-xs text-gray-500 mt-1">
                                                            {artist.genres.slice(0, 2).join(' • ')}
                                                        </div>
                                                    ) : null}
                                                </div>
                                                <div className="ml-3 p-2 bg-blue-100 text-blue-600 rounded-full hover:bg-blue-200 transition-colors">
                                                    <Plus className="h-4 w-4" />
                                                </div>
                                            </div>
                                        ))}
                                </div>

                                <div className="p-2 bg-gray-50">
                                    <p className="text-xs text-gray-500 text-center">
                                        클릭하여 아티스트를 추가하거나 ESC로 닫기
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* 선택된 아티스트들 */}
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                        {favArtists.map((artist) => (
                            <div key={artist.id} className="relative group">
                                <div className="bg-white/20 rounded-lg p-3 text-center">
                                    <img
                                        src={artist.image}
                                        alt={artist.name}
                                        className="w-16 h-16 rounded-full mx-auto mb-2 object-cover"
                                        onError={(e) => {
                                            const t = e.currentTarget as HTMLImageElement;
                                            if (!t.src.includes('dicebear')) {
                                                t.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(
                                                    artist.name
                                                )}&backgroundColor=random`;
                                            }
                                        }}
                                    />
                                    <div className="text-white text-sm font-medium">{artist.name}</div>

                                    <div className="mt-2 flex flex-wrap justify-center gap-1">
                                        {typeof artist.followers === 'number' && (
                                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/30 text-white">
                        팔로워 {artist.followers.toLocaleString()}명
                      </span>
                                        )}
                                        {typeof artist.popularity === 'number' && (
                                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/30 text-white">
                        인기도 {artist.popularity}/100
                      </span>
                                        )}
                                    </div>

                                    {artist.genres?.length ? (
                                        <div className="mt-2 flex flex-wrap justify-center gap-1">
                                            {artist.genres.slice(0, 3).map((g) => (
                                                <span
                                                    key={g}
                                                    className="text-[10px] px-2 py-0.5 rounded-full bg-black/30 text-white"
                                                    title={g}
                                                >
                          {g}
                        </span>
                                            ))}
                                        </div>
                                    ) : null}

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
                        좋아하는 장르 ({favGenres.length})
                    </h2>
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 max-h-96 overflow-y-auto">
                        {availableGenres.map((genre) => {
                            const isSelected = favGenres.find((g) => g.id === genre.id);
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
                        참여한 음악 페스티벌 ({attended.length})
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                        {musicFestivals.map((festival) => {
                            const isSelected = attended.includes(festival.id);
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
                                    onClick={() =>
                                        setProfile((prev) =>
                                            normalizeProfile(
                                                { ...prev, musicPreferences: { ...(prev.musicPreferences), preferredEra: era.value } },
                                                userId
                                            )
                                        )
                                    }
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
                                    onClick={() =>
                                        setProfile((prev) =>
                                            normalizeProfile(
                                                {
                                                    ...prev,
                                                    musicPreferences: { ...(prev.musicPreferences), listeningFrequency: freq.value },
                                                },
                                                userId
                                            )
                                        )
                                    }
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
                                    onClick={() =>
                                        setProfile((prev) =>
                                            normalizeProfile(
                                                {
                                                    ...prev,
                                                    musicPreferences: { ...(prev.musicPreferences), concertFrequency: freq.value },
                                                },
                                                userId
                                            )
                                        )
                                    }
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
                                    onClick={() =>
                                        setProfile((prev) =>
                                            normalizeProfile(
                                                { ...prev, musicPreferences: { ...(prev.musicPreferences), discoveryMethod: method.value } },
                                                userId
                                            )
                                        )
                                    }
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

                    {/* 선호 무드 (다중 선택) */}
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

                {/* 내가 좋아요한 음악 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
                    <h2 className="text-xl font-bold text-white mb-4 flex items-center">
                        <Heart className="h-6 w-6 text-red-400 mr-2" />
                        내가 좋아요한 음악 ({likedSongs.length})
                    </h2>

                    {isLoadingLikedSongs ? (
                        <div className="text-center py-8">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white mx-auto mb-4"></div>
                            <p className="text-blue-200">좋아요한 음악을 불러오는 중...</p>
                        </div>
                    ) : likedSongs.length === 0 ? (
                        <div className="text-center py-8">
                            <Heart className="h-16 w-16 text-gray-400 mx-auto mb-4 opacity-50" />
                            <p className="text-gray-400 text-lg mb-2">아직 좋아요한 음악이 없어요</p>
                            <p className="text-gray-500 text-sm">대시보드나 음악탐색에서 좋아하는 음악에 ❤️를 눌러보세요!</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {likedSongs.slice(0, 9).map((like, index) => (
                                <div
                                    key={like.id || index}
                                    className="bg-white/10 rounded-lg p-4 hover:bg-white/20 transition-colors group"
                                >
                                    <div className="flex items-center space-x-3 mb-3">
                                        <div className="relative">
                                            <img
                                                src={getAlbumCoverUrl(like)}
                                                alt={like.song?.title || like.title}
                                                className="w-16 h-16 rounded-lg object-cover shadow-lg"
                                                onError={(e) => {
                                                    const img = e.currentTarget;
                                                    img.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent((like.song?.title || like.title || 'Music') + ' ' + (like.song?.artist || like.artist || 'Artist'))}&backgroundColor=6366f1`;
                                                }}
                                            />
                                            <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 rounded-lg transition-all duration-200"></div>
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <h4 className="text-white font-medium text-sm truncate mb-1">
                                                {like.song?.title || like.title || '제목 없음'}
                                            </h4>
                                            <p className="text-blue-200 text-xs truncate mb-1">
                                                {like.song?.artist || like.artist || '아티스트 없음'}
                                            </p>
                                            {like.likedAt && (
                                                <p className="text-gray-400 text-xs">
                                                    {new Date(like.likedAt).toLocaleDateString('ko-KR')}
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-400 text-xs">좋아요한 음악</span>
                                        <button
                                            onClick={() => handleUnlikeSong(like)}
                                            className="group/btn flex items-center space-x-1 text-red-400 hover:text-red-300 transition-colors duration-200 p-1 rounded-md hover:bg-red-500/10"
                                            title="좋아요 취소"
                                        >
                                            <Heart className="h-4 w-4 fill-current group-hover/btn:scale-110 transition-transform duration-200" />
                                            <span className="text-xs opacity-0 group-hover/btn:opacity-100 transition-opacity duration-200">취소</span>
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {likedSongs.length > 9 && (
                        <div className="text-center mt-4">
                            <p className="text-blue-200 text-sm">
                                +{likedSongs.length - 9}개의 음악을 더 좋아요했어요
                            </p>
                        </div>
                    )}
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
                                {favArtists.length > 0 ? favArtists.map((a) => a.name).join(', ') : '아직 선택하지 않음'}
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center mb-3">
                                <Star className="h-5 w-5 text-purple-400 mr-2" />
                                <span className="text-blue-200">좋아하는 장르</span>
                            </div>
                            <div className="text-white text-sm">
                                {favGenres.length > 0 ? favGenres.map((g) => g.name).join(', ') : '아직 선택하지 않음'}
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center mb-3">
                                <Heart className="h-5 w-5 text-red-400 mr-2" />
                                <span className="text-blue-200">참여한 페스티벌</span>
                            </div>
                            <div className="text-white text-sm">
                                {attended.length > 0 ? `${attended.length}개 페스티벌` : '아직 선택하지 않음'}
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center mb-3">
                                <BarChart3 className="h-5 w-5 text-green-400 mr-2" />
                                <span className="text-blue-200">음악 활동</span>
                            </div>
                            <div className="text-white text-sm">
                                청취:{' '}
                                {listeningFrequencyOptions.find(
                                    (f) => f.value === profile.musicPreferences.listeningFrequency
                                )?.label || '미설정'}{' '}
                                | 공연:{' '}
                                {concertFrequencyOptions.find(
                                    (f) => f.value === profile.musicPreferences.concertFrequency
                                )?.label || '미설정'}
                            </div>
                        </div>
                    </div>
                </div>

                {/* 저장 버튼 */}
                <div className="text-center">
                    <button
                        onClick={onSaveProfile}
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
