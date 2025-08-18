import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
    Home,
    Users,
    MessageCircle,
    BarChart3,
    Moon,
    Sun,
    Music,
    Heart,
    Settings
} from 'lucide-react';

const Navbar = ({ darkMode, toggleDarkMode }) => {
    const location = useLocation();

    const navItems = [
        { path: '/dashboard', icon: Home, label: '홈' },
        { path: '/matching', icon: Users, label: '매칭' },
        { path: '/chat', icon: MessageCircle, label: '채팅' },
        { path: '/stats', icon: BarChart3, label: '통계' },
        { path: '/profile', icon: Settings, label: '프로필' }
    ];

    return (
        <nav className="bg-white/10 dark:bg-gray-900/50 backdrop-blur-lg border-b border-gray-200/20 dark:border-gray-700/30 sticky top-0 z-50">
            <div className="container mx-auto px-4">
                <div className="flex items-center justify-between h-16">
                    {/* 로고 */}
                    <Link to="/dashboard" className="flex items-center space-x-3">
                        <div className="w-8 h-8 bg-gradient-to-r from-purple-500 to-blue-500 rounded-lg flex items-center justify-center">
                            <Music className="h-5 w-5 text-white" />
                        </div>
                        <span className="text-xl font-bold bg-gradient-to-r from-purple-400 to-blue-600 bg-clip-text text-transparent">
              MusicMatch
            </span>
                    </Link>

                    {/* 네비게이션 메뉴 */}
                    <div className="hidden md:flex items-center space-x-1">
                        {navItems.map((item) => {
                            const Icon = item.icon;
                            const isActive = location.pathname === item.path;

                            return (
                                <Link
                                    key={item.path}
                                    to={item.path}
                                    className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-all duration-200 ${
                                        isActive
                                            ? 'bg-purple-600 text-white shadow-lg'
                                            : 'text-gray-700 dark:text-gray-300 hover:bg-purple-100 dark:hover:bg-purple-900/30 hover:text-purple-600 dark:hover:text-purple-400'
                                    }`}
                                >
                                    <Icon className="h-5 w-5" />
                                    <span className="font-medium">{item.label}</span>
                                </Link>
                            );
                        })}
                    </div>

                    {/* 우측 메뉴 */}
                    <div className="flex items-center space-x-4">
                        {/* 다크모드 토글 */}
                        <button
                            onClick={toggleDarkMode}
                            className="p-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200"
                            aria-label="다크모드 토글"
                        >
                            {darkMode ? (
                                <Sun className="h-5 w-5" />
                            ) : (
                                <Moon className="h-5 w-5" />
                            )}
                        </button>

                        {/* 사용자 정보 */}
                        <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-gradient-to-r from-purple-400 to-pink-400 rounded-full flex items-center justify-center">
                                <Heart className="h-4 w-4 text-white" />
                            </div>
                            <span className="hidden sm:block text-gray-700 dark:text-gray-300 font-medium">
                뮤직러버
              </span>
                        </div>
                    </div>
                </div>

                {/* 모바일 메뉴 */}
                <div className="md:hidden pb-4">
                    <div className="flex space-x-1 overflow-x-auto">
                        {navItems.map((item) => {
                            const Icon = item.icon;
                            const isActive = location.pathname === item.path;

                            return (
                                <Link
                                    key={item.path}
                                    to={item.path}
                                    className={`flex flex-col items-center space-y-1 px-4 py-2 rounded-lg transition-all duration-200 min-w-0 flex-shrink-0 ${
                                        isActive
                                            ? 'bg-purple-600 text-white'
                                            : 'text-gray-700 dark:text-gray-300 hover:bg-purple-100 dark:hover:bg-purple-900/30'
                                    }`}
                                >
                                    <Icon className="h-5 w-5" />
                                    <span className="text-xs font-medium">{item.label}</span>
                                </Link>
                            );
                        })}
                    </div>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;