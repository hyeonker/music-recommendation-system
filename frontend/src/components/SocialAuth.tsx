import React, { useEffect, useState } from 'react';
import api from '../api/client';

type Me = {
    authenticated: boolean;
    provider?: string;
    user?: {
        id: number;
        email: string | null;
        name: string;
        profileImageUrl?: string | null;
        provider: 'GOOGLE' | 'KAKAO' | 'NAVER';
        providerId: string;
    } | null;
    principal?: any;
};

const API_BASE = api.defaults.baseURL || '';

export default function SocialAuth() {
    const [me, setMe] = useState<Me | null>(null);
    const [loading, setLoading] = useState(false);

    const fetchMe = async () => {
        setLoading(true);
        try {
            const { data } = await api.get<Me>('/api/auth/me');
            setMe(data);
        } catch (e) {
            console.error('me error', e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMe();
    }, []);

    const goto = (p: string) => {
        window.location.href = `${API_BASE}${p}`;
    };

    const logout = async () => {
        await api.post('/api/auth/logout');
        await fetchMe();
    };

    return (
        <div style={{ padding: 16, border: '1px solid #eee', borderRadius: 12 }}>
            <h3>소셜 로그인</h3>
            <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                <button onClick={() => goto('/oauth2/authorization/google')}>구글로 로그인</button>
                <button onClick={() => goto('/oauth2/authorization/kakao')}>카카오로 로그인</button>
                <button onClick={() => goto('/oauth2/authorization/naver')}>네이버로 로그인</button>
            </div>

            <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                <button onClick={fetchMe} disabled={loading}>/api/auth/me 확인</button>
                <button onClick={logout}>로그아웃</button>
            </div>

            <pre style={{ background: '#f7f7f7', padding: 12, borderRadius: 8, maxHeight: 300, overflow: 'auto' }}>
        {me ? JSON.stringify(me, null, 2) : '미인증'}
      </pre>
        </div>
    );
}
