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

// (선택) 현재 로그인 사용자 정보
export const fetchMe = async () => {
    const { data } = await api.get('/api/auth/me');
    return data; // { authenticated: boolean, user?: { id, name, email, ... } }
};
