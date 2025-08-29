import React from 'react';

type Provider = 'google' | 'kakao' | 'naver';

const BACKEND_ORIGIN =
    process.env.REACT_APP_BACKEND_ORIGIN ?? 'http://localhost:9090';

const REDIRECT_AFTER_LOGIN =
    process.env.REACT_APP_REDIRECT_AFTER_LOGIN ?? '/profile';

const SocialBtn: React.FC<
    React.ButtonHTMLAttributes<HTMLButtonElement>
> = ({ className = '', children, ...props }) => (
    <button
        {...props}
        className={`group relative w-full overflow-hidden rounded-2xl border border-white/20 bg-white/10 backdrop-blur-md px-4 py-3 text-white transition hover:scale-[1.01] active:scale-[.99] ${className}`}
    >
        {children}
    </button>
);

export default function SocialAuth() {
    const login = (provider: Provider) => {
        // 백엔드의 OAuth2 엔드포인트로 "절대 경로" 이동해야 함 (프론트 라우터로 가면 빈 화면)
        const redirectUri = `${window.location.origin}${REDIRECT_AFTER_LOGIN}`;
        const url = `${BACKEND_ORIGIN}/oauth2/authorization/${provider}?redirect_uri=${encodeURIComponent(
            redirectUri
        )}`;
        window.location.assign(url);
    };

    return (
        <div className="space-y-3">
            <SocialBtn onClick={() => login('google')} className="flex items-center justify-center gap-3">
        <span className="h-6 w-6 rounded bg-white grid place-items-center">
          <svg viewBox="0 0 48 48" className="h-4 w-4"><path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3A13 13 0 0111 24a13 13 0 1122.2 9.2l5.7 5.7A21.9 21.9 0 1046 24c0-1.2-.1-2.3-.4-3.5z"/><path fill="#FF3D00" d="M6.3 14.7l6.6 4.8A12.9 12.9 0 0124 12a13 13 0 018.2 2.9l6.2-6.2A21.9 21.9 0 006 14.7z"/><path fill="#4CAF50" d="M24 46a21.9 21.9 0 0014.8-5.4l-5.7-5.7A13 13 0 0111 24H2.9l-6.6 5A22 22 0 0024 46z"/><path fill="#1976D2" d="M46 24c0-1.2-.1-2.3-.4-3.5H24v8h11.3a13 13 0 01-4.4 6l5.7 5.7C42.5 36.7 46 30.7 46 24z"/></svg>
        </span>
                <span className="font-semibold">Google로 계속하기</span>
            </SocialBtn>

            <SocialBtn onClick={() => login('kakao')} className="flex items-center justify-center gap-3">
        <span className="h-6 w-6 rounded bg-[#FEE500] grid place-items-center">
          <svg viewBox="0 0 24 24" className="h-4 w-4"><path fill="#000" d="M12 3C6.5 3 2 6.4 2 10.6c0 2.6 1.8 4.8 4.6 6l-.8 3 3.2-2.1c.6.1 1.2.1 1.9.1 5.5 0 10-3.4 10-7.6S17.5 3 12 3z"/></svg>
        </span>
                <span className="font-semibold">카카오로 계속하기</span>
            </SocialBtn>

            <SocialBtn onClick={() => login('naver')} className="flex items-center justify-center gap-3">
        <span className="h-6 w-6 rounded bg-[#03C75A] grid place-items-center">
          <svg viewBox="0 0 24 24" className="h-4 w-4"><path fill="#fff" d="M6 5h4.2l3.6 5.4V5H18v14h-4.2l-3.6-5.4V19H6z"/></svg>
        </span>
                <span className="font-semibold">네이버로 계속하기</span>
            </SocialBtn>

            <p className="text-xs text-blue-200/70 pt-1">
                로그인 시 서비스 약관 및 개인정보 처리방침에 동의하게 됩니다.
            </p>
        </div>
    );
}
