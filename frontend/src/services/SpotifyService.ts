// Spotify Web API 연동 서비스 (백엔드 통합 버전)
import axios from 'axios';

// 백엔드 API 응답 타입 정의
interface BackendArtist {
    id: string;
    name: string;
    image?: string;
    followers?: number;
    genres?: string[];
    popularity?: number;
    spotifyId?: string;
    spotifyUrl?: string;
}

export class SpotifyService {
    private static instance: SpotifyService;
    private readonly API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:9090';

    private constructor() {}

    // 싱글톤 패턴
    static getInstance(): SpotifyService {
        if (!SpotifyService.instance) {
            SpotifyService.instance = new SpotifyService();
        }
        return SpotifyService.instance;
    }

    /**
     * 백엔드를 통한 아티스트 검색
     */
    async searchArtists(query: string, limit: number = 10): Promise<BackendArtist[]> {
        try {
            console.log(`백엔드로 아티스트 검색 요청: ${query}`);

            const response = await axios.get(`${this.API_BASE_URL}/api/spotify/search/artists`, {
                params: {
                    q: query,
                    limit: limit
                },
                timeout: 10000 // 10초 타임아웃
            });

            console.log('백엔드 검색 응답:', response.data);
            return response.data || [];
        } catch (error) {
            console.error('백엔드 아티스트 검색 실패:', error);

            // 백엔드 API가 없으면 기본 검색 API 사용
            try {
                console.log('기본 검색 API로 폴백');
                const fallbackResponse = await axios.get(`${this.API_BASE_URL}/api/artists/search`, {
                    params: { q: query }
                });

                return fallbackResponse.data || [];
            } catch (fallbackError) {
                console.error('기본 검색 API도 실패:', fallbackError);
                throw new Error('아티스트 검색에 실패했습니다');
            }
        }
    }

    /**
     * 백엔드를 통한 아티스트 상세 정보 조회
     */
    async getArtistById(artistId: string): Promise<BackendArtist | null> {
        try {
            const response = await axios.get(`${this.API_BASE_URL}/api/spotify/artists/${artistId}`);
            return response.data;
        } catch (error) {
            console.error('아티스트 정보 조회 실패:', error);
            return null;
        }
    }

    /**
     * 아티스트의 최고 품질 이미지 URL 반환
     */
    static getArtistImageUrl(artist: BackendArtist): string {
        if (artist.image) {
            return artist.image;
        }

        // 이미지가 없으면 기본 아바타 반환
        return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(artist.name)}&backgroundColor=random`;
    }

    /**
     * 여러 아티스트 이름으로 배치 검색
     */
    async searchMultipleArtists(artistNames: string[]): Promise<Map<string, BackendArtist | null>> {
        const results = new Map<string, BackendArtist | null>();

        try {
            const response = await axios.post(`${this.API_BASE_URL}/api/spotify/search/batch`, {
                artistNames: artistNames
            });

            // 응답 형태에 따라 처리
            if (response.data) {
                artistNames.forEach((name, index) => {
                    results.set(name, response.data[index] || null);
                });
            }
        } catch (error) {
            console.error('배치 검색 실패:', error);

            // 개별 검색으로 폴백
            for (const name of artistNames) {
                try {
                    const artists = await this.searchArtists(name, 1);
                    results.set(name, artists[0] || null);
                } catch (individualError) {
                    console.error(`${name} 개별 검색 실패:`, individualError);
                    results.set(name, null);
                }
            }
        }

        return results;
    }

    /**
     * 인기 장르별 추천 아티스트
     */
    async getRecommendedArtistsByGenre(genre: string, limit: number = 10): Promise<BackendArtist[]> {
        try {
            const response = await axios.get(`${this.API_BASE_URL}/api/spotify/recommendations/genre/${genre}`, {
                params: { limit }
            });

            return response.data || [];
        } catch (error) {
            console.error(`${genre} 장르 아티스트 검색 실패:`, error);

            // 장르 기반 일반 검색으로 폴백
            try {
                return await this.searchArtists(`genre:${genre}`, limit);
            } catch (fallbackError) {
                console.error('장르 검색 폴백도 실패:', fallbackError);
                return [];
            }
        }
    }

    /**
     * Spotify 아티스트 정보를 앱의 Artist 인터페이스로 변환
     */
    static convertToAppArtist(backendArtist: BackendArtist): {
        id: string;
        name: string;
        image: string;
        followers?: number;
        genres?: string[];
        popularity?: number;
        spotifyId?: string;
        spotifyUrl?: string;
    } {
        return {
            id: backendArtist.id,
            name: backendArtist.name,
            image: this.getArtistImageUrl(backendArtist),
            followers: backendArtist.followers,
            genres: backendArtist.genres,
            popularity: backendArtist.popularity,
            spotifyId: backendArtist.spotifyId,
            spotifyUrl: backendArtist.spotifyUrl,
        };
    }

    /**
     * 더미 데이터 생성 (백엔드 API가 없을 때 사용)
     */
    private generateDummyArtist(name: string): BackendArtist {
        return {
            id: `dummy_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            name: name,
            image: `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(name)}&backgroundColor=random`,
            followers: Math.floor(Math.random() * 10000000),
            genres: ['검색결과'],
            popularity: Math.floor(Math.random() * 100),
        };
    }

    /**
     * API 상태 확인
     */
    async checkApiStatus(): Promise<boolean> {
        try {
            const response = await axios.get(`${this.API_BASE_URL}/api/spotify/health`, {
                timeout: 5000
            });
            return response.status === 200;
        } catch (error) {
            console.log('Spotify 백엔드 API 상태 확인 실패:', error);

            // 기본 백엔드 상태 확인
            try {
                const fallbackResponse = await axios.get(`${this.API_BASE_URL}/api/health`, {
                    timeout: 5000
                });
                return fallbackResponse.status === 200;
            } catch (fallbackError) {
                console.error('백엔드 연결 실패:', fallbackError);
                return false;
            }
        }
    }

    /**
     * 개발용: 더미 검색 결과 반환
     */
    async getDummySearchResults(query: string): Promise<BackendArtist[]> {
        console.log(`더미 검색 결과 생성: ${query}`);

        // 유명 아티스트 더미 데이터
        const dummyData: Record<string, Partial<BackendArtist>> = {
            'bts': {
                name: 'BTS',
                image: 'https://api.dicebear.com/7.x/avataaars/svg?seed=BTS&backgroundColor=6366f1',
                followers: 35000000,
                genres: ['K-Pop', 'Pop'],
                popularity: 95
            },
            '아이유': {
                name: '아이유',
                image: 'https://api.dicebear.com/7.x/avataaars/svg?seed=IU&backgroundColor=ec4899',
                followers: 8000000,
                genres: ['K-Pop', 'Ballad'],
                popularity: 90
            },
            'taylor swift': {
                name: 'Taylor Swift',
                image: 'https://api.dicebear.com/7.x/avataaars/svg?seed=TaylorSwift&backgroundColor=f59e0b',
                followers: 45000000,
                genres: ['Pop', 'Country'],
                popularity: 98
            },
            'coldplay': {
                name: 'Coldplay',
                image: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Coldplay&backgroundColor=10b981',
                followers: 25000000,
                genres: ['Rock', 'Alternative'],
                popularity: 85
            }
        };

        const normalizedQuery = query.toLowerCase().trim();
        const matchedArtist = dummyData[normalizedQuery];

        if (matchedArtist) {
            return [{
                id: `dummy_${normalizedQuery}`,
                ...matchedArtist,
                name: matchedArtist.name || query,
                image: matchedArtist.image || `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(query)}&backgroundColor=random`
            } as BackendArtist];
        }

        // 매칭되지 않으면 일반 더미 데이터 반환
        return [this.generateDummyArtist(query)];
    }
}

export default SpotifyService;