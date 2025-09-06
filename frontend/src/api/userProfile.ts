// src/api/userProfile.ts
import api from './client';

export interface ServerProfile {
    userId: number;
    favoriteArtists: any[];      // [{ id, name, image, ... }]
    favoriteGenres: any[];       // [{ id, name }, ...]
    attendedFestivals: string[]; // ["SUMMER SONIC 2024", ...]
    musicPreferences: Record<string, any>; // { preferredEra, moodPreferences, ... }
}

// GET /api/users/{id}/profile
export const fetchProfile = async (userId: number): Promise<ServerProfile> => {
    const { data } = await api.get<ServerProfile>(`/api/users/${userId}/profile`);
    return data;
};

// PUT /api/users/{id}/profile
export const saveProfile = async (
    userId: number,
    payload: ServerProfile
): Promise<ServerProfile> => {
    const { data } = await api.put<ServerProfile>(
        `/api/users/${userId}/profile`,
        payload
    );
    return data;
};

// (선택) 현재 로그인 사용자 정보 - OAuth와 로컬 로그인 모두 확인
export const fetchMe = async () => {
    try {
        // OAuth 로그인 확인
        const oauthResponse = await api.get('/api/auth/me');
        if (oauthResponse.data?.authenticated && oauthResponse.data?.user) {
            return oauthResponse.data;
        }
        
        // OAuth 로그인이 안되어 있으면 로컬 로그인 확인
        const localResponse = await api.get('/api/auth/local/me');
        if (localResponse.data?.success && localResponse.data?.user) {
            return {
                authenticated: true,
                user: localResponse.data.user
            };
        }
        
        return { authenticated: false };
    } catch (error) {
        return { authenticated: false };
    }
};

// 현재 사용자의 기본 정보 가져오기
export const fetchMyBasicInfo = async () => {
    const { data } = await api.get('/api/users/me');
    return data;
};

// 현재 사용자의 기본 정보 수정
export const updateMyBasicInfo = async (name: string) => {
    const { data } = await api.put('/api/users/me', { name });
    return data;
};
