// 아티스트 이미지 유틸리티 서비스
export class ArtistImageService {

    // 유명 아티스트 이미지 매핑
    private static knownArtists: Record<string, string> = {
        // K-Pop
        'bts': 'https://i.imgur.com/7xJqF8M.jpg',
        '아이유': 'https://i.imgur.com/2KqE9Lw.jpg',
        'iu': 'https://i.imgur.com/2KqE9Lw.jpg',
        'blackpink': 'https://i.imgur.com/8NqR5Vx.jpg',
        'twice': 'https://i.imgur.com/9MxL4Qr.jpg',
        'stray kids': 'https://i.imgur.com/6PzN3Yw.jpg',
        '뉴진스': 'https://i.imgur.com/4KtR7Zx.jpg',
        'newjeans': 'https://i.imgur.com/4KtR7Zx.jpg',
        'aespa': 'https://i.imgur.com/5LsM8Qw.jpg',
        '(여자)아이들': 'https://i.imgur.com/3HrT6Np.jpg',
        'itzy': 'https://i.imgur.com/2VwX9Mp.jpg',
        'le sserafim': 'https://i.imgur.com/7YqK5Rp.jpg',
        '르세라핌': 'https://i.imgur.com/7YqK5Rp.jpg',

        // 록/팝 (해외)
        'tame impala': 'https://lastfm.freetls.fastly.net/i/u/770x0/4128a6eb29f94943c9d206c08e625904.jpg',
        'muse': 'https://lastfm.freetls.fastly.net/i/u/770x0/c6f59c1e5e7240eaf6d0ed7fdc34bb68.jpg',
        'coldplay': 'https://lastfm.freetls.fastly.net/i/u/770x0/c9f71e7b4bb14f6595e4c7b5b5f8b4fb.jpg',
        'taylor swift': 'https://lastfm.freetls.fastly.net/i/u/770x0/c5d924bad35a47628306db0e8e5c3cd5.jpg',
        'ariana grande': 'https://lastfm.freetls.fastly.net/i/u/770x0/2a96cbd8b46e442fc41c2b86b821562f.jpg',
        'ed sheeran': 'https://lastfm.freetls.fastly.net/i/u/770x0/2a96cbd8b46e442fc41c2b86b821562f.jpg',
        'billie eilish': 'https://lastfm.freetls.fastly.net/i/u/770x0/4128a6eb29f94943c9d206c08e625904.jpg',
        'the weeknd': 'https://lastfm.freetls.fastly.net/i/u/770x0/c6f59c1e5e7240eaf6d0ed7fdc34bb68.jpg',
        'dua lipa': 'https://lastfm.freetls.fastly.net/i/u/770x0/c9f71e7b4bb14f6595e4c7b5b5f8b4fb.jpg',
        'bruno mars': 'https://lastfm.freetls.fastly.net/i/u/770x0/c5d924bad35a47628306db0e8e5c3cd5.jpg',

        // 록 밴드
        'queen': 'https://lastfm.freetls.fastly.net/i/u/770x0/2a96cbd8b46e442fc41c2b86b821562f.jpg',
        'the beatles': 'https://lastfm.freetls.fastly.net/i/u/770x0/4128a6eb29f94943c9d206c08e625904.jpg',
        'imagine dragons': 'https://lastfm.freetls.fastly.net/i/u/770x0/c6f59c1e5e7240eaf6d0ed7fdc34bb68.jpg',
        'maroon 5': 'https://lastfm.freetls.fastly.net/i/u/770x0/c9f71e7b4bb14f6595e4c7b5b5f8b4fb.jpg',
        'onerepublic': 'https://lastfm.freetls.fastly.net/i/u/770x0/c5d924bad35a47628306db0e8e5c3cd5.jpg',
        'linkin park': 'https://lastfm.freetls.fastly.net/i/u/770x0/2a96cbd8b46e442fc41c2b86b821562f.jpg',
        'green day': 'https://lastfm.freetls.fastly.net/i/u/770x0/4128a6eb29f94943c9d206c08e625904.jpg',

        // 힙합/랩
        'drake': 'https://lastfm.freetls.fastly.net/i/u/770x0/c6f59c1e5e7240eaf6d0ed7fdc34bb68.jpg',
        'kendrick lamar': 'https://lastfm.freetls.fastly.net/i/u/770x0/c9f71e7b4bb14f6595e4c7b5b5f8b4fb.jpg',
        'eminem': 'https://lastfm.freetls.fastly.net/i/u/770x0/c5d924bad35a47628306db0e8e5c3cd5.jpg',
        'post malone': 'https://lastfm.freetls.fastly.net/i/u/770x0/2a96cbd8b46e442fc41c2b86b821562f.jpg',
        'travis scott': 'https://lastfm.freetls.fastly.net/i/u/770x0/4128a6eb29f94943c9d206c08e625904.jpg',

        // 한국 아티스트 (기타)
        '박효신': 'https://i.imgur.com/3HrT6Np.jpg',
        '이승환': 'https://i.imgur.com/2VwX9Mp.jpg',
        '김범수': 'https://i.imgur.com/7YqK5Rp.jpg',
        '나얼': 'https://i.imgur.com/6PzN3Yw.jpg',
        '다비치': 'https://i.imgur.com/4KtR7Zx.jpg',
        '마마무': 'https://i.imgur.com/5LsM8Qw.jpg',
        '태연': 'https://i.imgur.com/2KqE9Lw.jpg',
        '지코': 'https://i.imgur.com/9MxL4Qr.jpg',
        '크러쉬': 'https://i.imgur.com/8NqR5Vx.jpg',
        '데이식스': 'https://i.imgur.com/7xJqF8M.jpg',

        // 일본 아티스트
        'one ok rock': 'https://lastfm.freetls.fastly.net/i/u/770x0/c6f59c1e5e7240eaf6d0ed7fdc34bb68.jpg',
        'x japan': 'https://lastfm.freetls.fastly.net/i/u/770x0/c9f71e7b4bb14f6595e4c7b5b5f8b4fb.jpg',
        '우타다 히카루': 'https://i.imgur.com/2KqE9Lw.jpg',
        'perfume': 'https://i.imgur.com/4KtR7Zx.jpg',

        // 추가 아티스트 (검색에서 자주 나오는)
        'sombr': 'https://api.dicebear.com/7.x/avataaars/svg?seed=Sombr&backgroundColor=6366f1',
    };

    /**
     * 아티스트 이름을 받아서 적절한 이미지 URL을 반환
     */
    static getArtistImage(artistName: string): string {
        const normalizedName = artistName.toLowerCase().trim();

        // 1. 알려진 아티스트 확인
        if (this.knownArtists[normalizedName]) {
            return this.knownArtists[normalizedName];
        }

        // 2. 부분 매칭 시도 (영어/한글 모두)
        for (const [knownName, imageUrl] of Object.entries(this.knownArtists)) {
            if (normalizedName.includes(knownName) || knownName.includes(normalizedName)) {
                return imageUrl;
            }
        }

        // 3. DiceBear 아바타 생성 (더 나은 기본 이미지)
        return this.generateMusicAvatar(artistName);
    }

    /**
     * 음악 테마의 일관된 아바타 생성
     */
    private static generateMusicAvatar(artistName: string): string {
        const hash = this.simpleHash(artistName);

        // 음악 관련 색상 팔레트
        const musicColors = [
            'ff6b6b', // coral
            '4ecdc4', // teal
            '45b7d1', // blue
            '96ceb4', // mint
            'ffeaa7', // yellow
            'dda0dd', // plum
            '98d8c8', // seafoam
            'f9ca24', // orange
            '6c5ce7', // purple
            'fd79a8'  // pink
        ];

        const colorIndex = hash % musicColors.length;
        const backgroundColor = musicColors[colorIndex];

        // 아티스트 이름의 첫 글자들 추출
        const seed = this.extractInitials(artistName);

        // DiceBear의 여러 스타일 중 음악에 적합한 스타일 선택
        const styles = ['avataaars', 'personas', 'miniavs', 'thumbs'];
        const styleIndex = hash % styles.length;
        const style = styles[styleIndex];

        return `https://api.dicebear.com/7.x/${style}/svg?seed=${encodeURIComponent(seed + artistName)}&backgroundColor=${backgroundColor}&scale=80`;
    }

    /**
     * 아티스트 이름을 기반으로 일관된 아바타 생성 (백업용)
     */
    private static generateConsistentAvatar(artistName: string): string {
        // 이름의 첫 글자들 추출
        const initials = this.extractInitials(artistName);

        // 이름 기반 해시로 배경색 결정
        const hash = this.simpleHash(artistName);
        const colors = [
            '3B82F6', // blue
            '8B5CF6', // purple
            'EF4444', // red
            '10B981', // green
            'F59E0B', // amber
            'EC4899', // pink
            '6366F1', // indigo
            '84CC16', // lime
            'F97316', // orange
            '06B6D4'  // cyan
        ];

        const colorIndex = hash % colors.length;
        const backgroundColor = colors[colorIndex];

        // DiceBear 서비스를 사용하여 더 나은 아바타 생성
        return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(artistName)}&backgroundColor=${backgroundColor}&fontSize=50`;
    }

    /**
     * 이름에서 이니셜 추출 (한글/영어 모두 지원)
     */
    private static extractInitials(name: string): string {
        const words = name.trim().split(/\s+/);

        if (words.length === 1) {
            // 단일 단어인 경우
            const word = words[0];
            if (this.isKorean(word)) {
                // 한글인 경우 첫 글자와 마지막 글자
                return word.length > 1 ? word[0] + word[word.length - 1] : word[0];
            } else {
                // 영어인 경우 첫 2글자
                return word.slice(0, 2).toUpperCase();
            }
        } else {
            // 여러 단어인 경우 각 단어의 첫 글자
            return words.slice(0, 2).map(word => word[0]).join('').toUpperCase();
        }
    }

    /**
     * 한글 여부 확인
     */
    private static isKorean(text: string): boolean {
        return /[ㄱ-ㅎ|ㅏ-ㅣ|가-힣]/.test(text);
    }

    /**
     * 문자열을 해시값으로 변환 (간단한 해시 함수)
     */
    private static simpleHash(str: string): number {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // 32bit 정수로 변환
        }
        return Math.abs(hash);
    }

    /**
     * 여러 이미지 소스를 시도하여 최적의 이미지 반환
     */
    static async getBestArtistImage(artistName: string): Promise<string> {
        const baseImage = this.getArtistImage(artistName);

        // 이미지 로드 테스트
        return new Promise((resolve) => {
            const img = new Image();
            img.onload = () => resolve(baseImage);
            img.onerror = () => resolve(this.generateConsistentAvatar(artistName));
            img.src = baseImage;

            // 5초 타임아웃
            setTimeout(() => resolve(this.generateConsistentAvatar(artistName)), 5000);
        });
    }
}

export default ArtistImageService;