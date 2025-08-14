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

    const navigation = [
        { name: '대시보드', href: '/dashboard', icon: Home },
        { name: '매칭', href: '/matching', icon: Users },
        { name: '채팅', href: '/chat', icon: MessageCircle },
        { name: '통계', href: '/stats', icon: BarChart3 },
    ];

    const isActive = (path) => location.pathname === path;

    return (
        <nav className="glass-card sticky top-0 z-50 border-b border-white/10 dark:border-gray-700/30">
            <div className="container mx-auto px-4">
                <div className="flex items-center justify-between h-16">
                    {/* 로고 */}
                    <Link to="/dashboard" className="flex items-center space-x-3 group">
                        <div className="relative">
                            <Music className="h-8 w-8 text-blue-500 group-hover:text-purple-500 transition-colors duration-300" />
                            <Heart className="h-4 w-4 text-pink-500 absolute -top-1 -right-1 animate-pulse" />
                        </div>
                        <div className="hidden sm:block">
                            <h1 className="text-xl font-bold bg-gradient-to-r from-blue-500 to-purple-600 bg-clip-text text-transparent">
                                Music Match
                            </h1>
                            <p className="text-xs text-gray-500 dark:text-gray-400">
                                음악으로 연결되다
                            </p>
                        </div>
                    </Link>

                    {/* 네비게이션 메뉴 */}
                    <div className="hidden md:flex items-center space-x-1">
                        {navigation.map((item) => {
                            const Icon = item.icon;
                            return (
                                <Link
                                    key={item.name}
                                    to={item.href}
                                    className={`nav-link ${isActive(item.href) ? 'active' : ''}`}
                                >
                                    <Icon className="h-4 w-4" />
                                    <span className="hidden lg:block">{item.name}</span>
                                </Link>
                            );
                        })}
                    </div>

                    {/* 오른쪽 메뉴 */}
                    <div className="flex items-center space-x-4">
                        {/* 다크모드 토글 */}
                        <button
                            onClick={toggleDarkMode}
                            className="p-2 rounded-lg bg-white/10 dark:bg-gray-800/50 hover:bg-white/20 dark:hover:bg-gray-700/50 transition-all duration-300 group"
                            aria-label="테마 변경"
                        >
                            {darkMode ? (
                                <Sun className="h-5 w-5 text-yellow-500 group-hover:rotate-180 transition-transform duration-300" />
                            ) : (
                                <Moon className="h-5 w-5 text-gray-600 group-hover:rotate-12 transition-transform duration-300" />
                            )}
                        </button>

                        {/* 사용자 프로필 */}
                        <div className="flex items-center space-x-3">
                            <div className="hidden sm:block text-right">
                                <p className="text-sm font-medium text-gray-800 dark:text-white">
                                    뮤직러버
                                </p>
                                <p className="text-xs text-gray-500 dark:text-gray-400">
                                    음악 애호가
                                </p>
                            </div>
                            <div className="relative">
                                <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
                                    <Music className="h-5 w-5 text-white" />
                                </div>
                                <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white dark:border-gray-800 animate-pulse"></div>
                            </div>
                        </div>

                        {/* 설정 버튼 */}
                        <button className="p-2 rounded-lg bg-white/10 dark:bg-gray-800/50 hover:bg-white/20 dark:hover:bg-gray-700/50 transition-all duration-300 group md:hidden">
                            <Settings className="h-5 w-5 text-gray-600 dark:text-gray-300 group-hover:rotate-90 transition-transform duration-300" />
                        </button>
                    </div>
                </div>

                {/* 모바일 네비게이션 */}
                <div className="md:hidden pb-4">
                    <div className="flex space-x-1 overflow-x-auto">
                        {navigation.map((item) => {
                            const Icon = item.icon;
                            return (
                                <Link
                                    key={item.name}
                                    to={item.href}
                                    className={`nav-link whitespace-nowrap ${isActive(item.href) ? 'active' : ''}`}
                                >
                                    <Icon className="h-4 w-4" />
                                    <span className="text-sm">{item.name}</span>
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