import React from 'react';
import { NavLink, Link, useLocation } from 'react-router-dom';
import {
    Home, Users, MessageCircle, BarChart3, Moon, Sun, Music, Heart, Settings, Star
} from 'lucide-react';
import { useUser } from '../context/UserContext';

const Navbar = ({ darkMode, toggleDarkMode }) => {
    const location = useLocation();
    const { user } = useUser();

    const navItems = [
        { path: '/dashboard', icon: Home,    label: '홈' },
        { path: '/matching',  icon: Users,   label: '매칭' },
        { path: '/chat',      icon: MessageCircle, label: '채팅' },
        { path: '/reviews',   icon: Star,          label: '리뷰' },
        { path: '/stats',     icon: BarChart3,     label: '통계' },
        { path: '/profile',   icon: Settings,      label: '프로필' },
    ];

    const baseLink =
        'nav-link'; // index.css에 정의된 기본 링크 스타일

    const activeLink =
        'nav-link active shadow-[0_10px_30px_rgba(167,139,250,.25)] ring-1 ring-fuchsia-400/40';

    return (
        <nav className="sticky top-0 z-50 bg-white/10 dark:bg-gray-900/40 backdrop-blur-xl border-b border-white/10">
            <div className="container mx-auto px-4">
                <div className="flex h-16 items-center justify-between">
                    {/* 로고 */}
                    <Link to="/dashboard" className="flex items-center gap-3 group">
                        <div className="w-9 h-9 rounded-xl bg-gradient-to-r from-purple-500 to-indigo-500 flex items-center justify-center shadow-lg shadow-purple-500/25">
                            <Music className="h-5 w-5 text-white" />
                        </div>
                        <span className="text-xl font-extrabold tracking-tight bg-gradient-to-r from-fuchsia-400 via-purple-300 to-blue-400 bg-clip-text text-transparent
              group-hover:from-fuchsia-300 group-hover:to-blue-300 transition-colors">
              MusicMatch
            </span>
                    </Link>

                    {/* 데스크탑 메뉴 */}
                    <div className="hidden md:flex items-center gap-1">
                        {navItems.map(({ path, icon: Icon, label }) => (
                            <NavLink
                                key={path}
                                to={path}
                                end
                                className={({ isActive }) =>
                                    isActive
                                        ? `${activeLink}`
                                        : `${baseLink} hover:shadow-[0_8px_18px_rgba(99,102,241,.15)]`
                                }
                            >
                                <Icon className="h-5 w-5" />
                                <span className="font-medium">{label}</span>
                            </NavLink>
                        ))}
                    </div>

                    {/* 오른쪽 영역 */}
                    <div className="flex items-center gap-3">
                        {/* 실시간 연결 상태 뱃지(예시) */}
                        <span className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full glass-card border-white/20 text-xs text-white/80">
              <span className="neon-dot" />
              실시간 연결
            </span>

                        {/* 다크모드 */}
                        <button
                            onClick={toggleDarkMode}
                            aria-label="다크모드 토글"
                            className="glass-card px-2 py-2 rounded-xl hover:scale-[1.02] active:scale-[.98] transition"
                        >
                            {darkMode ? <Sun className="h-5 w-5 text-white/90" /> : <Moon className="h-5 w-5 text-white/90" />}
                        </button>

                        {/* 사용자 미니 프로필 */}
                        <div className="hidden sm:flex items-center gap-2 pl-2">
                            <div className="w-8 h-8 rounded-full bg-gradient-to-r from-purple-400 to-pink-400 flex items-center justify-center shadow-md">
                                <Heart className="h-4 w-4 text-white" />
                            </div>
                            <span className="text-white/90 font-medium">{user.name}</span>
                        </div>
                    </div>
                </div>

                {/* 모바일 메뉴 */}
                <div className="md:hidden pb-3">
                    <div className="flex gap-1 overflow-x-auto">
                        {navItems.map(({ path, icon: Icon, label }) => {
                            const isActive = location.pathname === path || location.pathname.startsWith(path + '/');
                            return (
                                <NavLink
                                    key={path}
                                    to={path}
                                    className={isActive
                                        ? 'flex flex-col items-center gap-1 min-w-0 px-3 py-2 rounded-xl bg-white/15 text-white shadow-[0_0_18px_rgba(167,139,250,.3)]'
                                        : 'flex flex-col items-center gap-1 min-w-0 px-3 py-2 rounded-xl text-blue-100/90 hover:bg-white/10 hover:text-white'
                                    }
                                >
                                    <Icon className="h-5 w-5" />
                                    <span className="text-[11px] font-medium">{label}</span>
                                </NavLink>
                            );
                        })}
                    </div>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;
