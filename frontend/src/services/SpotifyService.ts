// Spotify Web API 연동 서비스
import axios from 'axios';

// Spotify API 응답 타입 정의
interface SpotifyArtist {
    id: string;
    name: string;
    images: Array<{
        url: string;
        height: number;
        width: number;
    }>;
    genres: string[];
    popularity: number;
    followers: {
        total: number;
    };
    external_urls: {
        spotify: string;
    };
}

interface SpotifySearchResponse {
    artists: {
        items: SpotifyArtist[];
        total: number;
        limit: number;
        offset: number;
    };
}

interface SpotifyTokenResponse {
    access_token: string;
    token_type: string;
    expires_in: number;
}

export class SpotifyService {
    private static instance: SpotifyService;
    private accessToken: string | null = null;
    private tokenExpiry: number = 0;

    // Spotify API 설정
    private readonly CLIENT_ID = process.env.REACT_APP_SPOTIFY_CLIENT_ID;
    private readonly CLIENT_SECRET = process.env.REACT_APP_SPOTIFY_CLIENT_SECRET;
    private readonly AUTH_URL = 'https://accounts.spotify.com/api/token';
    private readonly API_BASE_URL = 'https://api.spotify.com/v1';

    private constructor() {}

    // 싱글톤 패턴
    static getInstance(): SpotifyService {
        if (!SpotifyService.instance) {
            SpotifyService.instance = new SpotifyService();
        }
        return SpotifyService.instance;
    }

    /**
     * Client Credentials 플로우로 액세스 토큰 획득
     * 사용자 로그인 없이 공개 데이터 접근 가능
     */
    private async getAccessToken(): Promise<string> {
        // 토큰이 유효하면 재사용
        if (this.accessToken && Date.now() < this.tokenExpiry) {
            return this.accessToken;
        }

        if (!this.CLIENT_ID || !this.CLIENT_SECRET) {
            throw new Error('Spotify Client ID와 Client Secret이 환경변수에 설정되지 않았습니다');
        }

        try {
            const credentials = btoa(`${this.CLIENT_ID}:${this.CLIENT_SECRET}`);

            const response = await axios.post<SpotifyTokenResponse>(
                this.AUTH_URL,
                'grant_type=client_credentials',
                {
                    headers: {
                        'Authorization': `Basic ${credentials}`,
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                }
            );

            this.accessToken = response.data.access_token;
            // 토큰 만료 시간 설정 (5분 여유를 둠)
            this.tokenExpiry = Date.now() + (response.data.expires_in - 300) * 1000;

            return this.accessToken;
        } catch (error) {
            console.error('Spotify 액세스 토큰 획득 실패:', error);
            throw new Error('Spotify API 인증에 실패했습니다');
        }
    }

    /**
     * 아티스트 검색
     */
    async searchArtists(query: string, limit: number = 10): Promise<SpotifyArtist[]> {
        try {
            const token = await this.getAccessToken();

            const response = await axios.get<SpotifySearchResponse>(
                `${this.API_BASE_URL}/search`,
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                    },
                    params: {
                        q: query,
                        type: 'artist',
                        limit: limit,
                        market: 'KR', // 한국 시장 기준
                    },
                }
            );

            return response.data.artists.items;
        } catch (error) {
            console.error('아티스트 검색 실패:', error);
            throw new Error('아티스트 검색에 실패했습니다');
        }
    }

    /**
     * 아티스트 ID로 상세 정보 조회
     */
    async getArtistById(artistId: string): Promise<SpotifyArtist | null> {
        try {
            const token = await this.getAccessToken();

            const response = await axios.get<SpotifyArtist>(
                `${this.API_BASE_URL}/artists/${artistId}`,
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                    },
                }
            );

            return response.data;
        } catch (error) {
            console.error('아티스트 정보 조회 실패:', error);
            return null;
        }
    }

    /**
     * 아티스트의 최고 품질 이미지 URL 반환
     */
    static getArtistImageUrl(artist: SpotifyArtist): string {
        if (artist.images && artist.images.length > 0) {
            // 가장 큰 이미지 선택 (보통 첫 번째가 가장 큼)
            return artist.images[0].url;
        }

        // 이미지가 없으면 기본 아바타 반환
        return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(artist.name)}&backgroundColor=random`;
    }

    /**
     * 여러 아티스트 이름으로 배치 검색
     */
    async searchMultipleArtists(artistNames: string[]): Promise<Map<string, SpotifyArtist | null>> {
        const results = new Map<string, SpotifyArtist | null>();

        // 동시 요청 수 제한 (API 제한 고려)
        const BATCH_SIZE = 5;

        for (let i = 0; i < artistNames.length; i += BATCH_SIZE) {
            const batch = artistNames.slice(i, i + BATCH_SIZE);

            const promises = batch.map(async (name) => {
                try {
                    const artists = await this.searchArtists(name, 1);
                    return { name, artist: artists[0] || null };
                } catch (error) {
                    console.error(`${name} 검색 실패:`, error);
                    return { name, artist: null };
                }
            });

            const batchResults = await Promise.all(promises);
            batchResults.forEach(({ name, artist }) => {
                results.set(name, artist);
            });

            // API 제한을 피하기 위한 지연
            if (i + BATCH_SIZE < artistNames.length) {
                await new Promise(resolve => setTimeout(resolve, 100));
            }
        }

        return results;
    }

    /**
     * 인기 장르별 추천 아티스트
     */
    async getRecommendedArtistsByGenre(genre: string, limit: number = 10): Promise<SpotifyArtist[]> {
        try {
            // 장르 기반 검색 쿼리 생성
            const genreQueries = {
                'kpop': 'genre:k-pop',
                'pop': 'genre:pop',
                'rock': 'genre:rock',
                'hiphop': 'genre:hip-hop',
                'electronic': 'genre:electronic',
                'jazz': 'genre:jazz',
                'classical': 'genre:classical',
                'indie': 'genre:indie',
                'metal': 'genre:metal',
                'folk': 'genre:folk'
            };

            const query = genreQueries[genre as keyof typeof genreQueries] || `genre:${genre}`;

            return await this.searchArtists(query, limit);
        } catch (error) {
            console.error(`${genre} 장르 아티스트 검색 실패:`, error);
            return [];
        }
    }

    /**
     * 아티스트 정보를 앱의 Artist 인터페이스로 변환
     */
    static convertToAppArtist(spotifyArtist: SpotifyArtist): {
        id: string;
        name: string;
        image: string;
        followers?: number;
        genres?: string[];
        popularity?: number;
        spotifyId: string;
        spotifyUrl: string;
    } {
        return {
            id: spotifyArtist.id,
            name: spotifyArtist.name,
            image: this.getArtistImageUrl(spotifyArtist),
            followers: spotifyArtist.followers?.total,
            genres: spotifyArtist.genres,
            popularity: spotifyArtist.popularity,
            spotifyId: spotifyArtist.id,
            spotifyUrl: spotifyArtist.external_urls.spotify,
        };
    }

    /**
     * API 상태 확인
     */
    async checkApiStatus(): Promise<boolean> {
        try {
            await this.getAccessToken();
            return true;
        } catch (error) {
            console.error('Spotify API 상태 확인 실패:', error);
            return false;
        }
    }
}

export default SpotifyService;