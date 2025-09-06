import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

type Me = { authenticated: boolean; user?: any };

export default function RequireAuth({ children }: { children: React.ReactNode }) {
    const [loading, setLoading] = useState(true);
    const [authed, setAuthed] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        (async () => {
            try {
                // 1. OAuth 로그인 확인
                const { data: oauthData } = await api.get<Me>('/api/auth/me', { withCredentials: true });
                if (oauthData?.authenticated) {
                    setAuthed(true);
                    setLoading(false);
                    return;
                }

                // 2. OAuth 로그인이 안되어 있으면 로컬 로그인 확인
                const { data: localData } = await api.get('/api/auth/local/me', { withCredentials: true });
                setAuthed(!!localData?.success);
            } catch {
                setAuthed(false);
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) {
        return (
            <div className="min-h-[60vh] grid place-items-center">
                <div className="relative">
                    <div className="h-14 w-14 rounded-full border-4 border-white/20 border-t-white animate-spin" />
                    <div className="absolute inset-0 -z-10 blur-2xl bg-[radial-gradient(circle_at_center,rgba(168,85,247,.35),transparent_60%)] w-36 h-36 rounded-full translate-x-[-30%] translate-y-[-30%]" />
                </div>
            </div>
        );
    }

    if (!authed) {
        navigate('/login');
        return null;
    }

    return <>{children}</>;
}
