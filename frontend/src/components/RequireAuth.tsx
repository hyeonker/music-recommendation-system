import React, { useEffect, useState } from 'react';
import api from '../api/client';
import SocialAuth from './SocialAuth';

type Me = { authenticated: boolean; user?: any };

export default function RequireAuth({ children }: { children: React.ReactNode }) {
    const [loading, setLoading] = useState(true);
    const [authed, setAuthed] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                const { data } = await api.get<Me>('/api/auth/me', { withCredentials: true });
                setAuthed(!!data?.authenticated);
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
        return (
            <div className="min-h-[70vh] flex items-center justify-center">
                {/* 외곽 장식 */}
                <div className="absolute -z-10 inset-0 pointer-events-none">
                    <div className="animate-pulse-slow absolute top-24 left-1/4 h-56 w-56 rounded-full bg-purple-600/25 blur-3xl"></div>
                    <div className="animate-pulse-slower absolute bottom-20 right-1/5 h-72 w-72 rounded-full bg-indigo-500/25 blur-3xl"></div>
                </div>

                {/* 카드 */}
                <div className="relative mx-auto w-full max-w-xl">
                    <div className="absolute -inset-[1px] rounded-3xl bg-gradient-to-br from-fuchsia-500/40 via-purple-500/40 to-indigo-500/40 blur-lg" />
                    <div className="relative rounded-3xl bg-white/10 dark:bg-white/10 backdrop-blur-xl border border-white/20 shadow-[0_10px_30px_rgba(0,0,0,0.3)]">
                        <div className="px-7 py-8">
                            <div className="flex items-center gap-3 mb-4">
                <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-white/15 border border-white/20">
                  {/* lock icon */}
                    <svg viewBox="0 0 24 24" className="h-6 w-6 text-white/90"><path fill="currentColor" d="M12 1a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1V6a5 5 0 0 0-5-5m0 2a3 3 0 0 1 3 3v3H9V6a3 3 0 0 1 3-3m0 9a2 2 0 0 1 2 2a2 2 0 0 1-1 1.73V17a1 1 0 0 1-2 0v-.27A2 2 0 0 1 10 14a2 2 0 0 1 2-2Z"/></svg>
                </span>
                                <h2 className="text-2xl font-bold tracking-tight text-white">로그인이 필요합니다</h2>
                            </div>

                            <p className="text-blue-100/90 leading-relaxed mb-6">
                                소셜 로그인을 진행하면 <span className="font-semibold text-white/95">프로필 편집</span>과
                                <span className="font-semibold text-white/95"> 맞춤 아티스트 검색</span>이 활성화됩니다.
                            </p>

                            <SocialAuth />

                            {/* 상태 박스 */}
                            <div className="mt-6 rounded-2xl bg-black/30 border border-white/15 overflow-hidden">
                                <div className="px-4 py-2 text-xs uppercase tracking-wider text-blue-200/70 border-b border-white/10">
                                    /api/auth/me 응답
                                </div>
                                <pre className="px-4 py-3 text-green-200/90 text-sm">
{`{
  "authenticated": false
}`}
                </pre>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}
