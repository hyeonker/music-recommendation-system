import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';

// Components
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import Matching from './pages/Matching';
import Chat from './pages/Chat';
import Stats from './pages/Stats';
import Profile from './pages/Profile';
import Reviews from './pages/Reviews';
import AdminPanel from './pages/AdminPanel';
import AdminReports from './pages/AdminReports';
import ProtectedRoute from './components/ProtectedRoute';
import MusicDiscovery from './pages/MusicDiscovery';
import MusicHistory from './pages/MusicHistory';
import LoadingSpinner from './components/LoadingSpinner';
import RequireAuth from './components/RequireAuth';   // ⭐ 추가: 로그인 가드
import SocialAuth from './components/SocialAuth';     // ⭐ 선택: /login 전용 페이지에서 사용
import Login from './pages/Login';                    // ⭐ 새로운 로그인 페이지
import Signup from './pages/Signup';                  // ⭐ 새로운 회원가입 페이지

// Context
import { SocketProvider } from './context/SocketContext';
import { UserContext } from './context/UserContext';

// Styles
import './index.css';
import './styles/legendary-animations.css';

// Create a client for React Query with enhanced error handling
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            retry: (failureCount, error: any) => {
                if (error?.code === 'NETWORK_ERROR' && failureCount < 3) return true;
                if (error?.status >= 500 && failureCount < 1) return true;
                return false;
            },
            retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
        },
        mutations: { retry: 1 },
    },
});

function App() {
    const [user, setUser] = useState({ id: 1, name: '뮤직러버' });
    const [darkMode, setDarkMode] = useState(true);
    const [isLoading, setIsLoading] = useState(true);

    // ⭐ 다크모드 토글 (클래스 적용은 별도 useEffect에서 수행)
    const toggleDarkMode = () => setDarkMode((prev) => !prev);

    // 초기 로딩 시뮬레이션
    useEffect(() => {
        const loadingTimer = setTimeout(() => setIsLoading(false), 800);
        return () => clearTimeout(loadingTimer);
    }, []);
    
    // 앱 시작시 OAuth 사용자 정보 확인
    useEffect(() => {
        const checkAuth = async () => {
            try {
                const response = await fetch('/api/auth/me', { credentials: 'include' });
                if (response.ok) {
                    const data = await response.json();
                    if (data.authenticated && data.user) {
                        setUser({ id: data.user.id, name: data.user.name });
                    }
                }
            } catch (error) {
                // 로그인 안된 상태면 기본값 유지
            }
        };
        
        checkAuth();
    }, []);

    // ⭐ darkMode 상태에 따라 html class 토글 (이 방식이 버그 적음)
    useEffect(() => {
        const el = document.documentElement;
        if (darkMode) el.classList.add('dark');
        else el.classList.remove('dark');
    }, [darkMode]);

    // 전역 에러 처리
    useEffect(() => {
        const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
            console.error('Unhandled promise rejection:', event.reason);
            if (event.reason?.message?.includes('Network Error')) return;
        };
        const handleError = (event: ErrorEvent) => {
            console.error('Global error:', event.error);
        };
        window.addEventListener('unhandledrejection', handleUnhandledRejection);
        window.addEventListener('error', handleError);
        return () => {
            window.removeEventListener('unhandledrejection', handleUnhandledRejection);
            window.removeEventListener('error', handleError);
        };
    }, []);

    if (isLoading) return <LoadingSpinner />;

    return (
        <QueryClientProvider client={queryClient}>
            <UserContext.Provider value={{ user, setUser }}>
                <SocketProvider>
                    <div
                        className={`min-h-screen transition-colors duration-300 ${
                            darkMode ? 'dark bg-gradient-dark' : 'bg-gradient-to-br from-blue-50 to-purple-50'
                        }`}
                    >
                        <Router>
                            <div className="flex flex-col min-h-screen">
                                <Navbar darkMode={darkMode} toggleDarkMode={toggleDarkMode} />

                                <main className="flex-1 container mx-auto px-4 py-6 max-w-7xl">
                                    <Routes>
                                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                                        
                                        {/* ⭐ 로그인 필요: 홈/대시보드 */}
                                        <Route
                                            path="/dashboard"
                                            element={
                                                <RequireAuth>
                                                    <Dashboard />
                                                </RequireAuth>
                                            }
                                        />
                                        
                                        {/* ⭐ 로그인 필요: 매칭 */}
                                        <Route
                                            path="/matching"
                                            element={
                                                <RequireAuth>
                                                    <Matching />
                                                </RequireAuth>
                                            }
                                        />
                                        
                                        {/* ⭐ 로그인 필요: 채팅 */}
                                        <Route
                                            path="/chat"
                                            element={
                                                <RequireAuth>
                                                    <Chat />
                                                </RequireAuth>
                                            }
                                        />
                                        
                                        {/* ⭐ 로그인 필요: 통계 */}
                                        <Route
                                            path="/stats"
                                            element={
                                                <RequireAuth>
                                                    <Stats />
                                                </RequireAuth>
                                            }
                                        />
                                        
                                        {/* ⭐ 로그인 필요: 리뷰 */}
                                        <Route
                                            path="/reviews"
                                            element={
                                                <RequireAuth>
                                                    <Reviews />
                                                </RequireAuth>
                                            }
                                        />
                                        
                                        {/* ⭐ 로그인 필요: 음악탐색 */}
                                        <Route
                                            path="/music"
                                            element={
                                                <RequireAuth>
                                                    <MusicDiscovery />
                                                </RequireAuth>
                                            }
                                        />
                                        
                                        {/* ⭐ 로그인 필요: 프로필 */}
                                        <Route
                                            path="/profile"
                                            element={
                                                <RequireAuth>
                                                    <Profile />
                                                </RequireAuth>
                                            }
                                        />

                                        {/* ⭐ 로그인 필요: 음악 히스토리 */}
                                        <Route
                                            path="/music-history"
                                            element={
                                                <RequireAuth>
                                                    <MusicHistory />
                                                </RequireAuth>
                                            }
                                        />
                                        
                                        <Route 
                                            path="/admin" 
                                            element={
                                                <ProtectedRoute adminOnly={true}>
                                                    <AdminPanel />
                                                </ProtectedRoute>
                                            } 
                                        />
                                        <Route 
                                            path="/admin/reports" 
                                            element={
                                                <ProtectedRoute adminOnly={true}>
                                                    <AdminReports />
                                                </ProtectedRoute>
                                            } 
                                        />

                                        {/* ⭐ 새로운 로그인/회원가입 페이지 */}
                                        <Route path="/login" element={<Login />} />
                                        <Route path="/signup" element={<Signup />} />
                                        
                                        {/* ⭐ 기존 소셜 로그인만 (필요시) */}
                                        <Route path="/auth/social" element={<SocialAuth />} />

                                        {/* 404 처리 (선택) */}
                                        {/* <Route path="*" element={<Navigate to="/dashboard" replace />} /> */}
                                    </Routes>
                                </main>

                                {/* Footer */}
                                <footer className="bg-white/10 dark:bg-gray-900/50 backdrop-blur-lg border-t border-gray-200/20 dark:border-gray-700/30">
                                    <div className="container mx-auto px-4 py-4">
                                        <div className="flex justify-between items-center">
                                            <p className="text-gray-600 dark:text-gray-400 text-sm">음악으로 연결되는 사람들</p>
                                        </div>
                                    </div>
                                </footer>
                            </div>

                            {/* Toast Notifications */}
                            <Toaster
                                position="top-right"
                                toastOptions={{
                                    duration: 4000,
                                    style: {
                                        background: darkMode ? '#1a1a2e' : '#ffffff',
                                        color: darkMode ? '#ffffff' : '#000000',
                                        borderRadius: '12px',
                                        border: `1px solid ${darkMode ? '#374151' : '#e5e7eb'}`,
                                        boxShadow: '0 10px 25px rgba(0, 0, 0, 0.1)',
                                    },
                                    success: { iconTheme: { primary: '#10b981', secondary: '#ffffff' } },
                                    error: { iconTheme: { primary: '#ef4444', secondary: '#ffffff' } },
                                }}
                            />
                        </Router>
                    </div>
                </SocketProvider>
            </UserContext.Provider>
        </QueryClientProvider>
    );
}

export default App;
