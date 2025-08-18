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
import LoadingSpinner from './components/LoadingSpinner';

// Context
import { SocketProvider } from './context/SocketContext';
import { UserContext } from './context/UserContext';

// Styles
import './index.css';

// Create a client for React Query with enhanced error handling
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            retry: (failureCount, error: any) => {
                if (error?.code === 'NETWORK_ERROR' && failureCount < 3) {
                    return true;
                }
                if (error?.status >= 500 && failureCount < 1) {
                    return true;
                }
                return false;
            },
            retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
        },
        mutations: {
            retry: 1,
        },
    },
});

function App() {
    const [user, setUser] = useState({ id: 1, name: '뮤직러버' });
    const [darkMode, setDarkMode] = useState(true);
    const [isLoading, setIsLoading] = useState(true);

    // 다크모드 토글
    const toggleDarkMode = () => {
        setDarkMode(!darkMode);
        if (!darkMode) {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
    };

    // 컴포넌트 마운트 시 초기화
    useEffect(() => {
        // 다크모드 기본 설정
        document.documentElement.classList.add('dark');

        // 로딩 시뮬레이션
        const loadingTimer = setTimeout(() => {
            setIsLoading(false);
        }, 1500);

        return () => {
            clearTimeout(loadingTimer);
        };
    }, []);

    // 전역 에러 처리
    useEffect(() => {
        const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
            console.error('Unhandled promise rejection:', event.reason);
            if (event.reason?.message?.includes('Network Error')) {
                return;
            }
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

    if (isLoading) {
        return <LoadingSpinner />;
    }

    return (
        <QueryClientProvider client={queryClient}>
            <UserContext.Provider value={{ user, setUser }}>
                <SocketProvider>
                    <div className={`min-h-screen transition-colors duration-300 ${
                        darkMode ? 'dark bg-gradient-dark' : 'bg-gradient-to-br from-blue-50 to-purple-50'
                    }`}>
                        <Router>
                            <div className="flex flex-col min-h-screen">
                                <Navbar
                                    darkMode={darkMode}
                                    toggleDarkMode={toggleDarkMode}
                                />

                                <main className="flex-1 container mx-auto px-4 py-6 max-w-7xl">
                                    <Routes>
                                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                                        <Route path="/dashboard" element={<Dashboard />} />
                                        <Route path="/matching" element={<Matching />} />
                                        <Route path="/chat" element={<Chat />} />
                                        <Route path="/stats" element={<Stats />} />
                                        <Route path="/profile" element={<Profile />} />
                                    </Routes>
                                </main>

                                {/* Footer */}
                                <footer className="bg-white/10 dark:bg-gray-900/50 backdrop-blur-lg border-t border-gray-200/20 dark:border-gray-700/30">
                                    <div className="container mx-auto px-4 py-4">
                                        <div className="flex justify-between items-center">
                                            <p className="text-gray-600 dark:text-gray-400 text-sm">
                                                음악으로 연결되는 사람들
                                            </p>
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
                                    success: {
                                        iconTheme: {
                                            primary: '#10b981',
                                            secondary: '#ffffff',
                                        },
                                    },
                                    error: {
                                        iconTheme: {
                                            primary: '#ef4444',
                                            secondary: '#ffffff',
                                        },
                                    },
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